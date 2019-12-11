package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

public class NotificationServiceForAdminAppealWithdrawalTest extends NotificationServiceBase {

    @Value("${notification.appealWithdrawn.appellant.letterId}")
    private String adminAppealWithdrawalLetterId;

    @Before
    public void setUp() {
        setNotificationService(initialiseNotificationService());
    }

    @Test
    public void adminAppealWithdrawalWhenNoSubscription_shouldSendMandatoryLetter() throws Exception {
        SscsCaseDataWrapper sscsCaseDataWrapper = initTestData();
        sscsCaseDataWrapper.getNewSscsCaseData().setCreatedInGapsFrom(READY_TO_LIST.getId());
        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(sscsCaseDataWrapper));

        verify(getNotificationSender(), times(0)).sendEmail(any(), any(), any(), any(), any(), any());
        verify(getNotificationSender(), times(0))
            .sendSms(any(), any(), any(), any(), any(), any(), any());
        verify(getNotificationHandler(), times(1))
            .sendNotification(any(NotificationWrapper.class), eq(adminAppealWithdrawalLetterId), eq("Letter"),
                any());
    }

    private SscsCaseDataWrapper initTestData() {
        SscsCaseData newSscsCaseData = getSscsCaseData(null);
        newSscsCaseData.setSubscriptions(null);
        newSscsCaseData.getAppeal().setRep(null);
        return getSscsCaseDataWrapper(newSscsCaseData, null, ADMIN_APPEAL_WITHDRAWN);
    }

}
