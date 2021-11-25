package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

@Service
public class NotificationValidService {
    protected static final List<NotificationEventType> MANDATORY_LETTER_EVENT_TYPES = Arrays.asList(DWP_RESPONSE_RECEIVED_NOTIFICATION, STRUCK_OUT, DWP_UPLOAD_RESPONSE_NOTIFICATION, ADMIN_APPEAL_WITHDRAWN, APPEAL_WITHDRAWN_NOTIFICATION, APPEAL_RECEIVED_NOTIFICATION, PROCESS_AUDIO_VIDEO, PROCESS_AUDIO_VIDEO_WELSH, DIRECTION_ISSUED, DIRECTION_ISSUED_WELSH, DECISION_ISSUED, DECISION_ISSUED_WELSH, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH, ISSUE_ADJOURNMENT_NOTICE, ISSUE_ADJOURNMENT_NOTICE_WELSH, REQUEST_INFO_INCOMPLETE, NON_COMPLIANT_NOTIFICATION, APPEAL_LAPSED_NOTIFICATION, HMCTS_APPEAL_LAPSED_NOTIFICATION, DWP_APPEAL_LAPSED_NOTIFICATION, REVIEW_CONFIDENTIALITY_REQUEST, JOINT_PARTY_ADDED, ACTION_HEARING_RECORDING_REQUEST, ACTION_POSTPONEMENT_REQUEST, ACTION_POSTPONEMENT_REQUEST_WELSH, DEATH_OF_APPELLANT, PROVIDE_APPOINTEE_DETAILS, UPDATE_OTHER_PARTY_DATA);

    protected static final List<NotificationEventType> BUNDLED_LETTER_EVENT_TYPES = Arrays.asList(DIRECTION_ISSUED, DIRECTION_ISSUED_WELSH, DECISION_ISSUED, DECISION_ISSUED_WELSH, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH, ISSUE_ADJOURNMENT_NOTICE, ISSUE_ADJOURNMENT_NOTICE_WELSH, PROCESS_AUDIO_VIDEO, PROCESS_AUDIO_VIDEO_WELSH, ACTION_POSTPONEMENT_REQUEST, ACTION_POSTPONEMENT_REQUEST_WELSH);
    protected static final List<NotificationEventType> INTERLOC_LETTERS = Arrays.asList(DIRECTION_ISSUED, DIRECTION_ISSUED_WELSH, DECISION_ISSUED, DECISION_ISSUED_WELSH, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH, REQUEST_INFO_INCOMPLETE, NON_COMPLIANT_NOTIFICATION, VALID_APPEAL_CREATED, RESEND_APPEAL_CREATED_NOTIFICATION);
    protected static final List<NotificationEventType> DOCMOSIS_LETTERS = Arrays.asList(ADMIN_APPEAL_WITHDRAWN, APPEAL_WITHDRAWN_NOTIFICATION, STRUCK_OUT, APPEAL_RECEIVED_NOTIFICATION, PROCESS_AUDIO_VIDEO, PROCESS_AUDIO_VIDEO_WELSH, DIRECTION_ISSUED, DIRECTION_ISSUED_WELSH, DECISION_ISSUED, DECISION_ISSUED_WELSH, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH, ISSUE_ADJOURNMENT_NOTICE, ISSUE_ADJOURNMENT_NOTICE_WELSH, DWP_UPLOAD_RESPONSE_NOTIFICATION, VALID_APPEAL_CREATED, RESEND_APPEAL_CREATED_NOTIFICATION, REQUEST_INFO_INCOMPLETE, REVIEW_CONFIDENTIALITY_REQUEST, JOINT_PARTY_ADDED, NON_COMPLIANT_NOTIFICATION, APPEAL_LAPSED_NOTIFICATION, DWP_APPEAL_LAPSED_NOTIFICATION, HMCTS_APPEAL_LAPSED_NOTIFICATION, ACTION_HEARING_RECORDING_REQUEST, ACTION_POSTPONEMENT_REQUEST, ACTION_POSTPONEMENT_REQUEST_WELSH, DEATH_OF_APPELLANT, PROVIDE_APPOINTEE_DETAILS, UPDATE_OTHER_PARTY_DATA);

    private static final String HEARING_TYPE_ONLINE_RESOLUTION = "cor";

    static boolean isMandatoryLetterEventType(NotificationEventType eventType) {
        return MANDATORY_LETTER_EVENT_TYPES.contains(eventType);
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
