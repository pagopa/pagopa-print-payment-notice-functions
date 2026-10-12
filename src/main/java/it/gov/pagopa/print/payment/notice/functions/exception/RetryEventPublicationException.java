package it.gov.pagopa.print.payment.notice.functions.exception;

/**
 * Raised when a retry attempt has been acquired but the corresponding
 * retry event cannot be published.
 */
public class RetryEventPublicationException extends RuntimeException {

    private static final long serialVersionUID = -4711101261478933674L;

    public RetryEventPublicationException() {
        super("Unable to publish retry event");
    }

    public RetryEventPublicationException(Throwable cause) {
        super("Unable to publish retry event", cause);
    }
}