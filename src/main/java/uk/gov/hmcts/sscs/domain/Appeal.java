package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Appeal {
    private MrnDetails mrnDetails;
    private Appellant appellant;
    private BenefitType benefitType;
    private HearingOptions hearingOptions;
    private AppealReasons appealReasons;
    private Representative rep;
    private String signer;

    public Appeal(MrnDetails mrnDetails,
                  Appellant appellant,
                  BenefitType benefitType,
                  HearingOptions hearingOptions,
                  AppealReasons appealReasons,
                  Representative rep,
                  String signer) {
        this.mrnDetails = mrnDetails;
        this.appellant = appellant;
        this.benefitType = benefitType;
        this.hearingOptions = hearingOptions;
        this.appealReasons = appealReasons;
        this.rep = rep;
        this.signer = signer;
    }
}
