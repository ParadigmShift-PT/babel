package pt.unl.fct.di.novasys.babel.metrics.monitor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.messages.SendMetricsMessage;
import pt.unl.fct.di.novasys.babel.metrics.monitor.datalayer.Storage;
import pt.unl.fct.di.novasys.babel.metrics.monitor.timers.AggregationTimer;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionFailed;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionUp;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.babel.metrics.exporters.MonitorExporter;

/**
 * Protocol that receives metrics from other nodes (and itself), aggregates them and stores them using the provided Storage implementation.<br>
 * Messages are sent using messages from each node to the monitor node using {@link SendMetricsMessage}.
 * To send metric samples to this protocol, use the {@link MonitorExporter}
 * <p>
 * Properties:
 * <ul>
 *     <li> Monitor.aggregationTimer=12000 - The time in milliseconds between aggregations (default: 12000)</li>
 *</ul>
 */
public class SimpleMonitor extends Monitor {

    public final static String PROTOCOL_NAME = "MetricsMonitor";
	public final static short PROTOCOL_ID = 6666;

    private int channelId;

	//private Map<String, NodeSample> samplesPerNode = new HashMap<>();
	private Storage metricStorage;

	private long aggregationTimer; // Default aggregation timer in milliseconds

	private Host myself;
	private final Logger logger = LogManager.getLogger(SimpleMonitor.class);


    public SimpleMonitor(Host myself, Storage metricStorage) {
        super(SimpleMonitor.PROTOCOL_NAME, SimpleMonitor.PROTOCOL_ID);
        this.myself = myself;
		this.metricStorage = metricStorage;
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {

		this.aggregationTimer = Long.parseLong(props.getProperty("Monitor.aggregationTimer", "12000"));

        Properties channelProperties = new Properties();

        channelProperties.setProperty(TCPChannel.ADDRESS_KEY, this.myself.getAddress().getHostAddress());
        channelProperties.setProperty(TCPChannel.PORT_KEY, "" + (this.myself.getPort()));

        this.channelId = createChannel(TCPChannel.NAME, channelProperties);

        /*---------------------- Register Message Serializers ---------------------- */
        registerMessageSerializer(this.channelId, SendMetricsMessage.ID, SendMetricsMessage.serializer);
        /*---------------------- Register Message Handlers -------------------------- */
        registerMessageHandler(this.channelId, SendMetricsMessage.ID, this::uponReceiveSendMetricsMessage);

		registerTimerHandler(AggregationTimer.PROTO_ID, this::uponAggregationTimer);

        /*-------------------- Register Channel Event ------------------------------- */
		registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
		registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
		registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
		registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
		registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);

		setupPeriodicTimer(new AggregationTimer(), this.aggregationTimer, this.aggregationTimer);

    }

	/* Timers */

	private void uponAggregationTimer(AggregationTimer timer, long timerId) {
		try {
			//logger.info("Aggregating from {} nodes", this.samplesPerNode.size());
			Map<String, NodeSample> result = performAggregations();
			if(result.isEmpty()) {
				logger.debug("No samples aggregated!");
				return;
			}
			metricStorage.store(result);
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
	}


    /*--------------------------------- Messages ---------------------------------------- */

    private void uponReceiveSendMetricsMessage(SendMetricsMessage msg, Host from, short sourceProto, int channelId) {
		NodeSample sample = msg.getSample();
        addSampleToAggregate(from.toString(), sample);
	}

    /*
	 * --------------------------------- Channel Events ----------------------------
	 */
	private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
		logger.trace("Host {} is down, cause: {}", event.getNode(), event.getCause());
	}

	private void uponOutConnectionFailed(OutConnectionFailed<?> event, int channelId) {
		logger.trace("Connection to host {} failed, cause: {}", event.getNode(), event.getCause());
	}

	private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
		logger.trace("Host (out) {} is up", event.getNode());
	}

	private void uponInConnectionUp(InConnectionUp event, int channelId) {
		logger.trace("Host (in) {} is up", event.getNode());
	}

	private void uponInConnectionDown(InConnectionDown event, int channelId) {
		logger.trace("Connection from host {} is down, cause: {}", event.getNode(), event.getCause());
	}

}
