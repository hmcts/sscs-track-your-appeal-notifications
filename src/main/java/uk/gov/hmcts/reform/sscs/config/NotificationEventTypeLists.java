package uk.gov.hmcts.reform.sscs.config;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ACTION_HEARING_RECORDING_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ACTION_POSTPONEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ACTION_POSTPONEMENT_REQUEST_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADJOURNED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_DORMANT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DEATH_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DECISION_ISSUED_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DRAFT_TO_NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DRAFT_TO_VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_APPEAL_LAPSED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_BOOKED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HEARING_REMINDER;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.HMCTS_APPEAL_LAPSED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_ADJOURNMENT_NOTICE_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.JOINT_PARTY_ADDED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.JUDGE_DECISION_APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.POSTPONEMENT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.PROCESS_AUDIO_VIDEO;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.PROCESS_AUDIO_VIDEO_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.PROVIDE_APPOINTEE_DETAILS;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REISSUE_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REQUEST_FOR_INFORMATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.RESEND_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REVIEW_CONFIDENTIALITY_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SOR_EXTEND_TIME;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SOR_REFUSED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_CREATED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_OLD;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.TCW_DECISION_APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.VALID_APPEAL_CREATED;

import java.util.EnumSet;
import java.util.Set;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

public final class NotificationEventTypeLists {

    private NotificationEventTypeLists() {

    }

    public static final Set<NotificationEventType> EVENTS_TO_HANDLE = EnumSet.of(
        ADJOURNED,
        ADMIN_APPEAL_WITHDRAWN,
        APPEAL_DORMANT,
        APPEAL_LAPSED,
        APPEAL_RECEIVED,
        APPEAL_WITHDRAWN,
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
        NON_COMPLIANT,
        DRAFT_TO_NON_COMPLIANT,
        POSTPONEMENT,
        REISSUE_DOCUMENT,
        REQUEST_FOR_INFORMATION,
        RESEND_APPEAL_CREATED,
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

    public static final Set<NotificationEventType> EVENT_TYPES_NOT_FOR_DORMANT_CASES = EnumSet.of(
        ADMIN_APPEAL_WITHDRAWN,
        APPEAL_DORMANT,
        APPEAL_LAPSED,
        APPEAL_WITHDRAWN,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        PROVIDE_APPOINTEE_DETAILS,
        REISSUE_DOCUMENT,
        STRUCK_OUT
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
        APPEAL_LAPSED,
        APPEAL_RECEIVED,
        APPEAL_WITHDRAWN,
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
        JOINT_PARTY_ADDED,
        NON_COMPLIANT,
        POSTPONEMENT,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        PROVIDE_APPOINTEE_DETAILS,
        REQUEST_FOR_INFORMATION,
        REVIEW_CONFIDENTIALITY_REQUEST,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        STRUCK_OUT,
        UPDATE_OTHER_PARTY_DATA
    );

    public static final Set<NotificationEventType> EVENT_TYPES_FOR_BUNDLED_LETTER = EnumSet.of(
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
        PROCESS_AUDIO_VIDEO_WELSH,
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
        APPEAL_LAPSED,
        APPEAL_RECEIVED,
        APPEAL_WITHDRAWN,
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
        JOINT_PARTY_ADDED,
        NON_COMPLIANT,
        POSTPONEMENT,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        PROVIDE_APPOINTEE_DETAILS,
        REQUEST_FOR_INFORMATION,
        RESEND_APPEAL_CREATED,
        REVIEW_CONFIDENTIALITY_REQUEST,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
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
        APPEAL_DORMANT,
        APPEAL_LAPSED,
        APPEAL_WITHDRAWN,
        DIRECTION_ISSUED,
        DIRECTION_ISSUED_WELSH,
        DWP_UPLOAD_RESPONSE,
        EVIDENCE_RECEIVED,
        EVIDENCE_REMINDER,
        HEARING_BOOKED,
        HEARING_REMINDER,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH,
        POSTPONEMENT,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        STRUCK_OUT
    );

    public static final Set<NotificationEventType> EVENTS_VALID_FOR_APPOINTEE = EnumSet.of(
        APPEAL_RECEIVED,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        JOINT_PARTY_ADDED,
        JUDGE_DECISION_APPEAL_TO_PROCEED,
        NON_COMPLIANT,
        PROVIDE_APPOINTEE_DETAILS,
        RESEND_APPEAL_CREATED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        SYA_APPEAL_CREATED,
        SUBSCRIPTION_UPDATED,
        TCW_DECISION_APPEAL_TO_PROCEED,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> EVENTS_VALID_FOR_REP = EnumSet.of(
        APPEAL_RECEIVED,
        CASE_UPDATED,
        DEATH_OF_APPELLANT,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        JUDGE_DECISION_APPEAL_TO_PROCEED,
        NON_COMPLIANT,
        PROVIDE_APPOINTEE_DETAILS,
        RESEND_APPEAL_CREATED,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        SUBSCRIPTION_UPDATED,
        SYA_APPEAL_CREATED,
        TCW_DECISION_APPEAL_TO_PROCEED,
        VALID_APPEAL_CREATED
    );

    public static final Set<NotificationEventType> EVENTS_VALID_FOR_JOINT_PARTY = EnumSet.of(
        JOINT_PARTY_ADDED,
        SOR_EXTEND_TIME,
        SOR_REFUSED
    );

    public static final Set<NotificationEventType> EVENTS_VALID_FOR_OTHER_PARTY = EnumSet.of(
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_RESPONSE_RECEIVED,
        HMCTS_APPEAL_LAPSED,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        NON_COMPLIANT,
        REQUEST_FOR_INFORMATION,
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
        APPEAL_LAPSED,
        APPEAL_RECEIVED,
        APPEAL_WITHDRAWN,
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

    public static final Set<NotificationEventType> EVENTS_FOR_REPRESENTATIVE_PERSONALISATION = EnumSet.of(
        ACTION_HEARING_RECORDING_REQUEST,
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADJOURNED,
        ADMIN_APPEAL_WITHDRAWN,
        APPEAL_DORMANT,
        APPEAL_LAPSED,
        APPEAL_WITHDRAWN,
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
        NON_COMPLIANT,
        POSTPONEMENT,
        PROCESS_AUDIO_VIDEO,
        PROCESS_AUDIO_VIDEO_WELSH,
        PROVIDE_APPOINTEE_DETAILS,
        REQUEST_FOR_INFORMATION,
        SOR_EXTEND_TIME,
        SOR_REFUSED,
        STRUCK_OUT
    );

}
