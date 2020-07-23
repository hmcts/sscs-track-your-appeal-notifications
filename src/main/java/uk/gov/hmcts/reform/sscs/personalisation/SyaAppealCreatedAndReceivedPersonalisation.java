package uk.gov.hmcts.reform.sscs.personalisation;

import static uk.gov.hmcts.reform.sscs.config.PersonalisationKey.*;
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
import uk.gov.hmcts.reform.sscs.config.PersonalisationKey;
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

        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_MRN_DETAILS_LITERAL,
                    buildMrnDetails(ccdResponse.getAppeal().getMrnDetails(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH), this::convertLongFormattedLocalDateToWelshDate));
        }
        return personalisation;
    }

    private String buildMrnDetails(MrnDetails mrnDetails, Map<String, String> titleText, Function<String, String> mrnDate) {

        List<String> details = new ArrayList<>();

        if (mrnDetails.getMrnDate() != null) {
            details.add(titleText.get(DATE_OF_MRN.name()) + mrnDate.apply(mrnDetails.getMrnDate()));
        }

        if (mrnDetails.getMrnLateReason() != null) {
            details.add(titleText.get(REASON_FOR_LATE_APPEAL.name()) + mrnDetails.getMrnLateReason());
        }

        if (mrnDetails.getMrnMissingReason() != null) {
            details.add(titleText.get(REASON_FOR_NO_MRN.name()) + mrnDetails.getMrnMissingReason());
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

    private String buildYourDetails(Appeal appeal, Map<String, String> titleText, Function<String, String> convertDate) {

        return titleText.get(PersonalisationKey.NAME.name()) + appeal.getAppellant().getName().getFullNameNoTitle() + TWO_NEW_LINES
            + titleText.get(DATE_OF_BIRTH.name()) + convertDate.apply(getOptionalField(appeal.getAppellant().getIdentity().getDob(), NOT_PROVIDED))
            + TWO_NEW_LINES + titleText.get(NINO.name()) + appeal.getAppellant().getIdentity().getNino()
            + TWO_NEW_LINES + titleText.get(PersonalisationKey.ADDRESS.name()) + appeal.getAppellant().getAddress().getFullAddress() + TWO_NEW_LINES
            + titleText.get(PersonalisationKey.EMAIL.name()) + getOptionalField(appeal.getAppellant().getContact().getEmail(), titleText.get(PersonalisationKey.NOT_PROVIDED.name()))
            + TWO_NEW_LINES + titleText.get(PersonalisationKey.PHONE.name()) + getOptionalField(getPhoneOrMobile(appeal.getAppellant().getContact()), titleText.get(PersonalisationKey.NOT_PROVIDED.name()));
    }

    public Map<String, String> setTextMessageReminderDetails(Map<String, String> personalisation, Subscription subscription) {
        personalisation.put(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL, buildTextMessageDetails(subscription, personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH)));
        personalisation.put(AppConstants.WELSH_TEXT_MESSAGE_REMINDER_DETAILS_LITERAL, buildTextMessageDetails(subscription, personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH)));
        return personalisation;
    }

    private String buildTextMessageDetails(Subscription subscription, Map<String, String> titleText) {
        StringBuilder buildTextMessage = new StringBuilder()
            .append(titleText.get(RECEIVE_TEXT_MESSAGE_REMINDER.name()))
            .append(null != subscription && null != subscription.getSubscribeSms()
                    ? titleText.get(getYesNoKey(subscription.getSubscribeSms().toLowerCase(Locale.ENGLISH))) :  titleText.get(getYesNoKey(NO)));

        if (null != subscription && subscription.isSmsSubscribed()) {
            buildTextMessage
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.MOBILE.name()))
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
        if (NOT_PROVIDED.equals(date)) {
            return personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH).get(PersonalisationKey.NOT_PROVIDED.name());
        }
        LocalDate localDate = LocalDate.parse(date, longFormatter);
        return LocalDateToWelshStringConverter.convert(localDate);
    }

    private String buildAppointeeDetails(Appointee appointee, Map<String, String> titleText, Function<String, String> convertDate) {
        String hasAppointee = hasAppointee(appointee) ? YES : NO;

        StringBuilder appointeeBuilder = new StringBuilder()
            .append(titleText.get(HAVE_AN_APPOINTEE.name()))
            .append(titleText.get(getYesNoKey(hasAppointee)));

        if (StringUtils.equalsIgnoreCase(YES, hasAppointee)) {
            appointeeBuilder.append(TWO_NEW_LINES)
                 .append(titleText.get(PersonalisationKey.NAME.name())).append(appointee.getName().getFullNameNoTitle()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.DATE_OF_BIRTH.name())).append(convertDate.apply(appointee.getIdentity().getDob())).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.ADDRESS.name())).append(appointee.getAddress().getFullAddress()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.EMAIL.name())).append(getOptionalField(appointee.getContact().getEmail(), titleText.get(PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.PHONE.name()))
                .append(getOptionalField(getPhoneOrMobile(appointee.getContact()), titleText.get(PersonalisationKey.NOT_PROVIDED.name())));
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

    private String buildRepresentativeDetails(Representative representative,  Map<String, String> titleText) {
        String hasRepresentative = (representative != null
            && StringUtils.equalsIgnoreCase(YES, representative.getHasRepresentative())) ? YES : NO;

        StringBuilder representativeBuilder = new StringBuilder()
            .append(titleText.get(HAVE_A_REPRESENTATIVE.name()))
            .append(titleText.get(getYesNoKey(hasRepresentative)));

        if (representative != null && representative.getName() != null && StringUtils.equalsIgnoreCase(YES, hasRepresentative)) {
            representativeBuilder.append(TWO_NEW_LINES + titleText.get(PersonalisationKey.NAME.name())).append(getOptionalField(representative.getName().getFullNameNoTitle(), titleText.get(PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                .append(titleText.get(ORGANISATION.name())).append(getOptionalField(representative.getOrganisation(), titleText.get(PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.ADDRESS.name())).append(representative.getAddress().getFullAddress()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.EMAIL.name())).append(getOptionalField(representative.getContact().getEmail(), titleText.get(PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationKey.PHONE.name()))
                .append(getOptionalField(getPhoneOrMobile(representative.getContact()), titleText.get(PersonalisationKey.NOT_PROVIDED.name())));
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

    private String buildReasonsForAppealingDetails(AppealReasons appealReasons,  Map<String, String> titleText) {
        StringBuilder appealReasonsBuilder = new StringBuilder();

        if (appealReasons.getReasons() != null && !appealReasons.getReasons().isEmpty()) {
            for (AppealReason reason : appealReasons.getReasons()) {
                appealReasonsBuilder.append(titleText.get(WHAT_DISAGREE_WITH.name())).append(reason.getValue().getReason()).append(TWO_NEW_LINES)
                    .append(titleText.get(WHY_DISAGREE_WITH.name())).append(reason.getValue().getDescription()).append(TWO_NEW_LINES);
            }
        }

        appealReasonsBuilder.append(titleText.get(ANYTHING.name()))
            .append(getOptionalField(appealReasons.getOtherReasons(), titleText.get(PersonalisationKey.NOT_PROVIDED.name())));

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

    private String buildHearingDetails(HearingOptions hearingOptions, Map<String, String> titleText, Function<String, String> convertor) {
        String decisionKey = getYesNoKey(hearingOptions.getWantsToAttend().toLowerCase(Locale.ENGLISH));
        StringBuilder hearingOptionsBuilder = new StringBuilder()
            .append(titleText.get(PersonalisationKey.ATTENDING_HEARING.name()))
            .append(titleText.get(decisionKey));

        if (StringUtils.equalsIgnoreCase(hearingOptions.getWantsToAttend(), YES) && hearingOptions.getExcludeDates() != null && !hearingOptions.getExcludeDates().isEmpty()) {
            hearingOptionsBuilder.append(TWO_NEW_LINES + titleText.get(PersonalisationKey.DATES_NOT_ATTENDING.name()));

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

    private String buildHearingArrangements(HearingOptions hearingOptions, Map<String, String> titleText) {
        String languageInterpreterRequired = convertBooleanToRequiredText(hearingOptions.getLanguageInterpreter() != null
            && StringUtils.equalsIgnoreCase(YES, hearingOptions.getLanguageInterpreter()), titleText);

        return titleText.get(LANGUAGE_INTERPRETER.name()) + languageInterpreterRequired + TWO_NEW_LINES + titleText.get(SIGN_INTERPRETER.name())
            + convertBooleanToRequiredText(findHearingArrangement("signLanguageInterpreter", hearingOptions.getArrangements()),titleText)
            + TWO_NEW_LINES + titleText.get(HEARING_LOOP.name()) + convertBooleanToRequiredText(findHearingArrangement("hearingLoop", hearingOptions.getArrangements()), titleText)
            + TWO_NEW_LINES + titleText.get(DISABLED_ACCESS.name()) + convertBooleanToRequiredText(findHearingArrangement("disabledAccess", hearingOptions.getArrangements()), titleText)
            + TWO_NEW_LINES + titleText.get(OTHER_ARRANGEMENTS.name()) + getOptionalField(hearingOptions.getOther(), titleText.get(PersonalisationKey.NOT_REQUIRED.name()));
    }

    private Boolean findHearingArrangement(String field, List<String> arrangements) {
        return arrangements != null && arrangements.contains(field);
    }

    private String convertBooleanToRequiredText(Boolean bool, Map<String, String> titleText) {
        return bool ? titleText.get(PersonalisationKey.REQUIRED.name()) : titleText.get(PersonalisationKey.NOT_REQUIRED.name());
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
