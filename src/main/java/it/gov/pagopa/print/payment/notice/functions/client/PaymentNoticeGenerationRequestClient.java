package it.gov.pagopa.print.payment.notice.functions.client;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;

public interface PaymentNoticeGenerationRequestClient {
    void updatePaymentGenerationRequest(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest);
}
