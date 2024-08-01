package it.gov.pagopa.print.payment.notice.functions.events.model;

import it.gov.pagopa.print.payment.notice.functions.model.notice.NoticeGenerationRequestItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenerationEvent {

    private String folderId;
    private NoticeGenerationRequestItem noticeData;
    private String errorId;

}
