package git.artdeell.dnbootstrap.input;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GrabListener;
import git.artdeell.dnbootstrap.input.model.InputConfiguration;
import git.artdeell.dnbootstrap.input.model.VisibilityConfiguration;

public class ControlLayout extends LoadableButtonLayout implements GrabListener {
    private final Rect hitTestRect = new Rect();
    private final HashMap<Integer, HitTarget> lastHitTargets = new HashMap<>();
    private final Set<HitTarget> allHitTargets = new HashSet<>();
    private final HitTarget defaultHitTarget = new HitTarget(new DefaultConsumer());

    public ControlLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        GLFW.addGrabListener(this);
    }

    public ControlLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ControlLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ControlLayout(@NonNull Context context) {
        super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    private HitTarget hitTest(int x, int y) {
        for(HitTarget hitTarget : allHitTargets) {
            View child = ((View)hitTarget.consumer);
            hitTestRect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            if(hitTestRect.contains(x,y)) return hitTarget;
        }
        return defaultHitTarget;
    }

    @Override
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        if(!(view instanceof LayoutTouchConsumer)) return;
        LayoutTouchConsumer layoutTouchConsumer = (LayoutTouchConsumer)view;
        allHitTargets.add(new HitTarget(layoutTouchConsumer));
        updateVisibility(layoutTouchConsumer);
    }

    @Override
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        if(allHitTargets.isEmpty()) return;
        if(!(view instanceof LayoutTouchConsumer)) return;
        Iterator<HitTarget> iter = allHitTargets.iterator();
        while(iter.hasNext()) {
            if(iter.next().consumer != view) continue;
            iter.remove();
            break;
        }
    }

    @Override
    protected void onRemoveAllViews() {
        super.onRemoveAllViews();
        allHitTargets.clear();
    }

    private void processPointer(MotionEvent event, int pointer, int action) {
        int pointerId = event.getPointerId(pointer);
        HitTarget lastHit = lastHitTargets.get(pointerId);
        float x = event.getX(pointer), y = event.getY(pointer);

        if(action == MotionEvent.ACTION_MOVE) {
            // If the current pointer is taken over by a sticky view, just update its position
            // and leave
            if(lastHit != null && lastHit.consumer.getInputConfiguration().sticky) {
                lastHit.onTouchPosition(pointerId, x - lastHit.consumer.getLeft(), y - lastHit.consumer.getTop());
                return;
            }
            HitTarget newHit = hitTest((int)x, (int)y);

            if(lastHit != newHit) {
                if(lastHit != null) lastHit.onTouchState(pointerId, false);
                // Only press a new button if the finger started on the cursor zone (defaultHitTarget).
                // If the finger started on a button and slides onto another, don't activate the new one.
                if(newHit != null && lastHit == defaultHitTarget) {
                    newHit.onTouchState(pointerId, true);
                }
            }
            if(newHit != null) newHit.onTouchPosition(pointerId, x, y);
            lastHitTargets.put(pointerId, newHit);
        }else if(action == MotionEvent.ACTION_POINTER_UP) {
            if(lastHit != null) lastHit.onTouchState(pointerId, false);
            lastHitTargets.remove(pointerId);
        }else if(action == MotionEvent.ACTION_POINTER_DOWN) {
            HitTarget hit = hitTest((int) x, (int) y);
            if(hit != null) hit.onTouchState(pointerId, true);
            lastHitTargets.put(pointerId, hit);
        }
    }

    private void releaseAllPointers() {
        for(HitTarget target : lastHitTargets.values()) {
            if(target == null) continue;
            target.reset();
        }
        lastHitTargets.clear();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int affectedPointer = -1;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                releaseAllPointers();
                processPointer(event, 0, MotionEvent.ACTION_POINTER_DOWN);
                break;
            case MotionEvent.ACTION_UP:
                releaseAllPointers();
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_POINTER_DOWN:
                affectedPointer = event.getActionIndex();
            case MotionEvent.ACTION_MOVE:
                for(int i = 0; i < event.getPointerCount(); i++) {
                    int reportedAction = MotionEvent.ACTION_MOVE;
                    if(affectedPointer == i) reportedAction = action;
                    processPointer(event, i, reportedAction);
                }
                break;
        }
        return true;
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        post(this::updateVisibility);
    }

    private void updateVisibility(LayoutTouchConsumer layoutTouchConsumer) {
        boolean isGrabbing = GLFW.isGrabbing();
        VisibilityConfiguration visibilityConfiguration = layoutTouchConsumer.getVisibilityConfiguration();
        boolean visible = visibilityConfiguration.showInGame && isGrabbing;
        visible |= visibilityConfiguration.showInMenu && !isGrabbing;
        layoutTouchConsumer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateVisibility() {
        for(HitTarget hitTarget : allHitTargets) {
            if(hitTarget == defaultHitTarget) continue;
            updateVisibility(hitTarget.consumer);
        }
    }

    private class HitTarget {
        public final @NonNull LayoutTouchConsumer consumer;
        private int firstTouchedPointer;
        private boolean lastState;

        private HitTarget(@NonNull LayoutTouchConsumer consumer) {
            this.consumer = consumer;
        }

        public void onTouchState(int pointerId, boolean isTouched) {
            if(pointerId != firstTouchedPointer && firstTouchedPointer != -1) return;
            if(!isTouched) firstTouchedPointer = -1;
            if(isTouched && firstTouchedPointer == -1) firstTouchedPointer = pointerId;
            if(isTouched != lastState) {
                lastState = isTouched;
                consumer.onTouchState(isTouched);
            }
        }

        public void onTouchPosition(int pointerId, float x, float y) {
            if(pointerId != firstTouchedPointer) return;
            consumer.onTouchPosition(x - consumer.getLeft(), y - consumer.getTop());
        }

        public void reset() {
            firstTouchedPointer = -1;
            if(lastState) consumer.onTouchState(false);
            lastState = false;
        }
    }

    public class DefaultConsumer implements LayoutTouchConsumer {
        private final InputConfiguration defaultConfiguration = new InputConfiguration();
        private boolean deltaReady = false;
        private float lastX, lastY;

        @Override
        public void onTouchState(boolean isTouched) {
            lastX = lastY = 0;
            deltaReady = false;
        }

        @Override
        public void onTouchPosition(float x, float y) {
            if(!deltaReady) {
                lastX = x;
                lastY = y;
                deltaReady = true;
                return;
            }
            float deltaX = x - lastX;
            float deltaY = y - lastY;
            GLFW.cursorX += deltaX / getWidth();
            GLFW.cursorY += deltaY / getHeight();
            GLFW.sendMousePos();
            lastX = x;
            lastY = y;
        }

        @Override
        public void setVisibility(int visibility) {}

        @Override
        public int getLeft() {
            return 0;
        }

        @Override
        public int getTop() {
            return 0;
        }

        @NonNull
        @Override
        public InputConfiguration getInputConfiguration() {
            return defaultConfiguration;
        }

        @Override
        public VisibilityConfiguration getVisibilityConfiguration() {
            return null;
        }
    }
}
