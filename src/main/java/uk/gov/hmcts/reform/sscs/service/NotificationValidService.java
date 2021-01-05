package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasSubscription;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@Service
public class NotificationValidService {
    protected static final List<NotificationEventType> FALLBACK_LETTER_SUBSCRIPTION_TYPES = Arrays.asList(DWP_RESPONSE_RECEIVED_NOTIFICATION, SYA_APPEAL_CREATED_NOTIFICATION, VALID_APPEAL_CREATED, RESEND_APPEAL_CREATED_NOTIFICATION, DWP_APPEAL_LAPSED_NOTIFICATION);
    protected static final List<NotificationEventType> MANDATORY_LETTER_EVENT_TYPES = Arrays.asList(STRUCK_OUT, DWP_UPLOAD_RESPONSE_NOTIFICATION, ADMIN_APPEAL_WITHDRAWN, APPEAL_WITHDRAWN_NOTIFICATION, APPEAL_RECEIVED_NOTIFICATION, DIRECTION_ISSUED, DIRECTION_ISSUED_WELSH, DECISION_ISSUED, DECISION_ISSUED_WELSH, ISSUE_FINAL_DECISION, ISSUE_ADJOURNMENT_NOTICE, REQUEST_INFO_INCOMPLETE, NON_COMPLIANT_NOTIFICATION, APPEAL_LAPSED_NOTIFICATION, HMCTS_APPEAL_LAPSED_NOTIFICATION, DWP_APPEAL_LAPSED_NOTIFICATION, REVIEW_CONFIDENTIALITY_REQUEST, JOINT_PARTY_ADDED);

    protected static final List<NotificationEventType> LETTER_EVENT_TYPES = Stream.concat(FALLBACK_LETTER_SUBSCRIPTION_TYPES.stream(), MANDATORY_LETTER_EVENT_TYPES. stream()).collect(Collectors.toList());

    protected static final List<NotificationEventType> BUNDLED_LETTER_EVENT_TYPES = Arrays.asList(DIRECTION_ISSUED, DIRECTION_ISSUED_WELSH, DECISION_ISSUED, DECISION_ISSUED_WELSH, ISSUE_FINAL_DECISION, ISSUE_ADJOURNMENT_NOTICE);
    protected static final List<NotificationEventType> INTERLOC_LETTERS = Arrays.asList(DIRECTION_ISSUED, DIRECTION_ISSUED_WELSH, DECISION_ISSUED, DECISION_ISSUED_WELSH, ISSUE_FINAL_DECISION, REQUEST_INFO_INCOMPLETE, NON_COMPLIANT_NOTIFICATION, VALID_APPEAL_CREATED, RESEND_APPEAL_CREATED_NOTIFICATION);
    protected static final List<NotificationEventType> DOCMOSIS_LETTERS = Arrays.asList(STRUCK_OUT, APPEAL_RECEIVED_NOTIFICATION, DIRECTION_ISSUED, DIRECTION_ISSUED_WELSH, DECISION_ISSUED, DECISION_ISSUED_WELSH, ISSUE_FINAL_DECISION, ISSUE_ADJOURNMENT_NOTICE, DWP_UPLOAD_RESPONSE_NOTIFICATION, VALID_APPEAL_CREATED, RESEND_APPEAL_CREATED_NOTIFICATION, REQUEST_INFO_INCOMPLETE, REVIEW_CONFIDENTIALITY_REQUEST, JOINT_PARTY_ADDED, NON_COMPLIANT_NOTIFICATION, APPEAL_LAPSED_NOTIFICATION, DWP_APPEAL_LAPSED_NOTIFICATION, HMCTS_APPEAL_LAPSED_NOTIFICATION);

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
        if (FALLBACK_LETTER_SUBSCRIPTION_TYPES.contains(eventType)) {
            return (null == wrapper.getOldSscsCaseData() || null == wrapper.getOldSscsCaseData().getCaseReference() || wrapper.getOldSscsCaseData().getCaseReference().isEmpty())
                && (null != wrapper.getNewSscsCaseData().getCaseReference() && !wrapper.getNewSscsCaseData().getCaseReference().isEmpty()) || eventType == EVIDENCE_RECEIVED_NOTIFICATION;
        }

        return true;
    }

    static boolean isBundledLetter(NotificationEventType eventType) {
        return BUNDLED_LETTER_EVENT_TYPES.contains(eventType);
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
            String hearingAdjourned = latestHearing.getValue().getAdjourned();
            return hearingDateTime.isAfter(LocalDateTime.now()) && !"YES".equalsIgnoreCase(hearingAdjourned);
        } else {
            return false;
        }
    }
}
