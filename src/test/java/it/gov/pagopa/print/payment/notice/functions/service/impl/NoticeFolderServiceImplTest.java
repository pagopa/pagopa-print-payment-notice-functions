package it.gov.pagopa.print.payment.notice.functions.service.impl;

import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeBlobClient;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestClient;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NoticeFolderServiceImplTest  {

    @Mock
    PaymentNoticeBlobClient paymentNoticeBlobClient;

    @Mock
    PaymentNoticeGenerationRequestClient paymentNoticeGenerationRequestClient;

    NoticeFolderServiceImpl noticeFolderService;

    @BeforeEach
    void init() {
        Mockito.reset(paymentNoticeBlobClient, paymentNoticeGenerationRequestClient);
        noticeFolderService = new NoticeFolderServiceImpl(paymentNoticeBlobClient, paymentNoticeGenerationRequestClient);
    }

    @Test
    void manageShouldCompleteWithSuccess() {
        BlobStorageResponse blobStorageResponse = new BlobStorageResponse();
        blobStorageResponse.setStatusCode(200);
        when(paymentNoticeBlobClient.compressFolder(any())).thenReturn(blobStorageResponse);
        assertDoesNotThrow(() -> noticeFolderService.manageFolder(PaymentNoticeGenerationRequest.builder().build()));
        verify(paymentNoticeBlobClient).compressFolder(any());
        verify(paymentNoticeGenerationRequestClient).updatePaymentGenerationRequest(any());
    }

    @Test
    void manageShouldThrowExceptionOnBlobNotSuccessful() {
        BlobStorageResponse blobStorageResponse = new BlobStorageResponse();
        blobStorageResponse.setStatusCode(500);
        when(paymentNoticeBlobClient.compressFolder(any())).thenReturn(blobStorageResponse);
        assertThrows(SaveNoticeToBlobException.class, () -> noticeFolderService.manageFolder(PaymentNoticeGenerationRequest.builder().build()));
        verify(paymentNoticeBlobClient).compressFolder(any());
        verifyNoInteractions(paymentNoticeGenerationRequestClient);
    }

    @Test
    void manageShouldThrowException() {
        BlobStorageResponse blobStorageResponse = new BlobStorageResponse();
        blobStorageResponse.setStatusCode(500);
        when(paymentNoticeBlobClient.compressFolder(any())).thenAnswer(item -> {
            throw new SaveNoticeToBlobException("Error", 500);
        });
        assertThrows(SaveNoticeToBlobException.class, () -> noticeFolderService.manageFolder(PaymentNoticeGenerationRequest.builder().build()));
        verify(paymentNoticeBlobClient).compressFolder(any());
        verifyNoInteractions(paymentNoticeGenerationRequestClient);
    }

}
