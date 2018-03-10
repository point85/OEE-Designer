package org.point85.app.collector;

import org.apache.log4j.PropertyConfigurator;
import org.point85.domain.collector.CollectorServer;
import org.point85.domain.persistence.PersistenceService;

public class InProcessCollector {
	private static final String LOG4J_PROPS = "log4j";

	private static final int IDX_JDBC = 0;
	private static final int IDX_USER = 1;
	private static final int IDX_PASSWORD = 2;

	public static void main(String[] args) {
		// configure log4j
		PropertyConfigurator.configure(System.getProperty(LOG4J_PROPS));

		// create the EMF
		PersistenceService.instance().initialize(args[IDX_JDBC], args[IDX_USER], args[IDX_PASSWORD]);

		// create the collector server
		CollectorServer collectorServer = new CollectorServer();

		try {
			// start server
			collectorServer.startup();
		} catch (Exception e) {
			collectorServer.onException("Startup failed. ", e);
			collectorServer.shutdown();
		}
	}
}
