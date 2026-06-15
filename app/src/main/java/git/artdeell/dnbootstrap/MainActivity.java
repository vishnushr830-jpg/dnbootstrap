package git.artdeell.dnbootstrap;

import static android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;

import java.io.File;

import git.artdeell.dnbootstrap.assets.AppDirs;
import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.KeyCodes;
import git.artdeell.dnbootstrap.glfw.MouseCodes;
import git.artdeell.dnbootstrap.input.ControlLayout;
import git.artdeell.dnbootstrap.input.SoftInputCallback;
import git.artdeell.dnbootstrap.input.TouchCharInput;
import git.artdeell.dnbootstrap.input.editor.ControlEditorLayout;
import git.artdeell.dnbootstrap.input.editor.LayoutEditorHost;
import git.artdeell.dnbootstrap.utils.InsetUtils;
import git.artdeell.dnbootstrap.utils.Utils;

public class MainActivity extends Activity implements SoftInputCallback, LayoutEditorHost {
    static {
        System.loadLibrary("glfw");
        GLFW.initialize();
        System.loadLibrary("dnbootstrap");
    }

    private TouchCharInput touchCharInput;
    private ControlLayout controlLayout;
    private View layoutEditor;

    private static boolean isRunning = false;

    // Track last mouse position for delta calculation
    private float lastMouseX = -1, lastMouseY = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView surfaceView = findViewById(R.id.surface_view);
        touchCharInput = findViewById(R.id.touch_char_input);
        controlLayout = findViewById(R.id.control_layout);
        InsetUtils.setInsetsMode(this, true, false);
        surfaceView.getHolder().addCallback(new NativeSurfaceListener());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerOnBackInvoked();
        }
        if(!isRunning) {
            isRunning = true;
            new Thread(this::kickstart).start();
        }
    }

    // ─── Physical Keyboard ───────────────────────────────────────────────────

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isPhysicalKeyboard(event)) {
            GLFW.sendRawKeyEvent(keyCode, KeyCodes.GLFW_PRESS, getGLFWMods(event));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isPhysicalKeyboard(event)) {
            GLFW.sendRawKeyEvent(keyCode, KeyCodes.GLFW_RELEASE, getGLFWMods(event));
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    // ─── Physical Mouse ───────────────────────────────────────────────────────

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (!isMouseEvent(event)) return super.onGenericMotionEvent(event);

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_HOVER_MOVE || action == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            if (GLFW.isGrabbing()) {
                // In-game: use relative movement
                float dx = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X);
                float dy = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y);
                if (dx == 0 && dy == 0 && lastMouseX >= 0) {
                    dx = x - lastMouseX;
                    dy = y - lastMouseY;
                }
                int w = controlLayout.getWidth();
                int h = controlLayout.getHeight();
                if (w > 0 && h > 0) {
                    GLFW.cursorX += dx / w;
                    GLFW.cursorY += dy / h;
                    GLFW.sendMousePos();
                }
            } else {
                // In menu: use absolute position
                int w = controlLayout.getWidth();
                int h = controlLayout.getHeight();
                if (w > 0 && h > 0) {
                    GLFW.cursorX = x / w;
                    GLFW.cursorY = y / h;
                    GLFW.sendMousePos();
                }
            }

            lastMouseX = x;
            lastMouseY = y;
            return true;
        }

        // Mouse scroll wheel
        if (action == MotionEvent.ACTION_SCROLL) {
            float scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            float scrollX = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
            GLFW.sendScrollEvent(scrollX, scrollY);
            return true;
        }

        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onMouseEvent(MotionEvent event) {
        if (!isMouseEvent(event)) return false;

        int action = event.getActionMasked();
        int glfwButton = -1;

        int androidButton = event.getActionButton();
        if (androidButton == MotionEvent.BUTTON_PRIMARY)   glfwButton = MouseCodes.GLFW_MOUSE_BUTTON_LEFT;
        else if (androidButton == MotionEvent.BUTTON_SECONDARY) glfwButton = MouseCodes.GLFW_MOUSE_BUTTON_RIGHT;
        else if (androidButton == MotionEvent.BUTTON_TERTIARY)  glfwButton = MouseCodes.GLFW_MOUSE_BUTTON_MIDDLE;

        if (glfwButton < 0) return false;

        if (action == MotionEvent.ACTION_BUTTON_PRESS) {
            GLFW.sendMouseEvent(glfwButton, KeyCodes.GLFW_PRESS, 0);
            return true;
        } else if (action == MotionEvent.ACTION_BUTTON_RELEASE) {
            GLFW.sendMouseEvent(glfwButton, KeyCodes.GLFW_RELEASE, 0);
            return true;
        }

        return false;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean isPhysicalKeyboard(KeyEvent event) {
        return event.getDeviceId() != -1 &&
               (event.getSource() & InputDevice.SOURCE_KEYBOARD) != 0;
    }

    private boolean isMouseEvent(MotionEvent event) {
        return (event.getSource() & InputDevice.SOURCE_MOUSE) != 0 ||
               (event.getSource() & InputDevice.SOURCE_MOUSE_RELATIVE) != 0;
    }

    private int getGLFWMods(KeyEvent event) {
        int mods = 0;
        if (event.isShiftPressed())   mods |= 0x0001; // GLFW_MOD_SHIFT
        if (event.isCtrlPressed())    mods |= 0x0002; // GLFW_MOD_CONTROL
        if (event.isAltPressed())     mods |= 0x0004; // GLFW_MOD_ALT
        if (event.isMetaPressed())    mods |= 0x0008; // GLFW_MOD_SUPER
        return mods;
    }

    // ─── Existing code ────────────────────────────────────────────────────────

    @Override
    public void requestSoftInput() {
        touchCharInput.requestKeyboard();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void registerOnBackInvoked() {
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(PRIORITY_DEFAULT, this::onBackPressed);
    }

    @Override
    public void openLayoutEditor() {
        ViewGroup controlLayoutParent = (ViewGroup) controlLayout.getParent();
        controlLayoutParent.removeView(controlLayout);
        layoutEditor = LayoutInflater.from(this).inflate(R.layout.controls_editor, controlLayoutParent, false);
        ControlEditorLayout editorLayout = layoutEditor.findViewById(R.id.control_layout_editor);
        editorLayout.setEditorHost(this);
        controlLayoutParent.addView(layoutEditor);
    }

    @Override
    public void exitLayoutEditor() {
        if(layoutEditor == null) return;
        ViewGroup editorParent = (ViewGroup) layoutEditor.getParent();
        editorParent.removeView(layoutEditor);
        editorParent.addView(controlLayout);
        controlLayout.loadAsync();
        layoutEditor = null;
    }

    public void kickstart() {
        try {
            DotnetStarter.kickstart(new AppDirs(getFilesDir()), new File(getApplicationInfo().nativeLibraryDir));
        }catch (Throwable t) {
            Utils.showErrorDialog(this, t, true);
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_ESCAPE, 1, 0);
        GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_ESCAPE, 0, 0);
    }

    public static native void runDotnet(String dotnetRoot, String vsDir);
}
