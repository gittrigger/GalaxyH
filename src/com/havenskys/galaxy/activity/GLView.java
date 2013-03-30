package com.havenskys.galaxy.activity;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class GLView extends SurfaceView implements SurfaceHolder.Callback {

	private GLThread mGLThread;
	private static String TAG = "GLView";
	
	public GLView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		
		getHolder().addCallback(this);
		
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.w(TAG,"surfaceChanged()");
		// TODO Auto-generated method stub
	}

	public void surfaceCreated(SurfaceHolder holder) {
		mGLThread = new GLThread(this);
		mGLThread.start();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		mGLThread.requestExitAndWait();
		mGLThread = null;
		
	}
	

}
