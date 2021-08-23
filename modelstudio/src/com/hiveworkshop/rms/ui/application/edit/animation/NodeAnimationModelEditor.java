package com.hiveworkshop.rms.ui.application.edit.animation;

import com.hiveworkshop.rms.editor.actions.UndoAction;
import com.hiveworkshop.rms.editor.actions.animation.*;
import com.hiveworkshop.rms.editor.actions.editor.StaticMeshMoveAction;
import com.hiveworkshop.rms.editor.actions.util.CompoundAction;
import com.hiveworkshop.rms.editor.actions.util.GenericMoveAction;
import com.hiveworkshop.rms.editor.actions.util.GenericRotateAction;
import com.hiveworkshop.rms.editor.actions.util.GenericScaleAction;
import com.hiveworkshop.rms.editor.model.Bone;
import com.hiveworkshop.rms.editor.model.GlobalSeq;
import com.hiveworkshop.rms.editor.model.IdObject;
import com.hiveworkshop.rms.editor.model.animflag.AnimFlag;
import com.hiveworkshop.rms.editor.model.animflag.Entry;
import com.hiveworkshop.rms.editor.model.animflag.QuatAnimFlag;
import com.hiveworkshop.rms.editor.model.animflag.Vec3AnimFlag;
import com.hiveworkshop.rms.editor.render3d.RenderModel;
import com.hiveworkshop.rms.editor.render3d.RenderNode;
import com.hiveworkshop.rms.parsers.mdlx.InterpolationType;
import com.hiveworkshop.rms.parsers.mdlx.mdl.MdlUtils;
import com.hiveworkshop.rms.ui.application.edit.ModelStructureChangeListener;
import com.hiveworkshop.rms.ui.application.edit.mesh.ModelEditor;
import com.hiveworkshop.rms.ui.gui.modeledit.ModelHandler;
import com.hiveworkshop.rms.ui.gui.modeledit.selection.SelectionItemTypes;
import com.hiveworkshop.rms.ui.gui.modeledit.selection.SelectionManager;
import com.hiveworkshop.rms.ui.gui.modeledit.toolbar.ModelEditorActionType3;
import com.hiveworkshop.rms.util.Quat;
import com.hiveworkshop.rms.util.Vec3;

import java.util.*;

public class NodeAnimationModelEditor extends ModelEditor {
	private final RenderModel renderModel;
	private final ModelStructureChangeListener changeListener;

	public NodeAnimationModelEditor(SelectionManager selectionManager, ModelHandler modelHandler,
	                                SelectionItemTypes selectionMode) {
		super(selectionManager, modelHandler.getModelView());
		this.changeListener = ModelStructureChangeListener.changeListener;
		this.renderModel = modelHandler.getRenderModel();
	}

	private static AddKeyframeAction getAddKeyframeAction(AnimFlag<?> timeline, int trackTime, Entry<?> entry) {
		if (!timeline.hasEntryAt(trackTime)) {
			if (timeline.getInterpolationType().tangential()) {
				entry.unLinearize();
			}
			return new AddKeyframeAction(timeline, entry);
		}
		return null;
	}

	@Override
	public UndoAction translate(Vec3 v) {
		return beginTranslation().updateTranslation(v).redo();
	}

	@Override
	public UndoAction scale(Vec3 center, Vec3 scale) {
		return beginScaling(center).updateScale(scale).redo();
	}

	@Override
	public UndoAction rotate(Vec3 center, Vec3 rotate) {
		return new CompoundAction("rotate", Arrays.asList(
				beginRotation(center, (byte) 2, (byte) 1).updateRotation(rotate.x),
				beginRotation(center, (byte) 0, (byte) 2).updateRotation(rotate.y),
				beginRotation(center, (byte) 1, (byte) 0).updateRotation(rotate.z)))
				.redo();
	}

	@Override
	public boolean editorWantsAnimation() {
		return true;
	}

	@Override
	public UndoAction setPosition(Vec3 center, Vec3 v) {
		Vec3 delta = Vec3.getDiff(v, center);
		return new StaticMeshMoveAction(modelView, delta).redo();
	}

	private List<UndoAction> generateKeyframes(TimeEnvironmentImpl timeEnvironmentImpl, ModelEditorActionType3 actionType, Set<IdObject> selection) {
		List<UndoAction> actions = new ArrayList<>();

		int trackTime = timeEnvironmentImpl.getEnvTrackTime();

		for (IdObject node : selection) {
			AnimFlag<?> timeline = getTimeline(timeEnvironmentImpl, actionType, actions, node);
			if (!node.has(timeline.getName())) {
				actions.add(new AddTimelineAction(node, timeline));
			}

			RenderNode renderNode = renderModel.getRenderNode(node);

			AddKeyframeAction keyframeAction = getAddKeyframeAction(timeline, trackTime, getEntry(actionType, trackTime, renderNode));

			if (keyframeAction != null) {
				actions.add(keyframeAction);
			}
		}
		return actions;
	}

	private AnimFlag<?> getTimeline(TimeEnvironmentImpl timeEnvironmentImpl, ModelEditorActionType3 actionType, List<UndoAction> actions, IdObject node) {
		AnimFlag<?> timeline = switch (actionType) {
			case ROTATION, SQUAT -> node.getRotationFlag(timeEnvironmentImpl.getGlobalSeq());
			case SCALING -> node.getScalingFlag(timeEnvironmentImpl.getGlobalSeq());
			case TRANSLATION, EXTEND, EXTRUDE -> node.getTranslationFlag(timeEnvironmentImpl.getGlobalSeq());
		};

		if (timeline == null) {
			timeline = switch (actionType) {
				case ROTATION, SQUAT -> new QuatAnimFlag(MdlUtils.TOKEN_ROTATION, InterpolationType.HERMITE, timeEnvironmentImpl.getGlobalSeq());
				case SCALING -> new Vec3AnimFlag(MdlUtils.TOKEN_SCALING, InterpolationType.HERMITE, timeEnvironmentImpl.getGlobalSeq());
				case TRANSLATION, EXTEND, EXTRUDE -> new Vec3AnimFlag(MdlUtils.TOKEN_TRANSLATION, InterpolationType.HERMITE, timeEnvironmentImpl.getGlobalSeq());
			};
//			actions.add(new AddTimelineAction(node, timeline));
		}
		return timeline;
	}

	private Entry<?> getEntry(ModelEditorActionType3 actionType, int trackTime, RenderNode renderNode) {
		return switch (actionType) {
			case ROTATION, SQUAT -> new Entry<>(trackTime, new Quat(renderNode.getLocalRotation()));
			case SCALING -> new Entry<>(trackTime, new Vec3(renderNode.getLocalScale()));
			case TRANSLATION, EXTEND, EXTRUDE -> new Entry<>(trackTime, new Vec3(renderNode.getLocalLocation()));
		};
	}

	@Override
	public GenericMoveAction beginTranslation() {
		Set<IdObject> selection = modelView.getSelectedIdObjects();
		// TODO fix cast, meta knowledge: NodeAnimationModelEditor will only be constructed from a TimeEnvironmentImpl render environment, and never from the anim previewer impl
		TimeEnvironmentImpl timeEnvironmentImpl = renderModel.getTimeEnvironment();

//		List<UndoAction> actions = generateKeyframes(timeEnvironmentImpl, ModelEditorActionType3.TRANSLATION, selection);
		List<UndoAction> actions = createKeyframe(timeEnvironmentImpl, ModelEditorActionType3.TRANSLATION, selection);

		CompoundAction setup = new CompoundAction("setup", actions, changeListener::keyframesUpdated);
		return new TranslationKeyframeAction(setup.redo(), selection, renderModel);
	}

	@Override
	public GenericScaleAction beginScaling(Vec3 center) {
		Set<IdObject> selection = modelView.getSelectedIdObjects();
		TimeEnvironmentImpl timeEnvironmentImpl = renderModel.getTimeEnvironment();

		List<UndoAction> actions = generateKeyframes(timeEnvironmentImpl, ModelEditorActionType3.SCALING, selection);


		CompoundAction setup = new CompoundAction("setup", actions, changeListener::keyframesUpdated);
		return new ScalingKeyframeAction(setup.redo(), selection, center, renderModel);
	}

	@Override
	public GenericRotateAction beginRotation(Vec3 center, byte firstXYZ, byte secondXYZ) {
		Set<IdObject> selection = modelView.getSelectedIdObjects();

		TimeEnvironmentImpl timeEnvironmentImpl = renderModel.getTimeEnvironment();

		List<UndoAction> actions = generateKeyframes(timeEnvironmentImpl, ModelEditorActionType3.ROTATION, selection);

		CompoundAction setup = new CompoundAction("setup", actions, changeListener::keyframesUpdated);
		return new RotationKeyframeAction(setup.redo(), selection, renderModel, center, firstXYZ, secondXYZ);
	}

	@Override
	public GenericRotateAction beginSquatTool(Vec3 center, byte firstXYZ, byte secondXYZ) {
		Set<IdObject> selection = new HashSet<>(modelView.getSelectedIdObjects());

		for (IdObject idObject : modelView.getModel().getIdObjects()) {
			if (modelView.getSelectedIdObjects().contains(idObject.getParent())
					&& isBoneAndSameClass(idObject, idObject.getParent())) {
				selection.add(idObject);
			}
		}

		TimeEnvironmentImpl timeEnvironmentImpl = renderModel.getTimeEnvironment();
		List<UndoAction> actions = generateKeyframes(timeEnvironmentImpl, ModelEditorActionType3.SQUAT, selection);

		CompoundAction setup = new CompoundAction("setup", actions, changeListener::keyframesUpdated);
		return new SquatToolKeyframeAction(setup.redo(), selection, renderModel, modelView.getModel().getIdObjects(), center, firstXYZ, secondXYZ);
	}

	@Override
	public GenericRotateAction beginRotation(Vec3 center, Vec3 axis) {
		Set<IdObject> selection = modelView.getSelectedIdObjects();

		TimeEnvironmentImpl timeEnvironmentImpl = renderModel.getTimeEnvironment();

		List<UndoAction> actions = generateKeyframes(timeEnvironmentImpl, ModelEditorActionType3.ROTATION, selection);

		CompoundAction setup = new CompoundAction("setup", actions, changeListener::keyframesUpdated);
		return new RotationKeyframeAction2(setup.redo(), selection, renderModel, center, axis);
	}

	@Override
	public GenericRotateAction beginSquatTool(Vec3 center, Vec3 axis) {
		Set<IdObject> selection = new HashSet<>(modelView.getSelectedIdObjects());

		for (IdObject idObject : modelView.getModel().getIdObjects()) {
			if (modelView.getSelectedIdObjects().contains(idObject.getParent())
					&& isBoneAndSameClass(idObject, idObject.getParent())) {
				selection.add(idObject);
			}
		}

		TimeEnvironmentImpl timeEnvironmentImpl = renderModel.getTimeEnvironment();
		List<UndoAction> actions = generateKeyframes(timeEnvironmentImpl, ModelEditorActionType3.SQUAT, selection);

		CompoundAction setup = new CompoundAction("setup", actions, changeListener::keyframesUpdated);
		return new SquatToolKeyframeAction2(setup.redo(), selection, renderModel, modelView.getModel().getIdObjects(), center, axis);
	}

	private boolean isBoneAndSameClass(IdObject idObject1, IdObject idObject2) {
		return idObject1 instanceof Bone
				&& idObject2 instanceof Bone
				&& idObject1.getClass() == idObject2.getClass();
	}

	public List<UndoAction> createKeyframe(TimeEnvironmentImpl timeEnvironmentImpl, ModelEditorActionType3 actionType, Set<IdObject> selection) {
		List<UndoAction> actions = new ArrayList<>();
		for (IdObject node : selection) {
			UndoAction keyframeAction = switch (actionType) {
				case ROTATION, SQUAT -> createRotationKeyframe(node, actions, timeEnvironmentImpl);
				case SCALING -> createScalingKeyframe(node, actions, timeEnvironmentImpl);
				case TRANSLATION, EXTEND, EXTRUDE -> createTranslationKeyframe(node, actions, timeEnvironmentImpl);
			};
			if (keyframeAction != null) {
				actions.add(keyframeAction.redo());
			}
		}

		return actions;
	}

	private <T> AddKeyframeAction_T<T> getAddKeyframeAction(AnimFlag<T> timeline, Entry<T> entry, int trackTime, TimeEnvironmentImpl timeEnvironmentImpl) {
		// TODO global seqs, needs separate check on AnimRendEnv, and also we must make AnimFlag.find seek on globalSeqId
		if (timeline.tans()) {
			Entry<T> entryIn = timeline.getFloorEntry(trackTime, timeEnvironmentImpl);
			Entry<T> entryOut = timeline.getCeilEntry(trackTime, timeEnvironmentImpl);
			if (entryIn != null && entryOut != null) {

			}
			int animationLength = timeEnvironmentImpl.getCurrentAnimation().length();
//				float factor = getTimeFactor(trackTime, animationLength, entryIn.time, entryOut.time);
			float[] tbcFactor = timeline.getTbcFactor(0, 0.5f, 0);
			timeline.calcNewTans(tbcFactor, entryOut, entryIn, entry, animationLength);
			System.out.println("calc tans! " + entryIn + entryOut + entry);

//			AddKeyframeAction_T<T> addKeyframeAction = new AddKeyframeAction_T<>(timeline, entry);
//			addKeyframeAction.redo();
			return new AddKeyframeAction_T<>(timeline, entry);
		}
		if (!timeline.hasEntryAt(trackTime)) {
			if (timeline.getInterpolationType().tangential()) {
				entry.unLinearize();
			}

//			AddKeyframeAction_T<T> addKeyframeAction = new AddKeyframeAction_T<>(timeline, entry);
//			addKeyframeAction.redo();
			return new AddKeyframeAction_T<>(timeline, entry);
		}
		return null;
	}


	public UndoAction createTranslationKeyframe(IdObject node, List<UndoAction> actions, TimeEnvironmentImpl timeEnvironmentImpl) {
		GlobalSeq globalSeq = timeEnvironmentImpl.getGlobalSeq();
		int trackTime = timeEnvironmentImpl.getEnvTrackTime();
		AnimFlag<Vec3> timeline = node.getTranslationFlag(globalSeq);
		if (timeline == null) {
			timeline = new Vec3AnimFlag(MdlUtils.TOKEN_TRANSLATION, InterpolationType.HERMITE, globalSeq);

			actions.add(new AddTimelineAction(node, timeline));
		}
		RenderNode renderNode = renderModel.getRenderNode(node);
		return getAddKeyframeAction(timeline, new Entry<>(trackTime, new Vec3(renderNode.getLocalLocation())), trackTime, timeEnvironmentImpl);
	}

	public UndoAction createScalingKeyframe(IdObject node, List<UndoAction> actions, TimeEnvironmentImpl timeEnvironmentImpl) {
		GlobalSeq globalSeq = timeEnvironmentImpl.getGlobalSeq();
		int trackTime = timeEnvironmentImpl.getEnvTrackTime();
		AnimFlag<Vec3> timeline = node.getScalingFlag(globalSeq);
		if (timeline == null) {
			timeline = new Vec3AnimFlag(MdlUtils.TOKEN_SCALING, InterpolationType.HERMITE, globalSeq);

			actions.add(new AddTimelineAction(node, timeline));
		}
		RenderNode renderNode = renderModel.getRenderNode(node);
		return getAddKeyframeAction(timeline, new Entry<>(trackTime, new Vec3(renderNode.getLocalScale())), trackTime, timeEnvironmentImpl);
	}

	public UndoAction createRotationKeyframe(IdObject node, List<UndoAction> actions, TimeEnvironmentImpl timeEnvironmentImpl) {
		GlobalSeq globalSeq = timeEnvironmentImpl.getGlobalSeq();
		int trackTime = timeEnvironmentImpl.getEnvTrackTime();
		AnimFlag<Quat> timeline = node.getRotationFlag(globalSeq);
		if (timeline == null) {
			timeline = new QuatAnimFlag(MdlUtils.TOKEN_ROTATION, InterpolationType.HERMITE, globalSeq);

			actions.add(new AddTimelineAction(node, timeline));
		}
		RenderNode renderNode = renderModel.getRenderNode(node);
		return getAddKeyframeAction(timeline, new Entry<>(trackTime, new Quat(renderNode.getLocalRotation())), trackTime, timeEnvironmentImpl);
	}
}
