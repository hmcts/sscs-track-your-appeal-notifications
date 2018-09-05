package uk.gov.hmcts.reform.sscs.controller.test.smoke;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

@Controller
public class SmokeController {

    @Autowired
    private CcdClient ccdClient;

    @GetMapping("/smoke-test")
    @ResponseBody
    public List<SscsCaseDetails> smoke() {
        return ccdClient.findCaseBy(ImmutableMap.of("case.caseReference", "SC068/18/01217"));
    }

}
