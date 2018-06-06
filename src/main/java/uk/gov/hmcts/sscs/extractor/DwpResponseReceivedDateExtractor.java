package uk.gov.hmcts.sscs.extractor;

import static uk.gov.hmcts.sscs.domain.notify.EventType.DWP_RESPONSE_RECEIVED;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Events;

@Component
public class DwpResponseReceivedDateExtractor {

    public Optional<ZonedDateTime> extract(CcdResponse ccdResponse) {

        for (Events events : ccdResponse.getEvents()) {
            if (events.getValue() != null && events.getValue().getEventType().equals(DWP_RESPONSE_RECEIVED)) {
                return Optional.of(events.getValue().getDateTime());
            }
        }

        return Optional.empty();
    }

}
