package uk.gov.hmcts.sscs.domain;

import java.util.List;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public class CcdResponse {

    private String caseId;
    private Benefit benefitType;
    private String caseReference;
    private Subscription appellantSubscription;
    private Subscription supporterSubscription;
    private EventType notificationType;
    private List<Event> events;
    private List<Hearing> hearings;
    private List<Evidence> evidences;

    public CcdResponse() {
        //
    }

    public CcdResponse(String caseId,
                       Benefit benefitType,
                       String caseReference,
                       Subscription appellantSubscription,
                       Subscription supporterSubscription,
                       EventType notificationType,
                       List<Hearing> hearings,
                       List<Evidence> evidences) {
        this.caseId = caseId;
        this.benefitType = benefitType;
        this.caseReference = caseReference;
        this.appellantSubscription = appellantSubscription;
        this.supporterSubscription = supporterSubscription;
        this.notificationType = notificationType;
        this.hearings = hearings;
        this.evidences = evidences;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Benefit getBenefitType() {
        return benefitType;
    }

    public void setBenefitType(Benefit benefitType) {
        this.benefitType = benefitType;
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

    public List<Hearing> getHearings() {
        return hearings;
    }

    public void setHearings(List<Hearing> hearings) {
        this.hearings = hearings;
    }

    public List<Evidence> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<Evidence> evidences) {
        this.evidences = evidences;
    }

    @Override
    public String toString() {
        return "CcdResponse{"
                + " benefitType='" + benefitType + '\''
                + ", caseReference='" + caseReference + '\''
                + ", appellantSubscription=" + appellantSubscription
                + ", supporterSubscription=" + supporterSubscription
                + ", notificationType=" + notificationType
                + ", events=" + events
                + ", hearings=" + hearings
                + ", evidences=" + evidences
                + '}';
    }
}
