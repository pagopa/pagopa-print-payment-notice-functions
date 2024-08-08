package it.gov.pagopa.print.payment.notice.functions.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.ListBlobsOptions;
import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;
import it.gov.pagopa.print.payment.notice.functions.utils.WorkingDirectoryUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.gov.pagopa.print.payment.notice.functions.utils.WorkingDirectoryUtils.createWorkingDirectory;

@Component
@Slf4j
public class NoticeStorageClient {

    private BlobContainerClient blobContainerClient;

    @Autowired
    public NoticeStorageClient(
            @Value("${spring.cloud.azure.storage.blob.notices.enabled}") String enabled,
            @Value("${spring.cloud.azure.storage.blob.notices.connection_string}") String connectionString,
            @Value("${spring.cloud.azure.storage.blob.notices.containerName}") String containerName) {
        if (Boolean.TRUE.toString().equals(enabled)) {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString).buildClient();
            blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        }
    }

    public NoticeStorageClient(
            Boolean enabled,
            BlobContainerClient blobContainerClient) {
        if (Boolean.TRUE.equals(enabled)) {
            this.blobContainerClient = blobContainerClient;
        }
    }

    public BlobStorageResponse compressFolder(String folderId) throws IOException {
        log.info("Create Zip file. Request {}", folderId);

        File workingDirectory = createWorkingDirectory();
        Path tempDirectory = Files.createTempDirectory(workingDirectory.toPath(), "notice-generation-function")
                .normalize()
                .toAbsolutePath();
        File pathFile = File.createTempFile("tempFile", ".zip", tempDirectory.toFile());

        try {
            try (FileOutputStream fos = new FileOutputStream(pathFile);
                 BufferedOutputStream outputStream = new BufferedOutputStream(fos);
                 ZipOutputStream zipStream = new ZipOutputStream(outputStream)) {

                String delimiter = "/";
                ListBlobsOptions options = new ListBlobsOptions()
                        .setPrefix(folderId.concat("/"));


                blobContainerClient.listBlobsByHierarchy(delimiter, options, null)
                        .stream()
                        .forEach(blobItem -> {

                            if (!blobItem.isPrefix() && blobItem.getName().contains(".pdf")) {

                                final String[] splitName = blobItem.getName().split(delimiter, 2);
                                final String finalSingleFileName = splitName.length > 1 ? splitName[1] : splitName[0];
                                final String finalSingleFilepath = blobItem.getName();

                                MDC.put("itemId", finalSingleFilepath);
                                log.info("Get info file {} from blob. Request {}", blobItem.getName(), folderId);
                                final BlobClient blobClient = blobContainerClient.getBlobClient(blobItem.getName());

                                try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
                                    if (blobClient.exists()) {
                                        log.info("put file {} into zipStream. Request {}", blobItem.getName(), folderId);
                                        blobClient.downloadStream(fileOutputStream);
                                        zipStream.putNextEntry(new ZipEntry(finalSingleFileName));
                                        zipStream.write(fileOutputStream.toByteArray());
                                        zipStream.closeEntry();
                                        zipStream.flush();
                                    } else {
                                        log.error("file not found: {}", finalSingleFileName);
                                        throw new RuntimeException("File not found: " + finalSingleFilepath);
                                    }
                                } catch (IOException e) {
                                    log.error("Error processing file {}", finalSingleFileName, e);
                                    throw new RuntimeException("Error processing file: " + finalSingleFileName, e);
                                }
                                log.info("Download file completed. Request {}", folderId);


                            }
                        });

                zipStream.finish();


            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (FileInputStream fis = new FileInputStream(pathFile);
                 BufferedInputStream bif = new BufferedInputStream(fis)) {

                BlobClient zipFileClient = blobContainerClient.getBlobClient(
                        folderId + "/" + folderId.concat(".zip"));
                zipFileClient.upload(bif, true);
                log.info("Zip file uploaded. Request {}", folderId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            BlobStorageResponse blobStorageResponse = new BlobStorageResponse();
            blobStorageResponse.setStatusCode(HttpStatus.OK.value());
            return blobStorageResponse;
        } finally {
            WorkingDirectoryUtils.clearTempDirectory(tempDirectory);
        }
    }
}
