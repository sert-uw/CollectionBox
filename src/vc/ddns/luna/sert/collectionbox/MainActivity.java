package vc.ddns.luna.sert.collectionbox;

import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private static final int REQUEST_GALLERY = 0;		//他アプリからの返り値取得用
	private ImageView[]		imView = new ImageView[6];	//ボックスイメージ一覧
	private TextView[]		textView = new TextView[6];	//ボックス名一覧
	private boolean			deleteFlag;					//Deleteモードのフラグ
	private TextView		delNotif;					//Deleteモードの通知用View

	private TextView		pathView;					//new_dialogのTextView
	private FrameLayout		frame;						//main_layoutのFrameLayout

	private MySQLite		sql;						//SQLiteオブジェクト
	private SQLiteDatabase	db;							//データベースオブジェクト

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	//初期化処理
	private void init() {
		//ボタンにイベントリスナーを登録
		findViewById(R.id.newButton).setOnClickListener(this);
		findViewById(R.id.deleteButton).setOnClickListener(this);

		//Deleteモード通知用Viewの取得
		delNotif = (TextView)findViewById(R.id.delete_notification);

		//FrameLayoutの取得
		frame = (FrameLayout)findViewById(R.id.main_frame);

		//ImageViewとTextViewのオブジェクトを取得
		for (int i = 0; i < 6; i++) {
			imView[i] = (ImageView) (findViewById((getResources()
					.getIdentifier("boxImage" + i, "id", this.getPackageName()))));
			textView[i] = (TextView) (findViewById((getResources()
					.getIdentifier("boxTitle" + i, "id", this.getPackageName()))));
		}

		//SQLiteオブジェクト及びデータベースオブジェクトの生成
		sql = new MySQLite(this);
		db = sql.getWritableDatabase();

		//レイアウトへ表示させる
		reSet();
	}

	//ボックス一覧の更新
	private void reSet() {
		String[] data = sql.searchAllBox(db);
		StringTokenizer st;

		try{
			//読み込んだデータを適応する
			for (int i = 0; i < textView.length; i++) {
				//データが空ならダミー(透明画像、スペース文字)を表示する
				if (i >= data.length) {
					textView[i].setText(" ");
					imView[i].setImageResource(R.drawable.dummy);
				} else {
					//データからボックス名とイメージ画像を読み込む
					st = new StringTokenizer(data[i], ",");
					st.nextToken();
					textView[i].setText(st.nextToken());
					String imPath = st.nextToken();
					if(imPath.equals("  "))
						imView[i].setImageResource(R.drawable.no_image);
					else
						imView[i].setImageURI(Uri.parse(imPath));

					imView[i].setOnClickListener(this);
					imView[i].setTag("imView" + i);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
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

	//クリックイベント処理
	@Override
	public void onClick(View v) {
		String tag = v.getTag().toString();

		if (tag.equals("new")) {
			//Deleteモードならば解除する
			if(deleteFlag)
				changeDelNotif();

			//new_dialog用のレイアウトを読み込む
			LayoutInflater inflater = LayoutInflater.from(this);

			//layoutをviewに変換する
			View layoutView = inflater.inflate(R.layout.new_dialog, null);

			//ViewをIDで読み込み、クリックイベントで参照できるようにfinal修飾子をつける
			final EditText edit = (EditText)layoutView.findViewById(R.id.new_dialog_edit);
			pathView = (TextView)layoutView.findViewById(
					R.id.new_dialog_image_load_path);

			//new_dialogのボタンにイベントリスナーを適応
			((Button)layoutView.findViewById(R.id.new_dialog_image_load_button))
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

			//ダイアログの生成
			createDialog("New", layoutView,
					"Create", "Cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//Positiveボタンが押された場合
							if (which == DialogInterface.BUTTON_POSITIVE) {

								//EditTextの文字列取得
								String str = edit.getText().toString();
								if(!str.equals("")){

									//image_edit_layoutをViewで取得
									if(!pathView.getText().toString().equals("")){
										LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
										View imEditView = inflater.inflate(R.layout.image_edit_layout, null);

										//ImageEditオブジェクトを生成しRelativeLayoutへ追加
										ImageEdit imEdit = new ImageEdit(MainActivity.this,
												str, pathView.getText().toString());
										imEdit.setLayout(frame, imEditView);
									}else
										addDataToDB(str, "  ");
								}
							}
						}
			});

		} else if (tag.equals("delete")) {
			//Deleteモードの変更
			changeDelNotif();

		} else {
			//ImageViewのクリック処理
			if (tag.substring(0, 6).equals("imView") && deleteFlag) {

				//Dialogに表示するTextViewの生成
				TextView text = new TextView(this);

				//Dialogのクリックイベントから参照するボックス名を保持
				final String title = textView[Integer.parseInt(
						tag.replaceAll("[^0-9]", ""))].getText().toString();

				//TextViewのパラメータ決定
				text.setText(title + "を削除しますか？");

				//ダイアログの生成
				createDialog("Delete", text, "Delete", "Cancel",
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {

							//データベースから削除する
							sql.deleteEntry(db, "allBox", "title = ?",
									new String[] { title });
							reSet();
						}
						//最後にDeleteモードを解除する
						changeDelNotif();
					}
				});
			}
		}
	}

	//データベースへの追加
	public void addDataToDB(String boxName, String imPath){
		//同じタイトルがなければデータベースへ登録
		if(sql.searchBoxByTitle(db, boxName).length == 0){
			sql.createNewBox(db, boxName, imPath);
			reSet();
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

	//Deleteモード通知TextViewの更新
	private void changeDelNotif(){
		//フラグのONと通知設定
		if(!deleteFlag){
			deleteFlag = true;
			delNotif.setText("Touch and delete");
		}
		//フラグのOFFと通知解除
		else {
			deleteFlag = false;
			delNotif.setText("");
		}
	}
}
