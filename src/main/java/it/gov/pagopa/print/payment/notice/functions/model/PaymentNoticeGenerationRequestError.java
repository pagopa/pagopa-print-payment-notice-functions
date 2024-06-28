package it.gov.pagopa.print.payment.notice.functions.model;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "errorId")
@ToString
public class PaymentNoticeGenerationRequestError {

    private String id;

    private String folderId;

    private String errorId;

    private String errorCode;

    private String errorDescription;

    private String createdAt;

    private String data;

    private Integer numberOfAttempts;

    private boolean compressionError;

}
