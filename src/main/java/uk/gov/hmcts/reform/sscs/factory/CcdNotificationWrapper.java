package uk.gov.hmcts.reform.sscs.factory;

import static java.util.Objects.isNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_MAYBE_INVALID_FOR_APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_VALID_FOR_ALL_ENTITIES;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_VALID_FOR_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_VALID_FOR_JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_VALID_FOR_OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENTS_VALID_FOR_REP;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ACTION_HEARING_RECORDING_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REQUEST_INFO_INCOMPLETE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REVIEW_CONFIDENTIALITY_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasJointPartySubscription;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasRepSubscriptionOrIsMandatoryRepLetter;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasRepresentative;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isValidSubscriptionOrIsMandatoryLetter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityPartyMembers;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityType;
import uk.gov.hmcts.reform.sscs.service.scheduler.CcdActionSerializer;

@Slf4j
public class CcdNotificationWrapper implements NotificationWrapper {

    private final SscsCaseDataWrapper responseWrapper;

    private boolean notificationEventTypeOverridden = false;

    private boolean languageSwitched = false;

    public CcdNotificationWrapper(SscsCaseDataWrapper responseWrapper) {
        this.responseWrapper = responseWrapper;
    }

    @Override
    public NotificationEventType getNotificationType() {
        return responseWrapper.getNotificationEventType();
    }

    @Override
    public void setNotificationType(NotificationEventType notificationEventType) {
        responseWrapper.setNotificationEventType(notificationEventType);
    }

    @Override
    public SscsCaseData getNewSscsCaseData() {
        return responseWrapper.getNewSscsCaseData();
    }

    @Override
    public Subscription getAppellantSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();
    }

    @Override
    public Subscription getRepresentativeSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
    }

    @Override
    public Subscription getAppointeeSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
    }

    @Override
    public Subscription getJointPartySubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getJointPartySubscription();
    }

    @Override
    public List<SubscriptionWithType> getOtherPartySubscriptions(SscsCaseData newSscsCaseData, NotificationEventType notificationEventType) {
        return emptyIfNull(newSscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .flatMap(o -> filterOtherPartySubscription(newSscsCaseData, notificationEventType, o).stream())
                .collect(Collectors.toList());
    }

    @Override
    public SscsCaseDataWrapper getSscsCaseDataWrapper() {
        return responseWrapper;
    }

    @Override
    public String getCaseId() {
        return responseWrapper.getNewSscsCaseData().getCcdCaseId();
    }

    public AppealHearingType getHearingType() {
        final String hearingType = responseWrapper.getNewSscsCaseData().getAppeal().getHearingType();
        AppealHearingType returnHearingType = ORAL;
        if (StringUtils.equalsAnyIgnoreCase(PAPER.name(), hearingType)) {
            returnHearingType = PAPER;
        }
        return returnHearingType;
    }

    @Override
    public String getSchedulerPayload() {
        return new CcdActionSerializer().serialize(getCaseId());
    }

    @Override
    public SscsCaseData getOldSscsCaseData() {
        return responseWrapper.getOldSscsCaseData();
    }

    @Override
    public List<SubscriptionWithType> getSubscriptionsBasedOnNotificationType() {
        List<SubscriptionWithType> subscriptionWithTypeList = new ArrayList<>();

        SscsCaseData newSscsCaseData = getNewSscsCaseData();
        SscsCaseData oldSscsCaseData = getOldSscsCaseData();
        Appeal appeal = newSscsCaseData.getAppeal();
        Appellant appellant = appeal.getAppellant();
        JointParty jointParty = newSscsCaseData.getJointParty();
        NotificationEventType notificationEventType = getNotificationType();

        if (isNotificationEventValidToSendToAppointee(newSscsCaseData, oldSscsCaseData, notificationEventType)) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppointeeSubscription(), APPOINTEE,
                appellant, appellant.getAppointee()));
        } else if (isNotificationEventValidToSendToAppellant(newSscsCaseData, oldSscsCaseData, notificationEventType)) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppellantSubscription(), APPELLANT,
                appellant, appellant));
        }

        if (isNotificationEventValidToSendToRep(newSscsCaseData, oldSscsCaseData, notificationEventType)) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getRepresentativeSubscription(), REPRESENTATIVE,
                appellant, appeal.getRep()));
        }

        if (isNotificationEventValidToSendToJointParty(newSscsCaseData, oldSscsCaseData, notificationEventType)) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getJointPartySubscription(), JOINT_PARTY,
                jointParty, jointParty));
        }

        subscriptionWithTypeList.addAll(getOtherPartySubscriptions(newSscsCaseData, notificationEventType));

        return subscriptionWithTypeList;
    }

    private List<SubscriptionWithType> filterOtherPartySubscription(SscsCaseData newSscsCaseData, NotificationEventType notificationEventType, OtherParty otherParty) {
        List<SubscriptionWithType> otherPartySubscription = new ArrayList<>();

        log.info("isSendNewOtherPartyNotification {}", otherParty.getSendNewOtherPartyNotification());
        log.info("Notification Type {}", notificationEventType);
        log.info("Other Party id {} isSendNewOtherPartyNotification {}", otherParty.getId(), otherParty.getSendNewOtherPartyNotification());

        boolean isSendNewOtherPartyNotification = YesNo.isYes(otherParty.getSendNewOtherPartyNotification());

        if (hasAppointee(otherParty.getAppointee(), otherParty.getIsAppointee())
                && isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartyAppointeeSubscription(), isSendNewOtherPartyNotification, newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode())) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyAppointeeSubscription(),
                OTHER_PARTY, otherParty, otherParty.getAppointee(), otherParty.getAppointee().getId()));
        } else if (isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartySubscription(), isSendNewOtherPartyNotification, newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.OTHER_PARTY.getCode())) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartySubscription(), OTHER_PARTY,
                otherParty, otherParty, otherParty.getId()));
        }

        if (hasRepresentative(otherParty)
                && isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartyRepresentativeSubscription(), isSendNewOtherPartyNotification, newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode())) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyRepresentativeSubscription(),
                OTHER_PARTY, otherParty, otherParty.getRep(), otherParty.getRep().getId()));
        }

        log.info("Number of subscription {}", otherPartySubscription.size());

        return otherPartySubscription;
    }

    private List<String> getEligiblePartyMembersInTheCaseToSendNotification(SscsCaseData caseData) {
        List<String> eligiblePartyMembers =  new ArrayList<>();
        // the party members must exist in the case and the user has selected to send the notification via the radio button in issue direction notice.
        if (YesNo.isYes(caseData.getSendDirectionNoticeToAppellantOrAppointee())) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToJointParty()) && YesNo.isYes(caseData.getHasJointParty())) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.JOINT_PARTY.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToOtherParty()) && YesNo.isYes(caseData.getHasOtherParties())) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.OTHER_PARTY.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToOtherPartyAppointee()) && YesNo.isYes(caseData.getHasOtherPartyAppointee())) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToOtherPartyRep()) && YesNo.isYes(caseData.getHasOtherPartyRep())) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToRepresentative()) && YesNo.isYes(caseData.getHasRepresentative())) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.REPRESENTATIVE.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToFTA())) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.FTA.getCode());
        }

        return eligiblePartyMembers;
    }

    private boolean canSendBasedOnConfidentiality(SscsCaseData newSscsCaseData, NotificationEventType notificationEventType, String partyMember) {
        if (!(DIRECTION_ISSUED.equals(notificationEventType)
            || DIRECTION_ISSUED_WELSH.equals(notificationEventType))) {
            return true;
        }

        String confidentialityType = newSscsCaseData.getConfidentialityType();
        if (isNull(confidentialityType)
            || ConfidentialityType.GENERAL.getCode().equalsIgnoreCase(confidentialityType)) {
            return true;
        }

        List<String> eligiblePartyMembers =  getEligiblePartyMembersInTheCaseToSendNotification(newSscsCaseData);
        log.info("For caseID: {}, canSendNotificationBasedOnConfidentiality, notificationEventType: {}, partyMember: {}, eligiblePartyMembers: {}", newSscsCaseData.getCcdCaseId(), notificationEventType.getId(), partyMember,  String.join(", ", eligiblePartyMembers));
        return eligiblePartyMembers.contains(partyMember);
    }

    private boolean isNotificationEventValidToSendToAppointee(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType) {
        boolean isValid = hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter(responseWrapper)
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(notificationEventType)
            || EVENTS_VALID_FOR_APPOINTEE.contains(notificationEventType)
            || isValidProcessHearingRequestEventForParty(newSscsCaseData, oldSscsCaseData, notificationEventType, PartyItemList.APPELLANT)
            || isValidRequestInfoIncompleteEventForParty(newSscsCaseData, notificationEventType, PartyItemList.APPELLANT));
        return isValid && canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode());
    }

    private boolean isNotificationEventValidToSendToAppellant(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType) {
        boolean isValid =  (oldSscsCaseData != null && isValidReviewConfidentialityRequest(notificationEventType, oldSscsCaseData.getConfidentialityRequestOutcomeAppellant(), newSscsCaseData.getConfidentialityRequestOutcomeAppellant()))
            || isValidProcessHearingRequestEventForParty(newSscsCaseData, oldSscsCaseData, notificationEventType, PartyItemList.APPELLANT)
            || isValidRequestInfoIncompleteEventForParty(newSscsCaseData, notificationEventType, PartyItemList.APPELLANT)
            || !EVENTS_MAYBE_INVALID_FOR_APPELLANT.contains(notificationEventType);
        return isValid && canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode());
    }

    private boolean isValidProcessHearingRequestEventForParty(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType, PartyItemList partyItemList) {
        return ACTION_HEARING_RECORDING_REQUEST.equals(notificationEventType) && hasHearingRecordingRequestsForParty(newSscsCaseData, oldSscsCaseData, partyItemList);
    }

    private boolean hasHearingRecordingRequestsForParty(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, PartyItemList partyItemList) {
        List<HearingRecordingRequest> oldReleasedRecordings = new ArrayList<>();
        if (oldSscsCaseData != null && oldSscsCaseData.getSscsHearingRecordingCaseData() != null) {
            oldReleasedRecordings = Optional.ofNullable(oldSscsCaseData.getSscsHearingRecordingCaseData().getCitizenReleasedHearings())
                    .orElse(new ArrayList<>());
        }
        return hasNewReleasedHearingRecordingForParty(newSscsCaseData, oldReleasedRecordings).stream()
                .anyMatch(v -> partyItemList.getCode().equals(v.getValue().getRequestingParty()));
    }

    @NotNull
    private List<HearingRecordingRequest> hasNewReleasedHearingRecordingForParty(SscsCaseData newSscsCaseData, List<HearingRecordingRequest> oldReleasedRecordings) {
        return newSscsCaseData.getSscsHearingRecordingCaseData().getCitizenReleasedHearings().stream()
                .filter(e -> !oldReleasedRecordings.contains(e))
                .collect(Collectors.toList());
    }

    private boolean isNotificationEventValidToSendToRep(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType) {
        boolean isValid = hasRepSubscriptionOrIsMandatoryRepLetter(responseWrapper)
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(notificationEventType)
            || EVENTS_VALID_FOR_REP.contains(notificationEventType)
            || isValidProcessHearingRequestEventForParty(newSscsCaseData, oldSscsCaseData, notificationEventType, PartyItemList.REPRESENTATIVE)
            || isValidRequestInfoIncompleteEventForParty(newSscsCaseData, notificationEventType, PartyItemList.REPRESENTATIVE));
        return isValid && canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.REPRESENTATIVE.getCode());
    }

    private boolean isNotificationEventValidToSendToJointParty(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType) {
        boolean isValid = hasJointPartySubscription(responseWrapper)
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(notificationEventType)
            || EVENTS_VALID_FOR_JOINT_PARTY.contains(notificationEventType)
            || isValidRequestInfoIncompleteEventForParty(newSscsCaseData, notificationEventType, PartyItemList.JOINT_PARTY)
            || isValidProcessHearingRequestEventForParty(newSscsCaseData, oldSscsCaseData, notificationEventType, PartyItemList.JOINT_PARTY)
            || (oldSscsCaseData != null && isValidReviewConfidentialityRequest(notificationEventType, oldSscsCaseData.getConfidentialityRequestOutcomeJointParty(), newSscsCaseData.getConfidentialityRequestOutcomeJointParty())));
        return isValid && canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.JOINT_PARTY.getCode());
    }

    private boolean isNotificationEventValidToSendToOtherPartySubscription(Subscription subscription, boolean isSendNewOtherPartyNotification, SscsCaseData newSscsCaseData, NotificationEventType notificationEventType, String partyMember) {
        boolean isValid = isValidSubscriptionOrIsMandatoryLetter(subscription, responseWrapper.getNotificationEventType())
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(notificationEventType)
            || EVENTS_VALID_FOR_OTHER_PARTY.contains(notificationEventType)
            || (UPDATE_OTHER_PARTY_DATA.equals(notificationEventType) && isSendNewOtherPartyNotification));
        return isValid && canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, partyMember);
    }


    private boolean isValidRequestInfoIncompleteEventForParty(SscsCaseData newSscsCaseData, NotificationEventType notificationEventType, PartyItemList partyItem) {
        return REQUEST_INFO_INCOMPLETE.equals(notificationEventType)
                && newSscsCaseData.getInformationFromPartySelected() != null
                && newSscsCaseData.getInformationFromPartySelected().getValue() != null
                && partyItem.getCode().equals(newSscsCaseData.getInformationFromPartySelected().getValue().getCode());
    }

    private boolean isValidReviewConfidentialityRequest(NotificationEventType notificationEventType, DatedRequestOutcome previousRequestOutcome, DatedRequestOutcome latestRequestOutcome) {
        return REVIEW_CONFIDENTIALITY_REQUEST.equals(notificationEventType)
                && checkConfidentialityRequestOutcomeIsValidToSend(previousRequestOutcome, latestRequestOutcome);
    }

    private boolean checkConfidentialityRequestOutcomeIsValidToSend(DatedRequestOutcome previousRequestOutcome, DatedRequestOutcome latestRequestOutcome) {
        return latestRequestOutcome == null ? false : checkConfidentialityRequestOutcomeIsValidToSend(previousRequestOutcome, latestRequestOutcome.getRequestOutcome());
    }

    private boolean checkConfidentialityRequestOutcomeIsValidToSend(DatedRequestOutcome previousRequestOutcome, RequestOutcome latestRequestOutcome) {
        return (RequestOutcome.GRANTED.equals(latestRequestOutcome) && !isMatchingOutcome(previousRequestOutcome, RequestOutcome.GRANTED))
                || (RequestOutcome.REFUSED.equals(latestRequestOutcome) && !isMatchingOutcome(previousRequestOutcome, RequestOutcome.REFUSED));
    }

    private boolean isMatchingOutcome(DatedRequestOutcome datedRequestOutcome, RequestOutcome requestOutcome) {
        return datedRequestOutcome != null && requestOutcome != null && requestOutcome.equals(datedRequestOutcome.getRequestOutcome());
    }

    @Override
    public void setNotificationEventTypeOverridden(boolean notificationEventTypeOverridden) {
        this.notificationEventTypeOverridden = notificationEventTypeOverridden;
    }

    @Override
    public boolean hasNotificationEventBeenOverridden() {
        return notificationEventTypeOverridden;
    }

    @Override
    public void setSwitchLanguageType(boolean languageSwitched) {
        this.languageSwitched = languageSwitched;
    }

    @Override
    public boolean hasLanguageSwitched() {
        return languageSwitched;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CcdNotificationWrapper that = (CcdNotificationWrapper) o;
        return Objects.equals(responseWrapper, that.responseWrapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseWrapper);
    }
}

