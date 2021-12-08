package uk.gov.hmcts.reform.sscs.factory;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.scheduler.CcdActionSerializer;

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

        if (isNotificationEventValidToSendToAppointee()) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppointeeSubscription(), APPOINTEE));
        } else if (isNotificationEventValidToSendToAppellant()) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppellantSubscription(), APPELLANT));
        }

        if (isNotificationEventValidToSendToRep()) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getRepresentativeSubscription(), REPRESENTATIVE));
        }

        if (isNotificationEventValidToSendToJointParty()) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getJointPartySubscription(), JOINT_PARTY));
        }

        subscriptionWithTypeList.addAll(getOtherPartySubscriptions());

        return subscriptionWithTypeList;
    }

    private List<SubscriptionWithType> filterOtherPartySubscription(OtherParty otherParty) {
        List<SubscriptionWithType> otherPartySubscription = new ArrayList<>();
        boolean isSendNewOtherPartyNotification = YesNo.isYes(otherParty.getSendNewOtherPartyNotification());

        if (hasAppointee(otherParty.getAppointee(), otherParty.getIsAppointee())
                && isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartyAppointeeSubscription(), isSendNewOtherPartyNotification)) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyAppointeeSubscription(), OTHER_PARTY, Integer.parseInt(otherParty.getAppointee().getId())));
        } else if (isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartySubscription(), isSendNewOtherPartyNotification)) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartySubscription(), OTHER_PARTY, Integer.parseInt(otherParty.getId())));
        }

        if (hasRepresentative(otherParty)
                && isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartyRepresentativeSubscription(), isSendNewOtherPartyNotification)) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyRepresentativeSubscription(), OTHER_PARTY, Integer.parseInt(otherParty.getRep().getId())));
        }

        return otherPartySubscription;
    }


    private boolean isNotificationEventValidToSendToAppointee() {
        return hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter(responseWrapper)
                && (SYA_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
                || ADJOURNED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_DORMANT_NOTIFICATION.equals(getNotificationType())
                || APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || HMCTS_APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || DWP_APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || DWP_UPLOAD_RESPONSE_NOTIFICATION.equals(getNotificationType())
                || APPEAL_WITHDRAWN_NOTIFICATION.equals(getNotificationType())
                || ADMIN_APPEAL_WITHDRAWN.equals(getNotificationType())
                || EVIDENCE_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || HEARING_BOOKED_NOTIFICATION.equals(getNotificationType())
                || POSTPONEMENT_NOTIFICATION.equals(getNotificationType())
                || SUBSCRIPTION_UPDATED_NOTIFICATION.equals(getNotificationType())
                || EVIDENCE_REMINDER_NOTIFICATION.equals(getNotificationType())
                || HEARING_REMINDER_NOTIFICATION.equals(getNotificationType())
                || STRUCK_OUT.equals(getNotificationType())
                || VALID_APPEAL_CREATED.equals(getNotificationType())
                || PROCESS_AUDIO_VIDEO.equals(getNotificationType())
                || PROCESS_AUDIO_VIDEO_WELSH.equals(getNotificationType())
                || DIRECTION_ISSUED.equals(getNotificationType())
                || DECISION_ISSUED.equals(getNotificationType())
                || DIRECTION_ISSUED_WELSH.equals(getNotificationType())
                || DECISION_ISSUED_WELSH.equals(getNotificationType())
                || ISSUE_FINAL_DECISION.equals(getNotificationType())
                || ISSUE_FINAL_DECISION_WELSH.equals(getNotificationType())
                || ISSUE_ADJOURNMENT_NOTICE.equals(getNotificationType())
                || ISSUE_ADJOURNMENT_NOTICE_WELSH.equals(getNotificationType())
                || JUDGE_DECISION_APPEAL_TO_PROCEED.equals(getNotificationType())
                || TCW_DECISION_APPEAL_TO_PROCEED.equals(getNotificationType())
                || NON_COMPLIANT_NOTIFICATION.equals(getNotificationType())
                || RESEND_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
                || JOINT_PARTY_ADDED.equals(getNotificationType())
                || isValidProcessHearingRequestEventForParty(PartyItemList.APPELLANT)
                || ACTION_POSTPONEMENT_REQUEST.equals(getNotificationType())
                || ACTION_POSTPONEMENT_REQUEST_WELSH.equals(getNotificationType())
                || DEATH_OF_APPELLANT.equals(getNotificationType())
                || PROVIDE_APPOINTEE_DETAILS.equals(getNotificationType())
                || isValidRequestInfoIncompleteEventForParty(PartyItemList.APPELLANT));
    }

    private boolean isNotificationEventValidToSendToAppellant() {
        // Special list of notifications that might not be sent to appellant, depending on data set on the case
        List<NotificationEventType> notificationsMaybeNotForAppellant = List.of(REVIEW_CONFIDENTIALITY_REQUEST, REQUEST_INFO_INCOMPLETE, ACTION_HEARING_RECORDING_REQUEST, UPDATE_OTHER_PARTY_DATA);

        return (getOldSscsCaseData() != null && isValidReviewConfidentialityRequest(getOldSscsCaseData().getConfidentialityRequestOutcomeAppellant(), getNewSscsCaseData().getConfidentialityRequestOutcomeAppellant()))
                || isValidProcessHearingRequestEventForParty(PartyItemList.APPELLANT)
                || isValidRequestInfoIncompleteEventForParty(PartyItemList.APPELLANT)
                || !notificationsMaybeNotForAppellant.contains(getNotificationType());
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
        return hasRepSubscriptionOrIsMandatoryRepLetter(responseWrapper)
                && (APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || HMCTS_APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || DWP_APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_WITHDRAWN_NOTIFICATION.equals(getNotificationType())
                || ADMIN_APPEAL_WITHDRAWN.equals(getNotificationType())
                || EVIDENCE_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || SYA_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
                || RESEND_APPEAL_CREATED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_DORMANT_NOTIFICATION.equals(getNotificationType())
                || ADJOURNED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || DWP_UPLOAD_RESPONSE_NOTIFICATION.equals(getNotificationType())
                || POSTPONEMENT_NOTIFICATION.equals(getNotificationType())
                || HEARING_BOOKED_NOTIFICATION.equals(getNotificationType())
                || SUBSCRIPTION_UPDATED_NOTIFICATION.equals(getNotificationType())
                || CASE_UPDATED.equals(getNotificationType())
                || EVIDENCE_REMINDER_NOTIFICATION.equals(getNotificationType())
                || HEARING_REMINDER_NOTIFICATION.equals(getNotificationType())
                || STRUCK_OUT.equals(getNotificationType())
                || PROCESS_AUDIO_VIDEO.equals(getNotificationType())
                || PROCESS_AUDIO_VIDEO_WELSH.equals(getNotificationType())
                || DIRECTION_ISSUED.equals(getNotificationType())
                || DECISION_ISSUED.equals(getNotificationType())
                || DIRECTION_ISSUED_WELSH.equals(getNotificationType())
                || DECISION_ISSUED_WELSH.equals(getNotificationType())
                || ISSUE_FINAL_DECISION.equals(getNotificationType())
                || ISSUE_FINAL_DECISION_WELSH.equals(getNotificationType())
                || ISSUE_ADJOURNMENT_NOTICE.equals(getNotificationType())
                || ISSUE_ADJOURNMENT_NOTICE_WELSH.equals(getNotificationType())
                || JUDGE_DECISION_APPEAL_TO_PROCEED.equals(getNotificationType())
                || TCW_DECISION_APPEAL_TO_PROCEED.equals(getNotificationType())
                || NON_COMPLIANT_NOTIFICATION.equals(getNotificationType())
                || VALID_APPEAL_CREATED.equals(getNotificationType())
                || isValidProcessHearingRequestEventForParty(PartyItemList.REPRESENTATIVE)
                || ACTION_POSTPONEMENT_REQUEST.equals(getNotificationType())
                || ACTION_POSTPONEMENT_REQUEST_WELSH.equals(getNotificationType())
                || DEATH_OF_APPELLANT.equals(getNotificationType())
                || PROVIDE_APPOINTEE_DETAILS.equals(getNotificationType())
                || isValidRequestInfoIncompleteEventForParty(PartyItemList.REPRESENTATIVE));
    }

    private boolean isNotificationEventValidToSendToJointParty() {
        return hasJointPartySubscription(responseWrapper)
                && (APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_DORMANT_NOTIFICATION.equals(getNotificationType())
                || ADJOURNED_NOTIFICATION.equals(getNotificationType())
                || HEARING_BOOKED_NOTIFICATION.equals(getNotificationType())
                || HEARING_REMINDER_NOTIFICATION.equals(getNotificationType())
                || POSTPONEMENT_NOTIFICATION.equals(getNotificationType())
                || EVIDENCE_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_WITHDRAWN_NOTIFICATION.equals(getNotificationType())
                || ADMIN_APPEAL_WITHDRAWN.equals(getNotificationType())
                || STRUCK_OUT.equals(getNotificationType())
                || ISSUE_ADJOURNMENT_NOTICE.equals(getNotificationType())
                || ISSUE_ADJOURNMENT_NOTICE_WELSH.equals(getNotificationType())
                || PROCESS_AUDIO_VIDEO.equals(getNotificationType())
                || PROCESS_AUDIO_VIDEO_WELSH.equals(getNotificationType())
                || DIRECTION_ISSUED.equals(getNotificationType())
                || DIRECTION_ISSUED_WELSH.equals(getNotificationType())
                || DWP_UPLOAD_RESPONSE_NOTIFICATION.equals(getNotificationType())
                || EVIDENCE_REMINDER_NOTIFICATION.equals(getNotificationType())
                || JOINT_PARTY_ADDED.equals(getNotificationType())
                || ACTION_POSTPONEMENT_REQUEST.equals(getNotificationType())
                || ACTION_POSTPONEMENT_REQUEST_WELSH.equals(getNotificationType())
                || isValidRequestInfoIncompleteEventForParty(PartyItemList.JOINT_PARTY)
                || isValidProcessHearingRequestEventForParty(PartyItemList.JOINT_PARTY)
                || (getOldSscsCaseData() != null && isValidReviewConfidentialityRequest(getOldSscsCaseData().getConfidentialityRequestOutcomeJointParty(), getNewSscsCaseData().getConfidentialityRequestOutcomeJointParty())));
    }

    private boolean isNotificationEventValidToSendToOtherPartySubscription(Subscription subscription, boolean isSendNewOtherPartyNotification) {
        return isValidSubscriptionOrIsMandatoryLetter(subscription, responseWrapper.getNotificationEventType())
                && (DWP_UPLOAD_RESPONSE_NOTIFICATION.equals(getNotificationType())
                || ADJOURNED_NOTIFICATION.equals(getNotificationType())
                || POSTPONEMENT_NOTIFICATION.equals(getNotificationType())
                || APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || DWP_APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || HMCTS_APPEAL_LAPSED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_WITHDRAWN_NOTIFICATION.equals(getNotificationType())
                || ADMIN_APPEAL_WITHDRAWN.equals(getNotificationType())
                || SUBSCRIPTION_CREATED_NOTIFICATION.equals(getNotificationType())
                || SUBSCRIPTION_UPDATED_NOTIFICATION.equals(getNotificationType())
                || SUBSCRIPTION_OLD_NOTIFICATION.equals(getNotificationType())
                || HEARING_BOOKED_NOTIFICATION.equals(getNotificationType())
                || HEARING_REMINDER_NOTIFICATION.equals(getNotificationType())
                || DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || APPEAL_DORMANT_NOTIFICATION.equals(getNotificationType())
                || EVIDENCE_REMINDER_NOTIFICATION.equals(getNotificationType())
                || EVIDENCE_RECEIVED_NOTIFICATION.equals(getNotificationType())
                || STRUCK_OUT.equals(getNotificationType())
                || PROCESS_AUDIO_VIDEO.equals(getNotificationType())
                || DIRECTION_ISSUED.equals(getNotificationType())
                || DECISION_ISSUED.equals(getNotificationType())
                || ADJOURNED_NOTIFICATION.equals(getNotificationType())
                || ISSUE_ADJOURNMENT_NOTICE.equals(getNotificationType())
                || REQUEST_INFO_INCOMPLETE.equals(getNotificationType())
                || NON_COMPLIANT_NOTIFICATION.equals(getNotificationType())
                || ISSUE_FINAL_DECISION.equals(getNotificationType())
                || PROCESS_AUDIO_VIDEO_WELSH.equals(getNotificationType())
                || DIRECTION_ISSUED_WELSH.equals(getNotificationType())
                || ISSUE_FINAL_DECISION_WELSH.equals(getNotificationType())
                || DECISION_ISSUED_WELSH.equals(getNotificationType())
                || ISSUE_ADJOURNMENT_NOTICE_WELSH.equals(getNotificationType())
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

