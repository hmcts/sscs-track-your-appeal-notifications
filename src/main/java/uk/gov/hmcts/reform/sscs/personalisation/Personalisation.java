package uk.gov.hmcts.reform.sscs.personalisation;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.ACCEPT_VIEW_BY_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.ONLINE_HEARING_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.ONLINE_HEARING_REGISTER_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.ONLINE_HEARING_SIGN_IN_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.QUESTION_ROUND_EXPIRES_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.TRIBUNAL_RESPONSE_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class Personalisation<E extends NotificationWrapper> {

    private boolean sendSmsSubscriptionConfirmation;

    @Autowired
    protected NotificationConfig config;

    @Autowired
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Autowired
    private MessageAuthenticationServiceImpl macService;

    @Autowired
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Autowired
    private NotificationDateConverterUtil notificationDateConverterUtil;

    public Map<String, String> create(E notificationWrapper) {
        return create(notificationWrapper.getSscsCaseDataWrapper());
    }

    protected Map<String, String> create(SscsCaseDataWrapper responseWrapper) {
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();
        Map<String, String> personalisation = new HashMap<>();

        Benefit benefit = getBenefitByCode(ccdResponse.getAppeal().getBenefitType().getCode());

        personalisation.put(AppConstants.PANEL_COMPOSITION, getPanelCompositionByBenefitType(benefit));
        personalisation.put(AppConstants.DECISION_POSTED_RECEIVE_DATE, formatLocalDate(LocalDate.now().plusDays(7)));
        personalisation.put(AppConstants.BENEFIT_NAME_ACRONYM_LITERAL, benefit.name());
        personalisation.put(AppConstants.BENEFIT_NAME_ACRONYM_SHORT_LITERAL, benefit.name());
        personalisation.put(AppConstants.BENEFIT_FULL_NAME_LITERAL, benefit.getDescription());
        personalisation.put(AppConstants.APPEAL_REF, ccdResponse.getCaseReference());
        personalisation.put(AppConstants.APPELLANT_NAME, String.format("%s %s",
                ccdResponse.getAppeal().getAppellant().getName().getFirstName(), ccdResponse.getAppeal().getAppellant().getName().getLastName()));
        personalisation.put(AppConstants.PHONE_NUMBER, config.getHmctsPhoneNumber());

        Subscription appellantOrAppointeeSubscription = hasAppointee(responseWrapper)
                ? ccdResponse.getSubscriptions().getAppointeeSubscription()
                : ccdResponse.getSubscriptions().getAppellantSubscription();

        if (appellantOrAppointeeSubscription != null) {
            String tya = StringUtils.defaultIfBlank(appellantOrAppointeeSubscription.getTya(), StringUtils.EMPTY);
            personalisation.put(AppConstants.APPEAL_ID, tya);
            personalisation.put(AppConstants.MANAGE_EMAILS_LINK_LITERAL, config.getManageEmailsLink().replace(AppConstants.MAC_LITERAL,
                    getMacToken(tya,
                            benefit.name())));
            personalisation.put(AppConstants.TRACK_APPEAL_LINK_LITERAL, config.getTrackAppealLink() != null ? config.getTrackAppealLink().replace(AppConstants.APPEAL_ID_LITERAL, tya) : null);
            personalisation.put(AppConstants.SUBMIT_EVIDENCE_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(AppConstants.APPEAL_ID, tya));
            personalisation.put(AppConstants.SUBMIT_EVIDENCE_INFO_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(AppConstants.APPEAL_ID_LITERAL, tya));
            personalisation.put(AppConstants.CLAIMING_EXPENSES_LINK_LITERAL, config.getClaimingExpensesLink().replace(AppConstants.APPEAL_ID, tya));
            personalisation.put(AppConstants.HEARING_INFO_LINK_LITERAL,
                    config.getHearingInfoLink().replace(AppConstants.APPEAL_ID_LITERAL, tya));

            String email = appellantOrAppointeeSubscription.getEmail();
            if (email != null) {
                try {
                    String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.name());
                    personalisation.put(ONLINE_HEARING_LINK_LITERAL, config.getOnlineHearingLinkWithEmail().replace("{email}", encodedEmail));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        personalisation.put(AppConstants.FIRST_TIER_AGENCY_ACRONYM, AppConstants.DWP_ACRONYM);
        personalisation.put(AppConstants.FIRST_TIER_AGENCY_FULL_NAME, AppConstants.DWP_FUL_NAME);

        if (ccdResponse.getHearings() != null && !ccdResponse.getHearings().isEmpty()) {
            Hearing latestHearing = ccdResponse.getHearings().get(0);

            LocalDateTime hearingDateTime = latestHearing.getValue().getHearingDateTime();
            personalisation.put(AppConstants.HEARING_DATE, formatLocalDate(hearingDateTime.toLocalDate()));
            personalisation.put(AppConstants.HEARING_TIME, formatLocalTime(hearingDateTime));
            personalisation.put(AppConstants.VENUE_ADDRESS_LITERAL, formatAddress(latestHearing));
            personalisation.put(AppConstants.VENUE_MAP_LINK_LITERAL, latestHearing.getValue().getVenue().getGoogleMapLink());
            personalisation.put(AppConstants.DAYS_TO_HEARING_LITERAL, calculateDaysToHearingText(hearingDateTime.toLocalDate()));
        }

        setEvidenceProcessingAddress(personalisation, ccdResponse);

        NotificationEventType notificationEventType = responseWrapper.getNotificationEventType();
        setEventData(personalisation, ccdResponse, notificationEventType);
        setEvidenceReceivedNotificationData(personalisation, ccdResponse, notificationEventType);
        setHearingContactDate(personalisation, responseWrapper);

        LocalDate today = LocalDate.now();
        personalisation.put(TRIBUNAL_RESPONSE_DATE_LITERAL, notificationDateConverterUtil.toEmailDate(today.plusDays(7)));
        personalisation.put(ACCEPT_VIEW_BY_DATE_LITERAL, notificationDateConverterUtil.toEmailDate(today.plusDays(7)));
        personalisation.put(QUESTION_ROUND_EXPIRES_DATE_LITERAL, notificationDateConverterUtil.toEmailDate(today.plusDays(1)));

        personalisation.put(ONLINE_HEARING_REGISTER_LINK_LITERAL, config.getOnlineHearingLink() + "/register");
        personalisation.put(ONLINE_HEARING_SIGN_IN_LINK_LITERAL, config.getOnlineHearingLink() + "/sign-in");

        return personalisation;
    }

    private String getPanelCompositionByBenefitType(Benefit benefit) {
        if (Benefit.PIP.equals(benefit)) {
            return AppConstants.PIP_PANEL_COMPOSITION;
        }
        return AppConstants.ESA_PANEL_COMPOSITION;
    }

    void setHearingContactDate(Map<String, String> personalisation, SscsCaseDataWrapper wrapper) {
        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(wrapper);
        hearingContactDate.ifPresent(zonedDateTime -> personalisation.put(AppConstants.HEARING_CONTACT_DATE,
                formatLocalDate(zonedDateTime.toLocalDate())
        ));
    }

    public Map<String, String> setEventData(Map<String, String> personalisation, SscsCaseData ccdResponse, NotificationEventType notificationEventType) {
        if (ccdResponse.getEvents() != null) {

            for (Event event : ccdResponse.getEvents()) {
                if ((event.getValue() != null)
                    && ((notificationEventType.equals(APPEAL_RECEIVED_NOTIFICATION) && event.getValue().getEventType().equals(APPEAL_RECEIVED))
                    || (notificationEventType.equals(DWP_RESPONSE_LATE_REMINDER_NOTIFICATION)))
                    || notificationEventType.equals(INTERLOC_VALID_APPEAL)) {
                    return setAppealReceivedDetails(personalisation, event.getValue());
                }
            }
        }
        return personalisation;
    }

    public Map<String, String> setEvidenceReceivedNotificationData(Map<String, String> personalisation, SscsCaseData ccdResponse, NotificationEventType notificationEventType) {
        if (notificationEventType.equals(EVIDENCE_RECEIVED_NOTIFICATION)) {
            if (ccdResponse.getEvidence() != null && ccdResponse.getEvidence().getDocuments() != null && !ccdResponse.getEvidence().getDocuments().isEmpty()) {
                personalisation.put(AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL, formatLocalDate(ccdResponse.getEvidence().getDocuments().get(0).getValue().getEvidenceDateTimeFormatted()));
            } else {
                personalisation.put(AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL, StringUtils.EMPTY);
            }
        }
        return personalisation;
    }

    private Map<String, String> setAppealReceivedDetails(Map<String, String> personalisation, EventDetails eventDetails) {
        String dwpResponseDateString = formatLocalDate(eventDetails.getDateTime().plusDays(AppConstants.MAX_DWP_RESPONSE_DAYS).toLocalDate());
        personalisation.put(AppConstants.APPEAL_RESPOND_DATE, dwpResponseDateString);
        return personalisation;
    }

    public Map<String, String> setEvidenceProcessingAddress(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        RegionalProcessingCenter rpc;

        if (null != ccdResponse.getRegionalProcessingCenter()) {
            rpc = ccdResponse.getRegionalProcessingCenter();
        } else {
            rpc = regionalProcessingCenterService.getByScReferenceCode(ccdResponse.getCaseReference());
        }
        personalisation.put(AppConstants.REGIONAL_OFFICE_NAME_LITERAL, rpc.getAddress1());
        personalisation.put(AppConstants.SUPPORT_CENTRE_NAME_LITERAL, rpc.getAddress2());
        personalisation.put(AppConstants.ADDRESS_LINE_LITERAL, rpc.getAddress3());
        personalisation.put(AppConstants.TOWN_LITERAL, rpc.getAddress4());
        personalisation.put(AppConstants.COUNTY_LITERAL, rpc.getCity());
        personalisation.put(AppConstants.POSTCODE_LITERAL, rpc.getPostcode());

        return personalisation;
    }

    private String formatAddress(Hearing hearing) {
        return newArrayList(hearing.getValue().getVenue().getName(),
                hearing.getValue().getVenue().getAddress().getFullAddress())
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    private String calculateDaysToHearingText(LocalDate hearingDate) {
        Long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), hearingDate);

        return daysBetween == 1 ? AppConstants.TOMORROW_STRING : "in " + daysBetween + AppConstants.DAYS_STRING;
    }

    public String getMacToken(String id, String benefitType) {
        return macService.generateToken(id, benefitType);
    }

    private String formatLocalDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(AppConstants.RESPONSE_DATE_FORMAT));
    }

    private String formatLocalTime(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern(AppConstants.HEARING_TIME_FORMAT));
    }

    public Template getTemplate(E notificationWrapper, Benefit benefit, SubscriptionType subscriptionType) {
        String emailTemplateName = getEmailTemplateName(subscriptionType, notificationWrapper.getNotificationType());
        String smsTemplateName = isSendSmsSubscriptionConfirmation() ? SUBSCRIPTION_CREATED_NOTIFICATION.getId() :
                emailTemplateName;
        String letterTemplateName = getLetterTemplateName(subscriptionType, notificationWrapper.getNotificationType());
        return config.getTemplate(emailTemplateName, smsTemplateName, letterTemplateName, benefit, notificationWrapper.getHearingType());
    }

    private String getEmailTemplateName(SubscriptionType subscriptionType,
                                        NotificationEventType notificationEventType) {
        String emailTemplateName = notificationEventType.getId();
        if (APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType)
            || APPEAL_WITHDRAWN_NOTIFICATION.equals(notificationEventType)
            || EVIDENCE_RECEIVED_NOTIFICATION.equals(notificationEventType)
            || SYA_APPEAL_CREATED_NOTIFICATION.equals(notificationEventType)
            || RESEND_APPEAL_CREATED_NOTIFICATION.equals(notificationEventType)
            || APPEAL_DORMANT_NOTIFICATION.equals(notificationEventType)
            || ADJOURNED_NOTIFICATION.equals(notificationEventType)
            || APPEAL_RECEIVED_NOTIFICATION.equals(notificationEventType)
            || POSTPONEMENT_NOTIFICATION.equals(notificationEventType)
            || HEARING_BOOKED_NOTIFICATION.equals(notificationEventType)
            || INTERLOC_VALID_APPEAL.equals(notificationEventType)) {
            emailTemplateName = emailTemplateName + "." + subscriptionType.name().toLowerCase();
        }
        return emailTemplateName;
    }

    private String getLetterTemplateName(SubscriptionType subscriptionType, NotificationEventType notificationEventType) {
        String letterTemplateName = notificationEventType.getId();
        if (INTERLOC_VALID_APPEAL.equals(notificationEventType)) {
            letterTemplateName = letterTemplateName + "." + subscriptionType.name().toLowerCase();
        }
        return letterTemplateName;
    }

    public Boolean isSendSmsSubscriptionConfirmation() {
        return sendSmsSubscriptionConfirmation;
    }

    public void setSendSmsSubscriptionConfirmation(Boolean sendSmsSubscriptionConfirmation) {
        this.sendSmsSubscriptionConfirmation = sendSmsSubscriptionConfirmation;
    }
}
