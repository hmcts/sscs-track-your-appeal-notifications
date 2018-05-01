package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import uk.gov.hmcts.sscs.domain.notify.Destination;

@Data
@Builder(toBuilder = true)
public class Subscription {

    private String tya;
    private String email;
    private String mobile;
    private String subscribeSms;
    private String subscribeEmail;

    @JsonIgnore
    public Boolean isSubscribeSms() {
        if (subscribeSms == null || subscribeSms.toLowerCase().equals("no")) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    public Boolean isSubscribeEmail() {
        if (subscribeEmail == null || subscribeEmail.toLowerCase().equals("no"))  {
            return false;
        }
        return true;
    }

    @JsonIgnore
    public Boolean isSubscribed() {
        return (isSubscribeSms() || isSubscribeEmail()) ? true : false;
    }

    @JsonIgnore
    public Destination getDestination() {
        return Destination.builder().email(email).sms(mobile).build();
    }
}
