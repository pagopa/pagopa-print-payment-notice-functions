package it.gov.pagopa.print.payment.notice.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
import it.gov.pagopa.print.payment.notice.functions.service.impl.NoticeFolderServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * This function will be invoked when a Queue trigger occurs
     *
     * The function handles requests coming through the provided EH channel,
     * whenever a request is sent in status 'COMPLETING' it will check if the
     * number of elements are considered to be processed
     *
     * The function will attempt to retrieve the folder notices, compressing and
     * saving on the folder within the blob storage
     *
     * If the folder is successfully compressed the status will be saved
     * as PROCESSED, or PROCESSED_WITH_FAILURES if the folder is a partial
     *
     * In case of errors a new element will be sent on the error channel
     */
    @FunctionName("ManagePaymentNoticeFolderUpdatesProcess")
    public void processGenerateReceipt(
            @EventHubTrigger(
                    name = "PaymentNoticeRequest",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "NOTICE_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<PaymentNoticeGenerationRequest> requestMsg,
            @BindingName(value = "PropertiesArray") Map<String, Object>[] properties,
            @EventHubOutput(
                    name = "PaymentNoticeErrors",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "NOTICE_ERR_EVENTHUB_CONN_STRING")
            List<PaymentNoticeGenerationRequestError> errors,
            final ExecutionContext context) {

        requestMsg.stream().filter(item -> (
                Objects.equals(
                        item.getNumberOfElementsProcessed() + item.getNumberOfElementsFailed(),
                        item.getNumberOfElementsTotal()) &&
                        PaymentGenerationRequestStatus.COMPLETING.equals(item.getStatus()))
                )
                .forEach(item -> {
                    try {
                        noticeFolderService.manageFolder(item);
                    } catch (Exception e) {
                        logger.error("[{}] error managing notice rewuest with id {}",
                                context.getFunctionName(), item.getId(), e);
                        errors.add(PaymentNoticeGenerationRequestError.builder()
                                        .folderId(item.getId())
                                        .numberOfAttempts(0)
                                        .compressionError(true)
                                .build());
                    }
                });

    }

}
