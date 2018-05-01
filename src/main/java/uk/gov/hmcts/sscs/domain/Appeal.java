package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
public class Appeal {
    private MrnDetails mrnDetails;
    private Appellant appellant;
    private Benefit benefit;
    private HearingOptions hearingOptions;
    private AppealReasons appealReasons;
    private Representative rep;
    private String signer;

    public Appeal(MrnDetails mrnDetails,
                  Appellant appellant,
                  Benefit benefit,
                  HearingOptions hearingOptions,
                  AppealReasons appealReasons,
                  Representative rep,
                  String signer) {
        this.mrnDetails = mrnDetails;
        this.appellant = appellant;
        this.benefit = benefit;
        this.hearingOptions = hearingOptions;
        this.appealReasons = appealReasons;
        this.rep = rep;
        this.signer = signer;
    }
}
