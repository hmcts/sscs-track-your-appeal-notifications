package uk.gov.hmcts.reform.sscs.service.tca;

import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "Tca", url = "${tca.url}", decode404 = true)
public interface TcaClient {
    @PostMapping(value = "/actions")
    void performAction(
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestBody Map<String, Object> sscsCaseDataWrapper
    );

}
