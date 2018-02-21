package uk.gov.hmcts.sscs.personalisation;

import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.SUBSCRIPTION_CREATED;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.notify.Event;
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

        setEventData(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, String> setEventData(Map<String, String> personalisation, CcdResponse ccdResponse) {
        if (ccdResponse.getEvents() != null && !ccdResponse.getEvents().isEmpty()) {
            Event event = ccdResponse.getEvents().get(0);

            switch (event.getEventType()) {
                case DWP_RESPONSE_RECEIVED : {
                    String dwpResponseDateString = formatDate(addDays(event.getDate(), MAX_DWP_RESPONSE_DAYS));
                    personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);
                    setHearingContactDate(personalisation, event);
                    break;
                }
                case EVIDENCE_RECEIVED : {
                    personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL, formatDate(event.getDate()));
                    break;
                }
                case POSTPONEMENT : {
                    setHearingContactDate(personalisation, event);
                    break;
                }
                default: break;
            }
        }
        return personalisation;
    }

    private  Map<String, String> setHearingContactDate(Map<String, String> personalisation, Event event) {
        String hearingContactDate = formatDate(addDays(event.getDate(), 42));
        personalisation.put(HEARING_CONTACT_DATE, hearingContactDate);

        return personalisation;
    }

    private static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    public String getMacToken(String id) {
        return macService.generateToken(id);
    }

    protected String formatDate(Date date) {
        SimpleDateFormat dt = new SimpleDateFormat(RESPONSE_DATE_FORMAT);
        return dt.format(date);
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
