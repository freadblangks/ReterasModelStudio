package com.hiveworkshop.rms.ui.gui.modeledit.renderers;

import com.hiveworkshop.rms.editor.model.EditableModel;
import com.hiveworkshop.rms.ui.gui.modeledit.importpanel.BoneShell;
import com.hiveworkshop.rms.ui.gui.modeledit.importpanel.ObjectShell;
import com.hiveworkshop.rms.ui.util.AbstractSnapshottingListCellRenderer2D;
import com.hiveworkshop.rms.util.Vec3;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MatrixEditListRenderer extends AbstractSnapshottingListCellRenderer2D<BoneShell> {
	boolean showParent = false;

	BoneShell selectedBone;
	ObjectShell selectedObject;
	boolean showClass = false;

	Set<BoneShell> bonesInAllMatricies = new HashSet<>();
	Set<BoneShell> bonesNotInAllMatricies = new HashSet<>();

	public MatrixEditListRenderer(EditableModel recModel, EditableModel donModel) {
		super(recModel, donModel);
	}

	public MatrixEditListRenderer setShowClass(boolean b) {
		showClass = b;
		return this;
	}

	public MatrixEditListRenderer setShowParent(boolean b) {
		showParent = b;
		return this;
	}

	public void setSelectedBoneShell(BoneShell boneShell) {
		selectedBone = boneShell;
	}

	public void setSelectedObjectShell(ObjectShell objectShell) {
		selectedObject = objectShell;
	}

	@Override
	protected boolean isFromDonating(BoneShell value){
		if(value != null){
			return value.isFromDonating();
		}
		return false;
	}

	@Override
	protected boolean isFromReceiving(BoneShell value){
		if(value != null){
			return !value.isFromDonating();
		}
		return false;
	}

	@Override
	protected BoneShell valueToType(final Object value) {
		return (BoneShell) value;
	}

	@Override
	protected boolean contains(EditableModel model, final BoneShell object) {
		if(model != null){
			return model.contains(object.getBone());
		}
		return false;
	}

	@Override
	protected Vec3 getRenderVertex(final BoneShell value) {
		return value.getBone().getPivotPoint();
	}


	public void addInAllBone(BoneShell boneShell) {
		bonesInAllMatricies.add(boneShell);
	}

	public void removeInAllBone(BoneShell boneShell) {
		bonesInAllMatricies.remove(boneShell);
	}

	public void addNotInAllBone(BoneShell boneShell) {
		bonesNotInAllMatricies.add(boneShell);
	}

	public void removeNotInAllBone(BoneShell boneShell) {
		bonesNotInAllMatricies.remove(boneShell);
	}

	public void addNotInAllBone(Collection<BoneShell> boneShells) {
		bonesNotInAllMatricies.addAll(boneShells);
	}

	public void removeNotInAllBone(Collection<BoneShell> boneShells) {
		bonesNotInAllMatricies.removeAll(boneShells);
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSel, boolean hasFoc) {
		super.getListCellRendererComponent(list, value, index, isSel, hasFoc);

		Vec3 bg = noOwnerBgCol;
		Vec3 fg = noOwnerFgCol;

		if (value instanceof BoneShell) {
			setText(((BoneShell) value).toString(showClass, showParent));
			if (selectedBone != null && selectedBone.getNewParentBs() == value
					|| selectedObject != null && selectedObject.getNewParentBs() == value) {
				bg = selectedOwnerBgCol;
				fg = selectedOwnerFgCol;
			} else if (bonesNotInAllMatricies.contains(value)) {
				new Color(150, 80, 80);
				bg = new Vec3(150, 80, 80);
				fg = otherOwnerFgCol;
			}
		} else {
			setText(value.toString());
		}
		if (value instanceof BoneShell && ((BoneShell) value).getImportStatus() != BoneShell.ImportType.IMPORT) {
			bg = Vec3.getProd(bg, otherOwnerBgCol).normalize().scale(160);
			fg = Vec3.getProd(bg, otherOwnerFgCol).normalize().scale(60);
		}

		if (isSel) {
			bg = Vec3.getSum(bg, hLAdjBgCol);
		}

		this.setBackground(bg.asIntColor());
		this.setForeground(fg.asIntColor());

		return this;
	}
}
