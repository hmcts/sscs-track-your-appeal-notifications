package uk.gov.hmcts.sscs.extractor;

import static org.slf4j.LoggerFactory.getLogger;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.EventType;

@Component
public class HearingContactDateExtractor {

    private static final org.slf4j.Logger LOG = getLogger(HearingContactDateExtractor.class);

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;
    private final long initialDelay;
    private final long subsequentDelay;

    @Autowired
    public HearingContactDateExtractor(
        DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor,
        @Value("${reminder.hearingContactDate.initialDelay.seconds}") long initialDelay,
        @Value("${reminder.hearingContactDate.subsequentDelay.seconds}") long subsequentDelay
    ) {
        this.dwpResponseReceivedDateExtractor = dwpResponseReceivedDateExtractor;
        this.initialDelay = initialDelay;
        this.subsequentDelay = subsequentDelay;
    }

    public Optional<ZonedDateTime> extract(CcdResponse ccdResponse) {
        return extractForReferenceEvent(ccdResponse, ccdResponse.getNotificationType());
    }

    public Optional<ZonedDateTime> extractForReferenceEvent(
        CcdResponse ccdResponse,
        EventType referenceEventType
    ) {
        long delay;

        switch (referenceEventType) {

            case DWP_RESPONSE_RECEIVED:
            case POSTPONEMENT:
                delay = initialDelay;
                break;

            case FIRST_HEARING_HOLDING_REMINDER:
                delay = initialDelay + subsequentDelay;
                break;

            case SECOND_HEARING_HOLDING_REMINDER:
                delay = initialDelay + (subsequentDelay * 2);
                break;

            case THIRD_HEARING_HOLDING_REMINDER:
                delay = initialDelay + (subsequentDelay * 3);
                break;

            default:
                return Optional.empty();
        }

        Optional<ZonedDateTime> optionalDwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse);

        LOG.info("@@ Reference event type: " + referenceEventType.getId());
        if (optionalDwpResponseReceivedDate.isPresent()) {
            LOG.info("@@ DWP Response Date Extracted: " + optionalDwpResponseReceivedDate.get().toString());
        } else {
            LOG.info("@@ DWP Response Date Extracted: NONE");
        }

        return optionalDwpResponseReceivedDate
            .map(dwpResponseReceivedDate -> dwpResponseReceivedDate.plusSeconds(delay));
    }

}
