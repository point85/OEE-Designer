package org.point85.app;

import java.util.ResourceBundle;

import org.point85.app.charts.SampleChartController;
import org.point85.app.charts.TrendChartController;
import org.point85.app.collector.CollectorApplication;
import org.point85.app.collector.CollectorLocalizer;
import org.point85.app.cron.CronEditorController;
import org.point85.app.cron.CronHelpController;
import org.point85.app.cron.CronTrendController;
import org.point85.app.dashboard.AvailabilityEditorController;
import org.point85.app.dashboard.DashboardController;
import org.point85.app.dashboard.DashboardDialogController;
import org.point85.app.dashboard.ProductionEditorController;
import org.point85.app.dashboard.SetupEditorController;
import org.point85.app.db.DatabaseServerController;
import org.point85.app.db.DatabaseTrendController;
import org.point85.app.designer.DataCollectorController;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.app.designer.EntityWorkScheduleController;
import org.point85.app.designer.EquipmentMaterialController;
import org.point85.app.designer.EquipmentResolverController;
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
import org.point85.app.monitor.MonitorApplication;
import org.point85.app.monitor.MonitorLocalizer;
import org.point85.app.monitor.OeeEventTrendController;
import org.point85.app.opc.da.OpcDaBrowserController;
import org.point85.app.opc.da.OpcDaTrendController;
import org.point85.app.opc.ua.OpcUaBrowserController;
import org.point85.app.opc.ua.OpcUaTrendController;
import org.point85.app.operator.MaterialSelectorController;
import org.point85.app.operator.OperatorApplication;
import org.point85.app.operator.OperatorLocalizer;
import org.point85.app.operator.ReasonSelectorController;
import org.point85.app.reason.ReasonEditorController;
import org.point85.app.schedule.TemplateScheduleDialogController;
import org.point85.app.schedule.WorkScheduleEditorController;
import org.point85.app.schedule.WorkScheduleShiftsController;
import org.point85.app.script.EventResolverController;
import org.point85.app.tester.TesterApplication;
import org.point85.app.tester.TesterLocalizer;
import org.point85.app.uom.UomConversionController;
import org.point85.app.uom.UomEditorController;
import org.point85.app.uom.UomImporterController;

import javafx.fxml.FXMLLoader;

public class FXMLLoaderFactory {
	private FXMLLoaderFactory() {
		throw new IllegalStateException("Utility class");
	}

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
		FXMLLoader fxmlLoader = new FXMLLoader(DashboardController.class.getResource("Dashboard.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader dashboardDialogLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(DashboardDialogController.class.getResource("DashboardDialog.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader reasonEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(ReasonEditorController.class.getResource("ReasonEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader materialEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(MaterialEditorController.class.getResource("MaterialEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader uomEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(UomEditorController.class.getResource("UomEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader uomImporterLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(UomImporterController.class.getResource("UomImport.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader scheduleEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				WorkScheduleEditorController.class.getResource("WorkScheduleEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader scheduleShiftsLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				WorkScheduleShiftsController.class.getResource("WorkScheduleShifts.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opdDaBrowserLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OpcDaBrowserController.class.getResource("OpcDaBrowser.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opdUaBrowserLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OpcUaBrowserController.class.getResource("OpcUaBrowser.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader eventResolverLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(EventResolverController.class.getResource("EventResolver.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader httpServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(HttpServerController.class.getResource("HttpServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader mqBrokerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(MqBrokerController.class.getResource("MqBroker.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader kafkaServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(KafkaServerController.class.getResource("KafkaServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader emailServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(EmailServerController.class.getResource("EmailServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader modbusLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(ModbusMasterController.class.getResource("ModbusMaster.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader databaseServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(DatabaseServerController.class.getResource("DatabaseServer.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader fileShareLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FileShareController.class.getResource("FileShare.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader cronEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(CronEditorController.class.getResource("CronEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader cronHelpLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(CronHelpController.class.getResource("CronHelp.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader dataCollectorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(DataCollectorController.class.getResource("DataCollector.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader uomConversionLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(UomConversionController.class.getResource("UomConversion.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opcDaTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OpcDaTrendController.class.getResource("OpcDaTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opcUaTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OpcUaTrendController.class.getResource("OpcUaTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader httpTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(HttpTrendController.class.getResource("HttpTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader messagingTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(RmqTrendController.class.getResource("MessagingTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader jmsTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(JmsTrendController.class.getResource("JMSTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader kafkaTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(KafkaTrendController.class.getResource("KafkaTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader emailTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(EmailTrendController.class.getResource("EmailTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader mqttTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(MqttTrendController.class.getResource("MQTTTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader databaseTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(DatabaseTrendController.class.getResource("DatabaseTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader fileTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(FileTrendController.class.getResource("FileTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader cronTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(CronTrendController.class.getResource("CronTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader modbusTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(ModbusTrendController.class.getResource("ModbusTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader sampleChartLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(SampleChartController.class.getResource("SampleChart.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader trendChartLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(TrendChartController.class.getResource("TrendChart.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader equipmentMaterialLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(EquipmentMaterialController.class.getResource("EquipmentMaterial.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader entityWorkScheduleLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				EntityWorkScheduleController.class.getResource("EntityWorkSchedule.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader equipmentResolverLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(EquipmentResolverController.class.getResource("EquipmentResolver.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader templateScheduleLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				TemplateScheduleDialogController.class.getResource("TemplateScheduleDialog.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader availabilityEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(
				AvailabilityEditorController.class.getResource("AvailabilityEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader setupEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(SetupEditorController.class.getResource("SetupEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader productionEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(ProductionEditorController.class.getResource("ProductionEditor.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader splashLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(SplashController.class.getResource("Splash.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader reasonSelectorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(ReasonSelectorController.class.getResource("ReasonSelector.fxml"));
		fxmlLoader.setResources(getOperatorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader operatorApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OperatorApplication.class.getResource("OperatorApplication.fxml"));
		fxmlLoader.setResources(getOperatorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader materialSelectorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(MaterialSelectorController.class.getResource("MaterialSelector.fxml"));
		fxmlLoader.setResources(getOperatorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader testerApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(TesterApplication.class.getResource("TesterApplication.fxml"));
		fxmlLoader.setResources(getTesterLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader collectorApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(CollectorApplication.class.getResource("CollectorApplication.fxml"));
		fxmlLoader.setResources(getCollectorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader designerApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(DesignerApplication.class.getResource("DesignerApplication.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader monitorApplicationLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(MonitorApplication.class.getResource("MonitorApplication.fxml"));
		fxmlLoader.setResources(getMonitorLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader oeeEventTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OeeEventTrendController.class.getResource("OeeEventTrend.fxml"));
		fxmlLoader.setResources(getDesignerLangBundle());
		fxmlLoader.load();
		return fxmlLoader;
	}
}
