package vc.ddns.luna.sert.collectionbox;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.ViewFlipper;

public class PictureActivity extends Activity implements GestureDetector.OnGestureListener{

	private String	sheetName;//シート名
	private String	boxName;//ボックス名
	private int		selectedNumber;//選択された画像の番号
	private int		location;//ViewFlipperの位置

	private ViewFlipper 	viewFlipper;//アニメーション用Viewオブジェクト

	private MySQLite		sql;	//SQLiteオブジェクト
	private SQLiteDatabase	db;		//データベースオブジェクト

	private GestureDetector	gestureDetector; //GestureDetectorオブジェクト
	private LayoutInflater	inflater;//LayoutをViewとして取得する

	private List<Uri>		pictureList = new ArrayList<Uri>();
	private ImageView[]		imageView = new ImageView[3];

	//各種アニメーションオブジェクト
		private Animation 	inFromRightAnimation;
		private Animation 	inFromLeftAnimation;
		private Animation 	outToRightAnimation;
		private Animation 	outToLeftAnimation;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sheet_main_layout);

		//SheetActivityからデータを引き継ぐ
		Bundle extras = getIntent().getExtras();
		if(extras != null){
			sheetName = extras.getString("sheetName");
			boxName = extras.getString("boxName");
			selectedNumber = extras.getInt("select");
		}

		//SQLiteオブジェクト及びデータベースオブジェクトの生成
		sql = new MySQLite(this);
		db = sql.getWritableDatabase();

		gestureDetector = new GestureDetector(getApplicationContext(), this);
		inflater = LayoutInflater.from(getApplicationContext());

		setAnimations();

		viewFlipper = (ViewFlipper)findViewById(R.id.sheet_viewFlipper);

		readData();
	}

	//バックキーが押されたときの処理
	@Override
	public boolean dispatchKeyEvent(KeyEvent e){
		if(e.getAction() == KeyEvent.ACTION_DOWN){
			if(e.getKeyCode() == KeyEvent.KEYCODE_BACK){
				changeActivity();
			}
		}
		return super.dispatchKeyEvent(e);
	}

    //Activity変更
    public void changeActivity(){
    	//インテントの生成
    	Intent intent = new Intent(getApplicationContext(),
    				vc.ddns.luna.sert.collectionbox.SheetActivity.class);

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

	@Override
	public void onDestroy(){
		super.onDestroy();
		destroyObjects();

		db.close();
		sql.close();
	}

	//オブジェクトの削除
	private void destroyObjects(){
		for(int i=0; i<imageView.length; i++){
			cleanupView(imageView[i]);
		}

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

    //データベースから読み取る
    private void readData(){
    	String[] pictureData = sql.searchDataBySheetNameAndType(db, sheetName, "picture");

    	for(int i=0; i<pictureData.length; i++){
			StringTokenizer st = new StringTokenizer(pictureData[i], ",");
			st.nextToken(); st.nextToken();
			pictureList.add(Uri.parse(st.nextToken()));
		}

    	for(int i=0; i<3; i++){
    		View pictureView = inflater.inflate(R.layout.picture_view_layout, null);
    		imageView[i] = (ImageView)pictureView.findViewById(R.id.picture_view_image);
    		viewFlipper.addView(pictureView);
    	}

    	viewFlipper.showNext();
    	location = 1;

    	setImageView();
    }

    //ImageViewに画像をセットする
    private void setImageView(){
    	int befLocation = location - 1;
    	int nextLocation = location + 1;

    	if(befLocation <= -1)
    		befLocation = imageView.length - 1;
    	if(nextLocation >= imageView.length)
    		nextLocation = 0;

    	if(selectedNumber - 1 == -1)
    		imageView[befLocation].setImageURI(pictureList.get(pictureList.size() - 1));
    	else
    		imageView[befLocation].setImageURI(pictureList.get(selectedNumber - 1));

    	imageView[location].setImageURI(pictureList.get(selectedNumber));

    	if(selectedNumber + 1 == pictureList.size())
    		imageView[nextLocation].setImageURI(pictureList.get(0));
    	else
    		imageView[nextLocation].setImageURI(pictureList.get(selectedNumber + 1));

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

				location--;
				if(location <= -1)
					location = imageView.length - 1;

				selectedNumber--;
				if(selectedNumber < 0){
					selectedNumber = pictureList.size() - 1;
				}

			}else{
				viewFlipper.setInAnimation(inFromRightAnimation);
				viewFlipper.setOutAnimation(outToLeftAnimation);
				viewFlipper.showNext();

				location++;
				if(location >= imageView.length)
					location = 0;

				selectedNumber++;
				if(selectedNumber >= pictureList.size()){
					selectedNumber = 0;
				}
			}

			setImageView();

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
