package uk.gov.hmcts.reform.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AuthorisationException extends UnknownErrorCodeException {

    public AuthorisationException(Exception ex) {
        super(AlertLevel.P4, ex);
    }
}
