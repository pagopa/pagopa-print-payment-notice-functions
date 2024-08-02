package it.gov.pagopa.print.payment.notice.functions.events.producer;

import it.gov.pagopa.print.payment.notice.functions.events.model.ErrorEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
public class NoticeRequestErrorProducerImpl implements NoticeRequestErrorProducer {

    private final StreamBridge streamBridge;

    public NoticeRequestErrorProducerImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public static Message<ErrorEvent> buildMessage(ErrorEvent paymentNoticeGenerationRequestError) {
        return MessageBuilder.withPayload(paymentNoticeGenerationRequestError)
                .build();
    }

    @Override
    public boolean sendErrorEvent(ErrorEvent paymentNoticeGenerationRequestError) {
        var res = streamBridge.send("noticeError-out-0",
                buildMessage(paymentNoticeGenerationRequestError));

        MDC.put("topic", "complete");
        MDC.put("action", "sent");
        log.info("Complete Message Retry Sent");
        MDC.remove("topic");
        MDC.remove("action");

        return res;
    }

    /**
     * Declared just to let know Spring to connect the producer at startup
     */
    @Slf4j
    @Configuration
    static class NoticeGenerationRequestErrorConfig {

        @Bean
        public Supplier<Flux<Message<ErrorEvent>>> sendErrorEvent() {
            return Flux::empty;
        }

    }

}
