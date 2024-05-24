package it.gov.pagopa.print.payment.notice.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.exception.SaveNoticeToBlobException;
import it.gov.pagopa.print.payment.notice.functions.service.NoticeFolderService;
import it.gov.pagopa.print.payment.notice.functions.utils.ObjectMapperUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
                        .status(PaymentGenerationRequestStatus.COMPLETING).numberOfElementsProcessed(1)
                        .numberOfElementsTotal(2).numberOfElementsFailed(1).build());
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>
                paymentNoticeGenerationRequestErrors = new ArrayList<>();
        assertDoesNotThrow(() -> sut.processGenerateReceipt(paymentNoticeGenerationRequestList.stream().map(
                        item -> {
                            try {
                                return ObjectMapperUtils.writeValueAsString(item);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }).toList(),
                new HashMap[]{new HashMap<>()},
                paymentNoticeGenerationRequestErrors,executionContextMock));
        verify(noticeFolderService).manageFolder(any());
        assertEquals(0, paymentNoticeGenerationRequestErrors.size());
    }

    @Test
    void runShouldIgnoreUnfinishedElements() throws SaveNoticeToBlobException {
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>
                paymentNoticeGenerationRequestList =
                Collections.singletonList(
                        it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest.builder()
                        .status(PaymentGenerationRequestStatus.COMPLETING).numberOfElementsTotal(2)
                        .numberOfElementsProcessed(1).numberOfElementsFailed(0).build());
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>
                paymentNoticeGenerationRequestErrors = new ArrayList<>();
        assertDoesNotThrow(() -> sut.processGenerateReceipt(paymentNoticeGenerationRequestList.stream().map(
                item -> {
                    try {
                        return ObjectMapperUtils.writeValueAsString(item);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).toList(),
                new HashMap[]{new HashMap<>()},
                paymentNoticeGenerationRequestErrors,executionContextMock));
        verifyNoInteractions(noticeFolderService);
        assertEquals(0, paymentNoticeGenerationRequestErrors.size());
    }

    @Test
    void runShouldSendErrorOnException() throws SaveNoticeToBlobException {
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest>
                paymentNoticeGenerationRequestList =
                Collections.singletonList(it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequest.builder()
                        .status(PaymentGenerationRequestStatus.COMPLETING).numberOfElementsTotal(2)
                        .numberOfElementsProcessed(1).numberOfElementsFailed(1).build());
        List<it.gov.pagopa.print.payment.notice.functions.model.PaymentNoticeGenerationRequestError>
                paymentNoticeGenerationRequestErrors = new ArrayList<>();
        doAnswer(item -> {
            throw new SaveNoticeToBlobException("Error", 500);
        }).when(noticeFolderService).manageFolder(any());
        assertDoesNotThrow(() -> sut.processGenerateReceipt(paymentNoticeGenerationRequestList.stream().map(
                        item -> {
                            try {
                                return ObjectMapperUtils.writeValueAsString(item);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }).toList(),
                new HashMap[]{new HashMap<>()},
                paymentNoticeGenerationRequestErrors,executionContextMock));
        verify(noticeFolderService).manageFolder(any());
        assertEquals(1, paymentNoticeGenerationRequestErrors.size());
    }


}
