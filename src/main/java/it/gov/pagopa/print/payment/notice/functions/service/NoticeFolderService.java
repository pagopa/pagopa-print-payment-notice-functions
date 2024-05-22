package it.gov.pagopa.print.payment.notice.functions.service;


import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;

import java.nio.file.Path;

/**
 * Provides services to manage the incoming requests
 */
public interface NoticeFolderService {

    /**
     * Method that contains the logic for the compression function, whenever
     * a valid request is provided it will attempt to save the compressed folder
     * and update the status accordingly
     * @param paymentNoticeGenerationRequest data to use as input for folder management
     * @throws SaveNoticeToBlobException
     */
    void manageFolder(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest) throws SaveNoticeToBlobException;

}
