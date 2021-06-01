package com.hiveworkshop.rms.editor.model.util.ModelFactory;

import com.hiveworkshop.rms.editor.model.*;
import com.hiveworkshop.rms.editor.model.util.ModelUtils;
import com.hiveworkshop.rms.parsers.mdlx.*;
import com.hiveworkshop.rms.util.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IdObjectFactory {
	public static Bone createBone(MdlxBone mdlxBone) {
		Bone bone = new Bone();
		if ((mdlxBone.flags & 256) != 256) {
			System.err.println("MDX -> MDL error: A bone '" + mdlxBone.name + "' not flagged as bone in MDX!");
		}

		loadObject(bone, mdlxBone);

		bone.setGeosetId(mdlxBone.geosetId);
		bone.setGeosetAnimId(mdlxBone.geosetAnimationId);
		return bone;
	}


	public static void loadObject(IdObject idObject, final MdlxGenericObject object) {
		idObject.setName(object.name);
		idObject.setObjectId(object.objectId);
		idObject.setParentId(object.parentId);

		idObject.setDontInheritTranslation((object.flags & 0x1) != 0);
		idObject.setDontInheritRotation((object.flags & 0x2) != 0);
		idObject.setDontInheritScaling((object.flags & 0x4) != 0);
		idObject.setBillboarded((object.flags & 0x8) != 0);
		idObject.setBillboardLockX((object.flags & 0x10) != 0);
		idObject.setBillboardLockY((object.flags & 0x20) != 0);
		idObject.setBillboardLockZ((object.flags & 0x40) != 0);

		idObject.loadTimelines(object);
	}

	public static Light createLight(MdlxLight mdlxLight) {
		Light light = new Light();
		if ((mdlxLight.flags & 512) != 512) {
			System.err.println("MDX -> MDL error: A light '" + mdlxLight.name + "' not flagged as light in MDX!");
		}

		loadObject(light, mdlxLight);

		light.setType(mdlxLight.type);
		light.setAttenuationStart(mdlxLight.attenuation[0]);
		light.setAttenuationEnd(mdlxLight.attenuation[1]);
		light.setStaticColor(new Vec3(mdlxLight.color, true));
		light.setIntensity(mdlxLight.intensity);
		light.setStaticAmbColor(new Vec3(mdlxLight.ambientColor, true));
		light.setAmbIntensity(mdlxLight.ambientIntensity);
		return light;
	}

	public static Helper createHelper(MdlxHelper mdlxHelper) {
		Helper helper = new Helper();
		if ((mdlxHelper.flags & 1) != 0) {
			System.err.println("MDX -> MDL error: A helper '" + mdlxHelper.name + "' not flagged as helper in MDX!");
		}

		loadObject(helper, mdlxHelper);
		return helper;
	}

	public static Attachment createAttachment(MdlxAttachment mdlxAttachment) {
		Attachment attachment = new Attachment();
		if ((mdlxAttachment.flags & 2048) != 2048) {
			System.err.println("MDX -> MDL error: A light '" + mdlxAttachment.name + "' not flagged as light in MDX!");
		}

		loadObject(attachment, mdlxAttachment);
		List<String> ugg = new ArrayList<>(attachment.animFlags.keySet());
		System.out.println("attatchment timelines: " + (Arrays.toString(ugg.toArray(new String[0]))));

		attachment.setAttachmentID(mdlxAttachment.attachmentId);
		attachment.setPath(mdlxAttachment.path);
		return attachment;
	}

	public static ParticleEmitter createParticleEmitter(MdlxParticleEmitter mdlxEmitter) {
		ParticleEmitter particleEmitter = new ParticleEmitter();
		if ((mdlxEmitter.flags & 4096) != 4096) {
			System.err.println("MDX -> MDL error: A particle emitter '" + mdlxEmitter.name
					+ "' not flagged as particle emitter in MDX!");
		}

		loadObject(particleEmitter, mdlxEmitter);

		particleEmitter.setEmissionRate(mdlxEmitter.emissionRate);
		particleEmitter.setGravity(mdlxEmitter.gravity);
		particleEmitter.setInitVelocity(mdlxEmitter.speed);
		particleEmitter.setLatitude(mdlxEmitter.latitude);
		particleEmitter.setLifeSpan(mdlxEmitter.lifeSpan);
		particleEmitter.setLongitude(mdlxEmitter.longitude);
		particleEmitter.setPath(mdlxEmitter.path);

		particleEmitter.setMDLEmitter(((mdlxEmitter.flags >> 15) & 1) == 1);
		if (!particleEmitter.isMDLEmitter() && (((mdlxEmitter.flags >> 8) & 1) == 1)) {
			System.err.println(
					"WARNING in MDX -> MDL: ParticleEmitter of unknown type! Defaults to EmitterUsesTGA in my MDL code!");
		}
		return particleEmitter;
	}

	public static ParticleEmitter2 createParticleEmitter2(MdlxParticleEmitter2 mdlxEmitter) {
		ParticleEmitter2 particleEmitter2 = new ParticleEmitter2();
		if ((mdlxEmitter.flags & 4096) != 4096) {
			System.err.println("MDX -> MDL error: A particle emitter '" + mdlxEmitter.name
					+ "' not flagged as particle emitter in MDX!");
		}

		loadObject(particleEmitter2, mdlxEmitter);

		particleEmitter2.setUnshaded((mdlxEmitter.flags & 0x8000) != 0);
		particleEmitter2.setSortPrimsFarZ((mdlxEmitter.flags & 0x10000) != 0);
		particleEmitter2.setLineEmitter((mdlxEmitter.flags & 0x20000) != 0);
		particleEmitter2.setUnfogged((mdlxEmitter.flags & 0x40000) != 0);
		particleEmitter2.setModelSpace((mdlxEmitter.flags & 0x80000) != 0);
		particleEmitter2.setXYQuad((mdlxEmitter.flags & 0x100000) != 0);

		particleEmitter2.setSpeed(mdlxEmitter.speed);
		particleEmitter2.setVariation(mdlxEmitter.variation);
		particleEmitter2.setLatitude(mdlxEmitter.latitude);
		particleEmitter2.setGravity(mdlxEmitter.gravity);
		particleEmitter2.setLifeSpan(mdlxEmitter.lifeSpan);
		particleEmitter2.setEmissionRate(mdlxEmitter.emissionRate);
		particleEmitter2.setLength(mdlxEmitter.length);
		particleEmitter2.setWidth(mdlxEmitter.width);
		particleEmitter2.setFilterMode(mdlxEmitter.filterMode);

		particleEmitter2.setRows((int) mdlxEmitter.rows);
		particleEmitter2.setColumns((int) mdlxEmitter.columns);

		particleEmitter2.setHeadOrTail(mdlxEmitter.headOrTail);

		particleEmitter2.setTailLength(mdlxEmitter.tailLength);
		particleEmitter2.setTime(mdlxEmitter.timeMiddle);


		float[][] colors = mdlxEmitter.segmentColors;
		// SegmentColor - Inverse order for MDL!
		for (int i = 0; i < 3; i++) {
			particleEmitter2.setSegmentColor(i, new Vec3(colors[i]));
//			setSegmentColor(i, new Vec3(ModelUtils.flipRGBtoBGR(colors[i])));
		}

		short[] alphas = mdlxEmitter.segmentAlphas;
		particleEmitter2.setAlpha(new Vec3(alphas[0], alphas[1], alphas[2]));
		particleEmitter2.setParticleScaling(new Vec3(mdlxEmitter.segmentScaling));

		long[][] head = mdlxEmitter.headIntervals;
		long[][] tail = mdlxEmitter.tailIntervals;

		particleEmitter2.setHeadUVAnim(new Vec3(head[0][0], head[0][1], head[0][2]));
		particleEmitter2.setHeadDecayUVAnim(new Vec3(head[1][0], head[1][1], head[1][2]));
		particleEmitter2.setTailUVAnim(new Vec3(tail[0][0], tail[0][1], tail[0][2]));
		particleEmitter2.setTailDecayUVAnim(new Vec3(tail[1][0], tail[1][1], tail[1][2]));

		particleEmitter2.setTextureID(mdlxEmitter.textureId);

		particleEmitter2.setSquirt(mdlxEmitter.squirt == 1);

		particleEmitter2.setPriorityPlane(mdlxEmitter.priorityPlane);
		particleEmitter2.setReplaceableId((int) mdlxEmitter.replaceableId);
		return particleEmitter2;
	}

	public static ParticleEmitterPopcorn createParticleEmitterPopcorn(MdlxParticleEmitterPopcorn mdlxEmitter) {
		ParticleEmitterPopcorn particleEmitterPopcorn = new ParticleEmitterPopcorn();
		loadObject(particleEmitterPopcorn, mdlxEmitter);

		particleEmitterPopcorn.setLifeSpan(mdlxEmitter.lifeSpan);
		particleEmitterPopcorn.setEmissionRate(mdlxEmitter.emissionRate);
		particleEmitterPopcorn.setInitVelocity(mdlxEmitter.speed);
		System.out.println("emitter color: " + Arrays.toString(mdlxEmitter.color));
		particleEmitterPopcorn.setColor(new Vec3(mdlxEmitter.color));
//		color = new Vec3(ModelUtils.flipRGBtoBGR(emitter.color));
		particleEmitterPopcorn.setAlpha(mdlxEmitter.alpha);
		particleEmitterPopcorn.setReplaceableId(mdlxEmitter.replaceableId);
		particleEmitterPopcorn.setPath(mdlxEmitter.path);
		particleEmitterPopcorn.setAnimVisibilityGuide(mdlxEmitter.animationVisiblityGuide);
		System.out.println(mdlxEmitter.animationVisiblityGuide);
		return particleEmitterPopcorn;
	}

	public static RibbonEmitter createRibbonEmitter(MdlxRibbonEmitter mdlxEmitter) {
		RibbonEmitter ribbonEmitter = new RibbonEmitter();
		if ((mdlxEmitter.flags & 16384) != 16384) {
			System.err.println("MDX -> MDL error: A ribbon emitter '" + mdlxEmitter.name
					+ "' not flagged as ribbon emitter in MDX!");
		}

		loadObject(ribbonEmitter, mdlxEmitter);

		ribbonEmitter.setTextureSlot(mdlxEmitter.textureSlot);
		ribbonEmitter.setHeightAbove(mdlxEmitter.heightAbove);
		ribbonEmitter.setHeightBelow(mdlxEmitter.heightBelow);
		ribbonEmitter.setAlpha(mdlxEmitter.alpha);
		ribbonEmitter.setStaticColor(new Vec3(ModelUtils.flipRGBtoBGR(mdlxEmitter.color)));
		ribbonEmitter.setLifeSpan(mdlxEmitter.lifeSpan);
		ribbonEmitter.setEmissionRate((int) mdlxEmitter.emissionRate);
		ribbonEmitter.setRows((int) mdlxEmitter.rows);
		ribbonEmitter.setColumns((int) mdlxEmitter.columns);
		ribbonEmitter.setMaterialId(mdlxEmitter.materialId);
		ribbonEmitter.setGravity(mdlxEmitter.gravity);
		return ribbonEmitter;
	}

	public static EventObject createEventObject(MdlxEventObject mdlxObject) {
		EventObject eventObject = new EventObject();
		if ((mdlxObject.flags & 1024) != 1024) {
			System.err.println("MDX -> MDL error: An eventobject '" + mdlxObject.name
					+ "' not flagged as eventobject in MDX!");
		}

		loadObject(eventObject, mdlxObject);

		final int globalSequenceId = mdlxObject.globalSequenceId;

		if (globalSequenceId >= 0) {
			eventObject.setGlobalSeqId(globalSequenceId);
			eventObject.setHasGlobalSeq(true);
		}

		for (final long val : mdlxObject.keyFrames) {
			eventObject.addTrack((int) val);
		}
		return eventObject;
	}

	public static CollisionShape createCollisionShape(MdlxCollisionShape mdlxShape) {
		CollisionShape collisionShape = new CollisionShape();
		if ((mdlxShape.flags & 8192) != 8192) {
			System.err.println("MDX -> MDL error: A collisionshape '" + mdlxShape.name
					+ "' not flagged as collisionshape in MDX!");
		}

		loadObject(collisionShape, mdlxShape);

		collisionShape.setType(mdlxShape.type);

		float[][] vertices = mdlxShape.vertices;

		collisionShape.addVertex(new Vec3(vertices[0]));

		if (collisionShape.getType() != MdlxCollisionShape.Type.SPHERE) {
			collisionShape.addVertex(new Vec3(vertices[1]));
		}

		if (collisionShape.getType() == MdlxCollisionShape.Type.SPHERE || collisionShape.getType() == MdlxCollisionShape.Type.CYLINDER) {
			collisionShape.setExtents(new ExtLog(mdlxShape.boundsRadius));
		}
		return collisionShape;
	}
}