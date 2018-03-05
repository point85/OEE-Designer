package org.point85.app.monitor;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.messaging.CollectorServerStatusMessage;
import org.point85.domain.messaging.MessageListener;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.NotificationSeverity;
import org.point85.domain.messaging.PublisherSubscriber;
import org.point85.domain.messaging.RoutingKey;
import org.point85.domain.persistence.PersistenceService;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class MonitorApplication extends Application implements MessageListener {
	// data collectors being monitored
	private List<DataCollector> collectors;

	// serializer
	protected Gson gson = new Gson();

	// RabbitMQ message publisher/subscriber
	private List<PublisherSubscriber> notificationPubSubs = new ArrayList<>();

	// counter for pubsub queues
	private int queueCounter = 0;

	// status monitor
	private MonitorController monitorController;

	@Override
	public void start(Stage primaryStage) {
		try {
			primaryStage.setTitle("OEE Monitor");
			primaryStage.getIcons().add(ImageManager.instance().getImage(Images.POINT85));

			URL url = getClass().getResource("Monitor.fxml");
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(url);
			AnchorPane mainLayout = (AnchorPane) loader.load();
			loader.getController();

			// Show the scene containing the root layout.
			Scene scene = new Scene(mainLayout);
			primaryStage.setScene(scene);

			monitorController = loader.getController();
			monitorController.initializeApplication(this);

			// connect to RMA brokers
			connectToNotificationBrokers();

			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			// disconnect from notification pubsubs
			for (PublisherSubscriber pubSub : this.notificationPubSubs) {
				pubSub.disconnect();
			}
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void connectToNotificationBrokers() throws Exception {
		List<CollectorState> states = new ArrayList<>();
		states.add(CollectorState.READY);
		states.add(CollectorState.RUNNING);

		collectors = PersistenceService.instance().fetchCollectorsByState(states);

		// connect to notification brokers for consuming only
		Map<String, PublisherSubscriber> pubSubs = new HashMap<>();

		for (DataCollector collector : collectors) {
			String brokerHostName = collector.getBrokerHost();
			Integer brokerPort = collector.getBrokerPort();

			if (brokerHostName != null && brokerPort != null) {
				String key = brokerHostName + ":" + brokerPort;

				if (pubSubs.get(key) == null) {
					// new publisher
					PublisherSubscriber pubsub = new PublisherSubscriber();

					// connect to broker
					String queueName = getClass().getSimpleName() + "_" + queueCounter++;
					List<RoutingKey> routingKeys = new ArrayList<>();
					routingKeys.add(RoutingKey.NOTIFICATION_MESSAGE);
					routingKeys.add(RoutingKey.NOTIFICATION_STATUS);

					pubsub.connectToBroker(brokerHostName, brokerPort, queueName, false, routingKeys, this);

					pubSubs.put(key, pubsub);
					notificationPubSubs.add(pubsub);
				}
			}
		}
	}

	@Override
	public void onMessage(Channel channel, Envelope envelope, ApplicationMessage message) throws Exception {
		if (message == null) {
			return;
		}

		// ack it now
		try {
			channel.basicAck(envelope.getDeliveryTag(), PublisherSubscriber.ACK_MULTIPLE);
		} catch (Exception e) {
			throw new Exception("Failed to ack message: " + e.getMessage());
		}

		MessageType type = message.getMessageType();

		if (type.equals(MessageType.NOTIFICATION)) {
			Platform.runLater(() -> {
				try {
					monitorController.handleNotification((CollectorNotificationMessage) message);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			});
		} else if (type.equals(MessageType.STATUS)) {
			Platform.runLater(() -> {
				try {
					monitorController.handleCollectorStatus((CollectorServerStatusMessage) message);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			});
		} else if (type.equals(MessageType.RESOLVED_EVENT)) {
			Platform.runLater(() -> {
				try {
					// update dashboard
					monitorController.updateDashboard((CollectorResolvedEventMessage) message);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			});
		}
	}

	// TODO
	void generateData() {
		int num = 1;
		OffsetDateTime odt = OffsetDateTime.now();

		for (int i = 0; i < num; i++) {
			CollectorNotificationMessage msg = new CollectorNotificationMessage("LenovoE555", "192.168.9.0");

			double r = Math.random();
			if (r > 0.33d) {
				msg.setSeverity(NotificationSeverity.ERROR);
			} else {
				msg.setSeverity(NotificationSeverity.WARNING);
			}

			msg.setText("This is a notification");
			msg.setTimestamp(odt.plusHours(i));

			// Platform.runLater(() -> {
			try {
				if (this.notificationPubSubs.size() > 0) {
					PublisherSubscriber pubSub = notificationPubSubs.get(0);

					pubSub.publish(msg, RoutingKey.NOTIFICATION_MESSAGE);
				}
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
			// });
		}

		// status msg
		CollectorServerStatusMessage msg = new CollectorServerStatusMessage("LenovoE555", "192.168.9.0");

		try {
			if (this.notificationPubSubs.size() > 0) {
				PublisherSubscriber pubSub = notificationPubSubs.get(0);

				pubSub.publish(msg, RoutingKey.NOTIFICATION_STATUS);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	/**
	 * Main entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
