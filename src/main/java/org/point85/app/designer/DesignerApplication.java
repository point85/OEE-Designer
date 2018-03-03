package org.point85.app.designer;

import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.LoaderFactory;
import org.point85.app.dashboard.DashboardController;
import org.point85.app.dashboard.DashboardDialogController;
import org.point85.app.http.HttpServerController;
import org.point85.app.http.HttpTrendController;
import org.point85.app.material.MaterialEditorController;
import org.point85.app.messaging.MessagingTrendController;
import org.point85.app.messaging.MqBrokerController;
import org.point85.app.opc.da.OpcDaBrowserController;
import org.point85.app.opc.da.OpcDaTrendController;
import org.point85.app.opc.ua.OpcUaBrowserController;
import org.point85.app.opc.ua.OpcUaTreeNode;
import org.point85.app.opc.ua.OpcUaTrendController;
import org.point85.app.reason.ReasonEditorController;
import org.point85.app.schedule.WorkScheduleEditorController;
import org.point85.app.script.ScriptResolverController;
import org.point85.app.uom.UomConversionController;
import org.point85.app.uom.UomEditorController;
import org.point85.app.web.WebServerController;
import org.point85.app.web.WebTrendController;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.http.HttpSource;
import org.point85.domain.messaging.MessagingSource;
import org.point85.domain.opc.da.OpcDaBrowserLeaf;
import org.point85.domain.opc.da.OpcDaClient;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.performance.EquipmentLoss;
import org.point85.domain.performance.EquipmentLossManager;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.ScriptResolver;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.web.WebSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DesignerApplication extends Application {
	// logger
	private final Logger logger = LoggerFactory.getLogger(getClass());

	// physical model controller
	private PhysicalModelController physicalModelController;

	// reason editor controller
	private ReasonEditorController reasonController;

	// material editor controller
	private MaterialEditorController materialController;

	// UOM editor controller
	private UomEditorController uomController;

	// work schedule editor controller
	private WorkScheduleEditorController scheduleController;

	// OPC DA data source browser
	private OpcDaBrowserController opcDaBrowserController;

	// OPC UA data source browser
	private OpcUaBrowserController opcUaBrowserController;

	// HTTP server editor
	private HttpServerController httpServerController;

	// RabbitMQ broker editor
	private MqBrokerController mqBrokerController;

	// web server editor
	private WebServerController webServerController;

	// data collection definition
	private DataCollectorController dataCollectorController;

	// script resolver controller
	private ScriptResolverController scriptController;

	// UOM conversion controller
	private UomConversionController uomConversionController;

	// OEE dashboard controller
	private DashboardDialogController dashboardDialogController;

	// script execution context
	private OeeContext appContext;

	@Override
	public void start(Stage primaryStage) {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Launching OEE Designer");
			}

			// Load root layout from fxml file.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("DesignerApplication.fxml"));
			AnchorPane mainLayout = (AnchorPane) loader.load();

			// Give the controller access to the main app.
			physicalModelController = loader.getController();
			physicalModelController.initialize(this);

			if (logger.isInfoEnabled()) {
				logger.info("Initialized physical model controller");
			}

			// create application context
			appContext = new OeeContext();

			if (logger.isInfoEnabled()) {
				logger.info("Created OEE context");
			}

			// Show the scene containing the root layout.
			Scene scene = new Scene(mainLayout);

			// UI
			primaryStage.setTitle("OEE Designer");
			primaryStage.getIcons().add(Images.point85Image);
			primaryStage.setScene(scene);
			primaryStage.show();

			if (logger.isInfoEnabled()) {
				logger.info("Showed primary stage");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			// OPC DA
			if (getOpcDaClient() != null) {
				getOpcDaClient().disconnect();
			}

			// OPC UA
			if (getOpcUaClient() != null) {
				getOpcUaClient().disconnect();
			}

			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// display the reason editor as a dialog
	public Reason showReasonEditor() throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		if (reasonController == null) {
			FXMLLoader loader = LoaderFactory.reasonEditorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("Edit Reason");
			dialogStage.initModality(Modality.NONE);
			// dialogStage.initOwner(primaryStage);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			reasonController = loader.getController();
			reasonController.setDialogStage(dialogStage);
			reasonController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		reasonController.getDialogStage().showAndWait();

		return reasonController.getSelectedReason();
	}

	// display the material editor as a dialog
	public Material showMaterialEditor() throws Exception {
		if (this.materialController == null) {
			FXMLLoader loader = LoaderFactory.materialEditorLoader();
			AnchorPane pane = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("Edit Material");
			dialogStage.initModality(Modality.NONE);
			// dialogStage.initOwner(primaryStage);
			Scene scene = new Scene(pane);
			dialogStage.setScene(scene);

			// get the controller
			materialController = loader.getController();
			materialController.setDialogStage(dialogStage);
			materialController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		materialController.getDialogStage().showAndWait();

		return materialController.getSelectedMaterial();
	}

	// display the UOM editor as a dialog
	public UnitOfMeasure showUomEditor() throws Exception {
		if (this.uomController == null) {
			// Load the fxml file and create a new stage for the pop-up dialog.
			FXMLLoader loader = LoaderFactory.uomEditorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("Edit Unit Of Measure");
			dialogStage.initModality(Modality.NONE);
			// dialogStage.initOwner(primaryStage);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			uomController = loader.getController();
			uomController.setDialogStage(dialogStage);

			uomController.initializeApp(this);
		}

		// Show the dialog and wait until the user closes it
		if (!uomController.getDialogStage().isShowing()) {
			uomController.getDialogStage().showAndWait();
		}

		return uomController.getSelectedUom();
	}

	// display the work schedule editor as a dialog
	WorkSchedule showScheduleEditor() throws Exception {
		if (this.scheduleController == null) {
			// Load the fxml file and create a new stage for the pop-up dialog.
			FXMLLoader loader = LoaderFactory.scheduleEditorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("Edit Work Schedule");
			dialogStage.initModality(Modality.NONE);
			// dialogStage.initOwner(primaryStage);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			scheduleController = loader.getController();
			scheduleController.setDialogStage(dialogStage);
			scheduleController.initializeApp(this);
		}

		// Show the dialog and wait until the user closes it
		scheduleController.getDialogStage().showAndWait();

		return scheduleController.getSelectedSchedule();
	}

	OpcDaBrowserLeaf showOpcDaDataSourceBrowser() throws Exception {
		if (opcDaBrowserController == null) {
			// Load the fxml file and create a new stage for the pop-up dialog.
			FXMLLoader loader = LoaderFactory.opdDaBrowserLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("Browse OPC DA Data Source");
			dialogStage.initModality(Modality.NONE);
			// dialogStage.initOwner(primaryStage);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			opcDaBrowserController = loader.getController();
			opcDaBrowserController.setDialogStage(dialogStage);
			opcDaBrowserController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		opcDaBrowserController.getDialogStage().showAndWait();

		return opcDaBrowserController.getSelectedTag();
	}

	OpcUaTreeNode showOpcUaDataSourceBrowser() throws Exception {
		if (opcUaBrowserController == null) {
			// Load the fxml file and create a new stage for the pop-up dialog.
			FXMLLoader loader = LoaderFactory.opdUaBrowserLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("Browse OPC UA Data Source");
			dialogStage.initModality(Modality.NONE);
			// dialogStage.initOwner(primaryStage);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			opcUaBrowserController = loader.getController();
			opcUaBrowserController.setDialogStage(dialogStage);
			opcUaBrowserController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		opcUaBrowserController.getDialogStage().showAndWait();

		return opcUaBrowserController.getSelectedNodeId();
	}

	String showScriptEditor(ScriptResolver scriptResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		if (scriptController == null) {
			FXMLLoader loader = LoaderFactory.scriptResolverLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("Edit Script Resolver");
			dialogStage.initModality(Modality.NONE);
			// dialogStage.initOwner(primaryStage);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			scriptController = loader.getController();
			scriptController.setDialogStage(dialogStage);
			scriptController.initialize(this, scriptResolver);
		}

		// display old script
		scriptController.showFunctionScript(scriptResolver);

		// Show the dialog and wait until the user closes it
		scriptController.getDialogStage().showAndWait();

		return scriptController.getResolver().getScript();
	}

	HttpSource showHttpServerEditor() throws Exception {
		FXMLLoader loader = LoaderFactory.httpServerLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle("Edit HTTP Servers");
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		httpServerController = loader.getController();
		httpServerController.setDialogStage(dialogStage);
		httpServerController.initialize(this);

		// Show the dialog and wait until the user closes it
		httpServerController.getDialogStage().showAndWait();

		return httpServerController.getSource();
	}

	MessagingSource showRmqBrokerEditor() throws Exception {
		FXMLLoader loader = LoaderFactory.mqBrokerLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle("Edit RabbitMQ Brokers");
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		mqBrokerController = loader.getController();
		mqBrokerController.setDialogStage(dialogStage);
		mqBrokerController.initialize(this);

		// Show the dialog and wait until the user closes it
		mqBrokerController.getDialogStage().showAndWait();

		return mqBrokerController.getSource();
	}

	WebSource showWebServerEditor() throws Exception {
		FXMLLoader loader = LoaderFactory.webServerLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle("Edit Web Servers");
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		webServerController = loader.getController();
		webServerController.setDialogStage(dialogStage);
		webServerController.initialize(this);

		// Show the dialog and wait until the user closes it
		webServerController.getDialogStage().showAndWait();

		return webServerController.getSource();
	}

	DataCollector showCollectorEditor() throws Exception {
		if (dataCollectorController == null) {
			FXMLLoader loader = LoaderFactory.dataCollectorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("Edit Collector Definitions");
			dialogStage.initModality(Modality.WINDOW_MODAL);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			dataCollectorController = loader.getController();
			dataCollectorController.setDialogStage(dialogStage);
			dataCollectorController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		dataCollectorController.getDialogStage().showAndWait();

		return dataCollectorController.getCollectorDefinition();
	}

	void showUomConverter() throws Exception {
		if (uomConversionController == null) {
			FXMLLoader loader = LoaderFactory.uomConversionLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog stage
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("Unit of Measure Converter");
			dialogStage.initModality(Modality.NONE);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			uomConversionController = loader.getController();
			uomConversionController.setDialogStage(dialogStage);
			uomConversionController.initializeApp(this);
		}

		// Show the dialog and wait until the user closes it
		uomConversionController.getDialogStage().showAndWait();
	}

	void showOpcDaTrendDialog(ScriptResolver scriptResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = LoaderFactory.opcDaTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle("OPC DA Item Trend");
		dialogStage.initModality(Modality.NONE);
		// dialogStage.initOwner(primaryStage);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		OpcDaTrendController opcDaTrendController = loader.getController();
		opcDaTrendController.setDialogStage(dialogStage);
		opcDaTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = opcDaTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		opcDaTrendController.setScriptResolver(scriptResolver);

		// show the window
		opcDaTrendController.getDialogStage().show();
	}

	void showOpcUaTrendDialog(ScriptResolver scriptResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = LoaderFactory.opcUaTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle("OPC UA Item Trend");
		dialogStage.initModality(Modality.NONE);
		// dialogStage.initOwner(primaryStage);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		OpcUaTrendController opcUaTrendController = loader.getController();
		opcUaTrendController.setDialogStage(dialogStage);
		opcUaTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = opcUaTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		opcUaTrendController.setScriptResolver(scriptResolver);

		// show the window
		opcUaTrendController.getDialogStage().show();
	}

	void showHttpTrendDialog(ScriptResolver scriptResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = LoaderFactory.httpTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle("HTTP Event Trend");
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		HttpTrendController httpTrendController = loader.getController();
		httpTrendController.setDialogStage(dialogStage);
		httpTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = httpTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		httpTrendController.setScriptResolver(scriptResolver);

		// start HTTP server
		httpTrendController.onStartServer();

		// show the window
		httpTrendController.getDialogStage().show();
	}

	void showWebTrendDialog(ScriptResolver scriptResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = LoaderFactory.webTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle("Web Event Trend");
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		WebTrendController webTrendController = loader.getController();
		webTrendController.setDialogStage(dialogStage);
		webTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = webTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		webTrendController.setScriptResolver(scriptResolver);

		// start HTTP server
		// webTrendController.onStartServer();

		// show the window
		webTrendController.getDialogStage().show();
	}

	void showMessagingTrendDialog(ScriptResolver scriptResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = LoaderFactory.messagingTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle("Messaging Event Trend");
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		MessagingTrendController messagingTrendController = loader.getController();
		messagingTrendController.setDialogStage(dialogStage);
		messagingTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = messagingTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		messagingTrendController.setScriptResolver(scriptResolver);

		// subscribe to broker
		messagingTrendController.subscribeToDataSource();

		// show the window
		messagingTrendController.getDialogStage().show();
	}

	public PhysicalModelController getPhysicalModelController() {
		return this.physicalModelController;
	}

	public OpcDaClient getOpcDaClient() {
		OpcDaClient client = appContext.getOpcDaClient();

		if (client == null) {
			client = new OpcDaClient();
			appContext.getOpcDaClients().add(client);
		}
		return client;
	}

	OpcDaBrowserController getOpcDaBrowserController() {
		return this.opcDaBrowserController;
	}

	OpcUaBrowserController getOpcUaBrowserController() {
		return this.opcUaBrowserController;
	}

	MaterialEditorController getMaterialController() {
		return this.materialController;
	}

	ReasonEditorController getReasonController() {
		return this.reasonController;
	}

	ScriptResolverController getResolverController() {
		return this.scriptController;
	}

	WorkScheduleEditorController getScheduleController() {
		return this.scheduleController;
	}

	public OeeContext getAppContext() {
		return appContext;
	}

	// display the OEE dashboard as a dialog
	void showOeeDashboard() throws Exception {
		if (this.dashboardDialogController == null) {
			FXMLLoader dialogLoader = LoaderFactory.dashboardDialogLoader();
			AnchorPane pane = (AnchorPane) dialogLoader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle("OEE Dashboard");
			dialogStage.initModality(Modality.NONE);
			Scene scene = new Scene(pane);
			dialogStage.setScene(scene);

			// get the controller
			dashboardDialogController = dialogLoader.getController();
			dashboardDialogController.setDialogStage(dialogStage);

			// load the content
			FXMLLoader dashboardLoader = LoaderFactory.dashboardLoader();
			SplitPane spDashboard = (SplitPane) dashboardLoader.getRoot();

			pane.getChildren().add(0, spDashboard);

			AnchorPane.setTopAnchor(spDashboard, 0.0);
			AnchorPane.setBottomAnchor(spDashboard, 50.0);
			AnchorPane.setLeftAnchor(spDashboard, 0.0);
			AnchorPane.setRightAnchor(spDashboard, 0.0);

			DashboardController dashboardController = dashboardLoader.getController();
			dashboardDialogController.setDashboardController(dashboardController);

		}

		PlantEntity entity = this.getPhysicalModelController().getSelectedEntity();

		if (!(entity instanceof Equipment)) {
			throw new Exception("Equipment must be selected first.");
		}

		EquipmentLoss equipmentLoss = EquipmentLossManager.getEquipmentLoss((Equipment) entity);
		dashboardDialogController.getDashboardController().setEquipmentLoss(equipmentLoss);
		dashboardDialogController.getDashboardController().displayLosses();

		// Show the dialog and wait until the user closes it
		dashboardDialogController.getDialogStage().showAndWait();
	}

	public UaOpcClient getOpcUaClient() {
		UaOpcClient client = appContext.getOpcUaClient();

		if (client == null) {
			client = new UaOpcClient();
			appContext.getOpcUaClients().add(client);
		}
		return client;
	}

	/**
	 * Main entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
