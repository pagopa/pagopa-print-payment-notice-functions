package it.gov.pagopa.print.payment.notice.functions.exception;

/** Thrown in case an error occurred when saving a PDF Receipt to the Blob storage */
public class SaveNoticeToBlobException extends PaymentNoticeManagementException {

    /**
     * Constructs new exception with provided message, status code and cause
     *
     * @param message Detail message
     * @param statusCode Error code
     * @param cause Exception thrown
     */
    public SaveNoticeToBlobException(String message, int statusCode, Throwable cause) {
        super(message, statusCode, cause);
    }

    /**
     * Constructs new exception with provided message, status code
     *
     * @param message Detail message
     * @param statusCode Error code
     */
    public SaveNoticeToBlobException(String message, int statusCode) {
        super(message, statusCode);
    }
}
