package com.jiangpeng.android.antrace.Objects;

import com.jiangpeng.android.antrace.Utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public abstract class ImageInteraction {
	public enum HitTestResult
	{
		Top,
		Bottom,
		Left,
		Right,
		TopLeft,
		TopRight,
		BottomLeft,
		BottomRight,
		Inside,
		Outside,
		None
	};
	protected static final int NONE = 0;
    protected static final int DRAG = 1;
    protected static final int ZOOM = 2;
    protected static final int ZOOM_SELECTION = 4;
    protected static final int DRAG_SELECTION = 5;
    protected Matrix m_imageToScreen = new Matrix();
    protected Matrix m_screenToImage = new Matrix();
    protected Bitmap m_bitmap = null;
    protected Paint m_paint = new Paint();

    protected static float HitRadius = 40f;
    protected static final float MinSize = 80f;
    protected int m_mode = NONE;
    protected HitTestResult m_hitTest = HitTestResult.None;
    
    // Remember some things for zooming
    protected PointF m_start = new PointF();
    protected PointF m_last = new PointF();
    protected ImageView m_imageView = null;
    protected boolean m_isCropping = false;
    
    public abstract void draw(Canvas c);
    public abstract boolean onTouch(View v, MotionEvent rawEvent);
    public abstract void startCrop();

    public abstract Bitmap getCroppedBitmap();

    public ImageInteraction(ImageView view)
    {
    	m_imageView = view;
    	HitRadius = Utils.getDPI(view.getContext()) / 7;
    }
    
    protected RectF generateRect(PointF center, float radiusX, float radiusY)
    {
    	return new RectF(center.x - radiusX / 2, center.y - radiusY / 2, center.x + radiusX / 2, center.y + radiusY / 2);
    }
	
	protected float distance(PointF p1, PointF p2)
	{
		return (float)Math.sqrt(((p2.y - p1.y) * (p2.y - p1.y) + (p2.x - p1.x) * (p2.x - p1.x)));
	}
	
    protected RectF getImageRect()
    {
		float[] pts = new float[4];
		float w = m_bitmap.getWidth();
		float h = m_bitmap.getHeight();
		pts[0] = 0;
		pts[1] = 0;

		pts[2] = w;
		pts[3] = h;

    	float[] ret = new float[4];
    	m_imageToScreen.mapPoints(ret, pts);
    	
    	return new RectF(ret[0], ret[1], ret[2], ret[3]);
    }
    
    protected PointF correctPoint(RectF rc, PointF pt)
    {
    	if(rc.contains(pt.x, pt.y))
    	{
    		return pt;
    	}
    	
    	float x = pt.x;
    	float y = pt.y;
    	if(pt.y < rc.top)
    	{
    		y = rc.top;
    	}

    	if(pt.y > rc.bottom)
    	{
    		y = rc.bottom;
    	}

    	if(pt.x < rc.left)
    	{
    		x = rc.left;
    	}

    	if(pt.x > rc.right)
    	{
    		x = rc.right;
    	}
    	
    	return new PointF(x, y);
    }
    
    public void setBitmap(Bitmap bmp)
    {
    	m_bitmap = bmp;
    	resetImagePosition();
    }

    protected float calculateScale()
    {
        double w = m_bitmap.getWidth();
        double h = m_bitmap.getHeight();
        return calculateScale(w, h);
    }

    protected float calculateScale(double w, double h)
    {
    	double wr = m_imageView.getWidth() / w;
    	double hr = m_imageView.getHeight() / h;
    	return (float)Math.min(wr, hr);
    }
   
    protected void resetImagePosition()
    {
        m_imageToScreen.reset();
        
        m_imageToScreen.postTranslate(-m_bitmap.getWidth() / 2f, -m_bitmap.getHeight() / 2f);
        
        float matrix_values[] = {1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f};  
        Matrix flipMat = new Matrix();
        flipMat.setValues(matrix_values);   
        m_imageToScreen.postConcat(flipMat);
        m_imageToScreen.postRotate((float) (0 / Math.PI * 180f));
        
        double w = m_bitmap.getWidth();
        double h = m_bitmap.getHeight();
        m_imageToScreen.postTranslate((float)w / 2f, (float)h / 2f);
        
        float scale = calculateScale();
        m_imageToScreen.postScale(scale, scale);
        
        float redundantYSpace = (float)m_imageView.getHeight() - (float)(h * scale);
        float redundantXSpace = (float)m_imageView.getWidth() - (float)(w * scale);
        m_imageToScreen.postTranslate((float)redundantXSpace / 2f, (float)redundantYSpace / 2f);
        
        Matrix inverse = new Matrix();
        if(m_imageToScreen.invert(inverse))
        {
        	m_screenToImage.set(inverse);
        }
        m_imageView.setImageMatrix(m_imageToScreen);
    }

    public PointF toScreen(PointF pt)
    {
    	float[] input = new float[2];
    	input[0] = pt.x;
    	input[1] = pt.y;
    	
    	float[] ret = new float[2];
    	m_imageToScreen.mapPoints(ret, input);
    	PointF p = new PointF(ret[0], ret[1]);
    	return p;
    }

    public PointF toBitmap(PointF pt)
    {
    	float[] input = new float[2];
    	input[0] = pt.x;
    	input[1] = pt.y;
    	
    	float[] ret = new float[2];
    	m_screenToImage.mapPoints(ret, input);
    	PointF p = new PointF(ret[0], ret[1]);
    	return p;
    }
    
    public void endCrop()
    {
    	m_isCropping = false;
    	m_imageView.invalidate();
    }
    
	public void init() {
        m_imageToScreen.setTranslate(1f, 1f);
        m_imageView.setImageMatrix(m_imageToScreen);
        m_imageView.setScaleType(ScaleType.MATRIX);
	}

}
