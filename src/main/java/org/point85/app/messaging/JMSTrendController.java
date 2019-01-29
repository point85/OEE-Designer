package org.point85.app.messaging;

import java.time.OffsetDateTime;

import org.point85.app.AppUtils;
import org.point85.app.charts.DataSubscriber;
import org.point85.domain.jms.JMSEquipmentEventListener;
import org.point85.domain.jms.JMSClient;
import org.point85.domain.jms.JMSSource;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.script.EventResolver;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;

public class JMSTrendController extends BaseMessagingTrendController implements JMSEquipmentEventListener, DataSubscriber {
	// AMQ JMS client
	private JMSClient jmsClient;

	@Override
	public boolean isSubscribed() {
		return jmsClient != null ? true : false;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (jmsClient != null) {
			return;
		}

		jmsClient = new JMSClient();

		JMSSource source = (JMSSource) trendChartController.getEventResolver().getDataSource();

		jmsClient.startUp(source.getHost(), source.getPort(), source.getUserName(), source.getUserPassword(),
				this);

		// add to context
		getApp().getAppContext().addJMSClient(jmsClient);

		// start the trend
		trendChartController.onStartTrending();
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (jmsClient == null) {
			return;
		}
		jmsClient.shutDown();

		// remove from app context
		getApp().getAppContext().removeJMSClient(jmsClient);
		jmsClient = null;

		// stop the trend
		trendChartController.onStopTrending();
	}

	@Override
	public void onJMSEquipmentEvent(EquipmentEventMessage message) {
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
			if (jmsClient == null) {
				throw new Exception("The trend is not connected to a JMS broker.");
			}

			EventResolver eventResolver = trendChartController.getEventResolver();

			String sourceId = eventResolver.getSourceId();
			String value = getLoopbackValue();

			EquipmentEventMessage msg = new EquipmentEventMessage();
			msg.setSourceId(sourceId);
			msg.setValue(value);
			msg.setDateTime(OffsetDateTime.now());

			jmsClient.sendToQueue(msg, JMSClient.DEFAULT_QUEUE, 30);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
