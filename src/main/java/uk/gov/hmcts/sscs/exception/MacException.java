package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

public class MacException extends UnknownErrorCodeException {

    public MacException(Exception ex) {
        super(AlertLevel.P1, ex);
    }
}
