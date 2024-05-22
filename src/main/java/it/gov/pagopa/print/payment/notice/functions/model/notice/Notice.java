package it.gov.pagopa.print.payment.notice.functions.model.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notice {

    private String subject;

    private Long paymentAmount;

    private String dueDate;

    private String code;

    private String posteAuth;

    private String posteDocumentType;

    private List<InstallmentData> installments;

}
