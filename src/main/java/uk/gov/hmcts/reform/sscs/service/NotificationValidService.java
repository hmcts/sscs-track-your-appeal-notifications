package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENT_TYPES_FOR_BUNDLED_LETTER;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENT_TYPES_FOR_MANDATORY_LETTERS;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;


@Service
public class NotificationValidService {

    private static final String HEARING_TYPE_ONLINE_RESOLUTION = "cor";

    static boolean isMandatoryLetterEventType(NotificationEventType eventType) {
        return EVENT_TYPES_FOR_MANDATORY_LETTERS.contains(eventType);
    }

    static boolean isBundledLetter(NotificationEventType eventType) {
        return EVENT_TYPES_FOR_BUNDLED_LETTER.contains(eventType);
    }

    protected boolean isHearingTypeValidToSendNotification(SscsCaseData sscsCaseData, NotificationEventType eventType) {
        boolean isOralCase = sscsCaseData.getAppeal().getHearingOptions().isWantsToAttendHearing();
        boolean isOnlineHearing = HEARING_TYPE_ONLINE_RESOLUTION.equalsIgnoreCase(sscsCaseData.getAppeal().getHearingType());

        if (isOralCase && !isOnlineHearing && eventType.isSendForOralCase()) {
            return true;
        } else if (!isOralCase && !isOnlineHearing && eventType.isSendForPaperCase()) {
            return true;
        } else {
            return isOnlineHearing && eventType.isSendForCohCase();
        }
    }

    boolean isNotificationStillValidToSend(List<Hearing> hearings, NotificationEventType eventType) {
        switch (eventType) {
            case HEARING_BOOKED:
            case HEARING_REMINDER:
                return checkHearingIsInFuture(hearings);
            default:
                return true;
        }
    }

    boolean checkHearingIsInFuture(List<Hearing> hearings) {
        if (hearings != null && !hearings.isEmpty()) {

            Hearing latestHearing = hearings.get(0);

            LocalDateTime hearingDateTime = latestHearing.getValue().getHearingDateTime();
            if (isNull(hearingDateTime)) {
                hearingDateTime = latestHearing.getValue().getStart();
            }

            String hearingAdjourned = latestHearing.getValue().getAdjourned();
            return LocalDateTime.now().isBefore(hearingDateTime) && !"YES".equalsIgnoreCase(hearingAdjourned);
        } else {
            return false;
        }
    }

    boolean isNotificationValidForActionFurtherEvidence(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        if (notificationWrapper.getNotificationType().getEvent().equals(EventType.ACTION_FURTHER_EVIDENCE)) {
            if (isValidAppellantForSetAsideRequest(notificationWrapper, subscriptionWithType)
                    || isValidPartyForSetAsideRequest(notificationWrapper, subscriptionWithType)
                    || isValidJointPartyForSetAsideRequest(notificationWrapper, subscriptionWithType)
                    || isValidPartyRepresentativeForSetAsideRequest(notificationWrapper, subscriptionWithType)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidPartyRepresentativeForSetAsideRequest(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        return nonNull(getPartyForRepresentative(notificationWrapper.getNewSscsCaseData().getOriginalSender()
                .getValue().getCode(), subscriptionWithType.getParty().getId(), notificationWrapper.getNewSscsCaseData().getOtherParties()));
    }

    private boolean isValidAppellantForSetAsideRequest(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        if (notificationWrapper.getNewSscsCaseData().getOriginalSender().getValue().getCode().equals("appellant")) {
            if (subscriptionWithType.getParty().getId().equalsIgnoreCase(notificationWrapper.getNewSscsCaseData().getAppeal().getAppellant().getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidPartyForSetAsideRequest(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        return subscriptionWithType.getParty().getId().contains(notificationWrapper.getNewSscsCaseData().getOriginalSender().getValue().getCode())
                || (nonNull(subscriptionWithType.getPartyId()) && notificationWrapper.getNewSscsCaseData().getOriginalSender().getValue().getCode().contains(subscriptionWithType.getPartyId()));
    }

    private boolean isValidJointPartyForSetAsideRequest(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        return (notificationWrapper.getNewSscsCaseData().getOriginalSender().getValue().getCode().equalsIgnoreCase("jointParty")
                && subscriptionWithType.getParty().getId().equalsIgnoreCase(notificationWrapper.getNewSscsCaseData().getJointParty().getId()));
    }

    private CcdValue<OtherParty> getPartyForRepresentative(String representativeId, String partyId, List<CcdValue<OtherParty>> otherParties) {
        if (nonNull(partyId) && nonNull(otherParties)) {
            for (CcdValue<OtherParty> op : otherParties) {
                if (op.getValue().getId().equals(partyId)) {
                    if (op.getValue().hasRepresentative() && representativeId.contains(op.getValue().getRep().getId())) {
                        return op;
                    }
                }
            }
        }
        return null;
    }


}
