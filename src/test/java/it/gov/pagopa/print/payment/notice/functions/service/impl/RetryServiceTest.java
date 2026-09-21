package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.print.payment.notice.functions.events.model.ErrorEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeGenerationRequestProducer;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeRequestCompleteProducer;
import it.gov.pagopa.print.payment.notice.functions.exception.Aes256Exception;
import it.gov.pagopa.print.payment.notice.functions.exception.RetryEventPublicationException;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestRepository;
import it.gov.pagopa.print.payment.notice.functions.utils.Aes256Utils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class RetryServiceTest {

    @MockBean
    PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;

    @MockBean
    NoticeRequestCompleteProducer noticeRequestCompleteProducer;

    @MockBean
    NoticeGenerationRequestProducer noticeGenerationRequestProducer;

    @MockBean
    PaymentGenerationRequestRepository paymentGenerationRequestRepository;

    @Autowired
    @InjectMocks
    private RetryService retryService;

    @Autowired
    private Aes256Utils aes256Utils;

    @Test
    void retryError() throws JsonProcessingException, Aes256Exception {

        when(paymentGenerationRequestErrorRepository.findById("1")).thenReturn(Optional.ofNullable(PaymentNoticeGenerationRequestError.builder()
                .id("1")
                .folderId("1234")
                .errorId("8")
                .errorCode("NOT FOUND")
                .errorDescription("ITEM NOT FOUND")
                .data("{\"key\": \"value\"}")
                .numberOfAttempts(0)
                .compressionError(false)
                .build()));

        when(noticeGenerationRequestProducer.sendGenerationEvent(any())).thenReturn(true);
        when(paymentGenerationRequestErrorRepository.incrementNumberOfAttemptsIfBelowMax("1", 3)).thenReturn(1L);
        

        var elem = ErrorEvent.builder()
                .id("1")
                .folderId("1234")
                .errorId("8")
                .errorCode("NOT FOUND")
                .errorDescription("ITEM NOT FOUND")
                .createdAt("")
                .data(aes256Utils.encrypt(new ObjectMapper().writeValueAsString(GenerationEvent.builder().build())))
                .numberOfAttempts(0)
                .compressionError(false)
                .build();
        retryService.retryError(new ObjectMapper().writeValueAsString(elem));

        verify(paymentGenerationRequestErrorRepository, times(1)).incrementNumberOfAttemptsIfBelowMax("1", 3);
        verify(paymentGenerationRequestErrorRepository, never()).save(any());
        verify(noticeRequestCompleteProducer, never()).sendNoticeComplete(any());
        verify(noticeGenerationRequestProducer, times(1)).sendGenerationEvent(any());

    }

    @Test
    void retryErrorCompression() throws JsonProcessingException {

        /*
         * CompressionService produces an ErrorEvent without the error document id.
         * The folderId is the identifier of the massive-generation request that
         * must be compressed again.
         */
        var errorEntity = PaymentNoticeGenerationRequestError.builder()
                .id("generated-error-id")
                .folderId("123456")
                .errorId("123456")
                .numberOfAttempts(0)
                .compressionError(true)
                .build();

        when(paymentGenerationRequestErrorRepository.save(any()))
                .thenReturn(errorEntity);

        /*
         * The massive-generation request must be retrieved using folderId,
         * not the error document id.
         */
        when(paymentGenerationRequestRepository.findById("123456"))
                .thenReturn(Optional.of(
                        PaymentNoticeGenerationRequest.builder()
                                .id("123456")
                                .userId("user")
                                .createdAt(null)
                                .requestDate(null)
                                .status(PaymentGenerationRequestStatus.COMPLETING)
                                .items(List.of("1"))
                                .numberOfElementsFailed(0)
                                .numberOfElementsTotal(1)
                                .build()));

        when(noticeRequestCompleteProducer.sendNoticeComplete(any()))
                .thenReturn(true);
        when(paymentGenerationRequestErrorRepository
                .findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc("123456"))
                .thenReturn(Optional.empty());
        when(paymentGenerationRequestErrorRepository.save(any()))
                .thenReturn(PaymentNoticeGenerationRequestError.builder().id("compression-error-1").folderId("123456")
                        .errorId("123456").numberOfAttempts(0).compressionError(true).build());
        when(paymentGenerationRequestErrorRepository.incrementNumberOfAttemptsIfBelowMax("compression-error-1", 3))
                .thenReturn(1L);

        /*
         * Reproduce the ErrorEvent actually generated by CompressionService:
         * id is not populated, while folderId contains the folder to retry.
         */
        var elem = ErrorEvent.builder()
                .folderId("123456")
                .errorId("123456")
                .numberOfAttempts(0)
                .compressionError(true)
                .build();

        retryService.retryError(
                new ObjectMapper().writeValueAsString(elem));

        // The compression retry must recover the request using its folderId.
        verify(paymentGenerationRequestRepository, times(1))
                .findById("123456");

        /*
         * One save creates the compression error. Subsequent retry counter updates
         * are performed atomically without replacing the whole document.
         */
        verify(paymentGenerationRequestErrorRepository, times(1)).save(any());

        verify(paymentGenerationRequestErrorRepository, times(1))
                .incrementNumberOfAttemptsIfBelowMax("compression-error-1", 3);

        // A compression error must generate another completion event.
        verify(noticeRequestCompleteProducer, times(1))
                .sendNoticeComplete(any());

        verify(noticeGenerationRequestProducer, never())
                .sendGenerationEvent(any());
    }
    
    @Test
    void retryErrorCompressionShouldStopAfterMaxRetries() throws JsonProcessingException {

        PaymentNoticeGenerationRequestError persistedError = PaymentNoticeGenerationRequestError.builder()
                .id("compression-error-1").folderId("123456").errorId("123456").numberOfAttempts(0)
                .compressionError(true).build();

        // RetryService must always reuse the Mongo error associated with the folder.
        when(paymentGenerationRequestErrorRepository
                .findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc("123456"))
                .thenReturn(Optional.of(persistedError));

        // Simulate three available retry slots followed by exhaustion of the configured maximum.
        when(paymentGenerationRequestErrorRepository.incrementNumberOfAttemptsIfBelowMax("compression-error-1", 3))
                .thenReturn(1L, 1L, 1L, 0L);

        when(paymentGenerationRequestRepository.findById("123456"))
                .thenReturn(Optional.of(PaymentNoticeGenerationRequest.builder().id("123456").userId("user")
                        .status(PaymentGenerationRequestStatus.COMPLETING).items(List.of("1")).numberOfElementsFailed(0)
                        .numberOfElementsTotal(1).build()));

        when(noticeRequestCompleteProducer.sendNoticeComplete(any())).thenReturn(true);

        /*
         * Reproduce the message generated by CompressionService after every failure:
         * numberOfAttempts is always zero and id is not populated.
         */
        ErrorEvent event = ErrorEvent.builder().folderId("123456").errorId("123456").numberOfAttempts(0)
                .compressionError(true).build();

        String message = new ObjectMapper().writeValueAsString(event);

        retryService.retryError(message);
        retryService.retryError(message);
        retryService.retryError(message);
        retryService.retryError(message);

        /*
         * Mongo is consulted for every delivery, independently of the zero value
         * carried by the incoming ErrorEvent.
         */
        verify(paymentGenerationRequestErrorRepository, times(4))
                .findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc("123456");

        /*
         * The fourth atomic increment fails because the configured maximum has already
         * been reached.
         */
        verify(paymentGenerationRequestErrorRepository, times(4))
                .incrementNumberOfAttemptsIfBelowMax("compression-error-1", 3);

        // Only three actual compression retries are emitted.
        verify(noticeRequestCompleteProducer, times(3)).sendNoticeComplete(any());

        verify(noticeGenerationRequestProducer, never()).sendGenerationEvent(any());
    }
    
    @Test
    void retryCompressionShouldReleaseAttemptWhenPublicationFails() throws JsonProcessingException {

        var persistedError = PaymentNoticeGenerationRequestError.builder().id("compression-error-1").folderId("123456")
                .errorId("123456").numberOfAttempts(0).compressionError(true).build();

        when(paymentGenerationRequestErrorRepository
                .findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc("123456"))
                .thenReturn(Optional.of(persistedError));

        when(paymentGenerationRequestErrorRepository.incrementNumberOfAttemptsIfBelowMax("compression-error-1", 3))
                .thenReturn(1L);

        when(paymentGenerationRequestRepository.findById("123456"))
                .thenReturn(Optional.of(PaymentNoticeGenerationRequest.builder().id("123456").userId("user")
                        .status(PaymentGenerationRequestStatus.COMPLETING).items(List.of("1")).numberOfElementsFailed(0)
                        .numberOfElementsTotal(1).build()));

        when(noticeRequestCompleteProducer.sendNoticeComplete(any())).thenReturn(false);

        when(paymentGenerationRequestErrorRepository.decrementNumberOfAttemptsIfGreaterThanZero("compression-error-1"))
                .thenReturn(1L);

        var event = ErrorEvent.builder().folderId("123456").errorId("123456").numberOfAttempts(0).compressionError(true)
                .build();

        String message = new ObjectMapper().writeValueAsString(event);

        assertThrows(RetryEventPublicationException.class, () -> retryService.retryError(message));

        verify(paymentGenerationRequestErrorRepository).incrementNumberOfAttemptsIfBelowMax("compression-error-1", 3);

        verify(noticeRequestCompleteProducer).sendNoticeComplete(any());

        verify(paymentGenerationRequestErrorRepository)
                .decrementNumberOfAttemptsIfGreaterThanZero("compression-error-1");
        
        // No document must be saved.
        verify(paymentGenerationRequestErrorRepository, never()).save(any());
    }
    
    @Test
    void retryGenerationShouldReleaseAttemptWhenPublicationFails() throws JsonProcessingException, Aes256Exception {

        var persistedError = PaymentNoticeGenerationRequestError.builder().id("1").folderId("1234").errorId("8")
                .errorCode("NOT FOUND").errorDescription("ITEM NOT FOUND").data("{\"key\": \"value\"}")
                .numberOfAttempts(0).compressionError(false).build();

        when(paymentGenerationRequestErrorRepository.findById("1")).thenReturn(Optional.of(persistedError));

        
        // The retry slot is successfully acquired. 
        when(paymentGenerationRequestErrorRepository.incrementNumberOfAttemptsIfBelowMax("1", 3)).thenReturn(1L);

        // Simulate a publication failure reported by the producer without throwing an exception.
        when(noticeGenerationRequestProducer.sendGenerationEvent(any())).thenReturn(false);
        when(paymentGenerationRequestErrorRepository.decrementNumberOfAttemptsIfGreaterThanZero("1")).thenReturn(1L);

        var generationEvent = GenerationEvent.builder().build();

        var elem = ErrorEvent.builder().id("1").folderId("1234").errorId("8").errorCode("NOT FOUND")
                .errorDescription("ITEM NOT FOUND").createdAt("")
                .data(aes256Utils.encrypt(new ObjectMapper().writeValueAsString(generationEvent))).numberOfAttempts(0)
                .compressionError(false).build();

        String message = new ObjectMapper().writeValueAsString(elem);

        assertThrows(RetryEventPublicationException.class, () -> retryService.retryError(message));

        // The retry attempt is acquired before publishing the event.
        verify(paymentGenerationRequestErrorRepository, times(1)).incrementNumberOfAttemptsIfBelowMax("1", 3);

        // The generation retry is actually attempted.
        verify(noticeGenerationRequestProducer, times(1)).sendGenerationEvent(any());

        // Since publication failed, the acquired retry slot must be released.
        verify(paymentGenerationRequestErrorRepository, times(1)).decrementNumberOfAttemptsIfGreaterThanZero("1");

        // No compression event must be published.
        verify(noticeRequestCompleteProducer, never()).sendNoticeComplete(any());
        
        // No document must be saved.
        verify(paymentGenerationRequestErrorRepository, never()).save(any());
    }
    
    @Test
    void retryCompressionShouldBeSkippedWhenFolderIsNoLongerCompleting() throws JsonProcessingException {

        var persistedError = PaymentNoticeGenerationRequestError.builder().id("compression-error-1").folderId("123456")
                .errorId("123456").numberOfAttempts(1).compressionError(true).build();

        // The compression error already exists in Mongo.
        when(paymentGenerationRequestErrorRepository
                .findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc("123456"))
                .thenReturn(Optional.of(persistedError));

        /*
         * Simulate a duplicate ErrorEvent arriving after another execution has already
         * completed the folder.
         */
        when(paymentGenerationRequestRepository.findById("123456"))
                .thenReturn(Optional.of(PaymentNoticeGenerationRequest.builder().id("123456").userId("user")
                        .status(PaymentGenerationRequestStatus.PROCESSED).items(List.of("1")).numberOfElementsFailed(0)
                        .numberOfElementsTotal(1).build()));

        var event = ErrorEvent.builder().folderId("123456").errorId("123456").numberOfAttempts(0).compressionError(true)
                .build();

        String message = new ObjectMapper().writeValueAsString(event);

        retryService.retryError(message);

        verify(paymentGenerationRequestErrorRepository, never()).incrementNumberOfAttemptsIfBelowMax(any(), anyInt());

        // No new compression retry must be published.
        verify(noticeRequestCompleteProducer, never()).sendNoticeComplete(any());

        /*
         * It is a compression error, the generation producer must not be involved
         * either.
         */
        verify(noticeGenerationRequestProducer, never()).sendGenerationEvent(any());

        // No compensation is required because no retry attempt was acquired.
        verify(paymentGenerationRequestErrorRepository, never()).decrementNumberOfAttemptsIfGreaterThanZero(any());
    }
    
    @Test
    void retryCompressionShouldNotCreateErrorWhenFolderIsAlreadyProcessed() throws JsonProcessingException {

        /*
         * Simulate a stale compression ErrorEvent received after the folder has already
         * been successfully processed.
         */
        when(paymentGenerationRequestRepository.findById("123456"))
                .thenReturn(Optional.of(PaymentNoticeGenerationRequest.builder().id("123456").userId("user")
                        .status(PaymentGenerationRequestStatus.PROCESSED).items(List.of("1")).numberOfElementsFailed(0)
                        .numberOfElementsTotal(1).build()));

        var event = ErrorEvent.builder().folderId("123456").errorId("123456").numberOfAttempts(0).compressionError(true)
                .build();

        String message = new ObjectMapper().writeValueAsString(event);

        retryService.retryError(message);

        // The folder state is checked before accessing the error repository.
        verify(paymentGenerationRequestErrorRepository, never())
                .findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc(any());

        verify(paymentGenerationRequestErrorRepository, never()).save(any());

        // No retry slot must be consumed.
        verify(paymentGenerationRequestErrorRepository, never()).incrementNumberOfAttemptsIfBelowMax(any(), anyInt());

        // No retry event must be published.
        verify(noticeRequestCompleteProducer, never()).sendNoticeComplete(any());

        verify(noticeGenerationRequestProducer, never()).sendGenerationEvent(any());

        // No compensation is required because no retry attempt was acquired.
        verify(paymentGenerationRequestErrorRepository, never()).decrementNumberOfAttemptsIfGreaterThanZero(any());
    }
    
    @Test
    void retryCompressionShouldPropagatePublicationFailureWhenCompensationFails() throws JsonProcessingException {

        var persistedError = PaymentNoticeGenerationRequestError.builder().id("compression-error-1").folderId("123456")
                .errorId("123456").numberOfAttempts(0).compressionError(true).build();

        when(paymentGenerationRequestErrorRepository
                .findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc("123456"))
                .thenReturn(Optional.of(persistedError));
        when(paymentGenerationRequestRepository.findById("123456"))
                .thenReturn(Optional.of(PaymentNoticeGenerationRequest.builder().id("123456").userId("user")
                        .status(PaymentGenerationRequestStatus.COMPLETING).items(List.of("1")).numberOfElementsFailed(0)
                        .numberOfElementsTotal(1).build()));
        when(paymentGenerationRequestErrorRepository.incrementNumberOfAttemptsIfBelowMax("compression-error-1", 3))
                .thenReturn(1L);
        when(noticeRequestCompleteProducer.sendNoticeComplete(any())).thenReturn(false);
        when(paymentGenerationRequestErrorRepository.decrementNumberOfAttemptsIfGreaterThanZero("compression-error-1"))
                .thenThrow(new RuntimeException("Mongo unavailable"));

        var event = ErrorEvent.builder().folderId("123456").errorId("123456").numberOfAttempts(0).compressionError(true)
                .build();

        String message = new ObjectMapper().writeValueAsString(event);

        assertThrows(RetryEventPublicationException.class, () -> retryService.retryError(message));

        verify(noticeRequestCompleteProducer).sendNoticeComplete(any());
        verify(paymentGenerationRequestErrorRepository)
                .decrementNumberOfAttemptsIfGreaterThanZero("compression-error-1");
    }
}