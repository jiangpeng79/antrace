package com.jiangpeng.android.antrace;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ImageView;

public class PreviewImageView extends ImageView {
    private Matrix m_imageToScreen = new Matrix();
    private Matrix m_screenToImage = new Matrix();
    private Bitmap m_bitmap = null;
  
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
    }
    
    public void setImage(Bitmap bm) { 
        super.setImageBitmap(bm);
        m_bitmap = bm;
        if(m_bitmap != null)
        {
        	resetImagePosition();
        }
    }

    @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

}
