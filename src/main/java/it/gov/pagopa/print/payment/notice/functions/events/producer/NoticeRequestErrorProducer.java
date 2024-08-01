package it.gov.pagopa.print.payment.notice.functions.events.producer;

import it.gov.pagopa.print.payment.notice.functions.events.model.RetryEvent;

/**
 * Interface to use when required to execute sending of a notice generation request through
 * the eventhub channel
 */
public interface NoticeRequestErrorProducer {

    /**
     * Send notice generation errors through EH
     *
     * @param paymentNoticeGenerationRequestError data to send
     * @return boolean referring if the insertion on the sending channel was successfully
     */
    boolean noticeError(RetryEvent paymentNoticeGenerationRequestError);

}
