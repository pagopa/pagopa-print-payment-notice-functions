package it.gov.pagopa.print.payment.notice.functions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationPropertiesTest {

    private static final String ERROR_DESTINATION =
            "spring.cloud.stream.bindings.noticeError-out-0.destination";

    private static final String ERROR_PRODUCER_CREDENTIALS =
            "spring.cloud.stream.binders.error-producer.environment."
                    + "spring.cloud.stream.kafka.binder.configuration.sasl.jaas.config";

    @Test
    void noticeErrorProducerShouldUseErrorTopicAndCredentials() throws IOException {

        Properties properties = loadProperties(
                "src/main/resources/application.properties");

        assertEquals(
                "${KAFKA_NOTICE_ERROR_TOPIC:pagopa-printit-errors-evh}",
                properties.getProperty(ERROR_DESTINATION));

        assertEquals(
                "${NOTICE_ERROR_KAFKA_SASL_JAAS_CONFIG}",
                properties.getProperty(ERROR_PRODUCER_CREDENTIALS));
    }

    private Properties loadProperties(String path) throws IOException {

        Properties properties = new Properties();

        try (InputStream inputStream =
                     Files.newInputStream(Path.of(path))) {

            properties.load(inputStream);
        }

        return properties;
    }
}