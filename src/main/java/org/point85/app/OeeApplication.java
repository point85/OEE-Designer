package org.point85.app;

import org.apache.log4j.PropertyConfigurator;
import org.point85.app.collector.ClientTestApplication;
import org.point85.app.collector.CollectorApplication;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.monitor.MonitorApplication;
import org.point85.domain.DomainUtils;
import org.point85.domain.persistence.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class OeeApplication extends Application {
	// JFX applications
	private static final String DESIGNER_APP = "DESIGNER";
	private static final String MONITOR_APP = "MONITOR";
	private static final String COLLECTOR_APP = "COLLECTOR";
	private static final String TESTER_APP = "TESTER";

	private static final int IDX_APP = 0;
	private static final int IDX_JDBC = 1;
	private static final int IDX_USER = 2;
	private static final int IDX_PASSWORD = 3;

	// logger
	private static final Logger logger = LoggerFactory.getLogger(OeeApplication.class);

	// Designer application
	private DesignerApplication designerApp;

	// Monitor application
	private MonitorApplication monitorApp;

	// collector application
	private CollectorApplication collectorApp;

	// client test application
	private ClientTestApplication clientTestApp;

	// show splash screen during database connection time
	private boolean showSplash = true;

	// splash layout
	private Pane splashLayout;
	private ProgressIndicator loadProgress;
	private Label progressText;
	private Stage mainStage;
	private static final int SPLASH_WIDTH = 650;
	private static final int SPLASH_HEIGHT = 225;

	@Override
	public void start(Stage primaryStage) throws Exception {
		if (showSplash) {
			final Task<String> startTask = new Task<String>() {
				@Override
				protected String call() throws InterruptedException {
					// updateProgress(1, 2);
					updateMessage("Connecting to database.");
					PersistenceService.instance().getEntityManagerFactory();
					updateMessage("Connected");
					return "OK";
				}
			};

			showSplash(primaryStage, startTask, () -> showMainStage(null));
			new Thread(startTask).start();
		} else {
			showMainStage(primaryStage);
		}
	}

	@Override
	public void stop() {
		try {
			if (designerApp != null) {
				designerApp.stop();
			} else if (monitorApp != null) {
				monitorApp.stop();
			} else if (collectorApp != null) {
				collectorApp.stop();
			} else if (clientTestApp != null) {
				clientTestApp.stop();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	@Override
	public void init() throws Exception {
		if (showSplash) {
			ImageView splash = ImageManager.instance().getImageView(Images.SPLASH);

			//loadProgress = new ProgressIndicator();
			//loadProgress.setPrefWidth(SPLASH_WIDTH - 20);
			progressText = new Label("Point85 Designer");
			progressText.setPrefWidth(SPLASH_WIDTH - 20);
			splashLayout = new VBox();
			//splashLayout.getChildren().addAll(splash, loadProgress, progressText);
			splashLayout.getChildren().addAll(splash, progressText);
			//splashLayout.getChildren().addAll(progressText);
			progressText.setAlignment(Pos.CENTER);
			splashLayout.setStyle("-fx-padding: 5; " + "-fx-background-color: cornsilk; " + "-fx-border-width:5; "
					+ "-fx-border-color: " + "linear-gradient(" + "to bottom, " + "chocolate, "
					+ "derive(chocolate, 50%)" + ");");
			splashLayout.setEffect(new DropShadow());
		}
	}

	private void showSplash(final Stage initStage, Task<String> task, InitCompletionHandler initCompletionHandler) {
		progressText.textProperty().bind(task.messageProperty());
		//loadProgress.progressProperty().bind(task.progressProperty());
		task.stateProperty().addListener((observableValue, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED) {
				//loadProgress.progressProperty().unbind();
				//loadProgress.setProgress(1);
				initStage.toFront();
				FadeTransition fadeSplash = new FadeTransition(Duration.seconds(1.2), splashLayout);
				fadeSplash.setFromValue(1.0);
				fadeSplash.setToValue(0.0);
				fadeSplash.setOnFinished(actionEvent -> initStage.hide());
				fadeSplash.play();

				initCompletionHandler.complete();
			} 
		});

		Scene splashScene = new Scene(splashLayout, Color.TRANSPARENT);
		final Rectangle2D bounds = Screen.getPrimary().getBounds();
		initStage.setScene(splashScene);
		initStage.setX(bounds.getMinX() + bounds.getWidth() / 2 - SPLASH_WIDTH / 2);
		initStage.setY(bounds.getMinY() + bounds.getHeight() / 2 - SPLASH_HEIGHT / 2);
		initStage.initStyle(StageStyle.TRANSPARENT);
		initStage.setAlwaysOnTop(true);
		initStage.show();
	}

	private void showMainStage(final Stage stage) {
		Parameters parameters = getParameters();

		String appId = parameters.getRaw().get(IDX_APP);

		if (logger.isInfoEnabled()) {
			logger.info("Starting application " + appId);
		}

		if (stage == null) {
			mainStage = new Stage(StageStyle.DECORATED);
		} else {
			mainStage = stage;
		}

		if (appId.equals(DESIGNER_APP)) {
			designerApp = new DesignerApplication();
			designerApp.start(mainStage);
		} else if (appId.equals(MONITOR_APP)) {
			monitorApp = new MonitorApplication();
			monitorApp.start(mainStage);
		} else if (appId.equals(COLLECTOR_APP)) {
			collectorApp = new CollectorApplication();
			collectorApp.start(mainStage);
		} else if (appId.equals(TESTER_APP)) {
			clientTestApp = new ClientTestApplication();
			clientTestApp.start(mainStage);
		}
	}

	public interface InitCompletionHandler {
		void complete();
	}

	/**
	 * Main entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// configuration folder
		String configDir = System.getProperty(DomainUtils.CONFIG_DIR);
		
		// configure log4j
		PropertyConfigurator.configure(configDir + "/logging/log4j.properties");

		// create the EMF
		if (logger.isInfoEnabled()) {
			logger.info("Initializing persistence service with args: " + args[IDX_JDBC] + ", " + args[IDX_USER] + ", "
					+ args[IDX_PASSWORD]);
		}
		PersistenceService.instance().initialize(args[IDX_JDBC], args[IDX_USER], args[IDX_PASSWORD]);

		// start GUI
		launch(args);
	}

}
