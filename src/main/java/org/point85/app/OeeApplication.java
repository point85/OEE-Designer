package org.point85.app;

import javax.persistence.EntityManager;

import org.point85.app.collector.CollectorApplication;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.monitor.MonitorApplication;
import org.point85.app.operator.OperatorApplication;
import org.point85.app.tester.TesterApplication;
import org.point85.domain.DomainUtils;
import org.point85.domain.persistence.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
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
	private static final String OPERATOR_APP = "OPERATOR";

	// program args
	private static final int IDX_APP = 0;
	private static final int IDX_JDBC = 1;
	private static final int IDX_USER = 2;
	private static final int IDX_PASSWORD = 3;
	private static final int IDX_COLLECTOR = 4;

	// logger
	private static final Logger logger = LoggerFactory.getLogger(OeeApplication.class);

	// Designer application
	private DesignerApplication designerApp;

	// Monitor application
	private MonitorApplication monitorApp;

	// Collector application
	private CollectorApplication collectorApp;

	// Tester application
	private TesterApplication testerApp;

	// Operator application
	private OperatorApplication operatorApp;

	// splash screen
	private VBox splashLayout;

	@Override
	public void start(Stage primaryStage) throws Exception {
		final Task<String> startTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				// wait for a database connection by requesting an EntityManager
				EntityManager em = PersistenceService.instance().getEntityManager();
				em.close();
				return getClass().getSimpleName();
			}
		};

		// start the database connection
		new Thread(startTask).start();

		// show the dialog and show the main stage when done
		showSplash(primaryStage, startTask, () -> showMainStage(null));
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
			} else if (testerApp != null) {
				testerApp.stop();
			} else if (operatorApp != null) {
				operatorApp.stop();
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		System.exit(0);
	}

	private void showSplash(final Stage stage, Task<String> task, InitCompletionHandler initCompletionHandler)
			throws Exception {

		// completion listener
		task.stateProperty().addListener((observableValue, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED) {
				stage.toFront();

				// fade out to main app
				FadeTransition fadeSplash = new FadeTransition(Duration.seconds(0.5), splashLayout);
				fadeSplash.setFromValue(1.0);
				fadeSplash.setToValue(0.0);
				fadeSplash.setOnFinished(actionEvent -> stage.hide());
				fadeSplash.play();

				initCompletionHandler.complete();
			} else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
				stage.hide();
			}
		});

		// load dialog
		FXMLLoader loader = FXMLLoaderFactory.splashLoader();
		SplashController splashController = loader.getController();
		splashController.initialize();

		splashLayout = (VBox) loader.getRoot();

		Scene splashScene = new Scene(splashLayout, Color.TRANSPARENT);
		stage.setScene(splashScene);

		// center in main stage
		final Rectangle2D bounds = Screen.getPrimary().getBounds();
		stage.setX(bounds.getMinX() + bounds.getWidth() / 2.0d - SplashController.SPLASH_WIDTH / 2.0d);
		stage.setY(bounds.getMinY() + bounds.getHeight() / 2.0d - SplashController.SPLASH_HEIGHT / 2.0d);
		stage.initStyle(StageStyle.TRANSPARENT);
		stage.setAlwaysOnTop(true);
		stage.show();
	}

	private void showMainStage(final Stage stage) {
		Parameters parameters = getParameters();

		String appId = parameters.getRaw().get(IDX_APP);

		if (logger.isInfoEnabled()) {
			logger.info("Starting application " + appId);
		}

		String collectorName = null;
		if (parameters.getRaw().size() > IDX_COLLECTOR) {
			collectorName = parameters.getRaw().get(IDX_COLLECTOR);
		}

		if (collectorName != null && logger.isInfoEnabled()) {
			logger.info("Collector " + collectorName + " specified.");
		}

		Stage mainStage = null;
		if (stage == null) {
			mainStage = new Stage(StageStyle.DECORATED);
		} else {
			mainStage = stage;
		}

		// show the configured app
		if (appId.equals(DESIGNER_APP)) {
			designerApp = new DesignerApplication();
			designerApp.start(mainStage);
		} else if (appId.equals(MONITOR_APP)) {
			monitorApp = new MonitorApplication();
			monitorApp.start(mainStage);
		} else if (appId.equals(COLLECTOR_APP)) {
			collectorApp = new CollectorApplication(collectorName);
			collectorApp.start(mainStage);
		} else if (appId.equals(TESTER_APP)) {
			testerApp = new TesterApplication();
			testerApp.start(mainStage);
		} else if (appId.equals(OPERATOR_APP)) {
			operatorApp = new OperatorApplication();
			operatorApp.start(mainStage);
		}
	}

	public interface InitCompletionHandler {
		void complete();
	}

	/**
	 * Main entry point
	 * 
	 * @param args Program arguments
	 */
	public static void main(String[] args) {
		if (logger.isInfoEnabled()) {
			logger.info("JVM: " + DomainUtils.getJVMInfo());
			logger.info("JavaFX version: " + System.getProperty("javafx.version"));
		}

		if (args.length < IDX_USER) {
			logger.error("The application, jdbc connection string and user name must be specified.");
			return;
		}

		String password = args.length > IDX_PASSWORD ? args[IDX_PASSWORD] : null;

		// create the EMF
		if (logger.isInfoEnabled()) {
			logger.info("Running application " + args[IDX_APP] + " for " + DomainUtils.getVersionInfo());
			logger.info("Initializing persistence service with args: " + args[IDX_JDBC] + ", " + args[IDX_USER] + ", "
					+ password);
		}
		PersistenceService.instance().initialize(args[IDX_JDBC], args[IDX_USER], password);

		// start GUI
		launch(args);
	}
}
