package org.point85.app.file;

import java.io.File;
import java.io.IOException;
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
import org.point85.domain.file.FileEventClient;
import org.point85.domain.file.FileEventListener;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.script.EventResolver;

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
import javafx.stage.FileChooser;

public class FileTrendController extends DesignerDialogController implements FileEventListener, DataSubscriber {
	// polling client
	private FileEventClient fileClient;

	// folder to start browing from
	private File initialDirectory;

	// trend chart
	private TrendChartController trendChartController;

	// trend chart pane
	private SplitPane spTrendChart;

	@FXML
	private Label lbSourceId;

	@FXML
	private Label lbHost;

	@FXML
	private TextField tfFilePath;

	@FXML
	private Button btTest;

	@FXML
	private Button btFileBrowser;

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

			lbHost.setText("");
		}
		return spTrendChart;
	}

	// images for buttons
	@Override
	protected void setImages() throws Exception {
		super.setImages();

		// write test
		btTest.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btTest.setContentDisplay(ContentDisplay.LEFT);

		// file browser
		btFileBrowser.setGraphic(ImageManager.instance().getImageView(Images.IMPORT));
		btFileBrowser.setContentDisplay(ContentDisplay.LEFT);
	}

	public void setEventResolver(EventResolver eventResolver) throws Exception {
		eventResolver.setWatchMode(true);
		trendChartController.setEventResolver(eventResolver);

		lbSourceId.setText(
				"Equipment: " + eventResolver.getEquipment().getName() + ", Source Id: " + eventResolver.getSourceId());

		lbHost.setText("Share: " + eventResolver.getDataSource().getHost());
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
		return fileClient != null ? true : false;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (fileClient != null) {
			return;
		}

		FileEventSource eventSource = (FileEventSource) trendChartController.getEventResolver().getDataSource();
		Integer period = trendChartController.getEventResolver().getUpdatePeriod();
		String sourceId = trendChartController.getEventResolver().getSourceId();

		List<String> sourceIds = new ArrayList<>();
		sourceIds.add(sourceId);

		List<Integer> pollingIntervals = new ArrayList<>();
		pollingIntervals.add(period);

		fileClient = new FileEventClient(this, eventSource, sourceIds, pollingIntervals);

		// add to context
		getApp().getAppContext().addFileEventClient(fileClient);

		// start the trend
		trendChartController.enableTrending(true);

		// start polling for events
		fileClient.startPolling();
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (fileClient == null) {
			return;
		}

		// stop polling
		fileClient.stopPolling();

		// remove from app context
		getApp().getAppContext().removeFileEventClient(fileClient);
		fileClient = null;

		// stop the trend
		trendChartController.onStopTrending();
	}

	// File request
	@Override
	public void resolveFileEvents(FileEventClient client, String sourceId, List<File> files) {
		for (File file : files) {
			handleFileEvent(fileClient, file);
		}
	}

	private void handleFileEvent(FileEventClient fileClient, File file) {
		if (fileClient == null) {
			return;
		}

		if (fileClient.fileIsProcessing(file)) {
			return;
		}

		ResolutionService service = new ResolutionService(fileClient, file);

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
			if (fileClient == null) {
				throw new Exception("The trend is not connected to a file server.");
			}

			EventResolver eventResolver = trendChartController.getEventResolver();

			String sourceId = eventResolver.getSourceId();
			File selectedFile = (File) tfFilePath.getUserData();

			if (selectedFile == null) {
				throw new Exception("The file to test must be selected first.");
			}

			// move the file to READY folder
			FileEventSource source = (FileEventSource) eventResolver.getDataSource();

			fileClient.moveFile(selectedFile, source, sourceId, FileEventClient.READY_FOLDER);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onBrowseFile() {
		// show file chooser
		FileChooser fileChooser = new FileChooser();

		if (initialDirectory != null) {
			fileChooser.setInitialDirectory(initialDirectory);
		}
		File selectedFile = fileChooser.showOpenDialog(null);

		if (selectedFile == null) {
			return;
		}
		initialDirectory = selectedFile.getParentFile();

		tfFilePath.setUserData(selectedFile);
		tfFilePath.setText(selectedFile.getAbsolutePath());
	}

	// service class for callbacks on received events
	private class ResolutionService extends Service<Void> {
		private final FileEventClient fileClient;
		private final File file;

		public ResolutionService(FileEventClient fileClient, File file) {
			this.fileClient = fileClient;
			this.file = file;
		}

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

				@Override
				protected Void call() {
					try {
						// default timestamp
						OffsetDateTime odt = fileClient.getFileService().extractTimestamp(file);

						// read contents from ready folder
						String content = fileClient.readFile(file);

						// in-process
						fileClient.moveFile(file, FileEventClient.READY_FOLDER, FileEventClient.PROCESSING_FOLDER);

						trendChartController.invokeResolver(getApp().getAppContext(), content, odt, null);

						// passed
						fileClient.moveFile(file, FileEventClient.PROCESSING_FOLDER, FileEventClient.PASS_FOLDER);
					} catch (Exception e) {
						// record the error
						try {
							// failed
							fileClient.moveFile(file, FileEventClient.PROCESSING_FOLDER, FileEventClient.FAIL_FOLDER,
									e);
						} catch (IOException ex) {
							Platform.runLater(() -> {
								AppUtils.showErrorDialog(ex);
							});
						}

						Platform.runLater(() -> {
							AppUtils.showErrorDialog(e);
						});
					} finally {
						fileClient.stopProcessing(file);
					}
					return null;
				}
			};
		}
	}
}
