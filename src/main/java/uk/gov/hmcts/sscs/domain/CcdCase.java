package uk.gov.hmcts.sscs.domain;

public class CcdCase {

    //TODO: Work out what fields are returned by CCD and make sure they are mapped here correctly

    private Long appealCaseId;
    private String caseReference;
    private String appealNumber;
    private String name;
    private String appealStatus;

    public Long getAppealCaseId() {
        return appealCaseId;
    }

    public void setAppealCaseId(Long appealCaseId) {
        this.appealCaseId = appealCaseId;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public String getAppealNumber() {
        return appealNumber;
    }

    public void setAppealNumber(String appealNumber) {
        this.appealNumber = appealNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppealStatus() {
        return appealStatus;
    }

    public void setAppealStatus(String appealStatus) {
        this.appealStatus = appealStatus;
    }

    @Override
    public String toString() {
        return "CcdCase{"
                + " appealCaseId=" + appealCaseId
                + ", caseReference='" + caseReference + '\''
                + ", appealNumber='" + appealNumber + '\''
                + ", name='" + name + '\''
                + ", appealStatus='" + appealStatus + '\''
                + '}';
    }
}
