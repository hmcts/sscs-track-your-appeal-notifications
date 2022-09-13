package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;
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
    protected static final List<NotificationEventType> MANDATORY_LETTER_EVENT_TYPES = Arrays.asList(
        ACTION_HEARING_RECORDING_REQUEST,
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADMIN_APPEAL_WITHDRAWN,
        APPEAL_LAPSED_NOTIFICATION,
        APPEAL_RECEIVED_NOTIFICATION,
        APPEAL_WITHDRAWN_NOTIFICATION,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED_NOTIFICATION,
        DWP_RESPONSE_RECEIVED_NOTIFICATION,
        DWP_UPLOAD_RESPONSE_NOTIFICATION,
        HEARING_BOOKED_NOTIFICATION,
        HMCTS_APPEAL_LAPSED_NOTIFICATION,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        JOINT_PARTY_ADDED,
        NON_COMPLIANT_NOTIFICATION,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        PROVIDE_APPOINTEE_DETAILS,
        REQUEST_INFO_INCOMPLETE,
        REVIEW_CONFIDENTIALITY_REQUEST,
        STRUCK_OUT,
        UPDATE_OTHER_PARTY_DATA);

    protected static final List<NotificationEventType> BUNDLED_LETTER_EVENT_TYPES = Arrays.asList(
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH);

    protected static final List<NotificationEventType> INTERLOC_LETTERS = Arrays.asList(
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        NON_COMPLIANT_NOTIFICATION,
        REQUEST_INFO_INCOMPLETE,
        RESEND_APPEAL_CREATED_NOTIFICATION,
        VALID_APPEAL_CREATED);
    protected static final List<NotificationEventType> DOCMOSIS_LETTERS = Arrays.asList(
        ACTION_HEARING_RECORDING_REQUEST,
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADMIN_APPEAL_WITHDRAWN,
        APPEAL_LAPSED_NOTIFICATION,
        APPEAL_RECEIVED_NOTIFICATION,
        APPEAL_WITHDRAWN_NOTIFICATION,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED_NOTIFICATION,
        DWP_UPLOAD_RESPONSE_NOTIFICATION,
        HEARING_BOOKED_NOTIFICATION,
        HMCTS_APPEAL_LAPSED_NOTIFICATION,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        JOINT_PARTY_ADDED,
        NON_COMPLIANT_NOTIFICATION,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        PROVIDE_APPOINTEE_DETAILS,
        REQUEST_INFO_INCOMPLETE,
        RESEND_APPEAL_CREATED_NOTIFICATION,
        REVIEW_CONFIDENTIALITY_REQUEST,
        STRUCK_OUT,
        UPDATE_OTHER_PARTY_DATA,
        VALID_APPEAL_CREATED);

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
            if (isNull(hearingDateTime)) {
                hearingDateTime = latestHearing.getValue().getStart();
            }

            String hearingAdjourned = latestHearing.getValue().getAdjourned();
            return LocalDateTime.now().isBefore(hearingDateTime) && !"YES".equalsIgnoreCase(hearingAdjourned);
        } else {
            return false;
        }
    }
}
