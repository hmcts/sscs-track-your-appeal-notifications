package uk.gov.hmcts.sscs.scheduler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import javax.validation.constraints.NotNull;

public class Trigger {

    @NotNull
    public final ZonedDateTime startDateTime;

    public Trigger(
        @JsonProperty("start_date_time") ZonedDateTime startDateTime
    ) {
        this.startDateTime = startDateTime;
    }
}
