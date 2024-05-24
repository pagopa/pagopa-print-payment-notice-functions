package it.gov.pagopa.print.payment.notice.functions;

import com.microsoft.azure.functions.ExecutionContext;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeGenerationRequestErrorClient;
import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeGenerationRequestClientImpl;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

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
        List<PaymentNoticeGenerationRequest> paymentNoticeGenerationRequestList =
                new ArrayList<>();
        List<PaymentNoticeGenerationRequestError> paymentNoticeGenerationRequestErrors =
               Collections.singletonList(PaymentNoticeGenerationRequestError.builder().numberOfAttempts(0)
                               .compressionError(true)
                               .id("test")
                               .folderId("test")
                       .build());
        doReturn(Optional.of(paymentNoticeGenerationRequestErrors.get(0)))
                .when(paymentNoticeGenerationRequestErrorClient)
                .findOne(any());
        doReturn(PaymentNoticeGenerationRequest.builder()
                .status(PaymentGenerationRequestStatus.COMPLETING).numberOfElementsProcessed(1)
                .numberOfElementsTotal(2).numberOfElementsFailed(1).build())
                .when(noticeFolderService).findRequest(any());
        List<NoticeRequestEH> eventsToRetry = new ArrayList<>();
        assertDoesNotThrow(() -> sut.processNoticeErrors(paymentNoticeGenerationRequestErrors,
                new HashMap[]{new HashMap<>()},
                eventsToRetry,
                paymentNoticeGenerationRequestList,
                executionContextMock));
        verify(noticeFolderService).findRequest(any());
        assertEquals(0, eventsToRetry.size());
        assertEquals(1, paymentNoticeGenerationRequestList.size());
    }

    @Test
    void shouldSendNoticeOnValidData() throws Exception {
        withEnvironmentVariables(
                "AES_SECRET_KEY", "test",
                "AES_SALT", "test"
        ).execute(() -> {

            List<PaymentNoticeGenerationRequest> paymentNoticeGenerationRequestList =
                    new ArrayList<>();
            List<PaymentNoticeGenerationRequestError> paymentNoticeGenerationRequestErrors =
                    Collections.singletonList(PaymentNoticeGenerationRequestError.builder().numberOfAttempts(0)
                            .compressionError(false)
                            .id("test")
                            .folderId("test")
                                    .data(Aes256Utils.encrypt(ObjectMapperUtils.writeValueAsString(
                                            NoticeRequestEH.builder().folderId("folder")
                                                    .noticeData(NoticeGenerationRequestItem.builder().build())
                                                    .build())))
                            .build());
            List<NoticeRequestEH> eventsToRetry = new ArrayList<>();

            doReturn(Optional.of(paymentNoticeGenerationRequestErrors.get(0)))
                    .when(paymentNoticeGenerationRequestErrorClient)
                    .findOne(any());

            assertDoesNotThrow(() -> sut.processNoticeErrors(paymentNoticeGenerationRequestErrors,
                    new HashMap[]{new HashMap<>()},
                    eventsToRetry,
                    paymentNoticeGenerationRequestList,
                    executionContextMock));
            assertEquals(1, eventsToRetry.size());
            assertEquals(0, paymentNoticeGenerationRequestList.size());

        });


    }




}
