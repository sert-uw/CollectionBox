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
import android.net.Uri;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;

public class ImageEdit implements OnClickListener {
	private MainActivity activity;

	//コンストラクタ
	public ImageEdit(MainActivity activity, String path) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;

			//画像の読み込み
			Bitmap bmp = BitmapFactory.decodeStream(
					activity.getContentResolver().openInputStream(Uri.parse(path)),
					null, options);

			//端末の解像度を取得する
			DisplayMetrics metrics = new DisplayMetrics();
			activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

			int display_x = metrics.widthPixels;
			int display_y = metrics.heightPixels;

			int scaleW = options.outWidth / display_x + 1;
			int scaleH = options.outHeight / display_y + 1;
			int scale = Math.max(scaleW, scaleH);

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

			//bgim = new BitmapDrawable(reSize);

		} catch (IOException e) {
			/*bgim = new BitmapDrawable(BitmapFactory.decodeResource(
					activity.getResources(), R.drawable.back));
			System.out.println(e);
			readData[2] = "default";
			writeData();*/
		}
	}

	//クリック処理
	@Override
	public void onClick(View v) {

	}

}
