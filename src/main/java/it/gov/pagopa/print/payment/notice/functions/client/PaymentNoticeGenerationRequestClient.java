package it.gov.pagopa.print.payment.notice.functions.client;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;

import java.util.Optional;

public interface PaymentNoticeGenerationRequestClient {
    Optional<PaymentNoticeGenerationRequest> findById(String folderId);

    void updatePaymentGenerationRequest(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest);
}
