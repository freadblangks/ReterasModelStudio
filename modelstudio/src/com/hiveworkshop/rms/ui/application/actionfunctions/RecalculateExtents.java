package com.hiveworkshop.rms.ui.application.actionfunctions;

import com.hiveworkshop.rms.editor.actions.model.RecalculateExtentsAction;
import com.hiveworkshop.rms.editor.wrapper.v2.ModelView;
import com.hiveworkshop.rms.ui.application.ProgramGlobals;
import com.hiveworkshop.rms.ui.gui.modeledit.ModelPanel;
import com.hiveworkshop.rms.ui.language.TextKey;
import com.hiveworkshop.rms.util.SmartButtonGroup;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class RecalculateExtents extends ActionFunction{
	private static double lastNormalMaxAngle = 90;
	private static boolean useTris = false;

	public RecalculateExtents(){
		super(TextKey.RECALCULATE_EXTENTS, () -> recalculateExtents());
		setKeyStroke(KeyStroke.getKeyStroke("control shift E"));
	}

	public static void recalculateExtents() {
		ModelPanel modelPanel = ProgramGlobals.getCurrentModelPanel();
		if (modelPanel != null) {
			JPanel messagePanel = new JPanel(new MigLayout());
			messagePanel.add(new JLabel("This will calculate the extents of all model components. Proceed?"), "wrap");
			messagePanel.add(new JLabel("(It may destroy existing extents)"), "wrap");

			SmartButtonGroup buttonGroup2 = new SmartButtonGroup();
			buttonGroup2.addJRadioButton("Consider all geosets for calculation", null);
			buttonGroup2.addJRadioButton("Consider current editable geosets for calculation", null);
			buttonGroup2.setSelectedIndex(0);

			messagePanel.add(buttonGroup2.getButtonPanel(), "wrap");

//            JRadioButton considerAllBtn = new JRadioButton("Consider all geosets for calculation");
//            JRadioButton considerCurrentBtn = new JRadioButton("Consider current editable geosets for calculation");
//            ButtonGroup buttonGroup = new ButtonGroup();
//            buttonGroup.add(considerAllBtn);
//            buttonGroup.add(considerCurrentBtn);
//            considerAllBtn.setSelected(true);
//            messagePanel.add(considerAllBtn, "wrap");
//            messagePanel.add(considerCurrentBtn, "wrap");

			int userChoice = JOptionPane.showConfirmDialog(ProgramGlobals.getMainPanel(), messagePanel, "Message",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (userChoice == JOptionPane.YES_OPTION) {
				ModelView modelView = modelPanel.getModelView();

				RecalculateExtentsAction recalculateExtentsAction;
				if (buttonGroup2.getSelectedIndex()==0) {
					recalculateExtentsAction = new RecalculateExtentsAction(modelView, modelView.getEditableGeosets());
				} else {
					recalculateExtentsAction = new RecalculateExtentsAction(modelView, modelView.getModel().getGeosets());
				}

				modelPanel.getUndoManager().pushAction(recalculateExtentsAction.redo());
			}
		}
		ProgramGlobals.getMainPanel().repaint();
	}
}