package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HearingDetails {
    private Venue venue;
    private String hearingDate;
    private String time;
    private String adjourned;

    @JsonIgnore
    public LocalDateTime getHearingDateTime() {
        return LocalDateTime.of(LocalDate.parse(hearingDate), LocalTime.parse(time));
    }
}
