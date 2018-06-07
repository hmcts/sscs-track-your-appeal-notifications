package uk.gov.hmcts.sscs.service;

import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class ZonedDateTimeNowProvider {

    public ZonedDateTime now() {
        return ZonedDateTime.now();
    }

}
