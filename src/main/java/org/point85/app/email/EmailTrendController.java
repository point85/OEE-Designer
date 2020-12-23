package org.point85.app.email;

import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.app.messaging.BaseMessagingTrendController;
import org.point85.domain.email.EmailClient;
import org.point85.domain.email.EmailMessageListener;
import org.point85.domain.email.EmailSource;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;

public class EmailTrendController extends BaseMessagingTrendController implements EmailMessageListener, DataSubscriber {
	// Email consumer client
	private EmailClient emailClient;

	@FXML
	private Button btReceive;

	// images for buttons
	@Override
	protected void setImages() {
		super.setImages();

		// check email
		btReceive.setGraphic(ImageManager.instance().getImageView(Images.READ));
		btReceive.setContentDisplay(ContentDisplay.LEFT);
	}

	@Override
	public boolean isSubscribed() {
		return emailClient != null;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (isSubscribed()) {
			return;
		}

		// create a consumer
		EmailSource server = (EmailSource) trendChartController.getEventResolver().getDataSource();
		emailClient = new EmailClient(server);

		Integer interval = trendChartController.getEventResolver().getUpdatePeriod();

		if (interval == null) {
			interval = EmailClient.DEFAULT_POLLING_INTERVAL;
		}
		emailClient.setPollingInterval(interval);

		// add to context
		getApp().getAppContext().addEmailClient(emailClient);

		// start the trend
		trendChartController.enableTrending(true);

		// start the trend
		trendChartController.onStartTrending();

		// register this client as a message listener too
		emailClient.registerListener(this);
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (emailClient != null) {
			emailClient.stopPolling();
		}
	}

	@FXML
	private void onLoopbackTest() {
		try {
			if (emailClient == null) {
				return;
			}

			EquipmentEventMessage message = createEquipmentEventMessage();
			emailClient.sendEvent(emailClient.getSource().getUserName(),
					DesignerLocalizer.instance().getLangString("email.test.subject"), message);

			AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("email.sent.successfully"));
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onCheckEmail() {
		try {
			if (emailClient == null) {
				return;
			}

			List<ApplicationMessage> messages = emailClient.receiveEmails();

			for (ApplicationMessage message : messages) {
				onEmailMessage(message);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e.getClass().getSimpleName() + ":" + e);
		}
	}

	@Override
	public void onEmailMessage(ApplicationMessage appMessage) {
		if (!(appMessage instanceof EquipmentEventMessage)) {
			// ignore it
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
}
