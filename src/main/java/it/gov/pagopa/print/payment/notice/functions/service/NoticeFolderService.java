package it.gov.pagopa.print.payment.notice.functions.service;


import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;

import java.nio.file.Path;

public interface NoticeFolderService {

    void manageFolder(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest) throws SaveNoticeToBlobException;

}
