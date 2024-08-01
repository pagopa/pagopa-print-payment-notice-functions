package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.RetryEvent;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeRequestErrorProducer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CompressionService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NoticeFolderService noticeFolderService;

    @Autowired
    private NoticeRequestErrorProducer noticeRequestErrorProducer;

    public void compressFolder(String message) {
        try {
            var compressionMessage = objectMapper.readValue(message, CompressionEvent.class);

            if (isCompleted(compressionMessage)) {

                MDC.put("folderId", compressionMessage.getId());
                log.info("Starting Compress Function {}", compressionMessage);

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
                    var errorMsg = RetryEvent.builder()
                            .folderId(compressionMessage.getId())
                            .errorId(compressionMessage.getId())
                            .numberOfAttempts(0)
                            .compressionError(true)
                            .build();
                    noticeRequestErrorProducer.noticeError(errorMsg);
                    MDC.put("massiveStatus", "FAILED");
                    log.error("Massive Request FAILED", e);
                }

            }
        } catch (Exception e) {
            MDC.put("massiveStatus", "EXCEPTION");
            log.error("Massive Request EXCEPTION", e);
            throw new RuntimeException(e);
        }

    }

    private boolean isCompleted(CompressionEvent compressionMessage) {
        return compressionMessage.getStatus().equals(PaymentGenerationRequestStatus.COMPLETING)
                && (compressionMessage.getItems().size() + compressionMessage.getNumberOfElementsFailed() >= compressionMessage.getNumberOfElementsTotal());
    }
}

