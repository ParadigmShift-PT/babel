package pt.unl.fct.di.novasys.babel.metrics.exporters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.exporters.timers.ExportMetricsTimer;
import pt.unl.fct.di.novasys.babel.metrics.messages.SendMetricsMessage;
import pt.unl.fct.di.novasys.babel.metrics.monitor.SimpleMonitor;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.util.Properties;

/**
 * Exporter protocol that periodically exports metrics to a monitor node.
 * This protocol can be used to send metrics from a node to another protocol capable of receiving a {@link SendMetricsMessage} (normally a {@link SimpleMonitor}).
 */
public class MonitorExporter extends ProtocolExporter {
	public static final short PROTO_ID = 406;

	public static final String PROTO_NAME = "MonitorExporter";

	private Logger logger = LogManager.getLogger(MonitorExporter.class);

	private long exportPeriod;

//	private ProtocolExporter pe;
	private long exportMetricsTimerID;

	private Host myself;
	private Host monitor;

	/**
	 * Creates a new {@code MonitorExporter} that periodically sends metric snapshots to the given monitor host.
	 *
	 * @param myself       the local host address and port this protocol binds to
	 * @param monitor      the remote monitor host to which metrics are sent
	 * @param exportPeriod interval in milliseconds between successive metric exports
	 * @param eco          collect options controlling which protocols and metrics are gathered
	 */
	public MonitorExporter(Host myself, Host monitor, long exportPeriod, ExporterCollectOptions eco) {
		super(PROTO_NAME, PROTO_ID, eco);

		this.myself = myself;
		this.monitor = monitor;
		this.exportPeriod = exportPeriod;
		//this.pe = new ProtocolExporter.Builder("MonitorExporter").exporterCollectOptions(eco).build();
	}

	/**
	 * Initialises the TCP channel, registers message serializers and channel event handlers,
	 * schedules the periodic export timer, and opens the outbound connection to the monitor.
	 *
	 * @param props runtime properties passed by the Babel runtime (unused directly)
	 * @throws HandlerRegistrationException if a handler for an already-registered event or timer is re-registered
	 * @throws IOException                  if the TCP channel cannot be created
	 */
	@Override
	public void init(Properties props) throws HandlerRegistrationException, IOException {
		Properties channelProperties = new Properties();

		channelProperties.setProperty(TCPChannel.ADDRESS_KEY, this.myself.getAddress().getHostAddress());
		channelProperties.setProperty(TCPChannel.PORT_KEY, "" + (this.myself.getPort()));

		int channelId = createChannel(TCPChannel.NAME, channelProperties);

		registerMessageSerializer(channelId, SendMetricsMessage.ID, SendMetricsMessage.serializer);

		registerTimerHandler(ExportMetricsTimer.PROTO_ID, this::uponExportMetricsTimer);

		/*-------------------- Register Channel Event ------------------------------- */
		registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
		registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
		registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
		registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
		registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);

		this.exportMetricsTimerID = setupPeriodicTimer(new ExportMetricsTimer(), this.exportPeriod, this.exportPeriod);



        openConnection(this.monitor);
		
	}

	/**
	 * Timer handler invoked every {@code exportPeriod} milliseconds. Collects a metric snapshot and
	 * sends it to the monitor; cancels the timer if the exporter has been disabled.
	 *
	 * @param t    the fired timer object
	 * @param time the current Babel virtual time in milliseconds
	 */
	public void uponExportMetricsTimer(ExportMetricsTimer t, long time) {
		if(isExporterDisabled()){
			logger.debug("Exporter is disabled, not exporting metrics");
			cancelTimer(this.exportMetricsTimerID);
			return;
		}
		NodeSample sample = collectMetrics();
		SendMetricsMessage msg = new SendMetricsMessage(sample);

		logger.debug("Sending metrics to {}", this.monitor);
		sendMessage(msg, SimpleMonitor.PROTOCOL_ID, this.monitor);
		logger.debug("Sent metrics sucessfully");
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

//	@Override
//	public ProtocolExporter getExporter() {
//		return this.pe;
//	}
}
