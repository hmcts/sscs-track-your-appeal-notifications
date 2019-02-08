package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.INTERLOC_VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasSubscription;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@Service
public class NotificationValidService {
    private static final List<NotificationEventType> MANDATORY_LETTER_EVENT_TYPES = Arrays.asList(STRUCK_OUT, HEARING_BOOKED_NOTIFICATION);
    private static final List<NotificationEventType> FALLBACK_LETTER_SUBSCRIPTION_TYPES = Arrays.asList(CASE_UPDATED, SYA_APPEAL_CREATED_NOTIFICATION);
    private static final String HEARING_TYPE_ONLINE_RESOLUTION = "cor";

    static boolean isMandatoryLetterEventType(NotificationEventType eventType) {
        return MANDATORY_LETTER_EVENT_TYPES.contains(eventType);
    }

    boolean isFallbackLetterRequiredForSubscriptionType(NotificationWrapper wrapper, SubscriptionType subscriptionType, NotificationEventType eventType) {
        boolean result = false;

        if (FALLBACK_LETTER_SUBSCRIPTION_TYPES.contains(eventType)
            && hasSubscription(wrapper, subscriptionType)
            && fallbackConditionsMet(wrapper, eventType)) {
            result = true;
        }

        return result;
    }

    static boolean fallbackConditionsMet(NotificationWrapper wrapper, NotificationEventType eventType) {
        if (SYA_APPEAL_CREATED_NOTIFICATION.equals(eventType)) {
            return (null == wrapper.getOldSscsCaseData() || wrapper.getOldSscsCaseData().getCaseReference().isEmpty())
                && !wrapper.getNewSscsCaseData().getCaseReference().isEmpty();
        }

        return true;
    }

    static boolean isBundledLetter(NotificationEventType eventType) {
        return STRUCK_OUT.equals(eventType);
    }

    protected boolean isHearingTypeValidToSendNotification(SscsCaseData sscsCaseData, NotificationEventType eventType) {
        boolean isOralCase = sscsCaseData.getAppeal().getHearingOptions().isWantsToAttendHearing();
        boolean isOnlineHearing = HEARING_TYPE_ONLINE_RESOLUTION.equalsIgnoreCase(sscsCaseData.getAppeal().getHearingType());

        if (isOralCase && !isOnlineHearing && eventType.isSendForOralCase()) {
            return true;
        } else if (!isOralCase && !isOnlineHearing && eventType.isSendForPaperCase()) {
            return true;
        } else {
            return isOnlineHearing && eventType.isSendForCohCase();
        }
    }

    boolean isNotificationStillValidToSend(List<Hearing> hearings, NotificationEventType eventType) {
        switch (eventType) {
            case HEARING_BOOKED_NOTIFICATION:
                return checkHearingIsInFuture(hearings);
            case HEARING_REMINDER_NOTIFICATION:
                return checkHearingIsInFuture(hearings);
            default:
                return true;
        }
    }

    boolean checkHearingIsInFuture(List<Hearing> hearings) {
        if (hearings != null && !hearings.isEmpty()) {

            Hearing latestHearing = hearings.get(0);
            LocalDateTime hearingDateTime = latestHearing.getValue().getHearingDateTime();
            return hearingDateTime.isAfter(LocalDateTime.now());
        } else {
            return false;
        }
    }
}