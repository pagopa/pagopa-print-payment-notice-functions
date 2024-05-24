package it.gov.pagopa.print.payment.notice.functions.model.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeRequestData {

    private Notice notice;
    private CreditorInstitution creditorInstitution;
    private Debtor debtor;

}
