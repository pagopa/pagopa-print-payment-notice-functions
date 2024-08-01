package it.gov.pagopa.print.payment.notice.functions.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
@Document("payment_notice_generation_request_error")
public class PaymentNoticeGenerationRequest {

    private String id;

    private String userId;

    private Instant createdAt;

    private Instant requestDate;

    private PaymentGenerationRequestStatus status;

    private List<String> items;

    private Integer numberOfElementsFailed;

    private Integer numberOfElementsTotal;

}
