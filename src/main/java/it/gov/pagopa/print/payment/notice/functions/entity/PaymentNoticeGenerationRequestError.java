package it.gov.pagopa.print.payment.notice.functions.entity;

import lombok.*;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "errorId")
@ToString
public class PaymentNoticeGenerationRequestError {

    private String id;

    private String folderId;

    private String errorId;

    private Instant createdAt;

    private String errorCode;

    private String errorDescription;

    private String data;

    private Integer numberOfAttempts;

    private boolean compressionError;

}
