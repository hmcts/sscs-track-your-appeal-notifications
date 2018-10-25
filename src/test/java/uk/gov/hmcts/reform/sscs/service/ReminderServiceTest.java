package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.reminder.ReminderHandler;

@RunWith(JUnitParamsRunner.class)
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
    @Parameters({
            "STAGING, 0",
            "PRODUCTION, 1",
            "dev , 0"
    })
    public void createReminders(String host, int times) {

        ReflectionTestUtils.setField(reminderService, "slotName",
                host);
        NotificationWrapper wrapper = mock(NotificationWrapper.class);

        when(reminderHandler1.canHandle(wrapper)).thenReturn(true);
        when(reminderHandler2.canHandle(wrapper)).thenReturn(false);
        when(reminderHandler3.canHandle(wrapper)).thenReturn(true);

        reminderService.createReminders(wrapper);

        verify(reminderHandler1, times(times)).canHandle(wrapper);
        verify(reminderHandler1, times(times)).handle(wrapper);

        verify(reminderHandler2, times(times)).canHandle(wrapper);
        verify(reminderHandler2, never()).handle(wrapper);

        verify(reminderHandler3, times(times)).canHandle(wrapper);
        verify(reminderHandler3, times(times)).handle(wrapper);
    }

    @Test
    public void remindersAreNotSetForOnlineHearings() {
        NotificationWrapper notificationWrapper = mock(NotificationWrapper.class);
        when(notificationWrapper.getHearingType()).thenReturn(AppealHearingType.ONLINE);

        reminderService.createReminders(notificationWrapper);

        verifyZeroInteractions(reminderHandler1);
        verifyZeroInteractions(reminderHandler2);
        verifyZeroInteractions(reminderHandler3);
    }
}
