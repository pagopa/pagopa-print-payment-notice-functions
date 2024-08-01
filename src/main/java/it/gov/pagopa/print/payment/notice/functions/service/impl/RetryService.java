package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.RetryEvent;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeGenerationRequestProducer;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeRequestCompleteProducer;
import it.gov.pagopa.print.payment.notice.functions.exception.Aes256Exception;
import it.gov.pagopa.print.payment.notice.functions.exception.PaymentNoticeManagementException;
import it.gov.pagopa.print.payment.notice.functions.exception.RequestRecoveryException;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.print.payment.notice.functions.utils.Aes256Utils;
import it.gov.pagopa.print.payment.notice.functions.utils.ObjectMapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RetryService {

    @Value("${max_retry.on_error}")
    private int maxRetriesOnErrors;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NoticeFolderService noticeFolderService;

    @Autowired
    private NoticeGenerationRequestProducer noticeGenerationRequestProducer;

    @Autowired
    private NoticeRequestCompleteProducer noticeRequestCompleteProducer;

    @Autowired
    private PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;

    public void retryError(String message) {

        try {
            var retryMessage = objectMapper.readValue(message, RetryEvent.class);

            MDC.put("folderId", retryMessage.getFolderId());
            log.info("Starting Retry Function {}", retryMessage);

            var paymentNoticeGenerationRequestError = findErrorOrCreate(retryMessage);


            if (paymentNoticeGenerationRequestError != null && retryMessage.getNumberOfAttempts() < maxRetriesOnErrors) {

                updateNumberOfAttempts(paymentNoticeGenerationRequestError);

                if (isCompressionError(retryMessage, paymentNoticeGenerationRequestError)) {
                    CompressionEvent compressionEvent = buildCompressionError(retryMessage);
                    noticeRequestCompleteProducer.noticeComplete(compressionEvent);
                    log.debug("Sent a new compression event");

                } else {
                    GenerationEvent generationEvent = buildNoticeRetry(retryMessage);
                    noticeGenerationRequestProducer.noticeGeneration(generationEvent);
                    log.debug("Sent a new generation event");
                }
            }


        } catch (Exception e) {
            MDC.put("massiveStatus", "EXCEPTION");
            log.error(e.getMessage(), e);
        }
    }

    private void updateNumberOfAttempts(PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
        int numberOfAttempts = paymentNoticeGenerationRequestError.getNumberOfAttempts() + 1;
        var entity = paymentNoticeGenerationRequestError.toBuilder()
                .numberOfAttempts(numberOfAttempts)
                .build();
        paymentGenerationRequestErrorRepository.save(entity);
        log.debug("Updated Number Of Attempts");
    }

    private boolean isCompressionError(RetryEvent retryMessage, PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
        return retryMessage.isCompressionError() && !"UNKNOWN".equals(paymentNoticeGenerationRequestError.getFolderId());
    }

    private PaymentNoticeGenerationRequestError findErrorOrCreate(RetryEvent retryMessage) throws PaymentNoticeManagementException {
        PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError;
        if (retryMessage.getId() != null) {
            paymentNoticeGenerationRequestError = paymentGenerationRequestErrorRepository.findById(retryMessage.getId())
                    .orElseThrow(() -> new PaymentNoticeManagementException("Request retryMessage not found", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        } else {
            paymentNoticeGenerationRequestError = PaymentNoticeGenerationRequestError.builder()
                    .folderId(retryMessage.getFolderId())
                    .errorId(retryMessage.getErrorId())
                    .numberOfAttempts(retryMessage.getNumberOfAttempts())
                    .compressionError(retryMessage.isCompressionError())
                    .data(retryMessage.getData())
                    .errorCode(retryMessage.getErrorCode())
                    .errorDescription(retryMessage.getErrorDescription())
                    .build();
            var entity = paymentGenerationRequestErrorRepository.save(paymentNoticeGenerationRequestError);
            paymentNoticeGenerationRequestError.setId(entity.getId());
        }
        return paymentNoticeGenerationRequestError;
    }

    private CompressionEvent buildCompressionError(RetryEvent error) throws RequestRecoveryException {

        PaymentNoticeGenerationRequest paymentNoticeGenerationRequest = noticeFolderService.findRequest(error.getId());
        if (paymentNoticeGenerationRequest.getStatus().equals(PaymentGenerationRequestStatus.COMPLETING)) {
            return CompressionEvent
                    .builder()
                    .id(paymentNoticeGenerationRequest.getId())
                    .numberOfElementsTotal(paymentNoticeGenerationRequest
                            .getNumberOfElementsTotal())
                    .numberOfElementsFailed(paymentNoticeGenerationRequest
                            .getNumberOfElementsFailed())
                    .status(paymentNoticeGenerationRequest.getStatus())
                    .userId(paymentNoticeGenerationRequest.getUserId())
                    .items(paymentNoticeGenerationRequest.getItems())
                    .build();
        }
        return null;

    }

    private GenerationEvent buildNoticeRetry(RetryEvent error) throws Aes256Exception, JsonProcessingException {
        String plainRequestData = Aes256Utils.decrypt(error.getData());
        GenerationEvent noticeRequestEH = ObjectMapperUtils.mapString(plainRequestData, GenerationEvent.class);
        noticeRequestEH.setErrorId(error.getId());
        return noticeRequestEH;
    }


}