package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.sscs.deserialize.CcdResponseDeserializer;
import uk.gov.hmcts.sscs.domain.notify.Destination;
import uk.gov.hmcts.sscs.domain.notify.NotificationType;

@JsonDeserialize(using = CcdResponseDeserializer.class)
public class CcdResponse {

    private String appellantFirstName;
    private String appellantSurname;
    private String appellantTitle;
    private String appealNumber;
    private String caseReference;
    private String email;
    private String mobileNumber;
    private NotificationType notificationType;


    public CcdResponse() {
        //
    }

    public CcdResponse(String appellantFirstName, String appellantSurname, String appellantTitle, String appealNumber,
                       String caseReference, String email, String mobileNumber, NotificationType notificationType) {
        this.appellantFirstName = appellantFirstName;
        this.appellantSurname = appellantSurname;
        this.appellantTitle = appellantTitle;
        this.appealNumber = appealNumber;
        this.caseReference = caseReference;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.notificationType = notificationType;
    }

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

    public String getAppellantTitle() {
        return appellantTitle;
    }

    public void setAppellantTitle(String appellantTitle) {
        this.appellantTitle = appellantTitle;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public Destination getDestination() {
        return new Destination(email, mobileNumber);
    }

    @Override
    public String toString() {
        return "CcdResponse{"
                + " appellantFirstName='" + appellantFirstName + '\''
                + ", appellantSurname='" + appellantSurname + '\''
                + ", appellantTitle='" + appellantTitle + '\''
                + ", appealNumber='" + appealNumber + '\''
                + ", caseReference='" + caseReference + '\''
                + ", notificationType='" + notificationType + '\''
                + ", email='" + email + '\''
                + ", mobileNumber='" + mobileNumber + '\''
                + '}';
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }
}
