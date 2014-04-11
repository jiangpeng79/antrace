package com.jiangpeng.android.antrace;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.jiangpeng.android.antrace.Objects.path;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

public class PreviewActivity extends Activity {
	private String m_filename = null;
	private ProgressDialog m_progress = null;
	private PreviewImageView m_imageView = null;
	private Bitmap m_gray = null;
	private Bitmap m_mono = null;
	private Button m_ok = null;
	private Button m_cancel = null;
	private SeekBar m_thresholdSeek = null;
	private BitmapFactory.Options m_ops = null;
	private Thread m_t = null;
	private int m_processed = -1;
	private int m_current = 127;
	private ProgressBar m_progressBar = null;
    public static int CODE_SHARE_IMAGE = 1115;
    public static String FILENAME = "FILENAME";
    public static int STATE_START = 0;
    public static int STATE_LOADED = 1;
    public static int STATE_CROPPED = 2;
    public static int STATE_MONO = 3;
    public static int STATE_TRACE = 4;
    public static int STATE_SAVE = 5;
    
    public static int TYPE_SVG = 1;
    public static int TYPE_DXF = 2;
    public static int TYPE_PDF = 2;
    private int m_state = STATE_START;
	
    static {
        System.loadLibrary("antrace");
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_image);
        
        m_imageView = (PreviewImageView)findViewById(R.id.previewImageView);
        m_filename = this.getIntent().getStringExtra(PreviewActivity.FILENAME);
       	m_progress = ProgressDialog.show(this, getResources().getString(R.string.empty), getResources().getString(R.string.loading), true);	
		Thread t = new Thread(new LoadImageThread());
		t.start();
		
        m_ok = (Button)findViewById(R.id.okButton);
        m_cancel = (Button)findViewById(R.id.cancelButton);
        m_thresholdSeek = (SeekBar)findViewById(R.id.thresholdSeek);
        m_progressBar = (ProgressBar)findViewById(R.id.thresholdProgress);
        
        OnClickListener okListener = new OkListener();
        m_ok.setOnClickListener(okListener);
        
        OnClickListener cancelListener = new CancelListener();
        m_cancel.setOnClickListener(cancelListener);

        ThresholdListener thresholdListener = new ThresholdListener();
        m_thresholdSeek.setOnSeekBarChangeListener(thresholdListener);
        m_thresholdSeek.setVisibility(View.INVISIBLE);
        m_progressBar.setVisibility(View.INVISIBLE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK && requestCode == CODE_SHARE_IMAGE)
		{
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	class OkListener implements OnClickListener
	{
		@Override
		public void onClick(View v) {
			if(m_state == STATE_LOADED)
			{
				m_progress = ProgressDialog.show(PreviewActivity.this, getResources().getString(R.string.empty), getResources().getString(R.string.loading), true);	
				Thread t = new Thread(new CropThread());
				t.start();
				return;
			}
			if(m_state == STATE_CROPPED)
			{
		       	m_progress = ProgressDialog.show(PreviewActivity.this, getResources().getString(R.string.empty), getResources().getString(R.string.loading), true);	
				Thread t = new Thread(new GrayscaleThread());
				t.start();
				m_ok.setText(R.string.next);
				return;
			}
			if(m_state == STATE_MONO)
			{
				m_thresholdSeek.setVisibility(View.INVISIBLE);
		       	m_progress = ProgressDialog.show(PreviewActivity.this, getResources().getString(R.string.empty), getResources().getString(R.string.loading), true);	
				Thread t = new Thread(new TraceThread());
				t.start();
				m_ok.setText(R.string.save);
				return;
			}
			if(m_state == STATE_TRACE)
			{
				showSaveDialog();
				return;
			}
		}
	};
	
	class CancelListener implements OnClickListener
	{
	    @Override
	    public void onClick(View v) {
			if(m_state == STATE_LOADED)
			{
				endCrop();
			}
	        return;
	    }
	};
	
	private Handler m_thresholdHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			m_t = null;
            if(m_current != m_processed)
            {
            	checkAndStartThreshold(m_current);
            }
            else
            {
                Bitmap bmp = (Bitmap)msg.obj;
                m_mono = bmp;
                m_imageView.setImage(m_mono);
                m_progressBar.setVisibility(View.INVISIBLE);
            }
		}
	};

	class ThresholdThread implements Runnable
	{
		private int m_value = 127;
		public ThresholdThread(int v)
		{
			m_value = v;
		}
		@Override
		public void run() {
    		Bitmap bnw = Bitmap.createBitmap(m_gray.getWidth(), m_gray.getHeight(), Bitmap.Config.ARGB_8888);
    		Utils.threshold(m_gray, m_value, bnw);
			m_processed = m_value;
            Message msg = m_thresholdHandler.obtainMessage(m_value, bnw);
            m_thresholdHandler.sendMessage(msg);
		}
	}

	void checkAndStartThreshold(int t)
	{
		m_current = t;
		if(m_current == m_processed)
		{
			return;
		}
		m_progressBar.setVisibility(View.VISIBLE);
		if(m_t == null)
		{                
			m_imageView.setImage(m_gray);
			m_t = new Thread(new ThresholdThread(t));
			m_t.start();
		}
		else
		{
			m_current = t;
		}
	}

	class ThresholdListener implements SeekBar.OnSeekBarChangeListener
	{
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			checkAndStartThreshold(progress);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	}

	private void showSaveDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View view = getLayoutInflater().inflate(R.layout.save_file, null);
		final EditText nameEdit = (EditText)view.findViewById(R.id.filenameEdit);
		final RadioGroup typeRadio = (RadioGroup)view.findViewById(R.id.typeGroup);
		String name = "output.svg";
		String fullname = FileUtils.getRootFolder() + FileUtils.sep + name;
		nameEdit.setText(fullname);
		typeRadio.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				String text = nameEdit.getText().toString();
				String path = FileUtils.getPath(text);
				String shortName = FileUtils.getShortName(text);
				String stem = FileUtils.getFileStem(shortName);
				if(checkedId == R.id.dxfRadio)
				{
					String newname = path + stem + ".dxf";
					nameEdit.setText(newname);
				}
				else if(checkedId == R.id.svgRadio)
				{
					String newname = path + stem + ".svg";
					nameEdit.setText(newname);
				}
			}
		});
		builder.setView(view);
		builder.setTitle(R.string.save_to_file);
		builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	String filename = nameEdit.getText().toString();
            	int type = TYPE_SVG;
            	if(typeRadio.getCheckedRadioButtonId() == R.id.dxfRadio)
            	{
            		type = TYPE_DXF;
            	}
            	else if(typeRadio.getCheckedRadioButtonId() == R.id.svgRadio)
            	{
            		type = TYPE_SVG;
            	}
                // a choice has been made!		
        		dialog.dismiss();
              	m_progress = ProgressDialog.show(PreviewActivity.this, getResources().getString(R.string.empty), getResources().getString(R.string.loading), true);	
        		Thread t = new Thread(new SaveFileThread(type, filename));
        		t.start();
        		return;
            }
        });
        
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // a choice has been made!		
        		dialog.dismiss();
        		return;
            }
        });

		AlertDialog dlg = builder.create();
		dlg.show();
	}

    private Handler m_handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
        	if(m_progress != null)
        	{
        		m_progress.dismiss();
        		m_progress = null;
        	}
            
        	if(m_state == STATE_MONO)
        	{
        		m_state = STATE_TRACE;
        		path p = (path)msg.obj;

        		if(p == null)
        		{
        			Toast toast = Toast.makeText(PreviewActivity.this, R.string.trace_failed, Toast.LENGTH_SHORT);
        			toast.show();
        		}
        		else
        		{
        			m_imageView.setPath(p);
        		}
        		return;
        	}
        	if(m_state == STATE_TRACE)
        	{
        		Boolean ret = (Boolean)msg.obj;
        		if(!ret)
        		{
        			Toast toast = Toast.makeText(PreviewActivity.this, R.string.save_failed, Toast.LENGTH_SHORT);
        			toast.show();
        		}
        		return;
        	}
            Bitmap bmp = (Bitmap)msg.obj;
        	if(bmp == null)
        	{
                Toast toast = Toast.makeText(PreviewActivity.this, R.string.check_your_files, Toast.LENGTH_SHORT);
                toast.show();
                PreviewActivity.this.finish();
            	return;
        	}
            if (bmp.isRecycled())
            {
                Toast toast = Toast.makeText(PreviewActivity.this, R.string.check_your_files, Toast.LENGTH_SHORT);
                toast.show();
                PreviewActivity.this.finish();
            }
            else
            {
            	m_imageView.setImage(bmp);
            	if(m_state == STATE_START)
            	{
            		m_state = STATE_LOADED;
            		m_ok.setText(R.string.crop);
            		m_cancel.setText(R.string.skip);
            		m_gray = bmp;
            		m_imageView.startCrop();
            		return;
            	}
            	if(m_state == STATE_LOADED)
            	{
            		m_gray = bmp;
            		endCrop();
            		return;
            	}
            	if(m_state == STATE_CROPPED)
            	{
            		m_gray = bmp;
            		m_thresholdSeek.setVisibility(View.VISIBLE);
            		checkAndStartThreshold(127);
            		m_state = STATE_MONO;
            		return;
            	}
            }
        }
    };
    
    private void endCrop()
    {
		m_ok.setText(R.string.next);
		m_cancel.setText(android.R.string.cancel);
		m_state = STATE_CROPPED;
		m_imageView.endCrop();
    }

	class SaveFileThread implements Runnable
	{
		int m_type = TYPE_SVG;
		String m_name;
		public SaveFileThread(int t, String n)
		{
			m_type = t;
			m_name = n;
		}
		@Override
		public void run() {
			Boolean ret = true;
			if(m_type == TYPE_DXF)
			{
				ret = Utils.saveDXF(m_name, m_mono.getWidth(), m_mono.getHeight());
			}
			else if(m_type == TYPE_SVG)
			{
				ret = Utils.saveSVG(m_name, m_mono.getWidth(), m_mono.getHeight());
			}
            Message msg = m_handler.obtainMessage(0, ret);
            m_handler.sendMessage(msg);
		}
	};

	class GrayscaleThread implements Runnable
	{
		@Override
		public void run() {
    		if(m_gray != null)
    		{
    			Bitmap bm = m_gray;
    			try
    			{
    				m_gray = Bitmap.createBitmap(bm);
    				Utils.grayScale(bm, m_gray);
    			}
    	        catch(OutOfMemoryError err)
    	        {
    	        } 

                Message msg = m_handler.obtainMessage(0, m_gray);
                m_handler.sendMessage(msg);
    		}
		}
	};

	class TraceThread implements Runnable
	{
		@Override
		public void run() {
    		if(m_gray != null)
    		{
				path p = Utils.traceImage(m_mono);

				String svgFile = FileUtils.tempSvgFile();
				Utils.saveSVG(svgFile, m_mono.getWidth(), m_mono.getHeight());
                Message msg = m_handler.obtainMessage(0, p);
                m_handler.sendMessage(msg);
    		}
		}
	};
	
	float distance(PointF p1, PointF p2)
	{
		return (float)Math.sqrt(((p2.y - p1.y) * (p2.y - p1.y) + (p2.x - p1.x) * (p2.x - p1.x)));
	}
	
	class CropThread implements Runnable
	{
		@Override
		public void run() {
    		if(m_gray != null)
    		{
    		    Matrix matrix = new Matrix();
    		    float w1 = distance(m_imageView.getLeftBottom(), m_imageView.getRightBottom());
    		    float w2 = distance(m_imageView.getRightTop(), m_imageView.getLeftTop());

    		    float h1 = distance(m_imageView.getLeftBottom(), m_imageView.getLeftTop());
    		    float h2 = distance(m_imageView.getRightTop(), m_imageView.getRightBottom());
    		    float w = Math.max(w1, w2);
    		    float h = Math.max(h1, h2);
    		    float[] dst = new float[] {
    		    		0, 0, w, 0, w, h, 0, h
    		    };
    		    float[] src = new float[] {
    		    		m_imageView.getLeftTop().x,
    		    		m_imageView.getLeftTop().y,
    		    		m_imageView.getRightTop().x,
    		    		m_imageView.getRightTop().y,
    		    		m_imageView.getRightBottom().x,
    		    		m_imageView.getRightBottom().y,
    		    		m_imageView.getLeftBottom().x,
    		    		m_imageView.getLeftBottom().y
    		    };

    		    float[] o = new float[]
    		    		{
    		    		0, 0, m_gray.getWidth(), 0, m_gray.getWidth(), m_gray.getHeight(), 0, m_gray.getHeight()
    		    		};
//    		    float[] newpts = new float[8];
    		    float[] newo = new float[8];
    		    matrix.setPolyToPoly(src, 0, dst, 0, 4);
//    		    matrix.mapPoints(newpts, src);
    		    matrix.mapPoints(newo, o);
    		    Bitmap ret = Bitmap.createBitmap(m_gray, 0, 0, (int)m_gray.getWidth(), (int)m_gray.getHeight(), matrix, true);
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
    		    /*
    		    int nw = ret.getWidth();
    		    int nh = ret.getHeight();
    		    */
                Message msg = m_handler.obtainMessage(0, ret);
                m_handler.sendMessage(msg);
    		}
		}
	};
	
	class LoadImageThread implements Runnable
	{
		@Override
		public void run() {
			if(m_filename == null || m_filename.length() == 0)
			{
            	Message msg = m_handler.obtainMessage(0, null);
            	m_handler.sendMessage(msg);
            	return;
			}
			m_ops = ImageUtils.getBmpOptions(m_filename);
//			m_originalSize = new Point(m_ops.outWidth, m_ops.outHeight);
//            Bitmap ret = ImageUtils.getFullScreenImageFromFilename(EditImageActivity.this, m_filename);
			m_ops.inSampleSize = ImageUtils.computeInitialSampleSize(m_ops, -1, 512 * 1024);
			m_ops.inPreferredConfig = Bitmap.Config.ARGB_8888;
	        FileInputStream stream;
	        try
            {
	        	stream = new FileInputStream(m_filename);
            } 
	        catch (FileNotFoundException e)
   	        {
            	Message msg = m_handler.obtainMessage(0, null);
            	m_handler.sendMessage(msg);
	        	return;
   	        }
	        Bitmap bm = null;
	        try
	        {
	        	m_ops.inJustDecodeBounds = false;
	        	bm = BitmapFactory.decodeStream(stream, null, m_ops);
	        }
	        catch(OutOfMemoryError err)
	        {
	        } 
	
            if(bm == null || bm.getWidth() < 2 || bm.getHeight() < 2)
            {
            	Message msg = m_handler.obtainMessage(0, null);
            	m_handler.sendMessage(msg);
            	return;
            }
            
            float a = FileUtils.getPhotoAngle(m_filename);
    		Matrix m = new Matrix();
    		m.reset();
    		m.postRotate(a);
    		try
    		{
    			Bitmap ret = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
    			if(ret != bm)
    			{
    				bm.recycle();
    				bm = ret;
    			}
    		}
    		catch(OutOfMemoryError e)
    		{
    			bm = null;
    		}	
    		
            Message msg = m_handler.obtainMessage(0, bm);
            m_handler.sendMessage(msg);
		}
	}

	@Override
	protected void onDestroy() {
		if(m_progress != null)
		{
			if(m_progress.isShowing())
			{
				m_progress.dismiss();
			}
			m_progress = null;
		}
		super.onDestroy();
		Utils.clearState();
	}

}
