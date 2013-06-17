package vc.ddns.luna.sert.collectionbox;

import java.io.IOException;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;

public class ImageEdit implements OnClickListener {
	private MainActivity activity;
	private BitmapDrawable originBitmap;

	//コンストラクタ
	public ImageEdit(MainActivity activity, String path) {
		try {
			//画像の読み込みでout of memoryが生じないよう対策をする
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;

			//画像の情報を読み込む
			Bitmap bmp = BitmapFactory.decodeStream(
					activity.getContentResolver().openInputStream(Uri.parse(path)),
					null, options);

			//端末の解像度を取得する
			DisplayMetrics metrics = new DisplayMetrics();
			activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

			//端末のピクセル数
			int display_x = metrics.widthPixels;
			int display_y = metrics.heightPixels;

			//画像の大きさと端末の大きさから比率を求める
			int scaleW = options.outWidth / display_x + 1;
			int scaleH = options.outHeight / display_y + 1;
			int scale = Math.max(scaleW, scaleH);

			//画像の比率を求めたので、今度は画像すべてを読み込む
			options.inJustDecodeBounds = false;
			options.inSampleSize = scale;
			bmp = BitmapFactory.decodeStream(
					activity.getContentResolver().openInputStream(Uri.parse(path)),
					null, options);

			Cursor query = MediaStore.Images.Media.query(
					activity.getContentResolver(), Uri.parse(path),
					new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
					null, null);
			query.moveToFirst();
			System.out.println(query.getInt(0));

			Matrix mat = new Matrix();
			mat.postRotate(query.getInt(0));

			if (bmp == null)
				throw new IOException();

			//読み込んだ画像をBitmap化する
			bmp = Bitmap.createBitmap(bmp, 0, 0,
					bmp.getWidth(), bmp.getHeight(), mat, true);

			//背景画像の大きさを決定する
			Bitmap reSize = Bitmap.createBitmap(
					display_x, display_y,
					Config.ARGB_8888);

			Canvas canvas = new Canvas(reSize);
			canvas.drawColor(Color.WHITE);

			double aspectX = display_x / (double) bmp.getWidth();
			double aspectY = display_y / (double) bmp.getHeight();

			int w = 0, h = 0;
			int dx = 0, dy = 0;

			if (aspectX >= aspectY) {
				w = reSize.getWidth();
				h = (int) (bmp.getHeight() * aspectX);
				dy = (h - reSize.getHeight()) / 2;
			} else {
				w = (int) (bmp.getWidth() * aspectY);
				h = reSize.getHeight();
				dx = (w - reSize.getWidth()) / 2;
			}

			Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
			Rect dst = new Rect(-dx, -dy, w - dx, h - dy);

			canvas.drawBitmap(bmp, src, dst, null);

			originBitmap = new BitmapDrawable(reSize);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//読み込んだ画像を返す
	public BitmapDrawable getBitmap(String type){
		if(type.equals("origin")){
			return originBitmap;
		}
		return null;
	}

	//クリック処理
	@Override
	public void onClick(View v) {

	}

}
