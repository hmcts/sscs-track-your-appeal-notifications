package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

public class NotificationClientRuntimeException extends UnknownErrorCodeException {

    public NotificationClientRuntimeException(Exception ex) {
        super(AlertLevel.P1, ex);
    }
}
