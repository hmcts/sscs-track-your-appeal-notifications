package uk.gov.hmcts.reform.sscs.service.tca;

import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "Tca", url = "${tca.url}", decode404 = true)
public interface TcaClient {
    @RequestMapping(method = RequestMethod.POST, value = "/actions")
    void performAction(
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestBody Map<String, Object> sscsCaseDataWrapper
    );

}
