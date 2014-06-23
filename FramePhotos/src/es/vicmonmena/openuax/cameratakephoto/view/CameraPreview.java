package es.vicmonmena.openuax.cameratakephoto.view;

import es.vicmonmena.openuax.cameratakephoto.R;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * Clase que representa la capa de imagen donde se toam la foto
 * @author vicmonmena
 *
 */
public class CameraPreview extends FrameLayout implements SurfaceHolder.Callback{

	/**
	 * Etiqueta para los mensajes de LOG.
	 */
	private final String TAG = "CameraPreview";
	/**
	 * Vista que muestra la camara
	 */
	private SurfaceView surfaceView;
	/**
	 * Instancia de la cámara
	 */
	private Camera camera;
	
	// Cámaras que se pueden seleccionar (trasera y  delatenra)
	private final int READ_CAMERA = 0;
	private final int FRONT_CAMERA = 1;
	
	/**
	 * Cámara que está activa en este momento
	 */
	private int activeCameraId = READ_CAMERA;
	
	/**
	 * Controla la pausa de la cámara
	 */
	private boolean mPreviewRunning = false;
	
	/**
	 * Objeto para el control y acceso a la vista
	 */
	SurfaceHolder holder;
	
	
	/**
	 * Constructor por defecto
	 * @param context
	 * @param attrs
	 */
	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Obtengo la cámara
		createCamera();
		
		// Si no hay cámara no hago nada más
		if (camera == null) {
			return;
		}
		
		// Creo la vista y la añado al layout
		surfaceView = new SurfaceView(context);
		addView(surfaceView);
		
		holder = surfaceView.getHolder();
		// AÑadimos escuchador de la view
		holder.addCallback(this);
		
		// Llamadas necesarias para los SD antiguos (están deprecadas)
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		holder.setKeepScreenOn(true);
	}

	/**
	 * Obtener la instancia de la cámara
	 */
	public void createCamera() {
		
		if (camera != null) {
			Log.d(TAG, "creating camera");
			camera.stopPreview();
			camera.release();
		}
		
		// Comprobamos si existe cámara
		if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			try {
				// Obtenemos la instancia de la camara
				camera = Camera.open(activeCameraId);
				camera.setPreviewDisplay(holder);
			} catch (Exception e) {
				Toast.makeText(getContext(), getContext().getString(R.string.msg_error_opening_camera), Toast.LENGTH_LONG).show();
				Log.d(TAG, e.getMessage());
				return;
			}
		} else {
			// No hay camara
			Toast.makeText(getContext(), getContext().getString(R.string.msg_no_camera), Toast.LENGTH_LONG).show();
			return;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "surfaceChanged");
		// Si la superficie no exite no hago nada
		if (holder.getSurface() == null) {
			return;
		}
		
		
		// Antes de hacer cambios hay que detener la vista previa de la camara
		if (mPreviewRunning) {
			Log.d(TAG, "mPreviewRunning = true");
			camera.stopPreview(); // Esto hace que la app detenga la cámara
		}
		
		
		// Obtengo el display. Nos permitirá saber si la superfice está rotada
		Display display = ((WindowManager) getContext()
			.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		
		// Obtengo la rotación de la cámara
		CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(0, info);
		int cameraOrientation = info.orientation;
		
		// Obtenemos el tamaño de la vista previa de la cámara
		Size cameraSize = camera.getParameters().getPreviewSize();
		
		/*
		 * Existen dos posible casos, que las vistas sean perpendiculares o que 
		 * no lo sean. Si lo son tendremos que intercambiar el ancho por el alto
		 *  en el tamaño de la vista previa de la cámara y rotarla
		 */
		
		// Son perpendiculares
		if ((display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180) 
				&& (cameraOrientation == Surface.ROTATION_90 || cameraOrientation == Surface.ROTATION_270)) {
			// Como está rotado el alto y el ancho están intercambiados
			cameraSize.width = camera.getParameters().getPreviewSize().height;
			cameraSize.height = camera.getParameters().getPreviewSize().width;
			
			// Roto la cámara para que no sean perpendiculares
			camera.setDisplayOrientation(90);
			
			// Calculo la relación de aspecto de la cámara
			float ratio = (float) cameraSize.width / (float) cameraSize.height;
			
			// Calculo el tamaño del ancho en función del alto manteniendo el ratio
			FrameLayout.LayoutParams params = new FrameLayout
				.LayoutParams((int) (height * ratio), height,Gravity.CENTER);
			surfaceView.setLayoutParams(params);
			
			// Reinicio vista previa
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				mPreviewRunning = true;
			} catch (Exception e) {
				Toast.makeText(getContext(), getContext().getString(R.string.msg_error_showing_preview), Toast.LENGTH_LONG).show();
				Log.d("CameraPrevie", "Error showing camera preview" + e.getMessage());
			}
		}
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// Cuando se crea la superficie muestro la vista previa de la cámara
		try {
			camera.setPreviewDisplay(holder);
			camera.startPreview();
		} catch (Exception e) {
			Toast.makeText(getContext(), getContext().getString(R.string.msg_error_showing_preview), Toast.LENGTH_LONG).show();
			Log.e(TAG, e.getMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
		// Para vista previa
		camera.stopPreview();
		mPreviewRunning = false;
		// Liberar cámara
		camera.release();
		camera = null;
		
		holder.removeCallback(this);
	}
	
	// Getter de la camara para obtenerla desde la clase MainActivity
	public Camera getCamera() {
		return camera;
	}
	
	/**
	 * Cmabia a la cámara trasera o frontal
	 */
	public void setCamera() {
		// ALGO NO FUNCIONA EN ESTA OPCIÓN --> REVISAR PARA EL FUTURO
		if (activeCameraId == READ_CAMERA) {
			activeCameraId = FRONT_CAMERA;
		} else {
			activeCameraId = READ_CAMERA;
		}
		
		
		// Comprobamos si existe cámara
		if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			
			try {
				// Obtenemos la instancia de la camara
				camera.stopPreview();
				camera.release();
				camera = null;
				camera = Camera.open(activeCameraId);
				camera.setPreviewDisplay(holder);
				camera.startPreview();
			} catch (Exception e) {
				Toast.makeText(getContext(), getContext().getString(R.string.msg_error_opening_camera), Toast.LENGTH_LONG).show();
				Log.d(TAG, e.getMessage());
				return;
			}
		} else {
			// No hay camara
			Toast.makeText(getContext(), getContext().getString(R.string.msg_no_camera), Toast.LENGTH_LONG).show();
			return;
		}
	}
}
