package com.hiveworkshop.rms.ui.gui.modeledit.newstuff.actions.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hiveworkshop.rms.ui.gui.modeledit.UndoAction;
import com.hiveworkshop.rms.editor.model.Bone;
import com.hiveworkshop.rms.editor.model.GeosetVertex;
import com.hiveworkshop.rms.util.Vec3;

public class RigAction implements UndoAction {
	private final List<Vec3> selectedVertices;
	private final List<Bone> selectedBones;
	private final Map<Vec3, List<Bone>> vertexToPriorBoneAttachment;
	private final Map<GeosetVertex, Bone[]> vertexToOldSkinBoneReferences;
	private final Map<GeosetVertex, short[]> vertexToOldSkinBoneWeightReferences;

	public RigAction(final Collection<? extends Vec3> selectedVertices,
			final Collection<? extends Bone> selectedBones) {
		this.selectedVertices = new ArrayList<>(selectedVertices);
		this.selectedBones = new ArrayList<>(selectedBones);
		this.vertexToPriorBoneAttachment = new HashMap<>();
		this.vertexToOldSkinBoneReferences = new HashMap<>();
		this.vertexToOldSkinBoneWeightReferences = new HashMap<>();
		loadUndoData();
	}

	public RigAction(final RigAction... rigActions) {
		this.selectedVertices = new ArrayList<>();
		this.selectedBones = new ArrayList<>();
		this.vertexToPriorBoneAttachment = new HashMap<>();
		this.vertexToOldSkinBoneReferences = new HashMap<>();
		this.vertexToOldSkinBoneWeightReferences = new HashMap<>();
		for (final RigAction other : rigActions) {
			selectedVertices.addAll(other.selectedVertices);
			selectedBones.addAll(other.selectedBones);
		}
		loadUndoData();
	}

	private void loadUndoData() {
		for (final Vec3 vertex : selectedVertices) {
			if (vertex instanceof GeosetVertex) {
				GeosetVertex geosetVertex = (GeosetVertex) vertex;
				if(geosetVertex.getSkinBones() != null) {
					vertexToOldSkinBoneReferences.put(geosetVertex, geosetVertex.getSkinBones().clone());
					vertexToOldSkinBoneWeightReferences.put(geosetVertex, geosetVertex.getSkinBoneWeights().clone());
				} else {
					final List<Bone> boneAttachments = geosetVertex.getBoneAttachments();
					vertexToPriorBoneAttachment.put(vertex, new ArrayList<>(boneAttachments));
				}
			}
		}
	}

	@Override
	public void undo() {
		for (final Vec3 vertex : selectedVertices) {
			if (vertex instanceof GeosetVertex) {
				final List<Bone> list = vertexToPriorBoneAttachment.get(vertex);
				if (list != null) {
					((GeosetVertex) vertex).rigBones(new ArrayList<>(list));
				} else {
					Bone[] bones = vertexToOldSkinBoneReferences.get(vertex);
					short[] boneWeights = vertexToOldSkinBoneWeightReferences.get(vertex);
					((GeosetVertex) vertex).setSkinBones(bones);
					((GeosetVertex) vertex).setSkinBoneWeights(boneWeights);
				}
			}
		}
	}

	@Override
	public void redo() {
		for (final Vec3 vertex : selectedVertices) {
			if (vertex instanceof GeosetVertex) {
				((GeosetVertex) vertex).rigBones(new ArrayList<>(selectedBones));
			}
		}
	}

	@Override
	public String actionName() {
		return "rig";
	}

}