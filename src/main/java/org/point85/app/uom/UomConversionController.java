/*
MIT License

Copyright (c) 2017 Kent Randall

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package org.point85.app.uom;

import java.util.Collections;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.DomainUtils;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Prefix;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;

/**
 * Controller for converting Units of Measure
 * 
 * @author Kent Randall
 *
 */
public class UomConversionController extends DesignerDialogController {
	// list of Prefixes
	private final ObservableList<String> prefixes = FXCollections.observableArrayList();

	// list of UnitTypes
	private final ObservableList<UnitType> unitTypes = FXCollections.observableArrayList();

	@FXML
	private ComboBox<UnitType> cbUnitTypes;

	@FXML
	private ComboBox<String> cbFromPrefixes;

	@FXML
	private ComboBox<String> cbFromUnits;

	@FXML
	private ComboBox<String> cbToPrefixes;

	@FXML
	private ComboBox<String> cbToUnits;

	@FXML
	private TextField tfFromAmount;

	@FXML
	private TextField tfToAmount;

	@FXML
	private Button btConvert;

	// get the display strings for all prefixes
	protected ObservableList<String> getPrefixes() {
		if (prefixes.isEmpty()) {
			for (Prefix prefix : Prefix.getDefinedPrefixes()) {
				prefixes.add(prefix.getName());
			}
			prefixes.add(AppUtils.EMPTY_STRING);
			Collections.sort(prefixes);
		}
		return prefixes;
	}

	// get the display strings for all UOM types
	protected ObservableList<UnitType> getUnitTypes() {
		if (unitTypes.isEmpty()) {
			List<UnitType> types = AppUtils.sortUnitTypes();
			unitTypes.addAll(types);
		}
		return unitTypes;
	}

	@FXML
	public void initialize() {
		// set unit types
		cbUnitTypes.getItems().addAll(getUnitTypes());

		// set prefixes
		ObservableList<String> allPrefixes = getPrefixes();
		cbFromPrefixes.getItems().addAll(allPrefixes);
		cbToPrefixes.getItems().addAll(allPrefixes);

		// control images
		setImages();
	}

	// initialize app
	public void initializeApp(DesignerApplication app) {
		this.setApp(app);
	}

	// Populate from and to conversion comboBoxes. Called when user selects a
	// UOM type
	@FXML
	private void setPossibleConversions() {
		try {
			UnitType unitType = cbUnitTypes.getSelectionModel().getSelectedItem();

			if (unitType == null) {
				return;
			}

			// get all units of this type
			ObservableList<String> units = AppUtils.getUnitsOfMeasure(unitType);

			// get custom units
			ObservableList<String> customDisplayStrings = AppUtils.getCustomSymbols(unitType);

			// set from units
			cbFromUnits.getItems().clear();
			cbFromUnits.getItems().addAll(units);
			cbFromUnits.getItems().addAll(customDisplayStrings);

			// set to units
			cbToUnits.getItems().clear();
			cbToUnits.getItems().addAll(units);
			cbToUnits.getItems().addAll(customDisplayStrings);

			// set prefixes
			cbFromPrefixes.getSelectionModel().select(AppUtils.EMPTY_STRING);
			cbToPrefixes.getSelectionModel().select(AppUtils.EMPTY_STRING);

			// clear amounts
			tfFromAmount.clear();
			tfToAmount.clear();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// get the Prefix from its name
	private Prefix getPrefix(String name) {
		Prefix prefix = null;

		if (name != null && name.length() > 0) {
			prefix = Prefix.fromName(name);
		}
		return prefix;
	}

	@FXML
	private void handleConvertButton() {
		try {
			tfToAmount.clear();

			// from amount
			String text = tfFromAmount.getText().trim();

			if (text.length() == 0) {
				AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.from.amount"));
				return;
			}

			// from amount
			double fromAmount = Quantity.createAmount(DomainUtils.removeThousandsSeparator(text));

			// from prefix
			Prefix fromPrefix = getPrefix(cbFromPrefixes.getSelectionModel().getSelectedItem());

			// to prefix
			Prefix toPrefix = getPrefix(cbToPrefixes.getSelectionModel().getSelectedItem());

			// from UOM
			String symbol = AppUtils.parseSymbol(cbFromUnits.getSelectionModel().getSelectedItem());
			UnitOfMeasure fromUOM = AppUtils.getUOMForConversion(fromPrefix, symbol);

			if (fromUOM == null) {
				AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.from.uom"));
				return;
			}

			// to UOM
			symbol = AppUtils.parseSymbol(cbToUnits.getSelectionModel().getSelectedItem());
			UnitOfMeasure toUOM = AppUtils.getUOMForConversion(toPrefix, symbol);

			if (toUOM == null) {
				AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.to.uom"));
				return;
			}

			// from quantity
			Quantity fromQuantity = new Quantity(fromAmount, fromUOM);

			// converted quantity
			Quantity toQuantity = fromQuantity.convert(toUOM);

			// converted amount
			double toAmount = toQuantity.getAmount();
			String toShow = AppUtils.formatDouble(toAmount);
			tfToAmount.setText(toShow);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// show the editor dialog
	@FXML
	private void handleEditorButton() {
		try {
			getApp().showUomEditor();

			// UOMs could have been changed
			MeasurementSystem.instance().clearCache();

			// refresh units
			setPossibleConversions();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// images for buttons
	@Override
	protected void setImages() {
		super.setImages();

		// converter
		btConvert.setGraphic(ImageManager.instance().getImageView(Images.CONVERT));
		btConvert.setContentDisplay(ContentDisplay.LEFT);
	}
}
