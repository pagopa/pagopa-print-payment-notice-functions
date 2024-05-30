package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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

    @Mock
    public MongoClient mongoClient;

    @Mock
    public MongoDatabase mongoDatabase;

    PaymentNoticeGenerationRequestErrorClientImpl paymentNoticeGenerationRequestClient;

    @BeforeEach
    public void init() {
        reset(mongoClient, mongoDatabase, mongoCollection);
        mongoClient = Mockito.mock(MongoClient.class);
        mongoDatabase = Mockito.mock(MongoDatabase.class);
        lenient().when(mongoClient.getDatabase(any())).thenReturn(mongoDatabase);
        lenient().when(mongoDatabase.withCodecRegistry(any())).thenReturn(mongoDatabase);
        lenient().when(mongoDatabase.getCollection(any(),any())).thenReturn(mongoCollection);
        paymentNoticeGenerationRequestClient =
                new PaymentNoticeGenerationRequestErrorClientImpl(mongoClient);
    }

    @Test
    void testSingleton() throws Exception {
        withEnvironmentVariables(
                "NOTICE_REQUEST_MONGO_DB_NAME", "personDB",
                "NOTICE_REQUEST_MONGODB_CONN_STRING", "mongodb://localhost:27017/personDB",
                "NOTICE_ERR_REQUEST_MONGO_COLLECTION_NAME", "notice-errors"
        ).execute(() -> Assertions.assertDoesNotThrow(PaymentNoticeGenerationRequestErrorClientImpl::getInstance));
    }

    @Test
    void shouldExecuteUpdateWithoutExceptions() throws IOException {

        assertDoesNotThrow(() ->
                paymentNoticeGenerationRequestClient
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
                paymentNoticeGenerationRequestClient
                        .deleteRequestError("test"));
    }

}