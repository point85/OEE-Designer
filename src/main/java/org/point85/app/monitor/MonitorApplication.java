package org.point85.app.monitor;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.jms.JmsClient;
import org.point85.domain.jms.JmsMessageListener;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.BaseMessagingClient;
import org.point85.domain.messaging.CollectorCommandMessage;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.messaging.CollectorServerStatusMessage;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.NotificationSeverity;
import org.point85.domain.mqtt.MqttMessageListener;
import org.point85.domain.mqtt.MqttOeeClient;
import org.point85.domain.mqtt.QualityOfService;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.rmq.RmqClient;
import org.point85.domain.rmq.RmqMessageListener;
import org.point85.domain.rmq.RoutingKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class MonitorApplication implements RmqMessageListener, JmsMessageListener, MqttMessageListener {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(MonitorApplication.class);

	// RabbitMQ message publisher/subscribers for incoming notifications
	private final List<RmqClient> rmqSubscriptionClients = new ArrayList<>();

	// RMQ brokers for outgoing commands
	private final Map<String, RmqClient> rmqPublishingClients = new HashMap<>();

	// JMS message publisher/subscribers for incoming notifications
	private final List<JmsClient> jmsSubscriptionClients = new ArrayList<>();

	// JMS brokers for outgoing commands
	private final Map<String, JmsClient> jmsPublishingClients = new HashMap<>();

	// MQTT message publisher/subscribers for incoming notifications
	private final List<MqttOeeClient> mqttSubscriptionClients = new ArrayList<>();

	// MQTT brokers for outgoing commands
	private final Map<String, MqttOeeClient> mqttPublishingClients = new HashMap<>();

	// status monitor
	private MonitorController monitorController;

	// JVM host name
	private String hostname;

	// JVM host IP address
	private String ip;

	String getHostname() {
		return hostname;
	}

	String getIpAddress() {
		return ip;
	}

	public void start(Stage primaryStage) {
		try {
			// for the monitor app
			FXMLLoader loader = FXMLLoaderFactory.monitorApplicationLoader();
			AnchorPane mainLayout = (AnchorPane) loader.getRoot();

			monitorController = loader.getController();
			monitorController.initializeApplication(this);

			// Show the scene containing the root layout.
			Scene scene = new Scene(mainLayout);
			primaryStage.setScene(scene);

			primaryStage.setTitle(MonitorLocalizer.instance().getLangString("monitor.app.title"));
			primaryStage.getIcons().add(ImageManager.instance().getImage(Images.POINT85));

			// connect to messaging brokers
			connectToNotificationBrokers();

			primaryStage.show();

			// send a loopback message
			// our host
			InetAddress address = InetAddress.getLocalHost();
			hostname = address.getHostName();
			ip = address.getHostAddress();

			if (logger.isInfoEnabled()) {
				logger.info("Starting monitor on host " + hostname + " (" + ip + ")");
			}

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
			for (RmqClient pubSub : rmqSubscriptionClients) {
				pubSub.disconnect();
			}

			// disconnect from RMQ pubsubs
			for (Entry<String, RmqClient> entry : rmqPublishingClients.entrySet()) {
				entry.getValue().disconnect();
			}

			// disconnect from JMS pubsubs
			for (Entry<String, JmsClient> entry : jmsPublishingClients.entrySet()) {
				entry.getValue().disconnect();
			}

			// disconnect from MQTT pubsubs
			for (Entry<String, MqttOeeClient> entry : mqttPublishingClients.entrySet()) {
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

		// connect to notification brokers for consuming messages only
		Map<String, BaseMessagingClient> pubSubs = new HashMap<>();

		for (DataCollector collector : collectors) {
			DataSourceType sourceType = collector.getBrokerType();
			String brokerHostName = collector.getBrokerHost();
			Integer brokerPort = collector.getBrokerPort();
			String brokerUser = collector.getBrokerUserName();
			String brokerPassword = collector.getBrokerUserPassword();

			if (brokerHostName == null || brokerHostName.trim().length() == 0 || brokerPort == null) {
				continue;
			}

			switch (sourceType) {
			case JMS:
				startJMSBroker(pubSubs, brokerHostName, brokerPort, brokerUser, brokerPassword);
				break;
			case RMQ:
				startRMQBroker(pubSubs, brokerHostName, brokerPort, brokerUser, brokerPassword);
				break;
			case MQTT:
				startMqttBroker(pubSubs, brokerHostName, brokerPort, brokerUser, brokerPassword);
				break;
			default:
				return;

			}
		}
	}

	private void startRMQBroker(Map<String, BaseMessagingClient> pubSubs, String brokerHostName, Integer brokerPort,
			String brokerUser, String brokerPassword) throws Exception {

		String key = "RMQ." + brokerHostName + ":" + brokerPort;

		if (pubSubs.get(key) != null) {
			// already started
			return;
		}

		// new RMQ client
		RmqClient rmqClient = new RmqClient();

		// connect to broker and listen for messages
		String queueName = getClass().getSimpleName() + "_" + System.currentTimeMillis();

		List<RoutingKey> routingKeys = new ArrayList<>();
		routingKeys.add(RoutingKey.NOTIFICATION_MESSAGE);
		routingKeys.add(RoutingKey.NOTIFICATION_STATUS);
		routingKeys.add(RoutingKey.RESOLVED_EVENT);

		rmqClient.startUp(brokerHostName, brokerPort, brokerUser, brokerPassword, queueName, routingKeys, this);

		pubSubs.put(key, rmqClient);
		rmqSubscriptionClients.add(rmqClient);
	}

	private void startJMSBroker(Map<String, BaseMessagingClient> pubSubs, String brokerHostName, Integer brokerPort,
			String brokerUser, String brokerPassword) throws Exception {

		String key = "JMS." + brokerHostName + ":" + brokerPort;

		if (pubSubs.get(key) != null) {
			// already started
			return;
		}

		// new JMS client
		JmsClient jmsClient = new JmsClient();
		jmsClient.startUp(brokerHostName, brokerPort, brokerUser, brokerPassword, this);
		jmsClient.consumeNotifications(true);

		pubSubs.put(key, jmsClient);
		jmsSubscriptionClients.add(jmsClient);
	}

	private void startMqttBroker(Map<String, BaseMessagingClient> pubSubs, String brokerHostName, Integer brokerPort,
			String brokerUser, String brokerPassword) throws Exception {

		String key = "MQTT." + brokerHostName + ":" + brokerPort;

		if (pubSubs.get(key) != null) {
			// already started
			return;
		}

		// new MQTT client
		MqttOeeClient mqttClient = new MqttOeeClient();
		mqttClient.startUp(brokerHostName, brokerPort, brokerUser, brokerPassword, this);
		mqttClient.subscribeToNotifications(QualityOfService.EXACTLY_ONCE);

		pubSubs.put(key, mqttClient);
		mqttSubscriptionClients.add(mqttClient);
	}

	private void handleMessage(ApplicationMessage message) {
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

	@Override
	public void onRmqMessage(ApplicationMessage message) throws Exception {
		if (message == null) {
			return;
		}

		// process it
		handleMessage(message);
	}

	void sendStartupNotification() throws Exception {
		// publish to self
		CollectorNotificationMessage msg = new CollectorNotificationMessage(hostname, ip);

		msg.setSeverity(NotificationSeverity.INFO);
		msg.setText(MonitorLocalizer.instance().getLangString("monitor.startup"));
		msg.setTimestamp(DomainUtils.offsetDateTimeToString(OffsetDateTime.now(), DomainUtils.OFFSET_DATE_TIME_8601));

		try {
			if (!rmqSubscriptionClients.isEmpty()) {
				RmqClient pubSub = rmqSubscriptionClients.get(0);

				pubSub.sendNotificationMessage(msg);
			}

			if (!jmsSubscriptionClients.isEmpty()) {
				JmsClient pubSub = jmsSubscriptionClients.get(0);

				pubSub.sendNotificationMessage(msg);
			}

			if (!mqttSubscriptionClients.isEmpty()) {
				MqttOeeClient pubSub = mqttSubscriptionClients.get(0);

				pubSub.sendNotificationMessage(msg);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void sendRestartCommand(DataCollector collector) throws Exception {
		String key = collector.getBrokerHost() + ":" + collector.getBrokerPort();

		DataSourceType type = collector.getBrokerType();

		if (type == null) {
			return;
		}

		// create the message
		CollectorCommandMessage message = new CollectorCommandMessage(getHostname(), getIpAddress());
		message.setCommand(CollectorCommandMessage.CMD_RESTART);

		if (type.equals(DataSourceType.RMQ)) {
			RmqClient pubSub = rmqPublishingClients.get(key);

			if (pubSub == null) {
				// new publisher
				pubSub = new RmqClient();

				pubSub.connect(collector.getBrokerHost(), collector.getBrokerPort(), collector.getBrokerUserName(),
						collector.getBrokerUserPassword());

				rmqPublishingClients.put(key, pubSub);
			}
			pubSub.sendCommandMessage(message);

		} else if (type.equals(DataSourceType.JMS)) {
			JmsClient pubSub = jmsPublishingClients.get(key);

			if (pubSub == null) {
				// new publisher
				pubSub = new JmsClient();

				pubSub.connect(collector.getBrokerHost(), collector.getBrokerPort(), collector.getBrokerUserName(),
						collector.getBrokerUserPassword());

				jmsPublishingClients.put(key, pubSub);
			}
			pubSub.sendEventMessage(message);

		} else if (type.equals(DataSourceType.MQTT)) {
			MqttOeeClient pubSub = mqttPublishingClients.get(key);

			if (pubSub == null) {
				// new publisher
				pubSub = new MqttOeeClient();

				pubSub.connect(collector.getBrokerHost(), collector.getBrokerPort(), collector.getBrokerUserName(),
						collector.getBrokerUserPassword());

				mqttPublishingClients.put(key, pubSub);
			}
			pubSub.sendEventMessage(message);
		}
	}

	@Override
	public void onJmsMessage(ApplicationMessage message) {
		handleMessage(message);
	}

	@Override
	public void onMqttMessage(ApplicationMessage message) {
		handleMessage(message);
	}
}
