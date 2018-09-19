package uk.gov.hmcts.reform.sscs.service;

import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

@Service
public class DateTimeProvider {
    public ZonedDateTime now() {
        return ZonedDateTime.now();
    }
}
