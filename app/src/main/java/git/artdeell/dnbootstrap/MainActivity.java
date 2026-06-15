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
import android.view.PointerIcon;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

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
    private SurfaceView surfaceView;

    private static boolean isRunning = false;
    private float lastMouseX = -1, lastMouseY = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surface_view);
        touchCharInput = findViewById(R.id.touch_char_input);
        controlLayout = findViewById(R.id.control_layout);
        InsetUtils.setInsetsMode(this, true, false);
        surfaceView.getHolder().addCallback(new NativeSurfaceListener());
        surfaceView.setFocusable(true);
        surfaceView.setFocusableInTouchMode(true);
        surfaceView.requestFocus();

        // Hide system mouse cursor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getWindow().getDecorView().setPointerIcon(
                PointerIcon.getSystemIcon(this, PointerIcon.TYPE_NULL));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerOnBackInvoked();
        }
        if (!isRunning) {
            isRunning = true;
            new Thread(this::kickstart).start();
        }
    }

    // ─── Physical Keyboard ────────────────────────────────────────────────────

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isPhysicalKeyboard(event)) {
            int keyCode = event.getKeyCode();

            // Let volume keys pass to Android
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return super.dispatchKeyEvent(event);
            }

            int glfwKey = androidToGlfw(keyCode);
            if (glfwKey != KeyCodes.GLFW_KEY_NONE) {
                int action = event.getAction();
                if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() > 0) {
                    return true;
                }

                int state = action == KeyEvent.ACTION_DOWN ? KeyCodes.GLFW_PRESS : KeyCodes.GLFW_RELEASE;
                GLFW.sendKeyEvent(glfwKey, state, getGLFWMods(event));
            }

            // Dismiss soft keyboard if it appeared
            hideSoftKeyboard();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (focus != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }

    // ─── Android → GLFW keycode map ───────────────────────────────────────────

    private int androidToGlfw(int keyCode) {
        switch (keyCode) {
            // Letters
            case KeyEvent.KEYCODE_A: return KeyCodes.GLFW_KEY_A;
            case KeyEvent.KEYCODE_B: return KeyCodes.GLFW_KEY_B;
            case KeyEvent.KEYCODE_C: return KeyCodes.GLFW_KEY_C;
            case KeyEvent.KEYCODE_D: return KeyCodes.GLFW_KEY_D;
            case KeyEvent.KEYCODE_E: return KeyCodes.GLFW_KEY_E;
            case KeyEvent.KEYCODE_F: return KeyCodes.GLFW_KEY_F;
            case KeyEvent.KEYCODE_G: return KeyCodes.GLFW_KEY_G;
            case KeyEvent.KEYCODE_H: return KeyCodes.GLFW_KEY_H;
            case KeyEvent.KEYCODE_I: return KeyCodes.GLFW_KEY_I;
            case KeyEvent.KEYCODE_J: return KeyCodes.GLFW_KEY_J;
            case KeyEvent.KEYCODE_K: return KeyCodes.GLFW_KEY_K;
            case KeyEvent.KEYCODE_L: return KeyCodes.GLFW_KEY_L;
            case KeyEvent.KEYCODE_M: return KeyCodes.GLFW_KEY_M;
            case KeyEvent.KEYCODE_N: return KeyCodes.GLFW_KEY_N;
            case KeyEvent.KEYCODE_O: return KeyCodes.GLFW_KEY_O;
            case KeyEvent.KEYCODE_P: return KeyCodes.GLFW_KEY_P;
            case KeyEvent.KEYCODE_Q: return KeyCodes.GLFW_KEY_Q;
            case KeyEvent.KEYCODE_R: return KeyCodes.GLFW_KEY_R;
            case KeyEvent.KEYCODE_S: return KeyCodes.GLFW_KEY_S;
            case KeyEvent.KEYCODE_T: return KeyCodes.GLFW_KEY_T;
            case KeyEvent.KEYCODE_U: return KeyCodes.GLFW_KEY_U;
            case KeyEvent.KEYCODE_V: return KeyCodes.GLFW_KEY_V;
            case KeyEvent.KEYCODE_W: return KeyCodes.GLFW_KEY_W;
            case KeyEvent.KEYCODE_X: return KeyCodes.GLFW_KEY_X;
            case KeyEvent.KEYCODE_Y: return KeyCodes.GLFW_KEY_Y;
            case KeyEvent.KEYCODE_Z: return KeyCodes.GLFW_KEY_Z;
            // Numbers
            case KeyEvent.KEYCODE_0: return KeyCodes.GLFW_KEY_0;
            case KeyEvent.KEYCODE_1: return KeyCodes.GLFW_KEY_1;
            case KeyEvent.KEYCODE_2: return KeyCodes.GLFW_KEY_2;
            case KeyEvent.KEYCODE_3: return KeyCodes.GLFW_KEY_3;
            case KeyEvent.KEYCODE_4: return KeyCodes.GLFW_KEY_4;
            case KeyEvent.KEYCODE_5: return KeyCodes.GLFW_KEY_5;
            case KeyEvent.KEYCODE_6: return KeyCodes.GLFW_KEY_6;
            case KeyEvent.KEYCODE_7: return KeyCodes.GLFW_KEY_7;
            case KeyEvent.KEYCODE_8: return KeyCodes.GLFW_KEY_8;
            case KeyEvent.KEYCODE_9: return KeyCodes.GLFW_KEY_9;
            // Function keys
            case KeyEvent.KEYCODE_F1:  return KeyCodes.GLFW_KEY_F1;
            case KeyEvent.KEYCODE_F2:  return KeyCodes.GLFW_KEY_F2;
            case KeyEvent.KEYCODE_F3:  return KeyCodes.GLFW_KEY_F3;
            case KeyEvent.KEYCODE_F4:  return KeyCodes.GLFW_KEY_F4;
            case KeyEvent.KEYCODE_F5:  return KeyCodes.GLFW_KEY_F5;
            case KeyEvent.KEYCODE_F6:  return KeyCodes.GLFW_KEY_F6;
            case KeyEvent.KEYCODE_F7:  return KeyCodes.GLFW_KEY_F7;
            case KeyEvent.KEYCODE_F8:  return KeyCodes.GLFW_KEY_F8;
            case KeyEvent.KEYCODE_F9:  return KeyCodes.GLFW_KEY_F9;
            case KeyEvent.KEYCODE_F10: return KeyCodes.GLFW_KEY_F10;
            case KeyEvent.KEYCODE_F11: return KeyCodes.GLFW_KEY_F11;
            case KeyEvent.KEYCODE_F12: return KeyCodes.GLFW_KEY_F12;
            // Special keys
            case KeyEvent.KEYCODE_ESCAPE:    return KeyCodes.GLFW_KEY_ESCAPE;
            case KeyEvent.KEYCODE_ENTER:     return KeyCodes.GLFW_KEY_ENTER;
            case KeyEvent.KEYCODE_TAB:       return KeyCodes.GLFW_KEY_TAB;
            case KeyEvent.KEYCODE_DEL:       return KeyCodes.GLFW_KEY_BACKSPACE;
            case KeyEvent.KEYCODE_FORWARD_DEL: return KeyCodes.GLFW_KEY_DELETE;
            case KeyEvent.KEYCODE_INSERT:    return KeyCodes.GLFW_KEY_INSERT;
            case KeyEvent.KEYCODE_SPACE:     return KeyCodes.GLFW_KEY_SPACE;
            case KeyEvent.KEYCODE_CAPS_LOCK: return KeyCodes.GLFW_KEY_CAPS_LOCK;
            // Arrow keys
            case KeyEvent.KEYCODE_DPAD_UP:    return KeyCodes.GLFW_KEY_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:  return KeyCodes.GLFW_KEY_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT:  return KeyCodes.GLFW_KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return KeyCodes.GLFW_KEY_RIGHT;
            // Navigation
            case KeyEvent.KEYCODE_PAGE_UP:   return KeyCodes.GLFW_KEY_PAGE_UP;
            case KeyEvent.KEYCODE_PAGE_DOWN: return KeyCodes.GLFW_KEY_PAGE_DOWN;
            case KeyEvent.KEYCODE_MOVE_HOME: return KeyCodes.GLFW_KEY_HOME;
            case KeyEvent.KEYCODE_MOVE_END:  return KeyCodes.GLFW_KEY_END;
            // Modifiers
            case KeyEvent.KEYCODE_SHIFT_LEFT:   return KeyCodes.GLFW_KEY_LEFT_SHIFT;
            case KeyEvent.KEYCODE_SHIFT_RIGHT:  return KeyCodes.GLFW_KEY_RIGHT_SHIFT;
            case KeyEvent.KEYCODE_CTRL_LEFT:    return KeyCodes.GLFW_KEY_LEFT_CONTROL;
            case KeyEvent.KEYCODE_CTRL_RIGHT:   return KeyCodes.GLFW_KEY_RIGHT_CONTROL;
            case KeyEvent.KEYCODE_ALT_LEFT:     return KeyCodes.GLFW_KEY_LEFT_ALT;
            case KeyEvent.KEYCODE_ALT_RIGHT:    return KeyCodes.GLFW_KEY_RIGHT_ALT;
            case KeyEvent.KEYCODE_META_LEFT:    return KeyCodes.GLFW_KEY_LEFT_SUPER;
            case KeyEvent.KEYCODE_META_RIGHT:   return KeyCodes.GLFW_KEY_RIGHT_SUPER;
            // Punctuation
            case KeyEvent.KEYCODE_COMMA:        return KeyCodes.GLFW_KEY_COMMA;
            case KeyEvent.KEYCODE_PERIOD:       return KeyCodes.GLFW_KEY_PERIOD;
            case KeyEvent.KEYCODE_SLASH:        return KeyCodes.GLFW_KEY_SLASH;
            case KeyEvent.KEYCODE_SEMICOLON:    return KeyCodes.GLFW_KEY_SEMICOLON;
            case KeyEvent.KEYCODE_APOSTROPHE:   return KeyCodes.GLFW_KEY_APOSTROPHE;
            case KeyEvent.KEYCODE_LEFT_BRACKET: return KeyCodes.GLFW_KEY_LEFT_BRACKET;
            case KeyEvent.KEYCODE_RIGHT_BRACKET:return KeyCodes.GLFW_KEY_RIGHT_BRACKET;
            case KeyEvent.KEYCODE_BACKSLASH:    return KeyCodes.GLFW_KEY_BACKSLASH;
            case KeyEvent.KEYCODE_MINUS:        return KeyCodes.GLFW_KEY_MINUS;
            case KeyEvent.KEYCODE_EQUALS:       return KeyCodes.GLFW_KEY_EQUAL;
            case KeyEvent.KEYCODE_GRAVE:        return KeyCodes.GLFW_KEY_GRAVE_ACCENT;
            // Numpad
            case KeyEvent.KEYCODE_NUMPAD_0: return KeyCodes.GLFW_KEY_KP_0;
            case KeyEvent.KEYCODE_NUMPAD_1: return KeyCodes.GLFW_KEY_KP_1;
            case KeyEvent.KEYCODE_NUMPAD_2: return KeyCodes.GLFW_KEY_KP_2;
            case KeyEvent.KEYCODE_NUMPAD_3: return KeyCodes.GLFW_KEY_KP_3;
            case KeyEvent.KEYCODE_NUMPAD_4: return KeyCodes.GLFW_KEY_KP_4;
            case KeyEvent.KEYCODE_NUMPAD_5: return KeyCodes.GLFW_KEY_KP_5;
            case KeyEvent.KEYCODE_NUMPAD_6: return KeyCodes.GLFW_KEY_KP_6;
            case KeyEvent.KEYCODE_NUMPAD_7: return KeyCodes.GLFW_KEY_KP_7;
            case KeyEvent.KEYCODE_NUMPAD_8: return KeyCodes.GLFW_KEY_KP_8;
            case KeyEvent.KEYCODE_NUMPAD_9: return KeyCodes.GLFW_KEY_KP_9;
            case KeyEvent.KEYCODE_NUMPAD_DOT:      return KeyCodes.GLFW_KEY_KP_DECIMAL;
            case KeyEvent.KEYCODE_NUMPAD_DIVIDE:   return KeyCodes.GLFW_KEY_KP_DIVIDE;
            case KeyEvent.KEYCODE_NUMPAD_MULTIPLY: return KeyCodes.GLFW_KEY_KP_MULTIPLY;
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT: return KeyCodes.GLFW_KEY_KP_SUBTRACT;
            case KeyEvent.KEYCODE_NUMPAD_ADD:      return KeyCodes.GLFW_KEY_KP_ADD;
            case KeyEvent.KEYCODE_NUMPAD_ENTER:    return KeyCodes.GLFW_KEY_KP_ENTER;
            default: return KeyCodes.GLFW_KEY_NONE;
        }
    }

    // ─── Physical Mouse ───────────────────────────────────────────────────────

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isMouseEvent(event) && handleMouseEvent(event)) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (isMouseEvent(event) && handleMouseEvent(event)) {
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private boolean handleMouseEvent(MotionEvent event) {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_HOVER_MOVE || action == MotionEvent.ACTION_MOVE) {
            updateMousePosition(event);
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_BUTTON_PRESS || action == MotionEvent.ACTION_BUTTON_RELEASE) {
            updateMousePosition(event);
            int state = (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_BUTTON_PRESS)
                ? KeyCodes.GLFW_PRESS : KeyCodes.GLFW_RELEASE;
            int androidButton = event.getActionButton();
            if (androidButton == 0) {
                androidButton = MotionEvent.BUTTON_PRIMARY;
            }
            return sendMouseButton(androidButton, state, getGLFWMods(event));
        }

        if (action == MotionEvent.ACTION_SCROLL) {
            GLFW.sendScrollEvent(
                event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                event.getAxisValue(MotionEvent.AXIS_VSCROLL));
            return true;
        }

        return false;
    }

    private void updateMousePosition(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int w = surfaceView.getWidth();
        int h = surfaceView.getHeight();
        if (w <= 0 || h <= 0) {
            w = controlLayout.getWidth();
            h = controlLayout.getHeight();
        }

        if (w > 0 && h > 0) {
            if (GLFW.isGrabbing()) {
                float dx = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X);
                float dy = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y);
                if (dx == 0 && dy == 0 && lastMouseX >= 0) {
                    dx = x - lastMouseX;
                    dy = y - lastMouseY;
                }
                GLFW.cursorX += dx / w;
                GLFW.cursorY += dy / h;
            } else {
                GLFW.cursorX = x / w;
                GLFW.cursorY = y / h;
            }
            GLFW.sendMousePos();
        }

        lastMouseX = x;
        lastMouseY = y;
    }

    private boolean sendMouseButton(int androidButton, int state, int mods) {
        int glfwButton = -1;
        if ((androidButton & MotionEvent.BUTTON_PRIMARY) != 0)        glfwButton = MouseCodes.GLFW_MOUSE_BUTTON_LEFT;
        else if ((androidButton & MotionEvent.BUTTON_SECONDARY) != 0) glfwButton = MouseCodes.GLFW_MOUSE_BUTTON_RIGHT;
        else if ((androidButton & MotionEvent.BUTTON_TERTIARY) != 0)  glfwButton = MouseCodes.GLFW_MOUSE_BUTTON_MIDDLE;
        else if ((androidButton & MotionEvent.BUTTON_BACK) != 0)      glfwButton = MouseCodes.GLFW_MOUSE_BUTTON_4;
        else if ((androidButton & MotionEvent.BUTTON_FORWARD) != 0)   glfwButton = MouseCodes.GLFW_MOUSE_BUTTON_5;
        if (glfwButton < 0) {
            return false;
        }

        GLFW.sendMouseEvent(glfwButton, state, mods);
        return true;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean isPhysicalKeyboard(KeyEvent event) {
        InputDevice device = InputDevice.getDevice(event.getDeviceId());
        if (device == null) return false;
        return device.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC ||
            (isFromSource(event.getSource(), InputDevice.SOURCE_KEYBOARD) &&
             !isFromSource(event.getSource(), InputDevice.SOURCE_TOUCHSCREEN));
    }

    private boolean isMouseEvent(MotionEvent event) {
        return isFromSource(event.getSource(), InputDevice.SOURCE_MOUSE) ||
               isFromSource(event.getSource(), InputDevice.SOURCE_MOUSE_RELATIVE);
    }

    private boolean isFromSource(int eventSource, int sourceClass) {
        return (eventSource & sourceClass) == sourceClass;
    }

    private int getGLFWMods(MotionEvent event) {
        return getGLFWMods(event.getMetaState());
    }

    private int getGLFWMods(KeyEvent event) {
        return getGLFWMods(event.getMetaState());
    }

    private int getGLFWMods(int metaState) {
        int mods = 0;
        if ((metaState & KeyEvent.META_SHIFT_ON) != 0) mods |= 0x0001;
        if ((metaState & KeyEvent.META_CTRL_ON) != 0)  mods |= 0x0002;
        if ((metaState & KeyEvent.META_ALT_ON) != 0)   mods |= 0x0004;
        if ((metaState & KeyEvent.META_META_ON) != 0)  mods |= 0x0008;
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
        if (layoutEditor == null) return;
        ViewGroup editorParent = (ViewGroup) layoutEditor.getParent();
        editorParent.removeView(layoutEditor);
        editorParent.addView(controlLayout);
        controlLayout.loadAsync();
        layoutEditor = null;
    }

    public void kickstart() {
        try {
            DotnetStarter.kickstart(new AppDirs(getFilesDir()), new File(getApplicationInfo().nativeLibraryDir));
        } catch (Throwable t) {
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
