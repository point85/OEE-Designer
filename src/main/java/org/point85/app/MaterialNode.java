package org.point85.app;

import org.point85.domain.plant.Material;

// class for holding attributes of Material in a tree view leaf node
public class MaterialNode {
	// material
	private Material material;

	// category name
	private String category;

	public MaterialNode(String category) {
		this.category = category;
	}

	public MaterialNode(Material material) {
		setMaterial(material);
	}

	public String getCategory() {
		return category;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;

		// category could have changed
		this.category = material.getCategory();
	}

	public boolean isMaterial() {
		return material != null;
	}

	@Override
	public String toString() {
		if (material != null) {
			String description = material.getDescription();
			String value = material.getName();
			if (description != null) {
				value += " (" + material.getDescription() + ")";
			}
			return value;
		} else {
			return category;
		}
	}
}