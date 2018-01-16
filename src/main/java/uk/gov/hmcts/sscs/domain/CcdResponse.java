package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.sscs.deserialize.CcdResponseDeserializer;

@JsonDeserialize(using = CcdResponseDeserializer.class)
public class CcdResponse {

    private String appellantFirstName;
    private String appellantSurname;
    private String appellantTitle;
    private String appealNumber;
    private String appealStatus;
    private String email;
    private String phoneNumber;
    private String mobileNumber;

    public String getAppellantFirstName() {
        return appellantFirstName;
    }

    public void setAppellantFirstName(String appellantFirstName) {
        this.appellantFirstName = appellantFirstName;
    }

    public String getAppellantSurname() {
        return appellantSurname;
    }

    public void setAppellantSurname(String appellantSurname) {
        this.appellantSurname = appellantSurname;
    }

    public String getAppealNumber() {
        return appealNumber;
    }

    public void setAppealNumber(String appealNumber) {
        this.appealNumber = appealNumber;
    }

    public String getAppealStatus() {
        return appealStatus;
    }

    public void setAppealStatus(String appealStatus) {
        this.appealStatus = appealStatus;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getAppellantTitle() {
        return appellantTitle;
    }

    public void setAppellantTitle(String appellantTitle) {
        this.appellantTitle = appellantTitle;
    }

    @Override
    public String toString() {
        return "CcdResponse{"
                + " appellantFirstName='" + appellantFirstName + '\''
                + ", appellantSurname='" + appellantSurname + '\''
                + ", appellantTitle='" + appellantTitle + '\''
                + ", appealNumber='" + appealNumber + '\''
                + ", appealStatus='" + appealStatus + '\''
                + ", email='" + email + '\''
                + ", phoneNumber='" + phoneNumber + '\''
                + ", mobileNumber='" + mobileNumber + '\''
                + '}';
    }
}
