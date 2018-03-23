package com.github.florent37.depth.container;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class DepthContainerFrameLayout extends FrameLayout {

    private final DepthMotionHandler motionHandler;

    private final DepthContainerManager depthContainerManager;

    public DepthContainerFrameLayout(Context context) {
        super(context);
        motionHandler = new DepthMotionHandler(this);
        depthContainerManager = new DepthContainerManager(this);
        depthContainerManager.setup();
    }

    public DepthContainerFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        motionHandler = new DepthMotionHandler(this);
        depthContainerManager = new DepthContainerManager(this);
        depthContainerManager.setup();
    }

    public DepthContainerFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        motionHandler = new DepthMotionHandler(this);
        depthContainerManager = new DepthContainerManager(this);
        depthContainerManager.setup();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (!isInEditMode()) {
            depthContainerManager.drawChild(canvas, child, drawingTime);
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return motionHandler.onTouchEvent(event);
    }
}