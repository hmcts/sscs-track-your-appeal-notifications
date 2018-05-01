package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Contact {
    private String email;
    private String phone;

    public Contact(String email,
                   String phone) {
        this.email = email;
        this.phone = phone;
    }
}
