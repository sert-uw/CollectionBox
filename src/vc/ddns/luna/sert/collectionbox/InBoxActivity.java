package vc.ddns.luna.sert.collectionbox;

import java.util.StringTokenizer;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class InBoxActivity extends Activity implements OnClickListener,
	GestureDetector.OnGestureListener{
	private MySQLite		sql;	//SQLiteオブジェクト
	private SQLiteDatabase	db;		//データベースオブジェクト

	private GestureDetector	gestureDetector; //GestureDetectorオブジェクト

	private ViewFlipper 	viewFlipper;//アニメーション用Viewオブジェクト

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

		init();
		setAnimations();
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

		//レイアウトから読み込む
		TextView	boxNameView	= (TextView)findViewById(R.id.inBox_boxName);
		ImageView	boxImView	= (ImageView)findViewById(R.id.inBox_boxIm);
		((Button)findViewById(R.id.inBox_createButton)).setOnClickListener(this);

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

		LayoutInflater inflater = LayoutInflater.from(this);
		View inBoxLayoutView = inflater.inflate(R.layout.inbox_sheet_layout, null);

		viewFlipper.addView(inBoxLayoutView);
	}

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
