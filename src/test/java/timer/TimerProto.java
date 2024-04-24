package timer;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.metrics.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class TimerProto extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(TimerProto.class);

    private Instant instantLogger;

    public TimerProto() throws HandlerRegistrationException, IOException{
        super("TimerTest", (short) 100);
        instantLogger = new Instant("Time");
        registerMetric(instantLogger);

        registerTimerHandler(TimerTimer.TIMER_ID, this::handleTimerTimer);
    }
    
    @Override
    public void start() {
        setupPeriodicTimer(new TimerTimer(), 1000, 300);
    }

    private void handleTimerTimer(TimerTimer timer, long timerId) {
        instantLogger.log(System.currentTimeMillis());
    }
}
