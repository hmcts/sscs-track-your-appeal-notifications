package uk.gov.hmcts.reform.sscs.service.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class TcaJobPayload {

    private  static final String CASE_ID = "caseId";

    private Map<String,String> tcaEvent;

    public TcaJobPayload() {
        // Void
    }

    public TcaJobPayload(long caseId) {
        this.tcaEvent = Collections.singletonMap(CASE_ID, Long.toString(caseId));
    }

    public Map<String, String> getTcaEvent() {
        return tcaEvent;
    }

    long getCaseId() {
        return Long.parseLong(tcaEvent.get(CASE_ID));
    }
}
