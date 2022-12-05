package uk.gov.hmcts.reform.sscs.model.jobs;

public interface JobExecutor<T> {

    void execute(
        String jobId,
        String jobGroup,
        String jobName,
        T payload
    );

}
