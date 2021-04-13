package org.point85.app.proficy;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.proficy.ProficyClient;
import org.point85.domain.proficy.ProficyEventListener;
import org.point85.domain.proficy.ProficySource;
import org.point85.domain.proficy.TagData;
import org.point85.domain.proficy.TagDataType;
import org.point85.domain.proficy.TagSample;
import org.point85.domain.script.EventResolver;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;

public class ProficyTrendController extends DesignerDialogController implements ProficyEventListener, DataSubscriber {
	// Proficy client
	private ProficyClient proficyClient;

	// trend chart
	private TrendChartController trendChartController;

	// trend chart pane
	private SplitPane spTrendChart;

	@FXML
	private Label lbSourceId;

	@FXML
	private Label lbProficyDescription;

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
		}
		return spTrendChart;
	}

	public void setEventResolver(EventResolver eventResolver) {
		eventResolver.setWatchMode(true);
		trendChartController.setEventResolver(eventResolver);

		lbSourceId.setText(DesignerLocalizer.instance().getLangString("event.source",
				eventResolver.getEquipment().getName(), eventResolver.getSourceId()));
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

	@Override
	public boolean isSubscribed() {
		return proficyClient != null;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (proficyClient != null) {
			return;
		}

		ProficySource proficySource = (ProficySource) trendChartController.getEventResolver().getDataSource();
		Integer period = trendChartController.getEventResolver().getUpdatePeriod();
		String sourceId = trendChartController.getEventResolver().getSourceId();

		List<String> sourceIds = new ArrayList<>();
		sourceIds.add(sourceId);

		List<Integer> pollingIntervals = new ArrayList<>();
		pollingIntervals.add(period);

		proficyClient = new ProficyClient(this, proficySource, sourceIds, pollingIntervals);

		// add to context
		getApp().getAppContext().addProficyClient(proficyClient);

		// start the trend
		trendChartController.enableTrending(true);

		lbProficyDescription.setText(proficySource.getDescription());

		// start polling for events
		proficyClient.setWatchMode(true);
		proficyClient.startPolling();
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (proficyClient == null) {
			return;
		}

		// stop job schedule
		proficyClient.stopPolling();

		// remove from app context
		getApp().getAppContext().removeProficyClient(proficyClient);
		proficyClient = null;

		// stop the trend
		trendChartController.onStopTrending();
	}

	// service class for call backs on received events
	private class ResolutionService extends Service<Void> {
		private TagData tagData;

		public ResolutionService(TagData tagData) {
			this.tagData = tagData;
		}

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

				@Override
				protected Void call() {
					try {
						TagDataType dataType = tagData.getEnumeratedType();

						// samples are in chronological order
						for (TagSample sample : tagData.getSamples()) {
							// UTC time in local zone
							OffsetDateTime odt = sample.getTimeStampTime();
							Object sourceValue = sample.getTypedValue(dataType);

							// resolve this data
							trendChartController.invokeResolver(getApp().getAppContext(), sourceValue, odt, null);
						}

					} catch (Exception e) {
						Platform.runLater(() -> AppUtils.showErrorDialog(e));
					}
					return null;
				}
			};
		}
	}

	@Override
	public void onProficyEvent(TagData tagData) {
		ResolutionService service = new ResolutionService(tagData);

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
