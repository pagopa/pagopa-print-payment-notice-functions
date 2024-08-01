package it.gov.pagopa.print.payment.notice.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestEH;
import it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestErrorEH;
import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
import it.gov.pagopa.print.payment.notice.functions.service.impl.NoticeFolderServiceImpl;
import it.gov.pagopa.print.payment.notice.functions.utils.ObjectMapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;

/**
 * Azure Functions with Azure Queue trigger.
 */
public class ManagePaymentNoticeFolderUpdates {

    private final Logger logger = LoggerFactory.getLogger(ManagePaymentNoticeFolderUpdates.class);

    private final NoticeFolderService noticeFolderService;

    public ManagePaymentNoticeFolderUpdates() {
        this.noticeFolderService = new NoticeFolderServiceImpl();
    }

    ManagePaymentNoticeFolderUpdates(NoticeFolderService noticeFolderService) {
        this.noticeFolderService = noticeFolderService;
    }

    /**
     * This function will be invoked when a EH trigger occurs
     * <p>
     * The function handles requests coming through the provided EH channel,
     * whenever a request is sent in status 'COMPLETING' it will check if the
     * number of elements are considered to be processed
     * <p>
     * The function will attempt to retrieve the folder notices, compressing and
     * saving on the folder within the blob storage
     * <p>
     * If the folder is successfully compressed the status will be saved
     * as PROCESSED, or PROCESSED_WITH_FAILURES if the folder is a partial
     * <p>
     * In case of errors a new element will be sent on the error channel
     */
    @FunctionName("ManagePaymentNoticeFolderUpdatesProcess")
    public void processGenerateReceipt(
            @EventHubTrigger(
                    name = "PaymentNoticeComplete",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "NOTICE_COMPLETE_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<PaymentNoticeGenerationRequestEH> paymentNoticeComplete,
            @EventHubOutput(
                    name = "PaymentNoticeErrors",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "NOTICE_ERR_EVENTHUB_CONN_STRING")
            OutputBinding<
                    List<PaymentNoticeGenerationRequestErrorEH>
                    > errors,
            final ExecutionContext context) throws JsonProcessingException {
        logger.info("[{}] Starting completing function {}", context.getFunctionName(), ObjectMapperUtils.writeValueAsString(paymentNoticeComplete));

        var errorMsgList = new ArrayList<PaymentNoticeGenerationRequestErrorEH>();

        paymentNoticeComplete.stream()
                .filter(item -> item.getStatus().equals(PaymentGenerationRequestStatus.COMPLETING) &&
                        (item.getItems().size() + item.getNumberOfElementsFailed() >= item.getNumberOfElementsTotal())
                ).forEach(item -> {
                    try {
                        MDC.put("folderId", item.getId());
                        noticeFolderService.manageFolder(
                                PaymentNoticeGenerationRequest.builder()
                                        .id(item.getId())
                                        .userId(item.getUserId())
                                        .numberOfElementsFailed(item.getNumberOfElementsFailed())
                                        .numberOfElementsTotal(item.getNumberOfElementsTotal())
                                        .items(item.getItems())
                                        .status(item.getStatus())
                                        .build());
                    } catch (Exception e) {
                        logger.error("[{}] error managing notice request with id {}", context.getFunctionName(), item.getId(), e);
                        errorMsgList.add(PaymentNoticeGenerationRequestErrorEH.builder()
                                .folderId(item.getId())
                                .errorId(item.getId())
                                .numberOfAttempts(0)
                                .compressionError(true)
                                .build());
                    }

                    errors.setValue(errorMsgList);
                });

        logger.info("[{}] Done! {}", context.getFunctionName(), ObjectMapperUtils.writeValueAsString(paymentNoticeComplete));


    }

}

