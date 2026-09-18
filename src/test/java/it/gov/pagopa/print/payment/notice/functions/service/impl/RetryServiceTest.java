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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class RetryServiceTest {

    @MockBean
    PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;

    @MockBean
    NoticeRequestCompleteProducer noticeRequestCompleteProducer;

    @MockBean
    NoticeGenerationRequestProducer NoticeGenerationRequestProducer;

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

        when(NoticeGenerationRequestProducer.sendGenerationEvent(any())).thenReturn(true);
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
        verify(NoticeGenerationRequestProducer, times(1)).sendGenerationEvent(any());

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

        /*
         * The compression retry must recover the request using its folderId.
         */
        verify(paymentGenerationRequestRepository, times(1))
                .findById("123456");

        /*
         * One save creates the compression error. Subsequent retry counter updates
         * are performed atomically without replacing the whole document.
         */
        verify(paymentGenerationRequestErrorRepository, times(1)).save(any());

        verify(paymentGenerationRequestErrorRepository, times(1))
                .incrementNumberOfAttemptsIfBelowMax("compression-error-1", 3);

        /*
         * A compression error must generate another completion event.
         */
        verify(noticeRequestCompleteProducer, times(1))
                .sendNoticeComplete(any());

        verify(NoticeGenerationRequestProducer, never())
                .sendGenerationEvent(any());
    }
    
    @Test
    void retryErrorCompressionShouldStopAfterMaxRetries() throws JsonProcessingException {

        PaymentNoticeGenerationRequestError persistedError = PaymentNoticeGenerationRequestError.builder()
                .id("compression-error-1").folderId("123456").errorId("123456").numberOfAttempts(0)
                .compressionError(true).build();

        /*
         * RetryService must always reuse the Mongo error associated with the folder.
         */
        when(paymentGenerationRequestErrorRepository
                .findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc("123456"))
                .thenReturn(Optional.of(persistedError));

        /*
         * Simulate three available retry slots followed by exhaustion of the configured
         * maximum.
         */
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

        /*
         * Only three actual compression retries are emitted.
         */
        verify(noticeRequestCompleteProducer, times(3)).sendNoticeComplete(any());

        verify(NoticeGenerationRequestProducer, never()).sendGenerationEvent(any());
    }
}