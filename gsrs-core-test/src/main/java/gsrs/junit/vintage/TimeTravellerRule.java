package gsrs.junit.vintage;


import gov.nih.ncats.common.util.TimeUtil;
import org.junit.rules.ExternalResource;


import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A JUnit 4 {@link org.junit.Rule} that
 * can be used to change what
 * the date and time the Application
 * thinks it is.  Calling the
 * methods on this class will invoke
 * the corresponding
 * {@link TimeUtil} methods.  The
 * time is restored back to use system time
 * after each test.
 *
 * Example usage:
 *
 * <pre>
 *     @Rule
 *     TimeTravellerRule timeTraveller = new TimeTravellerRule();
 *
 *     ...
 *
 *     @Test
 *     public void myTest(){
 *
 *         timeTraveller.travelTo( 123456789L );
 *
 *         ...
 *     }
 *
 *
 * </pre>
 *
 * Time Travelling is not threadSafe.
 *
 * Created by katzelda on 3/24/16.
 */
public final class TimeTravellerRule extends ExternalResource {

   private gsrs.junit.TimeTraveller delegate;

    /**
     * Create a new instance that defaults
     * to use System time until one of the travelTo methods
     * is called.
     */
    public TimeTravellerRule(){
        delegate = new gsrs.junit.TimeTraveller();
    }

    /**
     * Creates a new instance that sets the
     * time to the given epoch time in milliseconds
     * before each test.
     *
     * @param timeMillis the epoch time to set; can be negativate
     *                   if the time is before epoch start time (1970).
     *
     * @throws IllegalArgumentException if timeMillis is negative.
     */
    public TimeTravellerRule(long timeMillis){

        delegate = new gsrs.junit.TimeTraveller(timeMillis);
    }
    /**
     * Creates a new instance that sets the
     * time to the given Date
     * before each test.
     *
     * @param date the epoch date to set to; can not be null
     *
     * @throws NullPointerException if date is null.
     */
    public TimeTravellerRule(Date date){

        delegate = new gsrs.junit.TimeTraveller(date);
    }

    /**
     * Creates a new instance that sets the
     * time to the midnight of the given local date.
     * before each test.
     *
     * @param date the epoch date to set to; can not be null
     *
     * @throws NullPointerException if date is null.
     */
    public TimeTravellerRule(LocalDate date){
        delegate = new gsrs.junit.TimeTraveller(date);
    }

    /**
     * Creates a new instance that sets the
     * time to the midnight of the given local date time.
     * before each test.
     *
     * @param dateTime the local date time with the default ZoneId; can not be null
     *
     * @throws NullPointerException if date is null.
     */
    public TimeTravellerRule(LocalDateTime dateTime){
        delegate = new gsrs.junit.TimeTraveller(dateTime);
    }

    @Override
    protected void before() throws Throwable {

        delegate.beforeEach(null);
    }


    public void travelTo(long timeMillis){

        delegate.travelTo(timeMillis);
    }
    /**
     * Travel to the given millisecond specified by the legacy
     * java.util.{@link Date} object.
     *
     * @param date the date to use; can not be null.
     */
    public void travelTo(Date date){

        delegate.travelTo(date);
    }

    /**
     * Travel to the Midnight of the given {@link LocalDate}.
     * To Also include a non-midnight time, use {@link #travelTo(LocalDateTime)}.
     *
     * @param date the date (year, month, day) to travel to, can not be null.
     *
     *  @see #travelTo(LocalDateTime)
     */
    public void travelTo(LocalDate date){
        delegate.travelTo(date);
    }
    public void travelTo(LocalDateTime datetime){

        delegate.travelTo(datetime);
    }


    /**
     * Stop the time at the current time, this is the same as
     * {@link #travelTo(long) travelTo( getCurrentTimeMillis()}.
     * this is useful when you don't really care what the current time is but want to make sure
     * time based actions happen at a time that can be asserted later.
     */
    public void freezeTime(){
        delegate.freezeTime();
    }
    /**
     * Jump to the current System time and
     * start using the normal system time until
     * another call to travelTo() or jump() is called.
     *
     * This is the same as TimeUtil.useSystemTime();
     */
    public void returnToSystemTime(){
        delegate.returnToSystemTime();
    }

    /**
     * Jump in time forward or backward from the
     * @param amount the number of units to jump ahead (positive) or back (negative)
     * @param units the TimeUnit, can not be null.
     */
    public void jump(long amount, TimeUnit units){
        delegate.jump(amount, units);
    }
    public Optional<Date> getWhereWeWereDate(){
       return delegate.getWhereWeWereDate();
    }

    public long getCurrentTimeMillis(){
        return delegate.getCurrentTimeMillis();
    }
    public LocalDate getCurrentLocalDate(){
        return delegate.getCurrentLocalDate();
    }
    public LocalDateTime getCurrentLocalDateTime(){
        return delegate.getCurrentLocalDateTime();
    }
    public Date getCurrentDate(){
        return delegate.getCurrentDate();
    }
    /**
     * Jump in time forward
     * @param amount the number of units to jump ahead, must be positive.
     * @param units the TimeUnit, can not be null.
     */
    public void jumpAhead(long amount, TimeUnit units){
      delegate.jumpAhead(amount, units);
    }

    /**
     * Jump in time forward by the amount of {@link ChronoUnit}s.  For example
     * to jump ahead an estimated 8 months use {@code jumpAhead(8, ChronoUnit.MONTHS)}.
     *
     * @param amount the number of units to jump ahead, must be positive.
     * @param units the ChronoUnit, can not be null.
     *
     *
     */
    public void jumpAhead(long amount, ChronoUnit units){
        delegate.jumpAhead(amount, units);
    }

    /**
     * Jump in time forward
     * @param duration the {@link Duration} to jump ahead, must be positive.
     */
    public void jumpAhead(Duration duration){

        delegate.jumpAhead(duration);
    }
    /**
    * Jump in time backward by the amount of {@link ChronoUnit}s.  For example
    * to jump back an estimated 8 months use {@code jumpBack(8, ChronoUnit.MONTHS)}.
    *
    * @param amount the number of units to jump ahead, must be positive.
    * @param units the ChronoUnit, can not be null.
    *
    *
    */
    public void jumpBack(long amount, ChronoUnit units){
      delegate.jumpBack(amount, units);
    }

    /**
     * Jump in time forward
     * @param duration the {@link Duration} to jump ahead, must be positive.
     */
    public void jumpBack(Duration duration){
        delegate.jumpBack(duration);
    }



    /**
     * Jump in time backward
     * @param amount the number of units to jump back, must be positive.
     * @param units the TimeUnit, can not be null.
     */
    public void jumpBack(long amount, TimeUnit units){
        delegate.jumpBack(amount, units);
    }

    @Override
    protected void after() {
       delegate.afterEach(null);
    }
}
