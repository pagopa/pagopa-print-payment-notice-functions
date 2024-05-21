package it.gov.pagopa.print.payment.notice.functions.entity;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class PaymentNoticeGenerationRequest {

    private String id;

    private String userId;

    private Instant createdAt;

    private Instant requestDate;

    private PaymentGenerationRequestStatus status;

    private List<String> items;

    private Integer numberOfElementsProcessed;

    private Integer numberOfElementsFailed;

    private Integer numberOfElementsTotal;

}
