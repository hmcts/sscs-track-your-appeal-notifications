package uk.gov.hmcts.reform.sscs.service.reminder;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public interface ReminderHandler {

    boolean canHandle(SscsCaseData ccdResponse);

    void handle(SscsCaseData ccdResponse);
}
