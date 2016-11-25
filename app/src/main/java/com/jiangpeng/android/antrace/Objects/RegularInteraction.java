package com.jiangpeng.android.antrace.Objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class RegularInteraction extends ImageInteraction {
    private PointF m_leftTop = new PointF(0, 0);
    private PointF m_size = new PointF(0, 0);
	public RegularInteraction(ImageView view) {
		super(view);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void draw(Canvas c) {
		if(m_isCropping)
		{
			int grey = Color.argb(64, 0, 0, 0);
		
			Paint paint = new Paint();
			paint.setColor(grey);
			paint.setStyle(Paint.Style.FILL);
			int width = m_imageView.getWidth();
			int height = m_imageView.getHeight();
		
			float[] rb = new float[2];
			rb[0] = m_leftTop.x + m_size.x;
			rb[1] = m_leftTop.y + m_size.y;	
			c.drawRect(0, 0, width, m_leftTop.y, paint); 
			c.drawRect(0, rb[1], width, height, paint); 
			c.drawRect(0, m_leftTop.y, m_leftTop.x, rb[1], paint); 
			c.drawRect(rb[0], m_leftTop.y, height, rb[1], paint); 
		
			paint = new Paint();
			paint.setColor(Color.argb(255, 255, 255, 0));
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(2.0f);
			c.drawRect(m_leftTop.x + 1, m_leftTop.y + 1, m_leftTop.x + m_size.x - 1, m_leftTop.y + m_size.y - 1, paint); 
			
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent rawEvent) {
        // Handle touch events here...
        switch (rawEvent.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            m_hitTest = hitTest(rawEvent);
            m_start.set(rawEvent.getX(), rawEvent.getY());
            m_last.set(rawEvent.getX(), rawEvent.getY());
            if(m_hitTest != HitTestResult.Outside)
            {
            	m_mode = DRAG_SELECTION;
            }
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
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
            	PointF eventPt = new PointF(rawEvent.getX(), rawEvent.getY());
            	RectF imageRc = getImageRect();
            	if(m_hitTest == HitTestResult.Inside)
            	{
            		dragSelection(rawEvent);
            	}
            	else if(m_hitTest == HitTestResult.TopLeft)
            	{
            		dragTopLeft(eventPt, imageRc);
            	}
            	else if(m_hitTest == HitTestResult.TopRight)
            	{
            		dragTopRight(eventPt, imageRc);
            	}
            	else if(m_hitTest == HitTestResult.BottomLeft)
            	{
            		dragBottomLeft(eventPt, imageRc);
            	}
            	else if(m_hitTest == HitTestResult.BottomRight)
            	{
            		dragBottomRight(eventPt, imageRc);
            	}
            	else if(m_hitTest == HitTestResult.Left)
            	{
            		dragLeft(eventPt, imageRc);
            	}
            	else if(m_hitTest == HitTestResult.Top)
            	{
            		dragTop(eventPt, imageRc);
            	}
            	else if(m_hitTest == HitTestResult.Bottom)
            	{
            		dragBottom(eventPt, imageRc);
            	}
            	else if(m_hitTest == HitTestResult.Right)
            	{
            		dragRight(eventPt, imageRc);
            	}
            	adjustSelection(true);
            	m_last.set(rawEvent.getX(), rawEvent.getY());

            }
            break;
        }

        m_imageView.setImageMatrix(m_imageToScreen);
       	m_imageView.invalidate();
        return true; // indicate event was handled
	}

	@Override
	public void startCrop() {
    	float[] lt = new float[2];
       	RectF rect = new RectF();
       	rect.left = m_bitmap.getWidth() / 8;
       	rect.top = m_bitmap.getHeight() / 8;
       	rect.right = m_bitmap.getWidth() * 7 / 8;
       	rect.bottom = m_bitmap.getHeight() * 7 / 8;
    	lt[0] = rect.left;
    	lt[1] = rect.top;
                	
    	float[] rb = new float[2];
    	rb[0] = rect.right;
    	rb[1] = rect.bottom;
                	
    	float[] leftTop = new float[2];
    	float[] rightBottom = new float[2];
   	
    	m_imageToScreen.mapPoints(leftTop, lt);
    	m_imageToScreen.mapPoints(rightBottom, rb);
    	
        m_leftTop = new PointF(leftTop[0], leftTop[1]);
        m_size = new PointF(rightBottom[0] - leftTop[0], rightBottom[1] - leftTop[1]);
        m_isCropping = true;
        m_imageView.invalidate(); 
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
    	area = generateRect(new PointF(m_leftTop.x + m_size.x, m_leftTop.y + m_size.y), HitRadius, HitRadius);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.BottomRight;
    	}
    	
    	area = generateRect(new PointF(m_leftTop.x, m_leftTop.y + m_size.y), HitRadius, HitRadius);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.BottomLeft;
    	}
    	
    	area = generateRect(new PointF(m_leftTop.x + m_size.x, m_leftTop.y), HitRadius, HitRadius);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.TopRight;
    	}
    	
    	area = generateRect(new PointF(m_leftTop.x + m_size.x / 2, m_leftTop.y), m_size.x, HitRadius);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.Top;
    	}
    	
    	area = generateRect(new PointF(m_leftTop.x, m_leftTop.y + m_size.y / 2), HitRadius, m_size.y);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.Left;
    	}
    	
    	area = generateRect(new PointF(m_leftTop.x + m_size.x, m_leftTop.y + m_size.y / 2), HitRadius, m_size.y);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.Right;
    	}
    	
    	area = generateRect(new PointF(m_leftTop.x + m_size.x / 2, m_leftTop.y + m_size.y), m_size.x, HitRadius);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.Bottom;
    	}
    	
    	area = new RectF(m_leftTop.x, m_leftTop.y, m_leftTop.x + m_size.x, m_leftTop.y + m_size.y);
    	if(area.contains(x, y))
    	{
    		return HitTestResult.Inside;
    	}
    	
    	return HitTestResult.Outside;
    }

    public RectF getImageSelection(float scale)
    {
    	float[] lt = new float[2];
    	lt[0] = m_leftTop.x;
    	lt[1] = m_leftTop.y;
                	
    	float[] rb = new float[2];
    	rb[0] = m_leftTop.x + m_size.x;
    	rb[1] = m_leftTop.y + m_size.y;
                	
    	float[] leftTop = new float[2];
    	float[] rightBottom = new float[2];
               	
    	Matrix mat = new Matrix();
    	mat.set(m_screenToImage);
    	mat.postScale(scale, scale);
    	
    	mat.mapPoints(leftTop, lt);
    	mat.mapPoints(rightBottom, rb);
    	
    	return new RectF(leftTop[0], leftTop[1], rightBottom[0], rightBottom[1]);
    }
    
    private void restrictToRect(float left, float top, float right, float bottom, boolean keepSize)
    {
     	if(m_leftTop.x < left)
       	{
       		m_leftTop.x = left;
    	}
    	if(m_leftTop.y < top)
    	{
    		m_leftTop.y = top;
    	}
    	
    	if(m_leftTop.y + m_size.y > bottom)
    	{
    		if(keepSize)
    		{
    			m_leftTop.y = bottom - m_size.y;
    		}
    		else
    		{
    			m_size.y = bottom - m_leftTop.y;
    		}
    	}
                	
    	if(m_leftTop.x + m_size.x > right)
    	{
    		if(keepSize)
    		{
    			m_leftTop.x = right - m_size.x;
    		}
    		else
    		{
    			m_size.x = right - m_leftTop.x;
    		}
    	}
    }
    
    private void dragTopLeft(PointF event, RectF rect)
    {
    	dragTop(event, rect);
    	dragLeft(event, rect);
    }
    
    private void dragTopRight(PointF event, RectF rect)
    {
    	dragTop(event, rect);
    	dragRight(event, rect);
    }
    
    private void dragBottomLeft(PointF event, RectF rect)
    {
    	dragBottom(event, rect);
    	dragLeft(event, rect);
    }
   
    private void dragBottomRight(PointF event, RectF rect)
    {
    	dragBottom(event, rect);
    	dragRight(event, rect);
    }
    
    private void dragLeft(PointF event, RectF rect)
    {    
    	float offset = event.x - m_last.x;
    	if(Math.abs(offset) < 1e-3)
    	{
    		return;
    	}
    	float xDiff = event.x - m_last.x;
    	xDiff = restrictLeft(xDiff, rect);
    	m_leftTop.x += xDiff;
    	m_size.x -= xDiff;
    }
 
    private float restrictLeft(float xDiff, RectF rect)
    {
    	if(m_leftTop.x + xDiff < rect.left)
    	{
     		xDiff += (rect.left - m_leftTop.x - xDiff);
    	}
    	if(m_size.x - xDiff < MinSize)
    	{
    		xDiff = m_size.x - MinSize;
    	}
    	return xDiff;
    }
   
    private void dragTop(PointF event, RectF rect)
    {
    	float offset = event.y - m_last.y;
    	if(Math.abs(offset) < 1e-3)
    	{
    		return;
    	}
    	float yDiff = event.y - m_last.y;
    	yDiff = restrictTop(yDiff, rect);
    	m_leftTop.y += yDiff;
    	m_size.y -= yDiff;
    }
 
    private float restrictTop(float yDiff, RectF rect)
    {
    	if(m_leftTop.y + yDiff < rect.top)
    	{
     		yDiff += (rect.top - m_leftTop.y - yDiff);
    	}
    	if(m_size.y - yDiff < MinSize)
    	{
    		yDiff = m_size.y - MinSize;
    	}
    	return yDiff;
    }
    
    private float restrictBottom(float yDiff, RectF rect)
    {
       	if(m_leftTop.y + m_size.y + yDiff > rect.bottom)
    	{
    		yDiff += rect.bottom - m_size.y - m_leftTop.y - yDiff;
//    		m_leftTop.y = rect.bottom - m_size.y - yDiff;
    	}
    	if(m_size.y + yDiff < MinSize)
    	{
    		yDiff = MinSize - m_size.y;
    	}
    	return yDiff;
    }
   
    private void dragBottom(PointF event, RectF rect)
    {
    	float offset = event.y - m_last.y;
    	if(Math.abs(offset) < 1e-3)
    	{
    		return;
    	}
    	float yDiff = event.y - m_last.y;
        yDiff = restrictBottom(yDiff, rect);
    	m_size.y += yDiff;
    }
                	
    private float restrictRight(float xDiff, RectF rect)
    {
       	if(m_leftTop.x + m_size.x + xDiff > rect.right)
    	{
    		xDiff += rect.right - m_size.x - m_leftTop.x - xDiff;
//    		m_leftTop.x = rect.right - m_size.x - xDiff;
    	}
    	if(m_size.x + xDiff < MinSize)
    	{
    		xDiff = MinSize - m_size.x;
    	}
    	return xDiff;
    }
    
    private void dragRight(PointF event, RectF rect)
    {
    	float offset = event.x - m_last.x;
    	if(Math.abs(offset) < 1e-3)
    	{
    		return;
    	}
    	
    	float xDiff = event.x - m_last.x;
    	xDiff = restrictRight(xDiff, rect);
    	m_size.x += xDiff;
    }
   
    private void dragSelection(MotionEvent event)
    {
    	float xDiff = event.getX() - m_last.x;
    	float yDiff = event.getY() - m_last.y;
    	m_leftTop.x += xDiff;
    	m_leftTop.y += yDiff;
    }
    
    private void adjustSelection(boolean keepSize)
    {
    	float width = (float)m_imageView.getWidth();
    	float height = (float)m_imageView.getHeight();
    	restrictToRect(0, 0, width, height, true);
 
    	float[] lt = new float[2];
    	lt[0] = 0;
    	lt[1] = 0;
                	
    	float[] rb = new float[2];
    	rb[0] = 0;
    	rb[1] = 0;
                	
    	float[] leftTop = new float[2];
    	float[] rightBottom = new float[2];
 
    	if(m_bitmap != null)
    	{
    		Bitmap bmp = m_bitmap;
    		rb[0] = bmp.getWidth();
    		rb[1] = bmp.getHeight();
    	}
                	
    	m_imageToScreen.mapPoints(leftTop, lt);
    	m_imageToScreen.mapPoints(rightBottom, rb);
                	
    	restrictToRect(leftTop[0], leftTop[1], rightBottom[0], rightBottom[1], keepSize);
    }

	@Override
	public Bitmap getCroppedBitmap() {
    	Bitmap bm = null;
        try
        {
            RectF selection = getImageSelection(1.0f);
            Rect rc = new Rect(Math.round(selection.left), Math.round(selection.top), Math.round(selection.right), Math.round(selection.bottom));
            Bitmap cropped = Bitmap.createBitmap(m_bitmap, rc.left, rc.top, rc.width(), rc.height());
            bm = cropped;
        }
        catch(OutOfMemoryError err)
        {
        } 
        return bm;
	}
    
}
