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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.DomainUtils;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.MeasurementType;
import org.point85.domain.uom.Prefix;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Controller for the UOM editor
 * 
 * @author Kent Randall
 *
 */
public class UomEditorController extends DesignerDialogController {
	// select UOM
	private TreeItem<UomNode> selectedUomItem;

	// list of edited UOMs
	private final List<TreeItem<UomNode>> editedUomItems = new ArrayList<>();

	// list of Prefixes
	private final ObservableList<String> prefixes = FXCollections.observableArrayList();

	// list of UnitTypes
	private final ObservableList<String> unitTypes = FXCollections.observableArrayList();

	// tree view by category
	@FXML
	private TreeView<UomNode> tvUoms;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btRefresh;

	@FXML
	private Button btDelete;

	@FXML
	private Button btImport;

	@FXML
	private TextField tfName;

	@FXML
	private TextField tfSymbol;

	@FXML
	private ComboBox<String> cbUnitTypes;

	@FXML
	private ComboBox<String> cbCategories;

	@FXML
	private TextArea taDescription;

	@FXML
	private ComboBox<String> cbScalingFactor;

	@FXML
	private TextField tfOffset;

	@FXML
	private ComboBox<String> cbAbscissaUnits;

	// for product and quotient
	@FXML
	private ComboBox<String> cbUom1Types;

	@FXML
	private ComboBox<String> cbUom1Units;

	@FXML
	private ComboBox<String> cbUom2Types;

	@FXML
	private ComboBox<String> cbUom2Units;

	@FXML
	private RadioButton rbProduct;

	@FXML
	private RadioButton rbQuotient;

	// for power
	@FXML
	private ComboBox<String> cbPowerTypes;

	@FXML
	private ComboBox<String> cbPowerUnits;

	@FXML
	private TextField tfExponent;

	@FXML
	private TabPane tpProductPower;

	@FXML
	private Tab tScalar;

	@FXML
	private Tab tProductQuotient;

	@FXML
	private Tab tPower;

	// context menu
	@FXML
	private MenuItem miRefreshAll;

	@FXML
	private MenuItem miSaveAll;

	// UOM import controller
	private UomImporterController uomImportController;

	// get the display strings for all UOM types
	protected ObservableList<String> getUnitTypes() {
		if (unitTypes.size() == 0) {
			for (UnitType unitType : UnitType.values()) {
				unitTypes.add(unitType.toString());
			}
			Collections.sort(unitTypes);
		}
		return unitTypes;
	}

	// get the display strings for all prefixes
	protected ObservableList<String> getPrefixes() {
		if (prefixes.size() == 0) {
			for (Prefix prefix : Prefix.getDefinedPrefixes()) {
				prefixes.add(prefix.getName());
			}
			prefixes.add(AppUtils.EMPTY_STRING);
			Collections.sort(prefixes);
		}
		return prefixes;
	}

	// extract the UOM from the tree item
	public UnitOfMeasure getSelectedUom() {
		UnitOfMeasure uom = null;

		if (selectedUomItem != null) {
			uom = selectedUomItem.getValue().getUnitOfMeasure();
		}

		return uom;
	}

	@FXML
	public void initialize() throws Exception {
		// images for buttons
		setImages();

		// unit types
		ObservableList<String> unitTypes = getUnitTypes();

		// scalar unit type
		cbUnitTypes.getItems().addAll(unitTypes);
		cbUnitTypes.getSelectionModel().select(UnitType.UNCLASSIFIED.toString());

		// UOM1 unit type
		this.cbUom1Types.getItems().addAll(unitTypes);

		// UOM2 unit type
		this.cbUom2Types.getItems().addAll(unitTypes);

		// power type
		this.cbPowerTypes.getItems().addAll(unitTypes);

		// add the tree view listener for UOM selection
		tvUoms.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectUom(newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});
		tvUoms.setShowRoot(false);

		// fill in the top-level category nodes
		populateCategories();

		// set scaling factor prefixes
		ObservableList<String> prefixes = getPrefixes();
		cbScalingFactor.getItems().addAll(prefixes);

		// refresh tree view
		tvUoms.setRoot(getRootUomItem());
		tvUoms.setShowRoot(false);
		tvUoms.refresh();
	}

	// initialize
	public void initializeApp(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

	}

	// the single root for all UOM categories (not persistent)
	private TreeItem<UomNode> getRootUomItem() throws Exception {
		if (tvUoms.getRoot() == null) {
			UnitOfMeasure rootUom = new UnitOfMeasure();
			rootUom.setName(UnitOfMeasure.ROOT_UOM_NAME);
			tvUoms.setRoot(new TreeItem<>(new UomNode(rootUom)));
		}
		return tvUoms.getRoot();
	}

	// images for controls
	@Override
	protected void setImages() throws Exception {
		super.setImages();

		// new
		btNew.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNew.setContentDisplay(ContentDisplay.RIGHT);

		// save
		btSave.setGraphic(ImageManager.instance().getImageView(Images.SAVE));
		btSave.setContentDisplay(ContentDisplay.RIGHT);

		// refresh
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
		btRefresh.setContentDisplay(ContentDisplay.RIGHT);

		// delete
		btDelete.setGraphic(ImageManager.instance().getImageView(Images.DELETE));
		btDelete.setContentDisplay(ContentDisplay.RIGHT);

		// import
		btImport.setGraphic(ImageManager.instance().getImageView(Images.IMPORT));
		btImport.setContentDisplay(ContentDisplay.RIGHT);

		// context menu
		miSaveAll.setGraphic(ImageManager.instance().getImageView(Images.SAVE_ALL));
		miRefreshAll.setGraphic(ImageManager.instance().getImageView(Images.REFRESH_ALL));
	}

	@FXML
	private void onImportUom() throws Exception {
		try {
			if (uomImportController == null) {
				FXMLLoader loader = FXMLLoaderFactory.uomImporterLoader();
				AnchorPane pane = (AnchorPane) loader.getRoot();

				// Create the dialog Stage.
				Stage dialogStage = new Stage(StageStyle.DECORATED);
				dialogStage.setTitle(DesignerLocalizer.instance().getLangString("import.uom.title"));
				dialogStage.initModality(Modality.NONE);
				Scene scene = new Scene(pane);
				dialogStage.setScene(scene);

				// get the controller
				uomImportController = loader.getController();
				uomImportController.setDialogStage(dialogStage);
			}

			// Show the dialog and wait until the user closes it
			uomImportController.getDialogStage().showAndWait();

			if (uomImportController.isCancelled()) {
				return;
			}

			UnitOfMeasure uom = uomImportController.getSelectedUom();

			if (uom == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("unit.cannot.be.null"));
			}

			// make sure that there is a non-null category
			uom.getCategory();

			PersistenceService.instance().fetchReferencedUnits(uom);

			PersistenceService.instance().save(uom);

			onRefreshAllUoms();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// update the editor upon selection of a UOM node
	private void onSelectUom(TreeItem<UomNode> item) throws Exception {
		if (item == null) {
			return;
		}

		// leaf node is UOM
		if (item.getValue().isUnitOfMeasure()) {
			// display UOM properties
			displayAttributes(item.getValue().getUnitOfMeasure());
			selectedUomItem = item;
			return;
		} else {
			// category selected
			selectedUomItem = null;

			cbCategories.getSelectionModel().select(item.getValue().getCategory());
		}

		// show the UOM children too
		List<UnitOfMeasure> children = PersistenceService.instance().fetchUomsByCategory(item.getValue().getCategory());
		boolean hasTreeChildren = item.getChildren().size() > 0 ? true : false;

		// check to see if the node's children have been previously shown
		if (!hasTreeChildren) {
			item.getChildren().clear();

			for (UnitOfMeasure child : children) {
				UomNode uomNode = new UomNode(child);
				TreeItem<UomNode> uomItem = new TreeItem<>(uomNode);
				uomItem.setGraphic(ImageManager.instance().getImageView(Images.UOM));
				item.getChildren().add(uomItem);
			}
		}
		item.setExpanded(true);
		tvUoms.refresh();

		// set what units can be worked with
		setPossibleAbscissaUnits();
		setPossiblePowerUnits();
		setPossibleUom1Units();
		setPossibleUom2Units();
	}

	// select the matching display string
	private void selectSymbol(UnitOfMeasure uom, ComboBox<String> combobox) {
		if (combobox == null) {
			return;
		}

		combobox.getSelectionModel().select(uom.toDisplayString());
	}

	// show the UOM attributes
	private void displayAttributes(UnitOfMeasure uom) {

		this.tfName.setText(uom.getName());
		this.tfSymbol.setText(uom.getSymbol());
		this.taDescription.setText(uom.getDescription());
		this.cbCategories.setValue(uom.getCategory());
		this.cbUnitTypes.getSelectionModel().select(uom.getUnitType().toString());

		// regular conversion
		double scalingFactor = uom.getScalingFactor();
		UnitOfMeasure abscissaUnit = uom.getAbscissaUnit();
		double offset = uom.getOffset();

		// regular conversion
		String factorText = String.valueOf(scalingFactor);
		UnitOfMeasure displayAbscissa = abscissaUnit;
		String offsetText = AppUtils.formatDouble(offset);

		// scaling
		Prefix prefix = Prefix.fromFactor(scalingFactor);

		if (prefix != null) {
			cbScalingFactor.setValue(prefix.getName());
		} else {
			cbScalingFactor.setValue(factorText);
		}

		// X-axis unit
		selectSymbol(displayAbscissa, cbAbscissaUnits);

		// offset
		tfOffset.setText(offsetText);

		// UOM1
		switch (uom.getMeasurementType()) {
		case PRODUCT: {
			rbProduct.setSelected(true);

			UnitOfMeasure multiplier = uom.getMultiplier();
			UnitOfMeasure multiplicand = uom.getMultiplicand();

			// multiplier UOM
			cbUom1Types.getSelectionModel().select(multiplier.getUnitType().toString());
			selectSymbol(multiplier, cbUom1Units);

			// multiplicand UOM
			cbUom2Types.getSelectionModel().select(multiplicand.getUnitType().toString());
			selectSymbol(multiplicand, cbUom2Units);

			tpProductPower.getSelectionModel().select(tProductQuotient);
			break;
		}

		case QUOTIENT: {
			rbQuotient.setSelected(true);

			UnitOfMeasure dividend = uom.getDividend();
			UnitOfMeasure divisor = uom.getDivisor();

			// dividend UOM
			cbUom1Types.getSelectionModel().select(dividend.getUnitType().toString());
			selectSymbol(dividend, cbUom1Units);

			// divisor UOM
			cbUom2Types.getSelectionModel().select(divisor.getUnitType().toString());
			selectSymbol(divisor, cbUom2Units);

			tpProductPower.getSelectionModel().select(tProductQuotient);
			break;
		}

		case POWER: {
			UnitOfMeasure base = uom.getPowerBase();

			// base of power UOM
			cbPowerTypes.getSelectionModel().select(base.getUnitType().toString());
			selectSymbol(base, cbPowerUnits);

			// exponent
			tfExponent.setText(uom.getPowerExponent().toString());

			tpProductPower.getSelectionModel().select(tPower);
			break;
		}

		case SCALAR: {
			// clear product, quotient and power for a scalar
			rbProduct.setSelected(false);
			rbQuotient.setSelected(false);

			cbUom1Types.getSelectionModel().clearSelection();
			cbUom1Units.getSelectionModel().clearSelection();

			cbUom2Types.getSelectionModel().clearSelection();
			cbUom2Units.getSelectionModel().clearSelection();

			cbPowerTypes.getSelectionModel().clearSelection();
			cbPowerUnits.getSelectionModel().clearSelection();

			tfExponent.setText(null);

			tpProductPower.getSelectionModel().select(tScalar);
			break;
		}

		default:
			break;
		}
	}

	// populate the tree view categories
	private void populateCategories() throws Exception {
		getRootUomItem().getChildren().clear();

		// fetch the categories
		List<String> categories = PersistenceService.instance().fetchUomCategories();
		Collections.sort(categories);

		for (String category : categories) {
			UomNode categoryNode = new UomNode(category);
			TreeItem<UomNode> categoryItem = new TreeItem<>(categoryNode);
			categoryItem.setGraphic(ImageManager.instance().getImageView(Images.CATEGORY));
			getRootUomItem().getChildren().add(categoryItem);
		}

		// also in the drop down
		cbCategories.getItems().clear();
		cbCategories.getItems().addAll(categories);
	}

	@FXML
	private void onNewUom() {
		try {
			// main
			this.tfName.clear();
			this.tfName.requestFocus();
			this.tfSymbol.clear();
			this.cbCategories.getSelectionModel().clearSelection();
			this.cbCategories.setValue(null);
			this.cbUnitTypes.getSelectionModel().select(UnitType.UNCLASSIFIED.toString());
			this.taDescription.clear();

			// conversion
			this.cbScalingFactor.setValue(null);
			this.cbAbscissaUnits.getSelectionModel().clearSelection();
			this.tfOffset.clear();

			// product/quotient
			this.cbUom1Units.getSelectionModel().clearSelection();
			this.cbUom2Units.getSelectionModel().clearSelection();
			this.cbUom1Types.getSelectionModel().clearSelection();
			this.cbUom2Types.getSelectionModel().clearSelection();

			this.rbProduct.setSelected(false);
			this.rbQuotient.setSelected(false);

			// power
			this.cbPowerTypes.getSelectionModel().clearSelection();
			this.cbPowerUnits.getSelectionModel().clearSelection();
			this.tfExponent.clear();

			// scalar by default
			this.tpProductPower.getSelectionModel().select(tScalar);

			// update custom types
			this.setPossibleAbscissaUnits();

			this.selectedUomItem = null;
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveAllUoms() {
		try {
			// save all modified UOMs
			for (TreeItem<UomNode> modifiedItem : editedUomItems) {
				UomNode node = modifiedItem.getValue();
				UnitOfMeasure saved = (UnitOfMeasure) PersistenceService.instance().save(node.getUnitOfMeasure());
				node.setUnitOfMeasure(saved);
				modifiedItem.setGraphic(ImageManager.instance().getImageView(Images.UOM));
			}
			editedUomItems.clear();

			tvUoms.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private UnitOfMeasure createUom() {
		UnitOfMeasure uom = null;
		try {
			// new child
			uom = new UnitOfMeasure();
			selectedUomItem = new TreeItem<>(new UomNode(uom));
			selectedUomItem.setGraphic(ImageManager.instance().getImageView(Images.UOM));

			// set the attributes
			setAttributes(selectedUomItem);
			editedUomItems.add(selectedUomItem);

			// category
			String category = uom.getCategory();

			ObservableList<TreeItem<UomNode>> categoryItems = getRootUomItem().getChildren();
			TreeItem<UomNode> parentCategoryItem = null;

			for (TreeItem<UomNode> categoryItem : categoryItems) {
				if (categoryItem.getValue().getCategory().equals(category)) {
					parentCategoryItem = categoryItem;
					break;
				}
			}

			if (parentCategoryItem == null) {
				// new category
				parentCategoryItem = new TreeItem<>(new UomNode(category));
				parentCategoryItem.setGraphic(ImageManager.instance().getImageView(Images.CATEGORY));
				getRootUomItem().getChildren().add(parentCategoryItem);
			}

			parentCategoryItem.getChildren().add(selectedUomItem);
			parentCategoryItem.setExpanded(true);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
		return uom;
	}

	@FXML
	private void onSaveUom() {
		try {
			if (selectedUomItem == null) {
				// create
				createUom();
			} else {
				// update
				setAttributes(selectedUomItem);
			}

			UnitOfMeasure uom = getSelectedUom();
			UnitOfMeasure saved = (UnitOfMeasure) PersistenceService.instance().save(uom);
			selectedUomItem.getValue().setUnitOfMeasure(saved);
			selectedUomItem.setGraphic(ImageManager.instance().getImageView(Images.UOM));

			editedUomItems.remove(selectedUomItem);

			tvUoms.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void markedChanged() throws Exception {
		if (selectedUomItem != null) {
			selectedUomItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
		}
	}

	// set the UOM attributes from the UI
	private void setAttributes(TreeItem<UomNode> uomItem) throws Exception {
		if (uomItem == null || !uomItem.getValue().isUnitOfMeasure()) {
			return;
		}
		UnitOfMeasure uom = uomItem.getValue().getUnitOfMeasure();

		// unit attributes
		String name = this.tfName.getText().trim();

		if (name.length() == 0) {
			AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.uom.name"));
			return;
		}

		String symbol = this.tfSymbol.getText().trim();
		String category = this.cbCategories.getSelectionModel().getSelectedItem();

		if (category == null) {
			category = DesignerLocalizer.instance().getLangString("uncategorized");
		}

		String type = this.cbUnitTypes.getSelectionModel().getSelectedItem();

		if (type == null) {
			AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.uom.type"));
			return;
		}

		// type of unit
		UnitType unitType = UnitType.valueOf(type);

		// description
		String description = this.taDescription.getText();

		if (uom != null) {
			uom.setUnitType(unitType);
			uom.setName(name);
			uom.setSymbol(symbol);
			uom.setDescription(description);
			uom.setCategory(category);
		}

		// scalar, product, quotient or power
		if (tPower.isSelected()) {
			// power base
			String baseSymbol = AppUtils.parseSymbol(cbPowerUnits.getSelectionModel().getSelectedItem());

			if (baseSymbol == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.base.uom"));
			}
			UnitOfMeasure base = AppUtils.getUOMForEditing(baseSymbol);

			if (base == null) {
				return;
			}

			if (!base.getMeasurementType().equals(MeasurementType.SCALAR)) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("base.scalar"));
			}

			// exponent
			if (tfExponent.getText() == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.exponent"));
			}

			String exp = tfExponent.getText().trim();
			Integer exponent = Integer.valueOf(exp);

			if (uom == null) {
				// new
				uom = MeasurementSystem.instance().createPowerUOM(unitType, name, symbol, description, base, exponent);
			} else {
				// update
				uom.setPowerUnit(base, exponent);
			}

		} else if (tProductQuotient.isSelected()) {
			// product or quotient
			String uom1Symbol = AppUtils.parseSymbol(cbUom1Units.getSelectionModel().getSelectedItem());
			UnitOfMeasure uom1 = AppUtils.getUOMForEditing(uom1Symbol);

			if (uom1 == null) {
				return;
			}

			if (!uom1.getMeasurementType().equals(MeasurementType.SCALAR)) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("not.scalar.multiplier"));
			}

			String uom2Symbol = AppUtils.parseSymbol(cbUom2Units.getSelectionModel().getSelectedItem());
			UnitOfMeasure uom2 = AppUtils.getUOMForEditing(uom2Symbol);

			if (!uom2.getMeasurementType().equals(MeasurementType.SCALAR)) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("not.scalar.multiplicand"));
			}

			if (rbProduct.isSelected()) {
				// product
				if (uom == null) {
					// new
					uom = MeasurementSystem.instance().createProductUOM(unitType, name, symbol, description, uom1,
							uom2);
				} else {
					// update
					uom.setProductUnits(uom1, uom2);
				}

			} else if (rbQuotient.isSelected()) {
				// quotient
				if (uom == null) {
					// new
					uom = MeasurementSystem.instance().createQuotientUOM(unitType, name, symbol, description, uom1,
							uom2);
				} else {
					// update
					uom.setQuotientUnits(uom1, uom2);
				}
			} else {
				throw new Exception(DesignerLocalizer.instance().getErrorString("select.product.quotient"));
			}
		} else if (tScalar.isSelected()) {
			// create scalar UOM
			if (uom == null) {
				// new
				uom = MeasurementSystem.instance().createScalarUOM(unitType, name, symbol, description);
			}
		} else {
			// should not happen
		}

		// conversion scaling factor
		double scalingFactor = 1d;
		Prefix prefix = Prefix.fromName(cbScalingFactor.getValue());

		if (prefix != null) {
			scalingFactor = prefix.getFactor();
		} else {
			String factor = cbScalingFactor.getValue();

			if (factor != null && factor.length() > 0) {
				scalingFactor = Quantity.createAmount(DomainUtils.removeThousandsSeparator(factor));
			}
		}

		// conversion UOM
		UnitOfMeasure abscissaUnit = null;
		String abscissaSymbol = AppUtils.parseSymbol(cbAbscissaUnits.getSelectionModel().getSelectedItem());
		if (abscissaSymbol != null) {
			abscissaUnit = AppUtils.getUOMForEditing(abscissaSymbol);
		} else {
			uom.setAbscissaUnit(uom);
		}

		// conversion offset
		String offsetValue = tfOffset.getText().trim();
		double offset = 0d;

		if (offsetValue.length() > 0) {
			offset = Quantity.createAmount(DomainUtils.removeThousandsSeparator(offsetValue));
		}

		if (abscissaUnit != null) {
			// regular conversion
			uom.setConversion(scalingFactor, abscissaUnit, offset);
		}

		// clear its conversion cache
		uom.clearCache();

		// update categories
		if (uom.getKey() != null) {
			populateCategories();
		}

		// update UOM choices by UOM
		setPossibleAbscissaUnits();
		setPossiblePowerUnits();
		setPossibleUom1Units();
		setPossibleUom2Units();

		boolean contains = false;
		for (TreeItem<UomNode> item : editedUomItems) {
			if (item.getValue().getUnitOfMeasure().getSymbol().equals(uom.getSymbol())) {
				contains = true;
				break;
			}
		}

		if (!contains) {
			editedUomItems.add(uomItem);
		}

		markedChanged();

		tvUoms.refresh();

		// update categories
		if (!cbCategories.getItems().contains(category)) {
			cbCategories.getItems().add(category);
			Collections.sort(cbCategories.getItems());
		}
	}

	// Delete button clicked
	@FXML
	private void onDeleteUom() {
		if (selectedUomItem == null || !selectedUomItem.getValue().isUnitOfMeasure()) {
			AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("unit.cannot.be.null"));
			return;
		}

		// confirm
		ButtonType type = AppUtils.showConfirmationDialog(
				DesignerLocalizer.instance().getLangString("uom.delete", getSelectedUom().getDisplayString()));

		if (type.equals(ButtonType.CANCEL)) {
			return;
		}

		try {
			// delete
			UnitOfMeasure toDelete = getSelectedUom();

			PersistenceService.instance().delete(toDelete);

			// remove this UOM from the tree
			TreeItem<UomNode> childNode = tvUoms.getSelectionModel().getSelectedItem();
			TreeItem<UomNode> parentNode = childNode.getParent();

			parentNode.getChildren().remove(childNode);
			tvUoms.refresh();
			parentNode.setExpanded(true);

			// update category list
			populateCategories();

			// clear editor
			onNewUom();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void resetGraphic(TreeItem<UomNode> uomItem) throws Exception {
		uomItem.setGraphic(ImageManager.instance().getImageView(Images.UOM));
	}

	@FXML
	private void onRefreshUom() {
		try {
			if (getSelectedUom() == null) {
				return;
			}

			if (getSelectedUom().getKey() != null) {
				// read from database
				UnitOfMeasure uom = (UnitOfMeasure) PersistenceService.instance()
						.fetchUomByKey(getSelectedUom().getKey());
				selectedUomItem.getValue().setUnitOfMeasure(uom);
				resetGraphic(selectedUomItem);
				displayAttributes(uom);
			} else {
				// remove from tree
				selectedUomItem.getParent().getChildren().remove(selectedUomItem);
			}
			tvUoms.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRefreshAllUoms() {
		try {
			// update category list
			populateCategories();

			// clear editor
			onNewUom();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// Find all possible abscissa units for this type.
	@FXML
	private void setPossibleAbscissaUnits() {
		try {
			String unitType = cbUnitTypes.getSelectionModel().getSelectedItem();

			if (unitType == null) {
				return;
			}

			// for pre-defined units
			ObservableList<String> units = AppUtils.getUnitsOfMeasure(unitType);

			// custom-defined units
			ObservableList<String> customDisplayStrings = AppUtils.getCustomSymbols(UnitType.valueOf(unitType));

			cbAbscissaUnits.getItems().clear();
			cbAbscissaUnits.getItems().addAll(units);
			cbAbscissaUnits.getItems().addAll(customDisplayStrings);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// Find all possible multiplier/dividend units for this type. Called by UI.
	@FXML
	private void setPossibleUom1Units() {
		try {
			String unitType = cbUom1Types.getSelectionModel().getSelectedItem();

			if (unitType == null) {
				return;
			}

			ObservableList<String> units = AppUtils.getUnitsOfMeasure(unitType);
			ObservableList<String> customDisplayStrings = AppUtils.getCustomSymbols(UnitType.valueOf(unitType));

			cbUom1Units.setDisable(false);
			cbUom1Units.getItems().clear();
			cbUom1Units.getItems().addAll(units);
			cbUom1Units.getItems().addAll(customDisplayStrings);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// Find all possible multiplicand/divisor units for this type. Called by UI.
	@FXML
	private void setPossibleUom2Units() {
		try {
			String unitType = cbUom2Types.getSelectionModel().getSelectedItem();

			if (unitType == null) {
				return;
			}

			ObservableList<String> units = AppUtils.getUnitsOfMeasure(unitType);
			ObservableList<String> customDisplayStrings = AppUtils.getCustomSymbols(UnitType.valueOf(unitType));

			cbUom2Units.setDisable(false);
			cbUom2Units.getItems().clear();
			cbUom2Units.getItems().addAll(units);
			cbUom2Units.getItems().addAll(customDisplayStrings);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// Find all possible power units for this type. Called by UI.
	@FXML
	private void setPossiblePowerUnits() {
		try {
			String unitType = cbPowerTypes.getSelectionModel().getSelectedItem();

			if (unitType == null) {
				return;
			}

			ObservableList<String> units = AppUtils.getUnitsOfMeasure(unitType);
			ObservableList<String> customDisplayStrings = AppUtils.getCustomSymbols(UnitType.valueOf(unitType));

			cbPowerUnits.setDisable(false);
			cbPowerUnits.getItems().clear();
			cbPowerUnits.getItems().addAll(units);
			cbPowerUnits.getItems().addAll(customDisplayStrings);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	@FXML
	protected void onCancel() {
		try {
			// close dialog with selected UOM set to null
			this.selectedUomItem = null;
			super.onCancel();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// class for holding attributes of UOM in a tree view leaf node
	private class UomNode {
		// UOM
		private UnitOfMeasure uom;

		// category name
		private String category;

		private UomNode(String category) {
			this.category = category;
		}

		private UomNode(UnitOfMeasure uom) {
			this.uom = uom;
		}

		private String getCategory() {
			return category;
		}

		private UnitOfMeasure getUnitOfMeasure() {
			return uom;
		}

		private void setUnitOfMeasure(UnitOfMeasure uom) {
			this.uom = uom;
		}

		private boolean isUnitOfMeasure() {
			return uom != null ? true : false;
		}

		@Override
		public String toString() {
			if (uom != null) {
				return uom.getName();
			} else {
				return category;
			}
		}
	}
}
