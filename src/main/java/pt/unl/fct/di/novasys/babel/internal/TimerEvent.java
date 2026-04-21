package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

import java.util.Comparator;

/**
 * An internal event representing a scheduled timer, carrying the {@link ProtoTimer} payload
 * and the scheduling metadata needed to fire and optionally re-schedule it.
 * Implements {@link Comparable} and {@link Comparator} so timer queues can order events
 * by their next trigger time.
 */
public class TimerEvent extends InternalEvent implements Comparable<TimerEvent>, Comparator<TimerEvent> {

    private final long uuid;
    private final ProtoTimer timer;

    private final GenericProtocol consumer;
    private final boolean periodic;
    private final long period;

    private long triggerTime;

    /**
     * Constructs a TimerEvent for the given timer and scheduling parameters.
     *
     * @param timer       the timer payload to deliver when the event fires
     * @param uuid        unique identifier assigned to this timer registration
     * @param consumer    the protocol instance that registered and will receive this timer
     * @param triggerTime absolute time (ms) at which the timer should next fire
     * @param periodic    {@code true} if the timer should repeat at the given {@code period}
     * @param period      repeat interval in milliseconds; ignored when {@code periodic} is {@code false}
     */
    public TimerEvent(ProtoTimer timer, long uuid, GenericProtocol consumer, long triggerTime, boolean periodic,
                      long period) {
        super(EventType.TIMER_EVENT);
        this.timer = timer;
        this.uuid = uuid;
        this.consumer = consumer;
        this.triggerTime = triggerTime;
        this.period = period;
        this.periodic = periodic;
    }

    /**
     * Returns the timer payload to be delivered to the consumer protocol.
     *
     * @return the protocol timer
     */
    public ProtoTimer getTimer() {
        return timer;
    }

    @Override
    public String toString() {
        return "TimerEvent{" +
                "uuid=" + uuid +
                ", timer=" + timer +
                ", consumer=" + consumer +
                ", triggerTime=" + triggerTime +
                ", periodic=" + periodic +
                ", period=" + period +
                '}';
    }

    /**
     * Returns the unique identifier assigned to this timer registration.
     *
     * @return timer UUID
     */
    public long getUuid() {
        return uuid;
    }

    /**
     * Returns the repeat interval in milliseconds for periodic timers.
     *
     * @return period in milliseconds
     */
    public long getPeriod() {
        return period;
    }

    /**
     * Returns {@code true} if this timer fires repeatedly at the configured period.
     *
     * @return {@code true} for periodic timers, {@code false} for one-shot timers
     */
    public boolean isPeriodic() {
        return periodic;
    }

    /**
     * Returns the absolute time (ms) at which this timer is scheduled to next fire.
     *
     * @return trigger time in milliseconds
     */
    public long getTriggerTime() {
        return triggerTime;
    }

    /**
     * Returns the protocol instance that registered and will receive this timer.
     *
     * @return the consumer protocol
     */
    public GenericProtocol getConsumer() {
        return consumer;
    }

    /**
     * Updates the absolute trigger time, used to reschedule a periodic timer after it fires.
     *
     * @param triggerTime new trigger time in milliseconds
     */
    public void setTriggerTime(long triggerTime) {
        this.triggerTime = triggerTime;
    }

    @Override
    public int compareTo(TimerEvent o) {
        return Long.compare(this.triggerTime, o.triggerTime);
    }

    @Override
    public int compare(TimerEvent o1, TimerEvent o2) {
        return Long.compare(o1.triggerTime, o2.triggerTime);
    }

}
