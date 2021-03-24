package com.hiveworkshop.rms.ui.application.model.nodepanels;

import com.hiveworkshop.rms.editor.model.Attachment;
import com.hiveworkshop.rms.editor.model.EditableModel;
import com.hiveworkshop.rms.editor.model.IdObject;
import com.hiveworkshop.rms.editor.wrapper.v2.ModelViewManager;
import com.hiveworkshop.rms.ui.application.edit.ModelStructureChangeListener;
import com.hiveworkshop.rms.ui.application.edit.mesh.activity.UndoActionListener;
import com.hiveworkshop.rms.ui.application.model.ComponentPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class ComponentAttatchmentPanel extends JPanel implements ComponentPanel<Attachment> {
	private final ModelViewManager modelViewManager;
	private final UndoActionListener undoActionListener;
	private final ModelStructureChangeListener modelStructureChangeListener;
	Attachment attachment;

	JLabel title;
	JLabel parentName;


	public ComponentAttatchmentPanel(final ModelViewManager modelViewManager,
	                                 final UndoActionListener undoActionListener,
	                                 final ModelStructureChangeListener modelStructureChangeListener) {
		this.undoActionListener = undoActionListener;
		this.modelViewManager = modelViewManager;
		this.modelStructureChangeListener = modelStructureChangeListener;
		setLayout(new MigLayout("fill, gap 0", "[][][grow]", "[][][grow]"));
		title = new JLabel("Select an Emitter");
		add(title, "wrap");
		add(new JLabel("Parent: "));
		parentName = new JLabel("Parent");
		add(parentName, "wrap");

	}

	@Override
	public void setSelectedItem(Attachment itemToSelect) {
		attachment = itemToSelect;
		title.setText(attachment.getName());
		IdObject parent = attachment.getParent();
		if (parent != null) {
			this.parentName.setText(parent.getName());
		} else {
			parentName.setText("no parent");
		}
		revalidate();
		repaint();

	}

	@Override
	public void save(EditableModel model, UndoActionListener undoListener, ModelStructureChangeListener changeListener) {

	}
}