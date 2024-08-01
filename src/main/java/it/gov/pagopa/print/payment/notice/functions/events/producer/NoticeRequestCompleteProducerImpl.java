package it.gov.pagopa.print.payment.notice.functions.events.producer;

import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
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
public class NoticeRequestCompleteProducerImpl implements NoticeRequestCompleteProducer {

    private final StreamBridge streamBridge;

    public NoticeRequestCompleteProducerImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public static Message<CompressionEvent> buildMessage(CompressionEvent paymentNoticeGenerationRequest) {
        return MessageBuilder.withPayload(paymentNoticeGenerationRequest).build();
    }

    @Override
    public boolean sendNoticeComplete(CompressionEvent paymentNoticeGenerationRequest) {
        return streamBridge.send("noticeComplete-out-0",
                buildMessage(paymentNoticeGenerationRequest));
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
