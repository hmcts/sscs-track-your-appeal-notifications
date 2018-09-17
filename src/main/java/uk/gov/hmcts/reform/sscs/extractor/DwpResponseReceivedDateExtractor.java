package uk.gov.hmcts.reform.sscs.extractor;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPOND;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
public class DwpResponseReceivedDateExtractor {

    public Optional<ZonedDateTime> extract(SscsCaseData caseData) {

        for (Event event : caseData.getEvents()) {
            if (event.getValue() != null && event.getValue().getEventType().equals(DWP_RESPOND)) {
                return Optional.of(event.getValue().getDateTime());
            }
        }

        return Optional.empty();
    }

}
