package uk.gov.hmcts.sscs.extractor;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.CcdResponse;

@Component
public class HearingUpdateDateExtractor {

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;
    private final long hearingUpdateDelay;

    @Autowired
    public HearingUpdateDateExtractor(
        DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor,
        @Value("${hearingUpdate.delay.seconds}") long hearingUpdateDelay
    ) {
        this.dwpResponseReceivedDateExtractor = dwpResponseReceivedDateExtractor;
        this.hearingUpdateDelay = hearingUpdateDelay;
    }

    public Optional<ZonedDateTime> extract(CcdResponse ccdResponse) {

        Optional<ZonedDateTime> dwpResponseReceivedDate =
            dwpResponseReceivedDateExtractor.extract(ccdResponse);

        if (!dwpResponseReceivedDate.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(
            dwpResponseReceivedDate.get().plusSeconds(hearingUpdateDelay)
        );
    }

}
