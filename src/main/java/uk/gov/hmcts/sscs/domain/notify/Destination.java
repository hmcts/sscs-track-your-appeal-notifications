package uk.gov.hmcts.sscs.domain.notify;

public class Destination {
    public final String email;
    public final String sms;

    public Destination(String email, String sms) {
        this.email = email;
        this.sms = sms;
    }
}
