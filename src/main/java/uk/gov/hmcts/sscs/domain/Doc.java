package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Doc {
    private String dateReceived;
    private String evidenceType;
    private String evidenceProvidedBy;

    @JsonCreator
    public Doc(@JsonProperty("dateReceived") String dateReceived,
               @JsonProperty("evidenceType") String evidenceType,
               @JsonProperty("evidenceProvidedBy") String evidenceProvidedBy) {
        this.dateReceived = dateReceived;
        this.evidenceType = evidenceType;
        this.evidenceProvidedBy = evidenceProvidedBy;
    }

    @JsonIgnore
    public LocalDate getEvidenceDateTimeFormatted() {
        return LocalDate.parse(dateReceived);
    }
}
