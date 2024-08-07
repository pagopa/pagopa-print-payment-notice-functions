package it.gov.pagopa.print.payment.notice.functions.service.impl;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.exception.RequestRecoveryException;
import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestRepository;
import it.gov.pagopa.print.payment.notice.functions.storage.NoticeStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@Slf4j
public class NoticeFolderService {

    @Autowired
    private NoticeStorageClient noticeStorageClient;

    @Autowired
    private PaymentGenerationRequestRepository paymentGenerationRequestRepository;

    @Autowired
    private PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;


    /**
     * Method to manage a folder in completion, calling the related services for compression,
     * if successful it will delete related errors
     *
     * @param paymentNoticeGenerationRequest data to use as input for folder management
     * @throws SaveNoticeToBlobException
     */
    public void manageFolder(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest) throws SaveNoticeToBlobException, IOException {
        var response = noticeStorageClient.compressFolder(paymentNoticeGenerationRequest.getId());
        if (response.getStatusCode() != HttpStatus.OK.value()) {
            throw new SaveNoticeToBlobException("Couldn't create the compressed file", response.getStatusCode());
        }
        log.info("created compressed file {} for User {}", paymentNoticeGenerationRequest.getId(), paymentNoticeGenerationRequest.getUserId());

        paymentNoticeGenerationRequest.setStatus(paymentNoticeGenerationRequest.getNumberOfElementsFailed() != 0 ?
                PaymentGenerationRequestStatus.PROCESSED_WITH_FAILURES :
                PaymentGenerationRequestStatus.PROCESSED);
        paymentGenerationRequestRepository.save(paymentNoticeGenerationRequest);
        paymentGenerationRequestErrorRepository.deleteById(paymentNoticeGenerationRequest.getId());
        MDC.put("massiveStatus", paymentNoticeGenerationRequest.getStatus().toString());
        log.info("Massive Request {} [user {}]", paymentNoticeGenerationRequest.getStatus(), paymentNoticeGenerationRequest.getUserId());
        MDC.remove("massiveStatus");
    }

    /**
     * Finds an existing request using the folder id
     *
     * @param id folder id to use
     * @return instance of a notice generation request
     * @throws RequestRecoveryException
     */
    public PaymentNoticeGenerationRequest findRequest(String id) throws RequestRecoveryException {
        try {
            return paymentGenerationRequestRepository.findById(id).orElseThrow(
                    () -> new RuntimeException("Error on folder recovery"));
        } catch (Exception e) {
            throw new RequestRecoveryException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

}
