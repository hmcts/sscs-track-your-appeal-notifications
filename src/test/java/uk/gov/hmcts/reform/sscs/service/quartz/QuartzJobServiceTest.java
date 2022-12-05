package uk.gov.hmcts.reform.sscs.service.quartz;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import uk.gov.hmcts.reform.sscs.exception.JobException;

@ExtendWith(MockitoExtension.class)
class QuartzJobServiceTest {

    @Mock
    private Scheduler scheduler;
    @InjectMocks
    private QuartzJobService quartzJobService;

    @Test
    void starts_quartz() throws SchedulerException {

        assertThatNoException().isThrownBy(quartzJobService::start);

        verify(scheduler, times(1)).start();
    }

    @Test
    void stops_quartz_with_waiting() throws SchedulerException {

        assertThatNoException().isThrownBy(() -> quartzJobService.stop(true));

        verify(scheduler, times(1)).shutdown(true);
    }

    @Test
    void stops_quartz_without_waiting() throws SchedulerException {

        assertThatNoException().isThrownBy(() -> quartzJobService.stop(false));

        verify(scheduler, times(1)).shutdown(false);
    }

    @Test
    void start_throws_when_quartz_fails() throws SchedulerException {

        doThrow(SchedulerException.class)
            .when(scheduler)
            .start();

        assertThatThrownBy(quartzJobService::start)
            .hasMessage("Cannot start Quartz job scheduler")
            .isExactlyInstanceOf(JobException.class);
    }

    @Test
    void stop_throws_when_quartz_fails() throws SchedulerException {
        doThrow(SchedulerException.class)
            .when(scheduler)
            .shutdown(true);

        assertThatThrownBy(() -> quartzJobService.stop(true))
            .hasMessage("Cannot stop Quartz job scheduler")
            .isExactlyInstanceOf(JobException.class);
    }

}
