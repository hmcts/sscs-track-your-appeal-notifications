package uk.gov.hmcts.sscs.domain;

import java.util.List;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public class CcdResponse {

    private String caseReference;
    private Subscription appellantSubscription;
    private Subscription supporterSubscription;
    private EventType notificationType;
    private List<Event> events;

    public CcdResponse() {
        //
    }

    public CcdResponse(String caseReference, Subscription appellantSubscription, Subscription supporterSubscription,
                       EventType notificationType) {
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

    public EventType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(EventType notificationType) {
        this.notificationType = notificationType;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return "CcdResponse{"
                + " caseReference='" + caseReference + '\''
                + ", appellantSubscription=" + appellantSubscription
                + ", supporterSubscription=" + supporterSubscription
                + ", notificationType=" + notificationType
                + ", events=" + events
                + '}';
    }
}
