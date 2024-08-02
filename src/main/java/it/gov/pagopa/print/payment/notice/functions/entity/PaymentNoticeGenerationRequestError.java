package it.gov.pagopa.print.payment.notice.functions.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "errorId")
@ToString
@Document("payment_notice_generation_request_error")
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
