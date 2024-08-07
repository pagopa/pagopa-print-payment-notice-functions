package it.gov.pagopa.print.payment.notice.functions.events.producer;


import it.gov.pagopa.print.payment.notice.functions.events.model.CompressionEvent;
import org.springframework.stereotype.Service;

/**
 * Interface to use when required to execute sending of a notice generation request through
 * the eventhub channel
 */
@Service
public interface NoticeRequestCompleteProducer {

    /**
     * Send notige generation request through EH
     *
     * @param paymentNoticeGenerationRequest data to send
     * @return boolean referring if the insertion on the sending channel was successfully
     */
    boolean sendNoticeComplete(CompressionEvent paymentNoticeGenerationRequest);

}
