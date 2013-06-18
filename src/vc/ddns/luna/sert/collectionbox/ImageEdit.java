package vc.ddns.luna.sert.collectionbox;

import java.io.IOException;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;

public class ImageEdit extends View {
	private MainActivity activity;	//MainActivityオブジェクト
	private Bitmap originBitmap;	//読み込んだ画像

	private int view_x;			//Viewの大きさ
	private int view_y;			//Viewの大きさ

	private float old_touch_x;		//タッチしたx座標
	private float old_touch_y;		//タッチしたy座標

	private float location_x;		//画像を描画するx座標
	private float location_y;		//画像を描画するy座標

	private String imPath;			//画像のパス
	private boolean isLoaded;		//ロード完了フラグ

	//コンストラクタ
	public ImageEdit(MainActivity activity, String path){
		super(activity);
		this.activity = activity;
		imPath = path;
	}

	//このViewがフォーカスされてから画像を読み込む
	@Override
	public void onWindowFocusChanged(boolean hasFocus){
		super.onWindowFocusChanged(hasFocus);
		loadImage();
	}

	//画像のロード
	public void loadImage() {
		try {
			//画像の読み込みでout of memoryが生じないよう対策をする
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;

			//画像の情報を読み込む
			Bitmap bmp = BitmapFactory.decodeStream(
					activity.getContentResolver().openInputStream(Uri.parse(imPath)),
					null, options);

			//Viewのピクセル数
			view_x = this.getWidth();
			view_y = this.getHeight();

			//画像の大きさと端末の大きさから高さの比率を求める
			int scale = options.outHeight / view_y + 1;

			//画像の比率を求めたので、今度は画像すべてを読み込む
			options.inJustDecodeBounds = false;
			options.inSampleSize = scale;
			bmp = BitmapFactory.decodeStream(
					activity.getContentResolver().openInputStream(Uri.parse(imPath)),
					null, options);

			Cursor query = MediaStore.Images.Media.query(
					activity.getContentResolver(), Uri.parse(imPath),
					new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
					null, null);
			query.moveToFirst();
			//System.out.println(query.getInt(0));

			Matrix mat = new Matrix();
			mat.postRotate(query.getInt(0));

			if (bmp == null)
				throw new IOException();

			//読み込んだ画像をBitmap化する
			originBitmap = Bitmap.createBitmap(bmp, 0, 0,
					bmp.getWidth(), bmp.getHeight(), mat, true);

			location_x = -(originBitmap.getWidth()/2 - view_x/2);
			location_y = (originBitmap.getHeight()/2 - view_y/2);

			//ロード完了フラグをtrueへ
			isLoaded = true;

			//再描画
			invalidate();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//描画処理
	@Override
	protected void onDraw(Canvas canvas){
		canvas.drawColor(Color.BLACK);
		if(isLoaded)
			canvas.drawBitmap(originBitmap, location_x, location_y, null);

	}

	//モーションイベント
	@Override
	public boolean onTouchEvent(MotionEvent event){
		float x=0, y=0;
		switch(event.getAction() & MotionEvent.ACTION_MASK){

		//画像を移動させる
		case MotionEvent.ACTION_MOVE:
			x = event.getX(); y = event.getY();

			//前回からの変位量で位置を変更し、old_touchを更新する
			location_x += x - old_touch_x;
			location_y += y - old_touch_y;
			old_touch_x = x;
			old_touch_y = y;

			//画像が画面の外に出ないようにする
			if(location_x <= - (originBitmap.getWidth() - view_x))
				location_x = - (originBitmap.getWidth() - view_x);
			else if(location_x >= 0)
				location_x = 0;

			if(location_y <= - (originBitmap.getHeight() - view_y))
				location_y = - (originBitmap.getHeight() - view_y);
			else if(location_y >= 0)
				location_y = 0;

			//再描画
			invalidate();
			break;

		case MotionEvent.ACTION_DOWN:
			old_touch_x = event.getX(); old_touch_y = event.getY(); break;

		}

		return true;
	}
}
