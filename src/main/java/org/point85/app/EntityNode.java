package org.point85.app;

import java.util.Objects;

import org.point85.domain.plant.PlantEntity;

// the wrapped PlantEntity
public class EntityNode {
	private PlantEntity entity;

	public EntityNode(PlantEntity entity) {
		setPlantEntity(entity);
	}

	public PlantEntity getPlantEntity() {
		return entity;
	}

	public void setPlantEntity(PlantEntity entity) {
		this.entity = entity;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entity.getName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EntityNode) {
			EntityNode other = (EntityNode) obj;
			if (entity.getName().equals(other.entity.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return entity.getName() + " (" + entity.getDescription() + ")";
	}
}
