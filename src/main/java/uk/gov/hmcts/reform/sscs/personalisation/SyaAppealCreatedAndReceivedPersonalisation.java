package uk.gov.hmcts.reform.sscs.personalisation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;

@Component
public class SyaAppealCreatedAndReceivedPersonalisation extends WithRepresentativePersonalisation {

    private static final String NOT_PROVIDED = "Not provided";

    @Override
    protected Map<String, String> create(SscsCaseDataWrapper responseWrapper) {
        Map<String, String> personalisation = super.create(responseWrapper);
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();

        setMrnDetails(personalisation, ccdResponse);
        setYourDetails(personalisation, ccdResponse);
        setTextMessageReminderDetails(personalisation, ccdResponse);
        setRepresentativeDetails(personalisation, ccdResponse);
        setReasonsForAppealingDetails(personalisation, ccdResponse);
        setHearingDetails(personalisation, ccdResponse);
        setHearingArrangementDetails(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, String> setMrnDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.MRN_DETAILS_LITERAL, buildMrnDetails(ccdResponse.getAppeal().getMrnDetails()));
        return personalisation;
    }

    private String buildMrnDetails(MrnDetails mrnDetails) {

        List<String> details = new ArrayList<>();

        if (mrnDetails.getMrnDate() != null) {
            details.add("Date of MRN: " + mrnDetails.getMrnDate());
        }
        if (mrnDetails.getMrnLateReason() != null) {
            details.add("Reason for late appeal: " + mrnDetails.getMrnLateReason());
        }
        if (mrnDetails.getMrnMissingReason() != null) {
            details.add("Reason for no MRN: " + mrnDetails.getMrnMissingReason());
        }

        return StringUtils.join(details.toArray(), "\n\n");
    }

    public Map<String, String> setYourDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.YOUR_DETAILS_LITERAL, buildYourDetails(ccdResponse.getAppeal()));
        return personalisation;
    }

    private String buildYourDetails(Appeal appeal) {
        return new StringBuilder()
                .append("Name: ")
                .append(appeal.getAppellant().getName().getFullNameNoTitle() + "\n\n")
                .append("Date of birth: ")
                .append(appeal.getAppellant().getIdentity().getDob() + "\n\n")
                .append("National Insurance number: ")
                .append(appeal.getAppellant().getIdentity().getNino() + "\n\n")
                .append("Address: ")
                .append(appeal.getAppellant().getAddress().getFullAddress() + "\n\n")
                .append("Email: ")
                .append(getOptionalField(appeal.getAppellant().getContact().getEmail(), NOT_PROVIDED) + "\n\n")
                .append("Phone: ")
                .append(getOptionalField(appeal.getAppellant().getContact().getPhone(), NOT_PROVIDED))
                .toString();
    }

    public Map<String, String> setTextMessageReminderDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL, buildTextMessageDetails(ccdResponse.getSubscriptions().getAppellantSubscription()));
        return personalisation;
    }

    private String buildTextMessageDetails(Subscription subscription) {
        StringBuilder buildTextMessage = new StringBuilder()
                .append("Receive text message reminders: ")
                .append(subscription.getSubscribeSms().toLowerCase());

        if (subscription.isSmsSubscribed()) {
            buildTextMessage
                    .append("\n\nMobile number: ")
                    .append(subscription.getMobile());
        }

        return buildTextMessage.toString();
    }

    public Map<String, String> setRepresentativeDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.REPRESENTATIVE_DETAILS_LITERAL, buildRepresentativeDetails(ccdResponse.getAppeal().getRep()));
        return personalisation;
    }

    private String buildRepresentativeDetails(Representative representative) {
        String hasRepresentative = (representative != null) ? "yes" : "no";

        StringBuilder representativeBuilder = new StringBuilder()
                .append("Have a representative: ")
                .append(hasRepresentative);

        if (representative != null) {
            representativeBuilder.append("\n\nName: ")
                    .append(representative.getName().getFullNameNoTitle() + "\n\n")
                    .append("Organisation: ")
                    .append(getOptionalField(representative.getOrganisation(), NOT_PROVIDED) + "\n\n")
                    .append("Address: ")
                    .append(representative.getAddress().getFullAddress() + "\n\n")
                    .append("Email: ")
                    .append(getOptionalField(representative.getContact().getEmail(), NOT_PROVIDED) + "\n\n")
                    .append("Phone: ")
                    .append(getOptionalField(representative.getContact().getPhone(), NOT_PROVIDED))
                    .toString();
        }
        return representativeBuilder.toString();
    }

    public Map<String, String> setReasonsForAppealingDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL, buildReasonsForAppealingDetails(ccdResponse.getAppeal().getAppealReasons()));

        return personalisation;
    }

    private String buildReasonsForAppealingDetails(AppealReasons appealReasons) {
        StringBuilder appealReasonsBuilder = new StringBuilder();

        if (appealReasons.getReasons() != null && !CollectionUtils.isEmpty(appealReasons.getReasons())) {
            for (AppealReason reason : appealReasons.getReasons()) {
                appealReasonsBuilder.append("What you disagree with: ")
                        .append(reason.getValue().getDescription() + "\n\n")
                        .append("Why you disagree with it: ")
                        .append(reason.getValue().getReason() + "\n\n");
            }
        }

        appealReasonsBuilder.append("Anything else you want to tell the tribunal: ")
                .append(getOptionalField(appealReasons.getOtherReasons(), NOT_PROVIDED));

        return appealReasonsBuilder.toString();
    }

    public Map<String, String> setHearingDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.HEARING_DETAILS_LITERAL, buildHearingDetails(ccdResponse.getAppeal().getHearingOptions()));

        return personalisation;
    }

    private String buildHearingDetails(HearingOptions hearingOptions) {
        StringBuilder hearingOptionsBuilder = new StringBuilder()
                .append("Attending the hearing: ")
                .append(hearingOptions.getWantsToAttend().toLowerCase());

        if ("yes".equalsIgnoreCase(hearingOptions.getWantsToAttend()) && hearingOptions.getExcludeDates() != null && !CollectionUtils.isEmpty(hearingOptions.getExcludeDates())) {
            hearingOptionsBuilder.append("\n\nDates you can't attend: ");

            StringJoiner joiner = new StringJoiner(", ");

            for (ExcludeDate excludeDate : hearingOptions.getExcludeDates()) {
                joiner.add((buildDateRangeString(excludeDate.getValue())));
            }
            hearingOptionsBuilder.append(joiner.toString());
        }
        return hearingOptionsBuilder.toString();
    }

    private String buildDateRangeString(DateRange range) {

        if (range.getStart() != null) {
            return convertLocalDateToLongDateString(range.getStart());
        }
        return "";
    }

    private String convertLocalDateToLongDateString(String localDateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter longFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");

        LocalDate localDate = LocalDate.parse(localDateString, formatter);

        return localDate.format(longFormatter);
    }

    public Map<String, String> setHearingArrangementDetails(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL, buildHearingArrangements(ccdResponse.getAppeal().getHearingOptions()));

        return personalisation;
    }

    private String buildHearingArrangements(HearingOptions hearingOptions) {
        String languageInterpreterRequired = convertBooleanToRequiredText(hearingOptions.getLanguageInterpreter() != null && "yes".equalsIgnoreCase(hearingOptions.getLanguageInterpreter()));

        return new StringBuilder()
                .append("Language interpreter: ")
                .append(languageInterpreterRequired + "\n\n")
                .append("Sign interpreter: ")
                .append(convertBooleanToRequiredText(findHearingArrangement("signLanguageInterpreter", hearingOptions.getArrangements())) + "\n\n")
                .append("Hearing loop: ")
                .append(convertBooleanToRequiredText(findHearingArrangement("hearingLoop", hearingOptions.getArrangements())) + "\n\n")
                .append("Disabled access: ")
                .append(convertBooleanToRequiredText(findHearingArrangement("disabledAccess", hearingOptions.getArrangements())) + "\n\n")
                .append("Any other arrangements: ")
                .append(getOptionalField(hearingOptions.getOther(), "Not required"))
                .toString();
    }

    private Boolean findHearingArrangement(String field, List<String> arrangements) {
        return (arrangements != null && arrangements.contains(field));
    }

    private String convertBooleanToRequiredText(Boolean value) {
        return value ? "Required" : "Not required";
    }


    private String getOptionalField(String field, String text) {
        return field == null || field.equals("null") || field.isEmpty() ? text : field;
    }
}
