package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Appellant {

    private Name name;
    private Address address;
    private Contact contact;
    private Identity identity;
    private String isAppointee;

    public Appellant(Name name,
                     Address address,
                     Contact contact,
                     Identity identity,
                     String isAppointee) {
        this.name = name;
        this.address = address;
        this.contact = contact;
        this.identity = identity;
        this.isAppointee = isAppointee;
    }
}
