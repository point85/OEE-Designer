package org.point85.app.charts;

public interface DataSubscriber {
	void subscribeToDataSource() throws Exception;
	void unsubscribeFromDataSource() throws Exception;
	boolean isSubscribed();
}
