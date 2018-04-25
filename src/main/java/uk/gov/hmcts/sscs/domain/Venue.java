package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Venue {
    private String name;
    private Address address;
    private String googleMapLink;
}
