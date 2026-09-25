package it.gov.pagopa.print.payment.notice.functions.events.producer;

import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
@Slf4j
public class NoticeGenerationRequestProducerImpl implements NoticeGenerationRequestProducer {

    private static final String MDC_TOPIC = "topic";
    private static final String MDC_ACTION = "action";

    @Autowired
    private StreamBridge streamBridge;

    public static Message<GenerationEvent> buildMessage(GenerationEvent noticeGenerationRequestEH) {
        return MessageBuilder.withPayload(noticeGenerationRequestEH).build();
    }

    @Override
    public boolean sendGenerationEvent(GenerationEvent noticeGenerationRequestEH) {
        var res = streamBridge.send("noticeGeneration-out-0", buildMessage(noticeGenerationRequestEH));

        MDC.put(MDC_TOPIC, "generation");

        if (res) {
            MDC.put(MDC_ACTION, "sent");
            log.info("Generation Message Retry Sent");
        } else {
            MDC.put(MDC_ACTION, "failed");
            log.error("Unable to send Generation Message Retry");
        }

        MDC.remove(MDC_TOPIC);
        MDC.remove(MDC_ACTION);

        return res;
    }

    /**
     * Declared just to let know Spring to connect the producer at startup
     */
    @Slf4j
    @Configuration
    static class NoticeGenerationRequestProducerConfig {

        @Bean
        Supplier<Flux<Message<GenerationEvent>>> sendGenerationEvent() {
            return Flux::empty;
        }
    }
}
