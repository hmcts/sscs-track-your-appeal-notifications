package uk.gov.hmcts.sscs.domain;

import static com.google.common.collect.Lists.newArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Address {
    private String line1;
    private String line2;
    private String town;
    private String county;
    private String postcode;

    public String getFullAddress() {
        return newArrayList(
                line1,
                line2,
                town,
                county,
                postcode)
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }
}
