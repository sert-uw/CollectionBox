package vc.ddns.luna.sert.collectionbox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.KeyEvent;
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

	private SeekBar			seekBar;//レイアウトのシークバー

	private String			boxName;//ボックス名
	private String			sheetName;//シート名

	private String[]		musicData;//音楽データ
	private String[]		pictureData;//画像データ
	private String			musicSequence;//音楽の再生順番定義

	private boolean			deleteFlag;//楽曲の削除モード

	private List<Uri>		musicPlayList = new ArrayList<Uri>();//プレイリスト保持
	private boolean			shuffleFlag;//シャッフルモードかどうか
	private boolean			repeatFlag;//リピートするかどうか

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
			 boxName = extras.getString("boxName");
		}

		//SQLiteオブジェクト及びデータベースオブジェクトの生成
		sql = new MySQLite(this);
		db = sql.getWritableDatabase();

		Intent intent = new Intent(this, MusicPlayerService.class);
		if(!isServiceRunning("vc.ddns.luna.sert.collectionbox.MusicPlayerService"))
			doStartService(intent);

		setLayout();
		doBindService(intent);
		setAnimations();
		readData();
	}

	//バックキーが押されたときの処理
	@Override
	public boolean dispatchKeyEvent(KeyEvent e){
		if(e.getAction() == KeyEvent.ACTION_DOWN){
			if(e.getKeyCode() == KeyEvent.KEYCODE_BACK){
				doUnbindService();
				changeActivity();
			}
		}
		return super.dispatchKeyEvent(e);
	}

    //Activity変更
    public void changeActivity(){
    	//インテントの生成
    	Intent intent = new Intent(this,
    			vc.ddns.luna.sert.collectionbox.InBoxActivity.class);
    	try{
    		//インテントへパラメータ追加
    		intent.putExtra("sheetName", sheetName);
    		intent.putExtra("boxName", boxName);

    		//Activityの呼び出し
    		this.startActivity(intent);
    		this.finish();
    	}catch(Exception e){
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

	//タグ用変数
	private int count = 0;

	//データベースから読み込む
	private void readData(){
		musicData = sql.searchDataBySheetNameAndType(db, sheetName, "music");
		pictureData = sql.searchDataBySheetNameAndType(db, sheetName, "picture");
		String[] readData = sql.searchDataBySheetNameAndType(db, sheetName, "musicSequence");
		if(readData.length == 0){
			musicSequence = "  ";
			String[] addData = new String[]{sheetName, "musicSequence", musicSequence};
			sql.createNewData(db, "sheetData", addData);
		}else{
			StringTokenizer st = new StringTokenizer(readData[0], ",");
			st.nextToken(); st.nextToken();
			musicSequence = st.nextToken();
		}

		((LinearLayout)musicView.findViewById(R.id.sheet_music_scroll_linear)).removeAllViews();
		musicList.removeAll(musicList);

		List<String> list = new ArrayList<String>();

		for(int i=0; i<musicData.length; i++){
			StringTokenizer st = new StringTokenizer(musicData[i], ",");
			st.nextToken(); st.nextToken();

			ContentResolver cr = getContentResolver();
            String[] columns = {MediaStore.Images.Media.DATA };
            Cursor c = cr.query(Uri.parse(st.nextToken()), columns, null, null, null);

            c.moveToFirst();
            File file = new File(c.getString(0));

            list.add(file.getName());
		}

		count = 0;

		musicPlayList.removeAll(musicPlayList);
		StringTokenizer st = new StringTokenizer(musicSequence, "/");
		for(int i=0; i<list.size(); i++){
			int number = i;
			if(st.hasMoreTokens() && !musicSequence.equals("  "))
				number = list.indexOf(st.nextToken());

			StringTokenizer st2 = new StringTokenizer(musicData[number], ",");
			st2.nextToken(); st2.nextToken();
			musicPlayList.add(Uri.parse(st2.nextToken()));

			readMusicData(MediaStore.Audio.Media.DISPLAY_NAME + " = ?", new String[]{list.get(number)});
		}

		setLinearBg(true);

		setPlayList();
	}

	//Layoutのセットを行う
	private void setLayout(){
		musicView = inflater.inflate(R.layout.sheet_music_layout, null);
		viewFlipper.addView(musicView);

		((ImageButton)musicView.findViewById(R.id.sheet_music_playBack_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_rewinding_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_fastForwarding_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_shuffle_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_repeat_button)).setOnClickListener(this);

		seekBar = (SeekBar)musicView.findViewById(R.id.sheet_music_seekBar);
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

        	linear.setOnClickListener(SheetActivity.this);
        	linear.setTag("" + count);
        	count++;

        	musicList.add(linear);
        	scrollLinear.addView(linear, params);

        	break;
        }
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

	//メニューの作成
	private final static int MENU_ITEM0 = 0;
	private final static int MENU_ITEM1 = 1;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem item0 = menu.add(0, MENU_ITEM0, 0, "楽曲追加");
		MenuItem item1 = menu.add(0, MENU_ITEM1, 1, "楽曲削除/削除解除");

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

		case MENU_ITEM1:
			deleteFlag = !deleteFlag;

			changeDelModView();
			Toast.makeText(SheetActivity.this, "削除する楽曲を選択してください。", Toast.LENGTH_SHORT).show();

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

	            musicPlayList.add(data.getData());
	            String list = "";

	            for(int i=0; i<musicPlayList.size(); i++){
	            	ContentResolver cr = getContentResolver();
	            	String[] columns = {MediaStore.Images.Media.DATA };
	            	Cursor c = cr.query(musicPlayList.get(i), columns, null, null, null);

	            	c.moveToFirst();
	            	File file = new File(c.getString(0));

	            	list += file.getName();
	            	if(i != musicPlayList.size() - 1)
	            		list += "/";
	            }

	            addData = new String[]{sheetName, "musicSequence", list};
	            sql.upDateEntry(db, "sheetData", "dataType = ?",
	            		new String[]{"musicSequence"}, addData);

	            readData();
			}
		}
	}

	//Deleteモードの見た目切り替え
	private void changeDelModView(){
		if(musicList.size() == 0)
			return;

		//削除フラグによって色を変更
		for(int i=0; i<musicList.size(); i++){
			if(deleteFlag)
				musicList.get(i).setBackgroundResource(R.drawable.under_line_delete);
			else
				musicList.get(i).setBackgroundResource(R.drawable.under_line);
		}
		setLinearBg(true);
	}

	//musicListの背景制御
	private void setLinearBg(boolean switching){
		if(musicList.size() == 0)
			return;

		if(deleteFlag){
			if(switching)
				musicList.get(selectedNumber).setBackgroundResource(R.drawable.under_line2_delete);
			else
				musicList.get(selectedNumber).setBackgroundResource(R.drawable.under_line_delete);
		}else {
			if(switching)
				musicList.get(selectedNumber).setBackgroundResource(R.drawable.under_line2);
			else
				musicList.get(selectedNumber).setBackgroundResource(R.drawable.under_line);
		}
	}

	//再生ボタンと一時停止ボタンの切り替え
	private void switchCenterButton(String tag){
		if(tag.equals("playBack")){
			((ImageButton)findViewById(R.id.sheet_music_playBack_button)).setImageResource(R.drawable.button1);
			findViewById(R.id.sheet_music_playBack_button).setTag("playBack");

		}else if(tag.equals("pause")){
			((ImageButton)findViewById(R.id.sheet_music_playBack_button)).setImageResource(R.drawable.button4);
			findViewById(R.id.sheet_music_playBack_button).setTag("pause");
		}
	}

	//再生中のトラックナンバー同期
	public void setTrackNumber(int number){
		setLinearBg(false);
		selectedNumber = number;
		setLinearBg(true);
	}

	/////////////////////////////////////////////////
	///////////クリック、ジェスチャー処理////////////
	/////////////////////////////////////////////////
	@Override
	public void onClick(View v) {
		String tag = v.getTag().toString();

		//再生ボタン処理
		if(tag.equals("playBack")){
			if(mpService != null && musicList.size() != 0){
				setPlayList();
				if(!mpService.isSetMusic())
					setMusic();
				startMusic();
			}

		}

		//一時停止ボタン処理
		else if(tag.equals("pause")  && musicList.size() != 0){
			mpService.pauseMusic();
			switchCenterButton("playBack");

		}

		//戻るボタン処理
		else if(tag.equals("rewinding")  && musicList.size() != 0){
			if(mpService != null)
				mpService.rewinding();

		}

		//進むボタン処理
		else if(tag.equals("fastForwarding")  && musicList.size() != 0){
			if(mpService != null)
				mpService.nextMusic();

		}

		//シャッフルボタン処理
		else if(tag.equals("shuffle")){
			shuffleFlag = !shuffleFlag;
			mpService.setShuffle(shuffleFlag);

			if(shuffleFlag){
				((ImageButton)findViewById(R.id.sheet_music_shuffle_button))
				.setImageResource(R.drawable.button5_2);

			}else {
				((ImageButton)findViewById(R.id.sheet_music_shuffle_button))
				.setImageResource(R.drawable.button5_1);
			}

		}

		//リピートボタン処理
		else if(tag.equals("repeat")){
			repeatFlag = !repeatFlag;
			mpService.setLooping(repeatFlag);

			if(repeatFlag){
				((ImageButton)findViewById(R.id.sheet_music_repeat_button))
				.setImageResource(R.drawable.button6_2);

			}else {
				((ImageButton)findViewById(R.id.sheet_music_repeat_button))
				.setImageResource(R.drawable.button6_1);
			}

		}

		//選曲/削除処理
		else {
			if(deleteFlag){
				setLinearBg(false);
				selectedNumber = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
				setLinearBg(true);
				deleteMusic();

			}else if(musicList.size() != 0){
				setLinearBg(false);
				selectedNumber = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
				setPlayList();
				setMusic();
				startMusic();
			}
		}
	}

	//楽曲の削除
	private void deleteMusic(){
		ContentResolver cr = getContentResolver();
        String[] columns = {MediaStore.Images.Media.DATA };
        Cursor c = cr.query(musicPlayList.get(selectedNumber), columns, null, null, null);

        c.moveToFirst();
        File file = new File(c.getString(0));

        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(
        		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ,  //データの種類
        		new String[]{
        				MediaStore.Audio.Media.TITLE,
        				MediaStore.Audio.Media.ARTIST ,
        				MediaStore.Audio.Media.ALBUM,
        		},
        		MediaStore.Audio.Media.DISPLAY_NAME + " = ?",
        		new String[]{file.getName()},
        		null   //並べ替え
        );

        if(!cursor.moveToNext())
        	return;

        final TextView text = new TextView(this);
        text.setText(cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.TITLE )));
        text.setTag(musicPlayList.get(selectedNumber));
        text.setTextSize(26);

		createDialog("削除しますか？", text, "delete", "cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which == DialogInterface.BUTTON_POSITIVE){
							Uri delUri = Uri.parse(text.getTag().toString());
							sql.deleteEntry(db, "sheetData",
									"sheetName = ? and dataType = ? and data = ?",
									new String[]{sheetName, "music", delUri.toString()});

							musicPlayList.remove(delUri);
				            String list = "";

				            for(int i=0; i<musicPlayList.size(); i++){
				            	ContentResolver cr = getContentResolver();
				            	String[] columns = {MediaStore.Images.Media.DATA };
				            	Cursor c = cr.query(musicPlayList.get(i), columns, null, null, null);

				            	c.moveToFirst();
				            	File file = new File(c.getString(0));

				            	list += file.getName();
				            	if(i != musicPlayList.size() - 1)
				            		list += "/";
				            }

				            String[] addData = new String[]{sheetName, "musicSequence", list};
				            sql.upDateEntry(db, "sheetData", "dataType = ?",
				            		new String[]{"musicSequence"}, addData);

				            if(selectedNumber >= musicPlayList.size())
				            	selectedNumber = 0;
				            readData();

				            deleteFlag = false;
							changeDelModView();
						}else if(which == DialogInterface.BUTTON_NEGATIVE){
							deleteFlag = false;
							changeDelModView();
						}
					}
				});
	}

	//楽曲のセット
	private void setMusic(){
		if(mpService != null){
			if(mpService.isSetMusic())
				mpService.moveToIdle();
			StringTokenizer st = new StringTokenizer(musicData[selectedNumber], ",");
			st.nextToken(); st.nextToken();
			mpService.setMusic(Uri.parse(st.nextToken()));
			setLinearBg(true);
		}
	}

	//プレイリストのセット
	private void setPlayList(){
		if(mpService != null)
			mpService.setPlayList(musicPlayList);
	}

	//楽曲の再生
	private void startMusic(){
		if(!mpService.isSetSeekBar())
			mpService.setSeekBar(seekBar);
		mpService.startMusic();
		switchCenterButton("pause");
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
			//サービスとの接続確立時に呼び出される
			//Toast.makeText(SheetActivity.this, "Activity:onServiceConnected", Toast.LENGTH_SHORT).show();

			//サービスにはIBinder経由で#getService()してダイレクトにアクセス可能
			mpService = ((MusicPlayerService.ServiceLocalBinder)service).getService();

			//サービスが起動済みの時、別のシートのサービスであるならば再起動する
			if(!mpService.setNames(boxName, sheetName)){
				mpService.shutdown();
				doUnbindService();
				doStartService(new Intent(SheetActivity.this, MusicPlayerService.class));
				doBindService(new Intent(SheetActivity.this, MusicPlayerService.class));
				return;
			}

			recoveryViews();

			switchCenterButton((mpService.isPlaying())? "pause":"playBack");
		}

		public void onServiceDisconnected(ComponentName className){
			//サービスとの切断(異常系処理)
			//プロセスのクラッシュなど意図しないサービスの切断が発生した場合に呼ばれる
			mpService = null;
			doUnbindService();
			//Toast.makeText(SheetActivity.this, "Activity:onServiceDisconnected", Toast.LENGTH_SHORT).show();
		}
	};

	//Bind完了後Viewの復元
	private void recoveryViews(){
		mpService.setContext(SheetActivity.this);
		mpService.setSeekBar(seekBar);
		mpService.sendTrackNumber();

		if(mpService.isShuffle()){
			((ImageButton)findViewById(R.id.sheet_music_shuffle_button))
			.setImageResource(R.drawable.button5_2);

		}else {
			((ImageButton)findViewById(R.id.sheet_music_shuffle_button))
			.setImageResource(R.drawable.button5_1);
		}

		if(mpService.isLooping()){
			((ImageButton)findViewById(R.id.sheet_music_repeat_button))
			.setImageResource(R.drawable.button6_2);

		}else {
			((ImageButton)findViewById(R.id.sheet_music_repeat_button))
			.setImageResource(R.drawable.button6_1);
		}
	}

	private void doStartService(Intent intent){
		startService(intent);
	}

	private void doBindService(Intent intent){
		//サービスとの接続を確立する　明示的にServiceを指定
		bindService(intent, mConnection, BIND_AUTO_CREATE);
		mIsBound = true;
	}

	private void doUnbindService(){
		if(mIsBound){
			//コネクションの解除
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	//サービスが起動中かどうかを調べる
	private boolean isServiceRunning(String className){
		ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceInfos =
				am.getRunningServices(Integer.MAX_VALUE);

		for(int i=0; i<serviceInfos.size(); i++){
			if(serviceInfos.get(i).service.getClassName().equals(className)){
				return true;
			}
		}
		return false;
	}
}
