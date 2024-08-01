//package it.gov.pagopa.print.payment.notice.functions;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.microsoft.azure.functions.ExecutionContext;
//import com.microsoft.azure.functions.HttpStatus;
//import com.microsoft.azure.functions.OutputBinding;
//import com.microsoft.azure.functions.annotation.Cardinality;
//import com.microsoft.azure.functions.annotation.EventHubOutput;
//import com.microsoft.azure.functions.annotation.EventHubTrigger;
//import com.microsoft.azure.functions.annotation.FunctionName;
//import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestErrorMongoClient;
//import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeGenerationRequestErrorMongoClientImpl;
//import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
//import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
//import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
//import it.gov.pagopa.print.payment.notice.functions.exception.Aes256Exception;
//import it.gov.pagopa.print.payment.notice.functions.exception.PaymentNoticeManagementException;
//import it.gov.pagopa.print.payment.notice.functions.exception.RequestRecoveryException;
//import it.gov.pagopa.print.payment.notice.functions.events.model.PaymentNoticeGenerationRequestEH;
//import it.gov.pagopa.print.payment.notice.functions.events.model.PaymentNoticeGenerationRequestErrorEH;
//import it.gov.pagopa.print.payment.notice.functions.events.model.NoticeRequestEH;
//import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
//import it.gov.pagopa.print.payment.notice.functions.service.impl.NoticeFolderServiceImpl;
//import it.gov.pagopa.print.payment.notice.functions.utils.Aes256Utils;
//import it.gov.pagopa.print.payment.notice.functions.utils.ObjectMapperUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Azure Functions with Azure Queue trigger.
// */
//public class ManageNoticeErrors {
//
//    private final Logger logger = LoggerFactory.getLogger(ManageNoticeErrors.class);
//
//    private final NoticeFolderService noticeFolderService;
//
//    private final PaymentNoticeGenerationRequestErrorMongoClient paymentNoticeGenerationRequestErrorMongoClient;
//
//    private final Integer maxRetriesOnErrors;
//
//    private final String ERROR_STRING = "[{}] error recovering error data with id {}";
//
//    public ManageNoticeErrors() {
//        this.noticeFolderService = new NoticeFolderServiceImpl();
//        this.maxRetriesOnErrors = Integer.parseInt(System.getenv("MAX_RETRIES_ON_ERRORS"));
//        this.paymentNoticeGenerationRequestErrorMongoClient = PaymentNoticeGenerationRequestErrorMongoClientImpl.getInstance();
//
//    }
//
//    ManageNoticeErrors(NoticeFolderService noticeFolderService,
//                       Integer maxRetriesOnErrors,
//                       PaymentNoticeGenerationRequestErrorMongoClient paymentNoticeGenerationRequestErrorMongoClient) {
//        this.noticeFolderService = noticeFolderService;
//        this.maxRetriesOnErrors = maxRetriesOnErrors;
//        this.paymentNoticeGenerationRequestErrorMongoClient = paymentNoticeGenerationRequestErrorMongoClient;
//    }
//
//    /**
//     * This function will be invoked when a EH trigger occurs
//     * <p>
//     * The function will manage errors coming from the generation service, or compression function
//     * <p>
//     * In both cases it will be checked if the error does exist, recovering the latest attempt, and updating
//     * the number of attempts. Whenever the number of attempts exceeds the configured limit, no more actions
//     * will follow.
//     * <p>
//     * The retries will be sent on the dedicated channel for the specific type of error managed
//     */
//    @FunctionName("ManageNoticeErrorsProcess")
//    public void processNoticeErrors(
//            @EventHubTrigger(
//                    name = "PaymentNoticeErrors",
//                    eventHubName = "", // blank because the value is included in the connection string
//                    connection = "NOTICE_ERR_EVENTHUB_CONN_STRING",
//                    cardinality = Cardinality.MANY)
//            List<PaymentNoticeGenerationRequestErrorEH> paymentNoticeErrors,
//            @EventHubOutput(
//                    name = "PaymentNoticeRequest",
//                    eventHubName = "", // blank because the value is included in the connection string
//                    connection = "NOTICE_EVENTHUB_CONN_STRING")
//            OutputBinding<List<NoticeRequestEH>> noticesToRetry,
//            @EventHubOutput(
//                    name = "PaymentNoticeComplete",
//                    eventHubName = "", // blank because the value is included in the connection string
//                    connection = "NOTICE_COMPLETE_EVENTHUB_CONN_STRING")
//            OutputBinding<List<PaymentNoticeGenerationRequestEH>> completionToRetry,
//            final ExecutionContext context) throws JsonProcessingException {
//
//        logger.info("[{}] Starting Retry function {}", context.getFunctionName(), ObjectMapperUtils.writeValueAsString(paymentNoticeErrors));
//
//        List<PaymentNoticeGenerationRequestEH> paymentNoticeGenerationRequestList = new ArrayList<>();
//        List<NoticeRequestEH> noticeRequestEHS = new ArrayList<>();
//
//        paymentNoticeErrors.forEach(error -> {
//
//            PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError = null;
//            try {
//                MDC.put("folderId", error.getFolderId());
//                if (error.getId() != null) {
//
//                    paymentNoticeGenerationRequestError =
//                            paymentNoticeGenerationRequestErrorMongoClient.findOne(error.getId())
//                                    .orElseThrow(() -> new PaymentNoticeManagementException("Request error not found",
//                                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
//                } else {
//                    paymentNoticeGenerationRequestError = PaymentNoticeGenerationRequestError.builder()
//                            .folderId(error.getFolderId())
//                            .errorId(error.getErrorId())
//                            .numberOfAttempts(error.getNumberOfAttempts())
//                            .compressionError(error.isCompressionError())
//                            .data(error.getData())
//                            .errorCode(error.getErrorCode())
//                            .errorDescription(error.getErrorDescription())
//                            .build();
//                    paymentNoticeGenerationRequestError.setId(
//                            paymentNoticeGenerationRequestErrorMongoClient.save(paymentNoticeGenerationRequestError));
//
//                }
//            } catch (Exception e) {
//                logger.error(ERROR_STRING, context.getFunctionName(), error.getFolderId(), e);
//            }
//            logger.info("[{}] Processing a new Retry Generation Event {}", context.getFunctionName(), error.getId());
//
//
//            if (paymentNoticeGenerationRequestError != null &&
//                    error.getNumberOfAttempts() < maxRetriesOnErrors) {
//
//                if (error.isCompressionError() &&
//                        !"UNKNOWN".equals(paymentNoticeGenerationRequestError.getFolderId())) {
//                    addRequestsToRetry(paymentNoticeGenerationRequestList, context, error, paymentNoticeGenerationRequestError);
//                    logger.info("[{}] Retry Generation Event", context.getFunctionName());
//                } else {
//                    logger.info("[{}] Retry Massive Request", context.getFunctionName());
//                    addToNoticesToRetry(noticeRequestEHS, context, error, paymentNoticeGenerationRequestError);
//                }
//
//                updateError(context, paymentNoticeGenerationRequestError);
//
//            }
//
//        });
//
//        noticesToRetry.setValue(noticeRequestEHS);
//        completionToRetry.setValue(paymentNoticeGenerationRequestList);
//
//    }
//
//    private void updateError(ExecutionContext context, PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
//        try {
//            paymentNoticeGenerationRequestErrorMongoClient.updatePaymentGenerationRequestError(
//                    paymentNoticeGenerationRequestError);
//        } catch (Exception e) {
//            logger.error(ERROR_STRING,
//                    context.getFunctionName(), paymentNoticeGenerationRequestError.getId(), e);
//        }
//    }
//
//    private void addRequestsToRetry(List<PaymentNoticeGenerationRequestEH> completionToRetry, ExecutionContext context, PaymentNoticeGenerationRequestErrorEH error, PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
//        try {
//            PaymentNoticeGenerationRequest paymentNoticeGenerationRequest =
//                    noticeFolderService.findRequest(error.getId());
//            if (paymentNoticeGenerationRequest.getStatus().equals(
//                    PaymentGenerationRequestStatus.COMPLETING)) {
//                completionToRetry.add(
//                        PaymentNoticeGenerationRequestEH
//                                .builder()
//                                .id(paymentNoticeGenerationRequest.getId())
//                                .numberOfElementsTotal(paymentNoticeGenerationRequest
//                                        .getNumberOfElementsTotal())
//                                .numberOfElementsFailed(paymentNoticeGenerationRequest
//                                        .getNumberOfElementsFailed())
//                                .status(paymentNoticeGenerationRequest.getStatus())
//                                .userId(paymentNoticeGenerationRequest.getUserId())
//                                .items(paymentNoticeGenerationRequest.getItems())
//                                .build()
//                );
//            }
//        } catch (RequestRecoveryException e) {
//            logger.error(ERROR_STRING,
//                    context.getFunctionName(), paymentNoticeGenerationRequestError.getId(), e);
//        }
//    }
//
//    private void addToNoticesToRetry(List<NoticeRequestEH> noticesToRetry, ExecutionContext context, PaymentNoticeGenerationRequestErrorEH error, PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
//        try {
//            String plainRequestData = Aes256Utils.decrypt(error.getData());
//            NoticeRequestEH noticeRequestEH =
//                    ObjectMapperUtils.mapString(plainRequestData, NoticeRequestEH.class);
//            noticeRequestEH.setErrorId(error.getId());
//            noticesToRetry.add(noticeRequestEH);
//        } catch (Aes256Exception | JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//}
