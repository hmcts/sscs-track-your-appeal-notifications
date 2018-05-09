package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExcludeDate {
    private DateRange value;

    public ExcludeDate(DateRange value) {
        this.value = value;
    }

}
