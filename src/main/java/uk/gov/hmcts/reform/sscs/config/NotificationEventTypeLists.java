package uk.gov.hmcts.reform.sscs.config;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.EnumSet;
import java.util.Set;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

public final class NotificationEventTypeLists {

    private NotificationEventTypeLists() {

    }

    public static final Set<NotificationEventType> EVENTS_TO_HANDLE = EnumSet.of(
        ADJOURNED,
        ADMIN_APPEAL_WITHDRAWN,
        ADMIN_CORRECTION_HEADER,
        APPEAL_DORMANT,
        APPEAL_LAPSED,
        APPEAL_RECEIVED,
        APPEAL_WITHDRAWN,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        DECISION_ISSUED,
        DIRECTION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        DWP_UPLOAD_RESPONSE,
        EVIDENCE_RECEIVED,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        NON_COMPLIANT,
        DRAFT_TO_NON_COMPLIANT,
        PERMISSION_TO_APPEAL_GRANTED,
        PERMISSION_TO_APPEAL_REFUSED,
        POST_HEARING_APP_SOR_WRITTEN,
        POSTPONEMENT,
        REISSUE_DOCUMENT,
        REQUEST_FOR_INFORMATION,
        RESEND_APPEAL_CREATED,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        STRUCK_OUT,
        SUBSCRIPTION_UPDATED,
        VALID_APPEAL_CREATED,
        DRAFT_TO_VALID_APPEAL_CREATED,
        REVIEW_CONFIDENTIALITY_REQUEST,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        JOINT_PARTY_ADDED,
        ACTION_HEARING_RECORDING_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        UPDATE_OTHER_PARTY_DATA
    );

    // TODO move to EVENTS_TO_HANDLE when feature.postHearings.enabled removed
    public static final Set<NotificationEventType> EVENTS_TO_HANDLE_POSTHEARINGS_A = EnumSet.of(
        CORRECTION_REQUEST,
        SET_ASIDE_REQUEST,
        STATEMENT_OF_REASONS_REQUEST
    );

    // move to EVENTS_TO_HANDLE when feature.postHearingsB.enabled removed
    public static final Set<NotificationEventType> EVENTS_TO_HANDLE_POSTHEARINGS_B = EnumSet.of(
        LIBERTY_TO_APPLY_REQUEST,
        REVIEW_AND_SET_ASIDE,
        PERMISSION_TO_APPEAL_GRANTED,
        PERMISSION_TO_APPEAL_REFUSED,
        PERMISSION_TO_APPEAL_REQUEST,
        BUNDLE_CREATED_FOR_UPPER_TRIBUNAL
    );

    public static final Set<NotificationEventType> EVENT_TYPES_FOR_DORMANT_CASES = EnumSet.of(
        ADMIN_APPEAL_WITHDRAWN,
        ADMIN_CORRECTION_HEADER,
        APPEAL_DORMANT,
        APPEAL_LAPSED,
        APPEAL_WITHDRAWN,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        BUNDLE_CREATED_FOR_UPPER_TRIBUNAL,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        PERMISSION_TO_APPEAL_GRANTED,
        PERMISSION_TO_APPEAL_REFUSED,
        POST_HEARING_APP_SOR_WRITTEN,
        PROVIDE_APPOINTEE_DETAILS,
        REISSUE_DOCUMENT,
        REVIEW_AND_SET_ASIDE,
        SOR_REFUSED,
        STRUCK_OUT,
        LIBERTY_TO_APPLY_REFUSED,
        SET_ASIDE_REFUSED,
        SOR_REFUSED
        );

    public static final Set<NotificationEventType> EVENT_TYPES_NOT_FOR_WELSH_CASES = EnumSet.of(
        ACTION_POSTPONEMENT_REQUEST,
        DECISION_ISSUED,
        DIRECTION_ISSUED,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_FINAL_DECISION,
        PROCESS_AUDIO_VIDEO
    );

    public static final Set<NotificationEventType> EVENT_TYPES_FOR_MANDATORY_LETTERS = EnumSet.of(
        ACTION_HEARING_RECORDING_REQUEST,
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADMIN_APPEAL_WITHDRAWN,
        ADMIN_CORRECTION_HEADER,
        APPEAL_LAPSED,
        APPEAL_RECEIVED,
        APPEAL_WITHDRAWN,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        BUNDLE_CREATED_FOR_UPPER_TRIBUNAL,
        CORRECTION_REQUEST,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        DWP_UPLOAD_RESPONSE,
        HEARING_BOOKED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        JOINT_PARTY_ADDED,
        LIBERTY_TO_APPLY_REQUEST,
        NON_COMPLIANT,
        PERMISSION_TO_APPEAL_GRANTED,
        PERMISSION_TO_APPEAL_REFUSED,
        PERMISSION_TO_APPEAL_REQUEST,
        POST_HEARING_APP_SOR_WRITTEN,
        POSTPONEMENT,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        PROVIDE_APPOINTEE_DETAILS,
        REQUEST_FOR_INFORMATION,
        REVIEW_AND_SET_ASIDE,
        REVIEW_CONFIDENTIALITY_REQUEST,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        SET_ASIDE_REQUEST,
        STRUCK_OUT,
        STATEMENT_OF_REASONS_REQUEST,
        UPDATE_OTHER_PARTY_DATA
    );

    public static final Set<NotificationEventType> EVENT_TYPES_FOR_BUNDLED_LETTER = EnumSet.of(
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADMIN_CORRECTION_HEADER,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        POST_HEARING_APP_SOR_WRITTEN,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        SOR_EXTEND_TIME,
        SOR_REFUSED
    );

    public static final Set<NotificationEventType> EVENT_TYPES_FOR_INTERLOC_LETTERS = EnumSet.of(
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        NON_COMPLIANT,
        REQUEST_FOR_INFORMATION,
        RESEND_APPEAL_CREATED,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> DOCMOSIS_LETTERS = EnumSet.of(
        ACTION_HEARING_RECORDING_REQUEST,
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADMIN_APPEAL_WITHDRAWN,
        ADMIN_CORRECTION_HEADER,
        APPEAL_LAPSED,
        APPEAL_RECEIVED,
        APPEAL_WITHDRAWN,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        BUNDLE_CREATED_FOR_UPPER_TRIBUNAL,
        CORRECTION_REQUEST,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_UPLOAD_RESPONSE,
        HEARING_BOOKED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        JOINT_PARTY_ADDED,
        LIBERTY_TO_APPLY_REQUEST,
        NON_COMPLIANT,
        PERMISSION_TO_APPEAL_GRANTED,
        PERMISSION_TO_APPEAL_REFUSED,
        PERMISSION_TO_APPEAL_REQUEST,
        POST_HEARING_APP_SOR_WRITTEN,
        POSTPONEMENT,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        PROVIDE_APPOINTEE_DETAILS,
        REQUEST_FOR_INFORMATION,
        RESEND_APPEAL_CREATED,
        REVIEW_AND_SET_ASIDE,
        REVIEW_CONFIDENTIALITY_REQUEST,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        SET_ASIDE_REQUEST,
        STATEMENT_OF_REASONS_REQUEST,
        STRUCK_OUT,
        UPDATE_OTHER_PARTY_DATA,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> EVENT_TYPES_FOR_NOTIFY_LETTERS = EnumSet.of(
        APPEAL_RECEIVED,
        DWP_RESPONSE_RECEIVED,
        DWP_UPLOAD_RESPONSE,
        EVIDENCE_RECEIVED,
        JUDGE_DECISION_APPEAL_TO_PROCEED,
        NON_COMPLIANT,
        SYA_APPEAL_CREATED,
        TCW_DECISION_APPEAL_TO_PROCEED,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> EVENTS_VALID_FOR_ALL_ENTITIES = EnumSet.of(
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADJOURNED,
        ADMIN_APPEAL_WITHDRAWN,
        ADMIN_CORRECTION_HEADER,
        APPEAL_DORMANT,
        APPEAL_LAPSED,
        APPEAL_WITHDRAWN,
        BUNDLE_CREATED_FOR_UPPER_TRIBUNAL,
        CORRECTION_REQUEST,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_UPLOAD_RESPONSE,
        EVIDENCE_RECEIVED,
        EVIDENCE_REMINDER,
        HEARING_BOOKED,
        HEARING_REMINDER,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        POST_HEARING_APP_SOR_WRITTEN,
        LIBERTY_TO_APPLY_REQUEST,
        PERMISSION_TO_APPEAL_GRANTED,
        PERMISSION_TO_APPEAL_REFUSED,
        PERMISSION_TO_APPEAL_REQUEST,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        POSTPONEMENT,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        REVIEW_AND_SET_ASIDE,
        SET_ASIDE_REQUEST,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        STATEMENT_OF_REASONS_REQUEST,
        STRUCK_OUT
    );

    public static final Set<NotificationEventType> EVENTS_VALID_FOR_APPOINTEE = EnumSet.of(
        ADMIN_CORRECTION_HEADER,
        APPEAL_RECEIVED,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        JOINT_PARTY_ADDED,
        JUDGE_DECISION_APPEAL_TO_PROCEED,
        NON_COMPLIANT,
        POST_HEARING_APP_SOR_WRITTEN,
        PROVIDE_APPOINTEE_DETAILS,
        RESEND_APPEAL_CREATED,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        SYA_APPEAL_CREATED,
        SUBSCRIPTION_UPDATED,
        TCW_DECISION_APPEAL_TO_PROCEED,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> EVENTS_VALID_FOR_REP = EnumSet.of(
        ADMIN_CORRECTION_HEADER,
        APPEAL_RECEIVED,
        CASE_UPDATED,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        JUDGE_DECISION_APPEAL_TO_PROCEED,
        NON_COMPLIANT,
        POST_HEARING_APP_SOR_WRITTEN,
        PROVIDE_APPOINTEE_DETAILS,
        RESEND_APPEAL_CREATED,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        SUBSCRIPTION_UPDATED,
        SYA_APPEAL_CREATED,
        TCW_DECISION_APPEAL_TO_PROCEED,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> EVENTS_VALID_FOR_JOINT_PARTY = EnumSet.of(
        ADMIN_CORRECTION_HEADER,
        JOINT_PARTY_ADDED,
        POST_HEARING_APP_SOR_WRITTEN,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        SOR_EXTEND_TIME,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        SOR_REFUSED
    );

    public static final Set<NotificationEventType> EVENTS_VALID_FOR_OTHER_PARTY = EnumSet.of(
        ADMIN_CORRECTION_HEADER,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        NON_COMPLIANT,
        POST_HEARING_APP_SOR_WRITTEN,
        REQUEST_FOR_INFORMATION,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_OLD,
        SUBSCRIPTION_UPDATED
    );

    // Special list of notifications that might not be sent to appellant, depending on data set on the case
    public static final Set<NotificationEventType> EVENTS_MAYBE_INVALID_FOR_APPELLANT = EnumSet.of(
        ACTION_HEARING_RECORDING_REQUEST,
        REQUEST_FOR_INFORMATION,
        REVIEW_CONFIDENTIALITY_REQUEST,
        UPDATE_OTHER_PARTY_DATA
    );

    public static final Set<NotificationEventType> EVENTS_WITH_SUBSCRIPTION_TYPE_EMAIL_TEMPLATES = EnumSet.of(
        ADJOURNED,
        ADMIN_APPEAL_WITHDRAWN,
        APPEAL_DORMANT,
        APPEAL_LAPSED,
        APPEAL_RECEIVED,
        APPEAL_WITHDRAWN,
        CASE_UPDATED,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        DWP_UPLOAD_RESPONSE,
        EVIDENCE_RECEIVED,
        EVIDENCE_REMINDER,
        HEARING_BOOKED,
        HEARING_REMINDER,
        HMCTS_APPEAL_LAPSED,
        POSTPONEMENT,
        RESEND_APPEAL_CREATED,
        SYA_APPEAL_CREATED,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> EVENTS_WITH_SUBSCRIPTION_TYPE_DOCMOSIS_TEMPLATES = EnumSet.of(
        ACTION_HEARING_RECORDING_REQUEST,
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADMIN_APPEAL_WITHDRAWN,
        ADMIN_CORRECTION_HEADER,
        APPEAL_LAPSED,
        APPEAL_RECEIVED,
        APPEAL_WITHDRAWN,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        HEARING_BOOKED,
        HMCTS_APPEAL_LAPSED,
        POSTPONEMENT,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        JOINT_PARTY_ADDED,
        PROVIDE_APPOINTEE_DETAILS,
        REQUEST_FOR_INFORMATION,
        REVIEW_CONFIDENTIALITY_REQUEST,
        UPDATE_OTHER_PARTY_DATA,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> EVENTS_FOR_SYA_PERSONALISATION = EnumSet.of(
        APPEAL_RECEIVED,
        CASE_UPDATED,
        RESEND_APPEAL_CREATED,
        SYA_APPEAL_CREATED,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> EVENTS_FOR_ACTION_FURTHER_EVIDENCE = EnumSet.of(
        CORRECTION_REQUEST,
        LIBERTY_TO_APPLY_REQUEST,
        PERMISSION_TO_APPEAL_REQUEST,
        SET_ASIDE_REQUEST,
        STATEMENT_OF_REASONS_REQUEST
    );

    public static final Set<NotificationEventType> EVENTS_FOR_REPRESENTATIVE_PERSONALISATION = EnumSet.of(
        ACTION_HEARING_RECORDING_REQUEST,
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADJOURNED,
        ADMIN_APPEAL_WITHDRAWN,
        ADMIN_CORRECTION_HEADER,
        APPEAL_DORMANT,
        APPEAL_LAPSED,
        APPEAL_WITHDRAWN,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        DWP_UPLOAD_RESPONSE,
        EVIDENCE_RECEIVED,
        EVIDENCE_REMINDER,
        HEARING_BOOKED,
        HEARING_REMINDER,
        HMCTS_APPEAL_LAPSED,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        LIBERTY_TO_APPLY_GRANTED,
        LIBERTY_TO_APPLY_REFUSED,
        NON_COMPLIANT,
        POST_HEARING_APP_SOR_WRITTEN,
        POSTPONEMENT,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        PROVIDE_APPOINTEE_DETAILS,
        REQUEST_FOR_INFORMATION,
        SET_ASIDE_GRANTED,
        SET_ASIDE_REFUSED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        STRUCK_OUT
    );
}
