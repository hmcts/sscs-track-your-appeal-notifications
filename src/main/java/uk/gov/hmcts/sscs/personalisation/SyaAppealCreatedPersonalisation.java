package uk.gov.hmcts.sscs.personalisation;

import static uk.gov.hmcts.sscs.config.AppConstants.*;

import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.*;

@Component
public class SyaAppealCreatedPersonalisation extends Personalisation {

    @Override
    public Map<String, String> create(CcdResponseWrapper responseWrapper) {
        Map<String, String> personalisation = super.create(responseWrapper);
        CcdResponse ccdResponse = responseWrapper.getNewCcdResponse();

        setMrnDetails(personalisation, ccdResponse);
        setYourDetails(personalisation, ccdResponse);
        setTextMessageReminderDetails(personalisation, ccdResponse);
        setRepresentativeDetails(personalisation, ccdResponse);
        setReasonsForAppealingDetails(personalisation, ccdResponse);
        setHearingDetails(personalisation, ccdResponse);
        setHearingArrangementDetails(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, String> setMrnDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        personalisation.put(MRN_DETAILS_LITERAL, buildMrnDetails(ccdResponse.getAppeal()));
        return personalisation;
    }

    private String buildMrnDetails(Appeal appeal) {
        return new StringBuilder()
            .append("Date of MRN: ")
            .append(appeal.getMrnDate() + "\n\n")
            .append("Reason for late appeal: ")
            .append(appeal.getMrnLateReason()  + "\n\n")
            .append("Reason for no MRN: ")
            .append(appeal.getMrnMissingReason())
            .toString();
    }

    public Map<String, String> setYourDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        personalisation.put(YOUR_DETAILS_LITERAL, buildYourDetails(ccdResponse.getAppeal()));
        return personalisation;
    }

    private String buildYourDetails(Appeal appeal) {
        return new StringBuilder()
            .append("Appointee: ")
            .append(appeal.getAppellant().getIsAppointee() + "\n\n")
            .append("Name: ")
            .append(appeal.getAppellant().getName().getFullNameNoTitle() + "\n\n")
            .append("Date of birth: ")
            .append(appeal.getAppellant().getIdentity().getDob() + "\n\n")
            .append("National Insurance number: ")
            .append(appeal.getAppellant().getIdentity().getNino() + "\n\n")
            .append("Address: ")
            .append(appeal.getAppellant().getAddress().getFullAddress() + "\n\n")
            .append("Email: ")
            .append(getOptionalField(appeal.getAppellant().getContact().getEmail()) + "\n\n")
            .append("Phone: ")
            .append(getOptionalField(appeal.getAppellant().getContact().getPhone()))
            .toString();
    }

    public Map<String, String> setTextMessageReminderDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        personalisation.put(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL, buildTextMessageDetails(ccdResponse.getSubscriptions().getAppellantSubscription()));
        return personalisation;
    }

    private String buildTextMessageDetails(Subscription subscription) {
        StringBuilder buildTextMessage = new StringBuilder()
                .append("Receive text message reminders: ")
                .append(subscription.getSubscribeSms().toLowerCase());

        if (subscription.isSubscribeSms()) {
            buildTextMessage
                    .append("\n\nMobile number: ")
                    .append(subscription.getMobile());
        }

        return buildTextMessage.toString();
    }

    public Map<String, String> setRepresentativeDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        personalisation.put(REPRESENTATIVE_DETAILS_LITERAL, buildRepresentativeDetails(ccdResponse.getAppeal().getRep()));
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
                .append(getOptionalField(representative.getOrganisation()) + "\n\n")
                .append("Address: ")
                .append(representative.getAddress().getFullAddress() + "\n\n")
                .append("Email: ")
                .append(getOptionalField(representative.getContact().getEmail()) + "\n\n")
                .append("Phone: ")
                .append(getOptionalField(representative.getContact().getPhone()))
                .toString();
        }
        return representativeBuilder.toString();
    }

    public Map<String, String> setReasonsForAppealingDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        personalisation.put(REASONS_FOR_APPEALING_DETAILS_LITERAL, buildReasonsForAppealingDetails(ccdResponse.getAppeal().getAppealReasons()));

        return personalisation;
    }

    private String buildReasonsForAppealingDetails(AppealReasons appealReasons) {
        StringBuilder appealReasonsBuilder = new StringBuilder();

        if (appealReasons.getReasons() != null && appealReasons.getReasons().size() > 0) {
            for (AppealReason reason : appealReasons.getReasons()) {
                appealReasonsBuilder.append("What you disagree with: ")
                        .append(reason.getValue().getReason() + "\n\n")
                        .append("Why you disagree with it: ")
                        .append(reason.getValue().getDescription() + "\n\n");
            }
        }

        appealReasonsBuilder.append("Anything else you want to tell the tribunal: ")
                .append(getOptionalField(appealReasons.getOtherReasons()));

        return appealReasonsBuilder.toString();
    }

    public Map<String, String> setHearingDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        personalisation.put(HEARING_DETAILS_LITERAL, buildHearingDetails(ccdResponse.getAppeal().getHearingOptions()));

        return personalisation;
    }

    private String buildHearingDetails(HearingOptions hearingOptions) {
        StringBuilder hearingOptionsBuilder = new StringBuilder()
                .append("Attending the hearing: ")
                .append(hearingOptions.getAttendingHearing().toLowerCase());

        if (hearingOptions.getAttendingHearing().toLowerCase().equals("yes") && hearingOptions.getExcludeDates() != null && hearingOptions.getExcludeDates().size() > 0) {
            hearingOptionsBuilder.append("\n\nDates you can't attend: ");

            StringJoiner joiner = new StringJoiner(", ");

            //FIXME: Check with Santhosh about how dates are stored
            for (ExcludeDate excludeDate : hearingOptions.getExcludeDates()) {
                joiner.add((buildDateRangeString(excludeDate.getValue())));
            }
            hearingOptionsBuilder.append(joiner.toString());
        }
        return hearingOptionsBuilder.toString();
    }

    private String buildDateRangeString(DateRange range) {

        if (range.getStart() != null && range.getEnd() != null) {
            return range.getStart() + " to " + range.getEnd();
        } else if (range.getStart() != null && range.getEnd() == null) {
            return range.getStart();
        }
        return "";
    }

    public Map<String, String> setHearingArrangementDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        return personalisation;
    }

    private String getOptionalField(String field) {
        return field == null || field.isEmpty() ? "Not provided" : field;
    }
}
