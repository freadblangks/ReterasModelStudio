package com.hiveworkshop.rms.ui.application.actionfunctions;

import com.hiveworkshop.rms.editor.model.*;
import com.hiveworkshop.rms.editor.model.util.ModelUtils;
import com.hiveworkshop.rms.ui.application.ProgramGlobals;
import com.hiveworkshop.rms.ui.language.TextKey;
import com.hiveworkshop.rms.util.Vec2;
import com.hiveworkshop.rms.util.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MakeModelSD extends ActionFunction {
	public MakeModelSD(){
		super(TextKey.HD_TO_SD, () -> convertToV800(1));
	}


	/**
	 * Please, for the love of Pete, don't actually do this.
	 */
	public static void convertToV800(int targetLevelOfDetail) {
		EditableModel model = ProgramGlobals.getCurrentModelPanel().getModel();
		// Things to fix:
		// 1.) format version
		model.setFormatVersion(800);
		// 2.) materials: only diffuse
		for (Bitmap tex : model.getTextures()) {
			String path = tex.getPath();
			if ((path != null) && !path.isEmpty()) {
				int dotIndex = path.lastIndexOf('.');
				if ((dotIndex != -1) && !path.endsWith(".blp")) {
					path = (path.substring(0, dotIndex));
				}
				if (!path.endsWith(".blp")) {
					path += ".blp";
				}
				tex.setPath(path);
			}
		}
		for (Material material : model.getMaterials()) {
			material.makeSD();
		}
		// 3.) geosets:
		// - Convert skin to matrices & vertex groups
		List<Geoset> wrongLOD = new ArrayList<>();
		for (Geoset geo : model.getGeosets()) {
			for (GeosetVertex vertex : geo.getVertices()) {
				vertex.un900Heuristic();
			}
			if (geo.getLevelOfDetail() != targetLevelOfDetail) {
				// wrong lod
				wrongLOD.add(geo);
			}
		}
		// - Probably overwrite normals with tangents, maybe, or maybe not
		// - Eradicate anything that isn't LOD==X
		if (model.getGeosets().size() > wrongLOD.size()) {
			for (Geoset wrongLODGeo : wrongLOD) {
				model.remove(wrongLODGeo);
				GeosetAnim geosetAnim = wrongLODGeo.getGeosetAnim();
				if (geosetAnim != null) {
					model.remove(geosetAnim);
				}
			}
		}
		// 4.) remove popcorn
		// - add hero glow from popcorn if necessary
		List<IdObject> incompatibleObjects = new ArrayList<>();
		for (int idObjIdx = 0; idObjIdx < model.getIdObjectsSize(); idObjIdx++) {
			IdObject idObject = model.getIdObject(idObjIdx);
			if (idObject instanceof ParticleEmitterPopcorn) {
				incompatibleObjects.add(idObject);
				if (((ParticleEmitterPopcorn) idObject).getPath().toLowerCase().contains("hero_glow")) {
					System.out.println("HERO HERO HERO");
					Bone dummyHeroGlowNode = new Bone("hero_reforged");
					// this model needs hero glow
					ModelUtils.Mesh heroGlowPlane = ModelUtils.createPlane((byte) 0, (byte) 1, new Vec3(0, 0, 1), 0, new Vec2(-64, -64), new Vec2(64, 64), 1, 1);

					Geoset heroGlow = new Geoset();
					heroGlow.getVertices().addAll(heroGlowPlane.getVertices());
					for (GeosetVertex gv : heroGlow.getVertices()) {
						gv.setGeoset(heroGlow);
						gv.clearBoneAttachments();
						gv.addBoneAttachment(dummyHeroGlowNode);
					}
					heroGlow.getTriangles().addAll(heroGlowPlane.getTriangles());
					heroGlow.setUnselectable(true);

					Bitmap heroGlowBitmap = new Bitmap("");
					heroGlowBitmap.setReplaceableId(2);
					Layer layer = new Layer("Additive", heroGlowBitmap);
					layer.setUnshaded(true);
					layer.setUnfogged(true);
					heroGlow.setMaterial(new Material(layer));

					model.add(dummyHeroGlowNode);
					model.add(heroGlow);

				}
			}
		}
		for (IdObject incompat : incompatibleObjects) {
			model.remove(incompat);
		}
		// 5.) remove other unsupported stuff
		for (IdObject obj : model.getIdObjects()) {
			obj.setBindPose(null);
		}
		for (Camera camera : model.getCameras()) {
			camera.setBindPose(null);
		}
		// 6.) fix dump bug with paths:
		for (Bitmap tex : model.getTextures()) {
			String path = tex.getPath();
			if (path != null) {
				tex.setPath(path.replace('/', '\\'));
			}
		}
		for (ParticleEmitter emitter : model.getParticleEmitters()) {
			String path = emitter.getPath();
			if (path != null) {
				emitter.setPath(path.replace('/', '\\'));
			}
		}
		for (Attachment emitter : model.getAttachments()) {
			String path = emitter.getPath();
			if (path != null) {
				emitter.setPath(path.replace('/', '\\'));
			}
		}

		model.setBindPoseChunk(null);
		model.getFaceEffects().clear();
	}
}