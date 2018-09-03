package uk.gov.hmcts.reform.sscs.extractor;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
public class AppealReceivedDateExtractor {

    public Optional<ZonedDateTime> extract(SscsCaseData ccdResponse) {

        for (Event event : ccdResponse.getEvents()) {
            if (event.getValue() != null && event.getValue().getEventType().equals(APPEAL_RECEIVED)) {
                return Optional.of(event.getValue().getDateTime());
            }
        }

        return Optional.empty();
    }

}
