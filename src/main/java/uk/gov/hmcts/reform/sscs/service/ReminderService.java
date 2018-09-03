package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.reminder.ReminderHandler;
import uk.gov.hmcts.reform.sscs.service.reminder.ReminderHandler;

@Service
public class ReminderService {

    private List<ReminderHandler> reminderHandlers;

    @Autowired
    public ReminderService(
        List<ReminderHandler> reminderHandlers
    ) {
        this.reminderHandlers = reminderHandlers;
    }

    public void createReminders(SscsCaseData ccdResponse) {

        for (ReminderHandler reminderHandler : reminderHandlers) {
            if (reminderHandler.canHandle(ccdResponse)) {
                reminderHandler.handle(ccdResponse);
            }
        }
    }

}
