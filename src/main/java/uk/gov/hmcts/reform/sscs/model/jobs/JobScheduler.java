package uk.gov.hmcts.reform.sscs.model.jobs;

public interface JobScheduler {

    <T> String schedule(Job<T> job);
}
