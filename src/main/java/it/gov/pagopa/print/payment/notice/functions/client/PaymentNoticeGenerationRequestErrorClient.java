package it.gov.pagopa.print.payment.notice.functions.client;

import it.gov.pagopa.print.payment.notice.functions.entity.PaymentNoticeGenerationRequestError;

import java.util.Optional;

public interface PaymentNoticeGenerationRequestErrorClient {

    void updatePaymentGenerationRequestError(PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError);

    Optional<PaymentNoticeGenerationRequestError> findOne(String folderId);

    void deleteRequestError(String id);

    String save(
            PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError);
}
