package com.hiveworkshop.rms.ui.gui.modeledit.manipulator;

import com.hiveworkshop.rms.editor.actions.UndoAction;
import com.hiveworkshop.rms.ui.application.ProgramGlobals;
import com.hiveworkshop.rms.ui.application.edit.mesh.viewport.axes.CoordinateSystem;
import com.hiveworkshop.rms.ui.application.edit.mesh.viewport.selection.ViewportSelectionHandler;
import com.hiveworkshop.rms.util.Vec2;

import java.awt.*;
import java.awt.event.MouseEvent;

public class SelectManipulator extends Manipulator {
	private final ViewportSelectionHandler viewportSelectionHandler;
	private Vec2 mouseEnd;
	private final CoordinateSystem coordinateSystem;
	private byte currentDim1;
	private byte currentDim2;

	public SelectManipulator(ViewportSelectionHandler viewportSelectionHandler, CoordinateSystem coordinateSystem) {
		this.viewportSelectionHandler = viewportSelectionHandler;
		this.coordinateSystem = coordinateSystem;
	}

	@Override
	protected void onStart(MouseEvent e, Vec2 mouseStart, byte dim1, byte dim2) {
		currentDim1 = dim1;
		currentDim2 = dim2;
	}

	@Override
	public void update(MouseEvent e, Vec2 mouseStart, Vec2 mouseEnd, byte dim1, byte dim2) {
		this.mouseEnd = mouseEnd;
	}

	@Override
	public UndoAction finish(MouseEvent e, Vec2 mouseStart, Vec2 mouseEnd, byte dim1, byte dim2) {
		Vec2 min = new Vec2(activityStart).minimize(mouseEnd);
		Vec2 max = new Vec2(activityStart).maximize(mouseEnd);
		return viewportSelectionHandler.selectRegion(min, max, coordinateSystem);
	}

	@Override
	public void render(Graphics2D graphics, CoordinateSystem coordinateSystem) {
		if (mouseEnd == null) {
			return;
		}
		if ((currentDim1 == coordinateSystem.getPortFirstXYZ()) && (currentDim2 == coordinateSystem.getPortSecondXYZ())) {
			double minX = Math.min(coordinateSystem.viewX(activityStart.x), coordinateSystem.viewX(mouseEnd.x));
			double minY = Math.min(coordinateSystem.viewY(activityStart.y), coordinateSystem.viewY(mouseEnd.y));
			double maxX = Math.max(coordinateSystem.viewX(activityStart.x), coordinateSystem.viewX(mouseEnd.x));
			double maxY = Math.max(coordinateSystem.viewY(activityStart.y), coordinateSystem.viewY(mouseEnd.y));
			graphics.setColor(ProgramGlobals.getPrefs().getSelectColor());
			graphics.drawRect((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
		}
	}
}