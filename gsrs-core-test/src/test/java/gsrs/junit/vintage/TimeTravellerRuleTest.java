package gsrs.junit.vintage;

import gov.nih.ncats.common.util.TimeUtil;
import org.junit.After;
import org.junit.Test;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by katzelda on 10/13/16.
 */
public class TimeTravellerRuleTest {

    LocalDate nov_5_1955 = LocalDate.of(1955, 11, 5);

    @After
    public void resetTheClock(){
        TimeUtil.useSystemTime();
    }
    @Test
    public void beforeNullConstructorUsesSystemTime() throws Throwable{
        long systemTime = System.currentTimeMillis();
        TimeTravellerRule tt = new TimeTravellerRule();
        tt.before();

        long beforeTime = TimeUtil.getCurrentTimeMillis();
        long delta = beforeTime - systemTime;
        assertTrue(delta < 1000);
    }

    @Test
    public void beforeConstructorUsesSpecificDate() throws Throwable{
        TimeTravellerRule tt = new TimeTravellerRule(nov_5_1955);
        tt.before();

        assertEquals("before", nov_5_1955, TimeUtil.getCurrentLocalDate());

        tt.jumpAhead(1, TimeUnit.DAYS);

        tt.after();

        tt.before();

        assertEquals("after", nov_5_1955, TimeUtil.getCurrentLocalDate());
    }

    @Test
    public void jump(){
        TimeTravellerRule tt = new TimeTravellerRule();
        LocalDate whereWeWere = LocalDate.now();
        tt.jumpAhead(1, TimeUnit.DAYS);

        LocalDate whereWeAre = TimeUtil.getCurrentLocalDate();
        assertEquals(whereWeAre, whereWeWere.plusDays(1));
    }

    @Test
    public void jumpBack(){
        TimeTravellerRule tt = new TimeTravellerRule();
        LocalDate whereWeWere = LocalDate.now();
        tt.jumpBack(1, TimeUnit.DAYS);

        LocalDate whereWeAre = TimeUtil.getCurrentLocalDate();
        assertEquals(whereWeAre, whereWeWere.minusDays(1));
    }

    @Test
    public void travelTo(){
        TimeTravellerRule tt = new TimeTravellerRule();

        tt.travelTo(nov_5_1955);

        assertEquals(nov_5_1955, TimeUtil.getCurrentLocalDate());
    }
}
