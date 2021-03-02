package com.hiveworkshop.rms.ui.application.viewer.perspective;

import com.hiveworkshop.rms.editor.model.*;
import com.hiveworkshop.rms.editor.model.util.ModelUtils;
import com.hiveworkshop.rms.editor.render3d.*;
import com.hiveworkshop.rms.editor.wrapper.v2.ModelView;
import com.hiveworkshop.rms.filesystem.sources.DataSource;
import com.hiveworkshop.rms.parsers.blp.BLPHandler;
import com.hiveworkshop.rms.parsers.blp.GPUReadyTexture;
import com.hiveworkshop.rms.parsers.mdlx.MdlxLayer.FilterMode;
import com.hiveworkshop.rms.ui.application.edit.animation.TimeBoundProvider;
import com.hiveworkshop.rms.ui.application.viewer.AnimatedRenderEnvironment;
import com.hiveworkshop.rms.ui.application.viewer.ComPerspRenderEnv;
import com.hiveworkshop.rms.ui.preferences.ProgramPreferences;
import com.hiveworkshop.rms.ui.util.BetterAWTGLCanvas;
import com.hiveworkshop.rms.ui.util.ExceptionPopup;
import com.hiveworkshop.rms.util.Mat4;
import com.hiveworkshop.rms.util.Quat;
import com.hiveworkshop.rms.util.Vec3;
import com.hiveworkshop.rms.util.Vec4;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;

public class PerspectiveViewport extends BetterAWTGLCanvas implements RenderResourceAllocator {
	public static final boolean LOG_EXCEPTIONS = true;

	private static final int BYTES_PER_PIXEL = 4;
	int popupCount = 0;

	ModelView modelView;
	private RenderModel renderModel;

	Vec3 cameraPos = new Vec3(0, 0, 0);
	Quat inverseCameraRotationQuat = new Quat();
	Quat inverseCameraRotationYSpin = new Quat();
	Quat inverseCameraRotationZSpin = new Quat();

	double m_zoom = 1;
	Point cameraPanStartPoint;
	Point actStart;
	Timer clickTimer = new Timer(16, e -> clickTimerAction());

	boolean mouseInBounds = false;
	boolean enabled = false;

	boolean texLoaded = false;

	JCheckBox wireframe;
	HashMap<Bitmap, Integer> textureMap = new HashMap<>();
	Map<Geoset, List<Vec4>> normalListMap;
	Map<Geoset, List<Vec4>> vertListMap;

	Class<? extends Throwable> lastThrownErrorClass;
	private final ProgramPreferences programPreferences;

	private int animationTime;
	private boolean live = true;
	private boolean looping = true;
	private Animation animation;
	private long lastUpdateMillis = System.currentTimeMillis();
	private long lastExceptionTimeMillis = 0;

	private int levelOfDetail = -1;
	Point cameraSpinStartPoint;
	boolean wantReload = false;
	boolean wantReloadAll = false;
	boolean initialized = false;
	private float animSpeed = 1.0f;

	private float xangle;
	private float yangle;
	private float xRatio;
	private float yRatio;

	private float backgroundRed, backgroundBlue, backgroundGreen;
	Timer paintTimer;

	public PerspectiveViewport(final ModelView modelView, final ProgramPreferences programPreferences, final RenderModel renderModel) throws LWJGLException {
		super();
		this.programPreferences = programPreferences;

		normalListMap = new HashMap<>();
		vertListMap = new HashMap<>();

		this.renderModel = renderModel;
		this.renderModel.setSpawnParticles(true);
//		renderModel.setAllowInanimateParticles(true);

		// Dimension 1 and Dimension 2, these specify which dimensions to  display. the d bytes can thus be from 0 to 2, specifying either the X, Y, or Z  dimensions
		setBackground((programPreferences == null) || (programPreferences.getPerspectiveBackgroundColor() == null) ? new Color(80, 80, 80) : programPreferences.getPerspectiveBackgroundColor());
		setMinimumSize(new Dimension(200, 200));

		this.modelView = modelView;

		MouseAdapter mouseAdapter = getMouseAdapter();
		addMouseListener(mouseAdapter);
		addMouseWheelListener(mouseAdapter);

		if (programPreferences != null) {
			programPreferences.addChangeListener(() -> updateBackgroundColor(programPreferences));
		}

		loadBackgroundColors();
		paintTimer = new Timer(16, e -> {
			repaint();
			if (!isShowing()) {
				paintTimer.stop();
			}
		});
		paintTimer.start();
	}

	public static int loadTexture(final GPUReadyTexture texture, final Bitmap bitmap) {
		if (texture == null) {
			return -1;
		}
		final ByteBuffer buffer = texture.getBuffer();
		// You now have a ByteBuffer filled with the color data of each pixel.
		// Now just create a texture ID and bind it. Then you can load it using
		// whatever OpenGL method you want, for example:

		final int textureID = GL11.glGenTextures(); // Generate texture ID
		bindTexture(bitmap, textureID);

		// Setup texture scaling filtering
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

		// Send texel data to OpenGL
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, texture.getWidth(), texture.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

		// Return the texture ID so we can bind it later again
		return textureID;
	}

	private static void bindTexture(Bitmap tex, Integer texture) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, tex.isWrapWidth() ? GL11.GL_REPEAT : GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, tex.isWrapHeight() ? GL11.GL_REPEAT : GL12.GL_CLAMP_TO_EDGE);
	}

	protected void updateBackgroundColor(ProgramPreferences programPreferences) {
		setBackground(programPreferences.getPerspectiveBackgroundColor() == null ? new Color(80, 80, 80) : programPreferences.getPerspectiveBackgroundColor());
		loadBackgroundColors();
	}

	private void loadBackgroundColors() {
		backgroundRed = programPreferences.getPerspectiveBackgroundColor().getRed() / 255f;
		backgroundGreen = programPreferences.getPerspectiveBackgroundColor().getGreen() / 255f;
		backgroundBlue = programPreferences.getPerspectiveBackgroundColor().getBlue() / 255f;
	}


	public void setViewportBackground(final Color background) {
		setBackground(background);
	}

	protected void loadToTexMap(final Bitmap tex) {
		if (textureMap.get(tex) == null) {
			String path = tex.getPath();
			if (!path.isEmpty() && !programPreferences.getAllowLoadingNonBlpTextures()) {
				path = path.replaceAll("\\.\\w+", "") + ".blp";
			}
			Integer texture = null;
			try {
				final DataSource workingDirectory = modelView.getModel().getWrappedDataSource();
				texture = loadTexture(BLPHandler.get().loadTexture2(workingDirectory, path, tex), tex);
			} catch (final Exception exc) {
				if (LOG_EXCEPTIONS) {
					exc.printStackTrace();
				}
			}
			if (texture != null) {
				textureMap.put(tex, texture);
			}
		}
	}

	public void forceReloadTextures() {
		texLoaded = true;

		deleteAllTextures();
		addGeosets(modelView.getModel().getGeosets());
	}

	private void deleteAllTextures() {
		for (final Integer textureId : textureMap.values()) {
			GL11.glDeleteTextures(textureId);
		}
		textureMap.clear();
	}

	public void setPosition(final double a, final double b) {
		cameraPos.x = (float) a;
		cameraPos.y = (float) b;
	}

	public void translate(final double a, final double b) {
		cameraPos.x += a;
		cameraPos.y += b;
	}



	public BufferedImage getBufferedImage() {
		try {
			int height = getHeight();
			int width = getWidth();
			final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			// paintComponent(image.getGraphics(),5);
			final Pbuffer buffer = new Pbuffer(width, height, new PixelFormat(), null, null);
			buffer.makeCurrent();
			final ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);

			initGL();
			paintGL(false);

			GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL_UNSIGNED_BYTE, pixels);

			final int[] array = new int[pixels.capacity() / 4];
			final int[] flippedArray = new int[pixels.capacity() / 4];

			pixels.asIntBuffer().get(array);
			for (int i = 0; i < array.length; i++) {
				final int rgba = array[i];
				final int a = rgba & 0xFF;
				array[i] = (rgba >>> 8) | (a << 24);
			}

			for (int i = 0; i < height; i++) {
				System.arraycopy(array, i * width, flippedArray, (height - 1 - i) * width, width);
			}
			image.getRaster().setDataElements(0, 0, width, height, flippedArray);

			buffer.releaseContext();
			return image;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}


	public void addGeosets(final List<Geoset> geosets) {
		for (final Geoset geo : geosets) {
			for (int i = 0; i < geo.getMaterial().getLayers().size(); i++) {
				if (ModelUtils.isShaderStringSupported(modelView.getModel().getFormatVersion())) {
					if ((geo.getMaterial().getShaderString() != null)
							&& (geo.getMaterial().getShaderString().length() > 0)) {
						if (i > 0) {
							break;
						}
					}
				}
				final Layer layer = geo.getMaterial().getLayers().get(i);
				if (layer.getTextureBitmap() != null) {
					loadToTexMap(layer.getTextureBitmap());
				}
				if (layer.getTextures() != null) {
					for (final Bitmap tex : layer.getTextures()) {
						loadToTexMap(tex);
					}
				}
			}
		}
	}


	@Override
	protected void exceptionOccurred(final LWJGLException exception) {
		super.exceptionOccurred(exception);
		exception.printStackTrace();
	}

	@Override
	public void initGL() {
		try {
			if ((programPreferences == null) || programPreferences.textureModels()) {
				forceReloadTextures();
			}
		} catch (final Throwable e) {
			JOptionPane.showMessageDialog(null, "initGL failed because of this exact reason:\n"
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
			throw new RuntimeException(e);
		}
		// JAVA 9+ or maybe WIN 10 allow ridiculous virtual pixes, this combination of old library code
		// and java std library code give me a metric for the ridiculous ratio:
		xRatio = (float) (Display.getDisplayMode().getWidth() / Toolkit.getDefaultToolkit().getScreenSize().getWidth());
		yRatio = (float) (Display.getDisplayMode().getHeight() / Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		// These ratios will be wrong and users will see corrupted visuals (bad scale, only fits part of window,
		// etc) if they are using Windows 10 differing UI scale per monitor. I don't think I have an API to
		// query that information yet, though.

	}

	@Override
	public void paintGL() {
		paintGL(true);
	}

	private void paintGL(final boolean autoRepainting) {
		reloadIfNeeded();
		try {
			normalListMap.clear();
			vertListMap.clear();
			final int formatVersion = modelView.getModel().getFormatVersion();
			initContext(0, 0, 0);

			if ((programPreferences != null) && (programPreferences.viewMode() == 0)) {
				glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
			} else if ((programPreferences == null) || (programPreferences.viewMode() == 1)) {
				glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
			}
			glViewport(0, 0, (int) (getWidth() * xRatio), (int) (getHeight() * yRatio));
			glEnable(GL_DEPTH_TEST);

			GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glDepthMask(true);
			glEnable(GL_COLOR_MATERIAL);
			glEnable(GL_LIGHTING);
			glEnable(GL_LIGHT0);
			glEnable(GL_LIGHT1);
			glEnable(GL_NORMALIZE);
			GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

			if (renderTextures()) {
				glEnable(GL11.GL_TEXTURE_2D);
			}
			GL11.glEnable(GL11.GL_BLEND);
			glClearColor(backgroundRed, backgroundGreen, backgroundBlue, autoRepainting ? 1.0f : 0.0f);
//			glClearColor(backgroundRed, backgroundGreen, backgroundBlue, 1.0f);
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

			setUpCamera();

			final FloatBuffer ambientColor = BufferUtils.createFloatBuffer(4);
			ambientColor.put(0.6f).put(0.6f).put(0.6f).put(1f).flip();
			glLightModel(GL_LIGHT_MODEL_AMBIENT, ambientColor);

			addLamp(0.8f, 40.0f, 100.0f, 80.0f, GL_LIGHT0);

			addLamp(0.2f, -100.0f, 100.5f, 0.5f, GL_LIGHT1);

			for (final Geoset geo : modelView.getModel().getGeosets()) {
				processMesh(geo, isHD(geo, formatVersion));
			}

			glColor4f(0.5882352941176471f, 0.5882352941176471f, 1f, 0.3f);

			renderGeosets(formatVersion);
			glColor3f(1f, 1f, 1f);
			renderGeosets(modelView.getEditableGeosets(), formatVersion);
			GL11.glDepthMask(true);

			if (modelView.getHighlightedGeoset() != null) {
				drawHighlightedGeosets(formatVersion);
			}

			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_TEXTURE_2D);

			if (renderTextures()) {
				GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
				glEnable(GL11.GL_TEXTURE_2D);
			}


			if ((programPreferences != null)) {
				if (programPreferences.showNormals()) {
					drawNormals(formatVersion);
				}
				if (programPreferences.getRenderParticles()) {
					renderParticles();
				}
				if (programPreferences.showPerspectiveGrid()) {
					paintGridFloor();
				}
			}

			// glPopMatrix();
			paintAndUpdate();
		} catch (final Throwable e) {
//			e.printStackTrace();
			lastExceptionTimeMillis = System.currentTimeMillis();
			if ((lastThrownErrorClass == null) || (lastThrownErrorClass != e.getClass())) {
				lastThrownErrorClass = e.getClass();
				popupCount++;
				if (popupCount < 10) {
					ExceptionPopup.display(e);
				}
			}
			throw new RuntimeException(e);
		}
	}


	private void reloadIfNeeded() {
		if (wantReloadAll) {
			wantReloadAll = false;
			wantReload = false;// If we just reloaded all, no need to reload some.
			try {
				initGL();// Re-overwrite textures
			} catch (final Exception e) {
				e.printStackTrace();
				ExceptionPopup.display("Error loading textures:", e);
			}
		} else if (wantReload) {
			wantReload = false;
			try {
				forceReloadTextures();
			} catch (final Exception e) {
				e.printStackTrace();
				ExceptionPopup.display("Error loading new texture:", e);
			}
		} else if (!texLoaded && ((programPreferences == null) || programPreferences.textureModels())) {
			forceReloadTextures();
			texLoaded = true;
		}
	}

	private void paintAndUpdate() {
		try {
			swapBuffers();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
		final boolean showing = isShowing();
		final boolean running = paintTimer.isRunning();
		if (showing && !running) {
			paintTimer.restart();
		} else if (!showing && running) {
			paintTimer.stop();
		}
	}

	private void addLamp(float lightValue, float x, float y, float z, int glLight) {
		final FloatBuffer lightColor = BufferUtils.createFloatBuffer(4);
		lightColor.put(lightValue).put(lightValue).put(lightValue).put(1f).flip();
		final FloatBuffer lightPos = BufferUtils.createFloatBuffer(4);
		lightPos.put(x).put(y).put(z).put(1f).flip();
		glLight(glLight, GL_DIFFUSE, lightColor);
		glLight(glLight, GL_POSITION, lightPos);
	}

	private void renderParticles() {
		if (renderTextures()) {
			GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
			glEnable(GL11.GL_TEXTURE_2D);
		}
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_DEPTH_TEST);

		renderModel.getParticleShader().use();
		for (final RenderParticleEmitter2 particle : renderModel.getParticleEmitters2()) {
			particle.render(renderModel, renderModel.getParticleShader());
		}
	}

	private void paintGridFloor() {
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_BLEND);
		disableGlThings(GL_ALPHA_TEST, GL_TEXTURE_2D, GL_CULL_FACE);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		float lineLength = 200;
		float lineSpacing = 50;
		float numberOfLines = 5;
		glColor3f(255f, 1f, 255f);
		glColor4f(.7f, .7f, .7f, .4f);
		glBegin(GL11.GL_LINES);
		GL11.glNormal3f(0, 0, 0);
		float lineSpread = (numberOfLines + 1) * lineSpacing / 2;
		for (float x = -lineSpread + lineSpacing; x < lineSpread; x += lineSpacing) {
			GL11.glVertex3f(-lineLength, 0, x);
			GL11.glVertex3f(lineLength, 0, x);
		}
		for (float y = -lineSpread + lineSpacing; y < lineSpread; y += lineSpacing) {
			GL11.glVertex3f(y, 0, -lineLength);
			GL11.glVertex3f(y, 0, lineLength);
		}
		glEnd();
	}

	private void setUpCamera() {
		gluPerspective(45f, (float) getWidth() / (float) getHeight(), 20.0f, 600.0f);

		Vec3 statCamPos = new Vec3(0f, -70f, -200f);
		Vec3 dynCamPos = Vec3.getScaled(cameraPos, (float) m_zoom).sub(statCamPos);

		glTranslatef(dynCamPos.x, -dynCamPos.y, -dynCamPos.z);
		glRotatef(yangle, 1f, 0f, 0f);
		glRotatef(xangle, 0f, 1f, 0f);
		glScalef((float) m_zoom, (float) m_zoom, (float) m_zoom);
	}


	private void setStandardColors(GeosetAnim geosetAnim, float geosetAnimVisibility, ComPerspRenderEnv timeEnvironment, Layer layer) {
		setStandardColors2(geosetAnim, geosetAnimVisibility, timeEnvironment, layer);
	}

	private void setStandardColors2(GeosetAnim geosetAnim, float geosetAnimVisibility, AnimatedRenderEnvironment timeEnvironment, Layer layer) {
		if (timeEnvironment.getCurrentAnimation() != null) {
			final float layerVisibility = layer.getRenderVisibility(timeEnvironment);
			float alphaValue = geosetAnimVisibility * layerVisibility;
			if (/* geo.getMaterial().isConstantColor() && */ geosetAnim != null) {
				final Vec3 renderColor = geosetAnim.getRenderColor(timeEnvironment);
				if (renderColor != null) {
					if (layer.getFilterMode() == FilterMode.ADDITIVE) {
						GL11.glColor4f(renderColor.z * alphaValue, renderColor.y * alphaValue, renderColor.x * alphaValue, alphaValue);
					} else {
						GL11.glColor4f(renderColor.z * 1f, renderColor.y * 1f, renderColor.x * 1f, alphaValue);
					}
				} else {
					GL11.glColor4f(1f, 1f, 1f, alphaValue);
				}
			} else {
				GL11.glColor4f(1f, 1f, 1f, alphaValue);
			}
		} else {
			GL11.glColor4f(1f, 1f, 1f, 1f);
		}
	}

	public void bindLayerTexture(final Layer layer, final Bitmap tex, final Integer texture, final int formatVersion, final Material parent) {
		if (texture != null) {
			// texture.bind();
			bindTexture(tex, texture);
		} else if (textureMap.size() > 0) {
			bindTexture(tex, 0);
		}
		boolean depthMask = false;
		switch (layer.getFilterMode()) {
			case BLEND -> {
				setBlendWOAlpha(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			}
			case ADDITIVE, ADDALPHA -> {
				setBlendWOAlpha(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			}
			case MODULATE -> {
				setBlendWOAlpha(GL11.GL_ZERO, GL11.GL_SRC_COLOR);
			}
			case MODULATE2X -> {
				setBlendWOAlpha(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
			}
			case NONE -> {
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				GL11.glDisable(GL11.GL_BLEND);
				depthMask = true;
			}
			case TRANSPARENT -> {
				GL11.glEnable(GL11.GL_ALPHA_TEST);
				GL11.glAlphaFunc(GL11.GL_GREATER, 0.75f);
				GL11.glDisable(GL11.GL_BLEND);
				depthMask = true;
			}
		}
		if (layer.getTwoSided() || ((ModelUtils.isShaderStringSupported(formatVersion)) && parent.getTwoSided())) {
			GL11.glDisable(GL11.GL_CULL_FACE);
		} else {
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
		if (layer.getNoDepthTest()) {
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		} else {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
		}
		if (layer.getNoDepthSet()) {
			GL11.glDepthMask(false);
		} else {
			GL11.glDepthMask(depthMask);
		}
		if (layer.getUnshaded()) {
			GL11.glDisable(GL_LIGHTING);
		} else {
			glEnable(GL_LIGHTING);
		}
	}

	public void bindParticleTexture(final ParticleEmitter2 particle2, final Bitmap tex, final Integer texture) {
		if (texture != null) {
			// texture.bind();
			bindTexture(tex, texture);
		} else if (textureMap.size() > 0) {
			bindTexture(tex, 0);
		}
		switch (particle2.getFilterMode()) {
			case BLEND -> {
				setBlendWOAlpha(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			}
			case ADDITIVE, ALPHAKEY -> {
				setBlendWOAlpha(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			}
			case MODULATE -> {
				setBlendWOAlpha(GL11.GL_ZERO, GL11.GL_SRC_COLOR);
			}
			case MODULATE2X -> {
				setBlendWOAlpha(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
			}
		}
		if (particle2.getUnshaded()) {
			GL11.glDisable(GL_LIGHTING);
		} else {
			glEnable(GL_LIGHTING);
		}
	}

	private void setBlendWOAlpha(int sFactor, int dFactor) {
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(sFactor, dFactor);
	}

	private void drawHighlightedGeosets(int formatVersion) {
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		if ((programPreferences != null) && (programPreferences.getHighlighTriangleColor() != null)) {
			final Color highlighTriangleColor = programPreferences.getHighlighTriangleColor();
			glColor3f(highlighTriangleColor.getRed() / 255f, highlighTriangleColor.getGreen() / 255f, highlighTriangleColor.getBlue() / 255f);
		} else {
			glColor3f(1f, 3f, 1f);
		}
		renderGeosets(modelView.getHighlightedGeoset(), true, true, true, formatVersion);
		renderGeosets(modelView.getHighlightedGeoset(), false, true, true, formatVersion);
	}


	private void renderGeosets(int formatVersion) {
		for (final Geoset geo : modelView.getVisibleGeosets()) {// .getMDL().getGeosets()
			if (!modelView.getEditableGeosets().contains(geo) && (modelView.getHighlightedGeoset() != geo)) {
				renderGeosets(geo, true, false, true, formatVersion);
			}
		}
		for (final Geoset geo : modelView.getVisibleGeosets()) {// .getMDL().getGeosets()
			if (!modelView.getEditableGeosets().contains(geo) && (modelView.getHighlightedGeoset() != geo)) {
				renderGeosets(geo, false, false, true, formatVersion);
			}
		}
	}


	public void renderGeosets(final Iterable<Geoset> geosets, final int formatVersion) {
		for (final Geoset geo : geosets) {// .getMDL().getGeosets()
			renderGeosets(geo, true, false, false, formatVersion);
		}
		for (final Geoset geo : geosets) {// .getMDL().getGeosets()
			renderGeosets(geo, false, false, false, formatVersion);
		}
	}

	public void renderGeosets(final Geoset geo, final boolean renderOpaque, final boolean overriddenMaterials, final boolean overriddenColors, final int formatVersion) {
		final GeosetAnim geosetAnim = geo.getGeosetAnim();
		float geosetAnimVisibility = 1;
		final AnimatedRenderEnvironment timeEnvironment = renderModel.getAnimatedRenderEnvironment();
		final TimeBoundProvider animation = timeEnvironment == null ? null : timeEnvironment.getCurrentAnimation();
		if ((animation != null) && (geosetAnim != null)) {
			geosetAnimVisibility = geosetAnim.getRenderVisibility(timeEnvironment);
			if (geosetAnimVisibility < RenderModel.MAGIC_RENDER_SHOW_CONSTANT) {
				return;
			}
		}
		Material material = geo.getMaterial();
		for (int i = 0; i < material.getLayers().size(); i++) {
			if (ModelUtils.isShaderStringSupported(modelView.getModel().getFormatVersion())) {
				if ((material.getShaderString() != null) && (material.getShaderString().length() > 0)) {
					if (i > 0) {
						break;
					}
				}
			}
			final Layer layer = material.getLayers().get(i);

			if (!overriddenColors) {
				setStandardColors2(geosetAnim, geosetAnimVisibility, timeEnvironment, layer);
			}

			final FilterMode filterMode = layer.getFilterMode();
			final boolean opaqueLayer = (filterMode == FilterMode.NONE) || (filterMode == FilterMode.TRANSPARENT);
			if ((renderOpaque && opaqueLayer) || (!renderOpaque && !opaqueLayer)) {
				if (!overriddenMaterials) {
					final Bitmap tex = layer.getRenderTexture(timeEnvironment, modelView.getModel());
					final Integer texture = textureMap.get(tex);
					bindLayerTexture(layer, tex, texture, formatVersion, material);
				}
				if (overriddenColors) {
					GL11.glDisable(GL11.GL_ALPHA_TEST);
				}
				glBegin(GL11.GL_TRIANGLES);

//				renderMesh(geo, formatVersion, layer);
				renderMesh(geo, layer, false);
				glEnd();
			}
		}
	}


	private void drawNormals(int formatVersion) {
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		glBegin(GL11.GL_LINES);
		glColor3f(1f, 1f, 3f);

		for (final Geoset geo : modelView.getModel().getGeosets()) {
			if (correctLoD(geo, formatVersion)) continue;
//			renderNormals(formatVersion, geo);
			renderMesh(geo, null, true);
		}
		glEnd();
	}

	private boolean correctLoD(Geoset geo, int formatVersion) {
		if ((ModelUtils.isLevelOfDetailSupported(formatVersion)) && (geo.getLevelOfDetailName() != null) && (geo.getLevelOfDetailName().length() > 0)) {
			return geo.getLevelOfDetail() != levelOfDetail;
		}
		return false;
	}

	private void renderMesh(Geoset geo, Layer layer, boolean onlyNormals) {
		int n = 0;
		List<Vec4> transformedVertices = vertListMap.get(geo);
		List<Vec4> transformedNormals = normalListMap.get(geo);
		if (transformedVertices != null) {
			for (final Triangle tri : geo.getTriangles()) {
				for (final GeosetVertex vertex : tri.getVerts()) {
					Vec4 vertexSumHeap = transformedVertices.get(n);

					if (vertex.getNormal() != null) {
						Vec4 normalSumHeap = transformedNormals.get(n);

						if (!normalSumHeap.isValid()) {
							continue;
						}

						GL11.glNormal3f(normalSumHeap.y, normalSumHeap.z, normalSumHeap.x);

						if (onlyNormals) {
							paintNormal(vertexSumHeap, normalSumHeap);
						}
					}
					if (!onlyNormals) {
						paintVert(layer, vertex, vertexSumHeap);
					}
					n++;
				}
			}
		}
	}

	private void paintNormal(Vec4 vertexSumHeap, Vec4 normalSumHeap) {
		GL11.glNormal3f(normalSumHeap.y, normalSumHeap.z, normalSumHeap.x);
		GL11.glVertex3f(vertexSumHeap.y, vertexSumHeap.z, vertexSumHeap.x);

		Vec3 nSA = normalSumHeap.getVec3().scale((float) (6 / m_zoom));
		Vec3 vSA = vertexSumHeap.getVec3().add(nSA);

		GL11.glNormal3f(normalSumHeap.y, normalSumHeap.z, normalSumHeap.x);
		GL11.glVertex3f(vSA.y, vSA.z, vSA.x);
	}

	public void paintVert(Layer layer, GeosetVertex vertex, Vec4 vertexSumHeap) {
		int coordId = layer.getCoordId();
		if (coordId >= vertex.getTverts().size()) {
			coordId = vertex.getTverts().size() - 1;
		}
		GL11.glTexCoord2f(vertex.getTverts().get(coordId).x, vertex.getTverts().get(coordId).y);
		GL11.glVertex3f(vertexSumHeap.y, vertexSumHeap.z, vertexSumHeap.x);
	}

	private void processMesh(Geoset geo, boolean isHd) {
		List<Vec4> transformedVertices = new ArrayList<>();
		List<Vec4> transformedNormals = new ArrayList<>();
		for (final Triangle tri : geo.getTriangles()) {
			for (final GeosetVertex vertex : tri.getVerts()) {
				Mat4 skinBonesMatrixSumHeap;
				if (isHd) {
					skinBonesMatrixSumHeap = processHdBones(vertex);
				} else {
					skinBonesMatrixSumHeap = processSdBones(vertex);
				}
				Vec4 vertexSumHeap = Vec4.getTransformed(new Vec4(vertex, 1), skinBonesMatrixSumHeap);
				transformedVertices.add(vertexSumHeap);
				if (vertex.getNormal() != null) {
					Vec4 normalSumHeap = Vec4.getTransformed(new Vec4(vertex.getNormal(), 0), skinBonesMatrixSumHeap);
					normalizeHeap(normalSumHeap);
					transformedNormals.add(normalSumHeap);
				} else {
					transformedNormals.add(null);
				}
			}
		}
		vertListMap.put(geo, transformedVertices);
		normalListMap.put(geo, transformedNormals);
	}

	private Mat4 processSdBones(GeosetVertex vertex) {
		final int boneCount = vertex.getBones().size();
		Mat4 bonesMatrixSumHeap = new Mat4().setZero();
		if (boneCount > 0) {
			for (final Bone bone : vertex.getBones()) {
				bonesMatrixSumHeap.add(renderModel.getRenderNode(bone).getWorldMatrix());
			}
			return bonesMatrixSumHeap.uniformScale(1f / boneCount);
		}
		return bonesMatrixSumHeap.setIdentity();
	}

	private Mat4 processHdBones(GeosetVertex vertex) {
		final Bone[] skinBones = vertex.getSkinBones();
		final short[] skinBoneWeights = vertex.getSkinBoneWeights();
		boolean processedBones = false;
		Mat4 skinBonesMatrixSumHeap = new Mat4().setZero();
		for (int boneIndex = 0; boneIndex < 4; boneIndex++) {
			final Bone skinBone = skinBones[boneIndex];
			if (skinBone == null) {
				continue;
			}
			processedBones = true;
			final Mat4 worldMatrix = renderModel.getRenderNode(skinBone).getWorldMatrix();
			float skinBoneWeight = skinBoneWeights[boneIndex] / 255f;
			skinBonesMatrixSumHeap.add(worldMatrix.getUniformlyScaled(skinBoneWeight));
		}
		if (!processedBones) {
			skinBonesMatrixSumHeap.setIdentity();
		}
		return skinBonesMatrixSumHeap;
	}

	public boolean isHD(Geoset geo, int formatVersion) {
		return (ModelUtils.isTangentAndSkinSupported(formatVersion)) && (geo.getVertices().size() > 0) && (geo.getVertex(0).getSkinBones() != null);
	}

	private void clickTimerAction() {
		final int xoff = 0;
		final int yoff = 0;
		PointerInfo pointerInfo = MouseInfo.getPointerInfo();
		if (pointerInfo != null) {
			final double mx = pointerInfo.getLocation().x - xoff;
			final double my = pointerInfo.getLocation().y - yoff;
			// JOptionPane.showMessageDialog(null,mx+","+my+" as mouse, "+lastClick.x+","+lastClick.y+" as last.");
			// System.out.println(xoff+" and "+mx);
			if (cameraPanStartPoint != null) {
				// translate Viewport Camera
				double cameraPointX = ((int) mx - cameraPanStartPoint.x) / m_zoom;
				double cameraPointY = ((int) my - cameraPanStartPoint.y) / m_zoom;

				Vec3 vertexHeap = new Vec3(cameraPointX, cameraPointY, 0);
				cameraPos.add(vertexHeap);
				cameraPanStartPoint.x = (int) mx;
				cameraPanStartPoint.y = (int) my;
			}
			if (cameraSpinStartPoint != null) {
				// rotate Viewport Camera
				xangle += mx - cameraSpinStartPoint.x;
				yangle += my - cameraSpinStartPoint.y;


				Vec4 yAxisHeap = new Vec4(0, 1, 0, (float) Math.toRadians(yangle));
				inverseCameraRotationYSpin.setFromAxisAngle(yAxisHeap).invertRotation();

				Vec4 zAxisHeap = new Vec4(0, 0, 1, (float) Math.toRadians(xangle));
				inverseCameraRotationZSpin.setFromAxisAngle(zAxisHeap).invertRotation();

				inverseCameraRotationQuat.set(Quat.getProd(inverseCameraRotationYSpin, inverseCameraRotationZSpin)).invertRotation();

				cameraSpinStartPoint.x = (int) mx;
				cameraSpinStartPoint.y = (int) my;
			}
			// MainFrame.panel.setMouseCoordDisplay(m_d1,m_d2,((mx-getWidth()/2)/m_zoom)-m_a,-(((my-getHeight()/2)/m_zoom)-m_b));

			if (actStart != null) {
				final Point actEnd = new Point((int) mx, (int) my);
				final Point2D.Double convertedStart = new Point2D.Double(geomX(actStart.x), geomY(actStart.y));
				final Point2D.Double convertedEnd = new Point2D.Double(geomX(actEnd.x), geomY(actEnd.y));
				// dispMDL.updateAction(convertedStart,convertedEnd,m_d1,m_d2);
				actStart = actEnd;
			}
		}
	}

	public Rectangle2D.Double pointsToGeomRect(final Point a, final Point b) {
		final Point2D.Double topLeft = new Point2D.Double(Math.min(geomX(a.x), geomX(b.x)), Math.min(geomY(a.y), geomY(b.y)));
		final Point2D.Double lowRight = new Point2D.Double(Math.max(geomX(a.x), geomX(b.x)), Math.max(geomY(a.y), geomY(b.y)));
		return new Rectangle2D.Double(topLeft.x, topLeft.y, lowRight.x - topLeft.x, lowRight.y - topLeft.y);
	}

	public Rectangle2D.Double pointsToRect(final Point a, final Point b) {
		final Point2D.Double topLeft = new Point2D.Double(Math.min(a.x, b.x), Math.min(a.y, b.y));
		final Point2D.Double lowRight = new Point2D.Double(Math.max(a.x, b.x), Math.max(a.y, b.y));
		return new Rectangle2D.Double(topLeft.x, topLeft.y, lowRight.x - topLeft.x, lowRight.y - topLeft.y);
	}

	public void setWireframeHandler(final JCheckBox nwireframe) {
		wireframe = nwireframe;
	}

	public boolean renderTextures() {
		return texLoaded && ((programPreferences == null) || programPreferences.textureModels());
	}

	public void reloadTextures() {
		wantReload = true;
	}

	public void reloadAllTextures() {
		wantReloadAll = true;
	}

	public void zoom(final double amount) {
		m_zoom *= 1 + amount;
	}

	public double getZoomAmount() {
		return m_zoom;
	}

	public Point2D.Double getDisplayOffset() {
		return new Point2D.Double(cameraPos.x, cameraPos.y);
	}

	public double convertX(final double x) {
		return ((x + cameraPos.x) * m_zoom) + (getWidth() / 2.0);
	}

	public double convertY(final double y) {
		return ((-y + cameraPos.y) * m_zoom) + (getHeight() / 2.0);
	}

	public double geomX(final double x) {
		return ((x - (getWidth() / 2.0)) / m_zoom) - cameraPos.x;
	}

	public double geomY(final double y) {
		return -(((y - (getHeight() / 2.0)) / m_zoom) - cameraPos.y);
	}

	private void normalizeHeap(Vec4 heap) {
		if (heap.length() > 0) {
			heap.normalize();
		} else {
			heap.set(0, 1, 0, 0);
		}
	}

	private void enableGlThings(int... thing) {
		for (int t : thing) {
			glEnable(t);
		}
	}

	private void disableGlThings(int... thing) {
		for (int t : thing) {
			glDisable(t);
		}
	}

	public MouseAdapter getMouseAdapter() {
		return new MouseAdapter() {
			@Override
			public void mouseEntered(final MouseEvent e) {
				clickTimer.setRepeats(true);
				clickTimer.start();
				mouseInBounds = true;
			}

			@Override
			public void mouseExited(final MouseEvent e) {
				if ((cameraSpinStartPoint == null) && (actStart == null) && (cameraPanStartPoint == null)) {
					clickTimer.stop();
				}
				mouseInBounds = false;
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				if (programPreferences.getThreeDCameraPanButton().isButton(e)) {
					cameraPanStartPoint = new Point(e.getXOnScreen(), e.getYOnScreen());
				} else if (programPreferences.getThreeDCameraSpinButton().isButton(e)) {
					cameraSpinStartPoint = new Point(e.getXOnScreen(), e.getYOnScreen());
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					actStart = new Point(e.getX(), e.getY());
					final Point2D.Double convertedStart = new Point2D.Double(geomX(actStart.x), geomY(actStart.y));
					// dispMDL.startAction(convertedStart,m_d1,m_d2,MainFrame.panel.currentActionType());
				}
				System.out.println("camPos: " + cameraPos + ", invQ: " + inverseCameraRotationQuat + ", invYspin: " + inverseCameraRotationYSpin + ", invZspin: " + inverseCameraRotationZSpin);
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				if (programPreferences.getThreeDCameraPanButton().isButton(e) && (cameraPanStartPoint != null)) {
					cameraPos.x += (e.getXOnScreen() - cameraPanStartPoint.x) / m_zoom;
					cameraPos.y += (e.getYOnScreen() - cameraPanStartPoint.y) / m_zoom;
					cameraPanStartPoint = null;
				} else if (programPreferences.getThreeDCameraSpinButton().isButton(e) && (cameraSpinStartPoint != null)) {
					final Point selectEnd = new Point(e.getX(), e.getY());
					final Rectangle2D.Double area = pointsToGeomRect(cameraSpinStartPoint, selectEnd);
					// System.out.println(area);
					// dispMDL.selectVerteces(area,m_d1,m_d2,MainFrame.panel.currentSelectionType());
					cameraSpinStartPoint = null;
				} else if ((e.getButton() == MouseEvent.BUTTON3) && (actStart != null)) {
					final Point actEnd = new Point(e.getX(), e.getY());
					final Point2D.Double convertedStart = new Point2D.Double(geomX(actStart.x), geomY(actStart.y));
					final Point2D.Double convertedEnd = new Point2D.Double(geomX(actEnd.x), geomY(actEnd.y));
					// dispMDL.finishAction(convertedStart,convertedEnd,m_d1,m_d2);
					actStart = null;
				}
				if (!mouseInBounds && (cameraSpinStartPoint == null) && (actStart == null) && (cameraPanStartPoint == null)) {
					clickTimer.stop();
				}
				/*
				 * if( dispMDL != null ) dispMDL.refreshUndo();
				 */
			}

			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {

					// if( actEnd.equals(actStart) )
					// {
					// actStart = null;

					// JPopupMenu.setDefaultLightWeightPopupEnabled(false);
					// ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
					// contextMenu.show(this, e.getX(), e.getY());
					// }
				}
			}

			@Override
			public void mouseWheelMoved(final MouseWheelEvent e) {
				int wr = e.getWheelRotation();
				final boolean neg = wr < 0;

				if (neg) {
					wr = -wr;
				}
				for (int i = 0; i < wr; i++) {
					if (neg) {
						// cameraPos.x -= (mx - getWidth() / 2)
						// * (1 / m_zoom - 1 / (m_zoom * 1.15));
						// cameraPos.y -= (my - getHeight() / 2)
						// * (1 / m_zoom - 1 / (m_zoom * 1.15));
						// cameraPos.z -= (getHeight() / 2)
						// * (1 / m_zoom - 1 / (m_zoom * 1.15));
						m_zoom *= 1.15;
					} else {
						m_zoom /= 1.15;
						// cameraPos.x -= (mx - getWidth() / 2)
						// * (1 / (m_zoom * 1.15) - 1 / m_zoom);
						// cameraPos.y -= (my - getHeight() / 2)
						// * (1 / (m_zoom * 1.15) - 1 / m_zoom);
						// cameraPos.z -= (getHeight() / 2)
						// * (1 / (m_zoom * 1.15) - 1 / m_zoom);
					}
				}
			}
		};
	}

	@Override
	public InternalResource allocateTexture(final Bitmap bitmap, final ParticleEmitter2 textureSource) {
		return new Particle2TextureInstance(bitmap, textureSource);
	}

	private final class Particle2TextureInstance implements InternalResource, InternalInstance {
		private final Bitmap bitmap;
		private final ParticleEmitter2 particle;
		private boolean loaded = false;

		public Particle2TextureInstance(final Bitmap bitmap, final ParticleEmitter2 particle) {
			this.bitmap = bitmap;
			this.particle = particle;
		}

		@Override
		public void setTransformation(final Vec3 worldLocation, final Quat rotation, final Vec3 worldScale) {
		}

		@Override
		public void setSequence(final int index) {
		}

		@Override
		public void show() {
		}

		@Override
		public void setPaused(final boolean paused) {
		}

		@Override
		public void move(final Vec3 deltaPosition) {
		}

		@Override
		public void hide() {
		}

		@Override
		public void bind() {
			if (!loaded) {
				loadToTexMap(bitmap);
				loaded = true;
			}
			final Integer texture = textureMap.get(bitmap);
			bindParticleTexture(particle, bitmap, texture);
		}

		@Override
		public InternalInstance addInstance() {
			return this;
		}

	}
}