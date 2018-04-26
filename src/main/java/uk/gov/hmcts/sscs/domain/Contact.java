package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Contact {
    private String email;
    private String phone;
    private String mobile;

    public Contact(String email,
                   String phone,
                   String mobile) {
        this.email = email;
        this.phone = phone;
        this.mobile = mobile;
    }
}
