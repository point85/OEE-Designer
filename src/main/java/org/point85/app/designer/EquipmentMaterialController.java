package org.point85.app.designer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.Images;
import org.point85.domain.persistence.PersistencyService;
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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

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

	void initialize(DesignerApplication app) {
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
	}

	void showMaterial(Equipment equipment) {
		Set<EquipmentMaterial> eqms = equipment.getEquipmentMaterials();

		equipmentMaterials.clear();
		for (EquipmentMaterial eqm : eqms) {
			equipmentMaterials.add(eqm);
		}
		this.tvMaterial.refresh();
	}

	void setEquipmentMaterials(Equipment equipment) {
		Set<EquipmentMaterial> equipmentMaterials = new HashSet<>();
		equipmentMaterials.addAll(equipmentMaterials);
		equipment.setEquipmentMaterials(equipmentMaterials);
	}

	protected void setImages() {
		// new equipment material
		btNewMaterial.setGraphic(new ImageView(Images.newImage));
		btNewMaterial.setContentDisplay(ContentDisplay.RIGHT);

		// add equipment material
		btAddMaterial.setGraphic(new ImageView(Images.addImage));
		btAddMaterial.setContentDisplay(ContentDisplay.RIGHT);

		// remove equipment material
		btRemoveMaterial.setGraphic(new ImageView(Images.removeImage));
		btRemoveMaterial.setContentDisplay(ContentDisplay.RIGHT);

		// find material
		btFindMaterial.setGraphic(new ImageView(Images.materialImage));
		btFindMaterial.setContentDisplay(ContentDisplay.CENTER);

		// find UOMs
		btFindIRRUnit.setGraphic(new ImageView(Images.uomImage));
		btFindIRRUnit.setContentDisplay(ContentDisplay.CENTER);

		btFindRejectUnit.setGraphic(new ImageView(Images.uomImage));
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

			if (selectedEquipmentMaterial != null) {
				selectedEquipmentMaterial.setMaterial(material);
			}

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

			if (selectedEquipmentMaterial == null) {
				return;
			}

			if (source.equals(btFindIRRUnit)) {
				lbIRRUnit.setText(symbol);
				selectedEquipmentMaterial.setRunRateUOM(uom);
			} else if (source.equals(btFindRejectUnit)) {
				lbRejectUnit.setText(symbol);
				selectedEquipmentMaterial.setRejectUOM(uom);
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void clearEditor() {
		this.lbMatlId.setText("");
		this.lbMatlDescription.setText("");
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
				material = (Material) PersistencyService.instance().fetchByName(Material.MATL_BY_NAME,
						lbMatlId.getText());
				selectedEquipmentMaterial.setMaterial(material);
			}

			// OEE target
			String target = this.tfTargetOEE.getText();
			selectedEquipmentMaterial.setOeeTarget(Quantity.createAmount(target));

			// IRR
			UnitOfMeasure uom = selectedEquipmentMaterial.getRunRateUOM();
			if (uom == null) {
				uom = PersistencyService.instance().fetchUOMBySymbol(this.lbIRRUnit.getText());
				selectedEquipmentMaterial.setRunRateUOM(uom);
			}

			String irr = this.tfIRR.getText();
			selectedEquipmentMaterial.setRunRateAmount(Quantity.createAmount(irr));

			// reject UOM
			uom = selectedEquipmentMaterial.getRejectUOM();
			if (uom == null) {
				uom = PersistencyService.instance().fetchUOMBySymbol(this.lbRejectUnit.getText());
				selectedEquipmentMaterial.setRejectUOM(uom);
			}

			// add to equipment if necessary
			addEquipmentMaterial();

			this.tvMaterial.refresh();

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

			selectedEquipmentMaterial = null;

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void addEquipmentMaterial() throws Exception {
		if (selectedEquipmentMaterial.getKey() != null) {
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

			Equipment equipment = selectedEquipmentMaterial.getEquipment();
			equipment.removeEquipmentMaterial(selectedEquipmentMaterial);

			equipmentMaterials.remove(selectedEquipmentMaterial);
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

		// target OEE
		tfTargetOEE.setText(AppUtils.formatDouble(eqm.getOeeTarget()));

		// design speed
		tfIRR.setText(AppUtils.formatDouble(eqm.getRunRateAmount()));
		lbIRRUnit.setText(eqm.getRunRateUOM().getSymbol());

		// reject UOM
		lbRejectUnit.setText(eqm.getRejectUOM().getSymbol());

		btAddMaterial.setText("Update");
	}

	public EquipmentMaterial getSelectedEquipmentMaterial() {
		if (selectedEquipmentMaterial == null) {
			selectedEquipmentMaterial = new EquipmentMaterial();
		}
		return selectedEquipmentMaterial;
	}

}
