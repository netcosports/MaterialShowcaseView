package uk.co.deanwild.materialshowcaseview.shape;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class RoundRectShape extends RectangleShape {

    private RectF rect = new RectF();
    private int radius = ShowcaseConfig.DEFAULT_SHAPE_PADDING;


    public RoundRectShape(int width, int height) {
        super(width, height);
    }

    public RoundRectShape(Rect bounds) {
        super(bounds);
    }

    public RoundRectShape(Rect bounds, boolean fullWidth) {
        super(bounds, fullWidth);
    }

    public RoundRectShape setRadius(int radius) {
        this.radius = radius;
        return this;
    }

    @Override
    public void draw(Canvas canvas, Paint paint, int x, int y, int padding) {
        if (!getRect().isEmpty()) {
            rect.left = getRect().left + x - padding;
            rect.top = getRect().top + y - padding;
            rect.right = getRect().right + x + padding;
            rect.bottom = getRect().bottom + y + padding;

            canvas.drawRoundRect(
                    rect,
                    radius,
                    radius,
                    paint
            );
        }
    }

}
