package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.reminder.ReminderHandler;
import uk.gov.hmcts.reform.sscs.service.reminder.ReminderHandler;

public class ReminderServiceTest {

    ReminderHandler reminderHandler1 = mock(ReminderHandler.class);
    ReminderHandler reminderHandler2 = mock(ReminderHandler.class);
    ReminderHandler reminderHandler3 = mock(ReminderHandler.class);

    List<ReminderHandler> reminderHandlers =
        ImmutableList.of(
            reminderHandler1,
            reminderHandler2,
            reminderHandler3
        );

    ReminderService reminderService = new ReminderService(reminderHandlers);

    @Test
    public void createReminders() {

        SscsCaseData ccdResponse = mock(SscsCaseData.class);

        when(reminderHandler1.canHandle(ccdResponse)).thenReturn(true);
        when(reminderHandler2.canHandle(ccdResponse)).thenReturn(false);
        when(reminderHandler3.canHandle(ccdResponse)).thenReturn(true);

        reminderService.createReminders(ccdResponse);

        verify(reminderHandler1, times(1)).canHandle(ccdResponse);
        verify(reminderHandler1, times(1)).handle(ccdResponse);

        verify(reminderHandler2, times(1)).canHandle(ccdResponse);
        verify(reminderHandler2, never()).handle(ccdResponse);

        verify(reminderHandler3, times(1)).canHandle(ccdResponse);
        verify(reminderHandler3, times(1)).handle(ccdResponse);
    }

}
