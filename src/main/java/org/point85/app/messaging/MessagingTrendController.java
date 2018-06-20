package org.point85.app.messaging;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
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

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;

public class MessagingTrendController extends DesignerDialogController implements MessageListener, DataSubscriber {
	// RabbitMQ message publisher/subscriber
	private PublisherSubscriber pubSub;

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

		// loopback test
		btLoopback.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btLoopback.setContentDisplay(ContentDisplay.LEFT);
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
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	@FXML
	protected void onCancel() {
		super.onCancel();

		try {
			unsubscribeFromDataSource();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
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
		return pubSub != null ? true : false;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (pubSub == null) {
			pubSub = new PublisherSubscriber();

			MessagingSource source = (MessagingSource) trendChartController.getEventResolver().getDataSource();

			List<RoutingKey> keys = new ArrayList<>();
			keys.add(RoutingKey.EQUIPMENT_SOURCE_EVENT);
			
			String queueName = getClass().getSimpleName() + "_" + System.currentTimeMillis();
			
			pubSub.connectAndSubscribe(source.getHost(), source.getPort(), source.getUserName(),
					source.getUserPassword(), queueName, keys, this);
			
			// add to context
			getApp().getAppContext().addMessagingClient(pubSub);

			// start the trend
			trendChartController.onStartTrending();
		}
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (pubSub == null) {
			return;
		}
		pubSub.disconnect();
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
		channel.basicAck(envelope.getDeliveryTag(), PublisherSubscriber.ACK_MULTIPLE);

		MessageType type = message.getMessageType();

		if (!type.equals(MessageType.EQUIPMENT_EVENT)) {
			throw new Exception("Received unknown message of type " + message);
		}

		handleEquipmentEvent((EquipmentEventMessage) message);
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
			String value = tfLoopbackValue.getText();

			EquipmentEventMessage msg = new EquipmentEventMessage();
			msg.setSourceId(sourceId);
			msg.setValue(value);

			pubSub.publish(msg, RoutingKey.EQUIPMENT_SOURCE_EVENT, 30);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// service class for callbacks on received data
	private class ResolutionService extends Service<Void> {
		private final String dataValue;
		private final OffsetDateTime timestamp;

		public ResolutionService(String sourceId, String dataValue, OffsetDateTime timestamp) {
			this.dataValue = dataValue;
			this.timestamp = timestamp;
		}

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

				@Override
				protected Void call() {
					try {
						trendChartController.invokeResolver(getApp().getAppContext(), dataValue, timestamp);
					} catch (Exception e) {
						Platform.runLater(() -> {
							AppUtils.showErrorDialog(e);
						});
					}
					return null;
				}
			};
		}
	}

}
