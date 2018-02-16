package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.sscs.deserialize.CcdResponseDeserializer;

@JsonDeserialize(using = CcdResponseDeserializer.class)
public class CcdResponseWrapper {

    private CcdResponse newCcdResponse;

    private CcdResponse oldCcdResponse;

    public CcdResponseWrapper() {
        //
    }

    public CcdResponseWrapper(CcdResponse newCcdResponse, CcdResponse oldCcdResponse) {
        this.newCcdResponse = newCcdResponse;
        this.oldCcdResponse = oldCcdResponse;
    }

    public CcdResponse getNewCcdResponse() {
        return newCcdResponse;
    }

    public CcdResponse getOldCcdResponse() {
        return oldCcdResponse;
    }

    @Override
    public String toString() {
        return "CcdResponseWrapper{"
                + " newCcdResponse=" + newCcdResponse
                + ", oldCcdResponse=" + oldCcdResponse
                + '}';
    }
}
