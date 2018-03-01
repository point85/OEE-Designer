package org.point85.app.uom;

import java.util.Collections;
import java.util.List;

import org.point85.app.DialogController;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class UomImporterController extends DialogController {

	// list of UnitTypes
	private ObservableList<String> unitTypes = FXCollections.observableArrayList();

	// list of UOMs of this type
	private ObservableList<UomItem> unitItems = FXCollections.observableArrayList();

	@FXML
	private ComboBox<String> cbUnitTypes;

	@FXML
	private ComboBox<UomItem> cbAvailableUnits;

	UnitOfMeasure getSelectedUom() {
		UnitOfMeasure selectedUom = null;
		UomItem item = cbAvailableUnits.getSelectionModel().getSelectedItem();
		if (item != null) {
			selectedUom = item.getUOM();
		}
		return selectedUom;
	}

	@FXML
	public void initialize() {
		// set unit types
		for (UnitType unitType : UnitType.values()) {
			unitTypes.add(unitType.toString());
		}
		Collections.sort(unitTypes);

		cbUnitTypes.setItems(unitTypes);

		cbAvailableUnits.setItems(unitItems);

		// control images
		setImages();
	}

	@FXML
	private void setPossibleUnits() throws Exception {
		String type = this.cbUnitTypes.getSelectionModel().getSelectedItem();

		List<UnitOfMeasure> uoms = MeasurementSystem.instance().getUnitsOfMeasure(UnitType.valueOf(type));

		unitItems.clear();
		for (UnitOfMeasure uom : uoms) {
			unitItems.add(new UomItem(uom));
		}
	}

	// images for buttons
	@Override
	protected void setImages() {
		super.setImages();
	}

	private class UomItem {
		private UnitOfMeasure uom;

		private UomItem(UnitOfMeasure uom) {
			this.uom = uom;
		}

		private UnitOfMeasure getUOM() {
			return uom;
		}

		@Override
		public String toString() {
			return uom.getSymbol() + " (" + uom.getName() + " )";
		}
	}
}
