package uk.gov.hmcts.reform.sscs.model.jobs;

public class JobClassMapping<T> {
    private final Class<T> canHandlePayloadClass;
    private final JobPayloadSerializer<T> jobPayloadSerializer;

    public JobClassMapping(Class<T> canHandlePayloadClass,
                           JobPayloadSerializer<T> jobPayloadSerializer) {
        this.canHandlePayloadClass = canHandlePayloadClass;
        this.jobPayloadSerializer = jobPayloadSerializer;
    }

    public boolean canHandle(Class payload) {
        return canHandlePayloadClass.equals(payload);
    }

    public String serialize(T payload) {
        return jobPayloadSerializer.serialize(payload);
    }
}
