package it.gov.pagopa.print.payment.notice.functions.events.consumer;

import it.gov.pagopa.print.payment.notice.functions.service.impl.RetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class ErrorConsumerConfig {

    @Bean
    public Consumer<String> noticeError(RetryService retryService) {
        return retryService::retryError;
    }

}
