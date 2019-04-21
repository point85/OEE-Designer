package org.point85.app.dashboard;

import org.point85.app.designer.DesignerDialogController;

import javafx.fxml.FXML;

public class DashboardDialogController extends DesignerDialogController {
	private DashboardController dashboardController;

	public DashboardController getDashboardController() {
		return dashboardController;
	}

	public void setDashboardController(DashboardController dashboardController) {
		this.dashboardController = dashboardController;
	}

	@FXML
	public void initialize() throws Exception {
		// images for buttons
		setImages();
	}
}
