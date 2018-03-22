package uk.gov.hmcts.sscs.personalisation;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Hearing;
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
        personalisation.put(APPELLANT_NAME, String.format("%s %s", ccdResponse.getAppellantSubscription().getFirstName(), ccdResponse.getAppellantSubscription().getSurname()));
        personalisation.put(PHONE_NUMBER, config.getHmctsPhoneNumber());

        if (ccdResponse.getAppellantSubscription().getAppealNumber() != null) {
            personalisation.put(APPEAL_ID, ccdResponse.getAppellantSubscription().getAppealNumber());
            personalisation.put(MANAGE_EMAILS_LINK_LITERAL, config.getManageEmailsLink().replace(MAC_LITERAL,
                    getMacToken(ccdResponse.getAppellantSubscription().getAppealNumber(),
                            ccdResponse.getBenefitType())));
            personalisation.put(TRACK_APPEAL_LINK_LITERAL, config.getTrackAppealLink() != null ? config.getTrackAppealLink().replace(APPEAL_ID_LITERAL, ccdResponse.getAppellantSubscription().getAppealNumber()) : null);
            personalisation.put(SUBMIT_EVIDENCE_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(APPEAL_ID, ccdResponse.getAppellantSubscription().getAppealNumber()));
            personalisation.put(CLAIMING_EXPENSES_LINK_LITERAL, config.getClaimingExpensesLink().replace(APPEAL_ID, ccdResponse.getAppellantSubscription().getAppealNumber()));
            personalisation.put(HEARING_INFO_LINK_LITERAL,
                    config.getHearingInfoLink().replace(APPEAL_ID_LITERAL, ccdResponse.getAppellantSubscription().getAppealNumber()));
        }

        personalisation.put(FIRST_TIER_AGENCY_ACRONYM, DWP_ACRONYM);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME, DWP_FUL_NAME);

        if (ccdResponse.getHearings() != null && ccdResponse.getHearings().size() > 0) {
            Hearing latestHearing = ccdResponse.getHearings().get(0);

            personalisation.put(HEARING_DATE, formatDate(latestHearing.getHearingDateTime()));
            personalisation.put(HEARING_TIME, formatTime(latestHearing.getHearingDateTime()));
            personalisation.put(VENUE_ADDRESS_LITERAL, formatAddress(latestHearing));
            personalisation.put(VENUE_MAP_LINK_LITERAL, latestHearing.getVenueGoogleMapUrl());
        }

        setEventData(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, String> setEventData(Map<String, String> personalisation, CcdResponse ccdResponse) {
        if (ccdResponse.getEvents() != null) {

            for (Event event : ccdResponse.getEvents()) {
                switch (ccdResponse.getNotificationType()) {
                    case APPEAL_RECEIVED: {
                        if (event.getEventType().equals(APPEAL_RECEIVED)) {
                            String dwpResponseDateString = formatDate(event.getDateTime().plusDays(MAX_DWP_RESPONSE_DAYS));
                            personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);
                            setHearingContactDate(personalisation, event);
                            return personalisation;
                        }
                        break;
                    }
                    case EVIDENCE_RECEIVED: {
                        if (event.getEventType().equals(EVIDENCE_RECEIVED)) {
                            personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL, formatDate(event.getDateTime()));
                            return personalisation;
                        }
                         break;
                    }
                    case POSTPONEMENT: {
                        if (event.getEventType().equals(POSTPONEMENT)) {
                            setHearingContactDate(personalisation, event);
                            return personalisation;
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        return personalisation;
    }

    private String formatAddress(Hearing hearing) {
        return newArrayList(hearing.getVenueName(),
                hearing.getVenueAddressLine1(),
                hearing.getVenueAddressLine2(),
                hearing.getVenueTown(),
                hearing.getVenueCounty(),
                hearing.getVenuePostcode())
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    private  Map<String, String> setHearingContactDate(Map<String, String> personalisation, Event event) {
        String hearingContactDate = formatDate(event.getDateTime().plusDays(42));
        personalisation.put(HEARING_CONTACT_DATE, hearingContactDate);

        return personalisation;
    }

    public String getMacToken(String id, String benefitType) {
        return macService.generateToken(id, benefitType);
    }

    public String formatDate(ZonedDateTime date) {
        return date.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT));
    }

    protected String formatTime(ZonedDateTime date) {
        return date.format(DateTimeFormatter.ofPattern(HEARING_TIME_FORMAT));
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
