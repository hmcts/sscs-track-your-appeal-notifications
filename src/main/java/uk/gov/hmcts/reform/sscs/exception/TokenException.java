package uk.gov.hmcts.reform.sscs.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class TokenException extends RuntimeException {

    public TokenException(Exception ex) {
        super(ex);
    }
}
