package it.gov.pagopa.print.payment.notice.functions.client;

import it.gov.pagopa.print.payment.notice.functions.model.response.BlobStorageResponse;

import java.io.InputStream;

public interface PaymentNoticeBlobClient {

    BlobStorageResponse compressFolder(String folderId);

}
