package org.point85.app.messaging;

import org.point85.app.AppUtils;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.mqtt.MqttMessageListener;
import org.point85.domain.mqtt.MqttOeeClient;
import org.point85.domain.mqtt.MqttSource;
import org.point85.domain.mqtt.QualityOfService;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;

public class MqttTrendController extends BaseMessagingTrendController implements MqttMessageListener, DataSubscriber {
	// MQTT client
	private MqttOeeClient mqttClient;

	@Override
	public boolean isSubscribed() {
		return mqttClient != null;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (mqttClient != null) {
			return;
		}

		// client
		mqttClient = new MqttOeeClient();

		// MQTT source
		MqttSource source = (MqttSource) trendChartController.getEventResolver().getDataSource();

		// connect and subscribe to events
		mqttClient.startUp(source.getHost(), source.getPort(), source.getUserName(), source.getUserPassword(), this);
		mqttClient.subscribeToEvents(QualityOfService.EXACTLY_ONCE);

		// add to context
		getApp().getAppContext().addMQTTClient(mqttClient);

		// start the trend
		trendChartController.onStartTrending();
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (mqttClient == null) {
			return;
		}
		mqttClient.disconnect();

		// remove from app context
		getApp().getAppContext().removeMQTTClient(mqttClient);
		mqttClient = null;

		// stop the trend
		trendChartController.onStopTrending();
	}

	@Override
	public void onMqttMessage(ApplicationMessage appMessage) {
		if (!(appMessage instanceof EquipmentEventMessage)) {
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

	@FXML
	private void onLoopbackTest() {
		try {
			if (mqttClient == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.mqtt.server"));
			}

			EquipmentEventMessage message = createEquipmentEventMessage();
			mqttClient.sendEventMessage(message);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
