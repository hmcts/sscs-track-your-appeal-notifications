package uk.gov.hmcts.reform.sscs.extractor;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPOND;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
public class DwpResponseReceivedDateExtractor {

    private static final List<EventType> DWP_RESPONSE_EVENT_TYPES = Arrays.asList(DWP_RESPOND, DWP_UPLOAD_RESPONSE);

    public Optional<ZonedDateTime> extract(SscsCaseData caseData) {

        for (Event event : caseData.getEvents()) {
            if (event.getValue() != null && DWP_RESPONSE_EVENT_TYPES.contains(event.getValue().getEventType())) {
                return Optional.of(event.getValue().getDateTime());
            }
        }

        return Optional.empty();
    }

}
