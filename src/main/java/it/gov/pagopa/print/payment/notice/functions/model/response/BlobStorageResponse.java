package it.gov.pagopa.print.payment.notice.functions.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model class for Blob Storage client's response
 */
@Getter
@Setter
@NoArgsConstructor
public class BlobStorageResponse {

    String documentUrl;
    String documentName;
    int statusCode;
}
