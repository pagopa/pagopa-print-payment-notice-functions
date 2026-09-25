package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeRequestErrorProducer;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestRepository;
import it.gov.pagopa.print.payment.notice.functions.storage.NoticeStorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CompressionService.class, NoticeFolderService.class, ObjectMapper.class})
class CompressionServiceTest {

    @MockBean
    private NoticeRequestErrorProducer noticeRequestErrorProducer;

    @MockBean
    private NoticeStorageClient noticeStorageClient;

    @MockBean
    private PaymentGenerationRequestRepository paymentGenerationRequestRepository;

    @MockBean
    private PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;


    @Autowired
    @InjectMocks
    private CompressionService compressionService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void compressFolder() throws IOException {

        String folderId = "123456789";

        BlobStorageResponse mock = mock(BlobStorageResponse.class);
        when(mock.getStatusCode()).thenReturn(200);

        when(noticeStorageClient.compressFolder(folderId)).thenReturn(mock);
        when(paymentGenerationRequestRepository.updateStatusById(folderId, PaymentGenerationRequestStatus.PROCESSED))
                .thenReturn(1L);

        var elem = CompressionEvent.builder().id(folderId).status(PaymentGenerationRequestStatus.COMPLETING)
                .userId("comune di roma").numberOfElementsFailed(0).numberOfElementsTotal(2).items(List.of("11", "22"))
                .build();

        var message = List.of(new ObjectMapper().writeValueAsString(elem));

        var persistedRequest = PaymentNoticeGenerationRequest.builder().id(folderId)
                .status(PaymentGenerationRequestStatus.COMPLETING).userId("comune di roma").numberOfElementsFailed(0)
                .numberOfElementsTotal(2).items(List.of("11", "22")).build();

        when(paymentGenerationRequestRepository.findById(folderId)).thenReturn(Optional.of(persistedRequest));

        compressionService.compressFolder(message);

        verify(paymentGenerationRequestRepository).updateStatusById(folderId, PaymentGenerationRequestStatus.PROCESSED);
        verify(paymentGenerationRequestRepository, never()).save(any());
        verify(paymentGenerationRequestErrorRepository).deleteByFolderIdAndCompressionErrorTrue(folderId);
    }

    @Test
    void compressFolderError() throws IOException {
        String folderId = "123456789";
        BlobStorageResponse mock = mock(BlobStorageResponse.class);
        when(mock.getStatusCode()).thenReturn(400);
        when(noticeStorageClient.compressFolder(folderId)).thenReturn(mock);
        when(noticeRequestErrorProducer.sendErrorEvent(any())).thenReturn(true);
        var elem = CompressionEvent.builder()
                .id(folderId)
                .status(PaymentGenerationRequestStatus.COMPLETING)
                .userId("comune di roma")
                .numberOfElementsFailed(0)
                .numberOfElementsTotal(2)
                .items(List.of("11", "22"))
                .build();
        var message = List.of(new ObjectMapper().writeValueAsString(elem));
        compressionService.compressFolder(message);
        verify(noticeRequestErrorProducer).sendErrorEvent(any());
        verify(paymentGenerationRequestRepository, never()).updateStatusById(any(), any());
        verify(paymentGenerationRequestErrorRepository, never()).deleteByFolderIdAndCompressionErrorTrue(any());
    }
    
    @Test
    void compressFolderShouldFailWhenErrorEventCannotBePublished() throws IOException {
        String folderId = "123456789";
        BlobStorageResponse mock = mock(BlobStorageResponse.class);
        when(mock.getStatusCode()).thenReturn(400);
        when(noticeStorageClient.compressFolder(folderId)).thenReturn(mock);

        /*
         * Simulate a binding failure where StreamBridge does not throw an exception but
         * reports that the message was not sent.
         */
        when(noticeRequestErrorProducer.sendErrorEvent(any())).thenReturn(false);

        var elem = CompressionEvent.builder().id(folderId).status(PaymentGenerationRequestStatus.COMPLETING)
                .userId("comune di roma").numberOfElementsFailed(0).numberOfElementsTotal(2).items(List.of("11", "22"))
                .build();

        var message = List.of(new ObjectMapper().writeValueAsString(elem));
        assertThrows(RuntimeException.class, () -> compressionService.compressFolder(message));
        verify(noticeRequestErrorProducer).sendErrorEvent(any());
        verify(paymentGenerationRequestRepository, never()).updateStatusById(any(), any());
    }
    
    @Test
    void compressFolderShouldFailWhenErrorEventPublicationThrowsException() throws IOException {
        String folderId = "123456789";
        BlobStorageResponse mock = mock(BlobStorageResponse.class);
        when(mock.getStatusCode()).thenReturn(400);
        when(noticeStorageClient.compressFolder(folderId)).thenReturn(mock);
        doThrow(new RuntimeException("Event Hub unavailable")).when(noticeRequestErrorProducer).sendErrorEvent(any());

        var elem = CompressionEvent.builder().id(folderId).status(PaymentGenerationRequestStatus.COMPLETING)
                .userId("comune di roma").numberOfElementsFailed(0).numberOfElementsTotal(2).items(List.of("11", "22"))
                .build();

        var message = List.of(new ObjectMapper().writeValueAsString(elem));
        assertThrows(RuntimeException.class, () -> compressionService.compressFolder(message));
        verify(noticeRequestErrorProducer).sendErrorEvent(any());
    }
    
    @Test
    void compressFolderShouldSkipStaleCompletionEventWhenFolderIsAlreadyProcessed() throws IOException {

        String folderId = "123456789";

        var completionEvent = CompressionEvent.builder().id(folderId).status(PaymentGenerationRequestStatus.COMPLETING)
                .userId("comune di roma").numberOfElementsFailed(0).numberOfElementsTotal(2).items(List.of("11", "22"))
                .build();

        /*
         * The event still says COMPLETING, but Mongo is the source of truth and the
         * folder has already been processed.
         */
        var persistedRequest = PaymentNoticeGenerationRequest.builder().id(folderId)
                .status(PaymentGenerationRequestStatus.PROCESSED).userId("comune di roma").numberOfElementsFailed(0)
                .numberOfElementsTotal(2).items(List.of("11", "22")).build();

        when(paymentGenerationRequestRepository.findById(folderId)).thenReturn(Optional.of(persistedRequest));

        var message = List.of(new ObjectMapper().writeValueAsString(completionEvent));

        compressionService.compressFolder(message);

        verify(paymentGenerationRequestRepository).findById(folderId);

        verify(noticeStorageClient, never()).compressFolder(anyString());

        verify(paymentGenerationRequestRepository, never()).updateStatusById(anyString(), any());

        verify(paymentGenerationRequestErrorRepository, never()).deleteByFolderIdAndCompressionErrorTrue(anyString());

        verifyNoInteractions(noticeRequestErrorProducer);
    }
}