package it.gov.pagopa.print.payment.notice.functions.model;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
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

    private PaymentGenerationRequestStatus status;

    private List<String> items;

    private String createdAt;

    private String requestDate;

    private Integer numberOfElementsFailed;

    private Integer numberOfElementsTotal;

}
