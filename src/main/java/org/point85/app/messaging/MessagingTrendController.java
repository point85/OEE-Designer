package org.point85.app.messaging;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.LoaderFactory;
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
import org.point85.domain.script.ScriptResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	// logger
	private final Logger logger = LoggerFactory.getLogger(getClass());

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
			FXMLLoader loader = LoaderFactory.trendChartLoader();
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
	protected void setImages() {
		super.setImages();
	}

	public void setScriptResolver(ScriptResolver scriptResolver) throws Exception {
		trendChartController.setScriptResolver(scriptResolver);

		lbSourceId.setText("Equipment: " + scriptResolver.getEquipment().getName() + ", Source Id: "
				+ scriptResolver.getSourceId());
		lbBroker.setText(scriptResolver.getDataSource().getId());
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

			MessagingSource source = (MessagingSource) trendChartController.getScriptResolver().getDataSource();

			List<RoutingKey> keys = new ArrayList<>();
			keys.add(RoutingKey.EQUIPMENT_SOURCE_EVENT);
			pubsub.connectToBroker(source.getHost(), source.getPort(), queueName, false, keys, this);
			
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
			logger.error("Failed to ack message: " + e.getMessage());
			return;
		}

		MessageType type = message.getMessageType();

		switch (type) {
		case EQUIPMENT_EVENT:
			handleEquipmentEvent((EquipmentEventMessage) message);
			break;

		case NOTIFICATION:
			// TODO
			logger.info(message.toString());
			break;

		default:
			if (logger.isInfoEnabled()) {
				logger.info("Received unknown message.");
			}
			break;
		}
	}

	private void handleEquipmentEvent(EquipmentEventMessage message) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Received equipment event message: " + message);
		}

		OffsetDateTime odt = DomainUtils.offsetDateTimeFromString(message.getTimestamp());
		ResolutionService service = new ResolutionService(message.getSourceId(), message.getValue(), odt);

		service.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				Throwable t = event.getSource().getException();

				if (t != null) {
					// connection failed
					logger.error("Resolution service failed: " + t.getMessage());
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
			
			ScriptResolver scriptResolver = trendChartController.getScriptResolver();
			//MessagingSource dataSource = (MessagingSource) scriptResolver.getDataSource();
			
			/*
			Integer port = dataSource.getPort();
			
			if (port == null) {
				throw new Exception("A host and port must be specified");
			}
			
			PublisherSubscriber pubsub = pubsubs.get(hostPort);

			if (pubsub == null) {
				pubsub = new PublisherSubscriber();
				pubsubs.put(hostPort, pubsub);

				String[] tokens = hostPort.split(":");

				pubsub.connect(tokens[0], Integer.valueOf(tokens[1]));
			}
			*/

			String sourceId = scriptResolver.getSourceId();
			String value = tfLoopbackValue.getText();
			
			//OffsetDateTime odt = OffsetDateTime.now();
			//String timestamp = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

			EquipmentEventMessage msg = new EquipmentEventMessage();
			msg.setSourceId(sourceId);
			msg.setValue(value);

			pubsub.publish(msg, RoutingKey.EQUIPMENT_SOURCE_EVENT);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// service class for callbacks on received data
	private class ResolutionService extends Service<Void> {
		private String sourceId;
		private String dataValue;
		private OffsetDateTime timestamp;

		public ResolutionService(String sourceId, String dataValue, OffsetDateTime timestamp) {
			this.sourceId = sourceId;
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
