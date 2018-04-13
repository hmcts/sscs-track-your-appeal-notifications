package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.sscs.deserialize.CcdResponseDeserializer;

@Value
@Builder
@JsonDeserialize(using = CcdResponseDeserializer.class)
public class CcdResponseWrapper {

    private CcdResponse newCcdResponse;
    private CcdResponse oldCcdResponse;

}
