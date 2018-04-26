package uk.gov.hmcts.sscs.personalisation;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;

@Component
public class SyaAppealCreatedPersonalisation extends Personalisation {

    @Override
    public Map<String, String> create(CcdResponseWrapper responseWrapper) {
        Map<String, String> personalisation = super.create(responseWrapper);
        CcdResponse ccdResponse = responseWrapper.getNewCcdResponse();

        setMrnDetails(personalisation, ccdResponse);
        setYourDetails(personalisation, ccdResponse);
        setTextMessageReminderDetails(personalisation, ccdResponse);
        setRepresentativeDetails(personalisation, ccdResponse);
        setReasonsForAppealingDetails(personalisation, ccdResponse);
        setHearingDetails(personalisation, ccdResponse);
        setHearingArrangementDetails(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, String> setMrnDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        return personalisation;
    }

    public Map<String, String> setYourDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        return personalisation;

    }

    public Map<String, String> setTextMessageReminderDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        return personalisation;
    }

    public Map<String, String> setRepresentativeDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        return personalisation;
    }

    public Map<String, String> setReasonsForAppealingDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        return personalisation;
    }

    public Map<String, String> setHearingDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        return personalisation;
    }

    public Map<String, String> setHearingArrangementDetails(Map<String, String> personalisation, CcdResponse ccdResponse) {
        return personalisation;
    }

}
