package it.gov.pagopa.print.payment.notice.functions.utils;

import it.gov.pagopa.print.payment.notice.functions.events.model.GenerationEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonUtility {


    private static final Logger log = LoggerFactory.getLogger(CommonUtility.class);


    public static String getMessageId(GenerationEvent noticeGenerationRequestEH) {
        try {
            String code = noticeGenerationRequestEH.getNoticeData().getData().getNotice().getCode();
            if (code == null) {
                code = noticeGenerationRequestEH.getNoticeData().getData().getNotice().getInstallments().get(0).getCode();
            }
            return code;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
