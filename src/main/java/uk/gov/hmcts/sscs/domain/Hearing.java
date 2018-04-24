package uk.gov.hmcts.sscs.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Hearing implements Comparable<Hearing> {

    private HearingDetails value;

    @Override
    public int compareTo(Hearing o) {
        return value.getHearingDateTime().compareTo(o.getValue().getHearingDateTime());
    }
}
