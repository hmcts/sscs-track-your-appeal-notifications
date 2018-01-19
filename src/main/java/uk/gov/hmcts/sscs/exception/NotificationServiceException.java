package uk.gov.hmcts.sscs.exception;


public class NotificationServiceException extends Exception {

    public NotificationServiceException(Exception ex) {
        super(ex);
    }
}
