package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    public Appeal(@JsonProperty("mrnDate") String mrnDate,
                  @JsonProperty("mrnLateReason") String mrnLateReason,
                  @JsonProperty("mrnMissingReason") String mrnMissingReason,
                  @JsonProperty("appellant") Appellant appellant,
                  @JsonProperty("benefitType") Benefit benefit,
                  @JsonProperty("hearingOptions") HearingOptions hearingOptions,
                  @JsonProperty("appealReasons") AppealReasons appealReasons,
                  @JsonProperty("rep") Representative rep,
                  @JsonProperty("signer") String signer) {
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
