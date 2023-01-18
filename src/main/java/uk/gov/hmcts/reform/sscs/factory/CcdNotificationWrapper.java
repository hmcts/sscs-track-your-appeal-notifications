package uk.gov.hmcts.reform.sscs.factory;

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
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_GENERIC_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REQUEST_INFO_INCOMPLETE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REVIEW_CONFIDENTIALITY_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasJointParty;
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
    public List<SubscriptionWithType> getOtherPartySubscriptions() {
        return emptyIfNull(responseWrapper.getNewSscsCaseData().getOtherParties()).stream()
                .map(CcdValue::getValue)
                .flatMap(o -> filterOtherPartySubscription(o).stream())
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

        Appeal appeal = responseWrapper.getNewSscsCaseData().getAppeal();
        Appellant appellant = appeal.getAppellant();
        JointParty jointParty = responseWrapper.getNewSscsCaseData().getJointParty();

        if (isNotificationEventValidToSendToAppointee()) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppointeeSubscription(), APPOINTEE,
                appellant, appellant.getAppointee()));
        } else if (isNotificationEventValidToSendToAppellant()) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppellantSubscription(), APPELLANT,
                appellant, appellant));
        }

        if (isNotificationEventValidToSendToRep()) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getRepresentativeSubscription(), REPRESENTATIVE,
                appellant, appeal.getRep()));
        }

        if (isNotificationEventValidToSendToJointParty()) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getJointPartySubscription(), JOINT_PARTY,
                jointParty, jointParty));
        }

        subscriptionWithTypeList.addAll(getOtherPartySubscriptions());

        return subscriptionWithTypeList;
    }

    private List<SubscriptionWithType> filterOtherPartySubscription(OtherParty otherParty) {
        List<SubscriptionWithType> otherPartySubscription = new ArrayList<>();

        log.info("isSendNewOtherPartyNotification {}", otherParty.getSendNewOtherPartyNotification());
        log.info("Notification Type {}", getNotificationType());
        log.info("Other Party id {} isSendNewOtherPartyNotification {}", otherParty.getId(), otherParty.getSendNewOtherPartyNotification());

        boolean isSendNewOtherPartyNotification = YesNo.isYes(otherParty.getSendNewOtherPartyNotification());

        if (hasAppointee(otherParty.getAppointee(), otherParty.getIsAppointee())
                && isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartyAppointeeSubscription(), isSendNewOtherPartyNotification)) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyAppointeeSubscription(),
                OTHER_PARTY, otherParty, otherParty.getAppointee(), otherParty.getAppointee().getId()));
        } else if (isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartySubscription(), isSendNewOtherPartyNotification)) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartySubscription(), OTHER_PARTY,
                otherParty, otherParty, otherParty.getId()));
        }

        if (hasRepresentative(otherParty)
                && isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartyRepresentativeSubscription(), isSendNewOtherPartyNotification)) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyRepresentativeSubscription(),
                OTHER_PARTY, otherParty, otherParty.getRep(), otherParty.getRep().getId()));
        }

        if (ISSUE_GENERIC_LETTER.equals(getNotificationType()) && (hasOtherPartySelected(otherParty)
                || YesNo.YES.equals(getNewSscsCaseData().getSendToAllParties()))) {
            if (hasAppointee(otherParty.getAppointee(), otherParty.getIsAppointee())) {
                otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyAppointeeSubscription(),
                        OTHER_PARTY, otherParty, otherParty.getAppointee(), otherParty.getAppointee().getId()));
            } else {
                otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartySubscription(),
                        OTHER_PARTY, otherParty, otherParty, otherParty.getId()));
            }

            if (hasRepresentative(otherParty)) {
                otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyRepresentativeSubscription(),
                        OTHER_PARTY, otherParty, otherParty.getRep(), otherParty.getRep().getId()));
            }
        }

        log.info("Number of subscription {}", otherPartySubscription.size());

        return otherPartySubscription;
    }

    private boolean hasOtherPartySelected(OtherParty otherParty) {
        for (var party : getNewSscsCaseData().getOtherPartySelection()) {
            if (otherParty.hasAppointee()
                    && party.getValue().getOtherPartiesList().getValue().getCode().contains(otherParty.getAppointee().getId())) {
                return true;
            }

            if (party.getValue().getOtherPartiesList().getValue().getCode().contains(otherParty.getId())) {
                return true;
            }

            if (otherParty.hasRepresentative()
                    && party.getValue().getOtherPartiesList().getValue().getCode().contains(otherParty.getRep().getId())) {
                return true;
            }
        }

        return false;
    }


    private boolean isNotificationEventValidToSendToAppointee() {
        return (hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter(responseWrapper)
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(getNotificationType())
            || EVENTS_VALID_FOR_APPOINTEE.contains(getNotificationType())
            || isValidProcessHearingRequestEventForParty(PartyItemList.APPELLANT)
            || isValidRequestInfoIncompleteEventForParty(PartyItemList.APPELLANT)))
            || (ISSUE_GENERIC_LETTER.equals(getNotificationType()) && hasAppointeeSelected());
    }

    private boolean hasAppointeeSelected() {
        return (YesNo.YES.equals(getNewSscsCaseData().getSendToAllParties()) && hasAppointee(getSscsCaseDataWrapper()))
            || (YesNo.YES.equals(getNewSscsCaseData().getSendToApellant()) && hasAppointee(getSscsCaseDataWrapper()));
    }

    private boolean isNotificationEventValidToSendToAppellant() {
        return ((getOldSscsCaseData() != null && isValidReviewConfidentialityRequest(getOldSscsCaseData().getConfidentialityRequestOutcomeAppellant(), getNewSscsCaseData().getConfidentialityRequestOutcomeAppellant()))
            || isValidProcessHearingRequestEventForParty(PartyItemList.APPELLANT)
            || isValidRequestInfoIncompleteEventForParty(PartyItemList.APPELLANT)
            || !EVENTS_MAYBE_INVALID_FOR_APPELLANT.contains(getNotificationType()))
            || (ISSUE_GENERIC_LETTER.equals(getNotificationType()) && hasAppellantSelected());
    }

    private boolean hasAppellantSelected() {
        return YesNo.YES.equals(getNewSscsCaseData().getSendToAllParties())
            || YesNo.YES.equals(getNewSscsCaseData().getSendToApellant());
    }

    private boolean isValidProcessHearingRequestEventForParty(PartyItemList partyItemList) {
        return ACTION_HEARING_RECORDING_REQUEST.equals(getNotificationType()) && hasHearingRecordingRequestsForParty(partyItemList);
    }

    private boolean hasHearingRecordingRequestsForParty(PartyItemList partyItemList) {
        List<HearingRecordingRequest> oldReleasedRecordings = new ArrayList<>();
        if (responseWrapper.getOldSscsCaseData() != null && responseWrapper.getOldSscsCaseData().getSscsHearingRecordingCaseData() != null) {
            oldReleasedRecordings = Optional.ofNullable(responseWrapper.getOldSscsCaseData().getSscsHearingRecordingCaseData().getCitizenReleasedHearings())
                    .orElse(new ArrayList<>());
        }
        return hasNewReleasedHearingRecordingForParty(oldReleasedRecordings).stream()
                .anyMatch(v -> partyItemList.getCode().equals(v.getValue().getRequestingParty()));
    }

    @NotNull
    private List<HearingRecordingRequest> hasNewReleasedHearingRecordingForParty(List<HearingRecordingRequest> oldReleasedRecordings) {
        return responseWrapper.getNewSscsCaseData().getSscsHearingRecordingCaseData().getCitizenReleasedHearings().stream()
                .filter(e -> !oldReleasedRecordings.contains(e))
                .collect(Collectors.toList());
    }

    private boolean isNotificationEventValidToSendToRep() {
        return (hasRepSubscriptionOrIsMandatoryRepLetter(responseWrapper)
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(getNotificationType())
            || EVENTS_VALID_FOR_REP.contains(getNotificationType())
            || isValidProcessHearingRequestEventForParty(PartyItemList.REPRESENTATIVE)
            || isValidRequestInfoIncompleteEventForParty(PartyItemList.REPRESENTATIVE)))
            || (ISSUE_GENERIC_LETTER.equals(getNotificationType()) && hasRepSelected());
    }

    private boolean hasRepSelected() {
        return (YesNo.YES.equals(getNewSscsCaseData().getSendToAllParties())
                && hasRepresentative(responseWrapper))
                || YesNo.YES.equals(getNewSscsCaseData().getSendToRepresentative());
    }

    private boolean isNotificationEventValidToSendToJointParty() {
        return (hasJointPartySubscription(responseWrapper)
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(getNotificationType())
            || EVENTS_VALID_FOR_JOINT_PARTY.contains(getNotificationType())
            || isValidRequestInfoIncompleteEventForParty(PartyItemList.JOINT_PARTY)
            || isValidProcessHearingRequestEventForParty(PartyItemList.JOINT_PARTY)
            || (getOldSscsCaseData() != null && isValidReviewConfidentialityRequest(getOldSscsCaseData().getConfidentialityRequestOutcomeJointParty(), getNewSscsCaseData().getConfidentialityRequestOutcomeJointParty()))))
            || (ISSUE_GENERIC_LETTER.equals(getNotificationType()) && hasJointPartySelected());
    }

    private boolean hasJointPartySelected() {
        return (YesNo.YES.equals(getNewSscsCaseData().getSendToAllParties())
                && hasJointParty(getNewSscsCaseData()))
                || YesNo.YES.equals(getNewSscsCaseData().getSendToJointParty());
    }

    private boolean isNotificationEventValidToSendToOtherPartySubscription(Subscription subscription, boolean isSendNewOtherPartyNotification) {
        return isValidSubscriptionOrIsMandatoryLetter(subscription, responseWrapper.getNotificationEventType())
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(getNotificationType())
            || EVENTS_VALID_FOR_OTHER_PARTY.contains(getNotificationType())
            || (UPDATE_OTHER_PARTY_DATA.equals(getNotificationType()) && isSendNewOtherPartyNotification));
    }


    private boolean isValidRequestInfoIncompleteEventForParty(PartyItemList partyItem) {
        return REQUEST_INFO_INCOMPLETE.equals(getNotificationType())
                && responseWrapper.getNewSscsCaseData().getInformationFromPartySelected() != null
                && responseWrapper.getNewSscsCaseData().getInformationFromPartySelected().getValue() != null
                && partyItem.getCode().equals(responseWrapper.getNewSscsCaseData().getInformationFromPartySelected().getValue().getCode());
    }

    private boolean isValidReviewConfidentialityRequest(DatedRequestOutcome previousRequestOutcome, DatedRequestOutcome latestRequestOutcome) {
        return REVIEW_CONFIDENTIALITY_REQUEST.equals(getNotificationType())
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

