package it.gov.pagopa.print.payment.notice.functions.events.producer;

import it.gov.pagopa.print.payment.notice.functions.events.model.ErrorEvent;
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
public class NoticeRequestErrorProducerImpl implements NoticeRequestErrorProducer {
    
    private static final String MDC_TOPIC = "topic";
    private static final String MDC_ACTION = "action";

    @Autowired
    private StreamBridge streamBridge;

    public static Message<ErrorEvent> buildMessage(ErrorEvent paymentNoticeGenerationRequestError) {
        return MessageBuilder.withPayload(paymentNoticeGenerationRequestError)
                .build();
    }

    @Override
    public boolean sendErrorEvent(ErrorEvent errorEvent) {
        var res = streamBridge.send("noticeError-out-0", buildMessage(errorEvent));

        MDC.put(MDC_TOPIC, "error");

        if (res) {
            MDC.put(MDC_ACTION, "sent");
            log.info("Error Message Sent");
        } else {
            MDC.put(MDC_ACTION, "failed");
            log.error("Unable to send Error Message");
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
    static class NoticeGenerationRequestErrorConfig {

        @Bean
        public Supplier<Flux<Message<ErrorEvent>>> sendErrorEvent() {
            return Flux::empty;
        }

    }

}
