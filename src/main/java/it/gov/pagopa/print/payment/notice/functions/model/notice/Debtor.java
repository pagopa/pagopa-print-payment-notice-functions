package it.gov.pagopa.print.payment.notice.functions.model.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Debtor {

    private String taxCode;

    private String fullName;

    private String address;

    private String postalCode;

    private String city;

    private String buildingNumber;

    private String province;

}
