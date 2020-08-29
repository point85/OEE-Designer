package org.point85.app.operator;

import java.util.Collections;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.MaterialNode;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Material;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class MaterialSelectorController extends DialogController {
	// materials
	@FXML
	private TreeView<MaterialNode> tvMaterials;

	// selected material
	private Material selectedMaterial;

	// initialize
	public void initialize() throws Exception {
		// images for buttons
		setImages();

		// add the tree view listener for reason selection
		tvMaterials.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectMaterial(oldValue, newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});
		tvMaterials.setShowRoot(false);

		// fill in the top-level reason nodes
		populateCategories();
	}

	// material selected in the hierarchy
	private void onSelectMaterial(TreeItem<MaterialNode> oldItem, TreeItem<MaterialNode> newItem) throws Exception {
		if (newItem == null) {
			return;
		}

		// leaf node is material
		if (newItem.getValue().isMaterial()) {
			selectedMaterial = newItem.getValue().getMaterial();
			return;
		} else {
			// category selected
			selectedMaterial = null;
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
				newItem.getChildren().add(materialItem);
			}
		}
		newItem.setExpanded(true);
		tvMaterials.refresh();
	}

	// the single root for all material categories
	private TreeItem<MaterialNode> getRootMaterialItem() {
		if (tvMaterials.getRoot() == null) {
			Material rootMaterial = new Material();
			rootMaterial.setName(Material.ROOT_MATERIAL_NAME);
			tvMaterials.setRoot(new TreeItem<>(new MaterialNode(rootMaterial)));
		}
		return tvMaterials.getRoot();
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
		tvMaterials.refresh();
	}

	public Material getSelectedMaterial() {
		return selectedMaterial;
	}
}
