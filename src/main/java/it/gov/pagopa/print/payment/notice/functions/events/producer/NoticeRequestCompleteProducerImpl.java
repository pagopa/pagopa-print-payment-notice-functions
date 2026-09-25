package it.gov.pagopa.print.payment.notice.functions.events.producer;

import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
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
public class NoticeRequestCompleteProducerImpl implements NoticeRequestCompleteProducer {

    private static final String MDC_TOPIC = "topic";
    private static final String MDC_ACTION = "action";
    
    @Autowired
    private StreamBridge streamBridge;

    public static Message<CompressionEvent> buildMessage(CompressionEvent paymentNoticeGenerationRequest) {
        return MessageBuilder.withPayload(paymentNoticeGenerationRequest).build();
    }

    @Override
    public boolean sendNoticeComplete(CompressionEvent compressionEvent) {
        var res = streamBridge.send("noticeComplete-out-0", buildMessage(compressionEvent));

        MDC.put(MDC_TOPIC, "complete");

        if (res) {
            MDC.put(MDC_ACTION, "sent");
            log.info("Complete Message Retry Sent");
        } else {
            MDC.put(MDC_ACTION, "failed");
            log.error("Unable to send Complete Message Retry");
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
        public Supplier<Flux<Message<CompressionEvent>>> noticeCompleteOut() {
            return Flux::empty;
        }

    }

}
