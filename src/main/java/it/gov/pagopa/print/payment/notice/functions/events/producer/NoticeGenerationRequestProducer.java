package it.gov.pagopa.print.payment.notice.functions.events.producer;


import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
import org.springframework.stereotype.Service;

/**
 * Interface to use when required to execute sending of a notice generation request through
 * the eventhub channel
 */
@Service
public interface NoticeGenerationRequestProducer {

    /**
     * Send notige generation request through EH
     *
     * @param noticeGenerationRequestEH data to send
     * @return boolean referring if the insertion on the sending channel was successfully
     */
    boolean noticeGeneration(GenerationEvent noticeGenerationRequestEH);

}
