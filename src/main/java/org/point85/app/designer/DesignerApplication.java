package org.point85.app.designer;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.cron.CronEditorController;
import org.point85.app.cron.CronHelpController;
import org.point85.app.cron.CronTrendController;
import org.point85.app.dashboard.DashboardController;
import org.point85.app.dashboard.DashboardDialogController;
import org.point85.app.db.DatabaseServerController;
import org.point85.app.db.DatabaseTrendController;
import org.point85.app.email.EmailServerController;
import org.point85.app.email.EmailTrendController;
import org.point85.app.file.FileShareController;
import org.point85.app.file.FileTrendController;
import org.point85.app.http.HttpServerController;
import org.point85.app.http.HttpTrendController;
import org.point85.app.material.MaterialEditorController;
import org.point85.app.messaging.JmsTrendController;
import org.point85.app.messaging.KafkaServerController;
import org.point85.app.messaging.KafkaTrendController;
import org.point85.app.messaging.MqBrokerController;
import org.point85.app.messaging.MqttTrendController;
import org.point85.app.messaging.RmqTrendController;
import org.point85.app.modbus.ModbusMasterController;
import org.point85.app.modbus.ModbusTrendController;
import org.point85.app.opc.da.OpcDaBrowserController;
import org.point85.app.opc.da.OpcDaTrendController;
import org.point85.app.opc.ua.OpcUaBrowserController;
import org.point85.app.opc.ua.OpcUaTreeNode;
import org.point85.app.opc.ua.OpcUaTrendController;
import org.point85.app.reason.ReasonEditorController;
import org.point85.app.schedule.WorkScheduleEditorController;
import org.point85.app.script.EventResolverController;
import org.point85.app.uom.UomConversionController;
import org.point85.app.uom.UomEditorController;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.cron.CronEventSource;
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.email.EmailSource;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.http.HttpSource;
import org.point85.domain.kafka.KafkaSource;
import org.point85.domain.modbus.ModbusMaster;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.opc.da.DaOpcClient;
import org.point85.domain.opc.da.OpcDaBrowserLeaf;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeContext;
import org.point85.domain.uom.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DesignerApplication {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(DesignerApplication.class);

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

	// script resolver controller
	private EventResolverController scriptController;

	// UOM conversion controller
	private UomConversionController uomConversionController;

	// Modbus editor
	private ModbusMasterController modbusController;

	// script execution context
	private OeeContext appContext;

	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = FXMLLoaderFactory.designerApplicationLoader();
			AnchorPane mainLayout = (AnchorPane) loader.getRoot();

			// Give the controller access to the main app.
			physicalModelController = loader.getController();
			physicalModelController.initialize(this);

			// create application context
			appContext = new OeeContext();

			// Show the scene containing the root layout.
			Scene scene = new Scene(mainLayout);

			// UI
			primaryStage.setTitle(DesignerLocalizer.instance().getLangString("designer.app.title"));
			primaryStage.getIcons().add(ImageManager.instance().getImage(Images.POINT85));
			primaryStage.setScene(scene);
			primaryStage.show();

			if (logger.isInfoEnabled()) {
				logger.info("Populating top entity nodes.");
			}

			Platform.runLater(() -> {
				try {
					physicalModelController.populateTopEntityNodes();
				} catch (Exception e) {
					AppUtils.showErrorDialog(
							DesignerLocalizer.instance().getErrorString("no.entities", e.getMessage()));
				}
			});

		} catch (Exception e) {
			logger.error(e.getMessage());
			stop();
		}
	}

	public void stop() {
		try {
			// JPA service
			PersistenceService.instance().close();

			// OPC DA
			if (getOpcDaClient() != null) {
				getOpcDaClient().disconnect();
			}

			// OPC UA
			if (getOpcUaClient() != null) {
				getOpcUaClient().disconnect();
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	// display the reason editor as a dialog
	public Reason showReasonEditor() throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		if (reasonController == null) {
			FXMLLoader loader = FXMLLoaderFactory.reasonEditorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("reason.editor.title"));
			dialogStage.initModality(Modality.NONE);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			reasonController = loader.getController();
			reasonController.setDialogStage(dialogStage);
			reasonController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		if (!reasonController.getDialogStage().isShowing()) {
			reasonController.getDialogStage().showAndWait();
		}

		return reasonController.getSelectedReason();
	}

	// display the material editor as a dialog
	public Material showMaterialEditor() throws Exception {
		if (materialController == null) {
			FXMLLoader loader = FXMLLoaderFactory.materialEditorLoader();
			AnchorPane pane = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("material.editor.title"));
			dialogStage.initModality(Modality.NONE);

			Scene scene = new Scene(pane);
			dialogStage.setScene(scene);

			// get the controller
			materialController = loader.getController();
			materialController.setDialogStage(dialogStage);
			materialController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		if (!materialController.getDialogStage().isShowing()) {
			materialController.getDialogStage().showAndWait();
		}

		return materialController.getSelectedMaterial();
	}

	// display the UOM editor as a dialog
	public UnitOfMeasure showUomEditor() throws Exception {
		if (this.uomController == null) {
			// Load the fxml file and create a new stage for the pop-up dialog.
			FXMLLoader loader = FXMLLoaderFactory.uomEditorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("uom.editor.title"));
			dialogStage.initModality(Modality.NONE);

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
			FXMLLoader loader = FXMLLoaderFactory.scheduleEditorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("schedule.editor.title"));
			dialogStage.initModality(Modality.NONE);

			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			scheduleController = loader.getController();
			scheduleController.setDialogStage(dialogStage);
			scheduleController.initializeApp(this);
		}

		// Show the dialog and wait until the user closes it
		if (!scheduleController.getDialogStage().isShowing()) {
			scheduleController.getDialogStage().showAndWait();
		}

		return scheduleController.getSelectedSchedule();
	}

	OpcDaBrowserLeaf showOpcDaDataSourceBrowser() throws Exception {
		if (opcDaBrowserController == null) {
			// Load the fxml file and create a new stage for the pop-up dialog.
			FXMLLoader loader = FXMLLoaderFactory.opdDaBrowserLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("opc.da.title"));
			dialogStage.initModality(Modality.NONE);

			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			opcDaBrowserController = loader.getController();
			opcDaBrowserController.setDialogStage(dialogStage);
			opcDaBrowserController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		if (!opcDaBrowserController.getDialogStage().isShowing()) {
			opcDaBrowserController.getDialogStage().showAndWait();
		}

		return opcDaBrowserController.getSelectedTag();
	}

	OpcUaTreeNode showOpcUaDataSourceBrowser() throws Exception {
		if (opcUaBrowserController == null) {
			// Load the fxml file and create a new stage for the pop-up dialog.
			FXMLLoader loader = FXMLLoaderFactory.opdUaBrowserLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("opc.ua.title"));
			dialogStage.initModality(Modality.NONE);

			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			opcUaBrowserController = loader.getController();
			opcUaBrowserController.setDialogStage(dialogStage);
			opcUaBrowserController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		if (!opcUaBrowserController.getDialogStage().isShowing()) {
			opcUaBrowserController.getDialogStage().showAndWait();
		}

		return opcUaBrowserController.getSelectedNodeId();
	}

	String showScriptEditor(EventResolver eventResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		if (scriptController == null) {
			FXMLLoader loader = FXMLLoaderFactory.eventResolverLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog Stage.
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("script.editor.title"));
			dialogStage.initModality(Modality.NONE);

			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			scriptController = loader.getController();
			scriptController.setDialogStage(dialogStage);
			scriptController.initialize(this, eventResolver);
		}

		// display current script
		scriptController.showFunctionScript(eventResolver);

		// Show the dialog and wait until the user closes it
		if (!scriptController.getDialogStage().isShowing()) {
			scriptController.getDialogStage().showAndWait();
		}

		return scriptController.getResolver().getScript();
	}

	HttpSource showHttpServerEditor() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.httpServerLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("http.servers.title"));
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		HttpServerController httpServerController = loader.getController();
		httpServerController.setDialogStage(dialogStage);
		httpServerController.initializeServer();

		// Show the dialog and wait until the user closes it
		httpServerController.getDialogStage().showAndWait();

		return httpServerController.getSource();
	}

	CollectorDataSource showMQBrokerEditor(DataSourceType type) throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.mqBrokerLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);

		if (type.equals(DataSourceType.MQTT)) {
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("mqtt.editor.title"));
		} else if (type.equals(DataSourceType.RMQ)) {
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("rmq.editor.title"));
		} else if (type.equals(DataSourceType.JMS)) {
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("jms.editor.title"));
		}

		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		MqBrokerController mqBrokerController = loader.getController();
		mqBrokerController.setDialogStage(dialogStage);
		mqBrokerController.initialize(this, type);

		// Show the dialog and wait until the user closes it
		if (!mqBrokerController.getDialogStage().isShowing()) {
			mqBrokerController.getDialogStage().showAndWait();
		}

		return mqBrokerController.getSource();
	}

	KafkaSource showKafkaServerEditor() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.kafkaServerLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("kafka.editor.title"));

		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		KafkaServerController kafkaServerController = loader.getController();
		kafkaServerController.setDialogStage(dialogStage);
		kafkaServerController.initialize(this);

		// Show the dialog and wait until the user closes it
		if (!kafkaServerController.getDialogStage().isShowing()) {
			kafkaServerController.getDialogStage().showAndWait();
		}

		return kafkaServerController.getSource();
	}

	EmailSource showEmailServerEditor() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.emailServerLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);

		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("email.editor.title"));

		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		EmailServerController emailServerController = loader.getController();
		emailServerController.setDialogStage(dialogStage);
		emailServerController.initialize(this);

		if (!emailServerController.getDialogStage().isShowing()) {
			emailServerController.getDialogStage().showAndWait();
		}

		return emailServerController.getSource();
	}

	ModbusSource showModbusEditor() throws Exception {
		if (modbusController == null) {
			FXMLLoader loader = FXMLLoaderFactory.modbusLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();
			Stage dialogStage = new Stage(StageStyle.DECORATED);

			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("modbus.editor.title"));

			dialogStage.initModality(Modality.WINDOW_MODAL);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			modbusController = loader.getController();
			modbusController.setDialogStage(dialogStage);
			modbusController.initialize(this);
		}

		// Show the dialog and wait until the user closes it
		if (!modbusController.getDialogStage().isShowing()) {
			modbusController.getDialogStage().showAndWait();
		}

		modbusController.setRegisterData();
		return modbusController.getSource();
	}

	DatabaseEventSource showDatabaseServerEditor() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.databaseServerLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("db.editor.title"));
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		DatabaseServerController databaseServerController = loader.getController();
		databaseServerController.setDialogStage(dialogStage);
		databaseServerController.initialize(this);

		// Show the dialog and wait until the user closes it
		if (!databaseServerController.getDialogStage().isShowing()) {
			databaseServerController.getDialogStage().showAndWait();
		}

		return databaseServerController.getSource();
	}

	FileEventSource showFileShareEditor() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.fileShareLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("file.editor.title"));
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		FileShareController fileShareController = loader.getController();
		fileShareController.setDialogStage(dialogStage);
		fileShareController.initialize(this);

		// Show the dialog and wait until the user closes it
		if (!fileShareController.getDialogStage().isShowing()) {
			fileShareController.getDialogStage().showAndWait();
		}

		return fileShareController.getSource();
	}

	CronEventSource showCronEditor() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.cronEditorLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("cron.editor.title"));
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		CronEditorController cronController = loader.getController();
		cronController.setDialogStage(dialogStage);
		cronController.initialize(this);

		// Show the dialog and wait until the user closes it
		if (!cronController.getDialogStage().isShowing()) {
			cronController.getDialogStage().showAndWait();
		}

		return cronController.getSource();
	}

	public void showCronHelp() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.cronHelpLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("cron.help.title"));
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		CronHelpController controller = loader.getController();
		controller.setDialogStage(dialogStage);
		controller.readHelpFile();

		// Show the dialog and wait until the user closes it
		if (!controller.getDialogStage().isShowing()) {
			controller.getDialogStage().showAndWait();
		}
	}

	DataCollector showCollectorEditor() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.dataCollectorLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("collector.editor.title"));
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		DataCollectorController dataCollectorController = loader.getController();
		dataCollectorController.setDialogStage(dialogStage);
		dataCollectorController.initialize(this);

		// Show the dialog and wait until the user closes it
		if (!dataCollectorController.getDialogStage().isShowing()) {
			dataCollectorController.getDialogStage().showAndWait();
		}

		return dataCollectorController.getCollector();
	}

	void showUomConverter() throws Exception {
		if (uomConversionController == null) {
			FXMLLoader loader = FXMLLoaderFactory.uomConversionLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			// Create the dialog stage
			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("uom.converter.title"));
			dialogStage.initModality(Modality.NONE);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			uomConversionController = loader.getController();
			uomConversionController.setDialogStage(dialogStage);
			uomConversionController.initializeApp(this);
		}

		// Show the dialog and wait until the user closes it
		if (!uomConversionController.getDialogStage().isShowing()) {
			uomConversionController.getDialogStage().showAndWait();
		}
	}

	void showAboutDialog() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(DesignerLocalizer.instance().getLangString("about"));
		alert.setHeaderText(DesignerLocalizer.instance().getLangString("about.header"));
		alert.setContentText(DomainUtils.getVersionInfo());
		alert.setResizable(true);

		alert.showAndWait();
	}

	void showOpcDaTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = FXMLLoaderFactory.opcDaTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("opc.da.trend"));
		dialogStage.initModality(Modality.NONE);
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
		opcDaTrendController.setScriptResolver(eventResolver);

		// show the window
		opcDaTrendController.getDialogStage().show();
	}

	void showOpcUaTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = FXMLLoaderFactory.opcUaTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("opc.ua.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		OpcUaTrendController opcUaTrendController = loader.getController();
		opcUaTrendController.setDialogStage(dialogStage);
		opcUaTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = opcUaTrendController.initializeTrend();

		opcUaTrendController.setUpdatePeriodMsec(eventResolver.getUpdatePeriod());

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		opcUaTrendController.setScriptResolver(eventResolver);

		// show the window
		opcUaTrendController.getDialogStage().show();
	}

	void showHttpTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = FXMLLoaderFactory.httpTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("http.trend.title"));
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
		httpTrendController.setScriptResolver(eventResolver);

		// start HTTP server
		httpTrendController.onStartServer();

		// show the trend
		httpTrendController.getDialogStage().show();
	}

	void showMessagingTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file
		FXMLLoader loader = FXMLLoaderFactory.messagingTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("rmq.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		RmqTrendController messagingTrendController = loader.getController();
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
		messagingTrendController.setEventResolver(eventResolver);

		// subscribe to broker
		messagingTrendController.subscribeToDataSource();

		// show the window
		messagingTrendController.getDialogStage().show();
	}

	void showJMSTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file
		FXMLLoader loader = FXMLLoaderFactory.jmsTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("jms.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		JmsTrendController jmsTrendController = loader.getController();
		jmsTrendController.setDialogStage(dialogStage);
		jmsTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = jmsTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		jmsTrendController.setEventResolver(eventResolver);

		// subscribe to broker
		jmsTrendController.subscribeToDataSource();

		// show the window
		jmsTrendController.getDialogStage().show();
	}

	void showKafkaTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file
		FXMLLoader loader = FXMLLoaderFactory.kafkaTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("kafka.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		KafkaTrendController kafkaTrendController = loader.getController();
		kafkaTrendController.setDialogStage(dialogStage);
		kafkaTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = kafkaTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		kafkaTrendController.setEventResolver(eventResolver);

		// subscribe to broker
		kafkaTrendController.subscribeToDataSource();

		// show the window
		kafkaTrendController.getDialogStage().show();
	}

	void showEmailTrendDialog(EventResolver eventResolver) throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.emailTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("email.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		EmailTrendController emailTrendController = loader.getController();
		emailTrendController.setDialogStage(dialogStage);
		emailTrendController.setApp(this);

		SplitPane chartPane = emailTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		emailTrendController.setEventResolver(eventResolver);

		// subscribe to server
		emailTrendController.subscribeToDataSource();

		// show the window
		emailTrendController.getDialogStage().show();
	}

	void showMQTTTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file
		FXMLLoader loader = FXMLLoaderFactory.mqttTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("mqtt.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		MqttTrendController mqttTrendController = loader.getController();
		mqttTrendController.setDialogStage(dialogStage);
		mqttTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = mqttTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		mqttTrendController.setEventResolver(eventResolver);

		// subscribe to broker
		mqttTrendController.subscribeToDataSource();

		// show the window
		mqttTrendController.getDialogStage().show();
	}

	void showDatabaseTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = FXMLLoaderFactory.databaseTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("db.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		DatabaseTrendController databaseTrendController = loader.getController();
		databaseTrendController.setDialogStage(dialogStage);
		databaseTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = databaseTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		databaseTrendController.setEventResolver(eventResolver);

		// connect to the database server
		databaseTrendController.subscribeToDataSource();

		// show the window
		databaseTrendController.getDialogStage().show();
	}

	void showFileTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = FXMLLoaderFactory.fileTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("file.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		FileTrendController fileTrendController = loader.getController();
		fileTrendController.setDialogStage(dialogStage);
		fileTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = fileTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		fileTrendController.setEventResolver(eventResolver);

		// connect to the file server
		fileTrendController.subscribeToDataSource();

		// show the window
		fileTrendController.getDialogStage().show();
	}

	void showCronTrendDialog(EventResolver eventResolver) throws Exception {
		// Load the fxml file and create a new stage for the pop-up dialog.
		FXMLLoader loader = FXMLLoaderFactory.cronTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("cron.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		CronTrendController cronTrendController = loader.getController();
		cronTrendController.setDialogStage(dialogStage);
		cronTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = cronTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		cronTrendController.setEventResolver(eventResolver);

		// start the job
		cronTrendController.subscribeToDataSource();

		// show the window
		cronTrendController.getDialogStage().show();
	}

	void showModbusTrendDialog(EventResolver eventResolver) throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.modbusTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("modbus.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		ModbusTrendController modbusTrendController = loader.getController();
		modbusTrendController.setDialogStage(dialogStage);
		modbusTrendController.setApp(this);

		// add the trend chart
		SplitPane chartPane = modbusTrendController.initializeTrend();

		AnchorPane.setBottomAnchor(chartPane, 50.0);
		AnchorPane.setLeftAnchor(chartPane, 5.0);
		AnchorPane.setRightAnchor(chartPane, 5.0);
		AnchorPane.setTopAnchor(chartPane, 50.0);

		page.getChildren().add(0, chartPane);

		// set the script resolver
		modbusTrendController.setEventResolver(eventResolver);

		// connect to the database server
		modbusTrendController.createModbusMaster();

		// show the window
		modbusTrendController.getDialogStage().show();
	}

	public PhysicalModelController getPhysicalModelController() {
		return this.physicalModelController;
	}

	public DaOpcClient getOpcDaClient() {
		if (appContext == null) {
			return null;
		}

		DaOpcClient client = appContext.getOpcDaClient();

		if (client == null) {
			client = new DaOpcClient();

			// add to context
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

	public OeeContext getAppContext() {
		return appContext;
	}

	// display the OEE dash board as a dialog
	void showDashboard() throws Exception {
		PlantEntity entity = getPhysicalModelController().getSelectedEntity();

		FXMLLoader dialogLoader = FXMLLoaderFactory.dashboardDialogLoader();
		AnchorPane pane = (AnchorPane) dialogLoader.getRoot();

		// Create the dialog Stage.
		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("oee.dashboard", entity.getDisplayString()));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(pane);
		dialogStage.setScene(scene);

		// get the controller
		DashboardDialogController dashboardDialogController = dialogLoader.getController();
		dashboardDialogController.setDialogStage(dialogStage);

		// load the content
		FXMLLoader dashboardLoader = FXMLLoaderFactory.dashboardLoader();
		DashboardController dashboardController = dashboardLoader.getController();

		SplitPane spDashboard = (SplitPane) dashboardLoader.getRoot();

		pane.getChildren().add(0, spDashboard);

		AnchorPane.setTopAnchor(spDashboard, 0.0);
		AnchorPane.setBottomAnchor(spDashboard, 50.0);
		AnchorPane.setLeftAnchor(spDashboard, 0.0);
		AnchorPane.setRightAnchor(spDashboard, 0.0);

		dashboardController.enableRefresh(true);

		dashboardDialogController.setDashboardController(dashboardController);

		dashboardController.setupEquipmentLoss((Equipment) entity);

		// Show the dialog and wait until the user closes it
		dashboardDialogController.getDialogStage().showAndWait();
	}

	public UaOpcClient getOpcUaClient() {
		if (appContext == null) {
			return null;
		}

		UaOpcClient client = appContext.getOpcUaClient();

		if (client == null) {
			client = new UaOpcClient();

			// add to context
			appContext.getOpcUaClients().add(client);
		}
		return client;
	}

	public ModbusMaster getModbusMaster() {
		if (appContext == null) {
			return null;
		}

		return appContext.getModbusMaster();
	}

	public ModbusMaster createModbusMaster(ModbusSource source) {
		ModbusMaster master = new ModbusMaster(source);

		// add to context
		appContext.getModbusMasters().add(master);
		return master;
	}
}
