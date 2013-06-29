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
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class SheetActivity extends Activity implements OnClickListener{

	private static final int REQUEST_MUSIC = 0;//他アプリからの返り値取得用
	private static final int REQUEST_GALLERY = 1;

	private ViewFlipper 	viewFlipper;//アニメーション用Viewオブジェクト
	private LayoutInflater	inflater;//LayoutをViewとして取得する

	private boolean			changeFlag;//falseならmusic trueならpicture

	private View			pictureView;//sheet_picture_layoutのView
	private View			musicView;//sheet_music_layoutのView
	private View			searchView;//検索結果レイアウト

	private SeekBar			seekBar;//レイアウトのシークバー
	private SeekBar			picSeekBar;

	private String			boxName;//ボックス名
	private String			sheetName;//シート名

	private String[]		musicData;//音楽データ
	private String[]		pictureData;//画像データ

	private List<Uri>		pictureList = new ArrayList<Uri>();//画像のUriを保持する

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
		readMusicData();
		readPictureData();

		setHelp();
	}

	//バックキーが押されたときの処理
	@Override
	public boolean dispatchKeyEvent(KeyEvent e){
		if(e.getAction() == KeyEvent.ACTION_DOWN){
			if(e.getKeyCode() == KeyEvent.KEYCODE_BACK){
				doUnbindService();
				changeActivity(-1);
			}
		}
		return super.dispatchKeyEvent(e);
	}

	@Override
	public void onUserLeaveHint(){
		//ホームボタンが押された時や、他のアプリが起動した時に呼ばれる
		//戻るボタンが押された場合には呼ばれない

		doUnbindService();
		this.finish();
	}

    //Activity変更
    public void changeActivity(int number){
    	//インテントの生成
    	Intent intent;

    	if(number == -1)
    		intent = new Intent(this,
    				vc.ddns.luna.sert.collectionbox.InBoxActivity.class);
    	else
    		intent = new Intent(this,
    				vc.ddns.luna.sert.collectionbox.PictureActivity.class);

    	try{
    		//インテントへパラメータ追加
    		intent.putExtra("sheetName", sheetName);
    		intent.putExtra("boxName", boxName);
    		intent.putExtra("select", number);

    		//Activityの呼び出し
    		this.startActivity(intent);
    		this.finish();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }

	//Layoutのセットを行う
	private void setLayout(){
		musicView = inflater.inflate(R.layout.sheet_music_layout, null);
		pictureView = inflater.inflate(R.layout.sheet_picture_layout, null);

		viewFlipper.addView(musicView);
		viewFlipper.addView(pictureView);

		((Button)musicView.findViewById(R.id.sheet_music_changeButton)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_playBack_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_rewinding_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_fastForwarding_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_shuffle_button)).setOnClickListener(this);
		((ImageButton)musicView.findViewById(R.id.sheet_music_repeat_button)).setOnClickListener(this);
		seekBar = (SeekBar)musicView.findViewById(R.id.sheet_music_seekBar);

		((Button)pictureView.findViewById(R.id.sheet_music_changeButton)).setOnClickListener(this);
		((ImageButton)pictureView.findViewById(R.id.sheet_music_playBack_button)).setOnClickListener(this);
		((ImageButton)pictureView.findViewById(R.id.sheet_music_rewinding_button)).setOnClickListener(this);
		((ImageButton)pictureView.findViewById(R.id.sheet_music_fastForwarding_button)).setOnClickListener(this);
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

	//説明を表示する
		private void setHelp(){
			if(!readSetPara()){
				LinearLayout linear = new LinearLayout(this);
				linear.setOrientation(LinearLayout.VERTICAL);
				TextView textView = new TextView(this);
				final CheckBox checkBox = new CheckBox(this);
				textView.setText(R.string.first_help_sheet_strings);
				checkBox.setText("この説明を次回から表示しない。");

				linear.addView(textView);
				linear.addView(checkBox);

				createDialog("操作説明", linear, "OK", null, null,
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
			String[] data = sql.searchDataBySheetNameAndType(db, "help", "first_help_4");

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
			String[] data = sql.searchDataBySheetNameAndType(db, "help", "first_help_4");

			if(data.length != 0){
				sql.upDateEntry(db, "sheetData", "dataType = ?",
						new String[]{"first_help_4"},
						new String[]{"help", "first_help_4", (flag)? "true":"false"});
			}else {
				sql.createNewData(db, "sheetData",
						new String[]{"help", "first_help_4", (flag)? "true":"false"});
			}
		}

	//タグ用変数
	private int count = 0;

	//データベースから画像を読み込む
	private void readPictureData(){
		pictureData = sql.searchDataBySheetNameAndType(db, sheetName, "picture");

		LinearLayout scrollLinear = (LinearLayout)pictureView.findViewById(R.id.sheet_picture_scrollLinear);
		scrollLinear.removeAllViews();

		View.OnClickListener clickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final int number = Integer.parseInt(v.getTag().toString());

				if(deleteFlag){
					LinearLayout linear = (LinearLayout)inflater.inflate(R.layout.sheet_picture_item, null);
					ImageView imageView = (ImageView)linear.findViewById(R.id.sheet_picture_item_imaeg1);
					linear.removeAllViews();

					imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
					imageView.setImageURI(pictureList.get(number));

					createDialog("画像削除", imageView, "Delete", null, "cancel",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if(which == DialogInterface.BUTTON_POSITIVE){
										sql.deleteEntry(db, "sheetData", "data = ?",
												new String[]{pictureList.get(number).toString()});

										deleteFlag = false;
										readPictureData();
									}
								}
							});

				}else{
					doUnbindService();
					changeActivity(number);
				}
			}
		};

		pictureList.removeAll(pictureList);

		for(int i=0; i<pictureData.length; i++){
			StringTokenizer st = new StringTokenizer(pictureData[i], ",");
			st.nextToken(); st.nextToken();
			pictureList.add(Uri.parse(st.nextToken()));
		}

		for(int i=0; i<pictureList.size()/3.0; i++){
			View itemView = inflater.inflate(R.layout.sheet_picture_item, null);
			ImageView imageView1 = (ImageView)itemView.findViewById(R.id.sheet_picture_item_imaeg1);
			ImageView imageView2 = (ImageView)itemView.findViewById(R.id.sheet_picture_item_imaeg2);
			ImageView imageView3 = (ImageView)itemView.findViewById(R.id.sheet_picture_item_imaeg3);

			if(i * 3 < pictureList.size()){
				imageView1.setImageURI(pictureList.get(i * 3));
				imageView1.setTag(i * 3);
				imageView1.setOnClickListener(clickListener);
			}
			if(i * 3 + 1 < pictureList.size()){
				imageView2.setImageURI(pictureList.get(i * 3 + 1));
				imageView2.setTag(i * 3 + 1);
				imageView2.setOnClickListener(clickListener);
			}
			if(i * 3 + 2 < pictureList.size()){
				imageView3.setImageURI(pictureList.get(i * 3 + 2));
				imageView3.setTag(i * 3 + 2);
				imageView3.setOnClickListener(clickListener);
			}

			scrollLinear.addView(itemView);
		}
	}

	//データベースから音楽を読み込む
	private void readMusicData(){

		musicData = sql.searchDataBySheetNameAndType(db, sheetName, "music");
		String[] readData = sql.searchDataBySheetNameAndType(db, sheetName, "musicSequence");

		try{
			if(readData.length == 0){
				musicSequence = "  ";
				String[] addData = new String[]{sheetName, "musicSequence", musicSequence};
				sql.createNewData(db, "sheetData", addData);
			}else{
				StringTokenizer st = new StringTokenizer(readData[0], ",");
				st.nextToken(); st.nextToken();
				musicSequence = st.nextToken();
			}
		}catch (Exception e){
			System.out.println(readData[0]);
			e.printStackTrace();
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

            c.close();
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

        cursor.close();
	}

	//ダイアログの表示
	/*title:Dialogのタイトル        view:Dialogに埋め込むView
	  ptext:Positiveボタンの文字列  ntext:Negativeボタンの文字列
	  listener:ボタンのイベントリスナー
	*/
	private void createDialog(String title, View view,
			String ptext, String neuText, String negText,
			DialogInterface.OnClickListener listener) {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		if(title != null)
			ad.setTitle(title);
		if(view != null)
			ad.setView(view);
		if(ptext != null)
			ad.setPositiveButton(ptext, listener);
		if(neuText != null)
			ad.setNeutralButton(neuText, listener);
		if(negText != null)
			ad.setNegativeButton(negText, listener);
		ad.show();
	}

	//メニューの作成
	private final static int MENU_ITEM0 = 0;
	private final static int MENU_ITEM1 = 1;
	private final static int MENU_ITEM2 = 2;
	private final static int MENU_ITEM3 = 3;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
			menu.add(0, MENU_ITEM0, 0, "画像追加");
			menu.add(0, MENU_ITEM1, 1, "画像削除/削除解除");
			menu.add(0, MENU_ITEM2, 0, "楽曲追加");
			menu.add(0, MENU_ITEM3, 1, "楽曲削除/削除解除");

		return true;
	}

	//表示するメニューを決定する
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		if(changeFlag){
			menu.findItem(MENU_ITEM0).setVisible(true);
			menu.findItem(MENU_ITEM1).setVisible(true);
			menu.findItem(MENU_ITEM2).setVisible(false);
			menu.findItem(MENU_ITEM3).setVisible(false);
		}else {
			menu.findItem(MENU_ITEM0).setVisible(false);
			menu.findItem(MENU_ITEM1).setVisible(false);
			menu.findItem(MENU_ITEM2).setVisible(true);
			menu.findItem(MENU_ITEM3).setVisible(true);
		}

		return true;
	}

	//メニューのイベント処理
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case MENU_ITEM0:
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(intent, REQUEST_GALLERY);
			break;

		case MENU_ITEM1:
			deleteFlag = !deleteFlag;

			Toast.makeText(SheetActivity.this, "削除する画像を選択してください。", Toast.LENGTH_SHORT).show();
			break;

		case MENU_ITEM2:
			final View searchView = inflater.inflate(R.layout.search_dialog, null);
			createDialog("楽曲検索", searchView, "検索", "一覧から探す", "閉じる",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface paramDialogInterface, int paramInt) {
							if(paramInt == DialogInterface.BUTTON_POSITIVE){
								String title = ((TextView)searchView.findViewById(R.id.search_dialog_title))
										.getText().toString();
								String artist = ((TextView)searchView.findViewById(R.id.search_dialog_artist))
										.getText().toString();
								String album = ((TextView)searchView.findViewById(R.id.search_dialog_album))
										.getText().toString();

								String searchStr = "";
								String[] value = new String[3];
								int count = 0;

								if(!title.equals("")){
									searchStr += MediaStore.Audio.Media.DISPLAY_NAME + " like '%' || ? || '%'";
									value[count] = title;
									count++;
								}
								if(!artist.equals("")){
									if(count != 0)
										searchStr += " and ";
									searchStr += MediaStore.Audio.Media.ARTIST + " like '%' || ? || '%'";
									value[count] = artist;
									count++;
								}
								if(!album.equals("")){
									if(count != 0)
										searchStr += " and ";
									searchStr += MediaStore.Audio.Media.ALBUM + " like '%' || ? || '%'";
									value[count] = album;
									count++;
								}

								String[] searchValue = new String[count];
								for(int i=0; i<count; i++)
									searchValue[i] = value[i];

								if(searchStr.equals(""))
									setSearchResult(null, null);
								else
									setSearchResult(searchStr, searchValue);

							}else if(paramInt == DialogInterface.BUTTON_NEUTRAL){
								Intent intent = new Intent();
								intent.setType("audio/*");
								intent.setAction(Intent.ACTION_GET_CONTENT);
								startActivityForResult(intent, REQUEST_MUSIC);
							}
						}
					});
			break;

		case MENU_ITEM3:
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

	            	 c.close();
	            }

	            addData = new String[]{sheetName, "musicSequence", list};
	            sql.upDateEntry(db, "sheetData", "dataType = ?",
	            		new String[]{"musicSequence"}, addData);

	            readMusicData();

			}else if(requestCode == REQUEST_GALLERY){
				String[] addData = new String[]{sheetName, "picture", data.getDataString()};
	            sql.createNewData(db, "sheetData", addData);

	            readPictureData();
			}
		}
	}

	//検索結果画面処理
	private void setSearchResult(String searchStr, String[] searchValue){
		ContentResolver resolver = getContentResolver();

        Cursor cursor = resolver.query(
        		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ,  //データの種類
        		new String[]{
        				MediaStore.Audio.Media.ALBUM ,
        				MediaStore.Audio.Media.ARTIST ,
        				MediaStore.Audio.Media.TITLE,
        				MediaStore.Audio.Media._ID
        		},    // keys for select. null means all
        		searchStr,
        		searchValue,
        		null   //並べ替え
        );

        searchView = inflater.inflate(R.layout.search_result, null);
        LinearLayout scrollLinear = (LinearLayout)searchView.findViewById(R.id.search_result_scrollLinear);

        int setCount = 0;
        final List<View> itemViewList = new ArrayList<View>();

        View.OnClickListener clickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String tag = v.getTag().toString();

				if(tag.equals("add")){
					for(int i=0; i<itemViewList.size(); i++){
						CheckBox check = (CheckBox)itemViewList.get(i)
								.findViewById(R.id.search_result_item_check);
						if(check.isChecked()){
							Uri uri = Uri.parse(check.getTag().toString());

				            String[] addData = new String[]{sheetName, "music", uri.toString()};
				            sql.createNewData(db, "sheetData", addData);

				            musicPlayList.add(uri);
						}
					}

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

		            	 c.close();
		            }

		            String[] addData = new String[]{sheetName, "musicSequence", list};
		            sql.upDateEntry(db, "sheetData", "dataType = ?",
		            		new String[]{"musicSequence"}, addData);

		            SheetActivity.this.setContentView(viewFlipper);
		            readMusicData();

				}else if(tag.equals("cancel")){
					SheetActivity.this.setContentView(viewFlipper);
		            readMusicData();

				}else {
					int number = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
					CheckBox checkBox = (CheckBox)v.findViewById(R.id.search_result_item_check);
					checkBox.setChecked(!checkBox.isChecked());
				}
			}
		};

		((Button)searchView.findViewById(R.id.search_result_addButton)).setOnClickListener(clickListener);
		((Button)searchView.findViewById(R.id.search_result_cancelButton)).setOnClickListener(clickListener);

        while( cursor.moveToNext() ){
        	View itemView = inflater.inflate(R.layout.search_result_item_view, null);

        	TextView titleView = (TextView)itemView.findViewById(R.id.search_result_item_title);
        	TextView artistView = (TextView)itemView.findViewById(R.id.search_result_item_artist);
        	TextView albumView = (TextView)itemView.findViewById(R.id.search_result_item_album);

        	titleView.setText(cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.TITLE )));
        	artistView.setText(cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST )));
        	albumView.setText(cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ALBUM )));
        	Uri uri = ContentUris.withAppendedId(
        			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        			Integer.parseInt(cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media._ID ))));

        	itemView.findViewById(R.id.search_result_item_check).setTag(uri.toString());

        	itemView.setOnClickListener(clickListener);
        	itemView.setTag("" + setCount);
        	setCount++;

        	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        			LinearLayout.LayoutParams.MATCH_PARENT,
        			LinearLayout.LayoutParams.WRAP_CONTENT);
        	params.setMargins(0, 30, 0, 0);

        	scrollLinear.addView(itemView, params);
        	itemViewList.add(itemView);
        }

        cursor.close();
        SheetActivity.this.setContentView(searchView);
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

		if(tag.equals("change")){
			if(changeFlag){
				viewFlipper.setInAnimation(inFromLeftAnimation);
				viewFlipper.setOutAnimation(outToRightAnimation);
				viewFlipper.showPrevious();
			}else {
				viewFlipper.setInAnimation(inFromRightAnimation);
				viewFlipper.setOutAnimation(outToLeftAnimation);
				viewFlipper.showNext();
			}

			deleteFlag = false;
			changeFlag = !changeFlag;
		}

		//再生ボタン処理
		else if(tag.equals("playBack")){
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

			}else {
				if(musicList.size() != 0){
					setLinearBg(false);
					selectedNumber = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
					setPlayList();
					setMusic();
					startMusic();
				}
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

        c.close();

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

        cursor.close();

		createDialog("削除しますか？", text, "delete", null, "cancel",
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

				            	 c.close();
				            }

				            if(list.equals(""))
				            	list = "  ";

				            String[] addData = new String[]{sheetName, "musicSequence", list};
				            sql.upDateEntry(db, "sheetData", "dataType = ?",
				            		new String[]{"musicSequence"}, addData);

				            if(selectedNumber >= musicPlayList.size())
				            	selectedNumber = 0;
				            readMusicData();

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
			//楽曲を再生していなければサービスを停止する
			if(mpService.isPauseMusic() || !mpService.isPlaying())
				mpService.shutdown();

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
