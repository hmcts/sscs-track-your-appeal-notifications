package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Identity {
    private String dob;
    private String nino;

    public Identity(String dob,
                    String nino) {
        this.dob = dob;
        this.nino = nino;
    }
}
