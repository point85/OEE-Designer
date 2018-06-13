package org.point85.app.monitor;

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.CollectorCommandMessage;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.messaging.CollectorServerStatusMessage;
import org.point85.domain.messaging.MessageListener;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.NotificationSeverity;
import org.point85.domain.messaging.PublisherSubscriber;
import org.point85.domain.messaging.RoutingKey;
import org.point85.domain.persistence.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class MonitorApplication implements MessageListener {
	// sec for a command message to live in the queue
	private static final int COMMAND_TTL_SEC = 30;

	// notification message TTL
	private static final int NOTIFICATION_TTL_SEC = 30;

	// logger
	private static final Logger logger = LoggerFactory.getLogger(MonitorApplication.class);

	// RabbitMQ message publisher/subscribers for incoming notifications
	private final List<PublisherSubscriber> notificationPubSubs = new ArrayList<>();

	// RMQ brokers for outgoing commands
	private final Map<String, PublisherSubscriber> commandPubSubs = new HashMap<>();

	// status monitor
	private MonitorController monitorController;

	// JVM host name
	private String hostname;

	// JVM host IP address
	private String ip;

	public MonitorApplication() {
		// nothing to initialize
	}

	String getHostname() {
		return hostname;
	}

	String getIpAddress() {
		return ip;
	}

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

			// send a loopback message
			// our host
			InetAddress address = InetAddress.getLocalHost();
			hostname = address.getHostName();
			ip = address.getHostAddress();

			sendStartupNotification();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void stop() {
		try {
			// JPA service
			PersistenceService.instance().close();

			// disconnect from notification pubsubs
			for (PublisherSubscriber pubSub : notificationPubSubs) {
				pubSub.disconnect();
			}

			// disconnect from command pubsubs
			for (Entry<String, PublisherSubscriber> entry : commandPubSubs.entrySet()) {
				entry.getValue().disconnect();
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	private void connectToNotificationBrokers() throws Exception {
		List<CollectorState> states = new ArrayList<>();
		states.add(CollectorState.READY);
		states.add(CollectorState.RUNNING);

		// data collectors being monitored
		List<DataCollector> collectors = PersistenceService.instance().fetchCollectorsByState(states);

		// connect to notification brokers for consuming only
		Map<String, PublisherSubscriber> pubSubs = new HashMap<>();

		for (DataCollector collector : collectors) {
			String brokerHostName = collector.getBrokerHost();
			Integer brokerPort = collector.getBrokerPort();
			String brokerUser = collector.getBrokerUserName();
			String brokerPassword = collector.getBrokerUserPassword();

			if (brokerHostName != null && brokerPort != null) {
				String key = brokerHostName + ":" + brokerPort;

				if (pubSubs.get(key) == null) {
					// new publisher
					PublisherSubscriber pubsub = new PublisherSubscriber();

					// connect to broker and listen for messages
					String queueName = getClass().getSimpleName() + "_" + System.currentTimeMillis();

					List<RoutingKey> routingKeys = new ArrayList<>();
					routingKeys.add(RoutingKey.NOTIFICATION_MESSAGE);
					routingKeys.add(RoutingKey.NOTIFICATION_STATUS);
					routingKeys.add(RoutingKey.RESOLVED_EVENT);

					pubsub.connectAndSubscribe(brokerHostName, brokerPort, brokerUser, brokerPassword, queueName,
							routingKeys, this);

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
		channel.basicAck(envelope.getDeliveryTag(), PublisherSubscriber.ACK_MULTIPLE);

		MessageType type = message.getMessageType();

		if (logger.isInfoEnabled()) {
			logger.info("Received message of class " + message.getClass().getSimpleName());
		}

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

	void sendStartupNotification() throws Exception {
		// our host
		CollectorNotificationMessage msg = new CollectorNotificationMessage(hostname, ip);

		msg.setSeverity(NotificationSeverity.INFO);
		msg.setText("Monitor startup");

		try {
			if (!notificationPubSubs.isEmpty()) {
				PublisherSubscriber pubSub = notificationPubSubs.get(0);

				pubSub.publish(msg, RoutingKey.NOTIFICATION_MESSAGE, NOTIFICATION_TTL_SEC);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void sendRestartCommand(DataCollector collector) throws Exception {
		String key = collector.getBrokerHost() + ":" + collector.getBrokerPort();
		PublisherSubscriber pubSub = commandPubSubs.get(key);

		if (pubSub == null) {
			// new publisher
			pubSub = new PublisherSubscriber();

			pubSub.connect(collector.getBrokerHost(), collector.getBrokerPort(), collector.getBrokerUserName(),
					collector.getBrokerUserPassword());

			commandPubSubs.put(key, pubSub);
		}

		// create the message
		CollectorCommandMessage message = new CollectorCommandMessage(getHostname(), getIpAddress());
		message.setCommand(CollectorCommandMessage.CMD_RESTART);

		pubSub.publish(message, RoutingKey.COMMAND_MESSAGE, COMMAND_TTL_SEC);
	}
}
