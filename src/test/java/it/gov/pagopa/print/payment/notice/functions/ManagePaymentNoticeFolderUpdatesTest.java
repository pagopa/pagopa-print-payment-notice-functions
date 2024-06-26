package it.gov.pagopa.print.payment.notice.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;
import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagePaymentNoticeFolderUpdatesTest {

    @Mock
    ExecutionContext executionContextMock;

    @Mock
    NoticeFolderService noticeFolderService;

    ManagePaymentNoticeFolderUpdates sut;

    @BeforeEach
    public void init() {
        Mockito.reset(noticeFolderService);
        sut = new ManagePaymentNoticeFolderUpdates(noticeFolderService);
    }

    @Test
    void runOK() throws SaveNoticeToBlobException {
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>
                paymentNoticeGenerationRequestList =
                Collections.singletonList(
                        it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest.builder()
                        .status(PaymentGenerationRequestStatus.COMPLETING).items(Collections.singletonList("test"))
                        .numberOfElementsTotal(2).numberOfElementsFailed(1).build());
        OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>>
                paymentNoticeGenerationRequestErrors =
                (OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>>)
                        mock(OutputBinding.class);
        assertDoesNotThrow(() -> sut.processGenerateReceipt(paymentNoticeGenerationRequestList,
                paymentNoticeGenerationRequestErrors,executionContextMock));
        verify(noticeFolderService).manageFolder(any());
    }

    @Test
    void runShouldIgnoreUnfinishedElements() throws SaveNoticeToBlobException {
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>
                paymentNoticeGenerationRequestList =
                Collections.singletonList(
                        it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest.builder()
                        .status(PaymentGenerationRequestStatus.COMPLETING).numberOfElementsTotal(2)
                                .items(Collections.singletonList("test")).numberOfElementsFailed(0).build());
        OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>>
                paymentNoticeGenerationRequestErrors =
                (OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>>)
                        mock(OutputBinding.class);
        assertDoesNotThrow(() -> sut.processGenerateReceipt(paymentNoticeGenerationRequestList,
                paymentNoticeGenerationRequestErrors,executionContextMock));
        verifyNoInteractions(noticeFolderService);
    }

    @Test
    void runShouldSendErrorOnException() throws SaveNoticeToBlobException {
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>
                paymentNoticeGenerationRequestList =
                Collections.singletonList(it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest.builder()
                        .status(PaymentGenerationRequestStatus.COMPLETING).numberOfElementsTotal(2)
                        .items(Collections.singletonList("test")).numberOfElementsFailed(1).build());
        OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>>
                paymentNoticeGenerationRequestErrors =
                (OutputBinding<List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>>)
                        mock(OutputBinding.class);
        doAnswer(item -> {
            throw new SaveNoticeToBlobException("Error", 500);
        }).when(noticeFolderService).manageFolder(any());
        assertDoesNotThrow(() -> sut.processGenerateReceipt(paymentNoticeGenerationRequestList,
                paymentNoticeGenerationRequestErrors,executionContextMock));
        verify(noticeFolderService).manageFolder(any());
    }


}
