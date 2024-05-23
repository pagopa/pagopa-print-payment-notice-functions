package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeGenerationRequestClientImpl;
import it.gov.pagopa.print.payment.notice.functions.client.impl.PaymentNoticeGenerationRequestErrorClientImpl;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

@ExtendWith(MockitoExtension.class)
class PaymentNoticeGenerationRequestErrorClientImplTest {

    @Mock
    public MongoCollection mongoCollection;

    PaymentNoticeGenerationRequestErrorClientImpl paymentNoticeGenerationRequestClient;

    @BeforeEach
    public void init() {
        reset(mongoCollection);
        paymentNoticeGenerationRequestClient =
                new PaymentNoticeGenerationRequestErrorClientImpl(mongoCollection);
    }

    @Test
    void testSingleton() throws Exception {
        withEnvironmentVariables(
                "NOTICE_REQUEST_MONGO_DB_NAME", "personDB",
                "NOTICE_REQUEST_MONGODB_CONN_STRING", "mongodb://localhost:27017/personDB",
                "NOTICE_REQUEST_MONGO_COLLECTION_NAME", "notice"
        ).execute(() -> Assertions.assertDoesNotThrow(PaymentNoticeGenerationRequestClientImpl::getInstance));
    }

    @Test
    void shouldExecuteUpdateWithoutExceptions() throws IOException {
        assertDoesNotThrow(() ->
                new PaymentNoticeGenerationRequestErrorClientImpl(Mockito.mock(MongoCollection.class))
                .updatePaymentGenerationRequestError(
                PaymentNoticeGenerationRequestError.builder()
                        .build()));
    }

    @Test
    void findOneShouldReturnOk() {
        PaymentNoticeGenerationRequestError optRequest =
                PaymentNoticeGenerationRequestError.builder().build();
        FindIterable findIterable = mock(FindIterable.class);
        doReturn(optRequest).when(findIterable).first();
        doReturn(findIterable).when(mongoCollection).find(any(Bson.class));
        assertEquals(optRequest,
                paymentNoticeGenerationRequestClient.findOne("test").get());
        verify(mongoCollection).find(any(Bson.class));
    }

    @Test
    void shouldExecuteDeleteWithoutExceptions() {
        assertDoesNotThrow(() ->
                new PaymentNoticeGenerationRequestErrorClientImpl(Mockito.mock(MongoCollection.class))
                        .deleteRequestError("test"));
    }

}