package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class CcdUtilException extends UnknownErrorCodeException {

    public CcdUtilException(Exception ex) {
        super(AlertLevel.P4, ex);
    }
}
