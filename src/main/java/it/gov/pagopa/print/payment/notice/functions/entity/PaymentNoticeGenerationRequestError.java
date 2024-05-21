package it.gov.pagopa.print.payment.notice.functions.entity;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "folderId")
@ToString
public class PaymentNoticeGenerationRequestError {

    private String id;

    private String folderId;

    private Instant createdAt;

    private String errorCode;

    private String errorDescription;

    private String data;

    private Integer numberOfAttempts;

}
