package it.gov.pagopa.print.payment.notice.functions.service.impl;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestRepository;
import it.gov.pagopa.print.payment.notice.functions.storage.NoticeStorageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeFolderServiceTest {

    @Mock
    private NoticeStorageClient noticeStorageClient;

    @Mock
    private PaymentGenerationRequestRepository paymentGenerationRequestRepository;

    @Mock
    private PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;

    @InjectMocks
    private NoticeFolderService noticeFolderService;

    @Test
    void manageFolderShouldNotCleanupCompressionErrorsWhenStatusUpdateFails() throws Exception {

        String folderId = "123456789";

        BlobStorageResponse response = mock(BlobStorageResponse.class);
        when(response.getStatusCode()).thenReturn(200);

        when(noticeStorageClient.compressFolder(folderId)).thenReturn(response);

        /*
         * Simulate a successful ZIP creation followed by a Mongo update that does not
         * modify any request.
         */
        when(paymentGenerationRequestRepository.updateStatusById(folderId, PaymentGenerationRequestStatus.PROCESSED))
                .thenReturn(0L);

        var request = PaymentNoticeGenerationRequest.builder().id(folderId).userId("comune di roma")
                .status(PaymentGenerationRequestStatus.COMPLETING).numberOfElementsFailed(0).build();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> noticeFolderService.manageFolder(request));

        assertEquals("Unable to update massive request status for folder: " + folderId, exception.getMessage());

        // The in-memory object must not pretend that finalization succeeded.
        assertEquals(PaymentGenerationRequestStatus.COMPLETING, request.getStatus());

        verify(paymentGenerationRequestRepository).updateStatusById(folderId, PaymentGenerationRequestStatus.PROCESSED);

        /*
         * Compression errors must be preserved because the final status was not
         * successfully persisted.
         */
        verify(paymentGenerationRequestErrorRepository, never()).deleteByFolderIdAndCompressionErrorTrue(anyString());
    }
}