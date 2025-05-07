package a4;

import javax.swing.JFrame;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CCW;
import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_DEPTH_ATTACHMENT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_COMPONENT32;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_FRAMEBUFFER;
import static com.jogamp.opengl.GL.GL_FRONT;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LESS;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_LINE_SMOOTH;
import static com.jogamp.opengl.GL.GL_NONE;
import static com.jogamp.opengl.GL.GL_POLYGON_OFFSET_FILL;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE1;
import static com.jogamp.opengl.GL.GL_TEXTURE2;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_COMPARE_REF_TO_TEXTURE;
import static com.jogamp.opengl.GL2ES2.GL_DEPTH_COMPONENT;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_COMPARE_FUNC;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_COMPARE_MODE;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_CUBE_MAP_SEAMLESS;

import java.nio.FloatBuffer;
import java.lang.Math;
import java.awt.event.*;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import org.joml.*;

public class Code extends JFrame implements GLEventListener, KeyListener {
	// game initialization variables
	private GLCanvas myCanvas;
	private int renderingProgram, renderingProgramCubeMap, renderingProgramShadow, renderingProgramNoTex;
	private int vao[] = new int[1];
	private int vbo[] = new int[30];
	private Camera camera;

	// variables for imported models and textures
	private int numObjVertices;
	private ImportedModel ufoModel, cowModel;
	private int skyboxTexture, groundTexture, ufoTexture, cowTexture;
	private int groundNormalMap;

	// display function variables
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();
	private Matrix4f vMat = new Matrix4f();
	private Matrix4f mMat = new Matrix4f();
	private Matrix4f invTrMat = new Matrix4f();
	private int mLoc, pLoc, vLoc, nLoc, acLoc;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private int sampLoc, normalMapLoc, shadowMapLoc;
	private float aspect;

	// time tracking variables
	private double startTime = 0;
	private double prevTime = 0;
	private double deltaTime = 0;
	private double elapsedTime = 0;
	private double currentTime = 0;

	// input state variables
	private boolean renderAxes = false;
	private boolean spaceWasPressed = false;

	// initial positions
	private Vector3f cameraLoc = new Vector3f(0, 1.75f, 5f);
	private Vector3f initialLightPos = new Vector3f(3.0f, 8.0f, 2.0f);
	private Vector3f initialUfoPos = new Vector3f(0, 2.0f, 0);
	private Vector3f initialCowPos = new Vector3f(0, 1.5f, 0);

	// for the light to point at
	private Vector3f origin = new Vector3f(0, 0, 0);
	private Vector3f upVec = new Vector3f(0, 1, 0);

	// Light control variables
	private float lightSpeed = 1f;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];
	private boolean lightToggleWasPressed = false;
	private boolean renderLight = true;

	// shadow stuff
	private int scSizeX, scSizeY;
	private int[] shadowTex = new int[1];
	private int[] shadowBuffer = new int[1];
	private Matrix4f lightVmat = new Matrix4f();
	private Matrix4f lightPmat = new Matrix4f();
	private Matrix4f shadowMVP = new Matrix4f();
	private Matrix4f b = new Matrix4f();
	private int sLoc;

	// white light properties
	private float[] globalAmbient = new float[] { 0.6f, 0.6f, 0.6f, 0.6f };
	private float[] lightAmbient = new float[] { 0.3f, 0.3f, 0.3f, 1.0f };
	private float[] lightDiffuse = new float[] { 1.0f, 1.0f, 0.9f, 1.0f };
	private float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

	// Material properties
	private float[] matAmb = Utils.silverAmbient();
	private float[] matDif = Utils.silverDiffuse();
	private float[] matSpe = Utils.silverSpecular();
	private float matShi = Utils.silverShininess();

	// custom cow material
	private float[] cowMatAmb = Utils.cowAmbient();
	private float[] cowMatDif = Utils.cowDiffuse();
	private float[] cowMatSpe = Utils.cowSpecular();
	private float cowMatShi = Utils.cowShininess();

	// animation variables
	private float ufoPositionX = 0.0f;
	private float ufoPositionY = 2.0f;
	private float ufoPositionZ = 0.0f;
	private float ufoRotationY = 0.0f;
	private float ufoRotationSpeed = 35.0f;
	private float cowRotationX = 0.0f;
	private float cowRotationY = 0.0f;
	private float cowRotationSpeed = 50.0f;

	public Code() {
		// setup window
		setTitle("CSC155 - Assignment 4");
		setSize(1200, 1200);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// setup GLCanvas
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		myCanvas.addKeyListener(this);

		this.add(myCanvas);
		this.setVisible(true);

		startTime = System.currentTimeMillis();
		prevTime = startTime;

		Animator animtr = new Animator(myCanvas);
		animtr.start();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		// Update time tracking
		currentTime = System.currentTimeMillis();
		elapsedTime = currentTime - startTime;
		deltaTime = (currentTime - prevTime) / 1000.0; // Convert to seconds
		prevTime = currentTime;

		// Update animation values
		ufoRotationY += ufoRotationSpeed * (float) deltaTime;
		cowRotationX += cowRotationSpeed * (float) deltaTime;
		cowRotationY += cowRotationSpeed * (float) deltaTime;

		// update light position
		currentLightPos.set(initialLightPos);

		// update camera's view matrix
		camera.updateViewMatrix();
		vMat = camera.getViewMatrix();

		// render all of the objects
		renderScene();
	}

	public void renderScene() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClearColor(0, 0.2f, 0.2f, 1);
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		// Setup light perspective
		lightVmat.identity().setLookAt(currentLightPos, origin, upVec);
		lightPmat.identity().setPerspective((float) Math.toRadians(60), aspect, 0.1f, 1000.0f);

		// Render shadow map
		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex[0], 0);

		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(3.0f, 5.0f);

		renderShadows();

		gl.glDisable(GL_POLYGON_OFFSET_FILL);

		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);

		gl.glDrawBuffer(GL_FRONT);

		// Main rendering pass
		drawSkybox();
		drawWorldAxes();
		drawLight();
		drawScene();
	}

	private void renderShadows() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(renderingProgramShadow);

		sLoc = gl.glGetUniformLocation(renderingProgramShadow, "shadowMVP");

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		// draw ground plane
		mMat.identity();

		shadowMVP.identity();
		shadowMVP.mul(lightPmat);
		shadowMVP.mul(lightVmat);
		shadowMVP.mul(mMat);

		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glDrawArrays(GL_TRIANGLES, 0, 6);

		// draw UFO
		mMat.identity();
		mMat.translate(ufoPositionX, ufoPositionY, ufoPositionZ);
		mMat.scale(0.5f);
		mMat.rotateY((float) Math.toRadians(ufoRotationY));

		shadowMVP.identity();
		shadowMVP.mul(lightPmat);
		shadowMVP.mul(lightVmat);
		shadowMVP.mul(mMat);

		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glDrawArrays(GL_TRIANGLES, 0, ufoModel.getNumVertices());

		// draw cow
		mMat.identity();
		mMat.translate(initialCowPos);
		mMat.scale(0.05f);
		mMat.rotateX((float) Math.toRadians(cowRotationX));
		mMat.rotateY((float) Math.toRadians(cowRotationY));
		mMat.rotateY((float) Math.toRadians(-90.0f));

		shadowMVP.identity();
		shadowMVP.mul(lightPmat);
		shadowMVP.mul(lightVmat);
		shadowMVP.mul(mMat);

		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glDrawArrays(GL_TRIANGLES, 0, cowModel.getNumVertices());
	}

	private void drawScene() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(renderingProgram);

		mLoc = gl.glGetUniformLocation(renderingProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");
		sLoc = gl.glGetUniformLocation(renderingProgram, "shadowMVP");
		sampLoc = gl.glGetUniformLocation(renderingProgram, "samp");

		sampLoc = gl.glGetUniformLocation(renderingProgram, "diffuseTexture");
		normalMapLoc = gl.glGetUniformLocation(renderingProgram, "normalMap");
		shadowMapLoc = gl.glGetUniformLocation(renderingProgram, "shadowMap");

		installLights(renderingProgram);

		// Set shadow map sampler
		gl.glUniform1i(shadowMapLoc, 1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		// Draw ground
		gl.glDisable(GL_CULL_FACE);

		mMat.identity();

		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		Matrix4f groundShadowMVP = new Matrix4f();
		groundShadowMVP.identity()
				.mul(b)
				.mul(lightPmat)
				.mul(lightVmat)
				.mul(mMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, groundShadowMVP.get(vals));

		// Ground vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Ground texture coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Ground normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Bind textures
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, groundTexture);
		gl.glUniform1i(sampLoc, 0);

		gl.glActiveTexture(GL_TEXTURE2);
		gl.glBindTexture(GL_TEXTURE_2D, groundNormalMap);
		gl.glUniform1i(normalMapLoc, 2);

		gl.glDrawArrays(GL_TRIANGLES, 0, 6);
		gl.glEnable(GL_CULL_FACE);

		// Draw UFO
		mMat.identity();
		mMat.translate(ufoPositionX, ufoPositionY, ufoPositionZ);
		mMat.scale(0.5f);
		mMat.rotateY((float) Math.toRadians(ufoRotationY));

		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		Matrix4f ufoShadowMVP = new Matrix4f();
		ufoShadowMVP.identity()
				.mul(b)
				.mul(lightPmat)
				.mul(lightVmat)
				.mul(mMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, ufoShadowMVP.get(vals));

		// UFO vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// UFO texture coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// UFO normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Bind textures
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, ufoTexture);
		gl.glUniform1i(sampLoc, 0);

		gl.glDrawArrays(GL_TRIANGLES, 0, ufoModel.getNumVertices());

		// Draw cow with custom material
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, cowMatAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, cowMatDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, cowMatSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, cowMatShi);

		mMat.identity();
		mMat.translate(initialCowPos);
		mMat.scale(0.05f);
		mMat.rotateX((float) Math.toRadians(cowRotationX));
		mMat.rotateY((float) Math.toRadians(cowRotationY));
		mMat.rotateY((float) Math.toRadians(-90.0f));

		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		Matrix4f cowShadowMVP = new Matrix4f();
		cowShadowMVP.identity()
				.mul(b)
				.mul(lightPmat)
				.mul(lightVmat)
				.mul(mMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, cowShadowMVP.get(vals));

		// Cow vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Cow texture coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// Cow normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// Bind textures
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, cowTexture);
		gl.glUniform1i(sampLoc, 0);

		gl.glDrawArrays(GL_TRIANGLES, 0, cowModel.getNumVertices());

		// Reset to default material
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}

	private void drawSkybox() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		// draw skybox cubemap
		gl.glUseProgram(renderingProgramCubeMap);

		vLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "v_matrix");
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));

		pLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "p_matrix");
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW); // cube is CW, but we are viewing the inside
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);
	}

	private void drawWorldAxes() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		if (!renderAxes)
			return;

		gl.glUseProgram(renderingProgramNoTex);

		mLoc = gl.glGetUniformLocation(renderingProgramNoTex, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgramNoTex, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgramNoTex, "p_matrix");
		acLoc = gl.glGetUniformLocation(renderingProgramNoTex, "axisColor");

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glEnable(GL_LINE_SMOOTH);
		gl.glLineWidth(3);

		mMat.identity();
		mMat.scale(5);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		// X axis (red)
		gl.glUniform3f(acLoc, 1.0f, 0.0f, 0.0f);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glDrawArrays(GL_LINES, 0, 2);

		// Y axis (green)
		gl.glUniform3f(acLoc, 0.0f, 1.0f, 0.0f);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glDrawArrays(GL_LINES, 0, 2);

		// Z axis (blue)
		gl.glUniform3f(acLoc, 0.0f, 0.0f, 1.0f);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glDrawArrays(GL_LINES, 0, 2);
	}

	private void drawLight() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		if (!renderLight)
			return;

		gl.glUseProgram(renderingProgramNoTex);

		mLoc = gl.glGetUniformLocation(renderingProgramNoTex, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgramNoTex, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgramNoTex, "p_matrix");
		acLoc = gl.glGetUniformLocation(renderingProgramNoTex, "axisColor");

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		// Draw a small indicator at light position
		mMat.identity();
		mMat.translate(currentLightPos);
		mMat.scale(0.3f);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniform3f(acLoc, 1.0f, 1.0f, 0.0f);

		// Use any model to visualize the light
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glDrawArrays(GL_TRIANGLES, 0, ufoModel.getNumVertices());
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		camera = new Camera();
		camera.setLocation(cameraLoc);
		camera.lookAt(0, 0, 0);

		// import models and create rendering program by compiling and linking shaders
		ufoModel = new ImportedModel("ufo.obj");
		cowModel = new ImportedModel("cow.obj");

		// Use your own vertex and fragment shaders
		renderingProgram = Utils.createShaderProgram("a4/blinnPhongShadowVert.glsl",
				"a4/blinnPhongShadowFrag.glsl");
		renderingProgramCubeMap = Utils.createShaderProgram("a4/skyboxVert.glsl",
				"a4/skyboxFrag.glsl");
		renderingProgramShadow = Utils.createShaderProgram("a4/shadowMapVert.glsl",
				"a4/shadowMapFrag.glsl");
		renderingProgramNoTex = Utils.createShaderProgram("a4/notex.vert", "a4/notex.frag");

		// set perspective matrix, only changes when screen is resized
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		// setup the vertex and texture information for all models in the scene
		setupVertices();
		setupShadowBuffers();

		b.set(
				0.5f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.5f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.5f, 0.0f,
				0.5f, 0.5f, 0.5f, 1.0f);

		// load all textures that will be used
		ufoTexture = Utils.loadTexture("ufo.png");
		cowTexture = Utils.loadTexture("cow.png");
		groundTexture = Utils.loadTexture("ground.jpg");
		groundNormalMap = Utils.loadTexture("ground_nmap.jpg");
		skyboxTexture = Utils.loadCubeMap("cubemap");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

		// Initialize light pos
		currentLightPos.set(initialLightPos);
	}

	private void setupVertices() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		// variables for all models within the scene
		FloatBuffer vertBuf, texBuf, normBuf, tanBuf;
		Vector3f[] vertices, normals, tangents;
		Vector2f[] texCoords;
		float[] pvalues, tvalues, nvalues, tanvalues;

		// Setup axis lines
		Vector3f origin = new Vector3f(0, 0, 0);
		Line worldXAxis = new Line(origin, new Vector3f(1, 0, 0));
		Line worldYAxis = new Line(origin, new Vector3f(0, 1, 0));
		Line worldZAxis = new Line(origin, new Vector3f(0, 0, 1));
		Cube skyBox = new Cube();

		// Ground plane vertices
		float[] groundVertices = {
				-10.0f, 0.0f, -10.0f,
				10.0f, 0.0f, -10.0f,
				-10.0f, 0.0f, 10.0f,
				10.0f, 0.0f, -10.0f,
				10.0f, 0.0f, 10.0f,
				-10.0f, 0.0f, 10.0f
		};

		// Ground texture coordinates
		float[] groundTexCoords = {
				0.0f, 0.0f,
				10.0f, 0.0f,
				0.0f, 10.0f,
				10.0f, 0.0f,
				10.0f, 10.0f,
				0.0f, 10.0f
		};

		// Ground normals
		float[] groundNormals = {
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f
		};

		// Ground tangents for normal mapping
		float[] groundTangents = {
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f
		};

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		// setup skybox
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		vertBuf = Buffers.newDirectFloatBuffer(skyBox.getVertices());
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		// setup world axes x, y, and z
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		vertBuf = Buffers.newDirectFloatBuffer(worldXAxis.getVertices());
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		vertBuf = Buffers.newDirectFloatBuffer(worldYAxis.getVertices());
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		vertBuf = Buffers.newDirectFloatBuffer(worldZAxis.getVertices());
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		// setup ground plane
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer groundBuf = Buffers.newDirectFloatBuffer(groundVertices);
		gl.glBufferData(GL_ARRAY_BUFFER, groundBuf.limit() * 4, groundBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer groundTex = Buffers.newDirectFloatBuffer(groundTexCoords);
		gl.glBufferData(GL_ARRAY_BUFFER, groundTex.limit() * 4, groundTex, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer groundNor = Buffers.newDirectFloatBuffer(groundNormals);
		gl.glBufferData(GL_ARRAY_BUFFER, groundNor.limit() * 4, groundNor, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer groundTan = Buffers.newDirectFloatBuffer(groundTangents);
		gl.glBufferData(GL_ARRAY_BUFFER, groundTan.limit() * 4, groundTan, GL_STATIC_DRAW);

		// Setup UFO model
		int numObjVerticesUfo = ufoModel.getNumVertices();
		vertices = ufoModel.getVertices();
		texCoords = ufoModel.getTexCoords();
		normals = ufoModel.getNormals();

		pvalues = new float[numObjVerticesUfo * 3];
		tvalues = new float[numObjVerticesUfo * 2];
		nvalues = new float[numObjVerticesUfo * 3];

		for (int i = 0; i < numObjVerticesUfo; i++) {
			pvalues[i * 3] = (float) (vertices[i]).x();
			pvalues[i * 3 + 1] = (float) (vertices[i]).y();
			pvalues[i * 3 + 2] = (float) (vertices[i]).z();

			tvalues[i * 2] = (float) (texCoords[i]).x();
			tvalues[i * 2 + 1] = (float) (texCoords[i]).y();

			nvalues[i * 3] = (float) (normals[i]).x();
			nvalues[i * 3 + 1] = (float) (normals[i]).y();
			nvalues[i * 3 + 2] = (float) (normals[i]).z();
		}

		// setup UFO model data
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		normBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, normBuf.limit() * 4, normBuf, GL_STATIC_DRAW);

		// Setup Cow model
		int numObjVerticesCow = cowModel.getNumVertices();
		vertices = cowModel.getVertices();
		texCoords = cowModel.getTexCoords();
		normals = cowModel.getNormals();

		pvalues = new float[numObjVerticesCow * 3];
		tvalues = new float[numObjVerticesCow * 2];
		nvalues = new float[numObjVerticesCow * 3];

		for (int i = 0; i < numObjVerticesCow; i++) {
			pvalues[i * 3] = (float) (vertices[i]).x();
			pvalues[i * 3 + 1] = (float) (vertices[i]).y();
			pvalues[i * 3 + 2] = (float) (vertices[i]).z();

			tvalues[i * 2] = (float) (texCoords[i]).x();
			tvalues[i * 2 + 1] = (float) (texCoords[i]).y();

			nvalues[i * 3] = (float) (normals[i]).x();
			nvalues[i * 3 + 1] = (float) (normals[i]).y();
			nvalues[i * 3 + 2] = (float) (normals[i]).z();
		}

		// setup cow model data
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		normBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, normBuf.limit() * 4, normBuf, GL_STATIC_DRAW);
	}

	private void setupShadowBuffers() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();

		gl.glGenFramebuffers(1, shadowBuffer, 0);

		gl.glGenTextures(1, shadowTex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
				scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	private void installLights(int renderingProgram) {
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

		if (renderLight) {
			gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
			gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		} else {
			float[] zeroLight = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
			gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, zeroLight, 0);
			gl.glProgramUniform4fv(renderingProgram, specLoc, 1, zeroLight, 0);
		}

		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Not used
	}

	@Override
	public void keyPressed(KeyEvent e) {
		float speed = 0.1f;

		switch (e.getKeyCode()) {
			case KeyEvent.VK_SPACE:
				// Toggle coordinate axes display
				renderAxes = !renderAxes;
				break;
			case KeyEvent.VK_P:
				// Toggle light on/off
				renderLight = !renderLight;
				break;
			case KeyEvent.VK_I:
				ufoPositionZ -= speed; // Move UFO forward (negative Z)
				break;
			case KeyEvent.VK_K:
				ufoPositionZ += speed; // Move UFO backward (positive Z)
				break;
			case KeyEvent.VK_J:
				ufoPositionX -= speed; // Move UFO left (negative X)
				break;
			case KeyEvent.VK_L:
				ufoPositionX += speed; // Move UFO right (positive X)
				break;
			case KeyEvent.VK_U:
				ufoPositionY += speed; // Move UFO up
				break;
			case KeyEvent.VK_O:
				ufoPositionY -= speed; // Move UFO down
				break;

			// Camera controls
			case KeyEvent.VK_W:
				camera.moveForward(speed);
				break;
			case KeyEvent.VK_S:
				camera.moveBackward(speed);
				break;
			case KeyEvent.VK_A:
				camera.strafeLeft(speed);
				break;
			case KeyEvent.VK_D:
				camera.strafeRight(speed);
				break;
			case KeyEvent.VK_Q:
				camera.moveUp(speed);
				break;
			case KeyEvent.VK_E:
				camera.moveDown(speed);
				break;
			case KeyEvent.VK_UP:
				camera.pitch(-speed * 30);
				break;
			case KeyEvent.VK_DOWN:
				camera.pitch(speed * 30);
				break;
			case KeyEvent.VK_LEFT:
				camera.yaw(speed * 30);
				break;
			case KeyEvent.VK_RIGHT:
				camera.yaw(-speed * 30);
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Not used
	}

	public static void main(String[] args) {
		new Code();
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// remake the perspective matrix when screen is resized, as aspect ratio may
		// have changed
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupShadowBuffers();
	}

	public void dispose(GLAutoDrawable drawable) {
		// Cleanup resources if needed
	}
}