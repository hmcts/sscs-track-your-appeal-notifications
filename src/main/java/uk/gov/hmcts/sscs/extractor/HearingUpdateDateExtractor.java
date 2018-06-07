package uk.gov.hmcts.sscs.extractor;

import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.service.ZonedDateTimeNowProvider;

@Component
public class HearingUpdateDateExtractor {

    private final ZonedDateTimeNowProvider nowProvider;
    private final long initialDelay;
    private final long subsequentDelay;

    @Autowired
    public HearingUpdateDateExtractor(
        ZonedDateTimeNowProvider nowProvider,
        @Value("${reminder.hearingHoldingReminder.initialDelay.seconds}") long initialDelay,
        @Value("${reminder.hearingHoldingReminder.subsequentDelay.seconds}") long subsequentDelay
    ) {
        this.nowProvider = nowProvider;
        this.initialDelay = initialDelay;
        this.subsequentDelay = subsequentDelay;
    }

    public Optional<ZonedDateTime> extract(CcdResponse ccdResponse) {

        if (!ccdResponse.getNotificationType().equals(DWP_RESPONSE_RECEIVED)
            && !ccdResponse.getNotificationType().equals(HEARING_HOLDING_REMINDER)) {
            return Optional.empty();
        }

        final ZonedDateTime now = nowProvider.now();

        long delay;

        if (ccdResponse.getNotificationType().equals(DWP_RESPONSE_RECEIVED)) {
            delay = initialDelay;
        } else {
            delay = subsequentDelay;
        }

        return Optional.of(
            now.plusSeconds(delay)
        );
    }

}
