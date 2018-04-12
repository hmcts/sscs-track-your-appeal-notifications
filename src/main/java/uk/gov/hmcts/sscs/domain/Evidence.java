package uk.gov.hmcts.sscs.domain;

import java.time.LocalDate;

public class Evidence implements Comparable<Evidence> {

    private LocalDate dateReceived;
    private String evidenceType;
    private String evidenceProvidedBy;

    public Evidence() {
    }

    public Evidence(LocalDate dateReceived, String evidenceType, String evidenceProvidedBy) {
        this.dateReceived = dateReceived;
        this.evidenceType = evidenceType;
        this.evidenceProvidedBy = evidenceProvidedBy;
    }

    public LocalDate getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(LocalDate dateReceived) {
        this.dateReceived = dateReceived;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getEvidenceProvidedBy() {
        return evidenceProvidedBy;
    }

    public void setEvidenceProvidedBy(String evidenceProvidedBy) {
        this.evidenceProvidedBy = evidenceProvidedBy;
    }

    @Override
    public String toString() {
        return "Evidence{"
                + " dateReceived=" + dateReceived
                + ", evidenceType='" + evidenceType + '\''
                + ", evidenceProvidedBy='" + evidenceProvidedBy + '\''
                + '}';
    }

    @Override
    public int compareTo(Evidence o) {
        return getDateReceived().compareTo(o.getDateReceived());
    }
}
