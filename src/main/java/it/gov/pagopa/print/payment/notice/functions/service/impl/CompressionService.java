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
                noticeFolderService.manageFolder(
                        PaymentNoticeGenerationRequest.builder()
                                .id(compressionMessage.getId())
                                .userId(compressionMessage.getUserId())
                                .numberOfElementsFailed(compressionMessage.getNumberOfElementsFailed())
                                .numberOfElementsTotal(compressionMessage.getNumberOfElementsTotal())
                                .items(compressionMessage.getItems())
                                .status(compressionMessage.getStatus())
                                .build());
            } catch (Exception e) {
                var errorMsg = ErrorEvent.builder()
                        .folderId(compressionMessage.getId())
                        .errorId(compressionMessage.getId())
                        .numberOfAttempts(0)
                        .compressionError(true)
                        .build();
                noticeRequestErrorProducer.sendErrorEvent(errorMsg);
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

