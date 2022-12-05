package uk.gov.hmcts.reform.sscs.model.jobs;

public interface JobService {

    void start();

    void stop(boolean waitForJobsToComplete);
}
