package uk.gov.hmcts.reform.sscs.personalisation;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCodeOrThrowException;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getLongBenefitNameDescriptionWithOptionalAcronym;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelComposition.JUDGE_DOCTOR_AND_DISABILITY_EXPERT_IF_APPLICABLE;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.personalisation.SyaAppealCreatedAndReceivedPersonalisation.NOT_REQUIRED;
import static uk.gov.hmcts.reform.sscs.personalisation.SyaAppealCreatedAndReceivedPersonalisation.REQUIRED;
import static uk.gov.hmcts.reform.sscs.personalisation.SyaAppealCreatedAndReceivedPersonalisation.TWO_NEW_LINES;
import static uk.gov.hmcts.reform.sscs.personalisation.SyaAppealCreatedAndReceivedPersonalisation.getOptionalField;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasJointParty;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppellantInfoRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointPartyName;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
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
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.NotificationUtils;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.SendNotificationHelper;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Component
@Slf4j
public class Personalisation<E extends NotificationWrapper> {
    private static final List<NotificationEventType> LETTER_SUBSCRIPTION_TYPES = Arrays.asList(DWP_RESPONSE_RECEIVED_NOTIFICATION, DWP_UPLOAD_RESPONSE_NOTIFICATION,
            APPEAL_RECEIVED_NOTIFICATION, SYA_APPEAL_CREATED_NOTIFICATION, EVIDENCE_RECEIVED_NOTIFICATION, NON_COMPLIANT_NOTIFICATION, VALID_APPEAL_CREATED);

    private static final String CRLF = format("%c%c", (char) 0x0D, (char) 0x0A);

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
            return defaultIfBlank(subscription.getTya(), EMPTY);
        } else {
            return EMPTY;
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

    public static void translateToWelshDate(LocalDate localDate, SscsCaseData sscsCaseData, Consumer<? super String> placeholders) {
        if (sscsCaseData.isLanguagePreferenceWelsh()) {
            String translatedDate = LocalDateToWelshStringConverter.convert(localDate);
            placeholders.accept(translatedDate);
        }
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
                && !isEmpty(ccdResponse.getAppeal().getBenefitType().getCode())) {
                benefit = getBenefitByCodeOrThrowException(ccdResponse.getAppeal().getBenefitType().getCode());

                if (benefit.isHasAcronym()) {
                    personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL, benefit.name());
                    personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL_WELSH, benefit.name());
                } else {
                    personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL, benefit.getDescription());
                    personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL_WELSH, benefit.getWelshDescription());
                }

                personalisation.put(BENEFIT_NAME_ACRONYM_SHORT_LITERAL, benefit.name());
                personalisation.put(BENEFIT_FULL_NAME_LITERAL, benefit.getDescription());
                personalisation.put(BENEFIT_FULL_NAME_LITERAL_WELSH, benefit.getWelshDescription());
                personalisation.put(BENEFIT_NAME_AND_OPTIONAL_ACRONYM, getLongBenefitNameDescriptionWithOptionalAcronym(benefit.getShortName(), true));
                personalisation.put(BENEFIT_NAME_AND_OPTIONAL_ACRONYM_WELSH, getLongBenefitNameDescriptionWithOptionalAcronym(benefit.getShortName(), false));
            } else {
                log.warn("Proceeding with 'null' benefit type for case !");
            }
        } catch (BenefitMappingException bme) {
            log.warn("Proceeding with 'null' benefit type for case !");
        }

        translateToWelshDate(LocalDate.now(), ccdResponse, value -> personalisation.put(WELSH_CURRENT_DATE, value));

        PanelComposition panelComposition = ofNullable(benefit).map(Benefit::getPanelComposition).orElse(JUDGE_DOCTOR_AND_DISABILITY_EXPERT_IF_APPLICABLE);
        personalisation.put(PANEL_COMPOSITION, panelComposition.getEnglish());
        personalisation.put(PANEL_COMPOSITION_WELSH, panelComposition.getWelsh());

        LocalDate decisionPostedReceivedDate = LocalDate.now().plusDays(7);
        personalisation.put(DECISION_POSTED_RECEIVE_DATE, formatLocalDate(decisionPostedReceivedDate));
        translateToWelshDate(decisionPostedReceivedDate, ccdResponse, value -> personalisation.put(WELSH_DECISION_POSTED_RECEIVE_DATE, value));

        personalisation.put(APPEAL_REF, getAppealReference(ccdResponse));
        personalisation.put(APPELLANT_NAME, ccdResponse.getAppeal().getAppellant().getName().getFullNameNoTitle());
        personalisation.put(NAME, getName(subscriptionWithType.getSubscriptionType(), ccdResponse, responseWrapper));
        personalisation.put(CCD_ID, defaultIfBlank(ccdResponse.getCcdCaseId(), EMPTY));

        // Some templates (notably letters) can be sent out before the SC Ref is added to the case
        // this allows those templates to be populated with either the CCD Id or SC Ref
        personalisation.put(CASE_REFERENCE_ID, getAppealReference(ccdResponse));

        personalisation.put(INFO_REQUEST_DETAIL, defaultIfBlank(getLatestInfoRequestDetail(ccdResponse), EMPTY));

        Subscription subscription = subscriptionWithType.getSubscription();
        subscriptionDetails(personalisation, subscription, benefit, ccdResponse);

        personalisation.put(FIRST_TIER_AGENCY_ACRONYM, DWP_ACRONYM);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME, DWP_FULL_NAME);
        personalisation.put(WELSH_FIRST_TIER_AGENCY_FULL_NAME, WELSH_DWP_FULL_NAME);

        LocalDate createdDate = LocalDate.parse(ofNullable(ccdResponse.getCaseCreated()).orElse(LocalDate.now().toString()));
        translateToWelshDate(createdDate, ccdResponse, value -> personalisation.put(CREATED_DATE_WELSH, value));
        personalisation.put(CREATED_DATE, createdDate.toString());

        personalisation.put(JOINT, subscriptionWithType.getSubscriptionType().equals(JOINT_PARTY) ? JOINT_TEXT_WITH_A_SPACE : EMPTY);
        personalisation.put(JOINT_WELSH, subscriptionWithType.getSubscriptionType().equals(JOINT_PARTY) ? JOINT_WELSH_TEXT_WITH_A_SPACE : EMPTY);

        if (StringUtils.equalsIgnoreCase(ccdResponse.getJointParty(), "yes")) {
            personalisation.put(JOINT_PARTY_APPEAL, "Yes");
            personalisation.put(JOINT_PARTY_NAME, ccdResponse.getJointPartyName().getFullNameNoTitle());
        } else {
            personalisation.put(JOINT_PARTY_APPEAL, "No");
        }


        if (ccdResponse.getHearings() != null && !ccdResponse.getHearings().isEmpty()) {

            Hearing latestHearing = NotificationUtils.getLatestHearing(ccdResponse);
            if (latestHearing != null) {
                HearingDetails latestHearingValue = latestHearing.getValue();
                LocalDateTime hearingDateTime = latestHearingValue.getHearingDateTime();
                personalisation.put(HEARING_DATE, formatLocalDate(hearingDateTime.toLocalDate()));
                translateToWelshDate(hearingDateTime.toLocalDate(), ccdResponse, value -> personalisation.put(WELSH_HEARING_DATE, value));
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
        if (subscriptionWithType.getSubscriptionType().equals(JOINT_PARTY)) {
            personalisation.put(AppConstants.JOINT_PARTY, "Yes");
        }

        setConfidentialFields(ccdResponse, subscriptionWithType, personalisation);

        setHelplineTelephone(ccdResponse, personalisation);

        return personalisation;
    }

    private void setHelplineTelephone(SscsCaseData ccdResponse, Map<String, String> personalisation) {
        if ("yes".equalsIgnoreCase(ccdResponse.getIsScottishCase())) {
            personalisation.put(HELPLINE_PHONE_NUMBER, config.getHelplineTelephoneScotland());
        } else {
            personalisation.put(HELPLINE_PHONE_NUMBER, config.getHelplineTelephone());
        }
    }

    private void setConfidentialFields(SscsCaseData ccdResponse, SubscriptionWithType subscriptionWithType, Map<String, String> personalisation) {
        if (subscriptionWithType.getSubscriptionType().equals(JOINT_PARTY) && null != ccdResponse.getConfidentialityRequestOutcomeJointParty()) {
            personalisation.put(OTHER_PARTY_NAME, ccdResponse.getAppeal().getAppellant().getName().getFullNameNoTitle());
            personalisation.put(CONFIDENTIALITY_OUTCOME, getRequestOutcome(ccdResponse.getConfidentialityRequestOutcomeJointParty()));

        } else if (subscriptionWithType.getSubscriptionType().equals(APPELLANT) && null != ccdResponse.getJointPartyName() && null != ccdResponse.getConfidentialityRequestOutcomeAppellant()) {
            personalisation.put(OTHER_PARTY_NAME, ccdResponse.getJointPartyName().getFullNameNoTitle());
            personalisation.put(CONFIDENTIALITY_OUTCOME, getRequestOutcome(ccdResponse.getConfidentialityRequestOutcomeAppellant()));
        }
    }

    private String getRequestOutcome(DatedRequestOutcome datedRequestOutcome) {
        return datedRequestOutcome == null || datedRequestOutcome.getRequestOutcome() == null ? null : datedRequestOutcome.getRequestOutcome().getValue();
    }

    private String getAppealReference(SscsCaseData ccdResponse) {
        final String caseReference = ccdResponse.getCaseReference();
        return isBlank(caseReference) || (ccdResponse.getCreatedInGapsFrom() != null && ccdResponse.getCreatedInGapsFrom().equals("readyToList"))
                ? ccdResponse.getCcdCaseId() : caseReference;
    }

    private String getName(SubscriptionType subscriptionType, SscsCaseData ccdResponse, SscsCaseDataWrapper wrapper) {
        if (ccdResponse.getAppeal() == null) {
            return EMPTY;
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
        } else if (subscriptionType.equals(JOINT_PARTY) && hasJointParty(ccdResponse)) {
            JointPartyName partyName = ccdResponse.getJointPartyName();
            return (partyName == null) ? EMPTY :
               getDefaultName(new Name(partyName.getTitle(), partyName.getFirstName(), partyName.getLastName()));
        }
        return EMPTY;
    }

    private String getDefaultName(Name name) {
        return name == null || name.getFirstName() == null || isBlank(name.getFirstName())
            || name.getLastName() == null || isBlank(name.getLastName()) ? EMPTY : name.getFullNameNoTitle();
    }

    private String getAppointeeDescription(SubscriptionType subscriptionType, SscsCaseData ccdResponse) {
        if (APPOINTEE.equals(subscriptionType) && ccdResponse.getAppeal() != null
            && ccdResponse.getAppeal().getAppellant().getName() != null) {
            return format("You are receiving this update as the appointee for %s.%s%s",
                ccdResponse.getAppeal().getAppellant().getName().getFullNameNoTitle(), CRLF, CRLF);
        } else {
            return EMPTY;
        }
    }

    private void subscriptionDetails(Map<String, String> personalisation, Subscription subscription, Benefit benefit, SscsCaseData sscsCaseData) {
        final String tya = tya(subscription);
        personalisation.put(APPEAL_ID_LITERAL, tya);
        if (benefit != null) {
            personalisation.put(MANAGE_EMAILS_LINK_LITERAL, config.getManageEmailsLink().replace(MAC_LITERAL, getMacToken(tya, benefit.name())));
        }
        if (equalsIgnoreCase(State.READY_TO_LIST.getId(), sscsCaseData.getCreatedInGapsFrom())) {
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

    void setHearingContactDate(Map<String, String> personalisation, SscsCaseDataWrapper wrapper) {
        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(wrapper);
        hearingContactDate.ifPresent(zonedDateTime -> personalisation.put(HEARING_CONTACT_DATE,
                formatLocalDate(zonedDateTime.toLocalDate())
        ));
    }

    Map<String, String> setEventData(Map<String, String> personalisation, SscsCaseData ccdResponse, NotificationEventType notificationEventType) {
        if (ccdResponse.getCreatedInGapsFrom() != null && ccdResponse.getCreatedInGapsFrom().equals("readyToList")) {
            LocalDate localDate = LocalDate.parse(ofNullable(ccdResponse.getDateSentToDwp()).orElse(LocalDate.now().toString())).plusDays(MAX_DWP_RESPONSE_DAYS);
            String dwpResponseDateString = formatLocalDate(localDate);
            personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);
            translateToWelshDate(localDate, ccdResponse, value ->
                    personalisation.put(WELSH_APPEAL_RESPOND_DATE, value)
            );

            return personalisation;
        } else if (ccdResponse.getEvents() != null) {
            //FIXME: Remove this block once digital RTL journey is live

            for (Event event : ccdResponse.getEvents()) {
                if ((event.getValue() != null) && isAppealReceivedAndUpdated(notificationEventType, event)
                        || notificationEventType.equals(CASE_UPDATED) || JUDGE_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType)
                        || TCW_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType)) {
                    return setAppealReceivedDetails(personalisation, event.getValue(), ccdResponse);
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
                LocalDate evidenceDateTimeFormatted = ccdResponse.getEvidence().getDocuments().get(0).getValue()
                        .getEvidenceDateTimeFormatted();
                personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL,
                        formatLocalDate(evidenceDateTimeFormatted));
                translateToWelshDate(evidenceDateTimeFormatted, ccdResponse, value ->
                        personalisation.put(WELSH_EVIDENCE_RECEIVED_DATE_LITERAL, value)
                );
            } else {
                personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL, EMPTY);
                personalisation.put(WELSH_EVIDENCE_RECEIVED_DATE_LITERAL, EMPTY);
            }
        }
        return personalisation;

    }

    private Map<String, String> setAppealReceivedDetails(Map<String, String> personalisation, EventDetails eventDetails, SscsCaseData ccdResponse) {
        LocalDate localDate = eventDetails.getDateTime().plusDays(MAX_DWP_RESPONSE_DAYS).toLocalDate();
        String dwpResponseDateString = formatLocalDate(localDate);
        personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);
        translateToWelshDate(localDate, ccdResponse, value ->
                personalisation.put(WELSH_APPEAL_RESPOND_DATE, value)
        );
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
            personalisation.put(ADDRESS_LINE_LITERAL, evidenceProperties.getAddress().getLine3(ccdResponse));
            personalisation.put(TOWN_LITERAL, evidenceProperties.getAddress().getTown());
            personalisation.put(COUNTY_LITERAL, evidenceProperties.getAddress().getCounty());
            personalisation.put(POSTCODE_LITERAL, evidenceProperties.getAddress().getPostcode(ccdResponse));
            personalisation.put(REGIONAL_OFFICE_POSTCODE_LITERAL, evidenceProperties.getAddress().getPostcode());
        } else if (rpc != null) {
            personalisation.put(REGIONAL_OFFICE_NAME_LITERAL, rpc.getAddress1());
            personalisation.put(SUPPORT_CENTRE_NAME_LITERAL, rpc.getAddress2());
            personalisation.put(ADDRESS_LINE_LITERAL, rpc.getAddress3());
            personalisation.put(TOWN_LITERAL, rpc.getAddress4());
            personalisation.put(COUNTY_LITERAL, rpc.getCity());
            personalisation.put(POSTCODE_LITERAL, rpc.getPostcode());
            personalisation.put(REGIONAL_OFFICE_POSTCODE_LITERAL, rpc.getPostcode());
        }

        personalisation.put(PHONE_NUMBER_WELSH, evidenceProperties.getAddress().getTelephoneWelsh());
        personalisation.put(PHONE_NUMBER, determinePhoneNumber(rpc));

        setHearingArrangementDetails(personalisation, ccdResponse);

        return personalisation;
    }

    private String determinePhoneNumber(RegionalProcessingCenter rpc) {

        if (rpc != null) {
            return rpc.getPhoneNumber();
        } else {
            return evidenceProperties.getAddress().getTelephone();
        }
    }

    private String formatAddress(Hearing hearing) {
        return newArrayList(hearing.getValue().getVenue().getName(),
                hearing.getValue().getVenue().getAddress().getFullAddress())
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    private String calculateDaysToHearingText(LocalDate hearingDate) {
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), hearingDate);

        return daysBetween == 1 ? TOMORROW_STRING : "in " + daysBetween + DAYS_STRING;
    }

    private String getMacToken(String id, String benefitType) {
        return macService.generateToken(id, benefitType);
    }

    private String formatLocalDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT));
    }

    private String formatLocalTime(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern(HEARING_TIME_FORMAT)).toUpperCase();
    }

    public Template getTemplate(E notificationWrapper, Benefit benefit, SubscriptionType subscriptionType) {

        String templateConfig = getEmailTemplateName(subscriptionType, notificationWrapper);
        String smsTemplateName = isSendSmsSubscriptionConfirmation() ? SUBSCRIPTION_CREATED_NOTIFICATION.getId() + "." + subscriptionType.toString().toLowerCase() :
                templateConfig;

        String letterTemplateName = getLetterTemplateName(subscriptionType, notificationWrapper.getNotificationType());

        String docmosisTemplateName = getDocmosisTemplateName(subscriptionType, notificationWrapper.getNotificationType(), notificationWrapper.getNewSscsCaseData());

        return config.getTemplate(templateConfig, smsTemplateName, letterTemplateName, docmosisTemplateName,
                benefit, notificationWrapper, notificationWrapper.getNewSscsCaseData().getCreatedInGapsFrom());
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
                || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(notificationEventType)
                || DWP_UPLOAD_RESPONSE_NOTIFICATION.equals(notificationEventType)
                || RESEND_APPEAL_CREATED_NOTIFICATION.equals(notificationEventType)
                || VALID_APPEAL_CREATED.equals(notificationEventType)
                || SYA_APPEAL_CREATED_NOTIFICATION.equals(notificationEventType)) {

            emailTemplateName = emailTemplateName + "." + lowerCase(subscriptionType.name());
        }


        return emailTemplateName;
    }

    private String getDocmosisTemplateName(SubscriptionType subscriptionType, NotificationEventType notificationEventType, SscsCaseData caseData) {

        String letterTemplateName = notificationEventType.getId();

        if (subscriptionType != null
                && (DIRECTION_ISSUED.equals(notificationEventType) || DIRECTION_ISSUED_WELSH.equals(notificationEventType))
                && caseData.getDirectionTypeDl() != null
                && caseData.getDirectionTypeDl().getValue() != null) {

            letterTemplateName = letterTemplateName + "." + caseData.getDirectionTypeDl().getValue().getCode() + "." + subscriptionType.name().toLowerCase();

        } else if (subscriptionType != null
                && (APPEAL_RECEIVED_NOTIFICATION.equals(notificationEventType)
                || DIRECTION_ISSUED.equals(notificationEventType)
                || APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType)
                || DWP_APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType)
                || HMCTS_APPEAL_LAPSED_NOTIFICATION.equals(notificationEventType)
                || DECISION_ISSUED.equals(notificationEventType)
                || DIRECTION_ISSUED_WELSH.equals(notificationEventType)
                || DECISION_ISSUED_WELSH.equals(notificationEventType)
                || REQUEST_INFO_INCOMPLETE.equals(notificationEventType)
                || ISSUE_FINAL_DECISION.equals(notificationEventType)
                || ISSUE_FINAL_DECISION_WELSH.equals(notificationEventType)
                || ISSUE_ADJOURNMENT_NOTICE.equals(notificationEventType)
                || ISSUE_ADJOURNMENT_NOTICE_WELSH.equals(notificationEventType)
                || JOINT_PARTY_ADDED.equals(notificationEventType)
                || ADMIN_APPEAL_WITHDRAWN.equals(notificationEventType)
                || APPEAL_WITHDRAWN_NOTIFICATION.equals(notificationEventType)
                || REVIEW_CONFIDENTIALITY_REQUEST.equals(notificationEventType)
                || ACTION_HEARING_RECORDING_REQUEST.equals(notificationEventType)
                || VALID_APPEAL_CREATED.equals(notificationEventType)
                || ACTION_POSTPONEMENT_REQUEST.equals(notificationEventType)
                || ACTION_POSTPONEMENT_REQUEST_WELSH.equals(notificationEventType))) {
            letterTemplateName = letterTemplateName + "." + subscriptionType.name().toLowerCase();

        }

        return letterTemplateName;
    }

    private String getLetterTemplateName(SubscriptionType subscriptionType, NotificationEventType
            notificationEventType) {

        String letterTemplateName = notificationEventType.getId();

        if (null != subscriptionType
                && ((LETTER_SUBSCRIPTION_TYPES.contains(notificationEventType)
                || HEARING_BOOKED_NOTIFICATION.equals(notificationEventType))
                || JUDGE_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType)
                || TCW_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType))) {
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
                && equalsIgnoreCase(YES, hearingOptions.getLanguageInterpreter()));

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
