package git.artdeell.dnbootstrap;

import android.view.SurfaceHolder;

import git.artdeell.dnbootstrap.glfw.GLFW;

public class NativeSurfaceListener implements SurfaceHolder.Callback {

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        GLFW.nativeSurfaceUpdated();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        GLFW.nativeSurfaceCreated(surfaceHolder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        GLFW.nativeSurfaceDestroyed();
    }
}
