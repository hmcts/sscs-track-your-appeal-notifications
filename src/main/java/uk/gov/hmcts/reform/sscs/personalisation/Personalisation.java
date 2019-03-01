package uk.gov.hmcts.reform.sscs.personalisation;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasRepresentative;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class Personalisation<E extends NotificationWrapper> {
    private static final List<NotificationEventType> FALLBACK_LETTER_SUBSCRIPTION_TYPES = Arrays.asList(APPEAL_LODGED, SYA_APPEAL_CREATED_NOTIFICATION);
    private static final String CRLF = String.format("%c%c", (char) 0x0D, (char) 0x0A);

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

    public Map<String, String> create(final E notificationWrapper, final SubscriptionWithType subscriptionWithType) {
        return create(notificationWrapper.getSscsCaseDataWrapper(), subscriptionWithType);
    }

    protected Map<String, String> create(final SscsCaseDataWrapper responseWrapper, final SubscriptionWithType subscriptionWithType) {
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();
        Map<String, String> personalisation = new HashMap<>();
        Benefit benefit = getBenefitByCode(ccdResponse.getAppeal().getBenefitType().getCode());
        personalisation.put(PANEL_COMPOSITION, getPanelCompositionByBenefitType(benefit));
        personalisation.put(DECISION_POSTED_RECEIVE_DATE, formatLocalDate(LocalDate.now().plusDays(7)));
        personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL, benefit.name());
        personalisation.put(BENEFIT_NAME_ACRONYM_SHORT_LITERAL, benefit.name());
        personalisation.put(BENEFIT_FULL_NAME_LITERAL, benefit.getDescription());
        personalisation.put(APPEAL_REF, getAppealReference(ccdResponse));
        personalisation.put(APPELLANT_NAME, ccdResponse.getAppeal().getAppellant().getName().getFullNameNoTitle());
        personalisation.put(NAME, getName(subscriptionWithType.getSubscriptionType(), ccdResponse, responseWrapper));
        personalisation.put(PHONE_NUMBER, config.getHmctsPhoneNumber());
        personalisation.put(CCD_ID, StringUtils.defaultIfBlank(ccdResponse.getCcdCaseId(), StringUtils.EMPTY));

        subscriptionDetails(personalisation, subscriptionWithType.getSubscription(), benefit);

        personalisation.put(FIRST_TIER_AGENCY_ACRONYM, DWP_ACRONYM);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME, DWP_FUL_NAME);

        if (ccdResponse.getHearings() != null && !ccdResponse.getHearings().isEmpty()) {
            Hearing latestHearing = ccdResponse.getHearings().get(0);
            LocalDateTime hearingDateTime = latestHearing.getValue().getHearingDateTime();

            personalisation.put(HEARING_DATE, formatLocalDate(hearingDateTime.toLocalDate()));
            personalisation.put(HEARING_TIME, formatLocalTime(hearingDateTime));
            personalisation.put(VENUE_ADDRESS_LITERAL, formatAddress(latestHearing));
            personalisation.put(VENUE_MAP_LINK_LITERAL, latestHearing.getValue().getVenue().getGoogleMapLink());
            personalisation.put(DAYS_TO_HEARING_LITERAL, calculateDaysToHearingText(hearingDateTime.toLocalDate()));
        }

        setEvidenceProcessingAddress(personalisation, ccdResponse);

        NotificationEventType notificationEventType = responseWrapper.getNotificationEventType();
        setEventData(personalisation, ccdResponse, notificationEventType);
        setEvidenceReceivedNotificationData(personalisation, ccdResponse, notificationEventType);
        setHearingContactDate(personalisation, responseWrapper);

        LocalDate today = LocalDate.now();
        personalisation.put(TRIBUNAL_RESPONSE_DATE_LITERAL, notificationDateConverterUtil.toEmailDate(today.plusDays(56)));
        personalisation.put(ACCEPT_VIEW_BY_DATE_LITERAL, notificationDateConverterUtil.toEmailDate(today.plusDays(7)));
        personalisation.put(QUESTION_ROUND_EXPIRES_DATE_LITERAL, notificationDateConverterUtil.toEmailDate(today.plusDays(1)));

        personalisation.put(ONLINE_HEARING_REGISTER_LINK_LITERAL, config.getOnlineHearingLink() + "/register");
        personalisation.put(ONLINE_HEARING_SIGN_IN_LINK_LITERAL, config.getOnlineHearingLink() + "/sign-in");

        personalisation.put(APPOINTEE_DESCRIPTION, getAppointeeDescription(subscriptionWithType.getSubscriptionType(), ccdResponse));

        return personalisation;
    }

    private String getAppealReference(SscsCaseData ccdResponse) {
        final String caseReference = ccdResponse.getCaseReference();
        return StringUtils.isBlank(caseReference) ? ccdResponse.getCcdCaseId() : caseReference;
    }

    private String getName(SubscriptionType subscriptionType, SscsCaseData ccdResponse, SscsCaseDataWrapper wrapper) {
        if (ccdResponse.getAppeal() == null) {
            return "";
        }

        Name name = null;

        if (subscriptionType.equals(APPELLANT)
                && ccdResponse.getAppeal().getAppellant() != null) {
            name = ccdResponse.getAppeal().getAppellant().getName();
        } else if (subscriptionType.equals(REPRESENTATIVE)
                && hasRepresentative(wrapper)) {
            name = ccdResponse.getAppeal().getRep().getName();
        } else if (subscriptionType.equals(APPOINTEE)
                && hasAppointee(wrapper)) {
            name = ccdResponse.getAppeal().getAppellant().getAppointee().getName();
        }
        return name == null ? "" : name.getFullNameNoTitle();
    }

    private String getAppointeeDescription(SubscriptionType subscriptionType, SscsCaseData ccdResponse) {
        if (APPOINTEE.equals(subscriptionType) && ccdResponse.getAppeal() != null
                && ccdResponse.getAppeal().getAppellant().getName() != null) {
            return String.format("You are receiving this update as the appointee for %s.%s%s",
                    ccdResponse.getAppeal().getAppellant().getName().getFullNameNoTitle(), CRLF, CRLF);
        } else {
            return "";
        }
    }

    private void subscriptionDetails(Map<String, String> personalisation, Subscription subscription, Benefit benefit) {
        final String tya = StringUtils.defaultIfBlank(subscription.getTya(), StringUtils.EMPTY);
        personalisation.put(APPEAL_ID, tya);
        personalisation.put(MANAGE_EMAILS_LINK_LITERAL, config.getManageEmailsLink().replace(MAC_LITERAL,
                getMacToken(tya, benefit.name())));
        personalisation.put(TRACK_APPEAL_LINK_LITERAL, config.getTrackAppealLink() != null ? config.getTrackAppealLink().replace(APPEAL_ID_LITERAL, tya) : null);
        personalisation.put(SUBMIT_EVIDENCE_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(APPEAL_ID, tya));
        personalisation.put(SUBMIT_EVIDENCE_INFO_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(APPEAL_ID_LITERAL, tya));
        personalisation.put(CLAIMING_EXPENSES_LINK_LITERAL, config.getClaimingExpensesLink().replace(APPEAL_ID, tya));
        personalisation.put(HEARING_INFO_LINK_LITERAL,
                config.getHearingInfoLink().replace(APPEAL_ID_LITERAL, tya));

        String email = subscription.getEmail();
        if (email != null) {
            try {
                String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.name());
                personalisation.put(ONLINE_HEARING_LINK_LITERAL, config.getOnlineHearingLinkWithEmail().replace("{email}", encodedEmail));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getPanelCompositionByBenefitType(Benefit benefit) {
        if (Benefit.PIP.equals(benefit)) {
            return PIP_PANEL_COMPOSITION;
        }
        return ESA_PANEL_COMPOSITION;
    }

    void setHearingContactDate(Map<String, String> personalisation, SscsCaseDataWrapper wrapper) {
        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(wrapper);
        hearingContactDate.ifPresent(zonedDateTime -> personalisation.put(HEARING_CONTACT_DATE,
                formatLocalDate(zonedDateTime.toLocalDate())
        ));
    }

    Map<String, String> setEventData(Map<String, String> personalisation, SscsCaseData ccdResponse, NotificationEventType notificationEventType) {
        if (ccdResponse.getEvents() != null) {

            for (Event event : ccdResponse.getEvents()) {
                if ((event.getValue() != null)
                    && ((notificationEventType.equals(APPEAL_RECEIVED_NOTIFICATION) && event.getValue().getEventType().equals(APPEAL_RECEIVED))
                    || (notificationEventType.equals(DWP_RESPONSE_LATE_REMINDER_NOTIFICATION)))
                    || notificationEventType.equals(CASE_UPDATED)) {
                    return setAppealReceivedDetails(personalisation, event.getValue());
                }
            }
        }
        return personalisation;
    }

    Map<String, String> setEvidenceReceivedNotificationData(Map<String, String> personalisation,
                                                            SscsCaseData ccdResponse,
                                                            NotificationEventType notificationEventType) {
        if (notificationEventType.equals(EVIDENCE_RECEIVED_NOTIFICATION)) {
            if (ccdResponse.getEvidence() != null && ccdResponse.getEvidence().getDocuments() != null
                    && !ccdResponse.getEvidence().getDocuments().isEmpty()) {
                personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL,
                        formatLocalDate(ccdResponse.getEvidence().getDocuments().get(0).getValue()
                                .getEvidenceDateTimeFormatted()));
            } else {
                personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL, StringUtils.EMPTY);
            }
        }
        return personalisation;
    }

    private Map<String, String> setAppealReceivedDetails(Map<String, String> personalisation, EventDetails eventDetails) {
        String dwpResponseDateString = formatLocalDate(eventDetails.getDateTime().plusDays(MAX_DWP_RESPONSE_DAYS).toLocalDate());
        personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);
        return personalisation;
    }

    Map<String, String> setEvidenceProcessingAddress(Map<String, String> personalisation, SscsCaseData ccdResponse) {
        RegionalProcessingCenter rpc;

        if (hasRegionalProcessingCenter(ccdResponse)) {
            rpc = ccdResponse.getRegionalProcessingCenter();
        } else {
            rpc = regionalProcessingCenterService.getByScReferenceCode(ccdResponse.getCaseReference());
        }
        if (rpc != null) {
            personalisation.put(REGIONAL_OFFICE_NAME_LITERAL, rpc.getAddress1());
            personalisation.put(SUPPORT_CENTRE_NAME_LITERAL, rpc.getAddress2());
            personalisation.put(ADDRESS_LINE_LITERAL, rpc.getAddress3());
            personalisation.put(TOWN_LITERAL, rpc.getAddress4());
            personalisation.put(COUNTY_LITERAL, rpc.getCity());
            personalisation.put(POSTCODE_LITERAL, rpc.getPostcode());
            personalisation.put(REGIONAL_OFFICE_POSTCODE_LITERAL, rpc.getPostcode());
        }

        return personalisation;
    }

    private static boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return null != ccdResponse.getRegionalProcessingCenter()
            && null != ccdResponse.getRegionalProcessingCenter().getName();
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

        return daysBetween == 1 ? TOMORROW_STRING : "in " + daysBetween + DAYS_STRING;
    }

    private String getMacToken(String id, String benefitType) {
        return macService.generateToken(id, benefitType);
    }

    private String formatLocalDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT));
    }

    private String formatLocalTime(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern(HEARING_TIME_FORMAT));
    }

    public Template getTemplate(E notificationWrapper, Benefit benefit, SubscriptionType subscriptionType) {
        String templateConfig = getTemplateConfig(subscriptionType, notificationWrapper.getNotificationType());
        String smsTemplateName = isSendSmsSubscriptionConfirmation() ? SUBSCRIPTION_CREATED_NOTIFICATION.getId() + "." + subscriptionType.toString().toLowerCase() :
                templateConfig;
        String letterTemplateName = getLetterTemplateName(subscriptionType, notificationWrapper.getNotificationType());
        return config.getTemplate(templateConfig, smsTemplateName, letterTemplateName, benefit, notificationWrapper.getHearingType());
    }

    private String getTemplateConfig(SubscriptionType subscriptionType,
                                     NotificationEventType notificationEventType) {
        String templateConfig = notificationEventType.getId();
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
            || APPEAL_LODGED.equals(notificationEventType)) {
            templateConfig = templateConfig + "." + StringUtils.lowerCase(subscriptionType.name());
        }
        return templateConfig;
    }

    private String getLetterTemplateName(SubscriptionType subscriptionType, NotificationEventType notificationEventType) {
        String letterTemplateName = notificationEventType.getId();
        if (FALLBACK_LETTER_SUBSCRIPTION_TYPES.contains(notificationEventType)) {
            letterTemplateName = letterTemplateName + "." + subscriptionType.name().toLowerCase();
        }
        return letterTemplateName;
    }

    Boolean isSendSmsSubscriptionConfirmation() {
        return sendSmsSubscriptionConfirmation;
    }

    void setSendSmsSubscriptionConfirmation(Boolean sendSmsSubscriptionConfirmation) {
        this.sendSmsSubscriptionConfirmation = sendSmsSubscriptionConfirmation;
    }
}
