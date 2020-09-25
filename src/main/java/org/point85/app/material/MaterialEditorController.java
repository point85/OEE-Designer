package org.point85.app.material;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.MaterialNode;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Material;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;

/**
 * Controller for editing and viewing material.
 * 
 * @author Kent Randall
 *
 */
public class MaterialEditorController extends DesignerDialogController {
	// Material being edited or viewed
	private TreeItem<MaterialNode> selectedMaterialItem;

	// edited materials
	private final Set<TreeItem<MaterialNode>> editedMaterialItems = new HashSet<>();

	// material hierarchy by category
	@FXML
	private TreeView<MaterialNode> tvMaterials;

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
	private TextArea taDescription;

	@FXML
	private ComboBox<String> cbCategories;

	// context menu
	@FXML
	private MenuItem miRefreshAll;

	@FXML
	private MenuItem miSaveAll;

	// extract the Material from the tree item
	public Material getSelectedMaterial() {
		Material material = null;

		if (selectedMaterialItem != null) {
			material = selectedMaterialItem.getValue().getMaterial();
		}
		return material;
	}

	@FXML
	public void initialize() {
		// images for buttons
		setImages();

		// add the tree view listener for material selection
		tvMaterials.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectMaterial(oldValue, newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		tvMaterials.setShowRoot(false);
	}

	// initialize
	public void initialize(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

		// fill in the top-level material nodes
		populateCategories();
	}

	// material selected in the hierarchy
	private void onSelectMaterial(TreeItem<MaterialNode> oldItem, TreeItem<MaterialNode> newItem) throws Exception {
		if (newItem == null) {
			return;
		}

		// check for previous edit
		if (oldItem != null && oldItem.getValue().isMaterial()) {
			boolean isChanged = setAttributes(oldItem);

			if (isChanged) {
				oldItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
				tvMaterials.refresh();
			}
		}

		// leaf node is material
		if (newItem.getValue().isMaterial()) {
			// display Material properties
			displayAttributes(newItem.getValue().getMaterial());
			selectedMaterialItem = newItem;
			return;
		} else {
			// category selected
			selectedMaterialItem = null;
		}

		// show the material children too
		List<Material> children = PersistenceService.instance()
				.fetchMaterialsByCategory(newItem.getValue().getCategory());
		Collections.sort(children);

		boolean hasTreeChildren = !newItem.getChildren().isEmpty();

		// check to see if the node's children have been previously shown
		if (!hasTreeChildren) {
			newItem.getChildren().clear();
			for (Material child : children) {
				MaterialNode materialNode = new MaterialNode(child);
				TreeItem<MaterialNode> materialItem = new TreeItem<>(materialNode);
				resetGraphic(materialItem);
				newItem.getChildren().add(materialItem);
			}
		}
		newItem.setExpanded(true);
		tvMaterials.refresh();
	}

	// images for controls
	@Override
	protected void setImages() {
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

	// show the Material attributes
	private void displayAttributes(Material material) {
		if (material == null) {
			return;
		}

		// name
		this.tfName.setText(material.getName());

		// description
		this.taDescription.setText(material.getDescription());

		// category
		this.cbCategories.setValue(material.getCategory());
	}

	// the single root for all material categories (not persistent)
	private TreeItem<MaterialNode> getRootMaterialItem() {
		if (tvMaterials.getRoot() == null) {
			Material rootMaterial = new Material();
			rootMaterial.setName(Material.ROOT_MATERIAL_NAME);
			tvMaterials.setRoot(new TreeItem<>(new MaterialNode(rootMaterial)));
		}
		return tvMaterials.getRoot();
	}

	// new material
	@FXML
	private void onNewMaterial() {
		try {
			// main attributes
			this.tfName.clear();
			this.tfName.requestFocus();
			this.taDescription.clear();
			this.cbCategories.getSelectionModel().clearSelection();

			// no current Material
			this.selectedMaterialItem = null;
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// set the Material attributes from the UI
	private boolean setAttributes(TreeItem<MaterialNode> materialItem) throws Exception {
		boolean isDirty = false;

		if (materialItem == null || !materialItem.getValue().isMaterial()) {
			return isDirty;
		}
		Material material = materialItem.getValue().getMaterial();

		// name
		String name = this.tfName.getText().trim();
		if (name.length() == 0) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.name"));
		}

		if (!name.equals(material.getName())) {
			material.setName(name);
			isDirty = true;
		}

		// description
		String description = this.taDescription.getText();

		if (description != null && !description.equals(material.getDescription())) {
			material.setDescription(description);
			isDirty = true;
		}

		// category
		String category = this.cbCategories.getSelectionModel().getSelectedItem();
		if (category == null) {
			category = DesignerLocalizer.instance().getLangString("uncategorized");
		}

		if (!category.equals(material.getCategory())) {
			material.setCategory(category);
			isDirty = true;
		}

		if (isDirty) {
			materialItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
			addEditedMaterial(materialItem);
		}
		return isDirty;
	}

	private void addEditedMaterial(TreeItem<MaterialNode> materialItem) {
		if (!editedMaterialItems.contains(materialItem)) {
			editedMaterialItems.add(materialItem);
		}
	}

	// populate the tree view categories
	private void populateCategories() throws Exception {
		// fetch the categories
		List<String> categories = PersistenceService.instance().fetchMaterialCategories();
		Collections.sort(categories);

		getRootMaterialItem().getChildren().clear();

		for (String category : categories) {
			MaterialNode categoryNode = new MaterialNode(category);
			TreeItem<MaterialNode> categoryItem = new TreeItem<>(categoryNode);
			categoryItem.setGraphic(ImageManager.instance().getImageView(Images.CATEGORY));
			getRootMaterialItem().getChildren().add(categoryItem);
		}

		// refresh tree view
		tvMaterials.setRoot(getRootMaterialItem());

		// also in the drop down
		cbCategories.getItems().clear();
		cbCategories.getItems().addAll(categories);
		tvMaterials.refresh();
	}

	// create material
	private void createMaterial() {
		try {
			// new child
			Material newMaterial = new Material();
			selectedMaterialItem = new TreeItem<>(new MaterialNode(newMaterial));
			addEditedMaterial(selectedMaterialItem);
			resetGraphic(selectedMaterialItem);

			// set attributes from UI
			setAttributes(selectedMaterialItem);

			// category
			String category = newMaterial.getCategory();

			ObservableList<TreeItem<MaterialNode>> categoryItems = getRootMaterialItem().getChildren();
			TreeItem<MaterialNode> parentCategoryItem = null;

			for (TreeItem<MaterialNode> categoryItem : categoryItems) {
				if (categoryItem.getValue().getCategory().equals(category)) {
					parentCategoryItem = categoryItem;
					break;
				}
			}

			if (parentCategoryItem == null) {
				// new category
				parentCategoryItem = new TreeItem<>(new MaterialNode(category));
				parentCategoryItem.setGraphic(ImageManager.instance().getImageView(Images.CATEGORY));
				getRootMaterialItem().getChildren().add(parentCategoryItem);
			}

			parentCategoryItem.getChildren().add(selectedMaterialItem);
			getRootMaterialItem().setExpanded(true);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void resetGraphic(TreeItem<MaterialNode> materialItem) {
		materialItem.setGraphic(ImageManager.instance().getImageView(Images.MATERIAL));
	}

	@FXML
	private void onSaveMaterial() {
		try {
			if (selectedMaterialItem == null) {
				// create
				createMaterial();
			} else {
				// update
				setAttributes(selectedMaterialItem);
			}

			// save the material
			Material material = getSelectedMaterial();
			Material saved = (Material) PersistenceService.instance().save(material);

			selectedMaterialItem.getValue().setMaterial(saved);
			resetGraphic(selectedMaterialItem);
			editedMaterialItems.remove(selectedMaterialItem);

			// update category just for the material being saved
			updateCategory(selectedMaterialItem);

			tvMaterials.refresh();

			tvMaterials.getSelectionModel().clearSelection();
			selectedMaterialItem = null;

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void updateCategory(TreeItem<MaterialNode> materialItem) {

		Material material = materialItem.getValue().getMaterial();

		String parentCategory = materialItem.getParent().getValue().getCategory();

		if (!material.getCategory().equals(parentCategory)) {
			// remove from category
			TreeItem<MaterialNode> categoryNode = materialItem.getParent();
			categoryNode.getChildren().remove(materialItem);

			if (categoryNode.getChildren().isEmpty()) {
				// remove category too
				categoryNode.getParent().getChildren().remove(categoryNode);
			}

			// add to new category
			TreeItem<MaterialNode> newItem = new TreeItem<>(new MaterialNode(material));
			resetGraphic(newItem);
			ObservableList<TreeItem<MaterialNode>> categoryItems = tvMaterials.getRoot().getChildren();

			TreeItem<MaterialNode> parentItem = null;
			for (TreeItem<MaterialNode> categoryItem : categoryItems) {
				if (categoryItem.getValue().getCategory().equals(material.getCategory())) {
					// existing category
					parentItem = categoryItem;
					break;
				}
			}

			if (parentItem == null) {
				// new category
				parentItem = new TreeItem<>(new MaterialNode(material.getCategory()));
				parentItem.setGraphic(ImageManager.instance().getImageView(Images.CATEGORY));
				tvMaterials.getRoot().getChildren().add(parentItem);
			}
			parentItem.getChildren().add(newItem);
		}
	}

	@FXML
	private void onSaveAllMaterial() {
		try {
			// current material could have been edited
			setAttributes(selectedMaterialItem);

			// save all modified materials
			for (TreeItem<MaterialNode> editedMaterialItem : editedMaterialItems) {
				MaterialNode node = editedMaterialItem.getValue();
				Material saved = (Material) PersistenceService.instance().save(node.getMaterial());
				node.setMaterial(saved);
				resetGraphic(editedMaterialItem);
			}
			editedMaterialItems.clear();

			// update category list
			populateCategories();
			tvMaterials.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// delete material
	@FXML
	private void onDeleteMaterial() throws Exception {
		if (selectedMaterialItem == null || !selectedMaterialItem.getValue().isMaterial()) {
			AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("material.deletion"));
			return;
		}

		// confirm
		ButtonType type = AppUtils.showConfirmationDialog(
				DesignerLocalizer.instance().getLangString("delete.material", getSelectedMaterial().getName()));

		if (type.equals(ButtonType.CANCEL)) {
			return;
		}

		try {
			// delete
			Material toDelete = getSelectedMaterial();
			PersistenceService.instance().delete(toDelete);

			// remove this material from the tree
			TreeItem<MaterialNode> childNode = tvMaterials.getSelectionModel().getSelectedItem();
			TreeItem<MaterialNode> parentNode = childNode.getParent();

			parentNode.getChildren().remove(childNode);
			tvMaterials.refresh();
			parentNode.setExpanded(true);

			// update category list
			populateCategories();

			// clear editor
			onNewMaterial();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// refresh the selected material
	@FXML
	private void onRefreshAllMaterial() {
		try {
			// clear any edits
			editedMaterialItems.clear();

			// update category list
			populateCategories();

			// clear editor
			onNewMaterial();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRefreshMaterial() {
		try {
			if (getSelectedMaterial() == null) {
				return;
			}

			if (getSelectedMaterial().getKey() != null) {
				// read from database
				Material material = PersistenceService.instance().fetchMaterialByKey(getSelectedMaterial().getKey());
				selectedMaterialItem.getValue().setMaterial(material);
				resetGraphic(selectedMaterialItem);
				displayAttributes(material);
			} else {
				// remove from tree
				selectedMaterialItem.getParent().getChildren().remove(selectedMaterialItem);
			}
			tvMaterials.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	@FXML
	protected void onCancel() {
		// close dialog with current material set to null
		this.selectedMaterialItem = null;
		super.onCancel();
	}

	@FXML
	private void onImportMaterial() throws Exception {
		// show file chooser
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(null);

		if (selectedFile == null) {
			return;
		}

		// read each line
		BufferedReader br = new BufferedReader(new FileReader(selectedFile));
		String line = null;

		try {
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");

				if (values.length > 0 && values[0] == null || values[0].trim().length() == 0) {
					throw new Exception(DesignerLocalizer.instance().getErrorString("no.material.id"));
				}

				// name
				String name = values[0].trim();

				// description
				String description = null;
				if (values.length > 1 && values[1] != null && values[1].trim().length() > 0) {
					description = values[1].trim();
				}

				// category
				String category = DesignerLocalizer.instance().getLangString("uncategorized");

				if (values.length > 2 && values[2] != null && values[2].trim().length() > 0) {
					category = values[2].trim();
				}

				Material material = null;
				try {
					material = PersistenceService.instance().fetchMaterialByName(name);

					// update
					material.setName(name);
					material.setDescription(description);
					material.setCategory(category);
				} catch (Exception e) {
					// new material
					material = new Material(name, description);
					material.setCategory(category);
				}

				PersistenceService.instance().save(material);
			}
		} finally {
			br.close();
		}

		populateCategories();
	}
}
