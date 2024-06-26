
package it.gov.pagopa.print.payment.notice.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
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

import java.util.ArrayList;
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

    private String ERROR_STRING = "[{}] error recovering error data with id {}";

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
            OutputBinding<List<NoticeRequestEH>> noticesToRetry,
            @EventHubOutput(
                    name = "PaymentNoticeComplete",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "NOTICE_COMPLETE_EVENTHUB_CONN_STRING")
            OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model
                    .PaymentNoticeGenerationRequest>> completionToRetry,
            final ExecutionContext context) {

        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>
                paymentNoticeGenerationRequestList =
                new ArrayList<>();
        List<NoticeRequestEH>
                noticeRequestEHS =
                new ArrayList<>();

        paymentNoticeErrors.forEach(error -> {

            PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError = null;
                try {
                    if (error.getId() != null) {

                        paymentNoticeGenerationRequestError =
                                paymentNoticeGenerationRequestErrorClient.findOne(error.getId()).orElseThrow(() ->
                                        new PaymentNoticeManagementException("Request error not found",
                                                HttpStatus.INTERNAL_SERVER_ERROR.value()));
                    } else {
                        paymentNoticeGenerationRequestError = PaymentNoticeGenerationRequestError.builder()
                                .folderId(error.getFolderId())
                                .errorId(error.getErrorId())
                                .numberOfAttempts(error.getNumberOfAttempts())
                                .compressionError(error.isCompressionError())
                                .data(error.getData())
                                .errorCode(error.getErrorCode())
                                .errorDescription(error.getErrorDescription())
                                .build();
                        paymentNoticeGenerationRequestError.setId(
                                paymentNoticeGenerationRequestErrorClient.save(paymentNoticeGenerationRequestError));
                    }
                } catch (Exception e) {
                    logger.error(ERROR_STRING,
                            context.getFunctionName(), error.getFolderId(), e);
                }


            if (paymentNoticeGenerationRequestError != null &&
                    error.getNumberOfAttempts() < maxRetriesOnErrors) {

                if (error.isCompressionError() &&
                        !"UNKNOWN".equals(paymentNoticeGenerationRequestError.getFolderId())) {
                    addRequestsToRetry(paymentNoticeGenerationRequestList, context, error, paymentNoticeGenerationRequestError);
                } else {

                    addToNoticesToRetry(noticeRequestEHS, context, error, paymentNoticeGenerationRequestError);
                }

                updateError(context, paymentNoticeGenerationRequestError);

            }

        });

        noticesToRetry.setValue(noticeRequestEHS);
        completionToRetry.setValue(paymentNoticeGenerationRequestList);

    }

    private void updateError(ExecutionContext context, PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
        try {
            paymentNoticeGenerationRequestErrorClient.updatePaymentGenerationRequestError(
                    paymentNoticeGenerationRequestError);
        } catch (Exception e) {
            logger.error(ERROR_STRING,
                    context.getFunctionName(), paymentNoticeGenerationRequestError.getId(), e);
        }
    }

    private void addRequestsToRetry(List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest> completionToRetry, ExecutionContext context, it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError error, PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
        try {
            PaymentNoticeGenerationRequest paymentNoticeGenerationRequest =
                    noticeFolderService.findRequest(error.getId());
            if (paymentNoticeGenerationRequest.getStatus().equals(
                    PaymentGenerationRequestStatus.COMPLETING)) {
                completionToRetry.add(
                        it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest
                                .builder()
                        .id(paymentNoticeGenerationRequest.getId())
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
            logger.error(ERROR_STRING,
                    context.getFunctionName(), paymentNoticeGenerationRequestError.getId(), e);
        }
    }

    private void addToNoticesToRetry(List<NoticeRequestEH> noticesToRetry, ExecutionContext context, it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError error, PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
        try {
            String plainRequestData = Aes256Utils.decrypt(error.getData());
            NoticeRequestEH noticeRequestEH =
                    ObjectMapperUtils.mapString(plainRequestData, NoticeRequestEH.class);
            noticeRequestEH.setErrorId(error.getId());
            noticesToRetry.add(noticeRequestEH);
        } catch (Aes256Exception | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
