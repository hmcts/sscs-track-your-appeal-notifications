package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DateRange {
    private String start;
    private String end;

    public DateRange(String start,
                     String end) {
        this.start = start;
        this.end = end;
    }

}
