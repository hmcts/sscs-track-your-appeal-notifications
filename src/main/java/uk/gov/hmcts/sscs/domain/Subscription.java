package uk.gov.hmcts.sscs.domain;

import uk.gov.hmcts.sscs.domain.notify.Destination;

public class Subscription {

    private String firstName;
    private String surname;
    private String title;
    private String appealNumber;
    private String email;
    private String mobileNumber;
    private Boolean subscribeSms;
    private Boolean subscribeEmail;

    public Subscription() {
        //
    }

    public Subscription(String firstName, String surname, String title, String appealNumber, String email,
                        String mobileNumber, Boolean subscribeSms, Boolean subscribeEmail) {
        this.firstName = firstName;
        this.surname = surname;
        this.title = title;
        this.appealNumber = appealNumber;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.subscribeSms = subscribeSms;
        this.subscribeEmail = subscribeEmail;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAppealNumber() {
        return appealNumber;
    }

    public void setAppealNumber(String appealNumber) {
        this.appealNumber = appealNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public Boolean isSubscribeSms() {
        if (subscribeSms == null) {
            subscribeSms = false;
        }
        return subscribeSms;
    }

    public void setSubscribeSms(Boolean subscribeSms) {
        this.subscribeSms = subscribeSms;
    }

    public Boolean isSubscribeEmail() {
        if (subscribeEmail == null) {
            subscribeEmail = false;
        }
        return subscribeEmail;
    }

    public void setSubscribeEmail(Boolean subscribeEmail) {
        this.subscribeEmail = subscribeEmail;
    }

    public Destination getDestination() {
        return new Destination(email, mobileNumber);
    }

    @Override
    public String toString() {
        return "Subscription{"
                + " firstName='" + firstName + '\''
                + ", surname='" + surname + '\''
                + ", title='" + title + '\''
                + ", appealNumber='" + appealNumber + '\''
                + ", email='" + email + '\''
                + ", mobileNumber='" + mobileNumber + '\''
                + ", subscribeSms='" + subscribeSms + '\''
                + ", subscribeEmail='" + subscribeEmail + '\''
                + '}';
    }
}
