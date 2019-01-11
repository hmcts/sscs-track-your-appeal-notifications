package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;

@Service
public class NotificationValidService {

    private static final String HEARING_TYPE_ONLINE_RESOLUTION = "cor";

    static final boolean isMandatoryLetter(NotificationEventType eventType) {
        boolean isMandatoryLetter = STRUCK_OUT.equals(eventType);

        if (isMandatoryLetter) {
            return true;
        } else {
            return false;
        }
    }

    boolean isHearingTypeValidToSendNotification(SscsCaseData sscsCaseData, NotificationEventType eventType) {

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
            case HEARING_BOOKED_NOTIFICATION:
                return checkHearingIsInFuture(hearings);
            case HEARING_REMINDER_NOTIFICATION:
                return checkHearingIsInFuture(hearings);
            default:
                return true;
        }
    }

    private boolean checkHearingIsInFuture(List<Hearing> hearings) {
        if (hearings != null && !hearings.isEmpty()) {

            Hearing latestHearing = hearings.get(0);
            LocalDateTime hearingDateTime = latestHearing.getValue().getHearingDateTime();
            return hearingDateTime.isAfter(LocalDateTime.now());
        } else {
            return false;
        }
    }
}