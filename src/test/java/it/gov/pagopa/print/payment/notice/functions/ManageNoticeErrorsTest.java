//package it.gov.pagopa.print.payment.notice.functions;
//
//import com.microsoft.azure.functions.ExecutionContext;
//import com.microsoft.azure.functions.OutputBinding;
//import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestErrorMongoClient;
//import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
//import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
//import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
//import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
//import it.gov.pagopa.print.payment.notice.functions.events.model.RetryEvent;
//import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
//import it.gov.pagopa.print.payment.notice.functions.exception.RequestRecoveryException;
//import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;
//import it.gov.pagopa.print.payment.notice.functions.model.notice.NoticeGenerationRequestItem;
//import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
//import it.gov.pagopa.print.payment.notice.functions.utils.Aes256Utils;
//import it.gov.pagopa.print.payment.notice.functions.utils.ObjectMapperUtils;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;
//
//@ExtendWith(MockitoExtension.class)
//class ManageNoticeErrorsTest {
//
//    @Mock
//    ExecutionContext executionContextMock;
//
//    @Mock
//    NoticeFolderService noticeFolderService;
//
//    @Mock
//    PaymentNoticeGenerationRequestErrorMongoClient paymentNoticeGenerationRequestErrorMongoClient;
//
//    ManageNoticeErrors sut;
//
//    @BeforeEach
//    public void init() {
//        Mockito.reset(noticeFolderService);
//        sut = new ManageNoticeErrors(
//                noticeFolderService, 1, paymentNoticeGenerationRequestErrorMongoClient);
//    }
//
//    @Test
//    void shouldSendRequestOnValidData() throws SaveNoticeToBlobException, RequestRecoveryException {
//
//        OutputBinding<List<GenerationEvent>> eventsToRetry =
//                (OutputBinding<List<GenerationEvent>>) mock(OutputBinding.class);
//        OutputBinding<List<CompressionEvent>>
//                paymentNoticeGenerationRequestList =
//                (OutputBinding<List<CompressionEvent>>)
//                        mock(OutputBinding.class);
//
//        List<RetryEvent>
//                retryErrorEHS =
//                Collections.singletonList(RetryEvent.builder().numberOfAttempts(0)
//                        .compressionError(true)
//                        .id("test")
//                        .folderId("test")
//                        .build());
//        doReturn(Optional.of(PaymentNoticeGenerationRequestError.builder().compressionError(true)
//                .folderId("test")
//                .numberOfAttempts(0)
//                .compressionError(true)
//                .build()))
//                .when(paymentNoticeGenerationRequestErrorMongoClient)
//                .findOne(any());
//        doReturn(PaymentNoticeGenerationRequest.builder()
//                .status(PaymentGenerationRequestStatus.COMPLETING)
//                .numberOfElementsTotal(2).numberOfElementsFailed(1).build())
//                .when(noticeFolderService).findRequest(any());
//        assertDoesNotThrow(() -> sut.processNoticeErrors(
//                retryErrorEHS,
//                eventsToRetry,
//                paymentNoticeGenerationRequestList,
//                executionContextMock));
//        verify(noticeFolderService).findRequest(any());
//    }
//
//    @Test
//    void shouldSendNoticeOnValidData() throws Exception {
//        withEnvironmentVariables(
//                "AES_SECRET_KEY", "test",
//                "AES_SALT", "test"
//        ).execute(() -> {
//
//
//            List<RetryEvent>
//                    retryErrorEHS = Collections.singletonList(
//                    RetryEvent
//                            .builder().numberOfAttempts(0)
//                            .compressionError(false)
//                            .id("test")
//                            .folderId("test")
//                            .data(Aes256Utils.encrypt(ObjectMapperUtils.writeValueAsString(
//                                    GenerationEvent.builder().folderId("folder")
//                                            .noticeData(NoticeGenerationRequestItem.builder().build())
//                                            .build())))
//                            .build());
//
//            OutputBinding<List<GenerationEvent>> eventsToRetry =
//                    (OutputBinding<List<GenerationEvent>>) mock(OutputBinding.class);
//            OutputBinding<List<CompressionEvent>>
//                    paymentNoticeGenerationRequestList =
//                    (OutputBinding<List<CompressionEvent>>)
//                            mock(OutputBinding.class);
//
//            doReturn(Optional.of(PaymentNoticeGenerationRequestError.builder().compressionError(true)
//                    .folderId("test")
//                    .id("test")
//                    .numberOfAttempts(0)
//                    .compressionError(false)
//                    .build()))
//                    .when(paymentNoticeGenerationRequestErrorMongoClient)
//                    .findOne(any());
//
//            assertDoesNotThrow(() -> sut.processNoticeErrors(retryErrorEHS,
//                    eventsToRetry,
//                    paymentNoticeGenerationRequestList,
//                    executionContextMock));
//
//        });
//
//
//    }
//
//
//    @Test
//    void shouldntSendRequestOnMissingData() throws SaveNoticeToBlobException, RequestRecoveryException {
//        OutputBinding<List<GenerationEvent>> eventsToRetry =
//                (OutputBinding<List<GenerationEvent>>) mock(OutputBinding.class);
//        OutputBinding<List<CompressionEvent>>
//                paymentNoticeGenerationRequestList =
//                (OutputBinding<List<CompressionEvent>>)
//                        mock(OutputBinding.class);
//        List<RetryEvent>
//                retryEvents =
//                Collections.singletonList(RetryEvent.builder().numberOfAttempts(0)
//                        .compressionError(true)
//                        .id("test")
//                        .folderId("test")
//                        .build());
//        doReturn(Optional.ofNullable(null))
//                .when(paymentNoticeGenerationRequestErrorMongoClient)
//                .findOne(any());
//        assertDoesNotThrow(() -> sut.processNoticeErrors(
//                retryEvents,
//                eventsToRetry,
//                paymentNoticeGenerationRequestList,
//                executionContextMock));
//    }
//
//    @Test
//    void shouldntSendRequestOnException() throws SaveNoticeToBlobException, RequestRecoveryException {
//        OutputBinding<List<GenerationEvent>> eventsToRetry =
//                (OutputBinding<List<GenerationEvent>>) mock(OutputBinding.class);
//        OutputBinding<List<CompressionEvent>>
//                paymentNoticeGenerationRequestList =
//                (OutputBinding<List<CompressionEvent>>)
//                        mock(OutputBinding.class);
//        List<RetryEvent>
//                retryEvents =
//                Collections.singletonList(RetryEvent.builder().numberOfAttempts(0)
//                        .compressionError(true)
//                        .id("test")
//                        .folderId("test")
//                        .build());
//        doReturn(Optional.of(PaymentNoticeGenerationRequestError.builder().compressionError(true)
//                .folderId("test")
//                .id("test")
//                .numberOfAttempts(0)
//                .compressionError(true)
//                .build()))
//                .when(paymentNoticeGenerationRequestErrorMongoClient)
//                .findOne(any());
//        doAnswer(item -> {
//            throw new RequestRecoveryException("test", 500);
//        }).when(noticeFolderService).findRequest(any());
//        assertDoesNotThrow(() -> sut.processNoticeErrors(
//                retryEvents,
//                eventsToRetry,
//                paymentNoticeGenerationRequestList,
//                executionContextMock));
//        verify(noticeFolderService).findRequest(any());
//    }
//
//}
