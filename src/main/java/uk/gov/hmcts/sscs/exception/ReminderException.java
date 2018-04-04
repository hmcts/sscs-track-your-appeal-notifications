package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

public class ReminderException extends UnknownErrorCodeException {

    public ReminderException(Exception ex) {
        super(AlertLevel.P4, ex);
    }
}
