package com.jiangpeng.android.antrace;

import com.jiangpeng.android.antrace.Objects.ImageInteraction;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class PreviewImageView extends ImageView {
    private ImageInteraction m_interaction = null;
    
    public void setInteraction(ImageInteraction interaction)
    {
    	m_interaction = interaction;
    }
    
    public ImageInteraction getInteraction()
    {
    	return m_interaction;
    }

    public void startCrop()
    {
    	m_interaction.startCrop();
    	/*
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
    	invalidate();
    	*/
    }
    
    public void endCrop()
    {
    	m_interaction.endCrop();
    	/*
    	m_isCropping = false;
    	invalidate();
    	*/
    }

    /*
    private float calculateScale()
    {
        double w = m_bitmap.getWidth();
        double h = m_bitmap.getHeight();
        return calculateScale(w, h);
    }

    private float calculateScale(double w, double h)
    {
    	double wr = getWidth() / w;
    	double hr = getHeight() / h;
    	return (float)Math.min(wr, hr);
    }
   
    private void resetImagePosition()
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
        
        float redundantYSpace = (float)getHeight() - (float)(h * scale);
        float redundantXSpace = (float)getWidth() - (float)(w * scale);
        m_imageToScreen.postTranslate((float)redundantXSpace / 2f, (float)redundantYSpace / 2f);
        
        Matrix inverse = new Matrix();
        if(m_imageToScreen.invert(inverse))
        {
        	m_screenToImage.set(inverse);
        }
        setImageMatrix(m_imageToScreen);
    }
    */
  
    public PreviewImageView(Context context) {
        super(context);
        init(context);
    }
 
    public PreviewImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
 
    public PreviewImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

	public static DisplayMetrics getDisplayMetrics(Context context)
	{
	    DisplayMetrics metrics = new DisplayMetrics();
	    WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
	    wm.getDefaultDisplay().getMetrics(metrics); 
	    
	    return metrics;
	}

    private void init(Context context)
    {
        super.setClickable(true);
        /*
        m_imageToScreen.setTranslate(1f, 1f);
        setImageMatrix(m_imageToScreen);
        setScaleType(ScaleType.MATRIX);
        */
    }
    
    public void init()
    {
        m_interaction.init();
        setOnTouchListener(m_touchListener);
    }
    
    public void setImage(Bitmap bm) { 
        super.setImageBitmap(bm);
        if(m_interaction != null)
        {
        	m_interaction.setBitmap(bm);
        }
        /*
        if(m_bitmap != null)
        {
        	resetImagePosition();
        }
        */
    }

    /*
    private float[] transform(dpoint dpt)
    {
    	float[] input = new float[2];
    	input[0] = (float)dpt.x;
    	input[1] = (float)dpt.y;
    	
    	float[] ret = new float[2];
    	m_imageToScreen.mapPoints(ret, input);
    	return ret;
    }

    private PointF toScreen(PointF pt)
    {
    	float[] input = new float[2];
    	input[0] = pt.x;
    	input[1] = pt.y;
    	
    	float[] ret = new float[2];
    	m_imageToScreen.mapPoints(ret, input);
    	PointF p = new PointF(ret[0], ret[1]);
    	return p;
    }

    private PointF toBitmap(PointF pt)
    {
    	float[] input = new float[2];
    	input[0] = pt.x;
    	input[1] = pt.y;
    	
    	float[] ret = new float[2];
    	m_screenToImage.mapPoints(ret, input);
    	PointF p = new PointF(ret[0], ret[1]);
    	return p;
    }

    private void drawPath(Canvas canvas)
    {
		if(m_path == null)
		{
			return;
		}
		m_paint.setColor(Color.RED);
		m_paint.setStyle(Paint.Style.STROKE);
		m_paint.setStrokeCap(Paint.Cap.ROUND);
		m_paint.setStrokeWidth(2.0f);
		m_paint.setAntiAlias(false);
		path next = m_path;
		while(next != null)
		{
			if(next.curve.n == 0)
			{
				next = next.next;
				continue;
			}
			float[] prev = transform(next.curve.c[next.curve.n - 1][2]);
			curve todraw = next.curve;
//			Path p = new Path();
			for(int i = 0; i < next.curve.n; ++i)
			{
				if(todraw.tag[i] == curve.POTRACE_CURVETO)
				{
					m_p.moveTo(prev[0], prev[1]);
					float[] p1 = transform(todraw.c[i][0]);
					float[] p2 = transform(todraw.c[i][1]);
					float[] p3 = transform(todraw.c[i][2]);
					m_p.cubicTo(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
//					Log.v("CURVE", Double.toString(p1[0]) + ", " + Double.toString(p1[1]) + "  "
//						+ Double.toString(p2[0]) + ", " + Double.toString(p2[1]) + "  "
//						+ Double.toString(p3[0]) + ", " + Double.toString(p3[1]));
//						
					prev = p3;
				}
				else if(todraw.tag[i] == curve.POTRACE_CORNER)
				{
					m_p.moveTo(prev[0], prev[1]);
//					dpoint pt = todraw.c[i][1];
					float[] cur = transform(todraw.c[i][1]);
					m_p.lineTo(cur[0], cur[1]);
//					Log.v("CORNER", Double.toString(pt.x) + ", " + Double.toString(pt.y));

					cur = transform(todraw.c[i][2]);
//					pt = todraw.c[i][2];
					m_p.lineTo(cur[0], cur[1]);
//					Log.v("CORNER", Double.toString(pt.x) + ", " + Double.toString(pt.y));
					prev = cur;
				}
//				m_p.close();
				canvas.drawPath(m_p, m_paint);
			}
			next = next.next;
//			Log.v("NEXT CURVE", "NEXT CURVE");
		}
    }
    */

    @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		m_interaction.draw(canvas);
		
		/*
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
			canvas.drawLines(pts, m_paint); 
			
			float radius = 8;
			canvas.drawCircle(m_leftTop.x, m_leftTop.y, radius, m_paint);
			canvas.drawCircle(m_rightTop.x, m_rightTop.y, radius, m_paint);
			canvas.drawCircle(m_rightBottom.x, m_rightBottom.y, radius, m_paint);
			canvas.drawCircle(m_leftBottom.x, m_leftBottom.y, radius, m_paint);
			return;
		}
		*/
	}

	private OnTouchListener m_touchListener = new OnTouchListener()
    {
		@Override
		public boolean onTouch(View v, MotionEvent rawEvent) {
			return m_interaction.onTouch(v, rawEvent);
			/*
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
                    performClick();
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
           	invalidate();
            return true; // indicate event was handled
		*/
		}
    };
    
    /*
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
    
    private RectF generateRect(PointF center, float radiusX, float radiusY)
    {
    	return new RectF(center.x - radiusX / 2, center.y - radiusY / 2, center.x + radiusX / 2, center.y + radiusY / 2);
    }
    
    public PointF getLeftTop()
    {
    	return m_interaction.getLeftTop();
    }

    public PointF getRightTop()
    {
    	return m_interaction.getRightTop();
    }

    public PointF getRightBottom()
    {
    	return m_interaction.getRightBottom();
    }

    public PointF getLeftBottom()
    {
    	return m_interaction.getLeftBottom();
    }
    
    private RectF getImageRect()
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
    
    private PointF correctPoint(RectF rc, PointF pt)
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
    */
}
