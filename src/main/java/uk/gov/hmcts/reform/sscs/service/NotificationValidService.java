package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
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
    private static final List<NotificationEventType> FALLBACK_LETTER_SUBSCRIPTION_TYPES = Arrays.asList(APPEAL_LODGED, SYA_APPEAL_CREATED_NOTIFICATION, DWP_RESPONSE_RECEIVED_NOTIFICATION);
    private static final String HEARING_TYPE_ONLINE_RESOLUTION = "cor";

    boolean isFallbackLetterRequiredForSubscriptionType(NotificationWrapper wrapper, SubscriptionType subscriptionType, NotificationEventType eventType) {
        boolean result = false;

        if (FALLBACK_LETTER_SUBSCRIPTION_TYPES.contains(eventType)
            && fallbackConditionsMet(wrapper, eventType)
            && (APPELLANT.equals(subscriptionType)
            || APPOINTEE.equals(subscriptionType)
            || (REPRESENTATIVE.equals(subscriptionType) && null != wrapper.getNewSscsCaseData().getAppeal().getRep()))) {
            result = true;
        }

        return result;
    }

    static boolean fallbackConditionsMet(NotificationWrapper wrapper, NotificationEventType eventType) {
        if (FALLBACK_LETTER_SUBSCRIPTION_TYPES.contains(eventType)) {
            return DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(eventType)
                || caseReferenceHasBeenAdded(wrapper);
        }

        return true;
    }

    private static boolean caseReferenceHasBeenAdded(NotificationWrapper wrapper) {
        return (null == wrapper.getOldSscsCaseData() || null == wrapper.getOldSscsCaseData().getCaseReference() || wrapper.getOldSscsCaseData().getCaseReference().isEmpty())
            && (null != wrapper.getNewSscsCaseData().getCaseReference() && !wrapper.getNewSscsCaseData().getCaseReference().isEmpty());
    }

    static final boolean isBundledLetter(NotificationEventType eventType) {
        return STRUCK_OUT.equals(eventType);
    }

    boolean isHearingTypeValidToSendNotification(SscsCaseData sscsCaseData, NotificationEventType eventType) {

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

    private boolean checkHearingIsInFuture(List<Hearing> hearings) {
        if (hearings != null && !hearings.isEmpty()) {

            Hearing latestHearing = hearings.get(0);
            LocalDateTime hearingDateTime = latestHearing.getValue().getHearingDateTime();
            return hearingDateTime.isAfter(LocalDateTime.now());
        } else {
            return false;
        }
    }
}