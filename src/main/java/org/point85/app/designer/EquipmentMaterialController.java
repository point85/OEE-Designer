package org.point85.app.designer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.UnitOfMeasure;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class EquipmentMaterialController extends DesignerController {

	// equipment materials
	private ObservableList<EquipmentMaterial> equipmentMaterials = FXCollections.observableArrayList(new ArrayList<>());

	// equipment material being edited
	private EquipmentMaterial selectedEquipmentMaterial;

	// equipment material section
	@FXML
	private Label lbMatlId;

	@FXML
	private Label lbMatlDescription;

	@FXML
	private CheckBox ckDefaultMaterial;

	@FXML
	private Button btFindMaterial;

	@FXML
	private TextField tfTargetOEE;

	@FXML
	private TextField tfIRR;

	@FXML
	private Label lbIRRUnit;

	@FXML
	private Button btFindIRRUnit;

	@FXML
	private Label lbRejectUnit;

	@FXML
	private Button btFindRejectUnit;

	@FXML
	private Button btNewMaterial;

	@FXML
	private Button btAddMaterial;

	@FXML
	private Button btRemoveMaterial;

	@FXML
	private TableView<EquipmentMaterial> tvMaterial;

	@FXML
	private TableColumn<EquipmentMaterial, String> materialCol;

	@FXML
	private TableColumn<EquipmentMaterial, String> targetOeeCol;

	@FXML
	private TableColumn<EquipmentMaterial, String> iRRCol;

	@FXML
	private TableColumn<EquipmentMaterial, String> iRRUnitCol;

	@FXML
	private TableColumn<EquipmentMaterial, String> rejectUnitCol;

	@FXML
	private TableColumn<EquipmentMaterial, String> defaultCol;

	void initialize(DesignerApplication app) throws Exception {
		setApp(app);
		setImages();
		initializeMaterialTable();
	}

	private void initializeMaterialTable() {
		// bind to list of equipment material
		tvMaterial.setItems(equipmentMaterials);

		// add the listener for material equipment selection
		tvMaterial.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue != null) {
				try {
					onSelectEquipmentMaterial(newValue);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			}
		});

		// material
		materialCol.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			if (cellDataFeatures.getValue() != null && cellDataFeatures.getValue().getMaterial() != null) {
				property = new SimpleStringProperty(cellDataFeatures.getValue().getMaterial().getName());
			}
			return property;
		});

		// target OEE
		targetOeeCol.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			if (cellDataFeatures.getValue() != null) {
				property = new SimpleStringProperty(AppUtils.formatDouble(cellDataFeatures.getValue().getOeeTarget()));
			}
			return property;
		});

		// IRR amount
		iRRCol.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			if (cellDataFeatures.getValue() != null) {
				property = new SimpleStringProperty(
						AppUtils.formatDouble(cellDataFeatures.getValue().getRunRateAmount()));
			}
			return property;
		});

		// IRR UOM
		iRRUnitCol.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			if (cellDataFeatures.getValue() != null && cellDataFeatures.getValue().getRunRateUOM() != null) {
				property = new SimpleStringProperty(cellDataFeatures.getValue().getRunRateUOM().getSymbol());
			}
			return property;
		});

		// reject UOM
		rejectUnitCol.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			if (cellDataFeatures.getValue() != null && cellDataFeatures.getValue().getRejectUOM() != null) {
				property = new SimpleStringProperty(cellDataFeatures.getValue().getRejectUOM().getSymbol());
			}
			return property;
		});

		// default
		defaultCol.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			if (cellDataFeatures.getValue() != null) {
				property = new SimpleStringProperty(cellDataFeatures.getValue().isDefault().toString());
			}
			return property;
		});
	}

	void showMaterial(Equipment equipment) {
		Set<EquipmentMaterial> eqms = equipment.getEquipmentMaterials();

		equipmentMaterials.clear();
		for (EquipmentMaterial eqm : eqms) {
			equipmentMaterials.add(eqm);
		}

		clearEditor();

		tvMaterial.refresh();
	}

	void setEquipmentMaterials(Equipment equipment) {
		Set<EquipmentMaterial> equipmentMaterials = new HashSet<>();
		equipmentMaterials.addAll(equipmentMaterials);
		equipment.setEquipmentMaterials(equipmentMaterials);
	}

	protected void setImages() throws Exception {
		// new equipment material
		btNewMaterial.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewMaterial.setContentDisplay(ContentDisplay.RIGHT);

		// add equipment material
		btAddMaterial.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddMaterial.setContentDisplay(ContentDisplay.RIGHT);

		// remove equipment material
		btRemoveMaterial.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveMaterial.setContentDisplay(ContentDisplay.RIGHT);

		// find material
		btFindMaterial.setGraphic(ImageManager.instance().getImageView(Images.MATERIAL));
		btFindMaterial.setContentDisplay(ContentDisplay.CENTER);

		// find UOMs
		btFindIRRUnit.setGraphic(ImageManager.instance().getImageView(Images.UOM));
		btFindIRRUnit.setContentDisplay(ContentDisplay.CENTER);

		btFindRejectUnit.setGraphic(ImageManager.instance().getImageView(Images.UOM));
		btFindRejectUnit.setContentDisplay(ContentDisplay.CENTER);
	}

	// find material
	@FXML
	private void onFindMaterial() {
		try {
			// get the material from the dialog
			Material material = getApp().showMaterialEditor();

			if (material == null) {
				return;
			}

			// add material to text field
			updateMaterialData(material);

			if (selectedEquipmentMaterial == null) {
				selectedEquipmentMaterial = new EquipmentMaterial();
			}

			selectedEquipmentMaterial.setMaterial(material);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void updateMaterialData(Material material) {
		lbMatlId.setText(material.getName());
		lbMatlDescription.setText(material.getDescription());
	}

	// find UOM
	@FXML
	private void onFindUOM(ActionEvent event) {
		try {
			// get the UOM from the dialog
			UnitOfMeasure uom = getApp().showUomEditor();

			if (uom == null) {
				return;
			}
			Button source = (Button) event.getSource();

			String symbol = uom.getSymbol();

			if (source.equals(btFindIRRUnit)) {
				lbIRRUnit.setText(symbol);
				getSelectedEquipmentMaterial().setRunRateUOM(uom);
			} else if (source.equals(btFindRejectUnit)) {
				lbRejectUnit.setText(symbol);
				getSelectedEquipmentMaterial().setRejectUOM(uom);
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void clear() {
		clearEditor();
		equipmentMaterials.clear();
		selectedEquipmentMaterial = null;
	}

	void clearEditor() {
		this.lbMatlId.setText("");
		this.lbMatlDescription.setText("");
		this.ckDefaultMaterial.setSelected(false);
		this.tfTargetOEE.setText("");
		this.tfIRR.setText("");
		this.lbIRRUnit.setText("");
		this.lbRejectUnit.setText("");

		this.tvMaterial.getSelectionModel().clearSelection();
	}

	@FXML
	private void onNewMaterial() {
		try {
			clearEditor();

			btAddMaterial.setText("Add");

			selectedEquipmentMaterial = null;

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onAddMaterial() {
		try {
			if (selectedEquipmentMaterial == null) {
				return;
			}

			if (!(getApp().getPhysicalModelController().getSelectedEntity() instanceof Equipment)) {
				throw new Exception("Equipment must be selected before adding material.");
			}

			// equipment
			Equipment equipment = (Equipment) getApp().getPhysicalModelController().getSelectedEntity();

			// add this equipment material
			selectedEquipmentMaterial.setEquipment(equipment);
			equipment.addEquipmentMaterial(selectedEquipmentMaterial);

			// material, check cache first
			Material material = selectedEquipmentMaterial.getMaterial();

			if (material == null) {
				material = PersistenceService.instance().fetchMaterialByName(lbMatlId.getText());
				selectedEquipmentMaterial.setMaterial(material);
			}
			selectedEquipmentMaterial.setDefault(ckDefaultMaterial.isSelected());

			// OEE target
			String target = tfTargetOEE.getText();
			selectedEquipmentMaterial.setOeeTarget(Quantity.createAmount(target));

			// IRR
			UnitOfMeasure uom = selectedEquipmentMaterial.getRunRateUOM();
			if (uom == null && lbIRRUnit.getText().length() > 0) {
				uom = PersistenceService.instance().fetchUOMBySymbol(lbIRRUnit.getText());
				selectedEquipmentMaterial.setRunRateUOM(uom);
			}

			String irr = tfIRR.getText();
			selectedEquipmentMaterial.setRunRateAmount(Quantity.createAmount(irr));

			// reject UOM
			uom = selectedEquipmentMaterial.getRejectUOM();
			if (uom == null && lbRejectUnit.getText().length() > 0) {
				uom = PersistenceService.instance().fetchUOMBySymbol(lbRejectUnit.getText());
				selectedEquipmentMaterial.setRejectUOM(uom);
			}

			// add to equipment if necessary
			addEquipmentMaterial();

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

			// tvMaterial.refresh();
			// selectedEquipmentMaterial = null;
			showMaterial(equipment);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void addEquipmentMaterial() throws Exception {
		if (getSelectedEquipmentMaterial().getKey() != null) {
			// already added
			return;
		}

		// add to entity
		PlantEntity selectedEntity = getApp().getPhysicalModelController().getSelectedEntity();

		if (selectedEntity == null || !(selectedEntity instanceof Equipment)) {
			throw new Exception("An equipment entity must be selected before adding material to it.");
		}

		Equipment equipment = ((Equipment) selectedEntity);

		// new resolver for equipment
		if (!equipment.hasEquipmentMaterial(selectedEquipmentMaterial)) {
			equipmentMaterials.add(selectedEquipmentMaterial);
			equipment.addEquipmentMaterial(selectedEquipmentMaterial);
		}
	}

	@FXML
	private void onRemoveMaterial() {
		try {
			if (selectedEquipmentMaterial == null) {
				AppUtils.showErrorDialog("No material for this equipment has been selected for deletion.");
				return;
			}

			Equipment equipment = getSelectedEquipmentMaterial().getEquipment();
			equipment.removeEquipmentMaterial(getSelectedEquipmentMaterial());

			equipmentMaterials.remove(getSelectedEquipmentMaterial());
			selectedEquipmentMaterial = null;
			tvMaterial.getSelectionModel().clearSelection();
			tvMaterial.refresh();

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void onSelectEquipmentMaterial(EquipmentMaterial eqm) {
		selectedEquipmentMaterial = eqm;

		Material material = eqm.getMaterial();

		// material
		updateMaterialData(material);
		ckDefaultMaterial.setSelected(eqm.isDefault());

		// target OEE
		tfTargetOEE.setText(AppUtils.formatDouble(eqm.getOeeTarget()));

		// design speed
		tfIRR.setText(AppUtils.formatDouble(eqm.getRunRateAmount()));

		if (eqm.getRunRateUOM() != null) {
			lbIRRUnit.setText(eqm.getRunRateUOM().getSymbol());
		}

		// reject UOM
		if (eqm.getRejectUOM() != null) {
			lbRejectUnit.setText(eqm.getRejectUOM().getSymbol());
		}

		btAddMaterial.setText("Update");
	}

	public EquipmentMaterial getSelectedEquipmentMaterial() {
		return selectedEquipmentMaterial;
	}

}
