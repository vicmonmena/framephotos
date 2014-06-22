package es.vicmonmena.openuax.cameratakephoto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import es.vicmonmena.openuax.cameratakephoto.view.CameraPreview;
import es.vicmonmena.openuax.cameratakephoto.view.FrameView;
import es.vicmonmena.openuax.cameratakephoto.R;

/**
 * 
 * @author vicmonmena
 *
 */
public class MainActivity extends Activity implements PictureCallback {
	
	/**
	 * Etiqueta para los mensajes de log.
	 */
	private final String TAG = "MainActivity";
	
	/**
	 *  Carpeta donde se guardan los dibujos.
	 */
	private final String FOLDER_NAME = "framephotos";
	
	/**
	 * Vista que contiene la cámara.
	 */
	private CameraPreview cameraPreview;
	
	/**
	 * Vista que muestra un marco.
	 */
	private FrameView frame;
	
	/**
	 * Ruta donde se almacena la última foto realizada
	 */
	private String photoPath;
	/**
	 * Imagen en miniatura
	 */
	private ImageView miniatureImgView;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        miniatureImgView = ((ImageView) findViewById(R.id.miniature));
    }

    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	
    	// Añadimos el marco a la cámara
		frame = new FrameView(this);
    	cameraPreview = ((CameraPreview) findViewById(R.id.camera_preview));
    	cameraPreview.addView(frame);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	/*
    	 * Para disponer de la pantalla entera usamos iconos en vez de 
    	 * ActionBar, por lo que no se entrará por este método.
    	 */
    	switch (item.getItemId()) {
			case R.id.action_set_frame:
				frame.setFrame();
				break;
			case R.id.action_info:
				Toast.makeText(this, getString(R.string.action_info_text), Toast.LENGTH_LONG).show();
				break;
			default:
				break;
		}
    	return super.onOptionsItemSelected(item);
    }
    
    /**
     * Toma una foto con la cámara
     * @param view
     */
    public void onClick(View view) {
    	switch (view.getId()) {
		case R.id.takePhotoBtn:
			cameraPreview.getCamera().takePicture(null, null, this);
			break;
		case R.id.setFrameBtn:
			frame.setFrame();
			break;
		case R.id.infoBtn:
			Toast.makeText(this, getString(R.string.action_info_text), Toast.LENGTH_LONG).show();
			break;
		case R.id.miniature:
			if (TextUtils.isEmpty(photoPath)) {
				Toast.makeText(this, getString(R.string.msg_take_photo), Toast.LENGTH_SHORT).show();
			} else {
				sharePhoto(photoPath);
			}
		case R.id.setCameraBtn:
			cameraPreview.setCamera();
			break;
		default:
			break;
		};
    }

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		
		// Salvamos la imagen en segundo plano
		SavePhotoAsyncTask async = new SavePhotoAsyncTask();
		async.execute(data);
		// La cámara se detiene automáticamene al tomar la foto => La iniciamos de nuevo
		camera.startPreview();
	}
	
	/**
	 * Compartir última foto tomada
	 * @throws IOException
	 */
	public void sharePhoto(String photoPath) {
		Log.d(TAG, "Sharing file");
		
		// Recuperamos y lo compartimos
		File file = new File(photoPath);
		
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("image/jpeg");
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.msg_share));
        startActivity(Intent.createChooser(sendIntent, getString(R.string.msg_action_share))
        	.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
	}
	/**
	 * Combina 2 fotos superpuestas
	 * @param bmp1
	 * @param bmp2
	 * @return
	 */
	public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2,new Matrix(), null);
        return bmOverlay;
    }
	
	/**
	 * 
	 * @author vicmonmena
	 *
	 */
	private class SavePhotoAsyncTask extends AsyncTask<byte[], Void, String> {

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			miniatureImgView.setVisibility(View.GONE);
			findViewById(R.id.progressBar1).setVisibility(View.VISIBLE);
		}
		
		@Override
		protected String doInBackground(byte[]... params) {
			
			// Obtenemos las 2 imágenes y las combinamos para formar la foto final
			Bitmap photo = BitmapFactory.decodeByteArray(params[0], 0, params[0].length, null);
			Bitmap photoFinish = null;
			
			// COmprobamos si hay marco establecido
			if (frame.getCurrentBitmapFrame() != null) {
				Bitmap photoFrame = Bitmap.createScaledBitmap(
					frame.getCurrentBitmapFrame(), photo.getWidth(), 
						photo.getHeight(), true);
				photoFinish = MainActivity.overlay(photo, photoFrame);
			} else {
				// Si no hay marco no combinamos las fotos
				photoFinish = photo;
			}
			// Comprobamos si la carpeta de fotos ya existe
			File folder = new File(Environment.getExternalStorageDirectory() 
					+ File.separator + FOLDER_NAME);
			if (!folder.exists()) {
				folder.mkdir();
			}
				
			// Obtengo el archivo donde se guardará la foto
			String photoName = "photoframe_" + Calendar.getInstance().getTimeInMillis() + ".jpg";
			String photoPath = Environment.getExternalStorageDirectory() + 
					File.separator + FOLDER_NAME + File.separator + photoName;
			
			// Guardo al foto
			try {
				OutputStream stream = new FileOutputStream(photoPath);
				photoFinish.compress(CompressFormat.JPEG, 80, stream);
				stream.close();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}

			
			return photoPath;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				// Muestro la imagen en miniatura
				photoPath = result;
				Bitmap miniature = BitmapFactory.decodeFile(result);
				miniatureImgView.setImageBitmap(miniature);
				miniatureImgView.setClickable(true);
				findViewById(R.id.progressBar1).setVisibility(View.GONE);
				miniatureImgView.setVisibility(View.VISIBLE);
				Toast.makeText(getApplicationContext(), getString(R.string.msg_photo_saved) + " " + photoPath, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(), getString(R.string.msg_photo_not_saved), Toast.LENGTH_LONG).show();
			}
			super.onPostExecute(result);
		}
	}
}
