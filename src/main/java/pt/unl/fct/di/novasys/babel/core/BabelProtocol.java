package pt.unl.fct.di.novasys.babel.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class BabelProtocol {
    private final ScheduledExecutorService executor;

    public BabelProtocol() {
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }
}
