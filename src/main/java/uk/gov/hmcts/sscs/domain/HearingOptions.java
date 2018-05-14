package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HearingOptions {
    private String wantsToAttend;
    private String wantsSupport;
    private String languageInterpreter;
    private String languages;
    private List<String> arrangements;
    private String scheduleHearing;
    private List<ExcludeDate> excludeDates;
    private String other;

    public HearingOptions(String wantsToAttend,
                          String wantsSupport,
                          String languageInterpreter,
                          String languages,
                          List<String> arrangements,
                          String scheduleHearing,
                          List<ExcludeDate> excludeDates,
                          String other) {
        this.wantsToAttend = wantsToAttend;
        this.wantsSupport = wantsSupport;
        this.languageInterpreter = languageInterpreter;
        this.languages = languages;
        this.arrangements = arrangements;
        this.scheduleHearing = scheduleHearing;
        this.excludeDates = excludeDates;
        this.other = other;
    }
}
