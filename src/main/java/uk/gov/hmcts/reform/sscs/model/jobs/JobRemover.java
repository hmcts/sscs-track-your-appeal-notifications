package uk.gov.hmcts.reform.sscs.model.jobs;

public interface JobRemover {

    void remove(String jobId, String jobGroup);

    void removeGroup(String jobGroup);

}
