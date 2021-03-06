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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
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

	private View[]			setFlipperView = new View[2];
	private int				location;//viewFlipperの場所

	private String[]		data;//シートデータ
	private int				viewSheetNumber;//表示しているViewが何番目か

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

		gestureDetector = new GestureDetector(getApplicationContext(), this);
		inflater = LayoutInflater.from(getApplicationContext());

		sheetNum = 0;

		//MainActivityからデータを引き継ぐ
		Bundle extras = getIntent().getExtras();
		if(extras != null){
			 boxName = extras.getString("boxName");
		}

		init();
		readSheet();
		setAnimations();
		setHelp();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		destroyObjects();

		db.close();
		sql.close();
	}

	//メニューの作成
	private final static int MENU_ITEM0 = 0;
	private final static int MENU_ITEM1 = 1;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem item0 = menu.add(0, MENU_ITEM0, 0, "シート編集");
		MenuItem item1 = menu.add(0, MENU_ITEM1, 0, "シート削除");

		return true;
	}

	//メニューのイベント処理
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM0:
			//LayoutをViewとして読み込む
			View layoutView = inflater.inflate(R.layout.create_sheet_dialog, null);

			//dialogのViewを取得
			final EditText titleView = (EditText)layoutView.findViewById(R.id.create_sheet_title);
			final EditText commentView = (EditText)layoutView.findViewById(R.id.create_sheet_comment);
			pathView = (TextView)layoutView.findViewById(R.id.create_sheet_showPath);

			String nowTitle = ((TextView)setFlipperView[location - 1].findViewById(
					R.id.inBox_sheetName)).getText().toString();
			titleView.setText(nowTitle);
			titleView.setTag(nowTitle);

			commentView.setText(((TextView)setFlipperView[location - 1].findViewById(
					R.id.inBox_sheetComment)).getText().toString());

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

			createDialog("Change Sheet Data", layoutView, "change", "cancel",
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
									if(sql.searchDataBySheetNameAndType(db, title, null).length == 0 ||
											title.equals(titleView.getTag().toString())){

										sql.upDateEntry(db, "boxSheet", "sheetName = ?",
												new String[]{titleView.getTag().toString()},
												new String[]{boxName, title, comment, bgPath});

										init();
										readSheet();

									}else
										toast(InBoxActivity.this.getApplicationContext(), "すでに同じ名前が登録されています");
								}
							}
					}
			});
			break;

		case MENU_ITEM1:
			if(viewSheetNumber == 0)
				break;

			final TextView txView = new TextView(getApplicationContext());
			txView.setText(((TextView)setFlipperView[location - 1].findViewById(
					R.id.inBox_sheetName)).getText().toString());
			txView.setTextSize(26);
			createDialog("このシートを削除しますか？", txView, "Delete", "cancel",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(which == DialogInterface.BUTTON_POSITIVE){
								String name = txView.getText().toString();

								//データベースから削除する
								//シートを削除するので個々のデータも削除する
								sql.deleteEntry(db, "boxSheet", "sheetName = ?", new String[]{name});
								sql.deleteEntry(db, "sheetData", "sheetName = ?", new String[]{name});

								init();
								readSheet();
							}
						}
					});
			break;

		default: break;

		}

		return true;
	}

	//初期化処理
	private void init(){
		RelativeLayout rela = (RelativeLayout)viewFlipper.findViewById(R.id.inBox_main_rela);
		viewFlipper.removeAllViews();
		viewFlipper.addView(rela);
		viewSheetNumber = 0;
		location = 0;

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

	//オブジェクトの削除
	private void destroyObjects(){
		for(int i=0; i<setFlipperView.length; i++){
			cleanupView(setFlipperView[i]);
		}

		cleanupView(inBoxRela);
		cleanupView(viewFlipper);
	}

	//View解放
	 public static final void cleanupView(View view) {
		 if(view == null)
			 return;

	      if(view instanceof ImageButton) {
	          ImageButton ib = (ImageButton)view;
	          ib.setImageDrawable(null);
	      } else if(view instanceof ImageView) {
	          ImageView iv = (ImageView)view;
	          iv.setImageDrawable(null);
	      } else if(view instanceof SeekBar) {
	          SeekBar sb = (SeekBar)view;
	          sb.setProgressDrawable(null);
	          sb.setThumb(null);
	      // } else if(view instanceof( xxxx )) {  -- 他にもDrawable を使用するUIコンポーネントがあれば追加
	      }

	      view.setBackgroundDrawable(null);
	      if(view instanceof ViewGroup) {
	          ViewGroup vg = (ViewGroup)view;
	          int size = vg.getChildCount();
	          for(int i = 0; i < size; i++) {
	              cleanupView(vg.getChildAt(i));
	          }
	      }

	      view.setOnClickListener(null);
	  }

	//データベースからシートを読み込む
	private void readSheet(){
		try{
			data = sql.searchSheetByBoxName(db, boxName);
			if(data.length == 0)
				return;

			sheetNum = data.length;

			for(int i=0; i<setFlipperView.length; i++){
				setFlipperView[i] = inflater.inflate(R.layout.inbox_sheet_layout, null);

				//ViewFlipperへ追加する
				viewFlipper.addView(setFlipperView[i]);
			}

		}catch(Exception e){
			e.printStackTrace();
		}

	}

	//フリックによるアニメーションをセットする
	private void setAnimations(){
		inFromRightAnimation =
				AnimationUtils.loadAnimation(getApplicationContext(), R.anim.right_in);
		inFromLeftAnimation =
				AnimationUtils.loadAnimation(getApplicationContext(), R.anim.left_in);
		outToRightAnimation =
				AnimationUtils.loadAnimation(getApplicationContext(), R.anim.right_out);
		outToLeftAnimation =
				AnimationUtils.loadAnimation(getApplicationContext(), R.anim.left_out);
	}

	//説明を表示する
	private void setHelp(){
		if(!readSetPara()){
			LinearLayout linear = new LinearLayout(getApplicationContext());
			linear.setOrientation(LinearLayout.VERTICAL);
			TextView textView = new TextView(getApplicationContext());
			final CheckBox checkBox = new CheckBox(getApplicationContext());
			textView.setText(R.string.first_help_inbox_strings);
			checkBox.setText("この説明を次回から表示しない。");

			linear.addView(textView);
			linear.addView(checkBox);

			createDialog("操作説明", linear, "OK", null,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							setPara(checkBox.isChecked());
						}
					});
		}
	}

	//アイコン加工の説明の表示設定の有無
	private boolean readSetPara(){
		boolean flag = false;
		String[] data = sql.searchDataBySheetNameAndType(db, "help", "first_help_3");

		if(data.length != 0){
			StringTokenizer st = new StringTokenizer(data[0], ",");
			st.nextToken(); st.nextToken();
			String flagString = st.nextToken();
			if(flagString.equals("true"))
				flag = true;
			else if(flagString.equals("false"))
				flag = false;
		}

		return flag;
	}

	//アイコン加工説明の表示設定
	private void setPara(boolean flag){
		String[] data = sql.searchDataBySheetNameAndType(db, "help", "first_help_3");

		if(data.length != 0){
			sql.upDateEntry(db, "sheetData", "dataType = ?",
					new String[]{"first_help_3"},
					new String[]{"help", "first_help_3", (flag)? "true":"false"});
		}else {
			sql.createNewData(db, "sheetData",
					new String[]{"help", "first_help_3", (flag)? "true":"false"});
		}
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
										toast(InBoxActivity.this.getApplicationContext(), "すでに同じ名前が登録されています");
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
		init();
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

	//バックキーが押されたときの処理
	@Override
	public boolean dispatchKeyEvent(KeyEvent e){
		if(e.getAction() == KeyEvent.ACTION_DOWN){
			if(e.getKeyCode() == KeyEvent.KEYCODE_BACK){
				changeActivity(null);
			}
		}
		return super.dispatchKeyEvent(e);
	}

    //Activity変更
    public void changeActivity(String sheetName){
    	//インテントの生成
    	Intent intent = null;
    	try{
	    	if(sheetName != null){
	    		intent = new Intent(getApplicationContext(),
	    				vc.ddns.luna.sert.collectionbox.SheetActivity.class);
	    		//インテントへパラメータ追加
	    		intent.putExtra("sheetName", sheetName);
	    		intent.putExtra("boxName", boxName);

	    	}else{
	    		intent = new Intent(getApplicationContext(),
	    				vc.ddns.luna.sert.collectionbox.MainActivity.class);
	    	}

    		//Activityの呼び出し
    		this.startActivity(intent);
    		this.finish();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }

    //OutOfMemory対策の背景制御
    private void setAndRemoveBgIm(int[] viewNumbers, int[] selectViewNumbers){
    	for(int i=0; i<viewNumbers.length; i++){
			TextView shName = (TextView)setFlipperView[viewNumbers[i]].findViewById(R.id.inBox_sheetName);
			TextView shCom  = (TextView)setFlipperView[viewNumbers[i]].findViewById(R.id.inBox_sheetComment);
			Button shButton = (Button)setFlipperView[viewNumbers[i]].findViewById(R.id.inBox_sheetOpenButton);

			StringTokenizer st = new StringTokenizer(data[selectViewNumbers[i]], ",");
			//読み込んだデータを適応する
			st.nextToken();
			shName.setText(st.nextToken());

			String comment = st.nextToken();
			if(comment.equals("  "))
				comment = "";
			shCom.setText(comment);

			String bgImPath = st.nextToken();

			shButton.setTag(shName.getText().toString());
			shButton.setOnClickListener(this);

	    	if(!bgImPath.equals("  ")){
	    		Drawable bgIm = new BitmapDrawable(BitmapFactory.decodeFile(bgImPath));
	    		setFlipperView[viewNumbers[i]].setBackgroundDrawable(bgIm);
	    		bgIm.setCallback(null);
	    	}else
	    		setFlipperView[viewNumbers[i]].setBackgroundDrawable(null);
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

				//以下　OutOfMemoryの対策
				//ViewFlipperにはメインViewとシートView２つの計３つのViewのみを登録し
				//メイン+シート数　のデータを状況に応じて適応してあたかもViewが多数あるかのように処理する
				//フリック動作によってViewを切り替えるときに表示させるViewがメインでないにもかかわらず
				//locationが0になる場合や、メインを表示させるのにlocationが0にならない場合は
				//アニメーションのないView切り替えを追加で行いViewをスキップして
				//目的のViewへ到達させる
				int[] setView = null, selectView = null;
				boolean Fling2Flag = false;

				if(location == 0 && viewSheetNumber == 0){
					setView = new int[]{1};
					selectView = new int[]{data.length - 1};

				}else if(location == 2){
					if(viewSheetNumber == 1){
						setView = new int[]{0, 1};
						selectView = new int[]{0, 0};
						Fling2Flag = true;

					}else{
						setView = new int[]{1, 0};
						selectView = new int[]{viewSheetNumber - 1, viewSheetNumber-2};
					}

				}else{
					if(viewSheetNumber == 1){
						setView = new int[]{0};
						selectView = new int[]{0};

					}else{
						setView = new int[]{0, 1};
						selectView = new int[]{viewSheetNumber - 1, viewSheetNumber-2};
						Fling2Flag = true;
					}
				}

				setAndRemoveBgIm(setView, selectView);

				viewFlipper.setInAnimation(inFromLeftAnimation);
				viewFlipper.setOutAnimation(outToRightAnimation);
				viewFlipper.showPrevious();

				location--;
				if(location <= -1)
					location = 2;

				viewSheetNumber--;
				if(viewSheetNumber < 0)
					viewSheetNumber = data.length;

				if(Fling2Flag){
					viewFlipper.showPrevious();
					location--;
					if(location <= -1)
						location = 2;
				}

			}else{
				int[] setView = null, selectView = null;
				boolean Fling2Flag = false;

				if(location == 0 && viewSheetNumber == 0){
					setView = new int[]{0};
					selectView = new int[]{0};

				}else if(location == 1){
					if(viewSheetNumber == data.length){
						setView = new int[]{0, 1};
						selectView = new int[]{data.length - 1, data.length - 1};
						Fling2Flag = true;

					}else{
						setView = new int[]{0, 1};
						selectView = new int[]{viewSheetNumber - 1, viewSheetNumber};
					}

				}else{
					if(viewSheetNumber == data.length){
						setView = new int[]{1};
						selectView = new int[]{data.length - 1};

					}else{
						setView = new int[]{1, 0};
						selectView = new int[]{viewSheetNumber - 1, viewSheetNumber};
						Fling2Flag = true;
					}
				}

				setAndRemoveBgIm(setView, selectView);

				viewFlipper.setInAnimation(inFromRightAnimation);
				viewFlipper.setOutAnimation(outToLeftAnimation);
				viewFlipper.showNext();

				location++;
				if(location >= 3)
					location = 0;

				viewSheetNumber++;
				if(viewSheetNumber >= data.length + 1)
					viewSheetNumber = 0;

				if(Fling2Flag){
					viewFlipper.showNext();
					location++;
					if(location >= 3)
						location = 0;
				}
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
