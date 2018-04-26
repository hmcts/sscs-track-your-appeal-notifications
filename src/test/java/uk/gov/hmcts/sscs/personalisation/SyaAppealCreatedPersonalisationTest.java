package uk.gov.hmcts.sscs.personalisation;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.EVIDENCE_RECEIVED_DATE_LITERAL;
import static uk.gov.hmcts.sscs.config.AppConstants.MRN_DETAILS_LITERAL;
import static uk.gov.hmcts.sscs.domain.Benefit.PIP;
import static uk.gov.hmcts.sscs.domain.notify.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.sscs.domain.notify.EventType.SYA_APPEAL_CREATED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Events;
import uk.gov.hmcts.sscs.domain.notify.Event;

public class SyaAppealCreatedPersonalisationTest {

    private static final String CASE_ID = "54321";

    CcdResponse response;

    @InjectMocks
    @Resource
    SyaAppealCreatedPersonalisation personalisation;

    @Before
    public void setup() {
        initMocks(this);

        response = CcdResponse.builder()
            .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
            .notificationType(SYA_APPEAL_CREATED)
            .build();
    }

    @Test
    public void givenASyaAppealCreated_setMrnDetailsForTemplate() {
        Map<String, String> result = personalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 3 November 2017\n" +
                "\nReason for late appeal: My train was cancelled.\n" +
                "\nReason for no MRN: My dog ate my homework.",
                result.get(MRN_DETAILS_LITERAL));
    }
}
