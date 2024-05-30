package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.functions.HttpStatus;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

@ExtendWith(MockitoExtension.class)
class PaymentNoticeGenerationRequestClientImplTest {

    @Mock
    public MongoCollection mongoCollection;

    @Mock
    public MongoClient mongoClient;

    @Mock
    public MongoDatabase mongoDatabase;

    PaymentNoticeGenerationRequestClientImpl paymentNoticeGenerationRequestClient;

    @BeforeEach
    public void init() {
        reset(mongoClient, mongoDatabase, mongoCollection);
        mongoClient = Mockito.mock(MongoClient.class);
        mongoDatabase = Mockito.mock(MongoDatabase.class);
        lenient().when(mongoClient.getDatabase(any())).thenReturn(mongoDatabase);
        lenient().when(mongoDatabase.withCodecRegistry(any())).thenReturn(mongoDatabase);
        lenient().when(mongoDatabase.getCollection(any(),any())).thenReturn(mongoCollection);
        paymentNoticeGenerationRequestClient =
                new PaymentNoticeGenerationRequestClientImpl(mongoClient);
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
                paymentNoticeGenerationRequestClient
                .updatePaymentGenerationRequest(
                PaymentNoticeGenerationRequest.builder().status(PaymentGenerationRequestStatus.PROCESSED)
                        .build()));
    }

    @Test
    void findById() {
        PaymentNoticeGenerationRequest optRequest =
                PaymentNoticeGenerationRequest.builder().build();
        FindIterable findIterable = mock(FindIterable.class);
        doReturn(optRequest).when(findIterable).first();
        doReturn(findIterable).when(mongoCollection).find(any(Bson.class));
        assertEquals(optRequest,
                paymentNoticeGenerationRequestClient.findById("test").get());
        verify(mongoCollection).find(any(Bson.class));
    }

}