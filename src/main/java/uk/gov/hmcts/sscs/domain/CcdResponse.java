package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.sscs.deserialize.CcdResponseDeserializer;
import uk.gov.hmcts.sscs.domain.notify.NotificationType;

@JsonDeserialize(using = CcdResponseDeserializer.class)
public class CcdResponse {

    private String caseReference;
    private Subscription appellantSubscription;
    private Subscription supporterSubscription;
    private NotificationType notificationType;

    public CcdResponse() {
        //
    }

    public CcdResponse(String caseReference, Subscription appellantSubscription, Subscription supporterSubscription,
                       NotificationType notificationType) {
        this.caseReference = caseReference;
        this.appellantSubscription = appellantSubscription;
        this.supporterSubscription = supporterSubscription;
        this.notificationType = notificationType;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public Subscription getAppellantSubscription() {
        return appellantSubscription;
    }

    public void setAppellantSubscription(Subscription appellantSubscription) {
        this.appellantSubscription = appellantSubscription;
    }

    public Subscription getSupporterSubscription() {
        return supporterSubscription;
    }

    public void setSupporterSubscription(Subscription supporterSubscription) {
        this.supporterSubscription = supporterSubscription;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    @Override
    public String toString() {
        return "CcdResponse{"
                + " caseReference='" + caseReference + '\''
                + ", appellantSubscription=" + appellantSubscription
                + ", supporterSubscription=" + supporterSubscription
                + ", notificationType=" + notificationType
                + '}';
    }
}
