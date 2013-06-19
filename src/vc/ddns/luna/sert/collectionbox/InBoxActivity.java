package vc.ddns.luna.sert.collectionbox;

import java.util.StringTokenizer;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

public class InBoxActivity extends Activity {
	private MySQLite		sql;	//SQLiteオブジェクト
	private SQLiteDatabase	db;		//データベースオブジェクト

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inbox_layout);

		init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	//初期化処理
	private void init(){
		String boxName = "";

		//MainActivityからデータを引き継ぐ
		Bundle extras = getIntent().getExtras();
		if(extras != null){
			 boxName = extras.getString("boxName");
		}

		TextView	boxNameView	= (TextView)findViewById(R.id.inBox_boxName);
		ImageView	boxImView	= (ImageView)findViewById(R.id.inBox_boxIm);

		//SQLiteオブジェクト及びデータベースオブジェクトの生成
		sql = new MySQLite(this);
		db = sql.getWritableDatabase();

		String[] data = sql.searchBoxByBoxName(db, boxName);

		StringTokenizer st = new StringTokenizer(data[0], ",");

		try{
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
}
