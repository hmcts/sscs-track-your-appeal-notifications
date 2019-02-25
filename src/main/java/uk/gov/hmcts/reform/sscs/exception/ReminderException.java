package uk.gov.hmcts.reform.sscs.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ReminderException extends RuntimeException {

    public ReminderException(Exception ex) {
        super(ex);
    }
}
