package org.point85.app.operator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.EntityNode;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorService;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.ShiftInstance;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.OeeEventType;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class OperatorController {
	// manual source id
	private static final String OPER_SOURCE_ID = "OPERATOR";

	// collection services
	private CollectorService collectorService;

	// reason dialog
	private ReasonSelectorController reasonController;

	// material dialog
	private MaterialSelectorController materialController;

	// equipment being worked with
	private Equipment equipment;

	// and it produced material
	private EquipmentMaterial equipmentMaterial;

	// last setup
	private OeeEvent lastSetup;

	// availability or production reason
	private Reason selectedReason;

	// selected setup material
	private Material selectedMaterial;

	@FXML
	private TreeView<EntityNode> tvEntities;

	@FXML
	private TabPane tpOee;

	@FXML
	private Tab tabAvailability;

	@FXML
	private Tab tabProduction;

	@FXML
	private Tab tabSetup;

	@FXML
	private Label lbMaterial;

	@FXML
	private Label lbJob;

	@FXML
	private Label lbMessage;

	@FXML
	private RadioButton rbAvailabilityByEvent;

	@FXML
	private RadioButton rbAvailabilityByPeriod;

	@FXML
	private Label lbAvailabilityReason;

	@FXML
	private Button btFindAvailabilityReason;

	@FXML
	private DatePicker dpAvailabilityStartDate;

	@FXML
	private TextField tfAvailabilityStartTime;

	@FXML
	private Label lbAvailabilityEndDate;

	@FXML
	private DatePicker dpAvailabilityEndDate;

	@FXML
	private Label lbAvailabilityEndTime;

	@FXML
	private TextField tfAvailabilityEndTime;

	@FXML
	private Label lbAvailabilityHours;

	@FXML
	private TextField tfAvailabilityHours;

	@FXML
	private Label lbAvailabilityMinutes;

	@FXML
	private TextField tfAvailabilityMinutes;

	@FXML
	private Button btRecordAvailability;

	@FXML
	private RadioButton rbProductionByEvent;

	@FXML
	private RadioButton rbProductionByPeriod;

	@FXML
	private RadioButton rbProductionGood;

	@FXML
	private RadioButton rbProductionReject;

	@FXML
	private RadioButton rbProductionStartup;

	@FXML
	private Label lbProductionReason;

	@FXML
	private Button btFindProductionReason;

	@FXML
	private DatePicker dpProductionStartDate;

	@FXML
	private TextField tfProductionStartTime;

	@FXML
	private Label lbProductionEndDate;

	@FXML
	private DatePicker dpProductionEndDate;

	@FXML
	private Label lbProductionEndTime;

	@FXML
	private TextField tfProductionEndTime;

	@FXML
	private TextField tfProductionAmount;

	@FXML
	private Label lbProductionUOM;

	@FXML
	private Button btRecordProduction;

	@FXML
	private Button btFindMaterial;

	@FXML
	private Label lbSetupMaterial;

	@FXML
	private TextField tfJob;

	@FXML
	private DatePicker dpChangeoverDate;

	@FXML
	private Button btRecordChangeover;

	@FXML
	private TextField tfChangeoverTime;

	void initialize() throws Exception {
		// images
		setImages();

		// add the tree view listener for entity selection
		tvEntities.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectEntity(oldValue, newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});
		tvEntities.setShowRoot(false);

		initializeDateTimes();

		rbAvailabilityByPeriod.setSelected(true);
		rbProductionByPeriod.setSelected(true);

		tpOee.setDisable(true);

		// collection services
		startCollector();
	}

	private void startCollector() throws Exception {
		// collector
		collectorService = new CollectorService(true);

		try {
			// startup server
			collectorService.startup();
		} catch (Exception e) {
			if (collectorService != null) {
				collectorService.shutdown();
			}
			throw e;
		}
	}

	// images for buttons
	protected void setImages() throws Exception {
		// find availability reason
		btFindAvailabilityReason.setGraphic(ImageManager.instance().getImageView(Images.REASON));
		btFindAvailabilityReason.setContentDisplay(ContentDisplay.LEFT);

		// record availability event
		btRecordAvailability.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btRecordAvailability.setContentDisplay(ContentDisplay.LEFT);

		// find production reason
		btFindProductionReason.setGraphic(ImageManager.instance().getImageView(Images.REASON));
		btFindProductionReason.setContentDisplay(ContentDisplay.LEFT);

		// record production event
		btRecordProduction.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btRecordProduction.setContentDisplay(ContentDisplay.LEFT);

		// find setup material
		btFindMaterial.setGraphic(ImageManager.instance().getImageView(Images.MATERIAL));
		btFindMaterial.setContentDisplay(ContentDisplay.LEFT);

		// record production event
		btRecordChangeover.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btRecordChangeover.setContentDisplay(ContentDisplay.LEFT);

		// tabs
		tabAvailability.setGraphic(ImageManager.instance().getImageView(Images.AVAILABILITY));
		tabProduction.setGraphic(ImageManager.instance().getImageView(Images.PRODUCT));
		tabSetup.setGraphic(ImageManager.instance().getImageView(Images.SETUP));

	}

	private Reason showReasonSelector() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.reasonSelectorLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(OperatorLocalizer.instance().getLangString("reason.selector.title"));
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		reasonController = loader.getController();
		reasonController.setDialogStage(dialogStage);
		reasonController.initialize();

		if (!reasonController.getDialogStage().isShowing()) {
			reasonController.getDialogStage().showAndWait();
		}
		return reasonController.getSelectedReason();
	}

	private Material showMaterialSelector() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.materialSelectorLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(OperatorLocalizer.instance().getLangString("material.selector.title"));
		dialogStage.initModality(Modality.WINDOW_MODAL);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		materialController = loader.getController();
		materialController.setDialogStage(dialogStage);
		materialController.initialize();

		if (!materialController.getDialogStage().isShowing()) {
			materialController.getDialogStage().showAndWait();
		}
		return materialController.getSelectedMaterial();
	}

	// show the entity attributes
	private void displayEntityAttributes(PlantEntity entity) throws Exception {
		if (entity == null || (!(entity instanceof Equipment))) {
			return;
		}

		// get last setup
		lastSetup = PersistenceService.instance().fetchLastEvent(((Equipment) entity), OeeEventType.MATL_CHANGE);

		if (lastSetup != null) {
			lbMaterial.setText(lastSetup.getMaterial().getDisplayString());
			lbJob.setText(lastSetup.getJob());

			equipmentMaterial = equipment.getEquipmentMaterial(lastSetup.getMaterial());
			if (equipmentMaterial == null) {
				throw new Exception(
						OperatorLocalizer.instance().getErrorString("no.produced.material", lastSetup.getMaterial()));
			}
		} else {
			lbMaterial.setText(null);
			lbJob.setText(null);
			lbMessage.setText(OperatorLocalizer.instance().getErrorString("no.setup", equipment.getName()));
		}
	}

	private void onSelectEntity(TreeItem<EntityNode> oldItem, TreeItem<EntityNode> newItem) throws Exception {
		if (newItem == null) {
			return;
		}

		PlantEntity selectedEntity = newItem.getValue().getPlantEntity();

		// show the children too
		Set<PlantEntity> children = selectedEntity.getChildren();
		List<PlantEntity> sortedChildren = new ArrayList<>(children);
		Collections.sort(sortedChildren);

		boolean hasTreeChildren = newItem.getChildren().size() > 0 ? true : false;

		// check to see if the node's children have been previously shown
		if (!hasTreeChildren) {
			newItem.getChildren().clear();
			for (PlantEntity child : children) {
				TreeItem<EntityNode> entityItem = new TreeItem<>(new EntityNode(child));
				newItem.getChildren().add(entityItem);
				setEntityGraphic(entityItem);
			}
		}
		newItem.setExpanded(true);

		reset();
		if (selectedEntity instanceof Equipment) {
			equipment = (Equipment) selectedEntity;
			tpOee.setDisable(false);
		} else {
			equipment = null;
			tpOee.setDisable(true);
		}

		displayEntityAttributes(selectedEntity);
	}

	private void reset() {
		lbMaterial.setText(null);
		lbJob.setText(null);
		lbMessage.setText(null);

		initializeDateTimes();

		selectedReason = null;
		lbProductionReason.setText(null);
		lbAvailabilityReason.setText(null);

		lbProductionUOM.setText(null);
		tfProductionAmount.clear();

		selectedMaterial = null;
		lbSetupMaterial.setText(null);
		tfJob.clear();
	}

	private void clearProductionCounts() {
		selectedReason = null;
		lbProductionReason.setText(null);

		lbProductionUOM.setText(null);
		tfProductionAmount.setText(null);
	}

	@FXML
	private void onSelectGoodProduction() {
		clearProductionCounts();
		lbProductionUOM.setText(equipmentMaterial.getRunRateUOM().getDividend().getDisplayString());
	}

	@FXML
	private void onSelectStartupProduction() {
		clearProductionCounts();
		lbProductionUOM.setText(equipmentMaterial.getRunRateUOM().getDividend().getDisplayString());
	}

	@FXML
	private void onSelectRejectProduction() {
		clearProductionCounts();
		lbProductionUOM.setText(equipmentMaterial.getRejectUOM().getDisplayString());
	}

	private void setEntityGraphic(TreeItem<EntityNode> item) throws Exception {
		ImageView nodeView = null;
		PlantEntity entity = item.getValue().getPlantEntity();
		EntityLevel level = entity.getLevel();

		if (level == null) {
			return;
		}

		switch (level) {
		case AREA:
			nodeView = ImageManager.instance().getImageView(Images.AREA);
			break;
		case ENTERPRISE:
			nodeView = ImageManager.instance().getImageView(Images.ENTERPRISE);
			break;
		case EQUIPMENT:
			nodeView = ImageManager.instance().getImageView(Images.EQUIPMENT);
			break;

		case PRODUCTION_LINE:
			nodeView = ImageManager.instance().getImageView(Images.LINE);
			break;

		case SITE:
			nodeView = ImageManager.instance().getImageView(Images.SITE);
			break;

		case WORK_CELL:
			nodeView = ImageManager.instance().getImageView(Images.CELL);
			break;

		default:
			break;
		}

		item.setGraphic(nodeView);
	}

	// the single root for all entities
	private TreeItem<EntityNode> getRootEntityItem() throws Exception {
		if (tvEntities.getRoot() == null) {
			PlantEntity rootEntity = new PlantEntity();
			rootEntity.setName(PlantEntity.ROOT_ENTITY_NAME);
			tvEntities.setRoot(new TreeItem<>(new EntityNode(rootEntity)));
		}
		return tvEntities.getRoot();
	}

	// display top-level entities
	public void populateTopEntityNodes() throws Exception {
		// fetch the entities
		List<PlantEntity> entities = PersistenceService.instance().fetchTopPlantEntities();

		Collections.sort(entities);

		// add them to the root entity
		ObservableList<TreeItem<EntityNode>> children = getRootEntityItem().getChildren();
		children.clear();

		for (PlantEntity entity : entities) {
			TreeItem<EntityNode> entityItem = new TreeItem<>(new EntityNode(entity));
			children.add(entityItem);
			setEntityGraphic(entityItem);
		}

		// refresh tree view
		getRootEntityItem().setExpanded(true);
		tvEntities.getSelectionModel().clearSelection();
		tvEntities.refresh();
	}

	private void initializeDateTimes() {
		final String hhMM = "00:00";
		final String zero = "0";
		final LocalDate now = LocalDate.now();

		// availability
		dpAvailabilityStartDate.setValue(now);
		tfAvailabilityStartTime.setText(hhMM);

		dpAvailabilityEndDate.setValue(now);
		tfAvailabilityEndTime.setText(hhMM);

		tfAvailabilityHours.setText(zero);
		tfAvailabilityMinutes.setText(zero);

		// production
		dpProductionStartDate.setValue(now);
		tfProductionStartTime.setText(hhMM);

		dpProductionEndDate.setValue(now);
		tfProductionEndTime.setText(hhMM);

		// changeover
		dpChangeoverDate.setValue(now);
		tfChangeoverTime.setText(hhMM);
	}

	@FXML
	private void onChooseAvailabilityReason() {
		try {
			// show the reason dialog
			selectedReason = showReasonSelector();

			if (reasonController.isCancelled()) {
				return;
			}

			if (selectedReason != null) {
				lbAvailabilityReason.setText(selectedReason.getDisplayString());
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onChooseProductionReason() {
		try {
			// show the reason dialog
			selectedReason = showReasonSelector();

			if (reasonController.isCancelled()) {
				return;
			}

			if (selectedReason != null) {
				lbProductionReason.setText(selectedReason.getDisplayString());
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onChooseMaterial() {
		try {
			// show the material dialog
			selectedMaterial = showMaterialSelector();

			if (materialController.isCancelled()) {
				return;
			}

			if (selectedMaterial != null) {
				lbSetupMaterial.setText(selectedMaterial.getDisplayString());
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void setEventTimes(OeeEvent event, LocalDate startDate, String start, LocalDate endDate, String end,
			boolean forPeriod) throws Exception {

		// start time
		Duration startSeconds = null;
		if (start != null && start.trim().length() > 0) {
			startSeconds = AppUtils.durationFromString(start.trim());
		} else {
			startSeconds = Duration.ZERO;
		}
		LocalTime startTime = LocalTime.ofSecondOfDay(startSeconds.getSeconds());
		LocalDateTime ldtStart = LocalDateTime.of(startDate, startTime);
		event.setStartTime(DomainUtils.fromLocalDateTime(ldtStart));

		// end time
		if (forPeriod) {
			Duration endSeconds = null;
			if (end != null && end.trim().length() > 0) {
				endSeconds = AppUtils.durationFromString(end.trim());
			} else {
				endSeconds = Duration.ZERO;
			}
			LocalTime endTime = LocalTime.ofSecondOfDay(endSeconds.getSeconds());
			LocalDateTime ldtEnd = LocalDateTime.of(endDate, endTime);

			if (ldtEnd.isBefore(ldtStart)) {
				throw new Exception(OperatorLocalizer.instance().getErrorString("invalid.start", ldtStart, ldtEnd));
			}

			event.setEndTime(DomainUtils.fromLocalDateTime(ldtEnd));
		}

		// get the shift from the work schedule
		WorkSchedule schedule = equipment.findWorkSchedule();

		if (schedule != null) {
			List<ShiftInstance> shifts = schedule.getShiftInstancesForTime(ldtStart);

			if (!shifts.isEmpty()) {
				event.setShift(shifts.get(0).getShift());
				event.setTeam(shifts.get(0).getTeam());
			}
		}
	}

	private void recordEvent(OeeEventType eventType) throws Exception {
		// equipment
		if (equipment == null) {
			throw new Exception(OperatorLocalizer.instance().getErrorString("no.equipment"));
		}

		// reason
		if (selectedReason == null && eventType.equals(OeeEventType.AVAILABILITY)) {
			throw new Exception(OperatorLocalizer.instance().getErrorString("no.reason"));
		}

		// create event
		OeeEvent event = createEvent(eventType, equipment);

		String msg = null;
		if (eventType.equals(OeeEventType.AVAILABILITY)) {
			event.setInputValue(selectedReason.getName());

			setEventTimes(event, dpAvailabilityStartDate.getValue(), tfAvailabilityStartTime.getText(),
					dpAvailabilityEndDate.getValue(), tfAvailabilityEndTime.getText(),
					rbAvailabilityByPeriod.isSelected());

			Duration duration = null;
			if (rbAvailabilityByPeriod.isSelected()) {
				// specified duration
				int seconds = 0;

				String hours = tfAvailabilityHours.getText();

				if (hours != null && hours.trim().length() > 0) {
					seconds = Integer.valueOf(hours.trim()) * 3600;
				}

				String minutes = tfAvailabilityMinutes.getText();
				if (minutes != null && minutes.trim().length() > 0) {
					seconds += Integer.valueOf(minutes.trim()) * 60;
				}

				duration = Duration.ofSeconds(seconds);
				event.setDuration(duration);
			}

			event.setReason(selectedReason);
			event.setMaterial(lastSetup.getMaterial());
			event.setJob(lastSetup.getJob());

			msg = OperatorLocalizer.instance().getLangString("availability.recorded", equipment.getName(),
					selectedReason.getName());
		} else if (eventType.equals(OeeEventType.PROD_GOOD) || eventType.equals(OeeEventType.PROD_REJECT)
				|| eventType.equals(OeeEventType.PROD_STARTUP)) {

			setEventTimes(event, dpProductionStartDate.getValue(), tfProductionStartTime.getText(),
					dpProductionEndDate.getValue(), tfProductionEndTime.getText(), rbProductionByPeriod.isSelected());

			// quantity produced
			Double amount = null;

			String qty = tfProductionAmount.getText();
			if (qty != null && qty.trim().length() > 0) {
				amount = Double.valueOf(qty);
			} else {
				throw new Exception(OperatorLocalizer.instance().getErrorString("no.amount"));
			}
			event.setAmount(amount);
			event.setInputValue(String.valueOf(amount));

			if (eventType.equals(OeeEventType.PROD_REJECT)) {
				event.setUOM(equipmentMaterial.getRejectUOM());
			} else {
				event.setUOM(equipmentMaterial.getRunRateUOM().getDividend());
			}

			event.setReason(selectedReason);
			event.setMaterial(lastSetup.getMaterial());
			event.setJob(lastSetup.getJob());

			msg = OperatorLocalizer.instance().getLangString("production.recorded", equipment.getName(),
					eventType.toString());
		} else if (eventType.equals(OeeEventType.MATL_CHANGE)) {
			setEventTimes(event, dpChangeoverDate.getValue(), tfChangeoverTime.getText(), null, null, false);

			// material
			if (selectedMaterial == null) {
				throw new Exception(OperatorLocalizer.instance().getErrorString("no.material", equipment.getName()));
			}
			event.setMaterial(selectedMaterial);

			// job
			String job = tfJob.getText();

			if (job != null && job.trim().length() > 0) {
				event.setJob(job);
			}
			msg = OperatorLocalizer.instance().getLangString("setup.recorded", equipment.getName(),
					selectedMaterial.getDisplayString());
		}

		// save to database
		collectorService.recordResolution(event);

		// notify user
		lbMessage.setText(msg);
	}

	@FXML
	private void onRecordAvailabilityEvent() {
		try {
			recordEvent(OeeEventType.AVAILABILITY);
		} catch (Exception e) {
			lbMessage.setText(null);
			AppUtils.showErrorDialog(e);
		}
		selectedReason = null;
		lbAvailabilityReason.setText(null);
		tfAvailabilityHours.setText("0");
		tfAvailabilityMinutes.setText("0");
	}

	@FXML
	private void onRecordProductionEvent() {
		try {
			if (rbProductionGood.isSelected()) {
				recordEvent(OeeEventType.PROD_GOOD);
			} else if (rbProductionReject.isSelected()) {
				recordEvent(OeeEventType.PROD_REJECT);
			} else if (rbProductionStartup.isSelected()) {
				recordEvent(OeeEventType.PROD_STARTUP);
			}
		} catch (Exception e) {
			lbMessage.setText(null);
			AppUtils.showErrorDialog(e);
		}
		tfProductionAmount.setText(null);
		lbProductionReason.setText(null);
		selectedReason = null;
	}

	@FXML
	private void onRecordSetupEvent() {
		try {
			recordEvent(OeeEventType.MATL_CHANGE);

			PlantEntity selectedEntity = tvEntities.getSelectionModel().getSelectedItem().getValue().getPlantEntity();
			displayEntityAttributes(selectedEntity);
		} catch (Exception e) {
			lbMessage.setText(null);
			AppUtils.showErrorDialog(e);
		}
	}

	private OeeEvent createEvent(OeeEventType type, Equipment equipment) throws Exception {
		OeeEvent event = new OeeEvent(equipment);
		event.setEventType(type);
		event.setSourceId(OPER_SOURCE_ID);
		return event;
	}

	private void toggleAvailabilityPeriod(boolean visible) {
		lbAvailabilityEndDate.setVisible(visible);
		dpAvailabilityEndDate.setVisible(visible);

		lbAvailabilityEndTime.setVisible(visible);
		tfAvailabilityEndTime.setVisible(visible);

		lbAvailabilityHours.setVisible(visible);
		tfAvailabilityHours.setVisible(visible);

		lbAvailabilityMinutes.setVisible(visible);
		tfAvailabilityMinutes.setVisible(visible);

		lbAvailabilityReason.setText(null);

		initializeDateTimes();
	}

	private void toggleProductionPeriod(boolean visible) {
		lbProductionEndDate.setVisible(visible);
		dpProductionEndDate.setVisible(visible);

		lbProductionEndTime.setVisible(visible);
		tfProductionEndTime.setVisible(visible);

		lbProductionReason.setText(null);

		tfProductionAmount.setText(null);

		initializeDateTimes();
	}

	@FXML
	private void onAvailabilityByEvent() {
		// each event is being entered separately
		toggleAvailabilityPeriod(false);
	}

	@FXML
	private void onAvailabilityByPeriod() {
		// events are summarized
		toggleAvailabilityPeriod(true);
	}

	@FXML
	private void onProductionByEvent() {
		// each event is being entered separately
		toggleProductionPeriod(false);
	}

	@FXML
	private void onProductionByPeriod() {
		// events are summarized
		toggleProductionPeriod(true);
	}
}
