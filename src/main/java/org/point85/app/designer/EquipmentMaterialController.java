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
import org.point85.domain.uom.MeasurementType;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;

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
	private final ObservableList<EquipmentMaterial> equipmentMaterials = FXCollections
			.observableArrayList(new ArrayList<>());

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
	private TableColumn<EquipmentMaterial, String> materialDescCol;

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
			} else {
				clearEditor();
			}
		});

		// material name
		materialCol.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			if (cellDataFeatures.getValue() != null && cellDataFeatures.getValue().getMaterial() != null) {
				property = new SimpleStringProperty(cellDataFeatures.getValue().getMaterial().getName());
			}
			return property;
		});

		// material description
		materialDescCol.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			if (cellDataFeatures.getValue() != null && cellDataFeatures.getValue().getMaterial() != null) {
				property = new SimpleStringProperty(cellDataFeatures.getValue().getMaterial().getDescription());
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

		// default material
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

	protected void setImages() {
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
				if (!uom.getMeasurementType().equals(MeasurementType.QUOTIENT)) {
					throw new Exception(DesignerLocalizer.instance().getErrorString("not.quotient", symbol));
				} else {
					if (!uom.getDivisor().getUnitType().equals(UnitType.TIME)) {
						throw new Exception(DesignerLocalizer.instance().getErrorString("not.rate", symbol));
					}
				}

				lbIRRUnit.setText(symbol);

				if (getSelectedEquipmentMaterial() != null) {
					getSelectedEquipmentMaterial().setRunRateUOM(uom);
				}
			} else if (source.equals(btFindRejectUnit)) {
				if (!uom.getMeasurementType().equals(MeasurementType.SCALAR)) {
					throw new Exception(DesignerLocalizer.instance().getErrorString("not.scalar", symbol));
				}

				lbRejectUnit.setText(symbol);
				if (getSelectedEquipmentMaterial() != null) {
					getSelectedEquipmentMaterial().setRejectUOM(uom);
				}
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
		this.lbMatlId.setText(null);
		this.lbMatlDescription.setText(null);
		this.ckDefaultMaterial.setSelected(false);
		this.tfTargetOEE.setText(null);
		this.tfIRR.setText(null);
		this.lbIRRUnit.setText(null);
		this.lbRejectUnit.setText(null);
		this.btAddMaterial.setText(DesignerLocalizer.instance().getLangString("add"));

		this.tvMaterial.getSelectionModel().clearSelection();
	}

	@FXML
	private void onNewMaterial() {
		try {
			clearEditor();

			btAddMaterial.setText(DesignerLocalizer.instance().getLangString("add"));

			selectedEquipmentMaterial = null;

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onAddOrUpdateMaterial() {
		try {
			if (selectedEquipmentMaterial == null) {
				return;
			}

			PlantEntity plantEntity = getApp().getPhysicalModelController().getSelectedEntity();

			if (!(plantEntity instanceof Equipment)) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.equipment"));
			}

			// equipment
			Equipment equipment = (Equipment) plantEntity;

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
			if (uom == null && lbIRRUnit.getText() != null && lbIRRUnit.getText().length() > 0) {
				uom = PersistenceService.instance().fetchUomBySymbol(lbIRRUnit.getText());
				selectedEquipmentMaterial.setRunRateUOM(uom);
			}

			String irr = tfIRR.getText();
			selectedEquipmentMaterial.setRunRateAmount(Quantity.createAmount(irr));

			// reject UOM
			uom = selectedEquipmentMaterial.getRejectUOM();
			if (uom == null && lbRejectUnit.getText() != null && lbRejectUnit.getText().length() > 0) {
				uom = PersistenceService.instance().fetchUomBySymbol(lbRejectUnit.getText());
				selectedEquipmentMaterial.setRejectUOM(uom);
			}

			// add this equipment material
			selectedEquipmentMaterial.setEquipment(equipment);

			if (!equipmentMaterials.contains(selectedEquipmentMaterial)) {
				equipmentMaterials.add(selectedEquipmentMaterial);
			}

			Set<EquipmentMaterial> materials = new HashSet<>();
			materials.addAll(equipmentMaterials);
			equipment.setEquipmentMaterials(materials);

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

			tvMaterial.getSelectionModel().clearSelection();
			selectedEquipmentMaterial = null;

			tvMaterial.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRemoveMaterial() {
		try {
			if (selectedEquipmentMaterial == null) {
				AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.material.selected"));
				return;
			}

			Equipment equipment = getSelectedEquipmentMaterial().getEquipment();
			equipment.removeEquipmentMaterial(getSelectedEquipmentMaterial());

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

			equipmentMaterials.remove(getSelectedEquipmentMaterial());
			selectedEquipmentMaterial = null;
			tvMaterial.getSelectionModel().clearSelection();
			tvMaterial.refresh();
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

		btAddMaterial.setText(DesignerLocalizer.instance().getLangString("update"));
	}

	public EquipmentMaterial getSelectedEquipmentMaterial() {
		return selectedEquipmentMaterial;
	}
}
