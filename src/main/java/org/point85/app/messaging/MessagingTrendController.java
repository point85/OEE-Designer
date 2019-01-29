package org.point85.app.messaging;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.charts.DataSubscriber;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageListener;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.MessagingClient;
import org.point85.domain.messaging.MessagingSource;
import org.point85.domain.messaging.RoutingKey;
import org.point85.domain.script.EventResolver;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;

public class MessagingTrendController extends BaseMessagingTrendController implements MessageListener, DataSubscriber {
	// RabbitMQ message publisher/subscriber
	private MessagingClient pubSub;

	@Override
	public boolean isSubscribed() {
		return pubSub != null ? true : false;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (pubSub != null) {
			return;
		}
		pubSub = new MessagingClient();

		MessagingSource source = (MessagingSource) trendChartController.getEventResolver().getDataSource();

		List<RoutingKey> keys = new ArrayList<>();
		keys.add(RoutingKey.EQUIPMENT_SOURCE_EVENT);

		String queueName = getClass().getSimpleName() + "_" + System.currentTimeMillis();

		pubSub.startUp(source.getHost(), source.getPort(), source.getUserName(), source.getUserPassword(),
				queueName, keys, this);

		// add to context
		getApp().getAppContext().addMessagingClient(pubSub);

		// start the trend
		trendChartController.onStartTrending();
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (pubSub == null) {
			return;
		}
		pubSub.shutDown();

		// remove from app context
		getApp().getAppContext().removeMessagingClient(pubSub);
		pubSub = null;

		// stop the trend
		trendChartController.onStopTrending();
	}

	@Override
	public void onMessage(Channel channel, Envelope envelope, ApplicationMessage message) throws Exception {
		if (message == null) {
			return;
		}

		// ack it now
		channel.basicAck(envelope.getDeliveryTag(), MessagingClient.ACK_MULTIPLE);

		MessageType type = message.getMessageType();

		if (!type.equals(MessageType.EQUIPMENT_EVENT)) {
			throw new Exception("Received unknown message of type " + message);
		}

		handleEquipmentEvent((EquipmentEventMessage) message);
	}

	private void handleEquipmentEvent(EquipmentEventMessage message) throws Exception {
		ResolutionService service = new ResolutionService(message.getSourceId(), message.getValue(),
				message.getDateTime());

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
			if (pubSub == null) {
				throw new Exception("The trend is not connected to an RMQ broker.");
			}

			EventResolver eventResolver = trendChartController.getEventResolver();

			String sourceId = eventResolver.getSourceId();
			String value = getLoopbackValue();

			EquipmentEventMessage msg = new EquipmentEventMessage();
			msg.setSourceId(sourceId);
			msg.setValue(value);

			pubSub.publish(msg, RoutingKey.EQUIPMENT_SOURCE_EVENT, 30);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
