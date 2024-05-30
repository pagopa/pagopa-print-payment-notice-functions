package it.gov.pagopa.print.payment.notice.functions.model.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeRequestEH {

    private String folderId;
    private NoticeGenerationRequestItem noticeData;
    private String errorId;

}
