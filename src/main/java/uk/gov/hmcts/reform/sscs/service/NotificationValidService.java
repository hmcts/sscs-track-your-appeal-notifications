package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

@Service
public class NotificationValidService {

    public Boolean isHearingTypeValidToSendNotification(Boolean isOralCase, NotificationEventType eventType) {
        if (isOralCase && eventType.isSendForOralCase()) {
            return true;
        } else if (!isOralCase && eventType.isSendForPaperCase()) {
            return true;
        }
        return false;
    }

    public Boolean isNotificationStillValidToSend(List<Hearing> hearings, NotificationEventType eventType) {
        switch (eventType) {
            case HEARING_BOOKED_NOTIFICATION: return checkHearingIsInFuture(hearings);
            case HEARING_REMINDER_NOTIFICATION: return checkHearingIsInFuture(hearings);
            default: return true;
        }
    }

    private Boolean checkHearingIsInFuture(List<Hearing> hearings) {
        if (hearings != null && !hearings.isEmpty()) {

            Hearing latestHearing = hearings.get(0);
            LocalDateTime hearingDateTime = latestHearing.getValue().getHearingDateTime();
            return hearingDateTime.isAfter(LocalDateTime.now());
        } else {
            return false;
        }
    }
}
