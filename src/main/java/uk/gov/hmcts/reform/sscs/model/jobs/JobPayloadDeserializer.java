package uk.gov.hmcts.reform.sscs.model.jobs;

public interface JobPayloadDeserializer<T> {

    T deserialize(String payload);
}
