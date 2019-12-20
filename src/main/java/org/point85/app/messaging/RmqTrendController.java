package org.point85.app.messaging;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.rmq.RmqClient;
import org.point85.domain.rmq.RmqMessageListener;
import org.point85.domain.rmq.RmqSource;
import org.point85.domain.rmq.RoutingKey;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;

public class RmqTrendController extends BaseMessagingTrendController implements RmqMessageListener, DataSubscriber {
	// RabbitMQ message publisher/subscriber
	private RmqClient pubSub;

	@Override
	public boolean isSubscribed() {
		return pubSub != null ? true : false;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (pubSub != null) {
			return;
		}
		pubSub = new RmqClient();

		RmqSource source = (RmqSource) trendChartController.getEventResolver().getDataSource();

		List<RoutingKey> keys = new ArrayList<>();
		keys.add(RoutingKey.EQUIPMENT_SOURCE_EVENT);

		String queueName = getClass().getSimpleName() + "_" + System.currentTimeMillis();

		pubSub.startUp(source.getHost(), source.getPort(), source.getUserName(), source.getUserPassword(), queueName,
				keys, this);

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
		pubSub.disconnect();

		// remove from app context
		getApp().getAppContext().removeMessagingClient(pubSub);
		pubSub = null;

		// stop the trend
		trendChartController.onStopTrending();
	}

	@Override
	public void onRmqMessage(ApplicationMessage message) throws Exception {
		if (message == null) {
			return;
		}

		MessageType type = message.getMessageType();

		if (!type.equals(MessageType.EQUIPMENT_EVENT)) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("unknown.message", type));
		}

		handleEquipmentEvent((EquipmentEventMessage) message);
	}

	private void handleEquipmentEvent(EquipmentEventMessage message) throws Exception {
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
			if (pubSub == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.rmq.broker"));
			}

			EquipmentEventMessage msg = createEquipmentEventMessage();
			pubSub.sendEquipmentEventMessage(msg);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
