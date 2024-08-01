package it.gov.pagopa.print.payment.notice.functions.events.consumer;

import it.gov.pagopa.print.payment.notice.functions.service.impl.CompressionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class CompressionConsumerConfig {
    @Bean
    public Consumer<String> noticeComplete(CompressionService compressionService) {
        return compressionService::compressFolder;
    }


//    @ServiceActivator(inputChannel = "medium-eventhub.$Default.errors")
//    public void consumerError(Message<?> message) {
//        log.error("Handling consumer ERROR: " + message);
//    }
}
