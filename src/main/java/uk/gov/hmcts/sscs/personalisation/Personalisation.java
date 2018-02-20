package uk.gov.hmcts.sscs.personalisation;

import static java.time.temporal.ChronoUnit.DAYS;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.SUBSCRIPTION_CREATED;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.domain.notify.Template;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;

public class Personalisation {

    protected NotificationConfig config;

    private boolean sendSmsSubscriptionConfirmation;
    private final MessageAuthenticationServiceImpl macService;

    public Personalisation(NotificationConfig config, MessageAuthenticationServiceImpl macService) {
        this.config = config;
        this.macService = macService;
    }

    public Map<String, String> create(CcdResponseWrapper responseWrapper) {
        CcdResponse ccdResponse = responseWrapper.getNewCcdResponse();
        Map<String, String> personalisation = new HashMap<>();
        personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL, BENEFIT_NAME_ACRONYM);
        personalisation.put(BENEFIT_FULL_NAME_LITERAL, BENEFIT_FULL_NAME);
        personalisation.put(APPEAL_REF, ccdResponse.getCaseReference());
        personalisation.put(APPEAL_ID, ccdResponse.getAppellantSubscription().getAppealNumber());
        personalisation.put(APPELLANT_NAME, String.format("%s %s", ccdResponse.getAppellantSubscription().getFirstName(), ccdResponse.getAppellantSubscription().getSurname()));
        personalisation.put(PHONE_NUMBER, config.getHmctsPhoneNumber());

        if (ccdResponse.getAppellantSubscription().getAppealNumber() != null) {
            personalisation.put(MANAGE_EMAILS_LINK_LITERAL, config.getManageEmailsLink().replace(MAC_LITERAL, getMacToken(ccdResponse.getAppellantSubscription().getAppealNumber())));
            personalisation.put(TRACK_APPEAL_LINK_LITERAL, config.getTrackAppealLink() != null ? config.getTrackAppealLink().replace(APPEAL_ID_LITERAL, ccdResponse.getAppellantSubscription().getAppealNumber()) : null);
            personalisation.put(SUBMIT_EVIDENCE_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(APPEAL_ID, ccdResponse.getAppellantSubscription().getAppealNumber()));
        }

        personalisation.put(FIRST_TIER_AGENCY_ACRONYM, DWP_ACRONYM);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME, DWP_FUL_NAME);
        // TODO: Set this to the actual event date once event story has been implemented
        ZonedDateTime dwpResponseDate = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"));
        String dwpResponseDateString = formatDate(dwpResponseDate.plus(MAX_DWP_RESPONSE_DAYS, DAYS));
        personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);
        // TODO: Set this to the actual event date once event story has been implemented
        ZonedDateTime evidenceReceivedDate = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"));
        String evidenceReceivedDateString = formatDate(evidenceReceivedDate);
        personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL, evidenceReceivedDateString);
        // TODO: Set this to the actual event date once event story has been implemented
        ZonedDateTime z = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"));
        personalisation.put(HEARING_CONTACT_DATE, formatDate(z.plusWeeks(6)));

        return personalisation;
    }

    public String getMacToken(String id) {
        return macService.generateToken(id);
    }

    protected String formatDate(ZonedDateTime date) {
        return date.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT));
    }

    public Template getTemplate(EventType type) {
        String smsTemplateId = isSendSmsSubscriptionConfirmation() ? SUBSCRIPTION_CREATED.getId() : type.getId();
        return config.getTemplate(type.getId(), smsTemplateId);
    }

    public Boolean isSendSmsSubscriptionConfirmation() {
        return sendSmsSubscriptionConfirmation;
    }

    public void setSendSmsSubscriptionConfirmation(Boolean sendSmsSubscriptionConfirmation) {
        this.sendSmsSubscriptionConfirmation = sendSmsSubscriptionConfirmation;
    }
}
