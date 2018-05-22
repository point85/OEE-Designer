package org.point85.app.messaging;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.DesignerDialogController;
import org.point85.domain.DomainUtils;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageListener;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.MessagingSource;
import org.point85.domain.messaging.PublisherSubscriber;
import org.point85.domain.messaging.RoutingKey;
import org.point85.domain.script.EventResolver;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;

public class MessagingTrendController extends DesignerDialogController implements MessageListener, DataSubscriber {
	private static int queueCounter = 0;

	// RabbitMQ message publisher/subscriber
	private PublisherSubscriber pubsub;

	// trend chart
	private TrendChartController trendChartController;

	// trend chart pane
	private SplitPane spTrendChart;

	@FXML
	private Label lbSourceId;

	@FXML
	private Label lbBroker;

	@FXML
	private TextField tfLoopbackValue;

	@FXML
	private Button btLoopback;

	public SplitPane initializeTrend() throws Exception {
		if (trendChartController == null) {
			// Load the fxml file and create the anchor pane
			FXMLLoader loader = FXMLLoaderFactory.trendChartLoader();
			spTrendChart = (SplitPane) loader.getRoot();

			trendChartController = loader.getController();
			trendChartController.initialize(getApp());

			// data provider
			trendChartController.setProvider(this);

			setImages();

			lbBroker.setText("");
		}
		return spTrendChart;
	}

	// images for buttons
	@Override
	protected void setImages() throws Exception {
		super.setImages();
	}

	public void setEventResolver(EventResolver eventResolver) throws Exception {
		eventResolver.setWatchMode(true);
		trendChartController.setScriptResolver(eventResolver);

		lbSourceId.setText(
				"Equipment: " + eventResolver.getEquipment().getName() + ", Source Id: " + eventResolver.getSourceId());
		lbBroker.setText(eventResolver.getDataSource().getId());
	}

	@Override
	@FXML
	protected void onOK() {
		super.onOK();
		try {
			unsubscribeFromDataSource();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@FXML
	protected void onCancel() {
		super.onCancel();

		try {
			unsubscribeFromDataSource();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onSubscribe() {
		try {
			subscribeToDataSource();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onUnsubscribe() {
		try {
			unsubscribeFromDataSource();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	public boolean isSubscribed() {
		return pubsub != null ? true : false;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		String queueName = getClass().getSimpleName() + "_" + queueCounter;
		queueCounter++;

		if (pubsub == null) {
			pubsub = new PublisherSubscriber();

			MessagingSource source = (MessagingSource) trendChartController.getEventResolver().getDataSource();

			List<RoutingKey> keys = new ArrayList<>();
			keys.add(RoutingKey.EQUIPMENT_SOURCE_EVENT);
			pubsub.connectAndSubscribe(source.getHost(), source.getPort(), source.getUserName(), source.getUserPassword(),
					queueName, false, keys, this);

			// start the trend
			trendChartController.onStartTrending();
		}
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (pubsub == null) {
			return;
		}
		pubsub.disconnect();
		pubsub = null;

		// stop the trend
		trendChartController.onStopTrending();
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

		switch (type) {
		case EQUIPMENT_EVENT:
			handleEquipmentEvent((EquipmentEventMessage) message);
			break;

		default:
			throw new Exception("Received unknown message of type " + message);
		}
	}

	private void handleEquipmentEvent(EquipmentEventMessage message) throws Exception {

		OffsetDateTime odt = DomainUtils.offsetDateTimeFromString(message.getTimestamp());
		ResolutionService service = new ResolutionService(message.getSourceId(), message.getValue(), odt);

		service.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				Throwable t = event.getSource().getException();

				if (t != null) {
					// connection failed
					t.printStackTrace();
				}
			}
		});

		// run on application thread
		service.start();
	}

	@FXML
	private void onLoopbackTest() {
		try {
			if (pubsub == null) {
				throw new Exception("The trend is not connected to an RMQ broker.");
			}

			EventResolver eventResolver = trendChartController.getEventResolver();

			String sourceId = eventResolver.getSourceId();
			String value = tfLoopbackValue.getText();

			EquipmentEventMessage msg = new EquipmentEventMessage();
			msg.setSourceId(sourceId);
			msg.setValue(value);

			pubsub.publish(msg, RoutingKey.EQUIPMENT_SOURCE_EVENT, 3600);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// service class for callbacks on received data
	private class ResolutionService extends Service<Void> {
		private String dataValue;
		private OffsetDateTime timestamp;

		public ResolutionService(String sourceId, String dataValue, OffsetDateTime timestamp) {
			this.dataValue = dataValue;
			this.timestamp = timestamp;
		}

		@Override
		protected Task<Void> createTask() {
			Task<Void> resolutionTask = new Task<Void>() {

				@Override
				protected Void call() throws Exception {
					trendChartController.invokeResolver(getApp().getAppContext(), dataValue, timestamp);
					return null;
				}
			};
			return resolutionTask;
		}
	}

}
