
package it.gov.pagopa.print.payment.notice.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestErrorClient;
import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeGenerationRequestErrorClientImpl;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.print.payment.notice.functions.exception.Aes256Exception;
import it.gov.pagopa.print.payment.notice.functions.exception.PaymentNoticeManagementException;
import it.gov.pagopa.print.payment.notice.functions.exception.RequestRecoveryException;
import it.gov.pagopa.print.payment.notice.functions.model.notice.NoticeRequestEH;
import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
import it.gov.pagopa.print.payment.notice.functions.service.impl.NoticeFolderServiceImpl;
import it.gov.pagopa.print.payment.notice.functions.utils.Aes256Utils;
import it.gov.pagopa.print.payment.notice.functions.utils.ObjectMapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Azure Functions with Azure Queue trigger.
 */
public class ManageNoticeErrors {

    private final Logger logger = LoggerFactory.getLogger(ManageNoticeErrors.class);

    private final NoticeFolderService noticeFolderService;

    private final PaymentNoticeGenerationRequestErrorClient paymentNoticeGenerationRequestErrorClient;

    private final Integer maxRetriesOnErrors;

    public ManageNoticeErrors() {
        this.noticeFolderService = new NoticeFolderServiceImpl();
        this.maxRetriesOnErrors =  Integer.parseInt(System.getenv("MAX_RETRIES_ON_ERRORS"));
        this.paymentNoticeGenerationRequestErrorClient = PaymentNoticeGenerationRequestErrorClientImpl.getInstance();

    }

    ManageNoticeErrors(NoticeFolderService noticeFolderService,
                       Integer maxRetriesOnErrors,
                       PaymentNoticeGenerationRequestErrorClient paymentNoticeGenerationRequestErrorClient) {
        this.noticeFolderService = noticeFolderService;
        this.maxRetriesOnErrors = maxRetriesOnErrors;
        this.paymentNoticeGenerationRequestErrorClient = paymentNoticeGenerationRequestErrorClient;
    }

    /**
     * This function will be invoked when a EH trigger occurs
     *
     * The function will manage errors coming from the generation service, or compression function
     *
     * In both cases it will be checked if the error does exist, recovering the latest attempt, and updating
     * the number of attempts. Whenever the number of attempts exceeds the configured limit, no more actions
     * will follow.
     *
     * The retries will be sent on the dedicated channel for the specific type of error managed
     */
    @FunctionName("ManageNoticeErrorsProcess")
    public void processNoticeErrors(
            @EventHubTrigger(
                    name = "PaymentNoticeErrors",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "NOTICE_ERR_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError> paymentNoticeErrors,
            @EventHubOutput(
                    name = "PaymentNoticeRequest",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "NOTICE_EVENTHUB_CONN_STRING")
            List<NoticeRequestEH> noticesToRetry,
            @EventHubOutput(
                    name = "PaymentNoticeComplete",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "NOTICE_COMPLETE_EVENTHUB_CONN_STRING")
            List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest> completionToRetry,
            final ExecutionContext context) {

        paymentNoticeErrors.forEach(error -> {

            PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError = null;
            try {
                paymentNoticeGenerationRequestError =
                        paymentNoticeGenerationRequestErrorClient.findOne(error.getFolderId()).orElseThrow(() ->
                                new PaymentNoticeManagementException("Request error not found",
                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
            } catch (Exception e) {
                logger.error("[{}] error recovering error data with id {}",
                        context.getFunctionName(), error.getFolderId(), e);
            }

            if (paymentNoticeGenerationRequestError != null &&
                    error.getNumberOfAttempts() < maxRetriesOnErrors) {

                if (error.isCompressionError() &&
                        !"UNKNOWN".equals(paymentNoticeGenerationRequestError.getFolderId())) {
                    try {
                        PaymentNoticeGenerationRequest paymentNoticeGenerationRequest =
                                noticeFolderService.findRequest(error.getId());
                        if (paymentNoticeGenerationRequest.getStatus().equals(
                                PaymentGenerationRequestStatus.COMPLETING)) {
                            completionToRetry.add(
                                    it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest
                                            .builder()
                                    .id(paymentNoticeGenerationRequest.getId())
                                    .numberOfElementsProcessed(paymentNoticeGenerationRequest
                                            .getNumberOfElementsProcessed())
                                    .numberOfElementsTotal(paymentNoticeGenerationRequest
                                            .getNumberOfElementsTotal())
                                    .numberOfElementsFailed(paymentNoticeGenerationRequest
                                            .getNumberOfElementsFailed())
                                    .status(paymentNoticeGenerationRequest.getStatus())
                                    .userId(paymentNoticeGenerationRequest.getUserId())
                                    .items(paymentNoticeGenerationRequest.getItems())
                                    .build()
                            );
                        }
                    } catch (RequestRecoveryException e) {
                        logger.error("[{}] error recovering notice request with id {}",
                                context.getFunctionName(), paymentNoticeGenerationRequestError.getId(), e);
                    }
                } else {

                    try {
                        String plainRequestData = Aes256Utils.decrypt(error.getData());
                        NoticeRequestEH noticeRequestEH =
                                ObjectMapperUtils.mapString(plainRequestData, NoticeRequestEH.class);
                        noticeRequestEH.setErrorId(error.getId());
                        noticesToRetry.add(noticeRequestEH);
                    } catch (Aes256Exception | JsonProcessingException e) {
                        logger.error("[{}] error recovering error data with id {}",
                                context.getFunctionName(), paymentNoticeGenerationRequestError.getId(), e);
                        throw new RuntimeException(e);
                    }

                }

                try {
                    paymentNoticeGenerationRequestErrorClient.updatePaymentGenerationRequestError(
                            paymentNoticeGenerationRequestError);
                } catch (Exception e) {
                    logger.error("[{}] error recovering error data with id {}",
                            context.getFunctionName(), paymentNoticeGenerationRequestError.getId(), e);
                }

            }

        });

    }

}
