package it.gov.pagopa.print.payment.notice.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestErrorClient;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.print.payment.notice.functions.exception.RequestRecoveryException;
import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;
import it.gov.pagopa.print.payment.notice.functions.model.notice.NoticeGenerationRequestItem;
import it.gov.pagopa.print.payment.notice.functions.model.notice.NoticeRequestEH;
import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
import it.gov.pagopa.print.payment.notice.functions.utils.Aes256Utils;
import it.gov.pagopa.print.payment.notice.functions.utils.ObjectMapperUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

@ExtendWith(MockitoExtension.class)
class ManageNoticeErrorsTest {

    @Mock
    ExecutionContext executionContextMock;

    @Mock
    NoticeFolderService noticeFolderService;

    @Mock
    PaymentNoticeGenerationRequestErrorClient paymentNoticeGenerationRequestErrorClient;

    ManageNoticeErrors sut;

    @BeforeEach
    public void init() {
        Mockito.reset(noticeFolderService);
        sut = new ManageNoticeErrors(
                noticeFolderService, 1, paymentNoticeGenerationRequestErrorClient);
    }

    @Test
    void shouldSendRequestOnValidData() throws SaveNoticeToBlobException, RequestRecoveryException {

        OutputBinding<List<NoticeRequestEH>> eventsToRetry =
                (OutputBinding<List<NoticeRequestEH>>) mock(OutputBinding.class);
        OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>>
                paymentNoticeGenerationRequestList =
                (OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>>)
                        mock(OutputBinding.class);

        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>
                paymentNoticeGenerationRequestErrors =
               Collections.singletonList(it.gov.pagopa.print.payment.notice.functions.model
                       .PaymentNoticeGenerationRequestError.builder().numberOfAttempts(0)
                               .compressionError(true)
                               .id("test")
                               .folderId("test")
                       .build());
        doReturn(Optional.of(PaymentNoticeGenerationRequestError.builder().compressionError(true)
                        .folderId("test")
                        .numberOfAttempts(0)
                        .compressionError(true)
                .build()))
                .when(paymentNoticeGenerationRequestErrorClient)
                .findOne(any());
        doReturn(PaymentNoticeGenerationRequest.builder()
                .status(PaymentGenerationRequestStatus.COMPLETING)
                .numberOfElementsTotal(2).numberOfElementsFailed(1).build())
                .when(noticeFolderService).findRequest(any());
        assertDoesNotThrow(() -> sut.processNoticeErrors(
                paymentNoticeGenerationRequestErrors,
                eventsToRetry,
                paymentNoticeGenerationRequestList,
                executionContextMock));
        verify(noticeFolderService).findRequest(any());
    }

    @Test
    void shouldSendNoticeOnValidData() throws Exception {
        withEnvironmentVariables(
                "AES_SECRET_KEY", "test",
                "AES_SALT", "test"
        ).execute(() -> {


            List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>
                    paymentNoticeGenerationRequestErrors = Collections.singletonList(
                            it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError
                                    .builder().numberOfAttempts(0)
                            .compressionError(false)
                            .id("test")
                            .folderId("test")
                                    .data(Aes256Utils.encrypt(ObjectMapperUtils.writeValueAsString(
                                            NoticeRequestEH.builder().folderId("folder")
                                                    .noticeData(NoticeGenerationRequestItem.builder().build())
                                                    .build())))
                            .build());

            OutputBinding<List<NoticeRequestEH>> eventsToRetry =
                    (OutputBinding<List<NoticeRequestEH>>) mock(OutputBinding.class);
            OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>>
                    paymentNoticeGenerationRequestList =
                    (OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>>)
                            mock(OutputBinding.class);

            doReturn(Optional.of(PaymentNoticeGenerationRequestError.builder().compressionError(true)
                    .folderId("test")
                    .id("test")
                    .numberOfAttempts(0)
                    .compressionError(false)
                    .build()))
                    .when(paymentNoticeGenerationRequestErrorClient)
                    .findOne(any());

            assertDoesNotThrow(() -> sut.processNoticeErrors(paymentNoticeGenerationRequestErrors,
                    eventsToRetry,
                    paymentNoticeGenerationRequestList,
                    executionContextMock));

        });


    }


    @Test
    void shouldntSendRequestOnMissingData() throws SaveNoticeToBlobException, RequestRecoveryException {
        OutputBinding<List<NoticeRequestEH>> eventsToRetry =
                (OutputBinding<List<NoticeRequestEH>>) mock(OutputBinding.class);
        OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>>
                paymentNoticeGenerationRequestList =
                (OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>>)
                        mock(OutputBinding.class);
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>
                paymentNoticeGenerationRequestErrors =
                Collections.singletonList(it.gov.pagopa.print.payment.notice.functions.model
                        .PaymentNoticeGenerationRequestError.builder().numberOfAttempts(0)
                        .compressionError(true)
                        .id("test")
                        .folderId("test")
                        .build());
        doReturn(Optional.ofNullable(null))
                .when(paymentNoticeGenerationRequestErrorClient)
                .findOne(any());
        assertDoesNotThrow(() -> sut.processNoticeErrors(
                paymentNoticeGenerationRequestErrors,
                eventsToRetry,
                paymentNoticeGenerationRequestList,
                executionContextMock));
    }

    @Test
    void shouldntSendRequestOnException() throws SaveNoticeToBlobException, RequestRecoveryException {
        OutputBinding<List<NoticeRequestEH>> eventsToRetry =
                (OutputBinding<List<NoticeRequestEH>>) mock(OutputBinding.class);
        OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>>
                paymentNoticeGenerationRequestList =
                (OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>>)
                        mock(OutputBinding.class);
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>
                paymentNoticeGenerationRequestErrors =
                Collections.singletonList(it.gov.pagopa.print.payment.notice.functions.model
                        .PaymentNoticeGenerationRequestError.builder().numberOfAttempts(0)
                        .compressionError(true)
                        .id("test")
                        .folderId("test")
                        .build());
        doReturn(Optional.of(PaymentNoticeGenerationRequestError.builder().compressionError(true)
                .folderId("test")
                .id("test")
                .numberOfAttempts(0)
                .compressionError(true)
                .build()))
                .when(paymentNoticeGenerationRequestErrorClient)
                .findOne(any());
        doAnswer(item -> {throw new RequestRecoveryException("test",500);}).when(noticeFolderService).findRequest(any());
        assertDoesNotThrow(() -> sut.processNoticeErrors(
                paymentNoticeGenerationRequestErrors,
                eventsToRetry,
                paymentNoticeGenerationRequestList,
                executionContextMock));
        verify(noticeFolderService).findRequest(any());
    }

}
