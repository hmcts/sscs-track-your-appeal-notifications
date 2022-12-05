package uk.gov.hmcts.reform.sscs.model.jobs;

import java.util.List;

public class JobClassMapper {
    private final List<JobClassMapping> jobMappings;

    public JobClassMapper(List<JobClassMapping> jobMappings) {
        this.jobMappings = jobMappings;
    }

    public <T> JobClassMapping<T> getJobMapping(Class<T> payload) {
        return jobMappings.stream()
                .filter(jobMapping -> jobMapping.canHandle(payload))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot map payload of type [" + payload.getSimpleName() + "]"));
    }
}
