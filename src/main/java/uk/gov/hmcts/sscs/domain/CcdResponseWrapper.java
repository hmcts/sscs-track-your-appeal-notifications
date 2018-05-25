package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.sscs.deserialize.CcdResponseWrapperDeserializer;

@Value
@Builder
@JsonDeserialize(using = CcdResponseWrapperDeserializer.class)
public class CcdResponseWrapper {

    private CcdResponse newCcdResponse;
    private CcdResponse oldCcdResponse;

}
