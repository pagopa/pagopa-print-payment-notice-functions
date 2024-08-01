package it.gov.pagopa.print.payment.notice.functions.events.model;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentGenerationRequestStatus;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class CompressionEvent {


    private String id;

    private String userId;

    private PaymentGenerationRequestStatus status;

    private List<String> items;

    private String createdAt;

    private String requestDate;

    private Integer numberOfElementsFailed;

    private Integer numberOfElementsTotal;

}
