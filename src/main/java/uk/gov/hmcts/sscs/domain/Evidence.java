package uk.gov.hmcts.sscs.domain;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Evidence implements Comparable<Evidence> {

    private LocalDate dateReceived;
    private String evidenceType;
    private String evidenceProvidedBy;

    @Override
    public int compareTo(Evidence o) {
        return getDateReceived().compareTo(o.getDateReceived());
    }
}
