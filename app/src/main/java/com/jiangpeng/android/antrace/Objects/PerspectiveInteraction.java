package com.jiangpeng.android.antrace.Objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class PerspectiveInteraction extends ImageInteraction {
    public PerspectiveInteraction(ImageView view) {
		super(view);
	}
	private PointF m_leftTop = new PointF();
    private PointF m_rightTop = new PointF();
    private PointF m_leftBottom = new PointF();
    private PointF m_rightBottom = new PointF();
	@Override
	public void draw(Canvas c) {
		// TODO Auto-generated method stub
		if(m_isCropping)
		{
			float[] pts = new float[16];
			pts[0] = m_leftTop.x;
			pts[1] = m_leftTop.y;
			pts[2] = m_rightTop.x;
			pts[3] = m_rightTop.y;

			pts[4] = m_rightTop.x;
			pts[5] = m_rightTop.y;
			pts[6] = m_rightBottom.x;
			pts[7] = m_rightBottom.y;

			pts[8] = m_rightBottom.x;
			pts[9] = m_rightBottom.y;
			pts[10] = m_leftBottom.x;
			pts[11] = m_leftBottom.y;

			pts[12] = m_leftBottom.x;
			pts[13] = m_leftBottom.y;
			pts[14] = m_leftTop.x;
			pts[15] = m_leftTop.y;

			m_paint.setColor(Color.argb(255, 255, 255, 0));
			m_paint.setStyle(Paint.Style.STROKE);
			m_paint.setStrokeWidth(2.0f);
			m_paint.setAntiAlias(true);
			c.drawLines(pts, m_paint); 
			
			float radius = 8;
			c.drawCircle(m_leftTop.x, m_leftTop.y, radius, m_paint);
			c.drawCircle(m_rightTop.x, m_rightTop.y, radius, m_paint);
			c.drawCircle(m_rightBottom.x, m_rightBottom.y, radius, m_paint);
			c.drawCircle(m_leftBottom.x, m_leftBottom.y, radius, m_paint);
			return;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent rawEvent) {
        // Handle touch events here...
        switch (rawEvent.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            m_start.set(rawEvent.getX(), rawEvent.getY());
            m_last.set(rawEvent.getX(), rawEvent.getY());
            m_hitTest = hitTest(rawEvent);
            if(m_hitTest != HitTestResult.Outside)
            {
            	m_mode = DRAG_SELECTION;
            }
            break;
        case MotionEvent.ACTION_UP:
        {
            int xDiff = (int) Math.abs(rawEvent.getX() - m_start.x);
            int yDiff = (int) Math.abs(rawEvent.getY() - m_start.y);
            if (xDiff < 8 && yDiff < 8){
                m_imageView.performClick();
            }
        }
        case MotionEvent.ACTION_POINTER_UP:
            m_mode = NONE;
            m_hitTest = HitTestResult.None;
            break;
        case MotionEvent.ACTION_MOVE:
            if(m_mode == DRAG_SELECTION)
            {
            	if(m_hitTest == HitTestResult.TopLeft)
            	{
            		m_leftTop.x += rawEvent.getX() - m_last.x;
            		m_leftTop.y += rawEvent.getY() - m_last.y;
            	}

            	if(m_hitTest == HitTestResult.TopRight)
            	{
            		m_rightTop.x += rawEvent.getX() - m_last.x;
            		m_rightTop.y += rawEvent.getY() - m_last.y;
            	}

            	if(m_hitTest == HitTestResult.BottomLeft)
            	{
            		m_leftBottom.x += rawEvent.getX() - m_last.x;
            		m_leftBottom.y += rawEvent.getY() - m_last.y;
            	}

            	if(m_hitTest == HitTestResult.BottomRight)
            	{
            		m_rightBottom.x += rawEvent.getX() - m_last.x;
            		m_rightBottom.y += rawEvent.getY() - m_last.y;
            	}
            	
            	restrictSelection(m_hitTest);
            	m_last.set(rawEvent.getX(), rawEvent.getY());
            }
            break;
        }
       	m_imageView.invalidate();
        return true; // indicate event was handled
	}
    
    private HitTestResult hitTest(MotionEvent event)
    {
    	float x = event.getX();
    	float y = event.getY();
    	
    	RectF area = generateRect(m_leftTop, HitRadius, HitRadius);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.TopLeft;
    	}
    	area = generateRect(m_rightBottom, HitRadius, HitRadius);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.BottomRight;
    	}
    	
    	area = generateRect(m_leftBottom, HitRadius, HitRadius);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.BottomLeft;
    	}
    	
    	area = generateRect(m_rightTop, HitRadius, HitRadius);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.TopRight;
    	}
   	
    	return HitTestResult.Outside;
    }

    private void restrictSelection(HitTestResult hitTest)
    {
    	RectF rc = getImageRect();
    	if(hitTest == HitTestResult.TopLeft)
    	{
    		m_leftTop = correctPoint(rc, m_leftTop);
    	}

    	if(hitTest == HitTestResult.TopRight)
    	{
    		m_rightTop = correctPoint(rc, m_rightTop);
    	}

    	if(hitTest == HitTestResult.BottomRight)
    	{
    		m_rightBottom = correctPoint(rc, m_rightBottom);
    	}

    	if(hitTest == HitTestResult.BottomLeft)
    	{
    		m_leftBottom = correctPoint(rc, m_leftBottom);
    	}
    }
    
    public void startCrop()
    {
    	m_isCropping = true;
    	float w = (float)m_bitmap.getWidth();
    	float h = (float)m_bitmap.getHeight();
    	
    	float div = 8.0f;
    	PointF pt = new PointF(w / div, h / div);
    	m_leftTop = toScreen(pt);

    	pt = new PointF(w / div, h - h / div);
    	m_leftBottom = toScreen(pt);

    	pt = new PointF(w - w / div, h - h / div);
    	m_rightBottom = toScreen(pt);

    	pt = new PointF(w - w / div, h / div);
    	m_rightTop = toScreen(pt);
    	m_imageView.invalidate();
    }

	private PointF getLeftTop() {
		return toBitmap(m_leftTop);
	}

	private PointF getRightTop() {
		return toBitmap(m_rightTop);
	}

	private PointF getRightBottom() {
		return toBitmap(m_rightBottom);
	}

	private PointF getLeftBottom() {
		return toBitmap(m_leftBottom);
	}

	@Override
	public Bitmap getCroppedBitmap() {
	    Matrix matrix = new Matrix();
	    float w1 = distance(getLeftBottom(), getRightBottom());
	    float w2 = distance(getRightTop(), getLeftTop());

	    float h1 = distance(getLeftBottom(), getLeftTop());
	    float h2 = distance(getRightTop(), getRightBottom());
	    float w = Math.max(w1, w2);
	    float h = Math.max(h1, h2);
	    float[] dst = new float[] {
	    		0, 0, w, 0, w, h, 0, h
	    };
	    float[] src = new float[] {
	    		getLeftTop().x,
	    		getLeftTop().y,
	    		getRightTop().x,
	    		getRightTop().y,
	    		getRightBottom().x,
	    		getRightBottom().y,
	    		getLeftBottom().x,
	    		getLeftBottom().y
	    };
	    
	    float l = src[0];
	    float t = src[1];
	    float r = src[0];
	    float b = src[1];
	    for(int i = 0; i < 4; ++i)
	    {
	    	if(src[i * 2] > r)
	    	{
	    		r = src[i * 2];
	    	}

	    	if(src[i * 2] < l)
	    	{
	    		l = src[i * 2];
	    	}

	    	if(src[i * 2 + 1] < t)
	    	{
	    		t = src[i * 2 + 1];
	    	}

	    	if(src[i * 2 + 1] > b)
	    	{
	    		b = src[i * 2 + 1];
	    	}
	    }
	    Bitmap ret = null;
	    try
	    {
	    	ret = Bitmap.createBitmap(m_bitmap, (int)l, (int)t, (int)(r - l), (int)(b - t), matrix, true);

	    	src = new float[] {
	    			getLeftTop().x - l,
	    			getLeftTop().y - t,
	    			getRightTop().x - l,
	    			getRightTop().y - t,
	    			getRightBottom().x - l,
	    			getRightBottom().y - t,
	    			getLeftBottom().x - l,
	    			getLeftBottom().y - t
	    	};
	    	matrix.setPolyToPoly(src, 0, dst, 0, 4);
	    	ret = Bitmap.createBitmap(ret, 0, 0, (int)ret.getWidth(), (int)ret.getHeight(), matrix, true);

	    	float[] o = new float[]
	    			{
	    			0, 0, ret.getWidth(), 0, ret.getWidth(), ret.getHeight(), 0, ret.getHeight()
	    			};
	    	float[] newo = new float[8];
	    	matrix.mapPoints(newo, o);
	    	matrix.reset();
	    	float lowx = newo[0];
	    	float lowy = newo[1];
	    	for(int i = 1; i < 4; ++i)
	    	{
	    		if(lowx > newo[i * 2])
	    		{
	    			lowx = newo[i * 2];
	    		}

	    		if(lowy > newo[i * 2 + 1])
	    		{
	    			lowy = newo[i * 2 + 1];
	    		}
	    	}
	    	ret = Bitmap.createBitmap(ret, -1 * (int)lowx, -1 * (int)lowy, (int)w, (int)h, matrix, true);
	    }
	    catch(OutOfMemoryError err)
	    {
	    } 
	    return ret;
	}
}
