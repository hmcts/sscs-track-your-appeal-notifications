package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MrnDetails {
    private String dwpIssuingOffice;
    private String mrnDate;
    private String mrnLateReason;
    private String mrnMissingReason;

    public MrnDetails(String dwpIssuingOffice,
                      String mrnDate,
                      String mrnLateReason,
                      String mrnMissingReason) {
        this.dwpIssuingOffice = dwpIssuingOffice;
        this.mrnDate = mrnDate;
        this.mrnLateReason = mrnLateReason;
        this.mrnMissingReason = mrnMissingReason;
    }
}
