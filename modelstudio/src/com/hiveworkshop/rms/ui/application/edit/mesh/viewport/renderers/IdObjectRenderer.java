package com.hiveworkshop.rms.ui.application.edit.mesh.viewport.renderers;

import com.hiveworkshop.rms.editor.model.*;
import com.hiveworkshop.rms.editor.model.visitor.IdObjectVisitor;
import com.hiveworkshop.rms.parsers.mdlx.MdlxCollisionShape;
import com.hiveworkshop.rms.ui.application.edit.mesh.viewport.NodeIconPalette;
import com.hiveworkshop.rms.ui.application.edit.mesh.viewport.axes.CoordinateSystem;
import com.hiveworkshop.rms.util.Vec3;

import java.awt.*;
import java.util.List;

public final class IdObjectRenderer implements IdObjectVisitor {
	private CoordinateSystem coordinateSystem;
	private Graphics2D graphics;
	private final int vertexSize;
	private final Color lightColor;
	private final Color pivotPointColor;
	private final NodeIconPalette nodeIconPalette;

	public IdObjectRenderer(Color lightColor, Color pivotPointColor, int vertexSize, NodeIconPalette nodeIconPalette) {
		this.lightColor = lightColor;
		this.pivotPointColor = pivotPointColor;
		this.vertexSize = vertexSize;
		this.nodeIconPalette = nodeIconPalette;
	}

	public static void drawNodeImage(Graphics2D graphics, byte xDimension, byte yDimension, CoordinateSystem coordinateSystem, IdObject attachment, Image nodeImage) {
		int xCoord = (int) coordinateSystem.viewX(attachment.getPivotPoint().getCoord(xDimension));
		int yCoord = (int) coordinateSystem.viewY(attachment.getPivotPoint().getCoord(yDimension));
		graphics.drawImage(nodeImage, xCoord - (nodeImage.getWidth(null) / 2), yCoord - (nodeImage.getHeight(null) / 2), nodeImage.getWidth(null), nodeImage.getHeight(null), null);
	}

	public static void drawCollisionShape(Graphics2D graphics, Color color, CoordinateSystem coordinateSystem, byte xDimension, byte yDimension, int vertexSize, CollisionShape collisionShape, Image collisionImage) {
		Vec3 pivotPoint = collisionShape.getPivotPoint();
		List<Vec3> vertices = collisionShape.getVertices();
		graphics.setColor(color);

		int xCoord = (int) coordinateSystem.viewX(pivotPoint.getCoord(xDimension));
		int yCoord = (int) coordinateSystem.viewY(pivotPoint.getCoord(yDimension));

		if (collisionShape.getType() == MdlxCollisionShape.Type.BOX) {
			if (vertices.size() > 1) {
				Vec3 vertex = vertices.get(0);
				Vec3 vertex2 = vertices.get(1);

				int firstXCoord = (int) coordinateSystem.viewX(vertex2.getCoord(xDimension));
				int firstYCoord = (int) coordinateSystem.viewY(vertex2.getCoord(yDimension));
				int secondXCoord = (int) coordinateSystem.viewX(vertex.getCoord(xDimension));
				int secondYCoord = (int) coordinateSystem.viewY(vertex.getCoord(yDimension));

				int minXCoord = Math.min(firstXCoord, secondXCoord);
				int minYCoord = Math.min(firstYCoord, secondYCoord);
				int maxXCoord = Math.max(firstXCoord, secondXCoord);
				int maxYCoord = Math.max(firstYCoord, secondYCoord);

				graphics.drawRoundRect(minXCoord, minYCoord, maxXCoord - minXCoord, maxYCoord - minYCoord, vertexSize, vertexSize);
			} else {
				drawNodeImage(graphics, xDimension, yDimension, coordinateSystem, collisionShape, collisionImage);
			}
		} else {
			if (collisionShape.getExtents() != null) {
				double zoom = CoordinateSystem.Util.getZoom(coordinateSystem);
				double boundsRadius = collisionShape.getExtents().getBoundsRadius() * zoom;
				graphics.drawOval((int) (xCoord - boundsRadius), (int) (yCoord - boundsRadius), (int) (boundsRadius * 2), (int) (boundsRadius * 2));
			} else {
				drawNodeImage(graphics, xDimension, yDimension, coordinateSystem, collisionShape, collisionImage);
			}
		}
	}

	public IdObjectRenderer reset(CoordinateSystem coordinateSystem, Graphics2D graphics) {
		this.coordinateSystem = coordinateSystem;
		this.graphics = graphics;
		return this;
	}

	private void drawCrosshair(Bone object) {
		int xCoord = (int) coordinateSystem.viewX(object.getPivotPoint().getCoord(coordinateSystem.getPortFirstXYZ()));
		int yCoord = (int) coordinateSystem.viewY(object.getPivotPoint().getCoord(coordinateSystem.getPortSecondXYZ()));
		graphics.drawOval(xCoord - vertexSize, yCoord - vertexSize, vertexSize * 2, vertexSize * 2);
		graphics.drawLine(xCoord - (int) (vertexSize * 1.5f), yCoord, xCoord + (int) (vertexSize * 1.5f), yCoord);
		graphics.drawLine(xCoord, yCoord - (int) (vertexSize * 1.5f), xCoord, yCoord + (int) (vertexSize * 1.5f));
	}

	private void drawNodeImage(IdObject object, Image nodeImage) {
		drawNodeImage(graphics, coordinateSystem.getPortFirstXYZ(), coordinateSystem.getPortSecondXYZ(), coordinateSystem, object, nodeImage);
	}

	@Override
	public void visitIdObject(IdObject object) {
		if (object instanceof Helper) {
			graphics.setColor(pivotPointColor.darker());
			drawCrosshair((Bone) object);
		} else if (object instanceof Bone) {
			graphics.setColor(pivotPointColor);
			drawCrosshair((Bone) object);
		} else if (object instanceof Light) {
			graphics.setColor(lightColor);
			drawNodeImage(object, nodeIconPalette.getObjectImage(object));
			light((Light) object);
		} else if (object instanceof CollisionShape) {
			collisionShape((CollisionShape) object);
		} else {
			drawNodeImage(object, nodeIconPalette.getObjectImage(object));
		}
	}

	public void light(Light object) {
		byte xDimension = coordinateSystem.getPortFirstXYZ();
		byte yDimension = coordinateSystem.getPortSecondXYZ();

		int xCoord2 = (int) coordinateSystem.viewX(object.getPivotPoint().getCoord(xDimension));
		int yCoord2 = (int) coordinateSystem.viewY(object.getPivotPoint().getCoord(yDimension));

		double zoom = CoordinateSystem.Util.getZoom(coordinateSystem);

		int attenuationStart = (int) (object.getAttenuationStart() * zoom);
		if (attenuationStart > 0) {
			graphics.drawOval(xCoord2 - attenuationStart, yCoord2 - attenuationStart, attenuationStart * 2, attenuationStart * 2);
		}
		int attenuationEnd = (int) (object.getAttenuationEnd() * zoom);
		if (attenuationEnd > 0) {
			graphics.drawOval(xCoord2 - attenuationEnd, yCoord2 - attenuationEnd, attenuationEnd * 2, attenuationEnd * 2);
		}
	}


	public void collisionShape(CollisionShape object) {
		drawCollisionShape(graphics, pivotPointColor, coordinateSystem, coordinateSystem.getPortFirstXYZ(), coordinateSystem.getPortSecondXYZ(), vertexSize, object, nodeIconPalette.getCollisionImage());
	}

	@Override
	public void camera(Camera camera) {
		graphics.setColor(Color.GREEN.darker());
		Graphics2D g2 = ((Graphics2D) graphics.create());
		Vec3 ver = camera.getPosition();
		Vec3 targ = camera.getTargetPosition();
		// boolean verSel = selection.contains(ver);
		// boolean tarSel = selection.contains(targ);
		Point start = new Point(
				(int) Math.round(coordinateSystem.viewX(ver.getCoord(coordinateSystem.getPortFirstXYZ()))),
				(int) Math.round(coordinateSystem.viewY(ver.getCoord(coordinateSystem.getPortSecondXYZ()))));
		Point end = new Point(
				(int) Math.round(coordinateSystem.viewX(targ.getCoord(coordinateSystem.getPortFirstXYZ()))),
				(int) Math.round(coordinateSystem.viewY(targ.getCoord(coordinateSystem.getPortSecondXYZ()))));
		// if (dispCameraNames) {
		// boolean changedCol = false;
		//
		// if (verSel) { g2.setColor(Color.orange.darker()); changedCol = true; }
		// g2.drawString(cam.getName(), (int) Math.round(vp.convertX(ver.getCoord(vp.getPortFirstXYZ()))),
		// (int) Math.round(vp.convertY(ver.getCoord(vp.getPortSecondXYZ()))));
		// if (tarSel) { g2.setColor(Color.orange.darker()); changedCol = true;
		// } else if (verSel) {g2.setColor(Color.green.darker());changedCol = false;}
		// g2.drawString(cam.getName() + "_target",(int) Math.round(vp.convertX(targ.getCoord(vp.getPortFirstXYZ()))),
		// (int) Math.round(vp.convertY(targ.getCoord(vp.getPortSecondXYZ()))));
		// if (changedCol) { g2.setColor(Color.green.darker());}}

		g2.translate(end.x, end.y);
		g2.rotate(-((Math.PI / 2) + Math.atan2(end.x - start.x, end.y - start.y)));
		double zoom = CoordinateSystem.Util.getZoom(coordinateSystem);
		int size = (int) (20 * zoom);
		double dist = start.distance(end);

		// if (verSel) {
		// g2.setColor(Color.orange.darker());
		// }
		// Cam
		g2.fillRect((int) dist - vertexSize, 0 - vertexSize, 1 + (vertexSize * 2), 1 + (vertexSize * 2));
		g2.drawRect((int) dist - size, -size, size * 2, size * 2);

		// if (tarSel) {
		// g2.setColor(Color.orange.darker());
		// } else if (verSel) {
		// g2.setColor(Color.green.darker());
		// }
		// Target
		g2.fillRect(0 - vertexSize, 0 - vertexSize, 1 + (vertexSize * 2), 1 + (vertexSize * 2));
		g2.drawLine(0, 0, size, size);// (int)Math.round(vp.convertX(targ.getCoord(vp.getPortFirstXYZ())+5)),
										// (int)Math.round(vp.convertY(targ.getCoord(vp.getPortSecondXYZ())+5)));
		g2.drawLine(0, 0, size, -size);// (int)Math.round(vp.convertX(targ.getCoord(vp.getPortFirstXYZ())-5)),
										// (int)Math.round(vp.convertY(targ.getCoord(vp.getPortSecondXYZ())-5)));

		// if (!verSel && tarSel) {
		// g2.setColor(Color.green.darker());
		// }
		g2.drawLine(0, 0, (int) dist, 0);
	}
}