package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
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
        var message = CompressionEvent.builder()
                .id(folderId)
                .status(PaymentGenerationRequestStatus.COMPLETING)
                .userId("comune di roma")
                .numberOfElementsFailed(0)
                .numberOfElementsTotal(2)
                .items(List.of("11", "22"))
                .build();
//        compressionService.compressFolder(new ObjectMapper().writeValueAsString(message));
        verify(paymentGenerationRequestRepository).save(any());
    }

    @Test
    void compressFolderError() throws IOException {
        String folderId = "123456789";
        BlobStorageResponse mock = mock(BlobStorageResponse.class);
        when(mock.getStatusCode()).thenReturn(400);
        when(noticeStorageClient.compressFolder(folderId)).thenReturn(mock);
        var message = CompressionEvent.builder()
                .id(folderId)
                .status(PaymentGenerationRequestStatus.COMPLETING)
                .userId("comune di roma")
                .numberOfElementsFailed(0)
                .numberOfElementsTotal(2)
                .items(List.of("11", "22"))
                .build();
//        compressionService.compressFolder(new ObjectMapper().writeValueAsString(message));
        verify(noticeRequestErrorProducer).sendErrorEvent(any());
    }
}