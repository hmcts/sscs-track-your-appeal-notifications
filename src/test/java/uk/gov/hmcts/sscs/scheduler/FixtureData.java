package uk.gov.hmcts.sscs.scheduler;

import feign.FeignException;
import java.util.Collections;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.authorisation.exceptions.AbstractAuthorisationException;

public final class FixtureData {

    private FixtureData() {
        // empty constructor
    }

    public static AbstractAuthorisationException getAuthorisationException() {
        feign.Response feignResponse = feign.Response.create(
            HttpStatus.UNAUTHORIZED.value(),
            "i must fail",
            Collections.emptyMap(),
            new byte[0]
        );
        FeignException feignException = FeignException.errorStatus("oh no", feignResponse);

        return AbstractAuthorisationException.parseFeignException(feignException);
    }
}
