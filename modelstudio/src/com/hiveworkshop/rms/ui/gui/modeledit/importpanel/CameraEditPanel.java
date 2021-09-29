package com.hiveworkshop.rms.ui.gui.modeledit.importpanel;

import com.hiveworkshop.rms.ui.gui.modeledit.renderers.CameraShellListCellRenderer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

public class CameraEditPanel extends JPanel {

	public CardLayout cardLayout = new CardLayout();
	public JPanel panelCards = new JPanel(cardLayout);
	public MultiCameraPanel multiCameraPanel;
	ModelHolderThing mht;

	CameraPanel singleCameraPanel;

	public CameraEditPanel(ModelHolderThing mht) {
		setLayout(new MigLayout("gap 0", "[grow][grow]", "[][grow]"));
		this.mht = mht;

		add(getButton("Import All", e -> mht.importAllCams(true)), "cell 0 0, right");
		add(getButton("Leave All", e -> mht.importAllCams(false)), "cell 1 0, left");


		singleCameraPanel = new CameraPanel(mht);
		multiCameraPanel = new MultiCameraPanel(mht);

		panelCards.add(new JPanel(), "blank");
		panelCards.add(singleCameraPanel, "single");
		panelCards.add(multiCameraPanel, "multiple");
		panelCards.setBorder(BorderFactory.createLineBorder(Color.blue.darker()));

		JScrollPane cameraListPane = getCameraListPane(mht);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cameraListPane, panelCards);
		add(splitPane, "cell 0 1, growx, growy, spanx 2");
	}

	private JScrollPane getCameraListPane(ModelHolderThing mht) {
		CameraShellListCellRenderer cameraPanelRenderer = new CameraShellListCellRenderer();
		mht.allCameraJList.setCellRenderer(cameraPanelRenderer);
		mht.allCameraJList.addListSelectionListener(e -> objectTabsValueChanged(mht, e));
		return new JScrollPane(mht.allCameraJList);
	}

	private JButton getButton(String text, ActionListener actionListener) {
		JButton button = new JButton(text);
		button.addActionListener(actionListener);
		return button;
	}

	private void objectTabsValueChanged(ModelHolderThing mht, ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			List<CameraShell> selectedValuesList = mht.donModCameraJList.getSelectedValuesList();
			if (selectedValuesList.size() < 1) {
				cardLayout.show(panelCards, "blank");
			} else if (selectedValuesList.size() == 1) {
				cardLayout.show(panelCards, "single");
				singleCameraPanel.setSelectedObject(mht.donModCameraJList.getSelectedValue());
			} else {
				multiCameraPanel.updateMultiCameraPanel(selectedValuesList);
				cardLayout.show(panelCards, "multiple");
			}
		}
	}
}
