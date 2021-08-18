package com.hiveworkshop.rms.editor.actions.animation;

import com.hiveworkshop.rms.editor.actions.UndoAction;
import com.hiveworkshop.rms.editor.actions.util.GenericRotateAction;
import com.hiveworkshop.rms.editor.model.AnimatedNode;
import com.hiveworkshop.rms.editor.model.Bone;
import com.hiveworkshop.rms.editor.model.Helper;
import com.hiveworkshop.rms.editor.model.IdObject;
import com.hiveworkshop.rms.editor.model.animflag.Entry;
import com.hiveworkshop.rms.editor.model.animflag.QuatAnimFlag;
import com.hiveworkshop.rms.editor.render3d.RenderModel;
import com.hiveworkshop.rms.editor.render3d.RenderNode;
import com.hiveworkshop.rms.parsers.mdlx.mdl.MdlUtils;
import com.hiveworkshop.rms.ui.application.edit.animation.TimeEnvironmentImpl;
import com.hiveworkshop.rms.ui.application.edit.mesh.viewport.axes.CoordSysUtils;
import com.hiveworkshop.rms.util.Mat4;
import com.hiveworkshop.rms.util.Quat;
import com.hiveworkshop.rms.util.Vec3;
import com.hiveworkshop.rms.util.Vec4;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class SquatToolKeyframeAction implements GenericRotateAction {
	private final UndoAction addingTimelinesOrKeyframesAction;
	private final int trackTime;
	private final HashMap<IdObject, Quat> nodeToLocalRotation;
	private final Vec3 center;
	private final byte dim1;
	private final byte dim2;
	private final Integer trackGlobalSeq;
	private final RenderModel editorRenderModel;
	private final List<IdObject> idObjects;

	public SquatToolKeyframeAction(UndoAction addingTimelinesOrKeyframesAction,
	                               Collection<IdObject> nodeSelection,
	                               RenderModel editorRenderModel,
	                               List<IdObject> idObjects,
	                               Vec3 center,
	                               byte dim1, byte dim2) {
		this.addingTimelinesOrKeyframesAction = addingTimelinesOrKeyframesAction;
		this.editorRenderModel = editorRenderModel;
		this.trackTime = editorRenderModel.getTimeEnvironment().getTrackTime();
		this.trackGlobalSeq = editorRenderModel.getTimeEnvironment().getGlobalSeq();
		this.idObjects = idObjects;
		this.dim1 = dim1;
		this.dim2 = dim2;
		nodeToLocalRotation = new HashMap<>();
		for (IdObject node : nodeSelection) {
			nodeToLocalRotation.put(node, new Quat());
		}
		this.center = new Vec3(center);
	}

	@Override
	public UndoAction undo() {
		for (IdObject node : nodeToLocalRotation.keySet()) {
			Quat localTranslation = nodeToLocalRotation.get(node);
			updateLocalRotationKeyframeInverse(node, trackTime, trackGlobalSeq, localTranslation);
		}
		addingTimelinesOrKeyframesAction.undo();
		return this;
	}

	@Override
	public UndoAction redo() {
		addingTimelinesOrKeyframesAction.redo();
		for (IdObject node : nodeToLocalRotation.keySet()) {
			Quat localTranslation = nodeToLocalRotation.get(node);
			updateLocalRotationKeyframe(node, trackTime, trackGlobalSeq, localTranslation);
		}
		return this;
	}

	@Override
	public String actionName() {
		return "edit rotation w/ squat";
	}

	@Override
	public GenericRotateAction updateRotation(double radians) {
//		modelEditor.rawSquatToolRotate2d(center, radians, dim1, dim2, nodeToLocalRotation);
		for (IdObject idObject : nodeToLocalRotation.keySet()) {
			updateRotationKeyframe(idObject, editorRenderModel, center, radians, dim1, dim2, nodeToLocalRotation.get(idObject));
		}
		//todo check if this should be here....
		for (IdObject idObject : idObjects) {
			if (nodeToLocalRotation.containsKey(idObject.getParent())
					&& (((idObject.getClass() == Bone.class) && (idObject.getParent().getClass() == Bone.class))
					|| ((idObject.getClass() == Helper.class) && (idObject.getParent().getClass() == Helper.class)))) {
				updateRotationKeyframe(idObject, editorRenderModel, center, -radians, dim1, dim2, nodeToLocalRotation.get(idObject));
			}
		}
		return this;
	}

	public void updateLocalRotationKeyframe(AnimatedNode animatedNode, int trackTime, Integer trackGlobalSeq, Quat localRotation) {
		// Note to future author: the reason for saved local rotation is that
		// we would like to be able to undo the action of rotating the animation data

		// TODO global seqs, needs separate check on AnimRendEnv, and also we must make AnimFlag.find seek on globalSeqId
		QuatAnimFlag rotationTimeline = (QuatAnimFlag) animatedNode.find(MdlUtils.TOKEN_ROTATION, trackGlobalSeq);
//		final AnimFlag rotationTimeline = find(MdlUtils.TOKEN_ROTATION, trackGlobalSeq);
		if (rotationTimeline == null) {
			return;
		}
		if (rotationTimeline.hasEntryAt(trackTime)) {
			Entry<Quat> entry = rotationTimeline.getEntryAt(trackTime);
			entry.getValue().mul(localRotation);
			if (rotationTimeline.tans()) {
				entry.getInTan().mul(localRotation);
				entry.getOutTan().mul(localRotation);
			}
		}
	}

	public void updateLocalRotationKeyframeInverse(AnimatedNode animatedNode, int trackTime, Integer trackGlobalSeq, Quat localRotation) {
		// Note to future author: the reason for saved local rotation is that
		// we would like to be able to undo the action of rotating the animation data

		// TODO global seqs, needs separate check on AnimRendEnv, and also we must make AnimFlag.find seek on globalSeqId
		QuatAnimFlag rotationTimeline = (QuatAnimFlag) animatedNode.find(MdlUtils.TOKEN_ROTATION, trackGlobalSeq);
		if (rotationTimeline == null) {
			return;
		}
		if (rotationTimeline.hasEntryAt(trackTime)) {
			Entry<Quat> entry = rotationTimeline.getEntryAt(trackTime);
			entry.getValue().mulLeft(localRotation);
			if (rotationTimeline.tans()) {
				entry.getInTan().mulLeft(localRotation);
				entry.getOutTan().mulLeft(localRotation);
			}
		}
	}

	public void updateRotationKeyframe(AnimatedNode animatedNode, RenderModel renderModel, Vec3 center,
	                                   double radians, byte firstXYZ, byte secondXYZ, Quat savedLocalRotation) {
		// Note to future author: the reason for saved local rotation is that
		// we would like to be able to undo the action of rotating the animation data

		// TODO global seqs, needs separate check on AnimRendEnv, and also we must make AnimFlag.find seek on globalSeqId
		// TODO fix cast, meta knowledge: NodeAnimationModelEditor will only be  constructed from
		//  a TimeEnvironmentImpl render environment, and never from the anim previewer impl
		TimeEnvironmentImpl timeEnvironmentImpl = renderModel.getTimeEnvironment();
		QuatAnimFlag rotationTimeline = (QuatAnimFlag) animatedNode.find(MdlUtils.TOKEN_ROTATION, timeEnvironmentImpl.getGlobalSeq());
		if (rotationTimeline == null) {
			return;
		}
		int animationTime = renderModel.getTimeEnvironment().getAnimationTime();
//		int trackTime = renderModel.getAnimatedRenderEnvironment().getCurrentAnimation().getStart() + animationTime;
//		int trackTime = renderModel.getAnimatedRenderEnvironment().getStart() + animationTime;
		int trackTime = animationTime;
		Integer globalSeq = timeEnvironmentImpl.getGlobalSeq();
		if (globalSeq != null) {
			trackTime = timeEnvironmentImpl.getGlobalSeqTime(globalSeq);
		}
		//final RenderNode renderNode = renderModel.getRenderNode(this);
		byte unusedXYZ = CoordSysUtils.getUnusedXYZ(firstXYZ, secondXYZ);
		AnimatedNode parent = null;// = getParent();
		if (animatedNode instanceof IdObject) {
			parent = ((IdObject) animatedNode).getParent();
		}

		Vec4 rotationAxis = new Vec4(0, 0, 0, 1);

		if (parent != null) {
			RenderNode parentRenderNode = renderModel.getRenderNode(parent);

			rotationAxis.transform(parentRenderNode.getWorldMatrix());
			rotationAxis.add(getUnusedAxis(unusedXYZ));
			rotationAxis.transform(Mat4.getInverted(parentRenderNode.getWorldMatrix()));
		} else {
			rotationAxis.add(getUnusedAxis(unusedXYZ));
		}
		rotationAxis.w = (float) radians;
		Quat rotation = new Quat().setFromAxisAngle(rotationAxis);


		if (rotationTimeline.hasEntryAt(trackTime)) {
			Entry<Quat> entry = rotationTimeline.getEntryAt(trackTime);
			entry.getValue().mulLeft(rotation);

			if (savedLocalRotation != null) {
				savedLocalRotation.mul(rotation);
			}

			if (rotationTimeline.tans()) {
				entry.getInTan().mul(rotation);
				entry.getOutTan().mul(rotation);
			}
		}
	}

	Vec3 getUnusedAxis(byte unusedXYZ) {
		return switch (unusedXYZ) {
			case 0 -> new Vec3(1, 0, 0);
			case 1 -> new Vec3(0, -1, 0);
			default -> new Vec3(0, 0, -1);
		};
	}

}
