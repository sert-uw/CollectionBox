package vc.ddns.luna.sert.collectionbox;

import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private ImageView[] imView = new ImageView[6];
	private TextView[] textView = new TextView[6];
	private boolean deleteFlag;

	private MySQLite sql;
	private SQLiteDatabase db;

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
		findViewById(R.id.newButton).setOnClickListener(this);
		findViewById(R.id.deleteButton).setOnClickListener(this);

		for (int i = 0; i < 6; i++) {
			imView[i] = (ImageView) (findViewById((getResources()
					.getIdentifier("boxImage" + i, "id", this.getPackageName()))));
			textView[i] = (TextView) (findViewById((getResources()
					.getIdentifier("boxTitle" + i, "id", this.getPackageName()))));
		}

		//SQLiteオブジェクト及びデータベースオブジェクトの生成
		sql = new MySQLite(this);
		db = sql.getWritableDatabase();

		reSet();
	}

	//ボックス一覧の更新
	private void reSet() {
		String[] data = sql.searchByProject(db);
		StringTokenizer st;

		for (int i = 0; i < textView.length; i++) {
			if (i >= data.length) {
				textView[i].setText(" ");
				imView[i].setImageResource(R.drawable.dummy);
			} else {
				st = new StringTokenizer(data[i], ",");
				st.nextToken();
				textView[i].setText(st.nextToken());
				imView[i].setImageResource(R.drawable.no_image);
				imView[i].setOnClickListener(this);
				imView[i].setTag("imView" + i);
			}
		}
	}

	//クリックイベント処理
	@Override
	public void onClick(View v) {
		String tag = v.getTag().toString();

		if (tag.equals("new")) {
			//sql.createNewBox(db, "test", "  ");
			//reSet();
			LayoutInflater inflater = LayoutInflater.from(this);
			createDialog("New", inflater.inflate(R.layout.new_dialog, null),
					"create", "cansel", null, null);

		} else if (tag.equals("delete")) {
			deleteFlag = true;

		} else {
			if (tag.substring(0, 6).equals("imView") && deleteFlag) {
				sql.deleteEntry(db, "allBox", "title = ?",
						new String[] { textView[Integer.parseInt(
								tag.replaceAll("[^0-9]", ""))]
								.getText().toString() });
				reSet();
			}
		}
	}

	//ダイアログの表示
	private void createDialog(String title, View view,
			String ptext, String ntext,
			DialogInterface.OnClickListener plistener,
			DialogInterface.OnClickListener nlistener){
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		ad.setTitle(title);
		ad.setView(view);
		ad.setPositiveButton(ptext, plistener);
		ad.setNeutralButton(ntext, nlistener);
		ad.show();
	}

}
