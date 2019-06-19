package uk.gov.hmcts.reform.sscs.domain.docmosis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PdfCoverSheet {
    @JsonProperty("case_id")
    private final String caseId;
    @JsonProperty("address_line1")
    private final String addressLine1;
    @JsonProperty("address_line2")
    private final String addressLine2;
    @JsonProperty("address_town")
    private final String addressTown;
    @JsonProperty("address_county")
    private final String addressCounty;
    @JsonProperty("address_postcode")
    private final String addressPostcode;

    public PdfCoverSheet(String caseId,
                         String addressLine1,
                         String addressLine2,
                         String addressTown,
                         String addressCounty,
                         String addressPostcode) {
        this.caseId = caseId;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressTown = addressTown;
        this.addressCounty = addressCounty;
        this.addressPostcode = addressPostcode;
    }

}
