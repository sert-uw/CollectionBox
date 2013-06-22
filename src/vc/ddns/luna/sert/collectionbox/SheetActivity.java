package vc.ddns.luna.sert.collectionbox;

import java.io.File;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class SheetActivity extends Activity implements OnClickListener,
		GestureDetector.OnGestureListener{

	private static final int REQUEST_MUSIC = 0;//他アプリからの返り値取得用

	private GestureDetector	gestureDetector; //GestureDetectorオブジェクト
	private ViewFlipper 	viewFlipper;//アニメーション用Viewオブジェクト
	private LayoutInflater	inflater;//LayoutをViewとして取得する
	private View			musicView;//sheet_music_layoutのView

	private String			sheetName;//シート名

	private String[]		musicData;//音楽データ
	private String[]		pictureData;//画像データ

	private MySQLite		sql;	//SQLiteオブジェクト
	private SQLiteDatabase	db;		//データベースオブジェクト

	//各種アニメーションオブジェクト
	private Animation 		inFromRightAnimation;
	private Animation 		inFromLeftAnimation;
	private Animation 		outToRightAnimation;
	private Animation 		outToLeftAnimation;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sheet_main_layout);
		viewFlipper = (ViewFlipper)findViewById(R.id.sheet_viewFlipper);

		gestureDetector = new GestureDetector(this, this);
		inflater = LayoutInflater.from(this);

		//InBoxActivityからデータを引き継ぐ
		Bundle extras = getIntent().getExtras();
		if(extras != null){
			 sheetName = extras.getString("sheetName");
		}

		//SQLiteオブジェクト及びデータベースオブジェクトの生成
		sql = new MySQLite(this);
		db = sql.getWritableDatabase();

		setAnimations();
		setLayout();
		readData();
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

	//データベースから読み込む
	private void readData(){
		musicData = sql.searchDataBySheetNameAndType(db, sheetName, "music");
		pictureData = sql.searchDataBySheetNameAndType(db, sheetName, "picture");

		((LinearLayout)musicView.findViewById(R.id.sheet_music_scroll_linear)).removeAllViews();

		for(int i=0; i<musicData.length; i++){
			StringTokenizer st = new StringTokenizer(musicData[i], ",");
			st.nextToken(); st.nextToken();

			readMusicData(MediaStore.Audio.Media.DISPLAY_NAME + " = ?", new String[]{st.nextToken()});
		}
	}

	//Layoutのセットを行う
	private void setLayout(){
		musicView = inflater.inflate(R.layout.sheet_music_layout, null);
		viewFlipper.addView(musicView);

		((ImageButton)musicView.findViewById(R.id.sheet_music_playBack_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_rewinding_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_fastForwarding_button)).setOnClickListener(this);

	}

	//音楽情報を読み込み、scrollViewに追加する
	private void readMusicData(String searchKey, String[] searchValue){
        LinearLayout scrollLinear = (LinearLayout)musicView.findViewById(R.id.sheet_music_scroll_linear);

		ContentResolver resolver = getContentResolver();

        Cursor cursor = resolver.query(
        		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ,  //データの種類
        		new String[]{
        				MediaStore.Audio.Media.ALBUM ,
        				MediaStore.Audio.Media.ARTIST ,
        				MediaStore.Audio.Media.TITLE,
        		},    // keys for select. null means all
        		searchKey,
        		searchValue,
        		null   //並べ替え
        );

        while( cursor.moveToNext() ){
        	LinearLayout linear = new LinearLayout(SheetActivity.this);
        	linear.setOrientation(LinearLayout.VERTICAL);

        	TextView titleView = new TextView(SheetActivity.this);
        	TextView artistView = new TextView(SheetActivity.this);
        	TextView albumView = new TextView(SheetActivity.this);

        	titleView.setMaxLines(1);
        	artistView.setMaxLines(1);
        	albumView.setMaxLines(1);

        	titleView.setTextSize(26);

        	titleView.setText(cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.TITLE )));
        	artistView.setText(cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST )));
        	albumView.setText(cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ALBUM )));

        	linear.addView(titleView);
        	linear.addView(artistView);
        	linear.addView(albumView);

        	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        			LinearLayout.LayoutParams.MATCH_PARENT,
        			LinearLayout.LayoutParams.WRAP_CONTENT);
        	params.setMargins(0, 30, 0, 0);

        	linear.setPadding(0, 0, 0, 10);
        	linear.setBackgroundResource(R.drawable.under_line);

        	scrollLinear.addView(linear, params);
        }
	}

	//メニューの作成
	private final static int MENU_ITEM0 = 0;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem item0 = menu.add(0, MENU_ITEM0, 0, "楽曲追加");

		return true;
	}

	//メニューのイベント処理
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM0:
			Intent intent = new Intent();
			intent.setType("audio/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(intent, REQUEST_MUSIC);
			break;
		default: break;

		}

		return true;
	}

	//他アプリからの結果参照
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode == RESULT_OK){
			if(requestCode == REQUEST_MUSIC){
				ContentResolver cr = getContentResolver();
	            String[] columns = {MediaStore.Images.Media.DATA };
	            Cursor c = cr.query(data.getData(), columns, null, null, null);

	            c.moveToFirst();
	            File file = new File(c.getString(0));

	            String[] addData = new String[]{sheetName, "music", file.getName()};

	            sql.createNewData(db, "sheetData", addData);
	            readData();
			}
		}
	}

	@Override
	public void onClick(View v) {
		String tag = v.getTag().toString();
		System.out.println(tag);
		if(tag.equals("playBack")){

		}else if(tag.equals("rewinding")){

		}else if(tag.equals("fastForwarding")){

		}
	}

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

}
