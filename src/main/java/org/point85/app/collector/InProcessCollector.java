package org.point85.app.collector;

import org.point85.domain.collector.CollectorServer;

public class InProcessCollector {
	public static void main(String[] args) {
		// create the collector server
		CollectorServer collectorServer = new CollectorServer();

		try {
			// start server
			collectorServer.startup();
		} catch (Exception e) {
			collectorServer.onException("Startup failed. ", e);
		} finally {			
			try {
				// pause thread
				Thread.sleep(Long.MAX_VALUE);
				
				collectorServer.shutdown();
			} catch (Exception ex) {
				collectorServer.onException("Shutdown failed. ", ex);
			}
		}
	}
}
