package com.hiveworkshop.rms.ui.gui.modeledit.selection;

import com.hiveworkshop.rms.editor.model.Bone;
import com.hiveworkshop.rms.editor.model.Camera;
import com.hiveworkshop.rms.editor.model.CollisionShape;
import com.hiveworkshop.rms.editor.model.IdObject;
import com.hiveworkshop.rms.editor.wrapper.v2.ModelView;
import com.hiveworkshop.rms.ui.application.ProgramGlobals;
import com.hiveworkshop.rms.ui.application.edit.mesh.AbstractModelEditor;
import com.hiveworkshop.rms.ui.application.edit.mesh.viewport.axes.CoordinateSystem;
import com.hiveworkshop.rms.ui.application.edit.uv.types.TVertexModelElementRenderer;
import com.hiveworkshop.rms.util.Vec2;
import com.hiveworkshop.rms.util.Vec3;

import java.util.*;

public final class PivotPointSelectionManager extends SelectionManager<Vec3> {
	private final Bone renderBoneDummy = new Bone();

	public PivotPointSelectionManager(ModelView modelView, SelectionItemTypes selectionMode) {
		super(modelView, selectionMode);
	}

	@Override
	public Set<Vec3> getSelection() {
		Set<Vec3> ugg = getVec3Set();
		return ugg;
	}

	@Override
	public void setSelection(final Collection<? extends Vec3> selectionItem) {
		Set<Vec3> ugg = new HashSet<>();
//		modelView.setSelectedIdObjects(ugg);
		fireChangeListeners();
	}

	private Set<Vec3> getVec3Set() {
		Set<Vec3> ugg = new HashSet<>();
		modelView.getSelectedIdObjects().stream().forEach(idObject -> ugg.add(idObject.getPivotPoint()));
		return ugg;
	}

	@Override
	public boolean isEmpty() {
		return modelView.getSelectedIdObjects().isEmpty();
	}

	@Override
	public void addSelection(final Collection<? extends Vec3> selectionItem) {
		fireChangeListeners();
	}

	@Override
	public void removeSelection(final Collection<? extends Vec3> selectionItem) {
		fireChangeListeners();
	}

	@Override
	public List<Vec3> genericSelect(Vec2 min, Vec2 max, CoordinateSystem coordinateSystem) {
		List<Vec3> selectedItems = new ArrayList<>();

		for (IdObject object : modelView.getEditableIdObjects()) {
			double vertexSize1 = object.getClickRadius(coordinateSystem) * coordinateSystem.getZoom() * 2;
			if (AbstractModelEditor.hitTest(min, max, object.getPivotPoint(), coordinateSystem, vertexSize1)) {
				System.out.println("selected " + object.getName());
				selectedItems.add(object.getPivotPoint());
			}

			if (object instanceof CollisionShape) {
				for (Vec3 vertex : ((CollisionShape) object).getVertices()) {
					int vertexSize = IdObject.DEFAULT_CLICK_RADIUS;
					if (AbstractModelEditor.hitTest(min, max, vertex, coordinateSystem, vertexSize)) {
						selectedItems.add(vertex);
					}
				}
			}
		}
		for (Camera camera : modelView.getEditableCameras()) {
			int vertexSize = ProgramGlobals.getPrefs().getVertexSize();
			if (AbstractModelEditor.hitTest(min, max, camera.getPosition(), coordinateSystem, vertexSize)) {
				selectedItems.add(camera.getPosition());
			}
			if (AbstractModelEditor.hitTest(min, max, camera.getTargetPosition(), coordinateSystem, vertexSize)) {
				selectedItems.add(camera.getTargetPosition());
			}
		}
		return selectedItems;
	}

//	@Override
//	public Set<Triangle> getSelectedFaces() {
//		return new HashSet<>();
//	}

//	@Override
//	public void renderSelection(ModelElementRenderer renderer, CoordinateSystem coordinateSystem, ModelView modelView) {
////		Set<Vec3> drawnSelection = new HashSet<>();
////		for (IdObject object : modelView.getSelectedIdObjects()) {
////			renderer.renderIdObject(object);
////			drawnSelection.add(object.getPivotPoint());
////		}
////		for (Camera camera : modelView.getEditableCameras()) {
////			Color targetColor = selection.contains(camera.getTargetPosition()) ? Color.GREEN.darker() : Color.ORANGE.darker();
////			Color boxColor = selection.contains(camera.getPosition()) ? Color.GREEN.darker() : Color.ORANGE.darker();
////			renderer.renderCamera(camera, boxColor, camera.getPosition(), targetColor, camera.getTargetPosition());
////			drawnSelection.add(camera.getPosition());
////			drawnSelection.add(camera.getTargetPosition());
////		}
////		for (Vec3 vertex : selection) {
////			if (!drawnSelection.contains(vertex)) {
////				renderBoneDummy.setPivotPoint(vertex);
////				renderer.renderIdObject(renderBoneDummy);
////			}
////		}
//
////		Set<Vec3> drawnSelection = new HashSet<>();
////		for (IdObject object : modelView.getEditableIdObjects()) {
////			if (selection.contains(object.getPivotPoint())) {
////				renderer.renderIdObject(object);
////				drawnSelection.add(object.getPivotPoint());
////			}
////		}
////		for (Camera camera : modelView.getEditableCameras()) {
////			renderer.renderCamera(camera, selection.contains(camera.getPosition()) ? Color.GREEN.darker() : Color.ORANGE.darker(), camera.getPosition(), selection.contains(camera.getTargetPosition()) ? Color.GREEN.darker() : Color.ORANGE.darker(), camera.getTargetPosition());
////			drawnSelection.add(camera.getPosition());
////			drawnSelection.add(camera.getTargetPosition());
////		}
////		for (Vec3 vertex : selection) {
////			if (!drawnSelection.contains(vertex)) {
////				renderBoneDummy.setPivotPoint(vertex);
////				renderer.renderIdObject(renderBoneDummy);
////			}
////		}
//	}

	@Override
	public double getCircumscribedSphereRadius(Vec3 sphereCenter) {
		double radius = 0;
		for (IdObject item : modelView.getSelectedIdObjects()) {
			double distance = sphereCenter.distance(item.getPivotPoint());
			if (distance >= radius) {
				radius = distance;
			}
		}
		for (Camera item : modelView.getSelectedCameras()) {
			double distance = sphereCenter.distance(item.getPosition());
			if (distance >= radius) {
				radius = distance;
			}
		}
//		for (Vec3 item : selection) {
//			double distance = sphereCenter.distance(item);
//			if (distance >= radius) {
//				radius = distance;
//			}
//		}
		return radius;
	}

	@Override
	public double getCircumscribedSphereRadius(Vec2 center, int tvertexLayerId) {
		return 0;
	}

	@Override
	public Vec2 getUVCenter(int tvertexLayerId) {
		return Vec2.ORIGIN;
	}

	@Override
	public Collection<? extends Vec2> getSelectedTVertices(int tvertexLayerId) {
		return Collections.emptySet();
	}

	@Override
	public void renderUVSelection(TVertexModelElementRenderer renderer, ModelView modelView, int tvertexLayerId) {
	}
}
