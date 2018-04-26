package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Appeal {
    private String mrnDate;
    private String mrnLateReason;
    private String mrnMissingReason;
    private Appellant appellant;
    private Benefit benefit;
    private HearingOptions hearingOptions;
    private AppealReasons appealReasons;
    private Representative rep;
    private String signer;

    public Appeal(String mrnDate,
                  String mrnLateReason,
                  String mrnMissingReason,
                  Appellant appellant,
                  Benefit benefit,
                  HearingOptions hearingOptions,
                  AppealReasons appealReasons,
                  Representative rep,
                  String signer) {
        this.mrnDate = mrnDate;
        this.mrnLateReason = mrnLateReason;
        this.mrnMissingReason = mrnMissingReason;
        this.appellant = appellant;
        this.benefit = benefit;
        this.hearingOptions = hearingOptions;
        this.appealReasons = appealReasons;
        this.rep = rep;
        this.signer = signer;
    }
}
