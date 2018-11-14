package uk.gov.hmcts.reform.sscs.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;

import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;

@RunWith(JUnitParamsRunner.class)
public class CcdNotificationWrapperTest {

    private CcdNotificationWrapper ccdNotificationWrapper;

    @Test
    @Parameters({"paper, PAPER", "null, REGULAR", "oral, ORAL", "online, ONLINE"})
    public void should_returnAccordingAppealHearingType_when_hearingTypeIsPresent(String hearingType,
                                                                                  AppealHearingType expected) {
        ccdNotificationWrapper = buildCcdNotificationWrapper(hearingType);

        assertThat(ccdNotificationWrapper.getHearingType(), is(expected));
    }

    private CcdNotificationWrapper buildCcdNotificationWrapper(String hearingType) {
        if ("online".equals(hearingType)) {
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
                            .build()
            );
        }
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
                        .notificationEventType(APPEAL_LAPSED_NOTIFICATION)
                        .build()

        );
    }

    @Test
    public void givenSubscriptions_shouldGetSubscriptionTypeList() {
        //scenarios where a subscription is null can never happen
        // because the SscsCaseDataWrapperDeserializer always builds them
        ccdNotificationWrapper = buildCcdNotificationWrapper("paper");
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        System.out.println(subsWithTypeList);
    }
}