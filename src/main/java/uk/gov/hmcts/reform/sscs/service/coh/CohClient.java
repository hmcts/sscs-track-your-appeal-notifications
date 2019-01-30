package uk.gov.hmcts.reform.sscs.service.coh;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "Coh", url = "${coh.url}", decode404 = true)
public interface CohClient {
    @GetMapping(value = "/continuous-online-hearings/{onlineHearingId}/questionrounds")
    QuestionRounds getQuestionRounds(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @PathVariable("onlineHearingId") String onlineHearingId
    );

}
