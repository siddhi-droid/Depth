package com.github.florent37.depth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.gihub.florent37.depth.R;

public class DepthRelativeLayoutContainer extends RelativeLayout {

    private Matrix matrix = new Matrix();
    private Paint shadowPaint = new Paint();
    private NinePatchDrawable softShadow;
    private Drawable roundSoftShadow;
    private Path edgePath = new Path();
    private float shadowAlpha = 0.3f;

    public DepthRelativeLayoutContainer(Context context) {
        super(context);
        setup();

    }

    public DepthRelativeLayoutContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public DepthRelativeLayoutContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DepthRelativeLayoutContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    public float getTopEdgeLength(DepthLayout dl) {
        return getDistance(dl.getDepthManager().getTopLeftBack(), dl.getDepthManager().getTopRightBack());
    }

    float getDistance(PointF p1, PointF p2) {
        return (float) Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
    }

    public float getShadowAlpha() {
        return shadowAlpha;
    }

    public void setShadowAlpha(float shadowAlpha) {
        this.shadowAlpha = Math.min(1f, Math.max(0, shadowAlpha));
    }

    void setup() {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                for (int i = 0; i < getChildCount(); i++) {
                    final View child = getChildAt(i);
                    if (child instanceof DepthLayout) {
                        boolean hasChangedBounds = ((DepthLayout) child).getDepthManager().calculateBounds();
                        if (hasChangedBounds)
                            invalidate();
                    }
                }
                return true;
            }
        });

        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAntiAlias(true);
        softShadow = (NinePatchDrawable) ContextCompat.getDrawable(getContext(), R.drawable.shadow);
        roundSoftShadow = ContextCompat.getDrawable(getContext(), R.drawable.round_soft_shadow);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (!isInEditMode()) {
            if (child instanceof DepthLayout) {
                final DepthLayout dl = (DepthLayout) child;
                final CustomShadow customShadow = dl.getDepthManager().getCustomShadow();

                final float[] src = new float[]{0, 0, dl.getWidth(), 0, dl.getWidth(), dl.getHeight(), 0, dl.getHeight()};
                if (dl.getDepthManager().isCircle()) {
                    customShadow.drawShadow(canvas, roundSoftShadow);
                    if (Math.abs(dl.getRotationX()) > 1 || Math.abs(dl.getRotationY()) > 1) {
                        drawCornerBaseShape(dl, canvas, src);
                    }
                } else {
                    customShadow.drawShadow(canvas, softShadow);
                    if (dl.getRotationX() != 0 || dl.getRotationY() != 0) {
                        if (getLongestHorizontalEdge(dl) > getLongestVerticalEdge(dl))
                            drawVerticalFirst(dl, canvas, src);
                        else
                            drawHorizontalFist(dl, canvas, src);
                    }
                }
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private void drawCornerBaseShape(DepthLayout dl, Canvas canvas, float[] src) {
        final float[] dst = new float[]{dl.getDepthManager().getTopLeftBack().x, dl.getDepthManager().getTopLeftBack().y, dl.getDepthManager().getTopRightBack().x, dl.getDepthManager().getTopRightBack().y, dl.getDepthManager().getBottomRightBack().x, dl.getDepthManager().getBottomRightBack().y, dl.getDepthManager().getBottomLeftBack().x, dl.getDepthManager().getBottomLeftBack().y};
        final int count = canvas.save();
        matrix.setPolyToPoly(src, 0, dst, 0, src.length >> 1);
        canvas.concat(matrix);
        edgePath.reset();
        edgePath.addRoundRect(0, 0, dl.getWidth(), dl.getHeight(), dl.getWidth() / 2f, dl.getHeight() / 2f, Path.Direction.CCW);

        canvas.drawPath(edgePath, dl.getDepthManager().getEdgePaint());
        shadowPaint.setAlpha((int) (shadowAlpha * 0.5f * 255));
        canvas.drawPath(edgePath, shadowPaint);

        canvas.restoreToCount(count);
    }

    private void drawHorizontalFist(DepthLayout dl, Canvas canvas, float[] src) {
        if (getLeftEdgeLength(dl) <= getRightEdgeLength(dl)) {
            drawLeftEdge(dl, canvas, src);
        } else {
            drawRightEdge(dl, canvas, src);
        }

        drawTopEdge(dl, canvas, src);
        drawBottomEdge(dl, canvas, src);

        if (getLeftEdgeLength(dl) >= getRightEdgeLength(dl)) {
            drawLeftEdge(dl, canvas, src);
        } else {
            drawRightEdge(dl, canvas, src);
        }
    }

    private void drawVerticalFirst(DepthLayout dl, Canvas canvas, float[] src) {
        if (getTopEdgeLength(dl) <= getBottomEdgeLength(dl)) {
            drawTopEdge(dl, canvas, src);
        } else {
            drawBottomEdge(dl, canvas, src);
        }

        drawLeftEdge(dl, canvas, src);
        drawRightEdge(dl, canvas, src);


        if (getTopEdgeLength(dl) >= getBottomEdgeLength(dl)) {
            drawTopEdge(dl, canvas, src);
        } else {
            drawBottomEdge(dl, canvas, src);
        }

    }

    float getLongestHorizontalEdge(DepthLayout dl) {
        final float topEdgeLength = getTopEdgeLength(dl);
        final float bottomEdgeLength = getBottomEdgeLength(dl);
        if (topEdgeLength > bottomEdgeLength) {
            return topEdgeLength;
        } else {
            return bottomEdgeLength;
        }
    }

    float getLongestVerticalEdge(DepthLayout dl) {
        final float leftEdgeLength = getLeftEdgeLength(dl);
        final float rightEdgeLength = getRightEdgeLength(dl);
        if (leftEdgeLength > rightEdgeLength) {
            return leftEdgeLength;
        } else {
            return rightEdgeLength;
        }
    }

    private float getRightEdgeLength(DepthLayout dl) {
        return getDistance(dl.getDepthManager().getTopRightBack(), dl.getDepthManager().getBottomRightBack());
    }

    private float getLeftEdgeLength(DepthLayout dl) {
        return getDistance(dl.getDepthManager().getTopLeftBack(), dl.getDepthManager().getBottomLeftBack());
    }

    private float getBottomEdgeLength(DepthLayout dl) {
        return getDistance(dl.getDepthManager().getBottomLeftBack(), dl.getDepthManager().getBottomRightBack());
    }

    void drawShadow(PointF point1, PointF point2, float correctionValue, Canvas canvas, DepthLayout dl) {
        final float angle = Math.abs(Math.abs(getAngle(point1, point2)) + correctionValue);
        final float alpha = angle / 180f;
        shadowPaint.setAlpha((int) (alpha * 255f * shadowAlpha));
        canvas.drawRect(0, 0, dl.getWidth(), dl.getHeight(), shadowPaint);
    }

    private void drawRectangle(DepthLayout dl, Canvas canvas) {
        canvas.drawRect(0, 0, dl.getWidth(), dl.getHeight(), dl.getDepthManager().getEdgePaint());
    }

    public float getAngle(PointF point1, PointF point2) {
        return (float) Math.toDegrees(Math.atan2(point1.y - point2.y, point1.x - point2.x));
    }

    private void drawLeftEdge(DepthLayout dl, Canvas canvas, float[] src) {
        final float[] dst = new float[]{dl.getDepthManager().getTopLeft().x, dl.getDepthManager().getTopLeft().y, dl.getDepthManager().getTopLeftBack().x, dl.getDepthManager().getTopLeftBack().y, dl.getDepthManager().getBottomLeftBack().x, dl.getDepthManager().getBottomLeftBack().y, dl.getDepthManager().getBottomLeft().x, dl.getDepthManager().getBottomLeft().y};
        final int count = canvas.save();
        matrix.setPolyToPoly(src, 0, dst, 0, src.length >> 1);
        canvas.concat(matrix);
        drawRectangle(dl, canvas);
        drawShadow(dl.getDepthManager().getTopLeft(), dl.getDepthManager().getBottomLeft(), 0, canvas, dl);
        canvas.restoreToCount(count);
    }

    private void drawRightEdge(DepthLayout dl, Canvas canvas, float[] src) {
        final float[] dst = new float[]{dl.getDepthManager().getTopRight().x, dl.getDepthManager().getTopRight().y, dl.getDepthManager().getTopRightBack().x, dl.getDepthManager().getTopRightBack().y, dl.getDepthManager().getBottomRightBack().x, dl.getDepthManager().getBottomRightBack().y, dl.getDepthManager().getBottomRight().x, dl.getDepthManager().getBottomRight().y};
        final int count = canvas.save();
        matrix.setPolyToPoly(src, 0, dst, 0, src.length >> 1);
        canvas.concat(matrix);
        drawRectangle(dl, canvas);
        drawShadow(dl.getDepthManager().getTopRight(), dl.getDepthManager().getBottomRight(), -180f, canvas, dl);
        canvas.restoreToCount(count);
    }

    private void drawTopEdge(DepthLayout dl, Canvas canvas, float[] src) {
        final float[] dst = new float[]{dl.getDepthManager().getTopLeft().x, dl.getDepthManager().getTopLeft().y, dl.getDepthManager().getTopRight().x, dl.getDepthManager().getTopRight().y, dl.getDepthManager().getTopRightBack().x, dl.getDepthManager().getTopRightBack().y, dl.getDepthManager().getTopLeftBack().x, dl.getDepthManager().getTopLeftBack().y};
        final int count = canvas.save();
        matrix.setPolyToPoly(src, 0, dst, 0, src.length >> 1);
        canvas.concat(matrix);
        drawRectangle(dl, canvas);
        drawShadow(dl.getDepthManager().getTopLeft(), dl.getDepthManager().getTopRight(), -180f, canvas, dl);
        canvas.restoreToCount(count);
    }

    private void drawBottomEdge(DepthLayout dl, Canvas canvas, float[] src) {
        final float[] dst = new float[]{dl.getDepthManager().getBottomLeft().x, dl.getDepthManager().getBottomLeft().y, dl.getDepthManager().getBottomRight().x, dl.getDepthManager().getBottomRight().y, dl.getDepthManager().getBottomRightBack().x, dl.getDepthManager().getBottomRightBack().y, dl.getDepthManager().getBottomLeftBack().x, dl.getDepthManager().getBottomLeftBack().y};
        final int count = canvas.save();
        matrix.setPolyToPoly(src, 0, dst, 0, dst.length >> 1);
        canvas.concat(matrix);
        drawRectangle(dl, canvas);
        drawShadow(dl.getDepthManager().getBottomLeft(), dl.getDepthManager().getBottomRight(), 0, canvas, dl);
        canvas.restoreToCount(count);
    }
}
