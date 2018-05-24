package uk.gov.hmcts.sscs.service.ccd;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.exception.CcdUtilException;

public class CcdUtil {

    private static final Logger LOG = getLogger(CcdUtil.class);

    private CcdUtil() {

    }

    public static CcdResponse getCcdResponse(Object object) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        try {
            return mapper.convertValue(object, CcdResponse.class);
        } catch (Exception e) {
            CcdUtilException ccdUtilException = new CcdUtilException(e);
            LOG.error("Error occurred when CaseDetails are mapped into CcdResponse", ccdUtilException);
            throw ccdUtilException;
        }
    }
}
