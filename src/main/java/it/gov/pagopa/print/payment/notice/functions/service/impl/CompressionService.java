package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.ErrorEvent;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeRequestErrorProducer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CompressionService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NoticeFolderService noticeFolderService;

    @Autowired
    private NoticeRequestErrorProducer noticeRequestErrorProducer;


    public void compressFolder(List<String> message) {
        try {
            log.info("{}", message);

            for (var elem : message) {
                var compressionMessage = objectMapper.readValue(elem, CompressionEvent.class);
                handleMessage(compressionMessage);
            }
        } catch (Exception e) {
            MDC.put("massiveStatus", "EXCEPTION");
            log.error("Massive Request EXCEPTION", e);
            MDC.remove("massiveStatus");
            throw new RuntimeException(e);
        }

    }

    private void handleMessage(CompressionEvent compressionMessage) {
        if (isCompleted(compressionMessage)) {
            MDC.clear();
            MDC.put("folderId", compressionMessage.getId());
            log.info("Starting Compress Function {}", compressionMessage);

            MDC.put("topic", "complete");
            MDC.put("action", "received");
            log.info("Received Complete Message");
            MDC.remove("topic");
            MDC.remove("action");

            try {
                /*
                 * The completion event may be stale or duplicated. Re-read the folder from
                 * Mongo before starting compression and use the persisted status as the source
                 * of truth.
                 */
                PaymentNoticeGenerationRequest currentRequest = noticeFolderService
                        .findRequest(compressionMessage.getId());

                if (!PaymentGenerationRequestStatus.COMPLETING.equals(currentRequest.getStatus())) {
                    log.info("Skipping compression because folder {} is no longer in COMPLETING status",
                            compressionMessage.getId());
                    return;
                }

                noticeFolderService.manageFolder(currentRequest);

            } catch (Exception e) {
                var errorMsg = ErrorEvent.builder().folderId(compressionMessage.getId())
                        .errorId(compressionMessage.getId()).numberOfAttempts(0).compressionError(true).build();

                boolean errorEventSent = noticeRequestErrorProducer.sendErrorEvent(errorMsg);

                if (!errorEventSent) {
                    /*
                     * The compression failed and the corresponding error event could not be
                     * published; propagate the failure.
                     */
                    throw new IllegalStateException("Unable to publish compression error event", e);
                }

                MDC.put("massiveStatus", "FAILED");
                log.error("Massive Request FAILED", e);
                MDC.remove("massiveStatus");
            }
        }
    }

    private boolean isCompleted(CompressionEvent compressionMessage) {
        return compressionMessage.getStatus().equals(PaymentGenerationRequestStatus.COMPLETING)
                && (compressionMessage.getItems().size() + compressionMessage.getNumberOfElementsFailed() >= compressionMessage.getNumberOfElementsTotal());
    }
}

