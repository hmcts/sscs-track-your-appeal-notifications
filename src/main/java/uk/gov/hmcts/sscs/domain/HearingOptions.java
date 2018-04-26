package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HearingOptions {
    private String languageInterpreter;
    private String languages;
    private List<String> arrangements;
    private List<ExcludeDate> excludeDates;
    private String other;

    @JsonCreator
    public HearingOptions(String languageInterpreter,
                          String languages,
                          List<String> arrangements,
                          List<ExcludeDate> excludeDates,
                          String other) {
        this.languageInterpreter = languageInterpreter;
        this.languages = languages;
        this.arrangements = arrangements;
        this.excludeDates = excludeDates;
        this.other = other;
    }
}
