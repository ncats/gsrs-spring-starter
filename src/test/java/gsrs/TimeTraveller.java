package gsrs;

import gov.nih.ncats.common.util.TimeUtil;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A JUnit 5 Extension that
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
 *    @RegisterExtension
 *    TimeTraveller timeTraveller = new TimeTraveller(LocalDate.of(1955, 11, 05));
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
public final class TimeTraveller implements BeforeEachCallback, AfterEachCallback {

    /**
     * initial time we set to at constructor time
     * and jump to each call to {@link #beforeEach(ExtensionContext)} ()}.
     */
    private Long initialTime;
    /**
     * This is where we are.
     */
    private Long whereWeAre;
    /**
     * This is where we were; the last place we were before the current jump.
     */
    private Long whereWeWere;
    boolean hasJumped;

    /**
     * Create a new instance that defaults
     * to use System time until one of the travelTo methods
     * is called.
     */
    public TimeTraveller(){
        initialTime = null;
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
    public TimeTraveller(long timeMillis){

        this.initialTime = timeMillis;
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
    public TimeTraveller(Date date){
        this(date.getTime());
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
    public TimeTraveller(LocalDate date){
        this(LocalDateTime.of(date, LocalTime.MIDNIGHT));
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
    public TimeTraveller(LocalDateTime dateTime){
        this(TimeUtil.toMillis(dateTime));
    }


    private void travelTo(long timeMillis, boolean inInitializer){
        whereWeWere = whereWeAre ==null ? TimeUtil.getCurrentTimeMillis() : whereWeAre;
        whereWeAre = timeMillis;
        TimeUtil.setCurrentTime(timeMillis);
        hasJumped = !inInitializer;
    }
    public void travelTo(long timeMillis){
        travelTo(timeMillis, false);
    }
    /**
     * Travel to the given millisecond specified by the legacy
     * java.util.{@link Date} object.
     *
     * @param date the date to use; can not be null.
     */
    public void travelTo(Date date){
        TimeUtil.setCurrentTime(date);
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
       travelTo(LocalDateTime.of(date, LocalTime.MIDNIGHT));
    }
    public void travelTo(LocalDateTime datetime){
        travelTo(TimeUtil.toMillis(datetime));
    }


    /**
     * Stop the time at the current time, this is the same as
     * {@link #travelTo(long) travelTo( getCurrentTimeMillis()}.
     * this is useful when you don't really care what the current time is but want to make sure
     * time based actions happen at a time that can be asserted later.
     */
    public long freezeTime(){
        long millis = getCurrentTimeMillis();
        travelTo(millis);
        return millis;
    }
    /**
     * Jump to the current System time and
     * start using the normal system time until
     * another call to travelTo() or jump() is called.
     *
     * This is the same as TimeUtil.useSystemTime();
     */
    public void returnToSystemTime(){
        whereWeWere = whereWeAre;
        whereWeAre=null;
        TimeUtil.useSystemTime();
    }

    /**
     * Jump in time forward or backward from the
     * @param amount the number of units to jump ahead (positive) or back (negative)
     * @param units the TimeUnit, can not be null.
     */
    public void jump(long amount, TimeUnit units){
        long delta = units.toMillis(amount);
        long newTime = TimeUtil.getCurrentTimeMillis() + delta;

        travelTo(newTime);
    }
    public Optional<Date> getWhereWeWereDate(){
        if(hasJumped) {
            return Optional.of( new Date(whereWeWere));
        }
        return Optional.empty();
    }

    public long getCurrentTimeMillis(){
        return TimeUtil.getCurrentTimeMillis();
    }
    public LocalDate getCurrentLocalDate(){
        return TimeUtil.getCurrentLocalDate();
    }
    public LocalDateTime getCurrentLocalDateTime(){
        return TimeUtil.getCurrentLocalDateTime();
    }
    public Date getCurrentDate(){
        return TimeUtil.getCurrentDate();
    }
    /**
     * Jump in time forward
     * @param amount the number of units to jump ahead, must be positive.
     * @param units the TimeUnit, can not be null.
     */
    public void jumpAhead(long amount, TimeUnit units){
        if(amount < 0){
            throw new IllegalArgumentException("amount must be >= 0 : " + amount);
        }
       jump(amount, units);
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
        if(amount < 0){
            throw new IllegalArgumentException("amount must be >= 0 : " + amount);
        }
        jumpAhead(units.getDuration().multipliedBy(amount));
    }

    /**
     * Jump in time forward
     * @param duration the {@link Duration} to jump ahead, must be positive.
     */
    public void jumpAhead(Duration duration){

        jump(duration.toMillis(), TimeUnit.MILLISECONDS);
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
        if(amount < 0){
            throw new IllegalArgumentException("amount must be >= 0 : " + amount);
        }
        jumpBack(units.getDuration().multipliedBy(amount));
    }

    /**
     * Jump in time forward
     * @param duration the {@link Duration} to jump ahead, must be positive.
     */
    public void jumpBack(Duration duration){

        jump(-duration.toMillis(), TimeUnit.MILLISECONDS);
    }



    /**
     * Jump in time backward
     * @param amount the number of units to jump back, must be positive.
     * @param units the TimeUnit, can not be null.
     */
    public void jumpBack(long amount, TimeUnit units){
        if(amount < 0){
            throw new IllegalArgumentException("amount must be >= 0 : " + amount);
        }
        jump(-amount, units);
    }



    @Override
    public void afterEach(ExtensionContext extensionContext){
        TimeUtil.useSystemTime();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext){
        if(initialTime !=null){
            travelTo(initialTime, true);
        }
    }
}
