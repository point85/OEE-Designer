package org.point85.app.cron;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.cron.CronEventClient;
import org.point85.domain.cron.CronEventListener;
import org.point85.domain.cron.CronEventSource;
import org.point85.domain.script.EventResolver;
import org.quartz.JobExecutionContext;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;

public class CronTrendController extends DesignerDialogController implements CronEventListener, DataSubscriber {
	// Cron client
	private CronEventClient cronClient;

	// trend chart
	private TrendChartController trendChartController;

	// trend chart pane
	private SplitPane spTrendChart;

	@FXML
	private Label lbSourceId;

	@FXML
	private Label lbJob;

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

			lbJob.setText(null);
		}
		return spTrendChart;
	}

	public void setEventResolver(EventResolver eventResolver) {
		eventResolver.setWatchMode(true);
		trendChartController.setEventResolver(eventResolver);

		lbSourceId.setText(DesignerLocalizer.instance().getLangString("event.source",
				eventResolver.getEquipment().getName(), eventResolver.getSourceId()));

		lbJob.setText(DesignerLocalizer.instance().getLangString("cron.job", eventResolver.getDataSource().getName()));
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
		return cronClient != null;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (cronClient != null) {
			return;
		}

		CronEventSource eventSource = (CronEventSource) trendChartController.getEventResolver().getDataSource();
		String expression = eventSource.getCronExpression();
		String sourceId = trendChartController.getEventResolver().getSourceId();

		List<String> sourceIds = new ArrayList<>();
		sourceIds.add(sourceId);

		List<String> expressions = new ArrayList<>();
		expressions.add(expression);

		cronClient = new CronEventClient(this, eventSource, sourceIds, expressions);

		// add to context
		getApp().getAppContext().addCronEventClient(cronClient);

		// start the trend
		trendChartController.enableTrending(true);

		// start job
		cronClient.scheduleJobs();
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (cronClient == null) {
			return;
		}

		// stop job schedule
		CronEventSource eventSource = (CronEventSource) trendChartController.getEventResolver().getDataSource();
		cronClient.unscheduleJob(eventSource.getName());

		cronClient.shutdownScheduler();

		// remove from app context
		getApp().getAppContext().removeCronEventClient(cronClient);
		cronClient = null;

		// stop the trend
		trendChartController.onStopTrending();
	}

	private void handleCronEvent(JobExecutionContext context) {
		ResolutionService service = new ResolutionService();

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
	private void onTest() {
		try {
			if (cronClient == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("cron.no.jobs"));
			}

			cronClient.scheduleJobs();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// service class for call backs on received events
	private class ResolutionService extends Service<Void> {
		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

				@Override
				protected Void call() {
					try {
						// default timestamp
						OffsetDateTime odt = OffsetDateTime.now();

						trendChartController.invokeResolver(getApp().getAppContext(), odt, odt, null);
					} catch (Exception e) {
						Platform.runLater(() -> AppUtils.showErrorDialog(e));
					}
					return null;
				}
			};
		}
	}

	@Override
	public void resolveCronEvent(JobExecutionContext context) {
		handleCronEvent(context);
	}
}
