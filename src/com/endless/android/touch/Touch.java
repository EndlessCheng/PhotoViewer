package com.endless.android.touch;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class Touch extends Activity implements OnTouchListener {

	//public static final String TAG = "Touch";

	static final float MIN_SCALE = 0.1f; // 最小缩放比例
	static final float MAX_SCALE = 4.0f; // 最大缩放比例

	static final int NONE = 0; // 初始状态
	static final int DRAG = 1;
	static final int ZOOM = 2;

	static final float MIN_FINGER_DISTANCE = 10.0f;

	int gestureMode;

	PointF prevPoint;
	PointF nowPoint;
	PointF midPoint;
	float prevFingerDistance;

	Matrix matrix;
	ImageView mImageView;
	Bitmap mBitmap;
	DisplayMetrics mDisplayMetrics;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gestureMode = NONE;
		prevPoint = new PointF();
		nowPoint = new PointF();
		midPoint = new PointF();
		matrix = new Matrix();

		setContentView(R.layout.scale);
		mImageView = (ImageView) findViewById(R.id.imag);
		mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.lijiang);
		mImageView.setImageBitmap(mBitmap);
		mImageView.setOnTouchListener(this);

		mDisplayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics); // 获取屏幕信息（分辨率等）

		initImageMatrix();
		mImageView.setImageMatrix(matrix);
	}

	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN: // 主点按下
			gestureMode = DRAG;
			prevPoint.set(event.getX(), event.getY());
			break;
		case MotionEvent.ACTION_POINTER_DOWN: // 副点按下
			prevFingerDistance = getFingerDistance(event);
			if (getFingerDistance(event) > MIN_FINGER_DISTANCE) {
				gestureMode = ZOOM;
				setMidpoint(midPoint, event);
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (gestureMode == DRAG) {
				nowPoint.set(event.getX(), event.getY());
				matrix.postTranslate(nowPoint.x - prevPoint.x, nowPoint.y
						- prevPoint.y);
				prevPoint.set(nowPoint);
			} else if (gestureMode == ZOOM) {
				float currentFingerDistance = getFingerDistance(event);
				if (currentFingerDistance > MIN_FINGER_DISTANCE) {
					float zoomScale = currentFingerDistance / prevFingerDistance;
					matrix.postScale(zoomScale, zoomScale, midPoint.x, midPoint.y);
					prevFingerDistance = currentFingerDistance;
				}
				checkImageViewSize();
			}
			mImageView.setImageMatrix(matrix);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			gestureMode = NONE;
			break;
		}
		return true;
	}

	/**
	 * 限制图片缩放比例：不能太小，也不能太大
	 */
	private void checkImageViewSize() {
		float p[] = new float[9];
		matrix.getValues(p);
		if (gestureMode == ZOOM) {
			if (p[0] < MIN_SCALE) {
				float tScale = MIN_SCALE / p[0];
				matrix.postScale(tScale, tScale, midPoint.x, midPoint.y);
			} else if (p[0] > MAX_SCALE) {
				float tScale = MAX_SCALE / p[0];
				matrix.postScale(tScale, tScale, midPoint.x, midPoint.y);
			}
		}
	}

	private void initImageMatrix() {
		float initImageScale = Math.min(1.0f, Math.min(
				(float) mDisplayMetrics.widthPixels
						/ (float) mBitmap.getWidth(),
				(float) mDisplayMetrics.heightPixels
						/ (float) mBitmap.getHeight()));
		if (initImageScale < 1.0f) { // 图片比屏幕大，需要缩小
			matrix.postScale(initImageScale, initImageScale);
		}

		RectF rect = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
		matrix.mapRect(rect); // 按 initImageScale 缩小矩形，或者不变

		float dx = (mDisplayMetrics.widthPixels - rect.width()) / 2.0f;
		float dy = (mDisplayMetrics.heightPixels - rect.height()) / 2.0f;
		matrix.postTranslate(dx, dy);
	}

	private float getFingerDistance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	private void setMidpoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2.0f, y / 2.0f);
	}
}