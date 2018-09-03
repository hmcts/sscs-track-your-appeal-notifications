package uk.gov.hmcts.reform.sscs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;

@Value
@Builder
@JsonDeserialize(using = SscsCaseDataWrapperDeserializer.class)
public class SscsCaseDataWrapper {

    private SscsCaseData newSscsCaseData;
    private SscsCaseData oldSscsCaseData;

}
