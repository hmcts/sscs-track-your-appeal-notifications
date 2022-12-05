package uk.gov.hmcts.reform.sscs.exception;

public class JobException extends RuntimeException {

    public JobException(String message, Throwable cause) {
        super(message, cause);
    }
}
