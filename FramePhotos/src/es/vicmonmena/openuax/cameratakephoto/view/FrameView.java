package es.vicmonmena.openuax.cameratakephoto.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceView;
import es.vicmonmena.openuax.cameratakephoto.R;

/**
 * Clase que representa la capa de imagen con el marco de la foto
 * @author vicmonmena
 *
 */
public class FrameView extends SurfaceView {

	/**
	 *Etiqueta para mensajes de log 
	 */
	private final String TAG = "FrameView";
	
	/**
	 * Pinta la imagen del marco.
	 */
	private Bitmap bitmap;	
	
	/**
	 * Estimo del dibujo.
	 */
	private Paint paint;
	
	/**
	 * Identificador del marco que hay actualmente seleccionado
	 */
	private int currentFrame = R.drawable.graffiti_frame;
	
	public FrameView(Context context) {
		super(context);
		// Preparamos el objeto paint
		paint = new Paint();
		// A false invocamos el método draw -> onDraw
		setWillNotDraw(false);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Log.d(TAG, "onDraw - w:" + getWidth() + "h:" + getHeight());
		if (currentFrame > 0) {
			bitmap = BitmapFactory.decodeResource(getResources(), currentFrame);
			// Escalamos la imagen para que se ajuste a la pantalla del terminal
			bitmap = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), true);
			
			// Pintamos el bitmap
			canvas.drawBitmap(bitmap, new Matrix(), paint);
		} else {
			// Sin marco
			bitmap = null;
		}
	}

	/**
	 * Método que cambia de marco.
	 */
	public void setFrame() {
		
		if (currentFrame == R.drawable.graffiti_frame) {
			currentFrame = R.drawable.star_wars_frame;
		} else if (currentFrame == R.drawable.star_wars_frame) {
			currentFrame = R.drawable.halloween_frame;
		} else if (currentFrame == R.drawable.halloween_frame) {
			currentFrame = R.drawable.christmas_frame;
		} else if (currentFrame == R.drawable.christmas_frame) {
			// Opción sin marco
			currentFrame = 0;
		} else {
			currentFrame = R.drawable.graffiti_frame;
		}
		
		invalidate();
	}
	
	/**
	 * Devuelve el marco actualmente seleccionado ya escalado
	 * @return
	 */
	public Bitmap getCurrentBitmapFrame() {
		return bitmap;
	}
}
