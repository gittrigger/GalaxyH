package com.havenskys.galaxy.activity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

class GLCube {

	private final IntBuffer mVertexBuffer;
	private final IntBuffer mTextureBuffer;
	
	public GLCube() {

		int one = 65536;
		int half = one / 2;
		int vertices[] = {
				// FRONT
				-half, -half, half, half, -half, half,
				-half, half, half, half, half, half, 
				// BACK
				-half, -half, -half, -half, half, -half, 
				half, -half, -half, half, half, -half, 
				// LEFT
				-half, -half, half, -half, half, half, 
				-half, -half, -half, -half, half, -half, 
				// RIGHT
				half, -half, -half, half, half, -half, 
				half, -half, half, half, half, half, 
				// TOP
				-half, half, half, half, half, half, 
				-half, half, -half, half, half, -half, 
				// BOTTOM
				-half, -half, half, -half, -half, -half, 
				half, -half, half, half, -half, -half
		};
		
		/*
		int texCoords[] = {
				// FRONT
				0,one,one,one,0,0,one,0,
				// BACK
				one,one,one,0,0,one,0,0,
				// LEFT
				one,one,one,0,0,one,0,0,
				// RIGHT
				one,one,one,0,0,one,0,0,
				// TOP
				one,0,0,0,one,one,0,one,
				// BOTTOM
				0,0,0,one,one,0,one,one
		};
		//*/
		
		int texCoords[] = {
				// FRONT
				0,one,one,one,0,0,one,0,
				// LEFT
				//one,one,one,0,0,one,0,0,
		};
		

		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asIntBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);
		
		
		
		ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
		tbb.order(ByteOrder.nativeOrder());
		mTextureBuffer = tbb.asIntBuffer();
		mTextureBuffer.put(texCoords);
		mTextureBuffer.position(0);
		
		
		
		
	}
	
	static void loadTexture(GL10 gl, Context context, int resource){
		Log.w("GLCube","loadTexture");
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resource);
		
		ByteBuffer bb = extract(bmp);
		
		load(gl, bb, bmp.getWidth(), bmp.getHeight());
		
	}
	
	private static void load(GL10 gl, ByteBuffer bb, int width, int height) {
		int[] tmp_tex = new int[1];
		gl.glGenTextures(1, tmp_tex, 0);
		int tex = tmp_tex[0];
		
		gl.glBindTexture(GL10.GL_TEXTURE_2D, tex);
		gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, width, height, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		
	}

	private static ByteBuffer extract(Bitmap bmp) {
		ByteBuffer bb = ByteBuffer.allocateDirect(bmp.getHeight() * bmp.getWidth() * 4);
		bb.order(ByteOrder.BIG_ENDIAN);
		IntBuffer ib = bb.asIntBuffer();
		
		//Conversion: ARGB - RGBA
		for( int y = bmp.getHeight() - 1; y > -1; y--){
			for( int x = bmp.getWidth() - 1; x > -1; x--){
				int pix = bmp.getPixel(x, bmp.getHeight() - y - 1);
				int red = ((pix >> 16) & 0xFF);
				int green = ((pix >> 8) & 0xFF);
				int blue = ((pix) & 0xFF);
				
				ib.put(red << 24 | green << 16 | blue << 8 | ((red + blue + green)/3));
			}
		}
		bb.position(0);
		return bb;
	}

	public void draw(GL10 gl){
		
		gl.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);

		gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, mTextureBuffer);
		
		gl.glColor4f(.9f, .9f, .9f, 1);
		gl.glNormal3f(0, 0, 1);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		gl.glNormal3f(0, 0, -1);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 4, 4);
		
		/*
		gl.glColor4f(.9f, .9f, .9f, 1);
		gl.glNormal3f(0, 0, -1);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		gl.glNormal3f(0, 0, 1);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 4, 4);
		//*/
		
		gl.glColor4f(.9f, .9f, .9f, 1);
		gl.glNormal3f(-1, 0, 0);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 8, 4);
		gl.glNormal3f(1, 0, 0);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 12, 4);
		
		/*
		gl.glColor4f(.9f, .9f, .9f, 1);
		gl.glNormal3f(1, 0, 0);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 8, 4);
		gl.glNormal3f(-1, 0, 0);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 12, 4);
		//*/
		
		gl.glColor4f(.9f, .9f, .9f, 1);
		gl.glNormal3f(0, 1, 0);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 16, 4);
		gl.glNormal3f(0, -1, 0);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 20, 4);
		
		/*
		gl.glColor4f(.9f, .9f, .9f, 1);
		gl.glNormal3f(0, -1, 0);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 16, 4);
		gl.glNormal3f(0, 1, 0);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 20, 4);		
		//*/
	}
}
