package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

public class NotificationServiceException extends UnknownErrorCodeException {

    public NotificationServiceException(Exception ex) {
        super(AlertLevel.P3, ex);
    }
}
