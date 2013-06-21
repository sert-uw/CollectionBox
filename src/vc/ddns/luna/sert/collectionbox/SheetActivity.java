package vc.ddns.luna.sert.collectionbox;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewFlipper;

public class SheetActivity extends Activity implements GestureDetector.OnGestureListener{

	private GestureDetector	gestureDetector; //GestureDetectorオブジェクト
	private ViewFlipper 	viewFlipper;//アニメーション用Viewオブジェクト
	private LayoutInflater	inflater;//LayoutをViewとして取得する

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

		//MainActivityからデータを引き継ぐ
		Bundle extras = getIntent().getExtras();
		if(extras != null){
			 sheetName = extras.getString("sheetName");
		}

		//SQLiteオブジェクト及びデータベースオブジェクトの生成
		sql = new MySQLite(this);
		db = sql.getWritableDatabase();

		setAnimations();
		readData();
		setLayout();
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
	}

	//Layoutのセットを行う
	private void setLayout(){
		View musicView = inflater.inflate(R.layout.sheet_music_layout, null);
		viewFlipper.addView(musicView);
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
