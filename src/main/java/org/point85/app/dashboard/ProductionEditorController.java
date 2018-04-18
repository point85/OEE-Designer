package org.point85.app.dashboard;

import org.point85.app.AppUtils;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.script.EventType;
import org.point85.domain.uom.UnitOfMeasure;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

public class ProductionEditorController extends EventEditorController {
	private OeeEvent productionEvent;

	private EquipmentMaterial equipmentMaterial;

	@FXML
	private RadioButton rbGood;

	@FXML
	private RadioButton rbReject;

	@FXML
	private RadioButton rbStartup;

	@FXML
	private TextField tfAmount;

	@FXML
	private Label lbUOM;

	public void initializeEditor(OeeEvent event) throws Exception {
		productionEvent = event;

		// images for buttons
		setImages();

		getDialogStage().setOnShown((we) -> {
			try {
				displayAttributes();
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});
	}

	@Override
	protected void saveRecord() throws Exception {
		// time period
		setTimePeriod(productionEvent);

		// amount
		Double amount = AppUtils.stringToDouble(tfAmount.getText());

		if (amount == null || amount <= 0d) {
			throw new Exception("An amount must be specified.");
		}
		productionEvent.setAmount(amount);

		PersistenceService.instance().save(productionEvent);
	}

	private EquipmentMaterial getEquipmentMaterial() throws Exception {
		if (equipmentMaterial == null) {
			// get from equipment material
			Equipment equipment = productionEvent.getEquipment();
			OeeEvent lastSetup = PersistenceService.instance().fetchLastSetup(equipment);

			if (lastSetup == null) {
				throw new Exception("No setup record found for equipment " + equipment.getName());
			}

			Material material = lastSetup.getMaterial();
			equipmentMaterial = equipment.getEquipmentMaterial(material);

			if (equipmentMaterial == null) {
				throw new Exception("No rate definition found for equipment " + equipment.getName() + " and material "
						+ material.getDisplayString());
			}
		}
		return equipmentMaterial;
	}

	void displayAttributes() throws Exception {
		// start date and time
		super.displayAttributes(productionEvent);

		// amount
		if (productionEvent.getAmount() != null) {
			tfAmount.setText(Double.toString(productionEvent.getAmount()));
		}

		// UOM
		UnitOfMeasure uom = productionEvent.getUOM();
		if (uom != null) {
			lbUOM.setText(productionEvent.getUOM().getSymbol());
		}
	}

	@FXML
	private void onSelectProductionType() throws Exception {
		UnitOfMeasure uom = null;
		EventType type = null;
		if (rbGood.isSelected()) {
			// good production
			uom = getEquipmentMaterial().getRunRateUOM().getDividend();
			type = EventType.PROD_GOOD;
		} else if (rbReject.isSelected()) {
			// reject or rework production
			uom = getEquipmentMaterial().getRejectUOM();
			type = EventType.PROD_REJECT;
		} else {
			// startup loss
			uom = getEquipmentMaterial().getRunRateUOM().getDividend();
			type = EventType.PROD_STARTUP;
		}
		productionEvent.setUOM(uom);
		productionEvent.setResolverType(type);

		lbUOM.setText(uom.getSymbol());
	}
}
