package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.functions.HttpStatus;
import com.mongodb.client.MongoCollection;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

class PaymentNoticeGenerationRequestClientImplTest {

    @Test
    void testSingleton() throws Exception {
        withEnvironmentVariables(
                "NOTICE_REQUEST_MONGO_DB_NAME", "personDB",
                "NOTICE_REQUEST_MONGODB_CONN_STRING", "mongodb://localhost:27017/personDB",
                "NOTICE_REQUEST_MONGO_COLLECTION_NAME", "notice"
        ).execute(() -> Assertions.assertDoesNotThrow(PaymentNoticeGenerationRequestClientImpl::getInstance));
    }

    @Test
    void runOk() throws IOException {
        assertDoesNotThrow(() ->
                new PaymentNoticeGenerationRequestClientImpl(Mockito.mock(MongoCollection.class))
                .updatePaymentGenerationRequest(
                PaymentNoticeGenerationRequest.builder().status(PaymentGenerationRequestStatus.PROCESSED)
                        .build()));
    }

}