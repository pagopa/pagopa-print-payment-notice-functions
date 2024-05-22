package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.azure.storage.blob.models.BlobStorageException;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeBlobClient;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestClient;
import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeBlobClientImpl;
import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeGenerationRequestClientImpl;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NoticeFolderServiceImpl implements NoticeFolderService {

    private final Logger logger = LoggerFactory.getLogger(NoticeFolderServiceImpl.class);

    private final PaymentNoticeBlobClient paymentNoticeBlobClient;

    private final PaymentNoticeGenerationRequestClient paymentNoticeGenerationRequestClient;

    public NoticeFolderServiceImpl() {
        paymentNoticeBlobClient = PaymentNoticeBlobClientImpl.getInstance();
        paymentNoticeGenerationRequestClient = PaymentNoticeGenerationRequestClientImpl.getInstance();
    }

    public NoticeFolderServiceImpl(PaymentNoticeBlobClient paymentNoticeBlobClient,
                                   PaymentNoticeGenerationRequestClient paymentNoticeGenerationRequestClient) {
        this.paymentNoticeBlobClient = paymentNoticeBlobClient;
        this.paymentNoticeGenerationRequestClient = paymentNoticeGenerationRequestClient;
    }

    @Override
    public void manageFolder(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest)
            throws SaveNoticeToBlobException {

        try {
            BlobStorageResponse response = paymentNoticeBlobClient
                    .compressFolder(paymentNoticeGenerationRequest.getId());
            if (response.getStatusCode() != 200) {
                throw new SaveNoticeToBlobException("Couldn't create the compressed file",
                        response.getStatusCode());
            }
        } catch (SaveNoticeToBlobException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }

        try {
            paymentNoticeGenerationRequest.setStatus(PaymentGenerationRequestStatus.PROCESSED);
            paymentNoticeGenerationRequestClient
                    .updatePaymentGenerationRequest(paymentNoticeGenerationRequest);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }

    }

}
