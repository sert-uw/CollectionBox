package vc.ddns.luna.sert.collectionbox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

public class ImageEdit extends View implements OnClickListener{
	private MainActivity	activity;		//MainActivityオブジェクト
	private Bitmap			originBitmap;	//読み込んだ画像

	private int 			view_x;			//Viewの大きさ
	private int 			view_y;			//Viewの大きさ

	private float			old_touch_x;	//タッチしたx座標
	private float			old_touch_y;	//タッチしたy座標

	private float			location_x;		//画像を描画するx座標
	private float			location_y;		//画像を描画するy座標

	private String			boxName;		//ボックス名
	private String			imPath;			//画像のパス
	private boolean			isLoaded;		//ロード完了フラグ

	private ViewGroup		top;			//このViewを含むレイアウトを適応しているViewGroup
	private View			inner;			//このViewを含むレイアウト

	private Bitmap			overView;		//トリミング範囲を明確化する

	//コンストラクタ
	public ImageEdit(MainActivity activity, String boxName, String path){
		super(activity);
		this.activity = activity;
		this.boxName = boxName;
		imPath = path;
	}

	//このViewをレイアウトへ適応する
	public void setLayout(ViewGroup top, View inner){
		//ViewからRelativeLayoutを取得
		RelativeLayout rela = (RelativeLayout)inner.findViewById(R.id.image_edit_rela);

		//ボタンにクリックイベントをセット
		((Button)inner.findViewById(R.id.image_edit_tri)).setOnClickListener(this);
		((Button)inner.findViewById(R.id.image_edit_cnc)).setOnClickListener(this);

		this.top = top;
		this.inner = inner;

		rela.addView(this);
		top.addView(inner);
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

			//Bitmapの生成
			overView = Bitmap.createBitmap(view_x, view_y, Config.ARGB_8888);

			//トリミングする範囲以外を暗くするOverViewを描く
			Canvas canvas = new Canvas(overView);
			Paint paint = new Paint();
			paint.setColor(Color.argb(126, 0, 0, 0));
			canvas.drawRect(0, 0, view_x, view_y/2 - view_x/2, paint);
			canvas.drawRect(0, view_y/2 + view_x/2, view_x, view_y, paint);

			//青い枠を描く
			paint.setColor(Color.BLUE);
			paint.setStyle(Style.STROKE);
			canvas.drawRect(0, view_y/2 - view_x/2 - 1, view_x - 1, view_y/2 + view_x/2 + 1, paint);

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
		canvas.drawColor(Color.DKGRAY);
		if(isLoaded){
			canvas.drawBitmap(originBitmap, location_x, location_y, null);
			canvas.drawBitmap(overView, 0, 0, null);
		}
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

			if(location_y <= - (originBitmap.getHeight() - view_y/2 - view_x/2))
				location_y = - (originBitmap.getHeight() - view_y/2 - view_x/2);

			else if(location_y >= view_y/2 - view_x/2)
				location_y = view_y/2 - view_x/2;

			//再描画
			invalidate();
			break;

		case MotionEvent.ACTION_DOWN:
			old_touch_x = event.getX(); old_touch_y = event.getY(); break;

		}

		return true;
	}

	//ボタンタッチイベント
	@Override
	public void onClick(View v) {
		String tag = v.getTag().toString();

		if(tag.equals("trimming")){
			String newImPath = saveImage();
			activity.addDataToDB(boxName, newImPath);
			top.removeView(inner);

		}else if(tag.equals("cancel")){

		}
	}

	//画像の保存
	private String saveImage(){
		int lx = (int)-location_x;
		int ly = (int)-location_y;
		Rect dst = new Rect(0, 0, 400, 400);
		Rect src = new Rect(lx, ly + (view_y - view_x)/2,
				lx + view_x, ly + (view_y - view_x)/2 + view_x);
		Bitmap bitmap = Bitmap.createBitmap(400, 400, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(originBitmap, src, dst, null);

		final String SAVE_DIR = "/MyPhoto/";
		File file = new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);
		try{
			if(!file.exists()){
				file.mkdir();
			}
		}catch(SecurityException e){
			e.printStackTrace();
		}

		Date mDate = new Date();
		SimpleDateFormat fileNameDate = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String fileName = fileNameDate.format(mDate) + ".jpg";
		String attachName = file.getAbsolutePath() + "/" + fileName;

		try {
			FileOutputStream out = new FileOutputStream(attachName);
			bitmap.compress(CompressFormat.JPEG, 100, out);
			out.flush();
			out.close();
		} catch(IOException e) {
			e.printStackTrace();
		}

		// save index
		ContentValues values = new ContentValues();
		ContentResolver contentResolver = activity.getContentResolver();
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.TITLE, fileName);
		values.put("_data", attachName);
		contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		return attachName;
	}
}
