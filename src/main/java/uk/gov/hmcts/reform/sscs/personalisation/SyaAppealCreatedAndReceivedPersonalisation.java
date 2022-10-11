package uk.gov.hmcts.reform.sscs.personalisation;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.PersonalisationKey.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;

@Component
public class SyaAppealCreatedAndReceivedPersonalisation extends WithRepresentativePersonalisation {

    private static final String NOT_PROVIDED = "Not provided";
    static final String TWO_NEW_LINES = "\n\n";
    static final String NOT_REQUIRED = "Not required";
    static final String REQUIRED = "Required";

    @Autowired
    private PersonalisationConfiguration syaPersonalisationConfig;

    @Override
    protected Map<String, Object> create(SscsCaseDataWrapper responseWrapper, SubscriptionWithType subscriptionWithType) {
        Map<String, Object> personalisation = super.create(responseWrapper, subscriptionWithType);
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();

        setMrnDetails(personalisation, ccdResponse);
        setYourDetails(personalisation, ccdResponse);
        setAppointeeName(personalisation, ccdResponse);
        setAppointeeDetails(personalisation, ccdResponse);
        setTextMessageReminderDetails(personalisation, subscriptionWithType.getSubscription());
        setRepresentativeDetails(personalisation, ccdResponse);
        setOtherPartyDetails(personalisation, ccdResponse);
        setReasonsForAppealingDetails(personalisation, ccdResponse);
        setHearingDetails(personalisation, ccdResponse);
        setHearingArrangementDetails(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, Object> setMrnDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.MRN_DETAILS_LITERAL,
                buildMrnDetails(ccdResponse.getAppeal().getMrnDetails(), syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.ENGLISH), LOCALE_UK));

        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_MRN_DETAILS_LITERAL,
                    buildMrnDetails(ccdResponse.getAppeal().getMrnDetails(), syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.WELSH), LOCALE_WELSH));
        }
        return personalisation;
    }

    private String buildMrnDetails(MrnDetails mrnDetails, Map<String, String> titleText, Locale locale) {



        List<String> details = new ArrayList<>();

        if (nonNull(mrnDetails)) {
            if (nonNull(mrnDetails.getMrnDate())) {
                String mrn = titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name());

                if (!NOT_PROVIDED.equalsIgnoreCase(mrnDetails.getMrnDate())) {
                    LocalDate mrnLocalDate = LocalDate.parse(mrnDetails.getMrnDate(), CCD_DATE_FORMAT);
                    mrn = mrnLocalDate.format(DATE_FORMAT_LONG.localizedBy(locale));
                }

                details.add(titleText.get(DATE_OF_MRN.name()) + mrn);
            }

            if (nonNull(mrnDetails.getMrnLateReason())) {
                details.add(titleText.get(REASON_FOR_LATE_APPEAL.name()) + mrnDetails.getMrnLateReason());
            }

            if (nonNull(mrnDetails.getMrnMissingReason())) {
                details.add(titleText.get(REASON_FOR_NO_MRN.name()) + mrnDetails.getMrnMissingReason());
            }
        }

        return StringUtils.join(details.toArray(), TWO_NEW_LINES);
    }

    public Map<String, Object> setYourDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.YOUR_DETAILS_LITERAL,
                buildYourDetails(ccdResponse, syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.ENGLISH), LOCALE_UK));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_YOUR_DETAILS_LITERAL,
                buildYourDetails(ccdResponse, syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.WELSH), LOCALE_WELSH));
        }
        return personalisation;
    }

    private String buildYourDetails(SscsCaseData ccdResponse, Map<String, String> titleText, Locale locale) {
        Appeal appeal = ccdResponse.getAppeal();
        LocalDate dateOfBirth = LocalDate.parse(appeal.getAppellant().getIdentity().getDob(), CCD_DATE_FORMAT);

        String yourDetails = titleText.get(
            PersonalisationConfiguration.PersonalisationKey.NAME.name())
            + appeal.getAppellant().getName().getFullNameNoTitle()
            + TWO_NEW_LINES
            + titleText.get(DATE_OF_BIRTH.name())
            + dateOfBirth.format(DATE_FORMAT_LONG.localizedBy(locale))
            + TWO_NEW_LINES
            + titleText.get(NINO.name())
            + appeal.getAppellant().getIdentity().getNino()
            + TWO_NEW_LINES
            + titleText.get(PersonalisationConfiguration.PersonalisationKey.ADDRESS.name())
            + appeal.getAppellant().getAddress().getFullAddress()
            + TWO_NEW_LINES
            + titleText.get(PersonalisationConfiguration.PersonalisationKey.EMAIL.name())
            + getOptionalField(appeal.getAppellant().getContact().getEmail(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))
            + TWO_NEW_LINES + titleText.get(PersonalisationConfiguration.PersonalisationKey.PHONE.name())
            + getOptionalField(getPhoneOrMobile(appeal.getAppellant().getContact()), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()));

        if (ccdResponse.getChildMaintenanceNumber() != null) {
            yourDetails = yourDetails
                + TWO_NEW_LINES
                + titleText.get(PersonalisationConfiguration.PersonalisationKey.CHILD_MAINTENANCE_NUMBER.name())
                + ccdResponse.getChildMaintenanceNumber();
        }
        return yourDetails;
    }

    public Map<String, Object> setTextMessageReminderDetails(Map<String, Object> personalisation, Subscription subscription) {
        personalisation.put(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL, buildTextMessageDetails(subscription, syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.ENGLISH)));
        personalisation.put(AppConstants.WELSH_TEXT_MESSAGE_REMINDER_DETAILS_LITERAL, buildTextMessageDetails(subscription, syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.WELSH)));
        return personalisation;
    }

    private String buildTextMessageDetails(Subscription subscription, Map<String, String> titleText) {
        StringBuilder buildTextMessage = new StringBuilder()
            .append(titleText.get(RECEIVE_TEXT_MESSAGE_REMINDER.name()))
            .append(null != subscription && null != subscription.getSubscribeSms()
                    ? titleText.get(getYesNoKey(subscription.getSubscribeSms().toLowerCase(LOCALE_UK))) :  titleText.get(getYesNoKey(NO.getValue())));

        if (null != subscription && subscription.isSmsSubscribed()) {
            buildTextMessage
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.MOBILE.name()))
                .append(subscription.getMobile());
        }

        return buildTextMessage.toString();
    }

    private Map<String, Object> setAppointeeName(Map<String, Object> personalisation, SscsCaseData sscsCaseData) {
        Appointee appointee = sscsCaseData.getAppeal().getAppellant().getAppointee();
        if (hasAppointee(appointee, sscsCaseData.getAppeal().getAppellant().getIsAppointee())) {
            personalisation.put(AppConstants.APPOINTEE_NAME, String.format("%s %s",
                appointee.getName().getFirstName(),
                appointee.getName().getLastName()));
        }
        return personalisation;
    }


    public Map<String, Object> setAppointeeDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        String isAppointee = ccdResponse.getAppeal().getAppellant().getIsAppointee();
        Appointee appointee = ccdResponse.getAppeal().getAppellant().getAppointee();
        String appointeeDetails = buildAppointeeDetails(appointee, isAppointee, syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.ENGLISH), LOCALE_UK);
        personalisation.put(AppConstants.APPOINTEE_DETAILS_LITERAL, appointeeDetails);
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            appointeeDetails = buildAppointeeDetails(appointee, isAppointee, syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.WELSH), LOCALE_WELSH);
            personalisation.put(AppConstants.WELSH_APPOINTEE_DETAILS_LITERAL, appointeeDetails);
        }
        return personalisation;
    }

    private String buildAppointeeDetails(Appointee appointee, String isAppointee, Map<String, String> titleText, Locale locale) {
        boolean hasAppointee = hasAppointee(appointee, isAppointee);

        StringBuilder appointeeBuilder = new StringBuilder()
            .append(titleText.get(HAVE_AN_APPOINTEE.name()))
            .append(titleText.get(getYesNoKey(hasAppointee)));

        if (hasAppointee) {
            LocalDate dateOfBirth = LocalDate.parse(appointee.getIdentity().getDob(), CCD_DATE_FORMAT);

            appointeeBuilder.append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.NAME.name()))
                .append(appointee.getName().getFullNameNoTitle())
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.DATE_OF_BIRTH.name()))
                .append(dateOfBirth.format(DATE_FORMAT_LONG.localizedBy(locale)))
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.ADDRESS.name()))
                .append(appointee.getAddress().getFullAddress()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.EMAIL.name()))
                .append(getOptionalField(appointee.getContact().getEmail(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())))
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.PHONE.name()))
                .append(getOptionalField(getPhoneOrMobile(appointee.getContact()), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())));
        }
        return appointeeBuilder.toString();
    }

    public Map<String, Object> setRepresentativeDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.REPRESENTATIVE_DETAILS_LITERAL, buildRepresentativeDetails(ccdResponse.getAppeal().getRep(), syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.ENGLISH)));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_REPRESENTATIVE_DETAILS_LITERAL, buildRepresentativeDetails(ccdResponse.getAppeal().getRep(), syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.WELSH)));
        }
        return personalisation;
    }

    private String buildRepresentativeDetails(Representative representative,  Map<String, String> titleText) {
        boolean hasRepresentative = nonNull(representative) && isYes(representative.getHasRepresentative());

        StringBuilder representativeBuilder = new StringBuilder()
            .append(titleText.get(HAVE_A_REPRESENTATIVE.name()))
            .append(titleText.get(getYesNoKey(hasRepresentative)));

        if (nonNull(representative) && nonNull(representative.getName()) && hasRepresentative) {
            representativeBuilder
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.NAME.name()))
                .append(getOptionalField(representative.getName().getFullNameNoTitle(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())))
                .append(TWO_NEW_LINES)
                .append(titleText.get(ORGANISATION.name())).append(getOptionalField(representative.getOrganisation(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())))
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.ADDRESS.name()))
                .append(representative.getAddress().getFullAddress())
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.EMAIL.name()))
                .append(getOptionalField(representative.getContact().getEmail(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())))
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.PHONE.name()))
                .append(getOptionalField(getPhoneOrMobile(representative.getContact()), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())));
        }
        return representativeBuilder.toString();
    }

    public Map<String, Object> setOtherPartyDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        if (ccdResponse.getOtherParties() != null && !ccdResponse.getOtherParties().isEmpty()) {
            personalisation.put(SHOW_OTHER_PARTY_DETAILS, "Yes");
            personalisation.put(OTHER_PARTY_DETAILS, buildOtherPartyDetails(ccdResponse.getOtherParties(), syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.ENGLISH)));
            if (ccdResponse.isLanguagePreferenceWelsh()) {
                personalisation.put(WELSH_OTHER_PARTY_DETAILS, buildOtherPartyDetails(ccdResponse.getOtherParties(), syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.WELSH)));
            }
        } else {
            personalisation.put(SHOW_OTHER_PARTY_DETAILS, "No");
            personalisation.put(OTHER_PARTY_DETAILS, "");
            personalisation.put(WELSH_OTHER_PARTY_DETAILS, "");
        }
        return personalisation;
    }

    private String buildOtherPartyDetails(List<CcdValue<OtherParty>> otherParties, Map<String, String> titleText) {
        StringBuilder otherPartyBuilder = new StringBuilder();

        for (CcdValue<OtherParty> otherParty : otherParties) {
            if (otherParty != null) {
                String name = otherParty.getValue().getName() != null ? otherParty.getValue().getName().getFullNameNoTitle() : null;
                String address = otherParty.getValue().getAddress() != null ? otherParty.getValue().getAddress().getFullAddress() : null;
                otherPartyBuilder.append(titleText.get(PersonalisationConfiguration.PersonalisationKey.NAME.name())).append(getOptionalField(name, titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                        .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.ADDRESS.name())).append(getOptionalField(address, titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES);
            }
        }

        return otherPartyBuilder.toString();
    }

    public Map<String, Object> setReasonsForAppealingDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL, buildReasonsForAppealingDetails(ccdResponse.getAppeal().getAppealReasons(), syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.ENGLISH)));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_REASONS_FOR_APPEALING_DETAILS_LITERAL, buildReasonsForAppealingDetails(ccdResponse.getAppeal().getAppealReasons(), syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.WELSH)));
        }
        return personalisation;
    }

    private String buildReasonsForAppealingDetails(AppealReasons appealReasons,  Map<String, String> titleText) {
        StringBuilder appealReasonsBuilder = new StringBuilder();

        if (appealReasons != null && appealReasons.getReasons() != null && !appealReasons.getReasons().isEmpty()) {
            for (AppealReason reason : appealReasons.getReasons()) {
                appealReasonsBuilder.append(titleText.get(WHAT_DISAGREE_WITH.name())).append(reason.getValue().getReason()).append(TWO_NEW_LINES)
                    .append(titleText.get(WHY_DISAGREE_WITH.name())).append(reason.getValue().getDescription()).append(TWO_NEW_LINES);
            }
        }

        if (appealReasons != null) {
            appealReasonsBuilder.append(titleText.get(ANYTHING.name()))
                    .append(getOptionalField(appealReasons.getOtherReasons(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())));
        }
        return appealReasonsBuilder.toString();
    }

    public Map<String, Object> setHearingDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        System.out.println(syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.ENGLISH));
        HearingOptions hearingOptions = ccdResponse.getAppeal().getHearingOptions();
        personalisation.put(AppConstants.HEARING_DETAILS_LITERAL,
            buildHearingDetails(hearingOptions, syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.ENGLISH), LOCALE_UK));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(AppConstants.WELSH_HEARING_DETAILS_LITERAL,
                buildHearingDetails(hearingOptions,
                        syaPersonalisationConfig.getPersonalisation().get(LanguagePreference.WELSH), LOCALE_WELSH));
        }
        return personalisation;
    }

    private String buildHearingDetails(HearingOptions hearingOptions, Map<String, String> titleText, Locale locale) {
        boolean wantsToAttend = isYes(hearingOptions.getWantsToAttend());
        StringBuilder hearingOptionsBuilder = new StringBuilder()
            .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.ATTENDING_HEARING.name()))
            .append(titleText.get(getYesNoKey(wantsToAttend)));

        if (wantsToAttend && isNotEmpty(hearingOptions.getExcludeDates())) {
            hearingOptionsBuilder.append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.DATES_NOT_ATTENDING.name()));

            StringJoiner joiner = new StringJoiner(", ");

            for (ExcludeDate excludeDate : hearingOptions.getExcludeDates()) {

                String start = excludeDate.getValue().getStart();
                if (nonNull(start)) {
                    LocalDate localDate = LocalDate.parse(start, CCD_DATE_FORMAT);
                    joiner.add(localDate.format(DATE_FORMAT_LONG.localizedBy(locale)));
                } else {
                    joiner.add(StringUtils.EMPTY);
                }

            }
            hearingOptionsBuilder.append(joiner);
        }
        return hearingOptionsBuilder.toString();
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
