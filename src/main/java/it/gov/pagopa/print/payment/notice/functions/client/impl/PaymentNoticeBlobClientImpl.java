package it.gov.pagopa.print.payment.notice.functions.client.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.microsoft.azure.functions.HttpStatus;
import it.gov.pagopa.print.payment.notice.functions.ManageNoticeErrors;
import it.gov.pagopa.print.payment.notice.functions.client.PaymentNoticeBlobClient;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Client for the Blob Storage regarding notices
 */
public class PaymentNoticeBlobClientImpl implements PaymentNoticeBlobClient {

    private static PaymentNoticeBlobClientImpl instance;
    private final Logger logger = LoggerFactory.getLogger(ManageNoticeErrors.class);
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
        if(instance == null) {
            instance = new PaymentNoticeBlobClientImpl();
        }

        return instance;
    }

    /**
     * Using the provided id it will attempt to recover the folder data,
     * compressing
     *
     * @param folderId
     * @return
     */
    @SneakyThrows
    public BlobStorageResponse compressFolder(String folderId) {
        logger.info("Create Zip file. Request {}", folderId);

        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {


            try (ZipOutputStream zipStream = new ZipOutputStream(outputStream)) {
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                String delimiter = "/";
                ListBlobsOptions options = new ListBlobsOptions()
                        .setPrefix(folderId.concat("/"));


                blobContainerClient.listBlobsByHierarchy(delimiter, options, null)
                        .stream()
                        .forEach(blobItem -> {

                            if(!blobItem.isPrefix() && blobItem.getName().contains(".pdf")) {
                                logger.info("Get info file {} from blob. Request {}", blobItem.getName(), folderId);
                                final BlobClient blobClient = blobContainerClient.getBlobClient(blobItem.getName());

                                final String[] splitName = blobItem.getName().split(delimiter, 2);
                                final String finalSingleFileName = splitName.length > 1 ? splitName[1] : splitName[0];
                                final String finalSingleFilepath = blobItem.getName();

//                                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                                try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
                                    if(blobClient.exists()) {
                                        blobClient.downloadStream(fileOutputStream);
                                        zipStream.putNextEntry(new ZipEntry(finalSingleFileName));
                                        zipStream.write(fileOutputStream.toByteArray());
                                        zipStream.closeEntry();
                                    } else {
                                        logger.error("file not found: {}", finalSingleFileName);
                                        throw new RuntimeException("File not found: " + finalSingleFilepath);
                                    }
                                } catch (IOException e) {
                                    logger.error("Error processing file {}", finalSingleFileName, e);
                                    throw new RuntimeException("Error processing file: " + finalSingleFileName, e);
                                }
                                logger.info("Download file completed. Request {}", folderId);
//                                });

//                                futures.add(future);

                            }
                        });

//                CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//                allOf.join();
            } catch (IOException e) {
                throw new RuntimeException("Error creating zip file", e);
            }

            BlobClient zipFileClient = blobContainerClient.getBlobClient(
                    folderId + "/" + folderId.concat(".zip"));
            zipFileClient.upload(new ByteArrayInputStream(
                    outputStream.toByteArray()), outputStream.size(), true);
            logger.info("Zip file uploaded. Request {}", folderId);
        }

        BlobStorageResponse blobStorageResponse = new BlobStorageResponse();
        blobStorageResponse.setStatusCode(HttpStatus.OK.value());
        return blobStorageResponse;
    }
}
