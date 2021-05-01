package com.hiveworkshop.rms.ui.gui.modeledit.toolbar;

import com.hiveworkshop.rms.editor.wrapper.v2.ModelView;
import com.hiveworkshop.rms.ui.application.MainPanel;
import com.hiveworkshop.rms.ui.application.edit.mesh.ModelEditorManager;
import com.hiveworkshop.rms.ui.application.edit.mesh.activity.ActivityDescriptor;
import com.hiveworkshop.rms.ui.application.edit.mesh.activity.ModelEditorMultiManipulatorActivity;
import com.hiveworkshop.rms.ui.application.edit.mesh.activity.ModelEditorViewportActivity;
import com.hiveworkshop.rms.ui.gui.modeledit.ModelHandler;
import com.hiveworkshop.rms.ui.gui.modeledit.newstuff.actions.ModelEditorActionType;
import com.hiveworkshop.rms.ui.gui.modeledit.newstuff.builder.model.*;
import com.hiveworkshop.rms.ui.icons.RMSIcons;
import com.hiveworkshop.rms.ui.preferences.ProgramPreferences;

import javax.swing.*;

public class ToolbarActionButtonType implements ToolbarButtonType, ActivityDescriptor {
	private final ImageIcon imageIcon;
	private final String name;
	MainPanel mainPanel;
	ProgramPreferences programPreferences;
	private String action;

	public ToolbarActionButtonType(String action, String path, String name, MainPanel mainPanel) {
		this.action = action;
		this.imageIcon = RMSIcons.loadToolBarImageIcon(path);
		this.name = name;
		this.mainPanel = mainPanel;
		this.programPreferences = mainPanel.getPrefs();
	}

	@Override
	public ImageIcon getImageIcon() {
		return imageIcon;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ModelEditorViewportActivity createActivity(ModelEditorManager modelEditorManager, ModelHandler modelHandler) {
		mainPanel.actionType = getActivityType();
		return new ModelEditorMultiManipulatorActivity(getBuilder(modelEditorManager, modelHandler.getModelView()), modelHandler.getUndoManager(), modelEditorManager.getSelectionView());
	}

	private ModelEditorActionType getActivityType() {
		return switch (action) {
			case "move", "extrude", "extend" -> ModelEditorActionType.TRANSLATION;
			case "scale" -> ModelEditorActionType.SCALING;
			case "rotate" -> ModelEditorActionType.ROTATION;

			default -> throw new IllegalStateException("Unexpected value: " + action);
		};
	}

	private ModelEditorManipulatorBuilder getBuilder(ModelEditorManager modelEditorManager, ModelView modelView) {
		return switch (action) {
			case "move" -> new MoverWidgetManipulatorBuilder(modelEditorManager.getModelEditor(), modelEditorManager.getViewportSelectionHandler(), programPreferences, modelView);
			case "scale" -> new ScaleWidgetManipulatorBuilder(modelEditorManager.getModelEditor(), modelEditorManager.getViewportSelectionHandler(), programPreferences, modelView);
			case "rotate" -> new RotatorWidgetManipulatorBuilder(modelEditorManager.getModelEditor(), modelEditorManager.getViewportSelectionHandler(), programPreferences, modelView);
			case "extrude" -> new ExtrudeWidgetManipulatorBuilder(modelEditorManager.getModelEditor(), modelEditorManager.getViewportSelectionHandler(), programPreferences, modelView);
			case "extend" -> new ExtendWidgetManipulatorBuilder(modelEditorManager.getModelEditor(), modelEditorManager.getViewportSelectionHandler(), programPreferences, modelView);

			default -> throw new IllegalStateException("Unexpected value: " + action);
		};
	}
}
