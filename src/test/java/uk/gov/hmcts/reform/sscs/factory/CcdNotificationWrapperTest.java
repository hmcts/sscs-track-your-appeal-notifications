package uk.gov.hmcts.reform.sscs.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;

import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

@RunWith(JUnitParamsRunner.class)
public class CcdNotificationWrapperTest {

    private CcdNotificationWrapper ccdNotificationWrapper;

    @Test
    @Parameters({"paper, PAPER", "null, REGULAR", "oral, ORAL", "cor, ONLINE"})
    public void should_returnAccordingAppealHearingType_when_hearingTypeIsPresent(String hearingType,
                                                                                  AppealHearingType expected) {
        ccdNotificationWrapper = buildCcdNotificationWrapper(hearingType);

        assertThat(ccdNotificationWrapper.getHearingType(), is(expected));
    }

    private CcdNotificationWrapper buildCcdNotificationWrapper(String hearingType) {
        return new CcdNotificationWrapper(
                SscsCaseDataWrapper.builder()
                        .newSscsCaseData(SscsCaseData.builder()
                                .appeal(Appeal.builder()
                                        .hearingType(hearingType)
                                        .build())
                                .subscriptions(Subscriptions.builder()
                                        .appellantSubscription(Subscription.builder().build())
                                        .representativeSubscription(Subscription.builder().build())
                                        .build())
                                .build())
                        .build()
        );
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType) {
        return new CcdNotificationWrapper(
            SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                    .appeal(Appeal.builder()
                        .hearingType("cor")
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .representativeSubscription(Subscription.builder().build())
                        .build())
                    .build())
                .notificationEventType(notificationEventType)
                .build()

        );
    }

    @Test
    @Parameters({"APPEAL_LAPSED_NOTIFICATION","APPEAL_WITHDRAWN_NOTIFICATION","SYA_APPEAL_CREATED_NOTIFICATION"})
    public void givenSubscriptions_shouldGetSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters(method = "getEventTypeFilteredOnReps")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithoutReps(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    private Object[] getEventTypeFilteredOnReps() {
        return Arrays.stream(NotificationEventType.values())
            .filter(type -> !(type.equals(APPEAL_LAPSED_NOTIFICATION)
                || type.equals(APPEAL_WITHDRAWN_NOTIFICATION)
                || type.equals(SYA_APPEAL_CREATED_NOTIFICATION))).toArray();
    }

}