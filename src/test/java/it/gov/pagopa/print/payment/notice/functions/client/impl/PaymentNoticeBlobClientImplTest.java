//package it.gov.pagopa.print.payment.notice.functions.client.impl;
//
//import com.azure.core.http.rest.PagedIterable;
//import com.azure.core.http.rest.PagedResponse;
//import com.azure.core.http.rest.Response;
//import com.azure.storage.blob.BlobClient;
//import com.azure.storage.blob.BlobContainerClient;
//import com.azure.storage.blob.BlobServiceClient;
//import com.azure.storage.blob.models.BlobItem;
//import com.microsoft.azure.functions.HttpStatus;
//import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//
//import java.io.IOException;
//import java.util.Collections;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;
//
//class PaymentNoticeBlobClientImplTest {
//
//    @Test
//    void testSingleton() throws Exception {
//        @SuppressWarnings("secrets:S6338")
//        String mockKey = "mockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeyMK==";
//        withEnvironmentVariables(
//                "BLOB_STORAGE_CONTAINER_NAME", "notice",
//                "BLOB_STORAGE_CONN_STRING", "DefaultEndpointsProtocol=https;AccountName=samplestorage;AccountKey="+mockKey+";EndpointSuffix=core.windows.net",
//                "BLOB_STORAGE_ACCOUNT_ENDPOINT", "https://samplestorage.blob.core.windows.net"
//        ).execute(() -> Assertions.assertDoesNotThrow(PaymentNoticeBlobClientImpl::getInstance));
//    }
//
//    @Test
//    void runOk() throws IOException {
//        BlobServiceClient mockServiceClient = mock(BlobServiceClient.class);
//        BlobContainerClient mockContainer = mock(BlobContainerClient.class);
//        BlobClient mockClient = mock(BlobClient.class);
//
//        Response mockBlockItem = mock(Response.class);
//
//        when(mockBlockItem.getStatusCode()).thenReturn(HttpStatus.CREATED.value());
//
//        when(mockClient.uploadWithResponse(any(), eq(null), eq(null))).thenReturn(
//                mockBlockItem
//        );
//        String VALID_BLOB_NAME = "a valid blob name";
//        String VALID_BLOB_URL = "a valid blob url";
//        when(mockClient.getBlobName()).thenReturn(VALID_BLOB_NAME);
//        when(mockClient.getBlobUrl()).thenReturn(VALID_BLOB_URL);
//
//        when(mockContainer.getBlobClient(any())).thenReturn(mockClient);
//
//        when(mockServiceClient.getBlobContainerClient(any())).thenReturn(mockContainer);
//
//        PaymentNoticeBlobClientImpl receiptBlobClient = new PaymentNoticeBlobClientImpl(mockServiceClient);
//
//
//
//        BlobItem blobItem = new BlobItem();
//        blobItem.setName("testName");
//        when(mockClient.exists()).thenReturn(true);
//        PagedIterable pagedIterable = Mockito.mock(PagedIterable.class);
//        doReturn(Collections.singletonList(blobItem).stream()).when(pagedIterable).stream();
//        when(mockContainer.listBlobsByHierarchy(any(), any(), any())).thenReturn(pagedIterable);
//
//        BlobStorageResponse response = receiptBlobClient.compressFolder("filename");
//
//        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
//
//    }
//
//}