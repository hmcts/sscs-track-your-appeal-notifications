package uk.gov.hmcts.reform.sscs.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class CcdUtilException extends RuntimeException {

    public CcdUtilException(Exception ex) {
        super(ex);
    }
}
