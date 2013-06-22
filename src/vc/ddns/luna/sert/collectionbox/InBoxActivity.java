package vc.ddns.luna.sert.collectionbox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class InBoxActivity extends Activity implements OnClickListener,
	GestureDetector.OnGestureListener{

	private static final int REQUEST_GALLERY = 0;//他アプリからの返り値取得用
	private TextView		pathView;					//dialogのTextView

	private RelativeLayout	inBoxRela;//inBoxLayoutのRelativeLayout

	private String 			boxName;//ボックス名

	private MySQLite		sql;	//SQLiteオブジェクト
	private SQLiteDatabase	db;		//データベースオブジェクト

	private GestureDetector	gestureDetector; //GestureDetectorオブジェクト

	private ViewFlipper 	viewFlipper;//アニメーション用Viewオブジェクト
	private int				sheetNum;//シートの数

	private LayoutInflater	inflater;//LayoutをViewとして取得する

	//各種アニメーションオブジェクト
	private Animation 		inFromRightAnimation;
	private Animation 		inFromLeftAnimation;
	private Animation 		outToRightAnimation;
	private Animation 		outToLeftAnimation;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inbox_main_layout);
		viewFlipper = (ViewFlipper)findViewById(R.id.inBox_viewFlipper);

		gestureDetector = new GestureDetector(this, this);
		inflater = LayoutInflater.from(this);

		sheetNum = 0;

		//MainActivityからデータを引き継ぐ
		Bundle extras = getIntent().getExtras();
		if(extras != null){
			 boxName = extras.getString("boxName");
		}

		init();
		readSheet();
		setAnimations();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	//初期化処理
	private void init(){

		//レイアウトから読み込む
		TextView	boxNameView	= (TextView)findViewById(R.id.inBox_boxName);
		ImageView	boxImView	= (ImageView)findViewById(R.id.inBox_boxIm);
		((Button)findViewById(R.id.inBox_createButton)).setOnClickListener(this);
		inBoxRela = (RelativeLayout)findViewById(R.id.inBox_rela);

		//SQLiteオブジェクト及びデータベースオブジェクトの生成
		sql = new MySQLite(this);
		db = sql.getWritableDatabase();

		try{
			String[] data = sql.searchBoxByBoxName(db, boxName);
			StringTokenizer st = new StringTokenizer(data[0], ",");

			//読み込んだデータを適応する
			st.nextToken();
			boxNameView.setText(st.nextToken());

			String imPath = st.nextToken();
			if(imPath.equals("  "))
				boxImView.setImageResource(R.drawable.no_image);
			else
				boxImView.setImageURI(Uri.parse(imPath));

		}catch(Exception e){
			e.printStackTrace();
		}
	}

	//データベースからシートを読み込む
	private void readSheet(){
		try{
			String[] data = sql.searchSheetByBoxName(db, boxName);
			if(data.length == 0)
				return;

			View[] shLayView = new View[data.length];
			sheetNum = data.length;

			for(int i=0; i<shLayView.length; i++){
				shLayView[i] = inflater.inflate(R.layout.inbox_sheet_layout, null);

				TextView shName = (TextView)shLayView[i].findViewById(R.id.inBox_sheetName);
				TextView shCom  = (TextView)shLayView[i].findViewById(R.id.inBox_sheetComment);
				Button shButton = (Button)shLayView[i].findViewById(R.id.inBox_sheetOpenButton);

				StringTokenizer st = new StringTokenizer(data[i], ",");
				//読み込んだデータを適応する
				st.nextToken();
				shName.setText(st.nextToken());

				String comment = st.nextToken();
				if(comment.equals("  "))
					comment = "";
				shCom.setText(comment);

				String bgImPath = st.nextToken();
				System.out.println(bgImPath);
				if(!bgImPath.equals("  ")){
					Drawable bgIm = new BitmapDrawable(BitmapFactory.decodeFile(bgImPath));
					shLayView[i].setBackgroundDrawable(bgIm);
				}
				shButton.setTag(shName.getText().toString());
				shButton.setOnClickListener(this);

				//ViewFlipperへ追加する
				viewFlipper.addView(shLayView[i]);
			}
		}catch(Exception e){
			System.out.println(e);
			e.printStackTrace();
		}
	}

	//フリックによるアニメーションをセットする
	private void setAnimations(){
		inFromRightAnimation =
				AnimationUtils.loadAnimation(this, R.anim.right_in);
		inFromLeftAnimation =
				AnimationUtils.loadAnimation(this, R.anim.left_in);
		outToRightAnimation =
				AnimationUtils.loadAnimation(this, R.anim.right_out);
		outToLeftAnimation =
				AnimationUtils.loadAnimation(this, R.anim.left_out);
	}

	@Override
	public void onClick(View v) {
		String tag = v.getTag().toString();

		if(tag.equals("inBox_create")){
			//LayoutをViewとして読み込む
			View layoutView = inflater.inflate(R.layout.create_sheet_dialog, null);

			//dialogのViewを取得
			final EditText titleView = (EditText)layoutView.findViewById(R.id.create_sheet_title);
			final EditText commentView = (EditText)layoutView.findViewById(R.id.create_sheet_comment);
			pathView = (TextView)layoutView.findViewById(R.id.create_sheet_showPath);

			//dialogのボタンにイベントリスナーを適応
			((Button)layoutView.findViewById(R.id.create_sheet_button))
					.setOnClickListener(
							new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent intent = new Intent();
									intent.setType("image/*");
									intent.setAction(Intent.ACTION_GET_CONTENT);
									startActivityForResult(intent, REQUEST_GALLERY);
								}
							});

			createDialog("New Sheet", layoutView, "create", "cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//Positiveボタンが押された場合
							if (which == DialogInterface.BUTTON_POSITIVE) {

								//EditTextの文字列取得
								String title = titleView.getText().toString();
								String comment = commentView.getText().toString();
								String bgPath = pathView.getText().toString();
								if(comment.equals(""))
									comment = "  ";
								if(bgPath.equals(""))
									bgPath = "  ";
								else
									bgPath = reSizeImage(bgPath);

								if(!title.equals("")){
									//同じタイトルがない場合
									if(sql.searchDataBySheetNameAndType(db, title, null).length == 0){
										addDataToDB(title, comment, bgPath);

									}else
										toast(InBoxActivity.this, "すでに同じ名前が登録されています");
								}
							}
					}
			});
		}else {
			changeActivity(tag);
		}
	}

	//他アプリからの結果参照
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode == RESULT_OK){
			if(requestCode == REQUEST_GALLERY){
				if(pathView != null)
					pathView.setText(data.getDataString());
			}
		}
	}

	//データベースへの追加
	public void addDataToDB(String sheetTitle, String sheetCom, String bgImPath){
		String[] data = new String[]{boxName, sheetTitle, sheetCom, bgImPath};
		sql.createNewData(db, "boxSheet", data);
		readSheet();
	}

	//ダイアログの表示
	/*title:Dialogのタイトル        view:Dialogに埋め込むView
	  ptext:Positiveボタンの文字列  ntext:Negativeボタンの文字列
	  listener:ボタンのイベントリスナー
	*/
	private void createDialog(String title, View view,
			String ptext, String ntext,
			DialogInterface.OnClickListener listener) {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		ad.setTitle(title);
		ad.setView(view);
		ad.setPositiveButton(ptext, listener);
		ad.setNegativeButton(ntext, listener);
		ad.show();
	}

    //トーストの表示　
    private static void toast(Context context,String text) {
        Toast.makeText(context,text,Toast.LENGTH_SHORT).show();
    }

    //Activity変更
    public void changeActivity(String sheetName){
    	//インテントの生成
    	Intent intent = new Intent(this,
    			vc.ddns.luna.sert.collectionbox.SheetActivity.class);
    	try{
    		//インテントへパラメータ追加
    		intent.putExtra("sheetName", sheetName);

    		//Activityの呼び出し
    		this.startActivity(intent);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }

	//////////////////////////////
	////以下タッチイベント処理////
	//////////////////////////////

	@Override
	public boolean onTouchEvent(MotionEvent e){
		//GestureDetectorでジェスチャー処理を行う
		gestureDetector.onTouchEvent(e);
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

		if(sheetNum == 0)
			return false;

		//絶対値の取得
		float dx = Math.abs(velocityX);
		float dy = Math.abs(velocityY);

		//指の移動方向及び距離の判定
		if(dx > dy && dx > 300){
			//指の左右方向の判定
			if(e1.getX() < e2.getX()){
				viewFlipper.setInAnimation(inFromLeftAnimation);
				viewFlipper.setOutAnimation(outToRightAnimation);
				viewFlipper.showPrevious();

			}else{
				viewFlipper.setInAnimation(inFromRightAnimation);
				viewFlipper.setOutAnimation(outToLeftAnimation);
				viewFlipper.showNext();
			}
			return true;
		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	//画像の大きさを画面に合わせて保存
	private String reSizeImage(String path){
		InBoxActivity activity = InBoxActivity.this;
		try{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;

			Bitmap bmp = BitmapFactory.decodeStream(
					activity.getContentResolver().openInputStream(Uri.parse(path)),
					null, options);

			//端末の解像度を取得する
			DisplayMetrics metrics = new DisplayMetrics();
			activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

			//Viewのピクセル数
			int view_x = inBoxRela.getWidth();
			int view_y = inBoxRela.getHeight();

			int scaleW = options.outWidth / view_x;
			int scaleH = options.outHeight / view_y;
			int scale = Math.max(scaleW, scaleH);

			options.inJustDecodeBounds = false;
			options.inSampleSize = scale;

			bmp = BitmapFactory.decodeStream(
					activity.getContentResolver().openInputStream(Uri.parse(path)),
					null, options);

			Cursor query = MediaStore.Images.Media.query(
					activity.getContentResolver(), Uri.parse(path),
					new String[]{MediaStore.Images.ImageColumns.ORIENTATION},
					null, null);
			query.moveToFirst();

			Matrix mat = new Matrix();
			mat.postRotate(query.getInt(0));

			if(bmp == null)
				throw new IOException();

			bmp = Bitmap.createBitmap(bmp, 0, 0,
					bmp.getWidth(), bmp.getHeight(), mat, true);

			//背景画像の大きさを決定する
			Bitmap reSize = Bitmap.createBitmap(
					view_x, view_y,
					Config.ARGB_8888);

			//画像を編集する
			Canvas canvas = new Canvas(reSize);
			canvas.drawColor(Color.BLACK);

			//viewと画像の比率を出す
			double aspectX = view_x / (double)bmp.getWidth();

			//画像は横幅を合わせて高さを調節する
			int w = view_x, h=0;
			int dy=0;

			//高さと横幅の比率を維持する
			h = (int)(bmp.getHeight() * aspectX);
			dy = (h - reSize.getHeight())/2;

			Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
			Rect dst = new Rect(0, -dy, w, h-dy);

			canvas.drawBitmap(bmp, src, dst, null);

			//Bitmapの保存先をしていする
			final String SAVE_DIR = "/CollectionBox/";
			File file = new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);

			//存在しなければ生成する
			try{
				if(!file.exists()){
					file.mkdir();
				}
			}catch(SecurityException e){
				e.printStackTrace();
			}

			//ファイル名には現在の時刻を使用してファイル名の衝突をさける
			Date mDate = new Date();
			SimpleDateFormat fileNameDate = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String fileName = fileNameDate.format(mDate) + ".jpg";
			String attachName = file.getAbsolutePath() + "/" + fileName;

			//jpgファイルとして保存する
			try {
				FileOutputStream out = new FileOutputStream(attachName);
				reSize.compress(CompressFormat.JPEG, 100, out);
				out.flush();
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}

			// ファイルパスを登録してギャラリーを更新する
			ContentValues values = new ContentValues();
			ContentResolver contentResolver = activity.getContentResolver();
			values.put(Images.Media.MIME_TYPE, "image/jpeg");
			values.put(Images.Media.TITLE, fileName);
			values.put("_data", attachName);
			contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

			return attachName;

		}catch(IOException e){
			e.printStackTrace();
			return "  ";
		}
	}
}
