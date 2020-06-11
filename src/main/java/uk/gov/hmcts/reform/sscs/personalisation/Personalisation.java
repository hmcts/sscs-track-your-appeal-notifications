package uk.gov.hmcts.reform.sscs.personalisation;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.personalisation.SyaAppealCreatedAndReceivedPersonalisation.*;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.exception.BenefitMappingException;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.*;

@Component
@Slf4j
public class Personalisation<E extends NotificationWrapper> {
    private static final List<NotificationEventType> LETTER_SUBSCRIPTION_TYPES = Arrays.asList(DWP_RESPONSE_RECEIVED_NOTIFICATION, DWP_UPLOAD_RESPONSE_NOTIFICATION,
        APPEAL_RECEIVED_NOTIFICATION, SYA_APPEAL_CREATED_NOTIFICATION, EVIDENCE_RECEIVED_NOTIFICATION, NON_COMPLIANT_NOTIFICATION, VALID_APPEAL_CREATED);

    private static final String CRLF = String.format("%c%c", (char) 0x0D, (char) 0x0A);

    @Autowired
    protected NotificationConfig config;
    private boolean sendSmsSubscriptionConfirmation;
    @Autowired
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Autowired
    private MessageAuthenticationServiceImpl macService;

    @Autowired
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Autowired
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @Autowired
    private EvidenceProperties evidenceProperties;

    private static String tya(Subscription subscription) {
        if (subscription != null) {
            return StringUtils.defaultIfBlank(subscription.getTya(), StringUtils.EMPTY);
        } else {
            return StringUtils.EMPTY;
        }
    }

    private static String email(Subscription subscription) {
        return subscription != null ? subscription.getEmail() : null;
    }

    private static boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return null != ccdResponse.getRegionalProcessingCenter()
            && null != ccdResponse.getRegionalProcessingCenter().getName();
    }

    protected static String getLatestInfoRequestDetail(SscsCaseData ccdResponse) {
        if (ccdResponse.getInfoRequests() != null) {
            List<AppellantInfoRequest> infoRequests = ccdResponse.getInfoRequests().getAppellantInfoRequest();

            if (infoRequests.isEmpty()) {
                return null;
            }

            AppellantInfoRequest latestAppellantInfoRequest = null;
            for (AppellantInfoRequest infoRequest : infoRequests) {
                latestAppellantInfoRequest = getLatestAppellantInfoRequest(latestAppellantInfoRequest, infoRequest);
            }

            if (latestAppellantInfoRequest != null && latestAppellantInfoRequest.getAppellantInfo() != null) {
                return latestAppellantInfoRequest.getAppellantInfo().getParagraph();
            }
        }

        return null;
    }

    private static AppellantInfoRequest getLatestAppellantInfoRequest(AppellantInfoRequest
                                                                          latestAppellantInfoRequest, AppellantInfoRequest infoRequest) {
        if (latestAppellantInfoRequest == null) {
            latestAppellantInfoRequest = infoRequest;
        } else {
            LocalDate latestDate = LocalDate.parse(latestAppellantInfoRequest.getAppellantInfo().getRequestDate(), CC_DATE_FORMAT);
            LocalDate currentDate = LocalDate.parse(infoRequest.getAppellantInfo().getRequestDate(), CC_DATE_FORMAT);
            if (currentDate.isAfter(latestDate)) {
                latestAppellantInfoRequest = infoRequest;
            }
        }
        return latestAppellantInfoRequest;
    }

    public Map<String, String> create(final E notificationWrapper, final SubscriptionWithType subscriptionWithType) {
        return create(notificationWrapper.getSscsCaseDataWrapper(), subscriptionWithType);
    }

    protected Map<String, String> create(final SscsCaseDataWrapper responseWrapper, final SubscriptionWithType subscriptionWithType) {
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();
        Map<String, String> personalisation = new HashMap<>();
        Benefit benefit = null;
        try {
            if (ccdResponse.getAppeal() != null
                && ccdResponse.getAppeal().getBenefitType() != null
                && !StringUtils.isEmpty(ccdResponse.getAppeal().getBenefitType().getCode())) {
                benefit = getBenefitByCode(ccdResponse.getAppeal().getBenefitType().getCode());
                personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL, benefit.name());
                personalisation.put(BENEFIT_NAME_ACRONYM_SHORT_LITERAL, benefit.name());
                personalisation.put(BENEFIT_FULL_NAME_LITERAL, benefit.getDescription());
            } else {
                log.warn("Proceeding with 'null' benefit type for case !");
            }
        } catch (BenefitMappingException bme) {
            log.warn("Proceeding with 'null' benefit type for case !");
        }
        personalisation.put(PANEL_COMPOSITION, getPanelCompositionByBenefitType(benefit));
        personalisation.put(DECISION_POSTED_RECEIVE_DATE, formatLocalDate(LocalDate.now().plusDays(7)));
        personalisation.put(APPEAL_REF, getAppealReference(ccdResponse));
        personalisation.put(APPELLANT_NAME, ccdResponse.getAppeal().getAppellant().getName().getFullNameNoTitle());
        personalisation.put(NAME, getName(subscriptionWithType.getSubscriptionType(), ccdResponse, responseWrapper));
        personalisation.put(CCD_ID, StringUtils.defaultIfBlank(ccdResponse.getCcdCaseId(), StringUtils.EMPTY));

        // Some templates (notably letters) can be sent out before the SC Ref is added to the case
        // this allows those templates to be populated with either the CCD Id or SC Ref
        personalisation.put(CASE_REFERENCE_ID, getAppealReference(ccdResponse));

        personalisation.put(INFO_REQUEST_DETAIL, StringUtils.defaultIfBlank(getLatestInfoRequestDetail(ccdResponse), StringUtils.EMPTY));

        Subscription subscription = subscriptionWithType.getSubscription();
        subscriptionDetails(personalisation, subscription, benefit, ccdResponse);

        personalisation.put(FIRST_TIER_AGENCY_ACRONYM, DWP_ACRONYM);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME, DWP_FUL_NAME);

        if (ccdResponse.getHearings() != null && !ccdResponse.getHearings().isEmpty()) {

            Hearing latestHearing = NotificationUtils.getLatestHearing(ccdResponse);
            if (latestHearing != null) {
                HearingDetails latestHearingValue = latestHearing.getValue();
                LocalDateTime hearingDateTime = latestHearingValue.getHearingDateTime();
                personalisation.put(HEARING_DATE, formatLocalDate(hearingDateTime.toLocalDate()));
                personalisation.put(HEARING_TIME, formatLocalTime(hearingDateTime));
                personalisation.put(VENUE_ADDRESS_LITERAL, formatAddress(latestHearing));
                personalisation.put(VENUE_MAP_LINK_LITERAL, latestHearingValue.getVenue().getGoogleMapLink());
                personalisation.put(DAYS_TO_HEARING_LITERAL, calculateDaysToHearingText(hearingDateTime.toLocalDate()));
            }
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

        final String tya = tya(subscription);
        personalisation.put(ONLINE_HEARING_REGISTER_LINK_LITERAL, config.getOnlineHearingLink() + "/register?tya=" + tya);
        personalisation.put(ONLINE_HEARING_SIGN_IN_LINK_LITERAL, config.getOnlineHearingLink() + "/sign-in");

        personalisation.put(APPOINTEE_DESCRIPTION, getAppointeeDescription(subscriptionWithType.getSubscriptionType(), ccdResponse));

        personalisation.put(HEARING_TYPE, responseWrapper.getNewSscsCaseData().getAppeal().getHearingType());

        if (subscriptionWithType.getSubscriptionType().equals(REPRESENTATIVE)) {
            personalisation.put(AppConstants.REPRESENTATIVE, "Yes");
        }

        return personalisation;
    }

    private String getAppealReference(SscsCaseData ccdResponse) {
        final String caseReference = ccdResponse.getCaseReference();
        return StringUtils.isBlank(caseReference) || (ccdResponse.getCreatedInGapsFrom() != null && ccdResponse.getCreatedInGapsFrom().equals("readyToList"))
            ? ccdResponse.getCcdCaseId() : caseReference;
    }

    private String getName(SubscriptionType subscriptionType, SscsCaseData ccdResponse, SscsCaseDataWrapper wrapper) {
        if (ccdResponse.getAppeal() == null) {
            return "";
        }

        if (subscriptionType.equals(APPELLANT)
            && ccdResponse.getAppeal().getAppellant() != null) {
            return getDefaultName(ccdResponse.getAppeal().getAppellant().getName());
        } else if (subscriptionType.equals(REPRESENTATIVE)
            && hasRepresentative(wrapper)) {
            return SendNotificationHelper.getRepSalutation(ccdResponse.getAppeal().getRep(), true);
        } else if (subscriptionType.equals(APPOINTEE)
            && hasAppointee(wrapper)) {
            return getDefaultName(ccdResponse.getAppeal().getAppellant().getAppointee().getName());
        }

        return "";
    }

    private String getDefaultName(Name name) {
        return name == null || name.getFirstName() == null || StringUtils.isBlank(name.getFirstName())
            || name.getLastName() == null || StringUtils.isBlank(name.getLastName()) ? "" : name.getFullNameNoTitle();
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

    private void subscriptionDetails(Map<String, String> personalisation, Subscription subscription, Benefit benefit, SscsCaseData sscsCaseData) {
        final String tya = tya(subscription);
        personalisation.put(APPEAL_ID_LITERAL, tya);
        if (benefit != null) {
            personalisation.put(MANAGE_EMAILS_LINK_LITERAL, config.getManageEmailsLink().replace(MAC_LITERAL, getMacToken(tya, benefit.name())));
        }
        if (StringUtils.equalsIgnoreCase(State.READY_TO_LIST.getId(), sscsCaseData.getCreatedInGapsFrom())) {
            personalisation.put(TRACK_APPEAL_LINK_LITERAL, config.getMyaLink() != null ? config.getMyaLink().replace(APPEAL_ID_LITERAL, tya) : null);
            personalisation.put(CLAIMING_EXPENSES_LINK_LITERAL, config.getMyaClaimingExpensesLink().getLinkUrl());
            personalisation.put(SUBMIT_EVIDENCE_LINK_LITERAL, config.getMyaEvidenceSubmissionInfoLink().getLinkUrl());
            personalisation.put(SUBMIT_EVIDENCE_INFO_LINK_LITERAL, config.getMyaEvidenceSubmissionInfoLink().getLinkUrl());
            personalisation.put(HEARING_INFO_LINK_LITERAL, config.getMyaHearingInfoLink().getLinkUrl());
        } else {
            personalisation.put(TRACK_APPEAL_LINK_LITERAL, config.getTrackAppealLink() != null ? config.getTrackAppealLink().replace(APPEAL_ID_LITERAL, tya) : null);
            personalisation.put(CLAIMING_EXPENSES_LINK_LITERAL, config.getClaimingExpensesLink().replace(APPEAL_ID_LITERAL, tya));
            personalisation.put(SUBMIT_EVIDENCE_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(APPEAL_ID_LITERAL, tya));
            personalisation.put(SUBMIT_EVIDENCE_INFO_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(APPEAL_ID_LITERAL, tya));
            personalisation.put(HEARING_INFO_LINK_LITERAL, config.getHearingInfoLink().replace(APPEAL_ID_LITERAL, tya));
        }

        String email = email(subscription);
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
        } else if (Benefit.ESA.equals(benefit)) {
            return ESA_PANEL_COMPOSITION;
        } else {
            return UC_PANEL_COMPOSITION;
        }
    }

    void setHearingContactDate(Map<String, String> personalisation, SscsCaseDataWrapper wrapper) {
        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(wrapper);
        hearingContactDate.ifPresent(zonedDateTime -> personalisation.put(HEARING_CONTACT_DATE,
            formatLocalDate(zonedDateTime.toLocalDate())
        ));
    }

    Map<String, String> setEventData(Map<String, String> personalisation, SscsCaseData ccdResponse, NotificationEventType notificationEventType) {
        if (ccdResponse.getCreatedInGapsFrom() != null && ccdResponse.getCreatedInGapsFrom().equals("readyToList")) {
            String dwpResponseDateString = formatLocalDate(LocalDate.parse(Optional.ofNullable(ccdResponse.getDateSentToDwp()).orElse(LocalDate.now().toString())).plusDays(MAX_DWP_RESPONSE_DAYS));
            personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);
            return personalisation;
        } else if (ccdResponse.getEvents() != null) {
            //FIXME: Remove this block once digital RTL journey is live

            for (Event event : ccdResponse.getEvents()) {
                if ((event.getValue() != null) && isAppealReceivedAndUpdated(notificationEventType, event)
                    || notificationEventType.equals(CASE_UPDATED) || JUDGE_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType)
                    || TCW_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType)) {
                    return setAppealReceivedDetails(personalisation, event.getValue());
                }
            }
        }
        return personalisation;
    }

    private boolean isAppealReceivedAndUpdated(NotificationEventType notificationEventType, Event event) {
        return notificationEventType.equals(APPEAL_RECEIVED_NOTIFICATION) && event.getValue().getEventType().equals(APPEAL_RECEIVED);
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
        if (EventType.READY_TO_LIST.getCcdType().equals(ccdResponse.getCreatedInGapsFrom())) {
            personalisation.put(REGIONAL_OFFICE_NAME_LITERAL, evidenceProperties.getAddress().getLine1());
            personalisation.put(SUPPORT_CENTRE_NAME_LITERAL, evidenceProperties.getAddress().getLine2());
            personalisation.put(ADDRESS_LINE_LITERAL, evidenceProperties.getAddress().getLine3());
            personalisation.put(TOWN_LITERAL, evidenceProperties.getAddress().getTown());
            personalisation.put(COUNTY_LITERAL, evidenceProperties.getAddress().getCounty());
            personalisation.put(POSTCODE_LITERAL, evidenceProperties.getAddress().getPostcode());
            personalisation.put(REGIONAL_OFFICE_POSTCODE_LITERAL, evidenceProperties.getAddress().getPostcode());
            personalisation.put(PHONE_NUMBER, evidenceProperties.getAddress().getTelephone());
        } else if (rpc != null) {
            personalisation.put(REGIONAL_OFFICE_NAME_LITERAL, rpc.getAddress1());
            personalisation.put(SUPPORT_CENTRE_NAME_LITERAL, rpc.getAddress2());
            personalisation.put(ADDRESS_LINE_LITERAL, rpc.getAddress3());
            personalisation.put(TOWN_LITERAL, rpc.getAddress4());
            personalisation.put(COUNTY_LITERAL, rpc.getCity());
            personalisation.put(POSTCODE_LITERAL, rpc.getPostcode());
            personalisation.put(REGIONAL_OFFICE_POSTCODE_LITERAL, rpc.getPostcode());
            personalisation.put(PHONE_NUMBER, rpc.getPhoneNumber());
        }

        setHearingArrangementDetails(personalisation, ccdResponse);

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
        String templateConfig = getEmailTemplateName(subscriptionType, notificationWrapper);
        String smsTemplateName = isSendSmsSubscriptionConfirmation() ? SUBSCRIPTION_CREATED_NOTIFICATION.getId() + "." + subscriptionType.toString().toLowerCase() :
            templateConfig;
        String letterTemplateName = getLetterTemplateName(subscriptionType, notificationWrapper.getNotificationType());
        String docmosisTemplateName = getDocmosisTemplateName(subscriptionType, notificationWrapper.getNotificationType());

        return config.getTemplate(templateConfig, smsTemplateName, letterTemplateName, docmosisTemplateName,
            benefit, notificationWrapper.getHearingType(), notificationWrapper.getNewSscsCaseData().getCreatedInGapsFrom());
    }

    private String getEmailTemplateName(SubscriptionType subscriptionType,
                                        NotificationWrapper notificationWrapper) {

        NotificationEventType notificationEventType = notificationWrapper.getNotificationType();
        String emailTemplateName = notificationEventType.getId();

        if (ADJOURNED_NOTIFICATION.equals(notificationEventType)
            || APPEAL_DORMANT_NOTIFICATION.equals(notificationEventType)
            || APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType)
            || HMCTS_APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType)
            || DWP_APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType)
            || APPEAL_RECEIVED_NOTIFICATION.equals(notificationEventType)
            || APPEAL_WITHDRAWN_NOTIFICATION.equals(notificationEventType)
            || ADMIN_APPEAL_WITHDRAWN.equals(notificationEventType)
            || CASE_UPDATED.equals(notificationEventType)
            || EVIDENCE_RECEIVED_NOTIFICATION.equals(notificationEventType)
            || EVIDENCE_REMINDER_NOTIFICATION.equals(notificationEventType)
            || HEARING_BOOKED_NOTIFICATION.equals(notificationEventType)
            || HEARING_REMINDER_NOTIFICATION.equals(notificationEventType)
            || POSTPONEMENT_NOTIFICATION.equals(notificationEventType)
            || (DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(notificationEventType)
            && !notificationWrapper.getHearingType().equals(AppealHearingType.ONLINE))
            || (DWP_UPLOAD_RESPONSE_NOTIFICATION.equals(notificationEventType)
            && !notificationWrapper.getHearingType().equals(AppealHearingType.ONLINE))
            || RESEND_APPEAL_CREATED_NOTIFICATION.equals(notificationEventType)
            || VALID_APPEAL_CREATED.equals(notificationEventType)
            || SYA_APPEAL_CREATED_NOTIFICATION.equals(notificationEventType)) {
            emailTemplateName = emailTemplateName + "." + StringUtils.lowerCase(subscriptionType.name());
        }
        return emailTemplateName;
    }

    private String getDocmosisTemplateName(SubscriptionType subscriptionType, NotificationEventType notificationEventType) {
        String letterTemplateName = notificationEventType.getId();
        if (subscriptionType != null
            && (APPEAL_RECEIVED_NOTIFICATION.equals(notificationEventType)
            || DIRECTION_ISSUED.equals(notificationEventType)
            || DECISION_ISSUED.equals(notificationEventType)
            || REQUEST_INFO_INCOMPLETE.equals(notificationEventType)
            || ISSUE_FINAL_DECISION.equals(notificationEventType))) {
            letterTemplateName = letterTemplateName + "." + subscriptionType.name().toLowerCase();
        }
        return letterTemplateName;
    }

    private String getLetterTemplateName(SubscriptionType subscriptionType, NotificationEventType
        notificationEventType) {
        String letterTemplateName = notificationEventType.getId();
        if (subscriptionType != null
            && ((LETTER_SUBSCRIPTION_TYPES.contains(notificationEventType)
            || APPEAL_WITHDRAWN_NOTIFICATION.equals(notificationEventType)
            || ADMIN_APPEAL_WITHDRAWN.equals(notificationEventType)
            || HEARING_BOOKED_NOTIFICATION.equals(notificationEventType))
            || JUDGE_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType)
            || TCW_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType)
            || APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType)
            || HMCTS_APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType)
            || DWP_APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType))) {
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

    private Map<String, String> setHearingArrangementDetails(Map<String, String> personalisation, SscsCaseData
        ccdResponse) {
        if (null != ccdResponse.getAppeal() && null != ccdResponse.getAppeal().getHearingOptions()) {
            personalisation.put(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL, buildHearingArrangements(ccdResponse.getAppeal().getHearingOptions()));

            return personalisation;
        }

        return personalisation;
    }

    private String buildHearingArrangements(HearingOptions hearingOptions) {
        if (null != hearingOptions) {
            String languageInterpreterRequired = convertBooleanToRequiredText(hearingOptions.getLanguageInterpreter() != null
                && StringUtils.equalsIgnoreCase(YES, hearingOptions.getLanguageInterpreter()));

            return "Language interpreter: " + languageInterpreterRequired + TWO_NEW_LINES + "Sign interpreter: "
                + convertBooleanToRequiredText(findHearingArrangement("signLanguageInterpreter", hearingOptions.getArrangements()))
                + TWO_NEW_LINES + "Hearing loop: " + convertBooleanToRequiredText(findHearingArrangement("hearingLoop", hearingOptions.getArrangements()))
                + TWO_NEW_LINES + "Disabled access: " + convertBooleanToRequiredText(findHearingArrangement("disabledAccess", hearingOptions.getArrangements()))
                + TWO_NEW_LINES + "Any other arrangements: " + getOptionalField(hearingOptions.getOther(), NOT_REQUIRED);
        }

        return null;
    }

    private Boolean findHearingArrangement(String field, List<String> arrangements) {
        return arrangements != null && arrangements.contains(field);
    }

    private String convertBooleanToRequiredText(Boolean bool) {
        return bool ? REQUIRED : NOT_REQUIRED;
    }
}
