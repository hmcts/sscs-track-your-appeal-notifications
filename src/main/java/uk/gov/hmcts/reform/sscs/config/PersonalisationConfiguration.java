package uk.gov.hmcts.reform.sscs.config;

import java.util.Arrays;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;

@Getter
@Setter
@Configuration
@ConfigurationProperties
public class PersonalisationConfiguration {
    private static final String ERROR_MESSAGE = "No text found for %s";

    public Map<LanguagePreference, Map<PersonalisationKey, String>> personalisation;

    public enum PersonalisationKey {
        ATTENDING_HEARING("attendingHearing"),
        YES("yes"),
        NO("no"),
        DATES_NOT_ATTENDING("datesNotAtteding"),
        DATE_OF_MRN("dateOfMrn"),
        REASON_FOR_LATE_APPEAL("reasonForLateAppeal"),
        REASON_FOR_NO_MRN("reasonForNoMrn"),
        NAME("name"),
        DATE_OF_BIRTH("dateOfBirth"),
        NINO("nino"),
        ADDRESS("address"),
        EMAIL("email"),
        PHONE("phone"),
        RECEIVE_TEXT_MESSAGE_REMINDER("receiveTextMessageReminder"),
        MOBILE("mobile"),
        HAVE_AN_APPOINTEE("haveAnAppointee"),
        NOT_PROVIDED("notProvided"),
        HAVE_A_REPRESENTATIVE("haveARepresentative"),
        ORGANISATION("organisation"),
        WHAT_DISAGREE_WITH("whatDisagreeWith"),
        WHY_DISAGREE_WITH("whyDisagreeWith"),
        ANYTHING("anything"),
        LANGUAGE_INTERPRETER("LanguageInterpreter"),
        SIGN_INTERPRETER("signInterpreter"),
        HEARING_LOOP("hearingLoop"),
        DISABLED_ACCESS("disabledAccess"),
        OTHER_ARRANGEMENTS("otherArrangements"),
        REQUIRED("required"),
        NOT_REQUIRED("notRequired"),
        OTHER("other");

        private final String value;
        PersonalisationKey(String value) {
            this.value = value;
        }

        public static PersonalisationKey getPersonalisationKey(String keyValue) {
            return Arrays.stream(PersonalisationKey.values()).filter(personalisationKey -> personalisationKey.value.equals(keyValue)).findAny()
                    .orElseThrow(() -> new IllegalArgumentException(String.format(ERROR_MESSAGE, keyValue)));
        }
    }
}
