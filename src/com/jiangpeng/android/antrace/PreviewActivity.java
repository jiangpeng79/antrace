package com.jiangpeng.android.antrace;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.jiangpeng.android.antrace.Objects.path;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.MediaColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class PreviewActivity extends Activity {
	private String m_filename = null;
	private ProgressDialog m_progress = null;
	private PreviewImageView m_imageView = null;
	private Bitmap m_bitmap = null;
	private Button m_ok = null;
	private Button m_cancel = null;
	private BitmapFactory.Options m_ops = null;
    public static int CODE_SHARE_IMAGE = 1115;
    public static String FILENAME = "FILENAME";
    private String m_photoFile = "/sdcard/__WatermarkMargin_tmp";
	
    static {
        System.loadLibrary("antrace");
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_image);
        
        m_imageView = (PreviewImageView)findViewById(R.id.previewImageView);
        m_filename = this.getIntent().getStringExtra(PreviewActivity.FILENAME);
        m_photoFile = FileUtils.getRootFolder() + FileUtils.sep + MainActivity.TEMP_FOLDER + FileUtils.sep + MainActivity.PHOTO_FILE_TEMP_;
		FileUtils.checkAndCreateFolder(FileUtils.getRootFolder() + FileUtils.sep + MainActivity.TEMP_FOLDER);
       	m_progress = ProgressDialog.show(this, getResources().getString(R.string.empty), getResources().getString(R.string.loading), true);	
		Thread t = new Thread(new LoadImageThread());
		t.start();
		
        m_ok = (Button)findViewById(R.id.okButton);
        m_cancel = (Button)findViewById(R.id.cancelButton);
        
        OnClickListener okListener = new OkListener();
        m_ok.setOnClickListener(okListener);
        
        OnClickListener cancelListener = new CancelListener();
        m_cancel.setOnClickListener(cancelListener);
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
		    if(PreviewActivity.this.m_bitmap == null)
		    {
		        return;
		    }
		}
	};
	
	class CancelListener implements OnClickListener
	{
	    @Override
	    public void onClick(View v) {
	    	PreviewActivity.this.finish();
	        return;
	    }
	};

	private Handler m_processHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
        {
            if(m_progress != null)
            {
                m_progress.dismiss();
                m_progress = null;
            }
            m_imageView.setImage(null);
            m_bitmap.recycle();
            m_bitmap = null;
            
            Bitmap bmp = (Bitmap)msg.obj;
            m_bitmap = bmp;
            m_imageView.setImage(m_bitmap);
        }
    };
    
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
            
            Bitmap bmp = (Bitmap)msg.obj;
        	if(bmp == null)
        	{
                Toast toast = Toast.makeText(PreviewActivity.this, R.string.check_your_files, Toast.LENGTH_SHORT);
                toast.show();
                PreviewActivity.this.finish();
            	return;
        	}
            if (!bmp.isRecycled())
            {
                m_bitmap = bmp;
                m_imageView.setImage(bmp);
            }
            else
            {
                Toast toast = Toast.makeText(PreviewActivity.this, R.string.check_your_files, Toast.LENGTH_SHORT);
                toast.show();
                PreviewActivity.this.finish();
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
			m_ops.inSampleSize = ImageUtils.computeSampleSize(m_ops, -1, 2 * 2 * 1024 * 1024, false);
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
    		m.reset();
    		Bitmap bnw = Bitmap.createBitmap(bm);
    		Utils.threshold(bm, bnw);
//    		path p = Utils.traceImage(bnw);
            Message msg = m_handler.obtainMessage(0, bnw);
            m_handler.sendMessage(msg);
		}
	}

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
	}

}
