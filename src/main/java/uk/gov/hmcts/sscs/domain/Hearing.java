package uk.gov.hmcts.sscs.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Hearing implements Comparable<Hearing> {

    private LocalDateTime hearingDateTime;
    private String venueName;
    private String venueAddressLine1;
    private String venueAddressLine2;
    private String venueTown;
    private String venueCounty;
    private String venuePostcode;
    private String venueGoogleMapUrl;

    @Override
    public int compareTo(Hearing o) {
        return getHearingDateTime().compareTo(o.getHearingDateTime());
    }
}
