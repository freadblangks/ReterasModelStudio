package com.hiveworkshop.rms.ui.gui.modeledit.selection;

import com.hiveworkshop.rms.editor.model.GeosetVertex;
import com.hiveworkshop.rms.editor.model.Triangle;
import com.hiveworkshop.rms.editor.render3d.RenderNode;
import com.hiveworkshop.rms.ui.application.edit.mesh.viewport.axes.CoordSysUtils;
import com.hiveworkshop.rms.ui.application.edit.mesh.viewport.axes.CoordinateSystem;
import com.hiveworkshop.rms.util.Mat4;
import com.hiveworkshop.rms.util.Vec2;
import com.hiveworkshop.rms.util.Vec3;

public class HitTestStuff {
	public static boolean hitTest(Vec2 min, Vec2 max, Vec3 vec3, CoordinateSystem coordinateSystem, double vertexSize) {
		byte dim1 = coordinateSystem.getPortFirstXYZ();
		byte dim2 = coordinateSystem.getPortSecondXYZ();

		Vec2 minView = new Vec2(min).minimize(max);
		Vec2 maxView = new Vec2(max).maximize(min);

		Vec2 vertexV2 = vec3.getProjected(dim1, dim2);


		double vertSize = vertexSize / 2.0 / coordinateSystem.getZoom();
		return (vertexV2.distance(min) <= vertSize)
				|| (vertexV2.distance(max) <= vertSize)
				|| within(vertexV2, min, max);
	}

	public static boolean hitTest(Vec2 min, Vec2 max, Vec3 vec3, Mat4 viewPortMat, double vertexSize, double zoom) {
//
//		Vec3 minView = new Vec3(min).minimize(max);
//		Vec3 maxView = new Vec3(max).maximize(min);
//
		Vec3 viewPAdj = new Vec3(vec3).transform(viewPortMat);
		Vec2 vertexV2 = viewPAdj.getProjected((byte) 1, (byte) 2);

		double vertSize = vertexSize / 2.0 * zoom;
		return (vertexV2.distance(min) <= vertSize)
				|| (vertexV2.distance(max) <= vertSize)
				|| within(vertexV2, min, max);
	}

	public static boolean hitTest(Vec3 vec3, Vec2 point, CoordinateSystem coordinateSystem, double vertexSize) {
		Vec2 vertexV2 = CoordSysUtils.convertToViewVec2(coordinateSystem, vec3);
		double vertSize = vertexSize / 2.0 / coordinateSystem.getZoom();
		return vertexV2.distance(point) <= vertSize;
	}

	private static boolean within(Vec2 point, Vec2 min, Vec2 max) {
		boolean xIn = max.x >= point.x && point.x >= min.x;
		boolean yIn = max.y >= point.y && point.y >= min.y;
		return xIn && yIn;
	}

	private static boolean within(Vec3 point, Vec3 min, Vec3 max) {
		boolean xIn = max.x >= point.x && point.x >= min.x;
		boolean yIn = max.y >= point.y && point.y >= min.y;
		boolean zIn = max.z >= point.z && point.z >= min.z;
		return xIn && yIn && zIn;
	}

	public static boolean triHitTest(Triangle triangle, Vec2 min, Vec2 max, CoordinateSystem coordinateSystem) {
		byte dim1 = coordinateSystem.getPortFirstXYZ();
		byte dim2 = coordinateSystem.getPortSecondXYZ();

		GeosetVertex[] verts = triangle.getVerts();

		return within(verts[0].getProjected(dim1, dim2), min, max)
				|| within(verts[1].getProjected(dim1, dim2), min, max)
				|| within(verts[2].getProjected(dim1, dim2), min, max);
	}

	public static boolean triHitTest(Triangle triangle, Vec2 point, CoordinateSystem coordinateSystem) {
		byte dim1 = coordinateSystem.getPortFirstXYZ();
		byte dim2 = coordinateSystem.getPortSecondXYZ();

		Vec2[] triPoints = triangle.getProjectedVerts(dim1, dim2);

		return pointInTriangle(point, triPoints[0], triPoints[1], triPoints[2]);
	}

	private static boolean pointInTriangle (Vec2 point, Vec2 v1, Vec2 v2, Vec2 v3) {
		float d1 = (point.x - v2.x) * (v1.y - v2.y) - (v1.x - v2.x) * (point.y - v2.y);
		float d2 = (point.x - v3.x) * (v2.y - v3.y) - (v2.x - v3.x) * (point.y - v3.y);
		float d3 = (point.x - v1.x) * (v3.y - v1.y) - (v3.x - v1.x) * (point.y - v1.y);
//        float d1 = sign(point, v1, v2);
//        float d2 = sign(point, v2, v3);
//        float d3 = sign(point, v3, v1);

		boolean has_neg = (d1 < 0) || (d2 < 0) || (d3 < 0);
		boolean has_pos = (d1 > 0) || (d2 > 0) || (d3 > 0);

		return !(has_neg && has_pos);
	}

	public static boolean hitTest(Vec3 pivotPoint, Vec2 point, CoordinateSystem coordinateSystem, double vertexSize, Mat4 worldMatrix) {
		Vec3 pivotHeap = Vec3.getTransformed(pivotPoint, worldMatrix);
//		pivotHeap.transform(worldMatrix);
		Vec2 vertexV2 = CoordSysUtils.convertToViewVec2(coordinateSystem, pivotHeap);
		double vertSize = vertexSize / 2.0 / coordinateSystem.getZoom();
		return vertexV2.distance(point) <= vertSize;
	}

	public static boolean hitTest(Vec2 min, Vec2 max,
	                              CoordinateSystem coordinateSystem,
	                              double vertexSize,
	                              RenderNode renderNode) {
		Vec3 pivotHeap = renderNode.getPivot();

		byte dim1 = coordinateSystem.getPortFirstXYZ();
		byte dim2 = coordinateSystem.getPortSecondXYZ();

		Vec2 minView = new Vec2(min).minimize(max);
		Vec2 maxView = new Vec2(max).maximize(min);

		Vec2 vertexV2 = pivotHeap.getProjected(dim1, dim2);

		vertexV2.distance(max);
		double vertSize = vertexSize / 2.0 / coordinateSystem.getZoom();
		return vertexV2.distance(min) <= vertSize || vertexV2.distance(max) <= vertSize || within(vertexV2, min, max);
	}



	public static boolean triHitTest(Triangle triangle, Vec2 point, CoordinateSystem coordinateSystem, int uvLayerIndex) {
		GeosetVertex[] verts = triangle.getVerts();
		return pointInTriangle(point, verts[0].getTVertex(uvLayerIndex), verts[1].getTVertex(uvLayerIndex), verts[2].getTVertex(uvLayerIndex));
	}

	public static boolean triHitTest(Triangle triangle, Vec2 min, Vec2 max, CoordinateSystem coordinateSystem, int uvLayerIndex) {
		GeosetVertex[] verts = triangle.getVerts();
		return within(verts[0].getTVertex(uvLayerIndex), min, max)
				|| within(verts[1].getTVertex(uvLayerIndex), min, max)
				|| within(verts[2].getTVertex(uvLayerIndex), min, max);
	}

	public static boolean hitTest(Vec2 min, Vec2 max, Vec2 tVertex, CoordinateSystem coordinateSystem, double vertexSize) {
		double vSizeView = vertexSize / coordinateSystem.getZoom();
		return tVertex.distance(min) <= vSizeView
				|| tVertex.distance(max) <= vSizeView
				|| within(tVertex, min, max);
	}

	public static boolean hitTest(Vec2 vertex, Vec2 point, CoordinateSystem coordinateSystem, double vertexSize) {
		double vSizeView = vertexSize / 2.0 / coordinateSystem.getZoom();
		return vertex.distance(point) <= vSizeView;
	}
}
