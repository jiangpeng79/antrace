package com.jiangpeng.android.antrace;
import java.io.File;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static int CAMERA_STATUS_CODE = 111;
    private static int EDIT_IMAGE_CODE = 122;
    private static int SELECT_PHOTO = 100;
	private static int REQUEST_PERMISSION = 133;
    public static String PHOTO_FILE_TEMP_ = "__antrace.jpg";

	Button m_takePicture = null;
	Button m_selectPicture = null;
	Button m_about = null;
    protected String m_photoFile = "";
    AdView m_adView = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		m_takePicture = (Button)findViewById(R.id.take_picture);
		m_selectPicture = (Button)findViewById(R.id.select_picture);
		m_about = (Button)findViewById(R.id.about);

        OnClickListener takeListener = new TakePictureListener();
        m_takePicture.setOnClickListener(takeListener);

        OnClickListener selectListener = new SelectPictureListener();
        m_selectPicture.setOnClickListener(selectListener);

        OnClickListener aboutListener = new AboutListener();
        m_about.setOnClickListener(aboutListener);

		m_photoFile = FileUtils.getCacheDir(this) + FileUtils.sep + PHOTO_FILE_TEMP_;
		FileUtils.checkAndCreateFolder(FileUtils.getCacheDir(this));

		String svgFile = FileUtils.tempSvgFile(this);
        File file = new File(svgFile);
        file.delete();

        LinearLayout layout = (LinearLayout)findViewById(R.id.adLayout);
        m_adView = new AdView(this);
        m_adView.setAdUnitId(AdmobID.id);
        m_adView.setAdSize(AdSize.BANNER);

        AdRequest request = (new AdRequest.Builder()).build();
        layout.addView(m_adView);
        m_adView.loadAd(request);
		if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
		{
			if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
			{
				showMessageDialog(R.string.permission_warning,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								ActivityCompat.requestPermissions(MainActivity.this,
										new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
										REQUEST_PERMISSION);
							}
						});
				return;
			}
			ActivityCompat.requestPermissions(MainActivity.this,
					new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
					REQUEST_PERMISSION);
			return;
		}
	}
	private void showMessageDialog(int str, DialogInterface.OnClickListener okListener) {
		new AlertDialog.Builder(MainActivity.this)
				.setMessage(str)
				.setPositiveButton("OK", okListener)
				.create()
				.show();
	}
	@Override
	protected void onDestroy() {
		m_adView.destroy();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		m_adView.pause();
		super.onPause();
	}
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == REQUEST_PERMISSION)
		{
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			} else {
				showMessageDialog(R.string.permission_warning_quit,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								MainActivity.this.finish();
							}
						});
				return;
			}
		}
	}
	@Override
	protected void onResume() {
		m_adView.resume();
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    class TakePictureListener implements OnClickListener
    {
        @Override
        public void onClick(View v)
        {
    	    Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
    	    i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(m_photoFile)));

           	try
           	{
           	    startActivityForResult(i, CAMERA_STATUS_CODE);
           	}
           	catch(ActivityNotFoundException err)
           	{
        	    Toast t = Toast.makeText(MainActivity.this, err.getLocalizedMessage(), Toast.LENGTH_SHORT);
          	    t.show();
           	}
        }
    }

    class SelectPictureListener implements OnClickListener
    {
        @Override
        public void onClick(View v)
        {
        	Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        	photoPickerIntent.setType("image/*");
        	startActivityForResult(photoPickerIntent, SELECT_PHOTO);
        }
    }

    class AboutListener implements OnClickListener
    {
        @Override
        public void onClick(View view)
        {
        	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        	View v = getLayoutInflater().inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.about_layout));
        	//TextView tv = (TextView) v.findViewById(R.id.appVersion);
        	String ver = getResources().getString(R.string.app_version, getResources().getString(R.string.app_name),
        			Utils.getCurrentVersionName(MainActivity.this));
        	builder.setTitle(ver);
        	//tv.setText(ver);
        	builder.setView(v);
        	builder.setIcon(R.drawable.ic_launcher);
        	builder.setPositiveButton(android.R.string.ok,
        			new DialogInterface.OnClickListener()
        	{
        		@Override
        		public void onClick(DialogInterface dialog, int whichButton)
        		{
        		}
        	});
        	AlertDialog dialog = builder.create();
        	dialog.show();
        }
    }

    static {
        System.loadLibrary("antrace");
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == CAMERA_STATUS_CODE && resultCode == RESULT_OK)
        {
            launchPreviewActivity(m_photoFile);
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }
        if(requestCode == SELECT_PHOTO && resultCode == RESULT_OK)
        {
            Uri selectedImage = intent.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
			if(cursor == null)
			{
				super.onActivityResult(requestCode, resultCode, intent);
				return;
			}
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();

            launchPreviewActivity(filePath);
        }
        super.onActivityResult(requestCode, resultCode, intent);
	}

	protected void launchPreviewActivity(String filename) {
		Intent i = new Intent();
		i.setClass(MainActivity.this, PreviewActivity.class);
	    i.putExtra(PreviewActivity.FILENAME, filename);
	    startActivityForResult(i, EDIT_IMAGE_CODE);

	}

}
