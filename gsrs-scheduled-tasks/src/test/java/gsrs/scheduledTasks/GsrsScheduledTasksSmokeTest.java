package gsrs.scheduledTasks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GsrsScheduledTasksSmokeTest {

    @Test
    void builderCreatesExpectedEveryFiveMinutesExpression() {
        String cron = CronExpressionBuilder.builder()
                .every(5)
                .minutes()
                .build();

        assertEquals("0 0/5 * * * ?", cron);
    }

    @Test
    void fromRoundTripsCronExpression() {
        String cron = "0 15 10 ? * 2#1";

        assertEquals(cron, CronExpressionBuilder.from(cron).build());
    }
}
