package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

public class NotificationServiceForAdminAppealWithdrawalTest extends NotificationServiceBase {

    @Value("${notification.appealWithdrawn.appellant.letterId}")
    private String adminAppealWithdrawalLetterId;

    @Before
    public void setUp() {

        initialiseNotificationService(false);
    }

    @Test
    public void adminAppealWithdrawalWhenNoSubscription_shouldSendMandatoryLetter() throws Exception {
        SscsCaseData newSscsCaseData = getSscsCaseData(null);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, null, ADMIN_APPEAL_WITHDRAWN);

        initialiseNotificationService(false).manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(getNotificationSender(), times(0)).sendEmail(any(), any(), any(), any(), any(), any());
        verify(getNotificationSender(), times(0))
            .sendSms(any(), any(), any(), any(), any(), any(), any());
    }

}
