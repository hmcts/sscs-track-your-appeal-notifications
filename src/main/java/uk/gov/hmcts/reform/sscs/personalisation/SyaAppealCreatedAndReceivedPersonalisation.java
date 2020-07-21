package uk.gov.hmcts.reform.sscs.personalisation;

import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.ANYTHING;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.DATE_OF_BIRTH;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.DATE_OF_MRN;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.DISABLED_ACCESS;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.HAVE_AN_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.HAVE_A_REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.HEARING_LOOP;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.LANGUAGE_INTERPRETER;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.MOBILE;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.NINO;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.ORGANISATION;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.OTHER_ARRANGEMENTS;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.REASON_FOR_LATE_APPEAL;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.REASON_FOR_NO_MRN;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.RECEIVE_TEXT_MESSAGE_REMINDER;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.SIGN_INTERPRETER;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.WHAT_DISAGREE_WITH;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.WHY_DISAGREE_WITH;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.getPersonalisationKey;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration;
import uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Component
public class SyaAppealCreatedAndReceivedPersonalisation extends WithRepresentativePersonalisation {

    private static final String NOT_PROVIDED = "Not provided";
    private static final String YES = "yes";
    private static final String NO = "no";
    static final String TWO_NEW_LINES = "\n\n";
    static final String NOT_REQUIRED = "Not required";
    static final String REQUIRED = "Required";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter longFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");

    @Autowired
    private PersonalisationConfiguration personalisationConfiguration;

    @Override
    protected Map<String, String> create(SscsCaseDataWrapper responseWrapper, SubscriptionWithType subscriptionWithType) {
        Map<String, String> personalisation = super.create(responseWrapper, subscriptionWithType);
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();

        setMrnDetails(personalisation, ccdResponse);
        setYourDetails(personalisation, ccdResponse);
        setAppointeeName(personalisation, ccdResponse);
        setAppointeeDetails(personalisation, ccdResponse);
        setTextMessageReminderDetails(personalisation, subscriptionWithType.getSubscription());
        setRepresentativeDetails(personalisation, ccdResponse);
        setReasonsForAppealingDetails(personalisation, ccdResponse);
        setHearingDetails(personalisation, ccdResponse);
        setHearingArrangementDetails(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, String> setMrnDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.MRN_DETAILS_LITERAL,
                buildMrnDetails(ccdResponse.getAppeal().getMrnDetails(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH),  Function.identity()));

        if(ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_MRN_DETAILS_LITERAL,
                    buildMrnDetails(ccdResponse.getAppeal().getMrnDetails(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH), this::convertLongFormattedLocalDateToWelshDate));
        }
        return personalisation;
    }

    private String buildMrnDetails(MrnDetails mrnDetails, Map<PersonalisationKey, String> titleText, Function<String, String> mrnDate) {

        List<String> details = new ArrayList<>();

        if (mrnDetails.getMrnDate() != null) {
            details.add( titleText.get(DATE_OF_MRN) + mrnDate.apply(mrnDetails.getMrnDate()));
        }
        if (mrnDetails.getMrnLateReason() != null) {
            details.add( titleText.get(REASON_FOR_LATE_APPEAL) + mrnDetails.getMrnLateReason());
        }
        if (mrnDetails.getMrnMissingReason() != null) {
            details.add( titleText.get(REASON_FOR_NO_MRN) + mrnDetails.getMrnMissingReason());
        }

        return StringUtils.join(details.toArray(), TWO_NEW_LINES);
    }

    public Map<String, String> setYourDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.YOUR_DETAILS_LITERAL,
                buildYourDetails(ccdResponse.getAppeal(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH), Function.identity()));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_YOUR_DETAILS_LITERAL,
                    buildYourDetails(ccdResponse.getAppeal(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH), this::convertLongFormattedLocalDateToWelshDate));
        }
        return personalisation;
    }

    private String buildYourDetails(Appeal appeal, Map<PersonalisationKey, String> titleText, Function<String, String> convertDate) {

        return titleText.get(PersonalisationKey.NAME) + appeal.getAppellant().getName().getFullNameNoTitle() + TWO_NEW_LINES
            + titleText.get(DATE_OF_BIRTH) + convertDate.apply(getOptionalField(appeal.getAppellant().getIdentity().getDob(), NOT_PROVIDED))
            + TWO_NEW_LINES + titleText.get(NINO) + appeal.getAppellant().getIdentity().getNino()
            + TWO_NEW_LINES + titleText.get(PersonalisationKey.ADDRESS) + appeal.getAppellant().getAddress().getFullAddress() + TWO_NEW_LINES
            + titleText.get(PersonalisationKey.EMAIL) + getOptionalField(appeal.getAppellant().getContact().getEmail(), titleText.get(PersonalisationKey.NOT_PROVIDED))
            + TWO_NEW_LINES + titleText.get(PersonalisationKey.PHONE) + getOptionalField(getPhoneOrMobile(appeal.getAppellant().getContact()), titleText.get(PersonalisationKey.NOT_PROVIDED));
    }

    public Map<String, String> setTextMessageReminderDetails(Map<String, String> personalisation, Subscription subscription) {
        personalisation.put(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL, buildTextMessageDetails(subscription, personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH)));
        personalisation.put(AppConstants.WELSH_TEXT_MESSAGE_REMINDER_DETAILS_LITERAL, buildTextMessageDetails(subscription, personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH)));
        return personalisation;
    }

    private String buildTextMessageDetails(Subscription subscription, Map<PersonalisationKey, String> titleText) {
        StringBuilder buildTextMessage = new StringBuilder()
            .append(titleText.get(RECEIVE_TEXT_MESSAGE_REMINDER))
            .append(null != subscription && null != subscription.getSubscribeSms() ?
                    titleText.get(getPersonalisationKey(subscription.getSubscribeSms().toLowerCase(Locale.ENGLISH))) :  titleText.get(getPersonalisationKey(NO)));

        if (null != subscription && subscription.isSmsSubscribed()) {
            buildTextMessage
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.MOBILE))
                .append(subscription.getMobile());
        }

        return buildTextMessage.toString();
    }

    private Map<String, String> setAppointeeName(Map<String, String> personalisation, SscsCaseData sscsCaseData) {
        Appointee appointee = sscsCaseData.getAppeal().getAppellant().getAppointee();
        if (hasAppointee(appointee)) {
            personalisation.put(AppConstants.APPOINTEE_NAME, String.format("%s %s",
                appointee.getName().getFirstName(),
                appointee.getName().getLastName()));
        }
        return personalisation;
    }


    public Map<String, String> setAppointeeDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.APPOINTEE_DETAILS_LITERAL, buildAppointeeDetails(ccdResponse.getAppeal().getAppellant().getAppointee(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH), Function.identity()));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_APPOINTEE_DETAILS_LITERAL, buildAppointeeDetails(ccdResponse.getAppeal().getAppellant().getAppointee(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH), this::convertLongFormattedLocalDateToWelshDate));
        }
        return personalisation;
    }

    private String convertLongFormattedLocalDateToWelshDate(String date) {
        if(NOT_PROVIDED.equals(date)) {
            return personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH).get(PersonalisationKey.NOT_PROVIDED);
        }
        LocalDate localDate = LocalDate.parse(date, longFormatter);
        return LocalDateToWelshStringConverter.convert(localDate);
    }

    private String buildAppointeeDetails(Appointee appointee, Map<PersonalisationKey, String> titleText, Function<String, String> convertDate) {
        String hasAppointee = hasAppointee(appointee) ? YES : NO;

        StringBuilder appointeeBuilder = new StringBuilder()
            .append(titleText.get(HAVE_AN_APPOINTEE))
            .append(titleText.get(PersonalisationKey.getPersonalisationKey(hasAppointee)));

        if (StringUtils.equalsIgnoreCase(YES, hasAppointee)) {
            appointeeBuilder.append(TWO_NEW_LINES)
                 .append(titleText.get(PersonalisationKey.NAME)).append(appointee.getName().getFullNameNoTitle()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.DATE_OF_BIRTH)).append(convertDate.apply(appointee.getIdentity().getDob())).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.ADDRESS)).append(appointee.getAddress().getFullAddress()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.EMAIL)).append(getOptionalField(appointee.getContact().getEmail(), titleText.get(PersonalisationKey.NOT_PROVIDED))).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.PHONE))
                .append(getOptionalField(getPhoneOrMobile(appointee.getContact()), titleText.get(PersonalisationKey.NOT_PROVIDED)));
        }
        return appointeeBuilder.toString();
    }

    public Map<String, String> setRepresentativeDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.REPRESENTATIVE_DETAILS_LITERAL, buildRepresentativeDetails(ccdResponse.getAppeal().getRep(), personalisationConfiguration.personalisation.get(LanguagePreference.ENGLISH)));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_REPRESENTATIVE_DETAILS_LITERAL, buildRepresentativeDetails(ccdResponse.getAppeal().getRep(), personalisationConfiguration.personalisation.get(LanguagePreference.WELSH)));
        }
        return personalisation;
    }

    private String buildRepresentativeDetails(Representative representative,  Map<PersonalisationKey, String> titleText) {
        String hasRepresentative = (representative != null
            && StringUtils.equalsIgnoreCase(YES, representative.getHasRepresentative())) ? YES : NO;

        StringBuilder representativeBuilder = new StringBuilder()
            .append(titleText.get(HAVE_A_REPRESENTATIVE))
            .append(titleText.get(PersonalisationKey.getPersonalisationKey(hasRepresentative)));

        if (representative != null && representative.getName() != null && StringUtils.equalsIgnoreCase(YES, hasRepresentative)) {
            representativeBuilder.append(TWO_NEW_LINES + titleText.get(PersonalisationKey.NAME)).append(getOptionalField(representative.getName().getFullNameNoTitle(), titleText.get(PersonalisationKey.NOT_PROVIDED))).append(TWO_NEW_LINES)
                .append(titleText.get(ORGANISATION)).append(getOptionalField(representative.getOrganisation(), titleText.get(PersonalisationKey.NOT_PROVIDED))).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.ADDRESS)).append(representative.getAddress().getFullAddress()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.EMAIL)).append(getOptionalField(representative.getContact().getEmail(), titleText.get(PersonalisationKey.NOT_PROVIDED))).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.PHONE))
                .append(getOptionalField(getPhoneOrMobile(representative.getContact()), titleText.get(PersonalisationKey.NOT_PROVIDED)));
        }
        return representativeBuilder.toString();
    }

    public Map<String, String> setReasonsForAppealingDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL, buildReasonsForAppealingDetails(ccdResponse.getAppeal().getAppealReasons(),personalisationConfiguration.personalisation.get(LanguagePreference.ENGLISH)));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_REASONS_FOR_APPEALING_DETAILS_LITERAL, buildReasonsForAppealingDetails(ccdResponse.getAppeal().getAppealReasons(), personalisationConfiguration.personalisation.get(LanguagePreference.WELSH)));
        }
        return personalisation;
    }

    private String buildReasonsForAppealingDetails(AppealReasons appealReasons,  Map<PersonalisationKey, String> titleText) {
        StringBuilder appealReasonsBuilder = new StringBuilder();

        if (appealReasons.getReasons() != null && !appealReasons.getReasons().isEmpty()) {
            for (AppealReason reason : appealReasons.getReasons()) {
                appealReasonsBuilder.append(titleText.get(WHAT_DISAGREE_WITH)).append(reason.getValue().getDescription()).append(TWO_NEW_LINES)
                    .append(titleText.get(WHY_DISAGREE_WITH)).append(reason.getValue().getReason()).append(TWO_NEW_LINES);
            }
        }

        appealReasonsBuilder.append(titleText.get(ANYTHING))
            .append(getOptionalField(appealReasons.getOtherReasons(), titleText.get(PersonalisationKey.NOT_PROVIDED)));

        return appealReasonsBuilder.toString();
    }

    public Map<String, String> setHearingDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        HearingOptions hearingOptions = ccdResponse.getAppeal().getHearingOptions();
        personalisation.put(AppConstants.HEARING_DETAILS_LITERAL,
                buildHearingDetails(hearingOptions, personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH), this::convertLocalDateToLongDateString));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_HEARING_DETAILS_LITERAL,
                    buildHearingDetails(hearingOptions, personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH), this::convertLocalDateToWelshDateString));
        }
        return personalisation;
    }

    private String buildHearingDetails(HearingOptions hearingOptions, Map<PersonalisationKey, String> titleText, Function<String, String> convertor) {
        PersonalisationKey decisionKey = getPersonalisationKey(hearingOptions.getWantsToAttend().toLowerCase(Locale.ENGLISH));
        StringBuilder hearingOptionsBuilder = new StringBuilder()
            .append(titleText.get(PersonalisationKey.ATTENDING_HEARING))
            .append(titleText.get(decisionKey));

        if (StringUtils.equalsIgnoreCase(hearingOptions.getWantsToAttend(), YES) && hearingOptions.getExcludeDates() != null && !hearingOptions.getExcludeDates().isEmpty()) {
            hearingOptionsBuilder.append(TWO_NEW_LINES + titleText.get(PersonalisationKey.DATES_NOT_ATTENDING));

            StringJoiner joiner = new StringJoiner(", ");

            for (ExcludeDate excludeDate : hearingOptions.getExcludeDates()) {
                joiner.add(buildDateRangeString(excludeDate.getValue(), convertor));
            }
            hearingOptionsBuilder.append(joiner.toString());
        }
        return hearingOptionsBuilder.toString();
    }

    private String buildDateRangeString(DateRange range, Function<String, String> convertor) {

        if (range.getStart() != null) {
            return convertor.apply(range.getStart());
        }
        return StringUtils.EMPTY;
    }

    private String convertLocalDateToLongDateString(String localDateString) {
        LocalDate localDate = LocalDate.parse(localDateString, formatter);
        return localDate.format(longFormatter);
    }

    private String convertLocalDateToWelshDateString(String localDateString) {
        LocalDate localDate = LocalDate.parse(localDateString, formatter);
        return LocalDateToWelshStringConverter.convert(localDate);
    }

    public Map<String, String> setHearingArrangementDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL, buildHearingArrangements(ccdResponse.getAppeal().getHearingOptions(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH)));

        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_HEARING_ARRANGEMENT_DETAILS_LITERAL, buildHearingArrangements(ccdResponse.getAppeal().getHearingOptions(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH)));

        }
        return personalisation;
    }

    private String buildHearingArrangements(HearingOptions hearingOptions, Map<PersonalisationKey, String> titleText) {
        String languageInterpreterRequired = convertBooleanToRequiredText(hearingOptions.getLanguageInterpreter() != null
            && StringUtils.equalsIgnoreCase(YES, hearingOptions.getLanguageInterpreter()), titleText);

        return titleText.get(LANGUAGE_INTERPRETER) + languageInterpreterRequired + TWO_NEW_LINES + titleText.get(SIGN_INTERPRETER)
            + convertBooleanToRequiredText(findHearingArrangement("signLanguageInterpreter", hearingOptions.getArrangements()),titleText)
            + TWO_NEW_LINES + titleText.get(HEARING_LOOP) + convertBooleanToRequiredText(findHearingArrangement("hearingLoop", hearingOptions.getArrangements()), titleText)
            + TWO_NEW_LINES + titleText.get(DISABLED_ACCESS)+ convertBooleanToRequiredText(findHearingArrangement("disabledAccess", hearingOptions.getArrangements()), titleText)
            + TWO_NEW_LINES + titleText.get(OTHER_ARRANGEMENTS) + getOptionalField(hearingOptions.getOther(), titleText.get(PersonalisationKey.NOT_REQUIRED));
    }

    private Boolean findHearingArrangement(String field, List<String> arrangements) {
        return arrangements != null && arrangements.contains(field);
    }

    private String convertBooleanToRequiredText(Boolean bool, Map<PersonalisationKey, String> titleText) {
        return bool ? titleText.get(PersonalisationKey.REQUIRED) : titleText.get(PersonalisationKey.NOT_REQUIRED);
    }

    public static String getOptionalField(String field, String text) {
        return field == null || StringUtils.equalsIgnoreCase("null", field)
            || StringUtils.equalsIgnoreCase("null null", field)
            || StringUtils.equalsIgnoreCase("null null null", field)
            || StringUtils.isBlank(field) ? text : field;
    }

    private String getPhoneOrMobile(Contact contact) {
        if (null != contact) {
            return null != contact.getPhone() ? contact.getPhone() : contact.getMobile();
        } else {
            return null;
        }
    }
}
