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
    public static int STATE_MONO = 2;
    public static int STATE_TRACE = 3;
    public static int STATE_SAVE = 4;
    
    public static int TYPE_SVG = 1;
    public static int TYPE_DXF = 2;
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
		FileUtils.checkAndCreateFolder(FileUtils.getRootFolder() + FileUtils.sep + MainActivity.TEMP_FOLDER);
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
				Thread t = new Thread(new GrayscaleThread());
				t.start();
				m_ok.setText("Next");
				return;
			}
			if(m_state == STATE_MONO)
			{
				m_thresholdSeek.setVisibility(View.INVISIBLE);
		       	m_progress = ProgressDialog.show(PreviewActivity.this, getResources().getString(R.string.empty), getResources().getString(R.string.loading), true);	
				Thread t = new Thread(new TraceThread());
				t.start();
				m_ok.setText("Save");
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
		if(typeRadio.getCheckedRadioButtonId() == R.id.dxfRadio)
		{
			name = "output.dxf";
		}
		else if(typeRadio.getCheckedRadioButtonId() == R.id.svgRadio)
		{
			name = "output.svg";
		}
		String path = FileUtils.getRootFolder() + FileUtils.sep + name;
		nameEdit.setText(path);
		builder.setView(view);
		builder.setTitle("Save to file");
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
        			Toast toast = Toast.makeText(PreviewActivity.this, "Trace failed...", Toast.LENGTH_SHORT);
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
        			Toast toast = Toast.makeText(PreviewActivity.this, "Save failed...", Toast.LENGTH_SHORT);
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
            		m_gray = bmp;
            		return;
            	}
            	if(m_state == STATE_LOADED)
            	{
            		m_thresholdSeek.setVisibility(View.VISIBLE);
            		checkAndStartThreshold(127);
            		m_state = STATE_MONO;
            		return;
            	}
            }
        }
    };

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
				ret = Utils.saveDXF(m_name);
			}
			else if(m_type == TYPE_SVG)
			{
				ret = Utils.saveSVG(m_name);
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
                Message msg = m_handler.obtainMessage(0, p);
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

	/*
	public void saveCurrentImage()
	{
		if(m_bitmap != null)
		{
			m_progress = ProgressDialog.show(this, "", 
					getResources().getString(R.string.loading), true);
			Thread t = new Thread(new SaveToFileThread());
			t.start();
		}
	}

   	class SaveToFileThread implements Runnable
   	{
   		private Uri saveToFile()
   		{
   			String tempfile = m_photoFile;
        	FileOutputStream out;
        	try
        	{
        		out = new FileOutputStream(tempfile);
        	} 
        	catch (FileNotFoundException e)
        	{
        		return null;
        	}
        	if(!m_bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out))
        	{
        		return null;
        	}
        	try
        	{
        		out.flush();
        		out.close();
        	} 
        	catch (IOException e)
        	{
        		return null;
        	}
        	
        	String[] s = {BaseColumns._ID, MediaColumns.DATA};
        	String sel = "_data=?";
        	String[] args = { tempfile };
        	Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, s, sel, args, null);
        		
        	ContentValues values = new ContentValues(); 	
        	values.put(Media.TITLE, "Image");
        	values.put(Images.Media.BUCKET_ID, tempfile.hashCode());
        	values.put(Images.Media.BUCKET_DISPLAY_NAME, "Watermark Blank");
        	values.put(Images.Media.MIME_TYPE, "image/jpeg");
        	values.put(Media.DESCRIPTION, "Watermark Blank Result");
        	values.put("_data", m_photoFile);
  
        	Uri u = null;
        	if(cursor != null)
        	{
        		if(cursor.moveToFirst())
        		//while(cursor.moveToNext())
        		{
        			int imageID = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
        			Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(imageID));
        			
        			getContentResolver().update(uri, values, null, null);
        			u = uri;
        			//m_imageContext.activity.getContentResolver().delete(uri, null, null);
        		}
        		else
        		{
        			u = getContentResolver().insert( Media.EXTERNAL_CONTENT_URI , values);
        		}
       			cursor.close();
        	}
       		else
       		{
       			u = getContentResolver().insert( Media.EXTERNAL_CONTENT_URI , values);
       		}
        	
        	return u;
   		}
	
		@Override
		public void run() {
			Uri u = saveToFile();
			
            Message msg = m_doneHandler.obtainMessage(0, u);
            m_doneHandler.sendMessage(msg);
		}
   	}

   	private Handler m_doneHandler = new Handler()
   	{
        @Override
        public void handleMessage(Message msg)
        {
           	if(m_progress != null)
        	{
           		if(m_progress.isShowing())
           		{
           			m_progress.dismiss();
           		}
        		m_progress = null;
        	}
        }
   	};
   	*/

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
