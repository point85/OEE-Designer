package org.point85.app.monitor;

import java.util.Objects;

import org.point85.domain.collector.DataCollector;
import org.point85.domain.messaging.CollectorServerStatusMessage;

public class CollectorServerStatus extends AbstractNotification {
	private String dataCollectorName;
	private String dataCollectorDescription;
	private double usedMemory = 0.0;
	private double freeMemory = 0.0;
	private double systemLoadAvg = 0.0;

	public CollectorServerStatus(CollectorServerStatusMessage message) {
		super(message);
		this.usedMemory = message.getUsedMemory();
		this.freeMemory = message.getFreeMemory();
		this.systemLoadAvg = message.getSystemLoadAvg();
	}

	public CollectorServerStatus(DataCollector collector) {
		dataCollectorName = collector.getName();
		dataCollectorDescription = collector.getDescription();
		collectorHostName = collector.getHost();

	}

	public double getUsedMemory() {
		return usedMemory;
	}

	public double getFreeMemory() {
		return freeMemory;
	}

	public double getSystemLoadAvg() {
		return systemLoadAvg;
	}

	public String getDataCollectorName() {
		return dataCollectorName;
	}

	public void setDataCollectorName(String dataCollectorName) {
		this.dataCollectorName = dataCollectorName;
	}

	public String getDataCollectorDescription() {
		return dataCollectorDescription;
	}

	public void setDataCollectorDescription(String dataCollectorDescription) {
		this.dataCollectorDescription = dataCollectorDescription;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getCollectorHost());
	}

	@Override
	public boolean equals(Object other) {

		if (other == null || !(other instanceof CollectorServerStatus)) {
			return false;
		}
		CollectorServerStatus otherResolver = (CollectorServerStatus) other;

		if (getCollectorHost() == null) {
			return true;
		}

		if (!getCollectorHost().equalsIgnoreCase(otherResolver.getCollectorHost())) {
			return false;
		}

		return true;
	}

}
