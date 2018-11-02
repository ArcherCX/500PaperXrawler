package com.archer.s00paperxrawler.gl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView.Renderer;
import android.view.SurfaceHolder;

public abstract class OpenGLES2WallpaperService extends GLWallpaperService {
	@Override
	public Engine onCreateEngine() {
		return new OpenGLES2Engine();
	}
	
	public class OpenGLES2Engine extends GLWallpaperService.GLEngine {

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			
			// Check if the system supports OpenGL ES 2.0.
			final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
			final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
			
			if (supportsEs2) 
			{
				// Request an OpenGL ES 2.0 compatible context.
				setEGLContextClientVersion(2);
				// Set the renderer to our user-defined renderer.
				setRenderer(getNewRenderer());
				setRenderMode(OpenGLES2WallpaperService.this.getRenderMode());
			} 
			else 
			{
				// This is where you could create an OpenGL ES 1.x compatible
				// renderer if you wanted to support both ES 1 and ES 2.
				return;
			}			
		}
	}	
	
	protected abstract GLRenderer getNewRenderer();
	protected abstract int getRenderMode();
}
