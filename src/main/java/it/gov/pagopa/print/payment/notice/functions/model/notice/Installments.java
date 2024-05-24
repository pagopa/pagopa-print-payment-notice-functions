package it.gov.pagopa.print.payment.notice.functions.model.notice;

import lombok.Data;

import java.util.List;

@Data
public class Installments {

    private Integer number;
    private List<InstallmentData> data;

}
