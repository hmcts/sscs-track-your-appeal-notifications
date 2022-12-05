package uk.gov.hmcts.reform.sscs.model.jobs;

public interface JobPayloadSerializer<T> {

    String serialize(T payload);
}
