package org.point85.app.messaging;

import org.point85.app.AppUtils;
import org.point85.app.charts.DataSubscriber;
import org.point85.domain.kafka.KafkaMessageListener;
import org.point85.domain.kafka.KafkaOeeClient;
import org.point85.domain.kafka.KafkaSource;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;

public class KafkaTrendController extends BaseMessagingTrendController implements KafkaMessageListener, DataSubscriber {
	// Kafka consumer client
	private KafkaOeeClient pubSub;

	@Override
	public boolean isSubscribed() {
		return pubSub != null;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (isSubscribed()) {
			return;
		}

		// create a consumer
		KafkaSource server = (KafkaSource) trendChartController.getEventResolver().getDataSource();
		pubSub = new KafkaOeeClient();

		pubSub.createConsumer(server, KafkaOeeClient.EVENT_TOPIC);
		Integer interval = trendChartController.getEventResolver().getUpdatePeriod();

		if (interval == null) {
			interval = KafkaOeeClient.DEFAULT_POLLING_INTERVAL;
		}
		pubSub.setPollingInterval(interval);

		// create a producer
		pubSub.createProducer(server, KafkaOeeClient.EVENT_TOPIC);

		// subscribe to event messages
		pubSub.startPolling();

		// add to context
		getApp().getAppContext().addKafkaClient(pubSub);
		
		// start the trend
		trendChartController.enableTrending(true);

		// start the trend
		trendChartController.onStartTrending();
		
		// register this client as a message listener too
		pubSub.registerListener(this);
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (pubSub != null) {
			pubSub.disconnect();

			// remove from app context
			getApp().getAppContext().removeKafkaClient(pubSub);
			pubSub = null;
		}
	}

	@FXML
	private void onLoopbackTest() {
		try {
			EquipmentEventMessage message = createEquipmentEventMessage();
			pubSub.sendEventMessage(message);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	public void onKafkaMessage(ApplicationMessage appMessage) {
		if (!(appMessage instanceof EquipmentEventMessage)) {
			// ignore it
			return;
		}

		EquipmentEventMessage message = (EquipmentEventMessage) appMessage;

		ResolutionService service = new ResolutionService(message.getValue(), message.getTimestamp(),
				message.getReason());

		service.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				Throwable t = event.getSource().getException();

				if (t != null) {
					// connection failed
					AppUtils.showErrorDialog(t.getMessage());
				}
			}
		});

		// run on application thread
		service.start();
	}
}
