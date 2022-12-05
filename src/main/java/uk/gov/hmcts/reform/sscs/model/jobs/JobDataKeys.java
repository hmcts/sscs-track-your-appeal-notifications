package uk.gov.hmcts.reform.sscs.model.jobs;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Keys that job-scheduler stores in job's JobDataMap.
 */
@Getter
@AllArgsConstructor
public enum JobDataKeys {
    ATTEMPT("attempt"),
    PAYLOAD("payload");

    private final String key;

    @Override
    @JsonValue
    public String toString() {
        return key;
    }
}
