package uk.gov.hmcts.sscs.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.Hearing;
import uk.gov.hmcts.sscs.domain.notify.EventType;

@Service
public class NotificationValidService {

    public Boolean isNotificationStillValidToSend(List<Hearing> hearings, EventType eventType) {
        switch (eventType) {
            case HEARING_BOOKED: return checkHearingIsInFuture(hearings);
            case HEARING_REMINDER: return checkHearingIsInFuture(hearings);
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
