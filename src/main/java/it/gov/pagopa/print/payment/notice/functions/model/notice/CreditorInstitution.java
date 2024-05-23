package it.gov.pagopa.print.payment.notice.functions.model.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreditorInstitution {

    private String taxCode;

    private String fullName;

    private String organization;

    private String info;

    private Boolean webChannel;

    private String physicalChannel;

    private String cbill;

    private String logo;

    private String posteAuth;

    private String posteAccountNumber;

}
