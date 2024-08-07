package it.gov.pagopa.print.payment.notice.functions.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.print.payment.notice.functions.events.model.ErrorEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeGenerationRequestProducer;
import it.gov.pagopa.print.payment.notice.functions.events.producer.NoticeRequestCompleteProducer;
import it.gov.pagopa.print.payment.notice.functions.exception.Aes256Exception;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.print.payment.notice.functions.repository.PaymentGenerationRequestRepository;
import it.gov.pagopa.print.payment.notice.functions.utils.Aes256Utils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class RetryServiceTest {

    @MockBean
    PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;

    @MockBean
    NoticeRequestCompleteProducer noticeRequestCompleteProducer;

    @MockBean
    NoticeGenerationRequestProducer NoticeGenerationRequestProducer;

    @MockBean
    PaymentGenerationRequestRepository paymentGenerationRequestRepository;

    @Autowired
    @InjectMocks
    private RetryService retryService;

    @Autowired
    private Aes256Utils aes256Utils;

    @Test
    void retryError() throws JsonProcessingException, Aes256Exception {

        when(paymentGenerationRequestErrorRepository.findById("1")).thenReturn(Optional.ofNullable(PaymentNoticeGenerationRequestError.builder()
                .id("1")
                .folderId("1234")
                .errorId("8")
                .errorCode("NOT FOUND")
                .errorDescription("ITEM NOT FOUND")
                .data("{\"key\": \"value\"}")
                .numberOfAttempts(0)
                .compressionError(false)
                .build()));

        when(NoticeGenerationRequestProducer.sendGenerationEvent(any())).thenReturn(true);

        var elem = ErrorEvent.builder()
                .id("1")
                .folderId("1234")
                .errorId("8")
                .errorCode("NOT FOUND")
                .errorDescription("ITEM NOT FOUND")
                .createdAt("")
                .data(aes256Utils.encrypt(new ObjectMapper().writeValueAsString(GenerationEvent.builder().build())))
                .numberOfAttempts(0)
                .compressionError(false)
                .build();
        retryService.retryError(new ObjectMapper().writeValueAsString(elem));

        verify(paymentGenerationRequestErrorRepository, times(1)).save(any());
        verify(noticeRequestCompleteProducer, never()).sendNoticeComplete(any());
        verify(NoticeGenerationRequestProducer, times(1)).sendGenerationEvent(any());

    }

    @Test
    void retryErrorCompression() throws JsonProcessingException, Aes256Exception {

        when(paymentGenerationRequestErrorRepository.findById("1")).thenReturn(Optional.ofNullable(PaymentNoticeGenerationRequestError.builder()
                .id("1")
                .folderId("12345")
                .errorId("8")
                .errorCode("NOT FOUND")
                .errorDescription("ITEM NOT FOUND")
                .data("{\"key\": \"value\"}")
                .numberOfAttempts(0)
                .compressionError(true)
                .build()));

        when(noticeRequestCompleteProducer.sendNoticeComplete(any())).thenReturn(true);
        when(paymentGenerationRequestRepository.findById(any())).thenReturn(Optional.ofNullable(PaymentNoticeGenerationRequest.builder()
                .id("1")
                .userId("user")
                .createdAt(null)
                .requestDate(null)
                .status(PaymentGenerationRequestStatus.COMPLETING)
                .items(List.of("1"))
                .numberOfElementsFailed(0)
                .numberOfElementsTotal(1)
                .build()));

        var elem = ErrorEvent.builder()
                .id("1")
                .folderId("123456")
                .errorId("8")
                .errorCode("NOT FOUND")
                .errorDescription("ITEM NOT FOUND")
                .createdAt("")
                .data(aes256Utils.encrypt(new ObjectMapper().writeValueAsString(GenerationEvent.builder().build())))
                .numberOfAttempts(0)
                .compressionError(true)
                .build();
        retryService.retryError(new ObjectMapper().writeValueAsString(elem));

        verify(paymentGenerationRequestErrorRepository, times(1)).save(any());
        verify(noticeRequestCompleteProducer, times(1)).sendNoticeComplete(any());
        verify(NoticeGenerationRequestProducer, never()).sendGenerationEvent(any());

    }
}