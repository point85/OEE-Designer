package org.point85.app;

import java.util.ResourceBundle;

import org.point85.app.collector.CollectorLocalizer;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.app.monitor.MonitorLocalizer;
import org.point85.app.operator.OperatorLocalizer;
import org.point85.app.tester.TesterLocalizer;

import javafx.fxml.FXMLLoader;

public class FXMLLoaderFactory {
	// FXML resource path
	private static final String FXML_PATH = "/fxml/";

	// name of Designer application resource bundle with translatable strings
	private static ResourceBundle designerLangBundle;

	// name of Monitor application resource bundle with translatable strings
	private static ResourceBundle monitorLangBundle;

	// name of Operator application resource bundle with translatable strings
	private static ResourceBundle operatorLangBundle;

	// name of Collector application resource bundle with translatable strings
	private static ResourceBundle collectorLangBundle;

	// name of Tester application resource bundle with translatable strings
	private static ResourceBundle testerLangBundle;

	private FXMLLoaderFactory() {
		// hide public constructor
	}

	public static ResourceBundle getDesignerLangBundle() {
		if (designerLangBundle == null) {
			designerLangBundle = DesignerLocalizer.instance().loadLangBundle();
		}
		return designerLangBundle;
	}

	public static ResourceBundle getMonitorLangBundle() {
		if (monitorLangBundle == null) {
			monitorLangBundle = MonitorLocalizer.instance().loadLangBundle();
		}
		return monitorLangBundle;
	}

	public static ResourceBundle getOperatorLangBundle() {
		if (operatorLangBundle == null) {
			operatorLangBundle = OperatorLocalizer.instance().loadLangBundle();
		}
		return operatorLangBundle;
	}

	public static ResourceBundle getCollectorLangBundle() {
		if (collectorLangBundle == null) {
			collectorLangBundle = CollectorLocalizer.instance().loadLangBundle();
		}
		return collectorLangBundle;
	}

	public static ResourceBundle getTesterLangBundle() {
		if (testerLangBundle == null) {
			testerLangBundle = TesterLocalizer.instance().loadLangBundle();
		}
		return testerLangBundle;
	}

	public static FXMLLoader dashboardLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "Dashboard.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader dashboardDialogLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "DashboardDialog.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader reasonEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "ReasonEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader materialEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "MaterialEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader uomEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "UomEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader uomImporterLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "UomImport.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader scheduleEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "WorkScheduleEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader scheduleShiftsLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "WorkScheduleShifts.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opdDaBrowserLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "OpcDaBrowser.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opdUaBrowserLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "OpcUaBrowser.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader eventResolverLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "EventResolver.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader httpServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "HttpServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader mqBrokerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "MqBroker.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader mqttServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "MqttServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader wsServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "WebSocketServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader kafkaServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "KafkaServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader emailServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "EmailServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader modbusLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "ModbusMaster.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader databaseServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "DatabaseServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader fileShareLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "FileShare.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader cronEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "CronEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader cronHelpLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "CronHelp.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader dataCollectorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "DataCollector.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader uomConversionLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "UomConversion.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opcDaTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "OpcDaTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opcUaTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "OpcUaTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader httpTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "HttpTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader messagingTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "MessagingTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader jmsTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "JMSTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader kafkaTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "KafkaTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader emailTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "EmailTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader mqttTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "MqttTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader databaseTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "DatabaseTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader fileTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "FileTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader cronTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "CronTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader proficyTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "ProficyTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader modbusTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "ModbusTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader sampleChartLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "SampleChart.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader trendChartLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "TrendChart.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader equipmentMaterialLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "EquipmentMaterial.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader entityWorkScheduleLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "EntityWorkSchedule.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader equipmentResolverLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "EquipmentResolver.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader templateScheduleLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "TemplateScheduleDialog.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader availabilityEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "AvailabilityEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader setupEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "SetupEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader productionEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "ProductionEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader splashLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "Splash.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader reasonSelectorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "ReasonSelector.fxml"));
		fxmlLoader.setResources(getOperatorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader operatorApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "OperatorApplication.fxml"));
		fxmlLoader.setResources(getOperatorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader materialSelectorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "MaterialSelector.fxml"));
		fxmlLoader.setResources(getOperatorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader testerApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "TesterApplication.fxml"));
		fxmlLoader.setResources(getTesterLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader collectorApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "CollectorApplication.fxml"));
		fxmlLoader.setResources(getCollectorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader designerApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "DesignerApplication.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader monitorApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				FXMLLoaderFactory.class.getResource(FXML_PATH + "MonitorApplication.fxml"));
		fxmlLoader.setResources(getMonitorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader oeeEventTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "OeeEventTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader proficyLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "ProficyBrowser.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader webSocketTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FXMLLoaderFactory.class.getResource(FXML_PATH + "WebSocketTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}
}
