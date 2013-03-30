package com.havenskys.galaxy.activity;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.opengl.GLU;
import android.util.Log;

class GLThread extends Thread {

	private final GLView mView;
	private boolean mDone = false;
	private final GLCube mCube = new GLCube();
	private final GLCube mCube2 = new GLCube();
	
	
	private long mStartTime;
	//private float[][] mLocation;
	private Object[][] mThing;
	private int[] mLocation;
	private GL10 mGL;
	
	private long mElapsed;
	private static String TAG = "GL Thread";
	private long mTime;
	private int mFrameCount;
	
	
	private static int THING_SIZE = 2;
	// 0: GL10
	// 1: Location/Target/Speed int[10] 

	private final static int ID = 0; 		// mThing[0] = GL10
	private final static int LOCATION = 1;	// mThing[1] = int[10]
	
	private final static int X      = 1;	// mThing[1][1] = int
	private final static int TO_X   = 2;	// mThing[1][2] = int
	private final static int XSPEED = 3;	// mThing[1][3] = int
	private final static int XMOVE  = 4;	// mThing[1][4] = int
	
	private final static int Y      = 5;	// mThing[1][5] = int
	private final static int TO_Y   = 6;	// mThing[1][6] = int
	private final static int YSPEED = 7;	// mThing[1][7] = int
	private final static int YMOVE  = 8;	// mThing[1][8] = int
	
	private final static int Z      = 9;	// mThing[1][9] = int
	private final static int TO_Z   = 10;	// mThing[1][10] = int
	private final static int ZSPEED = 11;	// mThing[1][11] = int
	private final static int ZMOVE  = 12;	// mThing[1][12] = int
		
	
	GLThread(GLView view){
		mView = view;
	}

	@Override
	public void run() {
		Log.w(TAG,"run()");
		//super.run();
		
		mTime = System.currentTimeMillis() - mStartTime;
		mFrameCount = 0;
		
		EGL10 egl = (EGL10) EGLContext.getEGL();
		EGL10 egl2 = (EGL10) EGLContext.getEGL();
		
		EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
		EGLDisplay display2 = egl2.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
		
		int[] version = new int[2];
		egl.eglInitialize(display, version);
		egl2.eglInitialize(display2, version);
		
		int[] configSpec = {
				EGL10.EGL_RED_SIZE, 5,
				EGL10.EGL_GREEN_SIZE, 6, 
				EGL10.EGL_BLUE_SIZE, 5,
				EGL10.EGL_DEPTH_SIZE, 16,
				EGL10.EGL_NONE
		};
		
		EGLConfig[] configs = new EGLConfig[1];
		int[] numConfig = new int[1];
		egl.eglChooseConfig(display, configSpec, configs, 1, numConfig);
		egl2.eglChooseConfig(display2, configSpec, configs, 1, numConfig);
		EGLConfig config = configs[0];
		
		EGLContext glc = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, null);
		EGLContext glc2 = egl2.eglCreateContext(display2, config, EGL10.EGL_NO_CONTEXT, null);
		
		EGLSurface surface = egl.eglCreateWindowSurface(display, config, mView.getHolder(), null);
		egl.eglMakeCurrent(display, surface, surface, glc);
		
		//EGLSurface surface2 = egl2.eglCreateWindowSurface(display2, config, mView.getHolder(), null);
		egl2.eglMakeCurrent(display2, surface, surface, glc2);
		
		
		mTime = System.currentTimeMillis();
		
		//mLocation = new float[4][7];
		//mThing    = new GL10[4];
		mThing    = new Object[4][THING_SIZE];

		mThing[0][0] = (GL10) (glc.getGL());
		mThing[0][1] = new int[] { 0, 
			0, 0, 80, 1, //X toX MoveSpeed
			0, 0, 80, 1, //Y toY MoveSpeed
			-40000, -40000, 80, 1 //Z toZ MoveSpeed
		};
		init((GL10) mThing[0][0]); // Distance
		
		mThing[1][0] = (GL10) (glc2.getGL());
		mThing[1][1] = new int[] { 0, 
			4650, 0, 120, 1, //X toX MoveSpeed
			-14150, 0, 120, 1, //Y toY MoveSpeed
			0, 36000, 120, 1 //Z toZ MoveSpeed
		};
		//init((GL10) mThing[1][0]);
		
		
		//GLCube.loadTexture( (GL10) mThing[1][0], mView.getContext(), R.drawable.ic_dialog_info);
		
		
		/*
		mThing[2][0] = (GL10) (glc.getGL());
		mThing[2][1] = new int[] { 0, 
				0, 2300, 200, 1, //X toX MoveSpeed
				0, -1300, 200, 1, //Y toY MoveSpeed
				0, -1300, 200, 1 //Z toZ MoveSpeed
				};
		//*/
		
		
	
		
		//init((GL10) mThing[2][0]);
		
		/*
		mThing[3][0] = (GL10) (glc.getGL());
		mThing[3][1] = new int[] { 0, 
				0, 2000, 80, 1, //X toX MoveSpeed
				0, 0, 80, 1, //Y toY MoveSpeed
				-2000, 8000, 80, 1 //Z toZ MoveSpeed
			};
		init((GL10) mThing[3][0]);
		//*/
		
		
		boolean seaView = true;
		for( int i = 2; i > 0; i --){
			if( mThing[i][0] == null ){ continue; }
			mGL = (GL10) mThing[i][0];
			
			mGL.glEnable(GL10.GL_DEPTH_TEST);
			mGL.glDepthFunc(GL10.GL_LEQUAL);
			mGL.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			
		
			if( seaView && false ){
				mGL.glDisable(GL10.GL_DEPTH_TEST);
				mGL.glEnable(GL10.GL_BLEND);
				mGL.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
			}
		
			mGL.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			mGL.glEnable(GL10.GL_TEXTURE_2D);
		}
		
		
		
		while(!mDone){
			drawFrame();
			
			egl.eglSwapBuffers(display, surface);
			
			if( egl.eglGetError() == EGL11.EGL_CONTEXT_LOST){
				Context c = mView.getContext();
				if( c instanceof Activity){
					((Activity) c).finish();
				}
			}
		}
		
		
		egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
		egl.eglDestroySurface(display, surface);
		egl.eglDestroyContext(display, glc);
		egl.eglTerminate(display);
		
	}
	
	
	
	public void moveTo(int id, int toX, int toY, int toZ){
		Log.w(TAG,"moveto()");
		
		mLocation = (int[]) mThing[id][LOCATION];
		
		mLocation[TO_X] = toX;
		mLocation[XMOVE] = 1;
		
		mLocation[TO_Y] = toY;
		mLocation[YMOVE] = 1;
		
		mLocation[TO_Z] = toZ;
		mLocation[ZMOVE] = 1;
		
		/*
		mThing[id][LOCATION] = new int[] { 0, 
				loc[X], toX, loc[XSPEED], //X toX MoveSpeed
				loc[Y], toY, loc[YSPEED], //Y toY MoveSpeed
				loc[Z], toZ, loc[ZSPEED] //Z toZ MoveSpeed
			};
		//*/
		
		mThing[id][LOCATION] = mLocation;
		
	}

	private void init(GL10 gl) { 
		Log.w(TAG,"init()");
		
				
		mStartTime = System.currentTimeMillis();
		
		gl.glViewport(0, 0, mView.getWidth(), mView.getHeight());
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
	
		float width = (float) mView.getWidth();
		float height = (float) mView.getHeight();
		
		float ratio = 0;
		//if( width < height ){
			ratio = width / height;
		//} else {
			//ratio = height / width;
		//}
		GLU.gluPerspective(gl, 45.0f, ratio, 1, 200f);
		
		
		//mX = -3.4f; // left right
		//mY = -5.1f; // up down
		//mZ = -15.0f; // back and up to face
		//mTargetX = mX; mTargetY = mY; mTargetZ = mZ;
	
		
		// Lighting
		float lightAmbient[] = new float[] { 
				 0.2f, 0.2f, 0.2f, 0.0f
				,0.2f, 0.2f, 1, 0.0f
				,0.2f, 0.2f, 0.2f, 0.1f
				};
		float lightDiffuse[] = new float[] { 
				     1,    0,  0,  0.5f
				, 0.0f, 0.0f,  1,  0.5f
				, 0.8f, 0.8f,  0.0f,  0.5f
				};
		float[] lightPos = new float[] { 
				   0,    -60,   30, 1
				,  0,    60,  -30, 1
				,  0,    0,  0, 1
				};
		//huh?
		
		//gl.glEnable(GL10.GL_FOG);
		//gl.glFogfv(GL10.GL_FOG, lightPos, mAt)
		//gl.glEnable(GL10.GL_FOG_DENSITY);
		
		gl.glEnable(GL10.GL_LIGHTING);
		
		gl.glEnable(GL10.GL_LIGHT0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, lightAmbient, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDiffuse, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPos, 0);
	
		gl.glEnable(GL10.GL_LIGHT1);
		gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_AMBIENT, lightAmbient, 1);
		gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_DIFFUSE, lightDiffuse, 1);
		gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_POSITION, lightPos, 1);
	
		gl.glEnable(GL10.GL_LIGHT2);
		gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_AMBIENT, lightAmbient, 2);
		gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_DIFFUSE, lightDiffuse, 2);
		gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_POSITION, lightPos, 2);
		
		

        //exponent propertie defines the concentration of the light
        gl.glLightf(GL10.GL_LIGHT2, GL10.GL_SPOT_EXPONENT, 15.0f);
        
        //light attenuation (default values used here : no attenuation with the distance)
        gl.glLightf(GL10.GL_LIGHT2, GL10.GL_CONSTANT_ATTENUATION, 1.0f);
        gl.glLightf(GL10.GL_LIGHT2, GL10.GL_LINEAR_ATTENUATION, 0.0f);
        gl.glLightf(GL10.GL_LIGHT2, GL10.GL_QUADRATIC_ATTENUATION, 0.0f);
		
		
	}

	

	private int mAt;
	private int mTo;
	private int mOne;
	private int mSpinner;
	private float mModifier;
	private float mBow;
	
	public void move(int id){
		//Log.w(TAG,"move()");
		
		mLocation = (int[]) mThing[id][LOCATION];
		
		mOne = 0;
		
		for( mSpinner = 1; mSpinner < 10; mSpinner += 4){
			mAt = mSpinner;
			mTo = mSpinner + 1;

			if (mLocation[mTo] >= 0){
				if ( mLocation[mAt] >= 0 ){
					mLocation[mAt+3] = ( mLocation[mTo] > mLocation[mAt]) ? mLocation[mTo] - mLocation[mAt] : mLocation[mAt] - mLocation[mTo]; 
				}else{
					// X < 0
					mLocation[mAt+3] = mLocation[mTo] + mLocation[mAt]*-1;
				}
			}else{
				// mTo < 0
				if ( mLocation[mAt] >= 0 ){
					mLocation[mAt+3] = mLocation[mTo]*-1 + mLocation[mAt];
				}else{
					// X < 0
					mLocation[mAt+3] = mLocation[mTo] + mLocation[mAt]*-1;
					mLocation[mAt+3] = ( mLocation[mTo] < mLocation[mAt]) ? mLocation[mTo]*-1 - mLocation[mAt]*-1 : mLocation[mAt]*-1 - mLocation[mTo]*-1;
				}
			}
			if( mLocation[mAt+3] > mOne ){ mOne = mLocation[mAt+3]; } 
		}
		if( mOne == 0 ){
			return;
		}
		
		

		if( mLocation[X] != mLocation[TO_X] ){
			mModifier = (float) mLocation[XMOVE]/mOne;
			mBow = 1;
			if( mLocation[XMOVE] != mOne ){ mBow = 1.001f; }
			if( mLocation[TO_X] > mLocation[X] +mLocation[XSPEED]*mModifier  ){ mLocation[X] += mLocation[XSPEED]*mModifier*mBow; } 
			else if( mLocation[TO_X] < mLocation[X] -mLocation[XSPEED]*mModifier ){ mLocation[X] -= mLocation[XSPEED]*mModifier*mBow; }
			
			else if( mLocation[XMOVE] <= mLocation[XSPEED]*mModifier ) {
				mLocation[X] = mLocation[TO_X];
				Log.w(TAG,"XMOVE <= XSPEED*modifier");
				}
			
			else {
				mLocation[X] += mLocation[TO_X]*mBow;
				Log.w(TAG,"XMOVE else");
			}
		}
		
		if( mLocation[Y] != mLocation[TO_Y] ){
			mModifier = (float) mLocation[YMOVE]/mOne;
			mBow = 1;
			if( mLocation[YMOVE] != mOne ){ mBow = 3.001f; }
			if( mLocation[TO_Y] > mLocation[Y] +mLocation[YSPEED]*mModifier  ){ mLocation[Y] += mLocation[YSPEED]*mModifier*mBow; } 
			else if( mLocation[TO_Y] < mLocation[Y] -mLocation[YSPEED]*mModifier ){ mLocation[Y] -= mLocation[YSPEED]*mModifier*mBow; }
			
			else if( mLocation[YMOVE] <= mLocation[YSPEED]*mModifier ){
				mLocation[Y] = mLocation[TO_Y];
				Log.w(TAG,"YMOVE <= YSPEED*modifier");
				}
			
			else {
				mLocation[Y] += mLocation[TO_Y]*mBow;
				}
			
		}

		if( mLocation[Z] != mLocation[TO_Z] ){
			mModifier = (float) mLocation[ZMOVE]/mOne;
			mBow = 1;
			//if( mLocation[ZMOVE] != mOne ){ mBow = 1.001f; }
			if( mLocation[TO_Z] > mLocation[Z] +mLocation[ZSPEED]*mModifier  ){ mLocation[Z] += mLocation[ZSPEED]*mModifier*mBow; }
			else if( mLocation[TO_Z] < mLocation[Z] -mLocation[ZSPEED]*mModifier ){ mLocation[Z] -= mLocation[ZSPEED]*mModifier*mBow; }
			else if( mLocation[ZMOVE] <= mLocation[ZSPEED]*mModifier-1 ){mLocation[Z] = mLocation[TO_Z]; }
			//else { mLocation[Z] += (float) mLocation[TO_Z]*mBow; }
		}


		/*
		if( mLocation[XMOVE] < 0 ){
			mLocation[XMOVE] = (mLocation[TO_X] < mLocation[X]) ? (mLocation[TO_X]+mLocation[X]):(mLocation[X]+mLocation[TO_X]); 
		}else{
			mLocation[XMOVE] = (mLocation[TO_X] > mLocation[X]) ? (mLocation[TO_X]-mLocation[X]):(mLocation[X]-mLocation[TO_X]); 
		}
		mLocation[XMOVE] = (mLocation[XMOVE] < 0) ? mLocation[XMOVE] : mLocation[XMOVE] * -1;
		
		if( mLocation[XMOVE] < 0 ){
			
		}else{
			mLocation[YMOVE] = (mLocation[TO_Y] > mLocation[Y]) ? (mLocation[TO_Y]-mLocation[Y]):(mLocation[Y]-mLocation[TO_Y]); 
		}
		mLocation[YMOVE] = (mLocation[YMOVE] < 0) ? mLocation[YMOVE] : mLocation[YMOVE] * -1;
		
		if( mLocation[XMOVE] < 0 ){
			
		}else{
			mLocation[ZMOVE] = (mLocation[TO_Z] > mLocation[Z]) ? (mLocation[TO_Z]-mLocation[Z]):(mLocation[Z]-mLocation[TO_Z]); 
		}
		mLocation[ZMOVE] = (mLocation[ZMOVE] < 0) ? mLocation[ZMOVE] : mLocation[ZMOVE] * -1;
		//*/
		
		mThing[id][LOCATION] = mLocation;

		//Log.w(TAG,"Move "+ id +" X:" + ((float) mLocation[X]/1000) + " Y:" + ( (float) mLocation[Y]/1000) + " Z:" + ( (float) mLocation[Z]/1000) );
		
		//mGL.glTranslatef((float) mLocation[X]/1000, (float) mLocation[Y]/1000, (float) mLocation[Z]/1000);
		//mGL.glRotatef(mElapsed * (30f / 1000f) , 0, 1, 0);
		//mGL.glRotatef(mElapsed * (15f / 1000f) , 1, 0, 0);
		//mCube.draw(mGL);

	}



	
	
	private void drawFrame() {
		
		
		//Log.w(TAG,"drawFrame()");
		
		long thisTime = (long) System.currentTimeMillis()/1000;
		if( mTime == thisTime ){
			mFrameCount++;
		}else if( mFrameCount > 0 ){
			Log.w(TAG,"FrameRate " + mFrameCount + " per second");
			mFrameCount = 0;
			mTime = thisTime;
		}
		mElapsed = System.currentTimeMillis() - mStartTime;
		

		move(0);
		mGL = (GL10) mThing[0][0];
		mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		mGL.glMatrixMode(GL10.GL_MODELVIEW);
		mGL.glLoadIdentity();
		mLocation = (int[]) mThing[0][LOCATION];
		mGL.glTranslatef((float) mLocation[X]/1000, (float) mLocation[Y]/1000, (float) mLocation[Z]/1000);
		
		//mGL.glTranslatef(0, 0, -10);
		//mCube.draw(mGL);
		
		
		for( int i = 1; i < mThing.length; i ++ ){
			if( mThing[i] == null ){ continue; }
			if( mThing[i][1] == null ){ continue; }
			
			//mGL = (GL10) mThing[i][0];
			//mGL.glTranslatef(0, 0, 6);
			
			//mGL = (GL10) mThing[0][0];
			//mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			//mGL.glMatrixMode(GL10.GL_MODELVIEW);
			//mGL.glLoadIdentity();
		
			//mGL = (GL10) mThing[0][0];
			//mGL.glClear(GL10.GL_);
			


			

			mGL = (GL10) mThing[i][0];
			//mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			
			mGL.glMatrixMode(GL10.GL_MODELVIEW);
			
			move(i);

			
			//mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			//mGL.glMatrixMode(GL10.GL_MODELVIEW);
			//mGL.glLoadIdentity();
			
			
			
			
			
			/*
			mGL = (GL10) mThing[2][0];
			
			//mGL.glEnable(GL10.GL_DEPTH_TEST);
			//mGL.glDepthFunc(GL10.GL_LEQUAL);
			mGL.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			
		
			if( seaView ){
				mGL.glDisable(GL10.GL_DEPTH_TEST);
				mGL.glEnable(GL10.GL_BLEND);
				mGL.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
			}
		
			mGL.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			mGL.glEnable(GL10.GL_TEXTURE_2D);
			//*/
			//gl.glDisable(GL10.GL_DITHER);

			
			//mGL.glMatrixMode(GL10.GL_MODELVIEW);
			//mGL.glLoadIdentity();
			
			/*
			// Material
			float matAmbient[] = new float[] {0.8f, 0.8f, 0.8f, 1};
			float matDiffuse[] = new float[] {0.8f, 0.8f, 0.8f, 1};
			
			mGL.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, matAmbient, 0);
			//mGL.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, matDiffuse, 0);
			//*/
		
			mLocation = (int[]) mThing[i][LOCATION];
			
			
			mGL.glTranslatef((float) mLocation[X]/1000, (float) mLocation[Y]/1000, (float) mLocation[Z]/1000);
	
			
			//if( i == 2 ){
				mGL.glRotatef(mElapsed * (30f / 1000f) , 0, 1, 0);
				mGL.glRotatef(mElapsed * (10f / 1000f) , 1, 0, 0);//15
			//}
	
			mCube.draw(mGL);
			
			
		}
		
/*		
		for( int i = 1; i < mThing.length; i ++ ){
			if( mThing[i] == null ){ continue; }
			if( mThing[i][1] == null ){ continue; }
	
			mGL = (GL10) mThing[i][0];
			
			//mGL.glTranslatef(0, 0, 6);
			
			if( i == 1 ){
				mCube2.draw(mGL);
			} else {
				//mGL.glRotatef(mElapsed * (30f / 1000f) , 0, 1, 0);
				//mGL.glRotatef(mElapsed * (30f / 1000f) , 1, 0, 0);//15				
				mCube2.draw(mGL);
			}
		}
//*/
		
		//gl.glTranslatef(mX, mY, mZ);
		//glSurface.glTranslatef(0, 0, -10f);
		
		//gl.glRotatef(mElapsed * (30f / 1000f) , 0, 1, 0);
		//gl.glRotatef(mElapsed * (15f / 1000f) , 1, 0, 0);
		
		//mCube.draw(gl);
		
		//gl.glTranslatef(mX+3, mY+3, mZ+6);
		//mCube.draw(gl);

		//gl2.glTranslatef(-2, -2, 0);
		//gl2.glRotatef(mElapsed * (30f / 1000f) , 0, 1, 0);
		//gl2.glRotatef(mElapsed * (15f / 1000f) , 1, 0, 0);
		//mCube.draw(gl2);
		
		//gl3.glTranslatef(1, 1, 0);
		//gl3.glRotatef(mElapsed * (30f / 1000f) , 0, 1, 0);
		//gl3.glRotatef(mElapsed * (15f / 1000f) , 1, 0, 0);
		
		//mCube.draw(gl3);
		
		
		
	}

	public void requestExitAndWait() {
		mDone = true;
		try {
			join();
		} catch (InterruptedException ex) {
			//Ignore
		}
	}

	private boolean mPaused = false;
	
    public void onPause() {
        synchronized (this) {
            mPaused = true;
        }
    }

    public void onResume() {
        synchronized (this) {
            mPaused = false;
            notify();
        }
    }

	
}
