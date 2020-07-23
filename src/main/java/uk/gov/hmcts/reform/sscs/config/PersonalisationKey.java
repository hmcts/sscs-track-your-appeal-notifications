package uk.gov.hmcts.reform.sscs.config;

import java.util.Optional;

public enum PersonalisationKey {
    ATTENDING_HEARING,
    YES,
    NO,
    DATES_NOT_ATTENDING,
    DATE_OF_MRN,
    REASON_FOR_LATE_APPEAL,
    REASON_FOR_NO_MRN,
    NAME,
    DATE_OF_BIRTH,
    NINO,
    ADDRESS,
    EMAIL,
    PHONE,
    RECEIVE_TEXT_MESSAGE_REMINDER,
    MOBILE,
    HAVE_AN_APPOINTEE,
    NOT_PROVIDED,
    HAVE_A_REPRESENTATIVE,
    ORGANISATION,
    WHAT_DISAGREE_WITH,
    WHY_DISAGREE_WITH,
    ANYTHING,
    LANGUAGE_INTERPRETER,
    SIGN_INTERPRETER,
    HEARING_LOOP,
    DISABLED_ACCESS,
    OTHER_ARRANGEMENTS,
    REQUIRED,
    NOT_REQUIRED,
    OTHER;

    public static String getYesNoKey(String value) {
        return Optional.ofNullable(value).filter(data -> "YES".equals(data.toUpperCase())).map(data -> YES.name()).orElse(NO.name());
    }
}
