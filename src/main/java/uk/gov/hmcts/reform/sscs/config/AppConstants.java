package uk.gov.hmcts.reform.sscs.config;

import java.time.format.DateTimeFormatter;

public final class AppConstants {

    public static final int MAX_DWP_RESPONSE_DAYS = 35;
    public static final String ACCEPT_VIEW_BY_DATE_LITERAL = "accept_view_by_date";
    public static final String ADDRESS_LINE_LITERAL = "address_line";
    public static final String APPEAL_ID = "appeal_id";
    public static final String APPEAL_ID_LITERAL = "appeal_id";
    public static final String APPEAL_REF = "appeal_ref";
    public static final String APPEAL_RESPOND_DATE = "appeal_respond_date";
    public static final String APPELLANT_NAME = "appellant_name";
    public static final String APPOINTEE_DESCRIPTION = "appointee_description";
    public static final String APPOINTEE_DETAILS_LITERAL = "appointee_details";
    public static final String APPOINTEE_NAME = "appointee_name";
    public static final String BENEFIT_FULL_NAME_LITERAL = "benefit_full_name";
    public static final String BENEFIT_NAME_ACRONYM_LITERAL = "benefit_name_acronym";
    public static final String BENEFIT_NAME_ACRONYM_SHORT_LITERAL = "benefit_name_acronym_short";
    public static final DateTimeFormatter CC_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String CCD_ID = "ccd_id";
    public static final String CASE_REFERENCE_ID = "case_reference_id";   // For when the SC Ref may be empty, so use CCD_ID
    public static final String CLAIMING_EXPENSES_LINK_LITERAL = "claiming_expenses_link";
    public static final String CLAIMANT_NAME = "claimant_name";
    public static final String COUNTY_LITERAL = "county";
    public static final String DAYS_STRING = " days";
    public static final String DAYS_TO_HEARING_LITERAL = "days_to_hearing_text";
    public static final String DECISION_POSTED_RECEIVE_DATE = "decision_posted_receive_date";
    public static final String DWP_ACRONYM = "DWP";
    public static final String DWP_FUL_NAME = "Department for Work and Pensions";
    public static final String ESA_PANEL_COMPOSITION = "judge and a doctor";
    public static final String EVIDENCE_RECEIVED_DATE_LITERAL = "evidence_received_date";
    public static final String FIRST_TIER_AGENCY_ACRONYM = "first_tier_agency_acronym";
    public static final String FIRST_TIER_AGENCY_FULL_NAME = "first_tier_agency_full_name";
    public static final String HEARING_ARRANGEMENT_DETAILS_LITERAL = "hearing_arrangement_details";
    public static final String HEARING_CONTACT_DATE = "hearing_contact_date";
    public static final String HEARING_DATE = "hearing_date";
    public static final String HEARING_DETAILS_LITERAL = "hearing_details";
    public static final String HEARING_INFO_LINK_LITERAL = "hearing_info_link";
    public static final String HEARING_TIME = "hearing_time";
    public static final String HEARING_TIME_FORMAT = "hh:mm a";
    public static final String INFO_REQUEST_DETAIL = "info_request_detail";
    public static final String MAC_ALGO = "HmacSHA256";
    public static final String MAC_LITERAL = "mac";
    public static final String MANAGE_EMAILS_LINK_LITERAL = "manage_emails_link";
    public static final String MRN_DETAILS_LITERAL = "mrn_details";
    public static final String NAME = "name";
    public static final String ONLINE_HEARING_LINK_LITERAL = "online_hearing_link";
    public static final String ONLINE_HEARING_REGISTER_LINK_LITERAL = "online_hearing_register_link";
    public static final String ONLINE_HEARING_SIGN_IN_LINK_LITERAL = "online_hearing_sign_in_link";
    public static final String PANEL_COMPOSITION = "panel_composition";
    public static final String PHONE_NUMBER = "phone_number";
    public static final String PIP_PANEL_COMPOSITION = "judge, doctor and disability expert";
    public static final String POSTCODE_LITERAL = "postcode";
    public static final String QUESTION_ROUND_EXPIRES_DATE_LITERAL = "question_round_expires_date";
    public static final String REASONS_FOR_APPEALING_DETAILS_LITERAL = "reasons_for_appealing_details";
    public static final String REGIONAL_OFFICE_NAME_LITERAL = "regional_office_name";
    public static final String REGIONAL_OFFICE_POSTCODE_LITERAL = "regional_office_postcode";
    public static final String REPRESENTATIVE_DETAILS_LITERAL = "representative_details";
    public static final String REPRESENTATIVE_NAME = "representative_name";
    public static final String RESPONSE_DATE_FORMAT = "d MMMM yyyy";
    public static final String SUBMIT_EVIDENCE_INFO_LINK_LITERAL = "submit_evidence_info_link";
    public static final String SUBMIT_EVIDENCE_LINK_LITERAL = "submit_evidence_link";
    public static final String SUPPORT_CENTRE_NAME_LITERAL = "support_centre_name";
    public static final String TEXT_MESSAGE_REMINDER_DETAILS_LITERAL = "text_message_reminder_details";
    public static final String TOMORROW_STRING = "tomorrow";
    public static final String TOWN_LITERAL = "town";
    public static final String TRACK_APPEAL_LINK_LITERAL = "track_appeal_link";
    public static final String TRIBUNAL_RESPONSE_DATE_LITERAL = "tribunal_response_date";
    public static final String UC_PANEL_COMPOSITION = "judge, doctor and disability expert (if applicable)";
    public static final String VENUE_ADDRESS_LITERAL = "venue_address";
    public static final String VENUE_MAP_LINK_LITERAL = "venue_map_link";
    public static final String YOUR_DETAILS_LITERAL = "your_details";
    public static final String ZONE_ID = "Europe/London";

    public static final String ADDRESS_NAME = "address_name";
    public static final String ADDRESS_LINE_1 = "address_line_1";
    public static final String ADDRESS_LINE_2 = "address_line_2";
    public static final String ADDRESS_LINE_3 = "address_line_3";
    public static final String ADDRESS_LINE_4 = "address_line_4";

    public static final String REP_SALUTATION = "Sir / Madam";
    public static final String LETTER_ADDRESS_LINE_1 = "letter_address_line_1";
    public static final String LETTER_ADDRESS_LINE_2 = "letter_address_line_2";
    public static final String LETTER_ADDRESS_LINE_3 = "letter_address_line_3";
    public static final String LETTER_ADDRESS_LINE_4 = "letter_address_line_4";
    public static final String LETTER_ADDRESS_POSTCODE = "letter_address_postcode";
    public static final String LETTER_NAME = "letter_name";

    private AppConstants() {
        //
    }
}
