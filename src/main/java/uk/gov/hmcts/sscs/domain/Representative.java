package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Representative {

    private String organisation;
    private Name name;
    private Address address;
    private Contact contact;

    @JsonCreator
    public Representative(String organisation,
                          Name name,
                          Address address,
                          Contact contact) {
        this.organisation = organisation;
        this.name = name;
        this.address = address;
        this.contact = contact;
    }
}
