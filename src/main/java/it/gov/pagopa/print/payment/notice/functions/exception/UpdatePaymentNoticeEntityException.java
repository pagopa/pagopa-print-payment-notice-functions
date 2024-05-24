package it.gov.pagopa.print.payment.notice.functions.exception;

public class UpdatePaymentNoticeEntityException extends PaymentNoticeManagementException {

    /**
     * Constructs new exception with provided message, status code and cause
     *
     * @param message Detail message
     * @param statusCode Error code
     * @param cause Exception thrown
     */
    public UpdatePaymentNoticeEntityException(String message, int statusCode, Throwable cause) {
        super(message, statusCode, cause);
    }

    /**
     * Constructs new exception with provided message, status code
     *
     * @param message Detail message
     * @param statusCode Error code
     */
    public UpdatePaymentNoticeEntityException(String message, int statusCode) {
        super(message, statusCode);
    }

}
