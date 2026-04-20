package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

/**
 * Functional interface for handling protocol timers.
 *
 * <p>Registered via {@code GenericProtocol.registerTimerHandler}. Invoked on this
 * protocol's thread when a timer of type {@code T} fires. The {@code uId} uniquely
 * identifies the specific timer instance, which is useful for cancelling repeating
 * timers via {@code GenericProtocol.cancelTimer}.
 *
 * @param <T> the concrete timer type
 */
@FunctionalInterface
public interface TimerHandler<T extends ProtoTimer> {

    /**
     * Called when a timer of type {@code T} fires.
     *
     * @param timer the timer that fired
     * @param uId   the unique id of this timer instance (as returned by {@code setupTimer})
     */
    void uponTimer(T timer, long uId);

}
