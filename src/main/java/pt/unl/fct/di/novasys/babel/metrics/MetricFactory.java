package pt.unl.fct.di.novasys.babel.metrics;

/**
 * This class provides a set of metrics that are relevant for the different types of protocols
 * <p>
 *     The types of protocols are:
 *     <ul>
 *         <li>Server-client protocols</li>
 *         <li>Membership protocols</li>
 *         <li>
 *     </ul>
 */
public class MetricFactory {
    /**
     * Provides a set of metrics relevant for server-client protocols
     * <p>
     *     The metrics are:
     *     <ul>
     *         <li>Number of requests received by the server</li>
     *         <li>Number of requests sent by the client</li>
     *
     *     </ul>
     *
     */
   public static class ServerClient{
        public static class Server{

            /**
             * Creates a new metric that counts the number of messages received by the server
             * <p>
             * The name of the metric is "N_Requests"
             *
             * @return the metric
             */
            public static Counter n_requests(){
                return n_requests("N_Requests");
            }

            public static Counter n_requests(String name){
                return new Counter(name, "request(s)");

            }
        }
    }
}
