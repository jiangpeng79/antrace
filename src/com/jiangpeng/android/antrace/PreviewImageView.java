package com.jiangpeng.android.antrace;

import com.jiangpeng.android.antrace.Objects.curve;
import com.jiangpeng.android.antrace.Objects.dpoint;
import com.jiangpeng.android.antrace.Objects.path;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

public class PreviewImageView extends ImageView {
    private Matrix m_imageToScreen = new Matrix();
    private Matrix m_screenToImage = new Matrix();
    private Bitmap m_bitmap = null;
    private path m_path = null;
    private Paint m_paint = new Paint();
    private Path m_p = new Path();
  
    public boolean hasPath()
    {
    	return m_path != null;
    }
    
    public void setPath(path p)
    {
    	m_path = p;
    	this.invalidate();
    }

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
//        m_context = context;
        m_imageToScreen.setTranslate(1f, 1f);
        setImageMatrix(m_imageToScreen);
        setScaleType(ScaleType.MATRIX);
		m_paint.setColor(Color.RED);
		m_paint.setStyle(Paint.Style.STROKE);
		m_paint.setStrokeCap(Paint.Cap.ROUND);
		m_paint.setStrokeWidth(2.0f);
		m_paint.setAntiAlias(true);
    }
    
    public void setImage(Bitmap bm) { 
        super.setImageBitmap(bm);
        m_bitmap = bm;
        if(m_bitmap != null)
        {
        	resetImagePosition();
        }
    }

    private float[] transform(dpoint dpt)
    {
    	float[] input = new float[2];
    	input[0] = (float)dpt.x;
    	input[1] = (float)dpt.y;
    	
    	float[] ret = new float[2];
    	m_imageToScreen.mapPoints(ret, input);
    	return ret;
    }

    @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if(m_path == null)
		{
			return;
		}
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
				m_p.moveTo(prev[0], prev[1]);
				if(todraw.tag[i] == curve.POTRACE_CURVETO)
				{
					float[] p1 = transform(todraw.c[i][0]);
					float[] p2 = transform(todraw.c[i][1]);
					float[] p3 = transform(todraw.c[i][2]);
					m_p.cubicTo(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
					/*
					Log.v("CURVE", Double.toString(p1[0]) + ", " + Double.toString(p1[1]) + "  "
						+ Double.toString(p2[0]) + ", " + Double.toString(p2[1]) + "  "
						+ Double.toString(p3[0]) + ", " + Double.toString(p3[1]));
						*/
					prev = p3;
				}
				else if(todraw.tag[i] == curve.POTRACE_CORNER)
				{
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
			Log.v("NEXT CURVE", "NEXT CURVE");
		}
	}
}
