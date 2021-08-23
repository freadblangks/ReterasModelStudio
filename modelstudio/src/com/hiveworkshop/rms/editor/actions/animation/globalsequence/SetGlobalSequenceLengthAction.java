package com.hiveworkshop.rms.editor.actions.animation.globalsequence;

import com.hiveworkshop.rms.editor.actions.UndoAction;
import com.hiveworkshop.rms.editor.model.GlobalSeq;
import com.hiveworkshop.rms.ui.application.edit.ModelStructureChangeListener;

public class SetGlobalSequenceLengthAction implements UndoAction {
	private final GlobalSeq globalSeq;
	private final Integer oldLength;
	private final Integer newLength;
	private final ModelStructureChangeListener changeListener;

	public SetGlobalSequenceLengthAction(GlobalSeq globalSeq, Integer newLength, ModelStructureChangeListener changeListener) {
		this.globalSeq = globalSeq;
		this.oldLength = globalSeq.getLength();
		this.newLength = newLength;
		this.changeListener = changeListener;
	}

	@Override
	public UndoAction undo() {
		globalSeq.setLength(oldLength);
		if (changeListener != null) {
			changeListener.globalSequenceLengthChanged();
		}
		return this;
	}

	@Override
	public UndoAction redo() {
		globalSeq.setLength(newLength);
		if (changeListener != null) {
			changeListener.globalSequenceLengthChanged();
		}
		return this;
	}

	@Override
	public String actionName() {
		return "change GlobalSequence length to " + newLength;
	}
}
