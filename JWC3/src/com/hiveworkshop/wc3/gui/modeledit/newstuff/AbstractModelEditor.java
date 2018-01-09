package com.hiveworkshop.wc3.gui.modeledit.newstuff;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.etheller.collections.HashMap;
import com.etheller.collections.ListView;
import com.etheller.collections.Map;
import com.etheller.util.CollectionUtils;
import com.hiveworkshop.wc3.gui.modeledit.CoordinateSystem;
import com.hiveworkshop.wc3.gui.modeledit.UndoAction;
import com.hiveworkshop.wc3.gui.modeledit.actions.DeleteAction;
import com.hiveworkshop.wc3.gui.modeledit.actions.ExtrudeAction;
import com.hiveworkshop.wc3.gui.modeledit.actions.RecalculateNormalsAction;
import com.hiveworkshop.wc3.gui.modeledit.actions.SnapAction;
import com.hiveworkshop.wc3.gui.modeledit.actions.SnapNormalsAction;
import com.hiveworkshop.wc3.gui.modeledit.actions.SpecialDeleteAction;
import com.hiveworkshop.wc3.gui.modeledit.actions.newsys.ModelStructureChangeListener;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.editor.MoveAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.editor.RotateAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.selection.AddSelectionAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.selection.MakeEditableAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.selection.RemoveSelectionAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.selection.SetSelectionAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.tools.CloneAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.tools.FlipFacesAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.tools.FlipNormalsAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.tools.MirrorModelAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.tools.SetMatrixAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.actions.util.CompoundAction;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.listener.ClonedNodeNamePicker;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.listener.EditabilityToggleHandler;
import com.hiveworkshop.wc3.gui.modeledit.selection.SelectableComponent;
import com.hiveworkshop.wc3.gui.modeledit.selection.SelectionManager;
import com.hiveworkshop.wc3.gui.modeledit.selection.VertexSelectionHelper;
import com.hiveworkshop.wc3.mdl.Bone;
import com.hiveworkshop.wc3.mdl.Geoset;
import com.hiveworkshop.wc3.mdl.GeosetVertex;
import com.hiveworkshop.wc3.mdl.IdObject;
import com.hiveworkshop.wc3.mdl.Matrix;
import com.hiveworkshop.wc3.mdl.Normal;
import com.hiveworkshop.wc3.mdl.Triangle;
import com.hiveworkshop.wc3.mdl.Vertex;
import com.hiveworkshop.wc3.mdl.v2.ModelView;

public abstract class AbstractModelEditor<T> implements ModelEditor {
	protected final SelectionManager<T> selectionManager;
	protected final ModelView model;
	protected final VertexSelectionHelper vertexSelectionHelper;

	public AbstractModelEditor(final SelectionManager<T> selectionManager, final ModelView model) {
		this.selectionManager = selectionManager;
		this.model = model;
		this.vertexSelectionHelper = new VertexSelectionHelper() {
			@Override
			public void selectVertices(final Collection<Vertex> vertices) {
				selectByVertices(vertices);
			}
		};
	}

	@Override
	public UndoAction translate(final double x, final double y, final double z) {
		final Vertex delta = new Vertex(x, y, z);
		final MoveAction moveAction = new MoveAction(this, delta);
		moveAction.redo();
		return moveAction;
	}

	@Override
	public UndoAction setPosition(final Vertex center, final double x, final double y, final double z) {
		final Vertex delta = new Vertex(x - center.x, y - center.y, z - center.z);
		final MoveAction moveAction = new MoveAction(this, delta);
		moveAction.redo();
		return moveAction;
	}

	@Override
	public UndoAction rotate(final Vertex center, final double rotateX, final double rotateY, final double rotateZ) {

		final CompoundAction compoundAction = new CompoundAction("rotate",
				ListView.Util.of(new RotateAction(this, center, rotateX, (byte) 0, (byte) 2),
						new RotateAction(this, center, rotateY, (byte) 1, (byte) 0),
						new RotateAction(this, center, rotateZ, (byte) 1, (byte) 2)));
		compoundAction.redo();
		return compoundAction;
	}

	@Override
	public UndoAction setMatrix(final Collection<Bone> bones) {
		final Matrix mx = new Matrix();
		mx.setBones(new ArrayList<Bone>());
		for (final Bone bone : bones) {
			mx.add(bone);
		}
		final Map<GeosetVertex, List<Bone>> vertexToOldBoneReferences = new HashMap<>();
		for (final Vertex vert : selectionManager.getSelectedVertices()) {
			if (vert instanceof GeosetVertex) {
				final GeosetVertex gv = (GeosetVertex) vert;
				vertexToOldBoneReferences.put(gv, new ArrayList<>(gv.getBoneAttachments()));
				gv.clearBoneAttachments();
				gv.addBoneAttachments(mx.getBones());
			}
		}
		return new SetMatrixAction(vertexToOldBoneReferences, bones);
	}

	@Override
	public UndoAction snapNormals() {
		final ArrayList<Vertex> oldLocations = new ArrayList<>();
		final ArrayList<Vertex> selectedNormals = new ArrayList<>();
		final Normal snapped = new Normal(0, 0, 1);
		for (final Vertex vertex : selectionManager.getSelectedVertices()) {
			if (vertex instanceof GeosetVertex) {
				final GeosetVertex gv = (GeosetVertex) vertex;
				if (gv.getNormal() != null) {
					oldLocations.add(new Vertex(gv.getNormal()));
					selectedNormals.add(gv.getNormal());
				} // else no normal to snap!!!
			}
		}
		final SnapNormalsAction temp = new SnapNormalsAction(selectedNormals, oldLocations, snapped);
		temp.redo();// a handy way to do the snapping!
		return temp;
	}

	@Override
	public UndoAction recalcNormals() {
		final ArrayList<Vertex> oldLocations = new ArrayList<>();
		final ArrayList<GeosetVertex> selectedVertices = new ArrayList<>();
		final Normal snapped = new Normal(0, 0, 1);
		for (final Vertex vertex : selectionManager.getSelectedVertices()) {
			if (vertex instanceof GeosetVertex) {
				final GeosetVertex gv = (GeosetVertex) vertex;
				if (gv.getNormal() != null) {
					oldLocations.add(new Vertex(gv.getNormal()));
					selectedVertices.add(gv);
				} // else no normal to snap!!!
			}
		}
		final RecalculateNormalsAction temp = new RecalculateNormalsAction(selectedVertices, oldLocations, snapped);
		temp.redo();// a handy way to do the snapping!
		return temp;
	}

	@Override
	public UndoAction deleteSelectedComponents(final ModelStructureChangeListener modelStructureChangeListener) {
		// TODO this code is RIPPED FROM MDLDispaly and is not good for general
		// cases
		// TODO this code operates directly on MODEL
		final ArrayList<Geoset> remGeosets = new ArrayList<>();// model.getGeosets()
		final ArrayList<Triangle> deletedTris = new ArrayList<>();
		final Collection<? extends Vertex> selection = new ArrayList<>(selectionManager.getSelectedVertices());
		for (final Vertex vertex : selection) {
			if (vertex.getClass() == GeosetVertex.class) {
				final GeosetVertex gv = (GeosetVertex) vertex;
				for (final Triangle t : gv.getTriangles()) {
					t.getGeoset().removeTriangle(t);
					if (!deletedTris.contains(t)) {
						deletedTris.add(t);
					}
				}
				gv.getGeoset().remove(gv);
			}
		}
		for (final Triangle t : deletedTris) {
			for (final GeosetVertex vertex : t.getAll()) {
				vertex.getTriangles().remove(t);
			}
		}
		for (int i = model.getModel().getGeosets().size() - 1; i >= 0; i--) {
			if (model.getModel().getGeosets().get(i).isEmpty()) {
				final Geoset g = model.getModel().getGeoset(i);
				remGeosets.add(g);
				model.getModel().remove(g);
				if (g.getGeosetAnim() != null) {
					model.getModel().remove(g.getGeosetAnim());
				}
			}
		}
		selectByVertices(new ArrayList<Vertex>());
		if (remGeosets.size() <= 0) {
			final DeleteAction temp = new DeleteAction(selection, deletedTris, vertexSelectionHelper);
			return temp;
		} else {
			final SpecialDeleteAction temp = new SpecialDeleteAction(selection, deletedTris, vertexSelectionHelper,
					remGeosets, model.getModel(), modelStructureChangeListener);
			modelStructureChangeListener.geosetsRemoved(remGeosets);
			return temp;
		}
	}

	@Override
	public UndoAction mirror(final byte dim, final boolean flipModel) {
		final MirrorModelAction mirror = new MirrorModelAction(selectionManager.getSelectedVertices(),
				CollectionUtils.toJava(model.getEditableIdObjects()), dim);
		// super weird passing of currently editable id Objects, works because
		// mirror action checks selected vertices against pivot points from this
		// list
		mirror.redo();
		if (flipModel) {
			final UndoAction flipFacesAction = flipSelectedFaces();
			return new CompoundAction(mirror.actionName(), ListView.Util.of(mirror, flipFacesAction));
		}
		return mirror;
	}

	@Override
	public UndoAction flipSelectedFaces() {
		// TODO implement using faces for FaceModelEditor... probably?
		final FlipFacesAction flipFacesAction = new FlipFacesAction(selectionManager.getSelectedVertices());
		flipFacesAction.redo();
		return flipFacesAction;
	}

	@Override
	public UndoAction flipSelectedNormals() {
		final FlipNormalsAction flipNormalsAction = new FlipNormalsAction(selectionManager.getSelectedVertices());
		flipNormalsAction.redo();
		return flipNormalsAction;
	}

	@Override
	public UndoAction snapSelectedNormals() {
		final Collection<? extends Vertex> selection = selectionManager.getSelectedVertices();
		final ArrayList<Vertex> oldLocations = new ArrayList<>();
		final ArrayList<Vertex> selectedNormals = new ArrayList<>();
		final Normal snapped = new Normal(0, 0, 1);
		for (final Vertex vertex : selection) {
			if (vertex instanceof GeosetVertex) {
				final GeosetVertex gv = (GeosetVertex) vertex;
				if (gv.getNormal() != null) {
					oldLocations.add(new Vertex(gv.getNormal()));
					selectedNormals.add(gv.getNormal());
				} // else no normal to snap!!!
			}
		}
		final SnapNormalsAction temp = new SnapNormalsAction(selectedNormals, oldLocations, snapped);
		temp.redo();// a handy way to do the snapping!
		return temp;
	}

	@Override
	public UndoAction beginExtrudingSelection() {
		final List<Vertex> selection = new ArrayList<>(selectionManager.getSelectedVertices());
		final ArrayList<GeosetVertex> copies = new ArrayList<>();
		final ArrayList<Triangle> selTris = new ArrayList<>();
		for (int i = 0; i < selection.size(); i++) {
			final Vertex vert = selection.get(i);
			if (vert.getClass() == GeosetVertex.class) {
				final GeosetVertex gv = (GeosetVertex) vert;
				copies.add(new GeosetVertex(gv));

				for (int ti = 0; ti < gv.getTriangles().size(); ti++) {
					final Triangle temptr = gv.getTriangles().get(ti);
					if (!selTris.contains(temptr)) {
						selTris.add(temptr);
					}
				}
			} else {
				copies.add(null);
				System.out.println("GeosetVertex " + i + " was not found.");
			}
		}
		for (final Triangle tri : selTris) {
			if (!selection.contains(tri.get(0)) || !selection.contains(tri.get(1)) || !selection.contains(tri.get(2))) {
				for (int i = 0; i < 3; i++) {
					final GeosetVertex a = tri.get(i);
					if (selection.contains(a)) {
						final GeosetVertex b = copies.get(selection.indexOf(a));
						tri.set(i, b);
						a.getTriangles().remove(tri);
						if (a.getTriangles().contains(tri)) {
							System.out.println("It's a bloody war!");
						}
						b.getTriangles().add(tri);
					}
				}
			}
		}
		System.out.println(selection.size() + " verteces cloned into " + copies.size() + " more.");
		final ArrayList<Triangle> newTriangles = new ArrayList<>();
		for (int k = 0; k < selection.size(); k++) {
			final Vertex vert = selection.get(k);
			if (vert.getClass() == GeosetVertex.class) {
				final GeosetVertex gv = (GeosetVertex) vert;
				final ArrayList<Triangle> gvTriangles = new ArrayList<>();// gv.getTriangles());
				// WHY IS GV.TRIANGLES WRONG????
				for (final Triangle tri : gv.getGeoset().getTriangles()) {
					if (tri.contains(gv)) {
						boolean good = true;
						for (final Vertex vTemp : tri.getAll()) {
							if (!selection.contains(vTemp)) {
								good = false;
								break;
							}
						}
						if (good) {
							gvTriangles.add(tri);
						}
					}
				}
				for (final Triangle tri : gvTriangles) {
					for (final GeosetVertex copyVer : copies) {
						if (copyVer != null) {
							if (tri.containsRef(copyVer)) {
								System.out.println("holy brejeezers!");
							}
						}
					}
					for (int gvI = 0; gvI < tri.getAll().length; gvI++) {
						final GeosetVertex gvTemp = tri.get(gvI);
						if (!gvTemp.equalLocs(gv) && gvTemp.getGeoset() == gv.getGeoset()) {
							int ctCount = 0;
							Triangle temptr = null;
							boolean okay = false;
							for (final Triangle triTest : gvTriangles) {
								if (triTest.contains(gvTemp)) {
									ctCount++;
									temptr = triTest;
									if (temptr.containsRef(gvTemp) && temptr.containsRef(gv)) {
										okay = true;
									}
								}
							}
							if (okay && ctCount == 1 && selection.contains(gvTemp)) {
								final GeosetVertex gvCopy = copies.get(selection.indexOf(gv));
								final GeosetVertex gvTempCopy = copies.get(selection.indexOf(gvTemp));
								if (gvCopy == null) {
									System.out.println("Vertex (gvCopy) copy found as null!");
								}
								if (gvTempCopy == null) {
									System.out.println("Vertex (gvTempCopy) copy found as null!");
								}
								Triangle newFace = new Triangle(null, null, null, gv.getGeoset());

								final int indexA = temptr.indexOf(gv);
								final int indexB = temptr.indexOf(gvTemp);
								int indexC = -1;

								for (int i = 0; i < 3 && indexC == -1; i++) {
									if (i != indexA && i != indexB) {
										indexC = i;
									}
								}

								System.out.println(" Indeces: " + indexA + "," + indexB + "," + indexC);

								newFace.set(indexA, gv);
								newFace.set(indexB, gvTemp);
								newFace.set(indexC, gvCopy);
								// Make sure it's included later
								// gvTemp.triangles.add(newFace);
								// gv.getTriangles().add(newFace);
								// gvCopy.triangles.add(newFace);
								// gv.getGeoset().addTriangle(newFace);
								boolean bad = false;
								for (final Triangle t : newTriangles) {
									// if( t.equals(newFace) )
									// {
									// bad = true;
									// break;
									// }
									if (t.contains(gv) && t.contains(gvTemp)) {
										bad = true;
										break;
									}
								}
								if (!bad) {
									newTriangles.add(newFace);

									System.out.println("New Face: ");
									System.out.println(newFace.get(0));
									System.out.println(newFace.get(1));
									System.out.println(newFace.get(2));

									newFace = new Triangle(null, null, null, gv.getGeoset());

									newFace.set(indexA, gvCopy);
									newFace.set(indexB, gvTemp);
									newFace.set(indexC, gvTempCopy);
									// Make sure it's included later
									newTriangles.add(newFace);

									System.out.println("New Alternate Face: ");
									System.out.println(newFace.get(0));
									System.out.println(newFace.get(1));
									System.out.println(newFace.get(2));

								}
							}
						}
					}
				}
			}
		}

		for (final Triangle t : newTriangles) {
			for (final GeosetVertex gv : t.getAll()) {
				if (!gv.getTriangles().contains(t)) {
					gv.getTriangles().add(t);
				}
				if (!gv.getGeoset().contains(t)) {
					gv.getGeoset().addTriangle(t);
				}
			}
		}
		for (final GeosetVertex cgv : copies) {
			if (cgv != null) {
				boolean inGeoset = false;
				for (final Triangle t : cgv.getGeoset().getTriangles()) {
					if (t.containsRef(cgv)) {
						inGeoset = true;
						break;
					}
				}
				if (inGeoset) {
					cgv.getGeoset().addVertex(cgv);
				}
			}
		}
		int probs = 0;
		for (int k = 0; k < selection.size(); k++) {
			final Vertex vert = selection.get(k);
			if (vert.getClass() == GeosetVertex.class) {
				final GeosetVertex gv = (GeosetVertex) vert;
				for (final Triangle t : gv.getTriangles()) {
					System.out.println("SHOULD be one: " + Collections.frequency(gv.getTriangles(), t));
					if (!t.containsRef(gv)) {
						probs++;
					}
				}
			}
		}
		System.out.println("Extrude finished with " + probs + " inexplicable errors.");
		final ExtrudeAction tempe = new ExtrudeAction(); // TODO better code
		tempe.storeSelection(selection);
		tempe.setType(true);
		tempe.storeBaseMovement(new Vertex(0, 0, 0));
		tempe.setAddedTriangles(newTriangles);
		tempe.setAddedVerts(copies);
		return tempe;
	}

	@Override
	public UndoAction beginExtendingSelection() {
		final List<Vertex> selection = new ArrayList<>(selectionManager.getSelectedVertices());
		final ArrayList<GeosetVertex> copies = new ArrayList<>();
		final ArrayList<Triangle> selTris = new ArrayList<>();
		final ArrayList<Triangle> newTriangles = new ArrayList<>();

		final ArrayList<Triangle> edges = new ArrayList<>();
		final ArrayList<Triangle> brokenFaces = new ArrayList<>();

		for (int i = 0; i < selection.size(); i++) {
			final Vertex vert = selection.get(i);
			if (vert.getClass() == GeosetVertex.class) {
				final GeosetVertex gv = (GeosetVertex) vert;
				// copies.add(new GeosetVertex(gv));

				// selTris.addAll(gv.getTriangles());
				for (int ti = 0; ti < gv.getTriangles().size(); ti++) {
					final Triangle temptr = gv.getTriangles().get(ti);
					if (!selTris.contains(temptr)) {
						selTris.add(temptr);
					}
				}
			} else {
				// copies.add(null);
				System.out.println("GeosetVertex " + i + " was not found.");
			}
		}
		System.out.println(selection.size() + " verteces cloned into " + copies.size() + " more.");
		final ArrayList<GeosetVertex> copiedGroup = new ArrayList<>();
		for (final Triangle tri : selTris) {
			if (!selection.contains(tri.get(0)) || !selection.contains(tri.get(1)) || !selection.contains(tri.get(2))) {
				int selVerts = 0;
				GeosetVertex gv = null;
				GeosetVertex gvTemp = null;
				GeosetVertex gvCopy = null;// copies.get(selection.indexOf(gv));
				GeosetVertex gvTempCopy = null;// copies.get(selection.indexOf(gvTemp));
				for (int i = 0; i < 3; i++) {
					final GeosetVertex a = tri.get(i);
					if (selection.contains(a)) {
						selVerts++;
						final GeosetVertex b = new GeosetVertex(a);
						copies.add(b);
						copiedGroup.add(a);
						tri.set(i, b);
						a.getTriangles().remove(tri);
						b.getTriangles().add(tri);
						if (gv == null) {
							gv = a;
							gvCopy = b;
						} else if (gvTemp == null) {
							gvTemp = a;
							gvTempCopy = b;
						}
					}
				}
				if (selVerts == 2) {
					if (gvCopy == null) {
						System.out.println("Vertex (gvCopy) copy found as null!");
					}
					if (gvTempCopy == null) {
						System.out.println("Vertex (gvTempCopy) copy found as null!");
					}
					Triangle newFace = new Triangle(null, null, null, gv.getGeoset());

					final int indexA = tri.indexOf(gvCopy);
					final int indexB = tri.indexOf(gvTempCopy);
					int indexC = -1;

					for (int i = 0; i < 3 && indexC == -1; i++) {
						if (i != indexA && i != indexB) {
							indexC = i;
						}
					}

					System.out.println(" Indeces: " + indexA + "," + indexB + "," + indexC);

					newFace.set(indexA, gv);
					newFace.set(indexB, gvTemp);
					newFace.set(indexC, gvCopy);
					// Make sure it's included later
					gvTemp.getTriangles().add(newFace);
					gv.getTriangles().add(newFace);
					gvCopy.getTriangles().add(newFace);
					gv.getGeoset().addTriangle(newFace);
					newTriangles.add(newFace);

					System.out.println("New Face: ");
					System.out.println(newFace.get(0));
					System.out.println(newFace.get(1));
					System.out.println(newFace.get(2));

					newFace = new Triangle(null, null, null, gv.getGeoset());

					newFace.set(indexA, gvCopy);
					newFace.set(indexB, gvTemp);
					newFace.set(indexC, gvTempCopy);
					// Make sure it's included later
					gvCopy.getTriangles().add(newFace);
					gvTemp.getTriangles().add(newFace);
					gvTempCopy.getTriangles().add(newFace);
					gv.getGeoset().addTriangle(newFace);
					newTriangles.add(newFace);

					System.out.println("New Alternate Face: ");
					System.out.println(newFace.get(0));
					System.out.println(newFace.get(1));
					System.out.println(newFace.get(2));
				}
			}
		}

		for (final GeosetVertex cgv : copies) {
			if (cgv != null) {
				cgv.getGeoset().addVertex(cgv);
			}
		}

		final ExtrudeAction tempe = new ExtrudeAction();
		tempe.storeSelection(selection);
		tempe.setType(false);
		tempe.storeBaseMovement(new Vertex(0, 0, 0));
		tempe.setAddedTriangles(newTriangles);
		tempe.setAddedVerts(copies);
		tempe.setCopiedGroup(copiedGroup);
		return tempe;
	}

	@Override
	public UndoAction snapSelectedVertices() {
		final Collection<? extends Vertex> selection = selectionManager.getSelectedVertices();
		final ArrayList<Vertex> oldLocations = new ArrayList<>();
		final Vertex cog = Vertex.centerOfGroup(selection);
		for (final Vertex vertex : selection) {
			oldLocations.add(new Vertex(vertex));
		}
		final SnapAction temp = new SnapAction(selection, oldLocations, cog);
		temp.redo();// a handy way to do the snapping!
		return temp;
	}

	@Override
	public UndoAction cloneSelectedComponents(final ModelStructureChangeListener modelStructureChangeListener,
			final ClonedNodeNamePicker clonedNodeNamePicker) {
		final List<Vertex> source = new ArrayList<>(selectionManager.getSelectedVertices());
		final ArrayList<Triangle> selTris = new ArrayList<>();
		final ArrayList<IdObject> selBones = new ArrayList<>();
		final ArrayList<IdObject> newBones = new ArrayList<>();
		final ArrayList<GeosetVertex> newVertices = new ArrayList<>();
		final ArrayList<Triangle> newTriangles = new ArrayList<>();
		for (int i = 0; i < source.size(); i++) {
			final Vertex vert = source.get(i);
			if (vert.getClass() == GeosetVertex.class) {
				final GeosetVertex gv = (GeosetVertex) vert;
				newVertices.add(new GeosetVertex(gv));
			} else {
				newVertices.add(null);
			}
		}
		for (final IdObject b : model.getEditableIdObjects()) {
			if (source.contains(b.getPivotPoint()) && !selBones.contains(b)) {
				selBones.add(b);
				newBones.add(b.copy());
			}
		}
		if (newBones.size() > 0) {
			final java.util.Map<IdObject, String> nodeToNamePicked = clonedNodeNamePicker.pickNames(newBones);
			if (nodeToNamePicked == null) {
				throw new RuntimeException(
						"user does not wish to continue so we put in an error to interrupt clone so model is OK");
			}
			for (final IdObject node : nodeToNamePicked.keySet()) {
				node.setName(nodeToNamePicked.get(node));
			}
		}
		for (int k = 0; k < source.size(); k++) {
			final Vertex vert = source.get(k);
			if (vert.getClass() == GeosetVertex.class) {
				final GeosetVertex gv = (GeosetVertex) vert;
				final ArrayList<Triangle> gvTriangles = new ArrayList<>();// gv.getTriangles());
				for (final Triangle tri : gv.getGeoset().getTriangles()) {
					if (tri.contains(gv)) {
						boolean good = true;
						for (final Vertex vTemp : tri.getAll()) {
							if (!source.contains(vTemp)) {
								good = false;
								break;
							}
						}
						if (good) {
							gvTriangles.add(tri);
							if (!selTris.contains(tri)) {
								selTris.add(tri);
							}
						}
					}
				}
			}
		}
		for (final Triangle tri : selTris) {
			final GeosetVertex a = newVertices.get(source.indexOf(tri.get(0)));
			final GeosetVertex b = newVertices.get(source.indexOf(tri.get(1)));
			final GeosetVertex c = newVertices.get(source.indexOf(tri.get(2)));
			final Triangle newTriangle = new Triangle(a, b, c, a.getGeoset());
			newTriangles.add(newTriangle);
			a.getTriangles().add(newTriangle);
			b.getTriangles().add(newTriangle);
			c.getTriangles().add(newTriangle);
		}
		final Set<Vertex> newSelection = new HashSet<>();
		for (final Vertex ver : newVertices) {
			if (ver != null) {
				newSelection.add(ver);
				if (ver.getClass() == GeosetVertex.class) {
					final GeosetVertex gv = (GeosetVertex) ver;
					for (int i = 0; i < gv.getBones().size(); i++) {
						final Bone b = gv.getBones().get(i);
						if (selBones.contains(b)) {
							gv.getBones().set(i, (Bone) newBones.get(selBones.indexOf(b)));
						}
					}
				}
			}
		}
		for (final IdObject b : newBones) {
			newSelection.add(b.getPivotPoint());
			if (selBones.contains(b.getParent())) {
				b.setParent(newBones.get(selBones.indexOf(b.getParent())));
			}
		}
		final List<GeosetVertex> newVerticesWithoutNulls = new ArrayList<>();
		for (final GeosetVertex vertex : newVertices) {
			if (vertex != null) {
				newVerticesWithoutNulls.add(vertex);
			}
		}
		// TODO cameras
		final CloneAction cloneAction = new CloneAction(model, source, modelStructureChangeListener,
				vertexSelectionHelper, selBones, newVerticesWithoutNulls, newTriangles, newBones, newSelection);
		cloneAction.redo();
		return cloneAction;
	}

	@Override
	public void rawTranslate(final double x, final double y, final double z) {
		for (final Vertex vertex : selectionManager.getSelectedVertices()) {
			vertex.translate(x, y, z);
		}
	}

	@Override
	public void rawScale(final double centerX, final double centerY, final double centerZ, final double scaleX,
			final double scaleY, final double scaleZ) {
		for (final Vertex vertex : selectionManager.getSelectedVertices()) {
			vertex.scale(centerX, centerY, centerZ, scaleX, scaleY, scaleZ);
		}
	}

	@Override
	public void rawRotate2d(final double centerX, final double centerY, final double centerZ, final double radians,
			final byte firstXYZ, final byte secondXYZ) {
		for (final Vertex vertex : selectionManager.getSelectedVertices()) {
			vertex.rotate(centerX, centerY, centerZ, radians, firstXYZ, secondXYZ);
		}
	}

	@Override
	public void rawRotate3d(final Vertex center, final Vertex axis, final double radians) {
		for (final Vertex vertex : selectionManager.getSelectedVertices()) {
			Vertex.rotateVertex(center, axis, radians, vertex);
		}
	}

	@Override
	public final UndoAction setSelectedRegion(final Rectangle2D region, final CoordinateSystem coordinateSystem) {
		final List<T> newSelection = genericSelect(region, coordinateSystem);
		return setSelectionWithAction(newSelection);
	}

	@Override
	public final UndoAction removeSelectedRegion(final Rectangle2D region, final CoordinateSystem coordinateSystem) {
		final List<T> newSelection = genericSelect(region, coordinateSystem);
		return removeSelectionWithAction(newSelection);
	}

	@Override
	public final UndoAction addSelectedRegion(final Rectangle2D region, final CoordinateSystem coordinateSystem) {
		final List<T> newSelection = genericSelect(region, coordinateSystem);
		return addSelectionWithAction(newSelection);
	}

	protected final UndoAction setSelectionWithAction(final List<T> newSelection) {
		final Set<T> previousSelection = new HashSet<>(selectionManager.getSelection());
		selectionManager.setSelection(newSelection);
		return (new SetSelectionAction<>(newSelection, previousSelection, selectionManager, "select"));
	}

	protected final UndoAction removeSelectionWithAction(final List<T> newSelection) {
		final Set<T> previousSelection = new HashSet<>(selectionManager.getSelection());
		selectionManager.removeSelection(newSelection);
		return (new RemoveSelectionAction<>(previousSelection, newSelection, selectionManager));
	}

	protected final UndoAction addSelectionWithAction(final List<T> newSelection) {
		final Set<T> previousSelection = new HashSet<>(selectionManager.getSelection());
		selectionManager.addSelection(newSelection);
		return (new AddSelectionAction<>(previousSelection, newSelection, selectionManager));
	}

	protected abstract List<T> genericSelect(final Rectangle2D region, final CoordinateSystem coordinateSystem);

	@Override
	public UndoAction hideComponent(final ListView<? extends SelectableComponent> selectableComponent,
			final EditabilityToggleHandler editabilityToggleHandler, final Runnable refreshGUIRunnable) {
		final UndoAction hideComponentAction = buildHideComponentAction(selectableComponent, editabilityToggleHandler,
				refreshGUIRunnable);
		hideComponentAction.redo();
		return hideComponentAction;
	}

	protected abstract UndoAction buildHideComponentAction(ListView<? extends SelectableComponent> selectableComponents,
			EditabilityToggleHandler editabilityToggleHandler, final Runnable refreshGUIRunnable);

	@Override
	public UndoAction showComponent(final EditabilityToggleHandler editabilityToggleHandler) {
		editabilityToggleHandler.makeEditable();
		return new MakeEditableAction(editabilityToggleHandler);
	}

	@Override
	public Vertex getSelectionCenter() {
		return selectionManager.getCenter();
	}
}
