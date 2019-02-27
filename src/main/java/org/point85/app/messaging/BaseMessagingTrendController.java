package org.point85.app.messaging;

import java.time.OffsetDateTime;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.DesignerDialogController;
import org.point85.domain.DomainUtils;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.script.EventResolver;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;

public abstract class BaseMessagingTrendController extends DesignerDialogController implements DataSubscriber {
	// trend chart
	protected TrendChartController trendChartController;

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
		trendChartController.setEventResolver(eventResolver);

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

	protected EquipmentEventMessage createMessage() throws Exception {
		EventResolver eventResolver = trendChartController.getEventResolver();

		String sourceId = eventResolver.getSourceId();

		String input = tfLoopbackValue.getText();
		String[] values = AppUtils.parseCsvInput(input);

		EquipmentEventMessage msg = new EquipmentEventMessage();
		msg.setSourceId(sourceId);
		msg.setValue(values[0]);
		msg.setReason(values[1]);
		String timestamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(), DomainUtils.OFFSET_DATE_TIME_8601);
		msg.setTimestamp(timestamp);

		return msg;
	}

	// service class for callbacks on received data
	protected class ResolutionService extends Service<Void> {
		private final String dataValue;
		private final String timestamp;
		private final String reason;

		protected ResolutionService(String dataValue, String timestamp, String reason) {
			this.dataValue = dataValue;
			this.timestamp = timestamp;
			this.reason = reason;
		}

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

				@Override
				protected Void call() {
					try {
						OffsetDateTime odt = DomainUtils.offsetDateTimeFromString(timestamp,
								DomainUtils.OFFSET_DATE_TIME_8601);
						trendChartController.invokeResolver(getApp().getAppContext(), dataValue, odt, reason);
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
