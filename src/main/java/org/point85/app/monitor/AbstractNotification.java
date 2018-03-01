package org.point85.app.monitor;

import org.point85.domain.messaging.ApplicationMessage;

public abstract class AbstractNotification {
	protected String collectorHostName;
	protected String collectorIpAddress;
	protected String timestamp;
	
	protected AbstractNotification() {
		
	}

	protected AbstractNotification(ApplicationMessage message) {
		this.collectorHostName = message.getSenderHostName();
		this.collectorIpAddress = message.getSenderHostAddress();
		this.timestamp = message.getTimestamp();
	}

	public String getCollectorHost() {
		return collectorHostName;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getCollectorIpAddress() {
		return collectorIpAddress;
	}

	@Override
	public String toString() {
		return "Sender: " + collectorHostName + ", time: " + timestamp;
	}
}
