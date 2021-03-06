package com.hiveworkshop.rms.ui.gui.modeledit.importpanel;

import com.hiveworkshop.rms.editor.model.Bone;
import com.hiveworkshop.rms.editor.model.Matrix;
import com.hiveworkshop.rms.editor.wrapper.v2.ModelViewManager;
import com.hiveworkshop.rms.util.IterableListModel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoneAttachmentEditPanel extends JPanel {

	JCheckBox displayParents;
	ModelHolderThing mht;
	public JTabbedPane geosetAnimTabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);

	public BoneAttachmentEditPanel(ModelHolderThing mht) {
		setLayout(new MigLayout("gap 0, fill", "[grow]", "[][grow]"));
		this.mht = mht;

		add(getTopPanel(), "align center, wrap");

		final ParentToggleRenderer ptr = makeMatricesPanel(mht.recModelManager, mht.donModelManager);

		for (GeosetShell geosetShell : mht.allGeoShells) {
			final BoneAttachmentPanel geoPanel = new BoneAttachmentPanel(mht, ptr);
			geoPanel.setGeoset(geosetShell);

			geosetAnimTabs.addTab(geosetShell.getModelName() + " " + (geosetShell.getIndex() + 1), ImportPanel.greenIcon, geoPanel, "Click to modify animation data for Geoset " + geosetShell.getIndex() + " from " + geosetShell.getModelName() + ".");
		}

		geosetAnimTabs.addChangeListener(mht.getDaChangeListener());


		add(geosetAnimTabs, "growx, growy");
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				super.componentShown(e);
				for (GeosetShell geosetShell : mht.allGeoShells) {
					for (int i = 0; i < geosetAnimTabs.getTabCount(); i++) {
						final BoneAttachmentPanel geoPanel = (BoneAttachmentPanel) geosetAnimTabs.getComponentAt(i);
						if (geoPanel.selectedGeoset == geosetShell) {
							geosetAnimTabs.setEnabledAt(i, geosetShell.isDoImport());
						}
					}
				}
			}
		});
	}

	static void uncheckUnusedBoneAttatchments(ModelHolderThing mht, List<BoneShell> usedBonePanels) {
		for (GeosetShell geosetShell : mht.allGeoShells) {
			if (geosetShell.isDoImport()) {
				for (MatrixShell ms : geosetShell.getMatrixShells()) {
					for (final BoneShell bs : ms.getNewBones()) {
						BoneShell shell = bs;
						BoneShell current = shell;
						if (!usedBonePanels.contains(current)) {
							usedBonePanels.add(current);
						}

						boolean good = true;
						int k = 0;
						while (good) {
							if ((current == null) || (current.getImportStatus() == 1)) {
								break;
							}
							shell = current.getNewParentBs();
							// If shell is null, then the bone has "No Parent"
							// If current's selected index is not 2,
							if (shell == null)// current.getSelectedIndex() != 2
							{
								good = false;
							} else {
								current = shell;
								if (usedBonePanels.contains(current)) {
									good = false;
								} else {
									usedBonePanels.add(current);
								}
							}
							k++;
							if (k > 1000) {
								JOptionPane.showMessageDialog(null, "Unexpected error has occurred: IdObject to Bone parent loop, circular logic");
								break;
							}
						}
					}
				}
			}
		}
	}

	private JPanel getTopPanel() {
		JPanel topPanel = new JPanel(new MigLayout("gap 0", "[align center]"));

		displayParents = new JCheckBox("Display parent names");
		displayParents.addChangeListener(mht.getDaChangeListener());
		topPanel.add(displayParents, "wrap");

		JButton allMatrOriginal = new JButton("Reset all Matrices");
		allMatrOriginal.addActionListener(e -> allMatrOriginal());
		topPanel.add(allMatrOriginal, "wrap");

		JButton allMatrSameName = new JButton("Set all to available, original names");
		allMatrSameName.addActionListener(e -> allMatrSameName());
		topPanel.add(allMatrSameName, "wrap");

		return topPanel;
	}

	private ParentToggleRenderer makeMatricesPanel(ModelViewManager recModelManager, ModelViewManager donModelManager) {
		return new ParentToggleRenderer(displayParents, recModelManager, donModelManager);
	}

	public void allMatrOriginal() {
		for (GeosetShell geosetShell : mht.allGeoShells) {
			if (geosetShell.isDoImport()) {
				for (MatrixShell ms : geosetShell.getMatrixShells()) {
					ms.resetMatrix();
				}
			}
		}
	}

	public void allMatrSameName() {
		IterableListModel<BoneShell> futureBoneList = mht.getFutureBoneList();

		Map<String, BoneShell> nameMap = new HashMap<>();
		for (BoneShell boneShell : futureBoneList) {
			nameMap.put(boneShell.getName(), boneShell);
		}

		for (GeosetShell geosetShell : mht.allGeoShells) {
			if (geosetShell.isDoImport()) {
				for (MatrixShell matrixShell : geosetShell.getMatrixShells()) {
					matrixShell.clearNewBones();
					final Matrix matrix = matrixShell.getMatrix();
					// For look to find similarly named stuff and add it
					for (final Bone bone : matrix.getBones()) {
						final String mName = bone.getName();
						if (nameMap.get(mName) != null) {
							matrixShell.addNewBone(nameMap.get(mName));
						}
					}
				}
			}
		}
	}
}
