package uk.gov.hmcts.sscs.service.reminder;

import uk.gov.hmcts.sscs.domain.CcdResponse;

public interface ReminderHandler {

    boolean canHandle(CcdResponse ccdResponse);

    void handle(CcdResponse ccdResponse);
}
