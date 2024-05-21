package it.gov.pagopa.print.payment.notice.functions.exception;

/** Thrown in case an error occurred when manating notice data */
public class PaymentNoticeManagementException extends Exception {

    private final int statusCode;

    /**
     * Constructs new exception with provided message, status code and cause
     *
     * @param message Detail message
     * @param statusCode Error code
     * @param cause Exception thrown
     */
    public PaymentNoticeManagementException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Constructs new exception with provided message, status code
     *
     * @param message Detail message
     * @param statusCode Error code
     */
    public PaymentNoticeManagementException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
