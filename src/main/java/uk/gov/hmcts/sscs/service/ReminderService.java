package uk.gov.hmcts.sscs.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.service.reminder.ReminderHandler;

@Service
public class ReminderService {

    private List<ReminderHandler> reminderHandlers;

    @Autowired
    public ReminderService(
        List<ReminderHandler> reminderHandlers
    ) {
        this.reminderHandlers = reminderHandlers;
    }

    public void createReminders(CcdResponse ccdResponse) {

        for (ReminderHandler reminderHandler : reminderHandlers) {
            if (reminderHandler.canHandle(ccdResponse)) {
                reminderHandler.handle(ccdResponse);
            }
        }
    }

}
