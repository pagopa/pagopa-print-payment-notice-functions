package it.gov.pagopa.print.payment.notice.functions.events.producer;

import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.ErrorEvent;
import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProducerImplTest {

    @Mock
    private StreamBridge streamBridge;

    private NoticeGenerationRequestProducerImpl generationProducer;
    private NoticeRequestCompleteProducerImpl completeProducer;
    private NoticeRequestErrorProducerImpl errorProducer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        generationProducer = new NoticeGenerationRequestProducerImpl();
        completeProducer = new NoticeRequestCompleteProducerImpl();
        errorProducer = new NoticeRequestErrorProducerImpl();
        ReflectionTestUtils.setField(generationProducer, "streamBridge", streamBridge);
        ReflectionTestUtils.setField(completeProducer, "streamBridge", streamBridge);
        ReflectionTestUtils.setField(errorProducer, "streamBridge", streamBridge);
    }

    @Test
    void sendGenerationEventShouldReturnTrueWhenMessageIsSent() {
        when(streamBridge.send(eq("noticeGeneration-out-0"), any())).thenReturn(true);
        GenerationEvent event = GenerationEvent.builder().build();
        boolean result = generationProducer.sendGenerationEvent(event);
        assertTrue(result);
        verify(streamBridge).send(eq("noticeGeneration-out-0"), any());
    }

    @Test
    void sendGenerationEventShouldReturnFalseWhenMessageIsNotSent() {
        when(streamBridge.send(eq("noticeGeneration-out-0"), any())).thenReturn(false);
        GenerationEvent event = GenerationEvent.builder().build();
        boolean result = generationProducer.sendGenerationEvent(event);
        assertFalse(result);
        verify(streamBridge).send(eq("noticeGeneration-out-0"), any());
    }

    @Test
    void sendNoticeCompleteShouldReturnTrueWhenMessageIsSent() {
        when(streamBridge.send(eq("noticeComplete-out-0"), any())).thenReturn(true);
        CompressionEvent event = CompressionEvent.builder().id("folder-1").build();
        boolean result = completeProducer.sendNoticeComplete(event);
        assertTrue(result);
        verify(streamBridge).send(eq("noticeComplete-out-0"), any());
    }

    @Test
    void sendNoticeCompleteShouldReturnFalseWhenMessageIsNotSent() {
        when(streamBridge.send(eq("noticeComplete-out-0"), any())).thenReturn(false);
        CompressionEvent event = CompressionEvent.builder().id("folder-1").build();
        boolean result = completeProducer.sendNoticeComplete(event);
        assertFalse(result);
        verify(streamBridge).send(eq("noticeComplete-out-0"), any());
    }

    @Test
    void sendErrorEventShouldReturnTrueWhenMessageIsSent() {
        when(streamBridge.send(eq("noticeError-out-0"), any())).thenReturn(true);
        ErrorEvent event = ErrorEvent.builder().folderId("folder-1").compressionError(true).build();
        boolean result = errorProducer.sendErrorEvent(event);
        assertTrue(result);
        verify(streamBridge).send(eq("noticeError-out-0"), any());
    }

    @Test
    void sendErrorEventShouldReturnFalseWhenMessageIsNotSent() {
        when(streamBridge.send(eq("noticeError-out-0"), any())).thenReturn(false);
        ErrorEvent event = ErrorEvent.builder().folderId("folder-1").compressionError(true).build();
        boolean result = errorProducer.sendErrorEvent(event);
        assertFalse(result);
        verify(streamBridge).send(eq("noticeError-out-0"), any());
    }
}