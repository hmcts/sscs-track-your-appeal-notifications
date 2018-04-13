package uk.gov.hmcts.sscs.domain;

import lombok.*;
import uk.gov.hmcts.sscs.domain.notify.Destination;

@Data
@Builder(toBuilder = true)
public class Subscription {

    private String firstName;
    private String surname;
    private String title;
    private String appealNumber;
    private String email;
    private String mobileNumber;
    @Getter(AccessLevel.NONE) private Boolean subscribeSms;
    @Getter(AccessLevel.NONE) private Boolean subscribeEmail;

    public Boolean isSubscribeSms() {
        if (subscribeSms == null) {
            return false;
        }
        return subscribeSms;
    }

    public Boolean isSubscribeEmail() {
        if (subscribeEmail == null) {
            return false;
        }
        return subscribeEmail;
    }

    public Destination getDestination() {
        return Destination.builder().email(email).sms(mobileNumber).build();
    }
}
