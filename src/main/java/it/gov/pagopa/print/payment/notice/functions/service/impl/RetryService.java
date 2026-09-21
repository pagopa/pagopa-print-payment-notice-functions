package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.ErrorEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeGenerationRequestProducer;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeRequestCompleteProducer;
import it.gov.pagopa.print.payment.notice.functions.exception.Aes256Exception;
import it.gov.pagopa.print.payment.notice.functions.exception.PaymentNoticeManagementException;
import it.gov.pagopa.print.payment.notice.functions.exception.RequestRecoveryException;
import it.gov.pagopa.print.payment.notice.functions.exception.RetryEventPublicationException;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.print.payment.notice.functions.utils.Aes256Utils;
import it.gov.pagopa.print.payment.notice.functions.utils.ObjectMapperUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RetryService {
    
    private static final String MDC_MASSIVE_STATUS = "massiveStatus";

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

    @Autowired
    private Aes256Utils aes256Utils;

    public void retryError(String message) {

        try {
            var retryMessage = objectMapper.readValue(message, ErrorEvent.class);

            MDC.put("folderId", retryMessage.getFolderId());
            log.info("Starting Retry Function {}", retryMessage);
            MDC.put("topic", "error");
            MDC.put("action", "received");
            log.info("Error Complete Message");
            MDC.remove("topic");
            MDC.remove("action");
            
            if (isCompressionError(retryMessage)) {
                // Before consuming a retry attempt, verify that the folder still needs compression.
                CompressionEvent compressionEvent = buildCompressionError(retryMessage);

                if (compressionEvent == null) {
                    log.info("Skipping compression retry because the folder is no longer in COMPLETING status");
                    return;
                }
                var paymentNoticeGenerationRequestError = findErrorOrCreate(retryMessage);
                if (paymentNoticeGenerationRequestError != null
                        && acquireRetryAttempt(paymentNoticeGenerationRequestError)) {

                    publishRetryOrReleaseAttempt(paymentNoticeGenerationRequestError,
                            () -> noticeRequestCompleteProducer.sendNoticeComplete(compressionEvent));
                    log.debug("Sent a new compression event");
                }

            } else {
                var paymentNoticeGenerationRequestError = findErrorOrCreate(retryMessage);
                if (paymentNoticeGenerationRequestError != null) {
                    GenerationEvent generationEvent = buildNoticeRetry(retryMessage);
                    if (acquireRetryAttempt(paymentNoticeGenerationRequestError)) {
                        publishRetryOrReleaseAttempt(paymentNoticeGenerationRequestError,
                                () -> noticeGenerationRequestProducer.sendGenerationEvent(generationEvent));
                        log.debug("Sent a new generation event");
                    }
                }
            }

        } catch (RetryEventPublicationException e) {
            MDC.put(MDC_MASSIVE_STATUS, "EXCEPTION");
            log.error("Retry Event Publication Error", e);
            MDC.remove(MDC_MASSIVE_STATUS);
            // The retry event could not be published; propagate the failure to the caller.
            throw e;
        } catch (Exception e) {
            MDC.put(MDC_MASSIVE_STATUS, "EXCEPTION");
            log.error("Retry Error", e);
            MDC.remove(MDC_MASSIVE_STATUS);
        }
    }

    /**
     * Atomically acquires the next retry attempt.
     *
     * Mongo is the source of truth for the retry counter. The update succeeds only
     * while numberOfAttempts is lower than the configured maximum, preventing
     * duplicate or concurrent deliveries from exceeding the retry limit.
     */
    private boolean acquireRetryAttempt(PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {

        long updated = paymentGenerationRequestErrorRepository
                .incrementNumberOfAttemptsIfBelowMax(paymentNoticeGenerationRequestError.getId(), maxRetriesOnErrors);

        if (updated == 0) {
            log.warn("Maximum number of retry attempts reached");
            return false;
        }

        log.debug("Acquired retry attempt");
        return true;
    }

    private boolean isCompressionError(ErrorEvent retryMessage) {
        return retryMessage.isCompressionError() && !"UNKNOWN".equals(retryMessage.getFolderId());
    }

    private PaymentNoticeGenerationRequestError findErrorOrCreate(ErrorEvent retryMessage)
            throws PaymentNoticeManagementException {

        if (retryMessage.getId() != null) {

            return paymentGenerationRequestErrorRepository.findById(retryMessage.getId())
                    .orElseThrow(() -> new PaymentNoticeManagementException("Request retryMessage not found",
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }

        /*
         * Reuse the error already associated with the folder instead of
         * creating a new record with numberOfAttempts reset to zero.
         */
        if (retryMessage.isCompressionError()) {

            var existingCompressionError = paymentGenerationRequestErrorRepository
                    .findTopByFolderIdAndCompressionErrorTrueOrderByNumberOfAttemptsDesc(retryMessage.getFolderId());

            if (existingCompressionError.isPresent()) {
                return existingCompressionError.get();
            }
        }

        PaymentNoticeGenerationRequestError newError = PaymentNoticeGenerationRequestError.builder()
                .folderId(retryMessage.getFolderId()).errorId(retryMessage.getErrorId())
                .numberOfAttempts(retryMessage.getNumberOfAttempts() != null ? retryMessage.getNumberOfAttempts() : 0)
                .compressionError(retryMessage.isCompressionError()).data(retryMessage.getData())
                .errorCode(retryMessage.getErrorCode()).errorDescription(retryMessage.getErrorDescription()).build();

        return paymentGenerationRequestErrorRepository.save(newError);
    }

    private CompressionEvent buildCompressionError(ErrorEvent error) throws RequestRecoveryException {

        /*
         * Compression errors are related to the massive-generation folder.
         * The folderId field is the one that identifies the request that must be compressed again.
         */
        PaymentNoticeGenerationRequest paymentNoticeGenerationRequest = noticeFolderService.findRequest(error.getFolderId());
        if (PaymentGenerationRequestStatus.COMPLETING.equals(paymentNoticeGenerationRequest.getStatus())) {
            return CompressionEvent.builder().id(paymentNoticeGenerationRequest.getId())
                    .numberOfElementsTotal(paymentNoticeGenerationRequest.getNumberOfElementsTotal())
                    .numberOfElementsFailed(paymentNoticeGenerationRequest.getNumberOfElementsFailed())
                    .status(paymentNoticeGenerationRequest.getStatus())
                    .userId(paymentNoticeGenerationRequest.getUserId()).items(paymentNoticeGenerationRequest.getItems())
                    .build();
        }
        return null;

    }

    private GenerationEvent buildNoticeRetry(ErrorEvent error) throws Aes256Exception, JsonProcessingException {
        String plainRequestData = aes256Utils.decrypt(error.getData());
        GenerationEvent noticeRequestEH = ObjectMapperUtils.mapString(plainRequestData, GenerationEvent.class);
        noticeRequestEH.setErrorId(error.getId());
        return noticeRequestEH;
    }
    
    /**
     * Publishes a retry event after a retry attempt has been acquired.
     *
     * If publication fails, the previously acquired attempt is released so that a
     * subsequent delivery can retry without consuming the configured limit.
     *
     * @param error     persisted error containing the retry counter
     * @param publisher operation used to publish the retry event
     * @throws RetryEventPublicationException if the retry event cannot be published
     */
    private void publishRetryOrReleaseAttempt(PaymentNoticeGenerationRequestError error, BooleanSupplier publisher) {
        boolean sent;
        try {
            sent = publisher.getAsBoolean();
        } catch (Exception e) {

            /*
             * The retry event was not successfully published. Release the retry slot
             * acquired immediately before this operation.
             */
            releaseRetryAttempt(error);
            throw new RetryEventPublicationException(e);
        }
        if (!sent) {
            /*
             * StreamBridge may report a failed send without throwing an exception. Release
             * the acquired retry slot in this case as well.
             */
            releaseRetryAttempt(error);
            throw new RetryEventPublicationException();
        }
    }
    
    /*
     * Compensates a previously acquired retry attempt.
     */
    private void releaseRetryAttempt(PaymentNoticeGenerationRequestError error) {
        long updated = paymentGenerationRequestErrorRepository
                .decrementNumberOfAttemptsIfGreaterThanZero(error.getId());
        if (updated == 0) {
            log.error("Unable to release acquired retry attempt");
        } else {
            log.debug("Released acquired retry attempt");
        }
    }
}