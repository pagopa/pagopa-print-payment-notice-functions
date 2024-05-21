package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeBlobClient;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Client for the Blob Storage
 */
public class PaymentNoticeBlobClientImpl implements PaymentNoticeBlobClient {

    private static PaymentNoticeBlobClientImpl instance;

    private final String containerName = System.getenv("BLOB_STORAGE_CONTAINER_NAME");

    private final BlobServiceClient blobServiceClient;

    private PaymentNoticeBlobClientImpl() {
        String connectionString = System.getenv("BLOB_STORAGE_CONN_STRING");
        String storageAccount = System.getenv("BLOB_STORAGE_ACCOUNT_ENDPOINT");

        this.blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(storageAccount)
                .connectionString(connectionString)
                .buildClient();
    }

    PaymentNoticeBlobClientImpl(BlobServiceClient serviceClient) {
        this.blobServiceClient = serviceClient;
    }

    public static PaymentNoticeBlobClientImpl getInstance() {
        if (instance == null) {
            instance = new PaymentNoticeBlobClientImpl();
        }

        return instance;
    }

    public BlobStorageResponse compressFolder(String folderId) {

        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipStream = new ZipOutputStream(outputStream)) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            String delimiter = "/";
            ListBlobsOptions options = new ListBlobsOptions()
                    .setPrefix(folderId);


            for (BlobItem blobItem :  blobContainerClient.listBlobsByHierarchy(delimiter, options, null)) {
                final BlobClient blobClient = blobContainerClient.getBlobClient(blobItem.getName());

                final String finalSingleFileName = blobItem.getName().split(delimiter)[1];
                final String finalSingleFilepath = blobItem.getName();

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
                        if (blobClient.exists()) {
                            blobClient.downloadStream(fileOutputStream);
                            zipStream.putNextEntry(new ZipEntry(finalSingleFileName));
                            zipStream.write(fileOutputStream.toByteArray());
                            zipStream.closeEntry();
                        } else {
                            throw new RuntimeException("File not found: " + finalSingleFilepath);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error processing file: " + finalSingleFileName, e);
                    }
                });

                futures.add(future);
            }

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.join();
        } catch (IOException e) {
            throw new RuntimeException("Error creating zip file", e);
        }

        BlobClient zipFileClient = blobContainerClient.getBlobClient(
                folderId + "/" + folderId.concat(".zip"));
        zipFileClient.upload(new ByteArrayInputStream(
                outputStream.toByteArray()), outputStream.size(), true);

        BlobStorageResponse blobStorageResponse = new BlobStorageResponse();
        blobStorageResponse.setStatusCode(200);
        return blobStorageResponse;
    }
}
