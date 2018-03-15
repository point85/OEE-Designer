package org.point85.app;

import org.point85.app.charts.SampleChartController;
import org.point85.app.charts.TrendChartController;
import org.point85.app.dashboard.DashboardController;
import org.point85.app.dashboard.DashboardDialogController;
import org.point85.app.designer.DataCollectorController;
import org.point85.app.designer.EquipmentMaterialController;
import org.point85.app.designer.EquipmentResolverController;
import org.point85.app.http.HttpServerController;
import org.point85.app.http.HttpTrendController;
import org.point85.app.material.MaterialEditorController;
import org.point85.app.messaging.MessagingTrendController;
import org.point85.app.messaging.MqBrokerController;
import org.point85.app.opc.da.OpcDaBrowserController;
import org.point85.app.opc.da.OpcDaTrendController;
import org.point85.app.opc.ua.OpcUaBrowserController;
import org.point85.app.opc.ua.OpcUaTrendController;
import org.point85.app.reason.ReasonEditorController;
import org.point85.app.schedule.TemplateScheduleDialogController;
import org.point85.app.schedule.WorkScheduleEditorController;
import org.point85.app.script.EventResolverController;
import org.point85.app.uom.UomConversionController;
import org.point85.app.uom.UomEditorController;
import org.point85.app.uom.UomImporterController;
import org.point85.app.web.WebServerController;

import javafx.fxml.FXMLLoader;

public class LoaderFactory {
	public static FXMLLoader dashboardLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(DashboardController.class.getResource("Dashboard.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader dashboardDialogLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(DashboardDialogController.class.getResource("DashboardDialog.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader reasonEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(ReasonEditorController.class.getResource("ReasonEditor.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader materialEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(MaterialEditorController.class.getResource("MaterialEditor.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader uomEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(UomEditorController.class.getResource("UomEditor.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader uomImporterLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(UomImporterController.class.getResource("UomImport.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader scheduleEditorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(WorkScheduleEditorController.class.getResource("WorkScheduleEditor.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opdDaBrowserLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OpcDaBrowserController.class.getResource("OpcDaBrowser.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opdUaBrowserLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OpcUaBrowserController.class.getResource("OpcUaBrowser.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader eventResolverLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(EventResolverController.class.getResource("EventResolver.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader httpServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(HttpServerController.class.getResource("HttpServer.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader mqBrokerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(MqBrokerController.class.getResource("MqBroker.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader webServerLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(WebServerController.class.getResource("WebServerController.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader dataCollectorLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(DataCollectorController.class.getResource("DataCollector.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader uomConversionLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(UomConversionController.class.getResource("UomConversion.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opcDaTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OpcDaTrendController.class.getResource("OpcDaTrend.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}

	public static FXMLLoader opcUaTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(OpcUaTrendController.class.getResource("OpcUaTrend.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader httpTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(HttpTrendController.class.getResource("HttpTrend.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader messagingTrendLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(MessagingTrendController.class.getResource("MessagingTrend.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader sampleChartLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(SampleChartController.class.getResource("SampleChart.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader trendChartLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(TrendChartController.class.getResource("TrendChart.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader equipmentMaterialLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(EquipmentMaterialController.class.getResource("EquipmentMaterial.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader equipmentResolverLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(EquipmentResolverController.class.getResource("EquipmentResolver.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
	
	public static FXMLLoader templateScheduleLoader() throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(TemplateScheduleDialogController.class.getResource("TemplateScheduleDialog.fxml"));
		fxmlLoader.load();
		return fxmlLoader;
	}
}
