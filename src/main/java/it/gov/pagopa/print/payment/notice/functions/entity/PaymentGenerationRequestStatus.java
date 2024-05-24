package it.gov.pagopa.print.payment.notice.functions.entity;

/**
 * Enum containing generation request status
 */
public enum PaymentGenerationRequestStatus {

    INSERTED,
    PROCESSING,
    COMPLETING,
    FAILED,
    PROCESSED,
    PROCESSED_WITH_FAILURES

}
