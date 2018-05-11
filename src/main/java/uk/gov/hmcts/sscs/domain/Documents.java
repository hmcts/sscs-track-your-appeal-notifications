package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Documents implements Comparable<Documents> {
    private Doc value;

    @Override
    public int compareTo(Documents o) {
        return value.getEvidenceDateTimeFormatted().compareTo(o.getValue().getEvidenceDateTimeFormatted());
    }

    @JsonCreator
    public Documents(@JsonProperty("value") Doc value) {
        this.value = value;
    }
}
