package uk.gov.hmcts.reform.sscs.controller.test.smoke;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Controller
public class SmokeController {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private IdamService idamService;

    @GetMapping("/smoke-test")
    @ResponseBody
    public List<SscsCaseDetails> smoke() {
        return ccdService.findCaseBy(ImmutableMap.of("case.caseReference", "SC068/18/01217"), idamService.getIdamTokens());
    }

}
