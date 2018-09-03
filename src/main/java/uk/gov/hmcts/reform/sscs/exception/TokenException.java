package uk.gov.hmcts.reform.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class TokenException extends UnknownErrorCodeException {

    public TokenException(Exception ex) {
        super(AlertLevel.P1, ex);
    }
}
