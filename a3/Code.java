package a3;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.*;
import javax.swing.*;
import java.lang.Math;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_BLEND;
import static com.jogamp.opengl.GL.GL_CCW;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_FRONT;
import static com.jogamp.opengl.GL.GL_FUNC_ADD;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LESS;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_ONE_MINUS_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_REPEAT;
import static com.jogamp.opengl.GL.GL_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE1;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import org.joml.*;

public class Code extends JFrame implements GLEventListener, KeyListener {
	private GLCanvas myCanvas;
	private int renderingProgram, renderingProgramCubeMap, renderingProgramGeometry;
	private int vao[] = new int[1];
	private int vbo[] = new int[15];

	private Camera camera;
	private Vector3f cameraLoc = new Vector3f(0, 1.75f, 5f);

	// ---------------------- Camera ----------------------
	private float cameraPitch = 0.0f;
	private float cameraYaw = 0.0f;

	// ---------------------- TIME ----------------------
	private double startTime = 0;
	private double prevTime = 0;
	private double deltaTime = 0;
	private double elapsedTime = 0;
	private double currentTime = 0;

	// ---------------------- Fog parameters ----------------------
	private float[] fogColor = { 0.7f, 0.8f, 0.9f };
	private float fogDensity = 0.5f;
	private float fogStartDistance = 5.0f;
	private float fogMaxDistance = 10.0f;
	private int fogColorLoc, fogDensityLoc, fogStartDistanceLoc, fogMaxDistanceLoc;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f(); // perspective matrix
	private Matrix4f vMat = new Matrix4f(); // view matrix
	private Matrix4f mMat = new Matrix4f(); // model matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int mLoc, vLoc, pLoc, nLoc, sampLoc;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private float aspect;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];

	// ---------------------- LIGHTING ----------------------
	float lightRotationAngle = 0;

	// white light properties
	float[] globalAmbient = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	// float[] lightAmbient = new float[] { 0.1f, 0.1f, 0.1f, 1.0f };
	// float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	// float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightAmbient = new float[] { 0.0f, 0.1f, 0.0f, 1.0f };
	float[] lightDiffuse = new float[] { 0.25f, 1.0f, 0.25f, 1.0f };
	float[] lightSpecular = new float[] { 0.25f, 1.0f, 0.25f, 1.0f };

	// Silver material
	float[] matAmb = Utils.silverAmbient();
	float[] matDif = Utils.silverDiffuse();
	float[] matSpe = Utils.silverSpecular();
	float matShi = Utils.silverShininess();

	// custom cow hide material
	float[] cowMatAmb = Utils.cowAmbient(); // or Utils.spottedCowAmbient()
	float[] cowMatDif = Utils.cowDiffuse(); // or Utils.spottedCowDiffuse()
	float[] cowMatSpe = Utils.cowSpecular(); // or Utils.spottedCowSpecular()
	float cowMatShi = Utils.cowShininess(); // or Utils.spottedCowShininess()

	// ---------------------- Blending ----------------------
	private ImportedModel pyramidModel;
	private int numObjVerticesPyramid;
	private float pyramidRotationY = 0.0f;
	private float pyramidRotationSpeed = 60.0f;
	private float[] pyramidMatAmb = { 0.0f, 0.5f, 0.0f, 1.0f }; // Light green ambient
	private float[] pyramidMatDif = { 0.0f, 0.8f, 0.0f, 1.0f }; // Light green diffuse
	private float[] pyramidMatSpe = { 0.0f, 1.0f, 0.0f, 1.0f }; // Light green specular
	private float pyramidMatShi = 50.0f;
	private int alphaLoc, flipLoc;

	// ---------------------- Models and Textures ----------------------
	private int ufoTexture;
	private int cowTexture;
	private int groundTexture;
	private int axisTexture;
	private int skyboxTexture;

	private int numObjVerticesUfo, numObjVerticesCow;
	private ImportedModel ufoModel, cowModel;

	// Axes
	private boolean visibleAxis = true;
	private float axesX = 0.0f;

	// ufo
	private float ufoPositionX = 0.0f;
	private float ufoPositionY = 1.5f;
	private float ufoPositionZ = 0.0f;
	private float ufoMovementSpeed = 0.5f;
	private float ufoRotationSpeed = 35.0f;
	private float ufoRotationY = 0.0f;
	private float ufoWave = 2.0f;

	// cow rotation angles
	private float cowRotationX = 0.0f;
	private float cowRotationY = 0.0f;
	private float cowRotationSpeed = 50.0f;

	private int reflectionFactorLoc;
	private float ufoReflectionFactor = 1.0f;

	// ---------------------- stereoscopy ----------------------
	private boolean stereoscopicMode = false;
	private float IOD = 0.01f;
	private int sizeX, sizeY;
	private float near = 0.1f;
	private float far = 1000.0f;

	// ---------------------- Geometry ----------------------
	private boolean explodeMode = false;
	private int explodeModeLocation;

	private Matrix4fStack mvStack = new Matrix4fStack(16);

	public Code() {
		setTitle("Lab 3");
		sizeX = 1920;
		sizeY = 1080;
		setSize(sizeX, sizeY);
		myCanvas = new GLCanvas();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		myCanvas.addGLEventListener(this);
		myCanvas.addKeyListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	private void renderSceneForEye(float leftRight) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		// Create an eye-specific view matrix with an offset if in stereoscopic mode
		Matrix4f eyeSpecificVMat = new Matrix4f(vMat);
		if (stereoscopicMode && leftRight != 0.0f) {
			// Offset the camera position for stereo viewing
			eyeSpecificVMat.identity().setTranslation(
					-(cameraLoc.x + leftRight * IOD / 2.0f),
					-cameraLoc.y,
					-cameraLoc.z);
			// Apply camera rotation after translation
			eyeSpecificVMat.rotateX((float) Math.toRadians(cameraPitch));
			eyeSpecificVMat.rotateY((float) Math.toRadians(cameraYaw));
		} else {
			// Regular camera view matrix
			eyeSpecificVMat.set(camera.getViewMatrix());
		}
		computePerspectiveMatrix(leftRight);

		// ---------------------- Skybox ----------------------
		gl.glUseProgram(renderingProgramCubeMap);

		gl.glDepthMask(false);

		int vLocCubeMap = gl.glGetUniformLocation(renderingProgramCubeMap, "v_matrix");
		gl.glUniformMatrix4fv(vLocCubeMap, 1, false, vMat.get(vals));
		int pLocCubeMap = gl.glGetUniformLocation(renderingProgramCubeMap, "p_matrix");
		gl.glUniformMatrix4fv(pLocCubeMap, 1, false, pMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW); // cube is CW, but we are viewing the inside
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);

		gl.glDepthMask(true);
		gl.glDepthFunc(GL_LESS);

		// ---------------------- Scene ----------------------
		if (explodeMode) {
			gl.glUseProgram(renderingProgramGeometry);
		} else {
			gl.glUseProgram(renderingProgram);
		}

		// Uniform Variables
		mLoc = gl.glGetUniformLocation(explodeMode ? renderingProgramGeometry : renderingProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(explodeMode ? renderingProgramGeometry : renderingProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(explodeMode ? renderingProgramGeometry : renderingProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(explodeMode ? renderingProgramGeometry : renderingProgram, "norm_matrix");
		sampLoc = gl.glGetUniformLocation(explodeMode ? renderingProgramGeometry : renderingProgram, "samp");
		alphaLoc = gl.glGetUniformLocation(explodeMode ? renderingProgramGeometry : renderingProgram, "alpha");
		flipLoc = gl.glGetUniformLocation(explodeMode ? renderingProgramGeometry : renderingProgram, "flipNormal");
		reflectionFactorLoc = gl.glGetUniformLocation(explodeMode ? renderingProgramGeometry : renderingProgram,
				"reflectionFactor");

		pyramidRotationY += pyramidRotationSpeed * deltaTime;

		// Pass perspective matrix to shader
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));

		// Get fog uniform locations
		fogColorLoc = gl.glGetUniformLocation(renderingProgram, "fogColor");
		fogDensityLoc = gl.glGetUniformLocation(renderingProgram, "fogDensity");
		fogStartDistanceLoc = gl.glGetUniformLocation(renderingProgram, "fogStartDistance");
		fogMaxDistanceLoc = gl.glGetUniformLocation(renderingProgram, "fogMaxDistance");

		// Set fog uniform values
		gl.glProgramUniform3fv(renderingProgram, fogColorLoc, 1, fogColor, 0);
		gl.glProgramUniform1f(renderingProgram, fogDensityLoc, fogDensity);
		gl.glProgramUniform1f(renderingProgram, fogStartDistanceLoc, fogStartDistance);
		gl.glProgramUniform1f(renderingProgram, fogMaxDistanceLoc, fogMaxDistance);

		// Time Values
		currentTime = System.currentTimeMillis();
		elapsedTime = currentTime - startTime;
		deltaTime = (currentTime - prevTime) / 1000;
		prevTime = currentTime;

		camera.updateViewMatrix();
		vMat = camera.getViewMatrix();

		// ---------------------- Ground ----------------------
		gl.glDisable(GL_CULL_FACE);

		Matrix4f groundMat = new Matrix4f();
		groundMat.identity().translate(0.0f, 0.0f, 0.0f);

		groundMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, groundMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// Ground Vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Ground Textures
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Ground normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Binding Texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, groundTexture);
		gl.glUniform1i(sampLoc, 0);

		gl.glDrawArrays(GL_TRIANGLES, 0, 6);
		gl.glEnable(GL_CULL_FACE); // Re-enable if needed

		// ---------------------- Axes ----------------------
		if (visibleAxis) {
			Matrix4f axesMat = new Matrix4f();
			axesMat.identity().translate(0.0f, 0.0f, 0.0f);

			gl.glUniformMatrix4fv(mLoc, 1, false, axesMat.get(vals));

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
			gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(1);

			gl.glActiveTexture(GL_TEXTURE0);
			gl.glBindTexture(GL_TEXTURE_2D, axisTexture);

			gl.glLineWidth(2.0f);
			gl.glDrawArrays(GL_LINES, 0, 6);
		}

		// ---------------------- Scene (MV STACK + VARIABLES) ----------------------

		cowRotationX += cowRotationSpeed * deltaTime;
		cowRotationY += cowRotationSpeed * deltaTime;

		ufoRotationY += ufoRotationSpeed * deltaTime;

		// Light Position
		currentLightPos.set(ufoPositionX, ufoPositionY, ufoPositionZ);

		installLights();

		mvStack.pushMatrix();

		// ---------------------- Ufo ----------------------
		mvStack.pushMatrix();

		mvStack.translate(ufoPositionX, ufoPositionY, ufoPositionZ)
				.scale(0.5f)
				.rotateY((float) Math.toRadians(ufoRotationY));

		mMat.set(mvStack);
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		// Send matrices to shaders
		gl.glUniformMatrix4fv(mLoc, 1, false, mvStack.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// Set reflection factor for UFO
		gl.glProgramUniform1f(renderingProgram, reflectionFactorLoc, 0.6f);

		// Ufo Vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Ufo Texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Ufo normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Binding Textures for UFO
		// gl.glActiveTexture(GL_TEXTURE0);
		// gl.glBindTexture(GL_TEXTURE_2D, ufoTexture);
		// gl.glUniform1i(sampLoc, 0);

		// Binding cubemap for environment mapping
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);
		int skyboxLoc = gl.glGetUniformLocation(renderingProgram, "skybox");
		gl.glUniform1i(skyboxLoc, 1);

		// Render
		gl.glDrawArrays(GL_TRIANGLES, 0, ufoModel.getNumVertices());

		// Reset reflection factor for other objects
		gl.glProgramUniform1f(renderingProgram, reflectionFactorLoc, 0.0f);

		// ---------------------- Cow ----------------------
		mvStack.pushMatrix();

		mvStack.translate(0, -2.0f, 0)
				.scale(0.05f)
				.rotateX((float) Math.toRadians(cowRotationX))
				.rotateY((float) Math.toRadians(cowRotationY))
				.rotateY((float) Math.toRadians(-90.0f));

		mMat.set(mvStack);
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		// Send matrices to shaders
		gl.glUniformMatrix4fv(mLoc, 1, false, mvStack.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// Set material
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, cowMatAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, cowMatDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, cowMatSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, cowMatShi);

		// Cow Vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Cow Texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Cow normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Binding Texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, cowTexture);
		gl.glUniform1i(sampLoc, 0);

		// Render
		gl.glDrawArrays(GL_TRIANGLES, 0, cowModel.getNumVertices());

		// Reseting material
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);

		mvStack.popMatrix();

		// ---------------------- Pyramid ----------------------
		// Draw the pyramid with transparency
		mvStack.pushMatrix();

		// Position the pyramid below the UFO
		mvStack.translate(0.0f, -1.6f, 0.0f)
				.scale(0.8f, 1.4f, 0.8f)
				.rotateY((float) Math.toRadians(pyramidRotationY));

		mMat.set(mvStack);
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		// Send matrices to shaders
		gl.glUniformMatrix4fv(mLoc, 1, false, mvStack.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// Set pyramid material properties
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, pyramidMatAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, pyramidMatDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, pyramidMatSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, pyramidMatShi);

		// Enable blending for transparency
		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glBlendEquation(GL_FUNC_ADD);

		// Draw the back faces first (two-pass transparency rendering)
		gl.glEnable(GL_CULL_FACE);

		// First pass - render back faces
		gl.glCullFace(GL_FRONT);
		gl.glProgramUniform1f(renderingProgram, alphaLoc, 0.3f); // 30% opacity for back faces
		gl.glProgramUniform1f(renderingProgram, flipLoc, -1.0f); // Flip normals for back faces

		// Pyramid Vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Pyramid Texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Pyramid normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glDrawArrays(GL_TRIANGLES, 0, numObjVerticesPyramid);

		// Second pass - render front faces
		gl.glCullFace(GL_BACK);
		gl.glProgramUniform1f(renderingProgram, alphaLoc, 0.7f); // 70% opacity for front faces
		gl.glProgramUniform1f(renderingProgram, flipLoc, 1.0f); // Normal direction for front faces

		gl.glDrawArrays(GL_TRIANGLES, 0, numObjVerticesPyramid);

		// Disable blending and restore default state
		gl.glDisable(GL_BLEND);
		gl.glProgramUniform1f(renderingProgram, alphaLoc, 1.0f); // Reset alpha to fully opaque
		gl.glProgramUniform1f(renderingProgram, flipLoc, 1.0f); // Reset normal direction

		// Reset material properties to default
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);

		mvStack.popMatrix();
		// --------------------------------------------
		mvStack.popMatrix();
		mvStack.popMatrix();
		// --------------------------------------------
		gl.glDisable(GL_CULL_FACE);
	}

	private void computePerspectiveMatrix(float leftRight) {
		if (stereoscopicMode) {
			float top = (float) Math.tan(Math.toRadians(60.0f / 2.0f)) * near;
			float bottom = -top;
			float frustumshift = (IOD / 2.0f) * near / far;
			float stereoAspect = aspect / 2.0f;
			float left = -stereoAspect * top - frustumshift * leftRight;
			float right = stereoAspect * top - frustumshift * leftRight;

			pMat.identity().frustum(left, right, bottom, top, near, far);
		} else {
			// default
			pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, near, far);
		}
	}

	public void display(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Black background
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		if (stereoscopicMode) {
			// Left eye (first half of screen)
			gl.glViewport(0, 0, myCanvas.getWidth() / 2, myCanvas.getHeight());
			renderSceneForEye(-1.0f); // -1.0f for left eye

			// Right eye (second half of screen)
			gl.glViewport(myCanvas.getWidth() / 2, 0, myCanvas.getWidth() / 2, myCanvas.getHeight());
			renderSceneForEye(1.0f); // 1.0f for right eye
		} else {
			// Regular rendering (full screen)
			gl.glViewport(0, 0, myCanvas.getWidth(), myCanvas.getHeight());
			renderSceneForEye(0.0f); // 0.0f for center/regular view
		}
	}

	public void init(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		camera = new Camera();

		ufoModel = new ImportedModel("ufo.obj");
		cowModel = new ImportedModel("cow.obj");
		pyramidModel = new ImportedModel("pyr.obj");

		renderingProgram = Utils.createShaderProgram("a3/vertShader.glsl", "a3/fragShader.glsl");
		renderingProgramCubeMap = Utils.createShaderProgram("a3/vertCShader.glsl", "a3/fragCShader.glsl");
		renderingProgramGeometry = Utils.createShaderProgram("a3/vertShader.glsl", "a3/geomShader.glsl",
				"a3/fragShader.glsl");

		startTime = System.currentTimeMillis();
		prevTime = startTime;

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupVertices();

		ufoTexture = Utils.loadTexture("ufo.png");
		cowTexture = Utils.loadTexture("cow.png");
		groundTexture = Utils.loadTexture("ground.jpg");
		axisTexture = Utils.loadTexture("axis.png");
		skyboxTexture = Utils.loadCubeMap("cubeMap");

		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
		gl.glBindTexture(GL_TEXTURE_2D, groundTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
	}

	private void installLights() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		lightPos[0] = currentLightPos.x();
		lightPos[1] = currentLightPos.y();
		lightPos[2] = currentLightPos.z();

		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");

		// set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}

	private void setupVertices() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		numObjVerticesPyramid = pyramidModel.getNumVertices();
		Vector3f[] pyramidVertices = pyramidModel.getVertices();
		Vector2f[] pyramidTexCoords = pyramidModel.getTexCoords();
		Vector3f[] pyramidNormals = pyramidModel.getNormals();

		float[] pyramidPvalues = new float[numObjVerticesPyramid * 3];
		float[] pyramidTvalues = new float[numObjVerticesPyramid * 2];
		float[] pyramidNvalues = new float[numObjVerticesPyramid * 3];

		for (int i = 0; i < numObjVerticesPyramid; i++) {
			pyramidPvalues[i * 3] = (float) (pyramidVertices[i]).x();
			pyramidPvalues[i * 3 + 1] = (float) (pyramidVertices[i]).y();
			pyramidPvalues[i * 3 + 2] = (float) (pyramidVertices[i]).z();
			pyramidTvalues[i * 2] = (float) (pyramidTexCoords[i]).x();
			pyramidTvalues[i * 2 + 1] = (float) (pyramidTexCoords[i]).y();
			pyramidNvalues[i * 3] = (float) (pyramidNormals[i]).x();
			pyramidNvalues[i * 3 + 1] = (float) (pyramidNormals[i]).y();
			pyramidNvalues[i * 3 + 2] = (float) (pyramidNormals[i]).z();
		}

		float[] cubeVertexPositions = { -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f,
				-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f
		};

		// ---------------------- Axis Lines ----------------------
		float[] axisVertices = {
				// X
				0.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,

				// Y
				0.0f, 0.0f, 0.0f,
				0.0f, 1.0f, 0.0f,

				// Z
				0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f
		};

		float[] axisTexCoords = {
				// X
				1.0f, 0.0f,
				1.0f, 0.0f,

				// Y
				0.0f, 1.0f,
				0.0f, 1.0f,

				// Z
				0.0f, 0.0f,
				0.0f, 0.0f
		};

		// ---------------------- Ground ----------------------
		float[] groundVertices = {
				-10.0f, 0.0f, -10.0f, // Bottom-left
				10.0f, 0.0f, -10.0f, // Bottom-right
				-10.0f, 0.0f, 10.0f, // Top-left
				10.0f, 0.0f, -10.0f, // Bottom-right
				10.0f, 0.0f, 10.0f, // Top-right
				-10.0f, 0.0f, 10.0f // Top-left
		};

		float[] groundTexCoords = {
				0.0f, 0.0f, // Bottom-left
				10.0f, 0.0f, // Bottom-right
				0.0f, 10.0f, // Top-left
				10.0f, 0.0f, // Bottom-right
				10.0f, 10.0f, // Top-right
				0.0f, 10.0f // Top-left
		};

		float[] groundNormals = {
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f
		};

		// ---------------------- Ufo ----------------------
		numObjVerticesUfo = ufoModel.getNumVertices();
		Vector3f[] ufovertices = ufoModel.getVertices();
		Vector2f[] ufotexCoords = ufoModel.getTexCoords();
		Vector3f[] ufonormals = ufoModel.getNormals();

		float[] pvaluesufo = new float[numObjVerticesUfo * 3];
		float[] tvaluesufo = new float[numObjVerticesUfo * 2];
		float[] nvaluesufo = new float[numObjVerticesUfo * 3];

		for (int i = 0; i < numObjVerticesUfo; i++) {
			pvaluesufo[i * 3] = (float) (ufovertices[i]).x();
			pvaluesufo[i * 3 + 1] = (float) (ufovertices[i]).y();
			pvaluesufo[i * 3 + 2] = (float) (ufovertices[i]).z();
			tvaluesufo[i * 2] = (float) (ufotexCoords[i]).x();
			tvaluesufo[i * 2 + 1] = (float) (ufotexCoords[i]).y();
			nvaluesufo[i * 3] = (float) (ufonormals[i]).x();
			nvaluesufo[i * 3 + 1] = (float) (ufonormals[i]).y();
			nvaluesufo[i * 3 + 2] = (float) (ufonormals[i]).z();
		}

		// ---------------------- Car ----------------------
		numObjVerticesCow = cowModel.getNumVertices();
		Vector3f[] cowvertices = cowModel.getVertices();
		Vector2f[] cowtexCoords = cowModel.getTexCoords();
		Vector3f[] cownormals = cowModel.getNormals();

		float[] pvaluescow = new float[numObjVerticesCow * 3];
		float[] tvaluescow = new float[numObjVerticesCow * 2];
		float[] nvaluescow = new float[numObjVerticesCow * 3];

		for (int i = 0; i < numObjVerticesCow; i++) {
			pvaluescow[i * 3] = (float) (cowvertices[i]).x();
			pvaluescow[i * 3 + 1] = (float) (cowvertices[i]).y();
			pvaluescow[i * 3 + 2] = (float) (cowvertices[i]).z();
			tvaluescow[i * 2] = (float) (cowtexCoords[i]).x();
			tvaluescow[i * 2 + 1] = (float) (cowtexCoords[i]).y();
			nvaluescow[i * 3] = (float) (cownormals[i]).x();
			nvaluescow[i * 3 + 1] = (float) (cownormals[i]).y();
			nvaluescow[i * 3 + 2] = (float) (cownormals[i]).z();
		}

		// --------------------------------------------
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		// ---------------------- Ground ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer groundBuf = Buffers.newDirectFloatBuffer(groundVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, groundBuf.limit() * 4, groundBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer groundTex = Buffers.newDirectFloatBuffer(groundTexCoords);
		gl.glBufferData(GL_ARRAY_BUFFER, groundTex.limit() * 4, groundTex, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]); // Use an unused VBO slot
		FloatBuffer groundNor = Buffers.newDirectFloatBuffer(groundNormals);
		gl.glBufferData(GL_ARRAY_BUFFER, groundNor.limit() * 4, groundNor, GL_STATIC_DRAW);

		// ---------------------- Axis Lines ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer axisVertBuf = Buffers.newDirectFloatBuffer(axisVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, axisVertBuf.limit() * 4, axisVertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer axisTexBuf = Buffers.newDirectFloatBuffer(axisTexCoords);
		gl.glBufferData(GL_ARRAY_BUFFER, axisTexBuf.limit() * 4, axisTexBuf, GL_STATIC_DRAW);

		// ---------------------- Ufo ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvaluesufo);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvaluesufo);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvaluesufo);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL_STATIC_DRAW);

		// ---------------------- Cow ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer vertBufCow = Buffers.newDirectFloatBuffer(pvaluescow);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBufCow.limit() * 4, vertBufCow, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer texBufCow = Buffers.newDirectFloatBuffer(tvaluescow);
		gl.glBufferData(GL_ARRAY_BUFFER, texBufCow.limit() * 4, texBufCow, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		FloatBuffer norBufCow = Buffers.newDirectFloatBuffer(nvaluescow);
		gl.glBufferData(GL_ARRAY_BUFFER, norBufCow.limit() * 4, norBufCow, GL_STATIC_DRAW);

		// ---------------------- Cube ----------------------
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		FloatBuffer cvertBuf = Buffers.newDirectFloatBuffer(cubeVertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cvertBuf.limit() * 4, cvertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]); // Assuming VBOs 0-11 are already used
		FloatBuffer pyramidVertBuf = Buffers.newDirectFloatBuffer(pyramidPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, pyramidVertBuf.limit() * 4, pyramidVertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		FloatBuffer pyramidTexBuf = Buffers.newDirectFloatBuffer(pyramidTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, pyramidTexBuf.limit() * 4, pyramidTexBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		FloatBuffer pyramidNorBuf = Buffers.newDirectFloatBuffer(pyramidNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, pyramidNorBuf.limit() * 4, pyramidNorBuf, GL_STATIC_DRAW);

	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		float speed = 0.1f;

		switch (e.getKeyCode()) {
			case KeyEvent.VK_SPACE:
				if (visibleAxis) {
					axesX += 10f;
				} else {
					axesX -= 10f;
				}
				visibleAxis = !visibleAxis;
				break;
			case KeyEvent.VK_X:
				explodeMode = !explodeMode;
				break;
			case KeyEvent.VK_V:
				stereoscopicMode = !stereoscopicMode;
				aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
				if (!stereoscopicMode) {
					pMat.setPerspective((float) Math.toRadians(60.0f), aspect, near, far);
				}
				break;
			case KeyEvent.VK_I:
				ufoPositionZ -= speed;
				break;
			case KeyEvent.VK_K:
				ufoPositionZ += speed;
				break;
			case KeyEvent.VK_J:
				ufoPositionX -= speed;
				break;
			case KeyEvent.VK_L:
				ufoPositionX += speed;
				break;

			default:
				// camera controls: W, A, S, D, Q, E, and arrow keys
				camera.handleKeyInput(e.getKeyCode(), (float) deltaTime);
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	public static void main(String[] args) {
		new Code();
	}

	public void dispose(GLAutoDrawable drawable) {
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		sizeX = width;
		sizeY = height;
		aspect = (float) width / (float) height;
		if (stereoscopicMode) {
		} else {
			pMat.setPerspective((float) Math.toRadians(60.0f), aspect, near, far);
		}
	}

}