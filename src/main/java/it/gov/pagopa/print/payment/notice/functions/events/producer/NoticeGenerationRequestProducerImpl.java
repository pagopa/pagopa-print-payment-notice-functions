package it.gov.pagopa.print.payment.notice.functions.events.producer;

import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
public class NoticeGenerationRequestProducerImpl implements NoticeGenerationRequestProducer {

    private final StreamBridge streamBridge;

    public NoticeGenerationRequestProducerImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public static Message<GenerationEvent> buildMessage(
            GenerationEvent noticeGenerationRequestEH) {
        return MessageBuilder.withPayload(noticeGenerationRequestEH).build();
    }

    @Override
    public boolean sendGenerationEvent(GenerationEvent noticeGenerationRequestEH) {
        return streamBridge.send("noticeGeneration-out-0",
                buildMessage(noticeGenerationRequestEH));
    }

    /**
     * Declared just to let know Spring to connect the producer at startup
     */
    @Slf4j
    @Configuration
    static class NoticeGenerationRequestProducerConfig {

        @Bean
        public Supplier<Flux<Message<GenerationEvent>>> sendGenerationEvent() {
            return Flux::empty;
        }

    }

}
