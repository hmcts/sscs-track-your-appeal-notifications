package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

public class TokenException extends UnknownErrorCodeException {

    public TokenException(Exception ex) {
        super(AlertLevel.P1, ex);
    }
}
