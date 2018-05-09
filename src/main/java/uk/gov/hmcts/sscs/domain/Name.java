package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Name {
    private String title;
    private String firstName;
    private String lastName;

    public Name(String title,
                String firstName,
                String lastName) {
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @JsonIgnore
    public String getFullName() {
        return title + " " + firstName + " " + lastName;
    }

    @JsonIgnore
    public String getFullNameNoTitle() {
        return firstName + " " + lastName;
    }
}
