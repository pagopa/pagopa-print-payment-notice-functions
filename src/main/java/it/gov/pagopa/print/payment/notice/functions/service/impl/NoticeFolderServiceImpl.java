package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.microsoft.azure.functions.HttpStatus;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeBlobClient;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestClient;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestErrorClient;
import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeBlobClientImpl;
import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeGenerationRequestClientImpl;
import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeGenerationRequestErrorClientImpl;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.exception.RequestRecoveryException;
import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NoticeFolderServiceImpl implements NoticeFolderService {

    private final Logger logger = LoggerFactory.getLogger(NoticeFolderServiceImpl.class);

    private final PaymentNoticeBlobClient paymentNoticeBlobClient;

    private final PaymentNoticeGenerationRequestClient paymentNoticeGenerationRequestClient;

    private final PaymentNoticeGenerationRequestErrorClient paymentNoticeGenerationRequestErrorClient;


    public NoticeFolderServiceImpl() {
        paymentNoticeBlobClient = PaymentNoticeBlobClientImpl.getInstance();
        paymentNoticeGenerationRequestClient = PaymentNoticeGenerationRequestClientImpl.getInstance();
        paymentNoticeGenerationRequestErrorClient = PaymentNoticeGenerationRequestErrorClientImpl.getInstance();
    }

    public NoticeFolderServiceImpl(PaymentNoticeBlobClient paymentNoticeBlobClient,
                                   PaymentNoticeGenerationRequestClient paymentNoticeGenerationRequestClient, PaymentNoticeGenerationRequestErrorClient paymentNoticeGenerationRequestErrorClient) {
        this.paymentNoticeBlobClient = paymentNoticeBlobClient;
        this.paymentNoticeGenerationRequestClient = paymentNoticeGenerationRequestClient;
        this.paymentNoticeGenerationRequestErrorClient = paymentNoticeGenerationRequestErrorClient;
    }

    /**
     * Method to managed a folder in completion, calling the related services for compression,
     * if successful it will delete related errors
     * @param paymentNoticeGenerationRequest data to use as input for folder management
     * @throws SaveNoticeToBlobException
     */
    @Override
    public void manageFolder(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest)
            throws SaveNoticeToBlobException {

        try {
            BlobStorageResponse response = paymentNoticeBlobClient
                    .compressFolder(paymentNoticeGenerationRequest.getId());
            if (response.getStatusCode() != HttpStatus.OK.value()) {
                throw new SaveNoticeToBlobException("Couldn't create the compressed file",
                        response.getStatusCode());
            }
        } catch (SaveNoticeToBlobException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }

        try {
            paymentNoticeGenerationRequest.setStatus(paymentNoticeGenerationRequest.getNumberOfElementsFailed() != 0 ?
                    PaymentGenerationRequestStatus.PROCESSED_WITH_FAILURES :
                    PaymentGenerationRequestStatus.PROCESSED);
            paymentNoticeGenerationRequestClient
                    .updatePaymentGenerationRequest(paymentNoticeGenerationRequest);
            paymentNoticeGenerationRequestErrorClient.deleteRequestError(paymentNoticeGenerationRequest.getId());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }



    }

    /**
     * Finds an existing request using the folder id
     * @param id folder id to use
     * @return instance of a notice generation request
     * @throws RequestRecoveryException
     */
    @Override
    public PaymentNoticeGenerationRequest findRequest(String id) throws RequestRecoveryException {
        try {
            return paymentNoticeGenerationRequestClient.findById(id).orElseThrow(
                    () -> new RuntimeException("Error on folder recovery"));
        } catch (Exception e) {
            throw new RequestRecoveryException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

}
