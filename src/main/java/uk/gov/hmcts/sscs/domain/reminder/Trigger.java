package uk.gov.hmcts.sscs.domain.reminder;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Trigger {

    //CHECKSTYLE.OFF: MemberName
    private String start_date_time;
    //CHECKSTYLE.ON: MemberName

    public Trigger(ZonedDateTime dateTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        this.start_date_time = dateTime.format(dateFormatter) + "T" + dateTime.format(timeFormatter) + dateTime.getOffset();
    }

    public String getStart_date_time() {
        return start_date_time;
    }
}