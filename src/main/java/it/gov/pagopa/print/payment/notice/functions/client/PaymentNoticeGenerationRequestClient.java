package it.gov.pagopa.print.payment.notice.functions.client;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;

import java.util.Optional;

public interface PaymentNoticeGenerationRequestClient {
    Optional<PaymentNoticeGenerationRequest> findById(String folderId);

    void updatePaymentGenerationRequest(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest);

}
