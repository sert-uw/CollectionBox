package vc.ddns.luna.sert.collectionbox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
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

	private List<LinearLayout> musicList = new ArrayList<LinearLayout>();//楽曲一覧をListで保持
	private int				selectedNumber = 0;//選択されてる楽曲番号を保持

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

		doBindService();
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
		musicList.removeAll(musicList);

		for(int i=0; i<musicData.length; i++){
			StringTokenizer st = new StringTokenizer(musicData[i], ",");
			st.nextToken(); st.nextToken();

			ContentResolver cr = getContentResolver();
            String[] columns = {MediaStore.Images.Media.DATA };
            Cursor c = cr.query(Uri.parse(st.nextToken()), columns, null, null, null);

            c.moveToFirst();
            File file = new File(c.getString(0));

			readMusicData(MediaStore.Audio.Media.DISPLAY_NAME + " = ?", new String[]{file.getName()});
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

	//タグ用変数
	private int count = 0;

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

        	linear.setOnClickListener(SheetActivity.this);
        	linear.setTag("" + count);
        	count++;

        	musicList.add(linear);
        	scrollLinear.addView(linear, params);

        	break;
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
	            String[] addData = new String[]{sheetName, "music", data.getDataString()};

	            sql.createNewData(db, "sheetData", addData);
	            readData();
			}
		}
	}

	//musicListの背景制御
	private void setLinearBg(boolean switching){
		if(switching)
			musicList.get(selectedNumber).setBackgroundResource(R.drawable.under_line2);
		else
			musicList.get(selectedNumber).setBackgroundResource(R.drawable.under_line);
	}

	/////////////////////////////////////////////////
	///////////クリック、ジェスチャー処理////////////
	/////////////////////////////////////////////////
	@Override
	public void onClick(View v) {
		String tag = v.getTag().toString();

		if(tag.equals("playBack")){
			if(mpService != null){
				if(!mpService.isSetMusic())
					setMusic();
				startMusic();
			}

		}else if(tag.equals("pause")){
			mpService.pauseMusic();
			((ImageButton)findViewById(R.id.sheet_music_playBack_button)).setImageResource(R.drawable.button1);
			findViewById(R.id.sheet_music_playBack_button).setTag("playBack");

		}else if(tag.equals("rewinding")){
			if(mpService != null)
				mpService.movePosition(0);

		}else if(tag.equals("fastForwarding")){

		}else {
			setLinearBg(false);
			selectedNumber = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
			setMusic();
			startMusic();
		}
	}

	//楽曲のセット
	private void setMusic(){
		if(mpService != null){
			if(mpService.isSetMusic())
				mpService.moveToIdle();
			StringTokenizer st = new StringTokenizer(musicData[selectedNumber], ",");
			st.nextToken(); st.nextToken();
			mpService.setMusic(SheetActivity.this, Uri.parse(st.nextToken()));
			setLinearBg(true);
		}
	}

	//楽曲の再生
	private void startMusic(){
		mpService.startMusic();
		mpService.setLooping(true);
		mpService.setSeekBar((SeekBar)findViewById(R.id.sheet_music_seekBar));
		((ImageButton)findViewById(R.id.sheet_music_playBack_button)).setImageResource(R.drawable.button4);
		findViewById(R.id.sheet_music_playBack_button).setTag("pause");
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

	/////////////////////////////////////////////////////////
	//////////////サービスとのバインド処理など///////////////
	/////////////////////////////////////////////////////////

	private MusicPlayerService mpService;
	private boolean mIsBound;

	private ServiceConnection mConnection = new ServiceConnection(){
		public void onServiceConnected(ComponentName className, IBinder service){
			//サービスとの接続確率時に呼び出される
			Toast.makeText(SheetActivity.this, "Activity:onServiceConnected", Toast.LENGTH_SHORT).show();

			//サービスにはIBinder経由で#getService()してダイレクトにアクセス可能
			mpService = ((MusicPlayerService.ServiceLocalBinder)service).getService();
		}

		public void onServiceDisconnected(ComponentName className){
			//サービスとの切断(異常系処理)
			//プロセスのクラッシュなど意図しないサービスの切断が発生した場合に呼ばれる
			mpService = null;
			Toast.makeText(SheetActivity.this, "Activity:onServiceDisconnected", Toast.LENGTH_SHORT).show();
		}
	};

	void doBindService(){
		//サービスとの接続を確立する　明示的にServiceを指定
		bindService(new Intent(SheetActivity.this, MusicPlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService(){
		if(mIsBound){
			//コネクションの解除
			unbindService(mConnection);
			mIsBound = false;
		}
	}
}
