package vc.ddns.luna.sert.collectionbox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MusicPlayerService extends Service{

	private final		IBinder iBinder = new ServiceLocalBinder();//Binderの生成
	private int			startId;//サービスID
	private MediaPlayer	mediaPlayer;//音楽再生用
	private boolean		setFlag = false;//楽曲のセットが完了したかどうか
	private boolean		pauseFlag = false;//一時停止しているかどうか
	private boolean		repeatFlag = false;//リピートフラグ
	private boolean		stop = false;//ストップフラグ
	private SeekBar		seekBar;//レイアウトのSeekBar
	private List<Uri>	playList = new ArrayList<Uri>();//プレイリスト
	private int[]		shuffleList;//シャッフルプレイリスト保持
	private boolean		shuffleFlag;//シャッフルモードかどうか
	private int			trackNumber;//再生しているトラックナンバー
	private int			beforeChangeNumber;//シャッフルのON/OFFが行われた瞬間のトラックナンバー
	private Context		context;
	private String		boxName = null;
	private String		sheetName = null;
	private boolean		seekLoopFlag;//シークバー制御スレッド

	//サービス生成時に呼ばれる
	@Override
	public void onCreate(){
		super.onCreate();
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				if(!repeatFlag && playList.size() != 0){
					if(beforeChangeNumber != -1)
						beforeChangeNumber = -1;
					nextMusic();
				}
			}
		});
	}

	//サービス開始時に呼ばれる
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		this.startId = startId;
		return START_STICKY;
	}

	//サービス終了時に呼ばれる
	@Override
	public void onDestroy(){
		shutdown();
		super.onDestroy();
	}

	//バインド(接続時)に呼ばれる
	@Override
	public IBinder onBind(Intent intent) {
		return iBinder;
	}

	//バインド解除(切断時)に呼ばれる
	@Override
	public boolean onUnbind(Intent intent){
		seekLoopFlag = false;
		return true;
	}

	//再バインド(再接続時)に呼ばれる
	@Override
	public void onRebind(Intent intent){}

	//サービスに接続するためのBinder
	public class ServiceLocalBinder extends Binder{
		//サービスの取得
		MusicPlayerService getService(){
			return MusicPlayerService.this;
		}
	}

	//boxName, sheetNameのセットすでにセットされていればfalseを返す
	public boolean setNames(String box, String sheet){
		if(boxName != null && sheetName != null)
			if(!boxName.equals(box) || !sheetName.equals(sheet))
				return false;

		boxName = box;
		sheetName = sheet;
		return true;
	}

	//boxName, sheetNameを返す
	public String[] getNames(){
		return new String[]{boxName, sheetName};
	}

	//Contextのセット
	public void setContext(Context context){
		this.context = context;
	}

	//Activityへトラックナンバーを通知する
	public void sendTrackNumber(){
		SheetActivity activity = (SheetActivity)context;
		if(activity != null){
			if(beforeChangeNumber != -1)
				activity.setTrackNumber(beforeChangeNumber);
			else {
				if(shuffleFlag)
					activity.setTrackNumber(shuffleList[trackNumber]);
				else
					activity.setTrackNumber(trackNumber);
			}
		}
	}

	// ノーティフィケーションの表示
	private void showNotification() {

		//情報取得
		Uri uri;
		if(shuffleFlag)
			uri = playList.get(shuffleList[trackNumber]);
		else
			uri = playList.get(trackNumber);

		String[] detail = getMusicDetail(MediaStore.Audio.Media.DISPLAY_NAME + " = ?",
				new String[]{getFileName(uri)});

		// ノティフィケーションオブジェクトの生成
		Notification notif = new Notification(
				R.drawable.ic_launcher,
				detail[0],
				System.currentTimeMillis());

		Intent intent = new Intent(context, SheetActivity.class);
		//インテントへパラメータ追加
		intent.putExtra("sheetName", sheetName);
		intent.putExtra("boxName", boxName);

		PendingIntent pIntent = PendingIntent.getActivity(context, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		notif.setLatestEventInfo(this, detail[0], detail[1], pIntent);

		//ノーティフィケーションが消えないようにする
		notif.flags = Notification.FLAG_NO_CLEAR;

		// ノーティフィケーションマネージャの取得
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		// ノーティフィケーションのキャンセル
		nm.cancel(0);

		// ノーティフィケーションの表示
		nm.notify(0, notif);
	}

	//ファイル名の取得
	private String getFileName(Uri uri){
		ContentResolver cr = context.getContentResolver();
        String[] columns = {MediaStore.Images.Media.DATA };
        Cursor c = cr.query(uri, columns, null, null, null);

        c.moveToFirst();
        File file = new File(c.getString(0));

        return file.getName();
	}

	//楽曲情報の取得
	private String[] getMusicDetail(String searchKey, String[] searchValue){
		ContentResolver resolver = context.getContentResolver();

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

        String[] detail = new String[3];//タイトル、アーティスト、アルバムの順
        if(cursor.moveToNext()){
         	detail[0] = (cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.TITLE )));
         	detail[1] = (cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST )));
         	detail[2] = (cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ALBUM )));
        }

        return detail;
	}

	//SeekBarのセット
	public void setSeekBar(SeekBar seekbar){
		if(seekbar == null)
			return;

		seekBar = seekbar;
		seekBar.setMax(mediaPlayer.getDuration());

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar paramSeekBar) {}

			@Override
			public void onStartTrackingTouch(SeekBar paramSeekBar) {}

			@Override
			public void onProgressChanged(SeekBar paramSeekBar, int paramInt, boolean fromUser) {
				if(fromUser)
					movePosition(paramSeekBar.getProgress());
			}
		});

		seekLoopFlag = true;

		/*new Thread(new Runnable() {
			@Override
			public void run() {
				while(seekLoopFlag){
					if(mediaPlayer == null)
						continue;

					if(mediaPlayer.isPlaying() && !pauseFlag){
						if(seekBar == null)
							continue;

						seekBar.setProgress(mediaPlayer.getCurrentPosition());
						try{
							Thread.sleep(1000);
						}catch(InterruptedException e){
							e.printStackTrace();
						}
					}
				}
			}
		}).start();*/
	}

	//SeekBarの有無
	public boolean isSetSeekBar(){
		return (seekBar == null)? false : true;
	}

	//楽曲のセット
	public void setMusic(Uri uri){
		if(mediaPlayer == null)
			return;

		try {
			mediaPlayer.setDataSource(context, uri);
			setFlag = true;

			trackNumber = playList.indexOf(uri);
			if(trackNumber == -1) trackNumber = 0;

		} catch (Exception e) {
			e.printStackTrace();
			mediaPlayer.reset();
		}
	}

	//プレイリストのセット
	public void setPlayList(List<Uri> list){
		playList = list;
	}

	//楽曲の再生
	public void startMusic(){
		if(setFlag){
			try{
				if(!pauseFlag)
					mediaPlayer.prepare();

				if(seekBar != null){
					seekBar.setProgress(0);
					seekBar.setMax(mediaPlayer.getDuration());
				}

				mediaPlayer.start();
				pauseFlag = false;

				showNotification();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	//楽曲の一時停止
	public void pauseMusic(){
		if(setFlag){
			mediaPlayer.pause();
			pauseFlag = true;
		}
	}

	//巻き戻し
	public void rewinding(){
		if(mediaPlayer == null)
			return;

		int sec = 0;

		if(mediaPlayer.isPlaying())
			sec = mediaPlayer.getCurrentPosition() / 1000;

		if(sec < 10){
			mediaPlayer.reset();
			beforeChangeNumber = -1;
			trackNumber--;
			if(trackNumber < 0){
				if(playList != null)
					trackNumber = playList.size() - 1;

				if(trackNumber < 0)
					trackNumber = 0;
			}

			sendTrackNumber();

			mediaPlayer.setLooping(repeatFlag);

			try {
				if(shuffleFlag)
					mediaPlayer.setDataSource(context, playList.get(shuffleList[trackNumber]));
				else
					mediaPlayer.setDataSource(context, playList.get(trackNumber));
			} catch (Exception e) {
				e.printStackTrace();
			}

			startMusic();
		}else
			this.movePosition(0);
	}

	//次の楽曲へ
	public void nextMusic(){
		mediaPlayer.reset();
		beforeChangeNumber = -1;
		trackNumber++;
		if(playList != null)
			if(trackNumber == playList.size())
				trackNumber = 0;

		sendTrackNumber();

		mediaPlayer.setLooping(repeatFlag);

		try {
			if(shuffleFlag)
				mediaPlayer.setDataSource(context, playList.get(shuffleList[trackNumber]));
			else
				mediaPlayer.setDataSource(context, playList.get(trackNumber));
		} catch (Exception e) {
			e.printStackTrace();
		}

		startMusic();
	}

	//楽曲の停止
	public void stopMusic(){
		if(setFlag)
			mediaPlayer.stop();
	}

	//楽曲が再生されているかどうか
	public boolean isPlaying(){
		return mediaPlayer.isPlaying();
	}

	//シャッフルの設定
	public void setShuffle(boolean flag){
		shuffleFlag = flag;

		if(shuffleFlag){
			beforeChangeNumber = trackNumber;

			boolean[] countFlag = new boolean[playList.size()];
			shuffleList = new int[playList.size()];
			int count = 0;
			Random rnd = new Random();
			while(true){
				int number = rnd.nextInt(playList.size());
				if(countFlag[number])
					continue;

				countFlag[number] = true;
				shuffleList[count] = number;
				count++;

				if(count == countFlag.length)
					break;
			}
		}else {
			if(beforeChangeNumber != -1)
				trackNumber = beforeChangeNumber;
			else
				trackNumber = shuffleList[trackNumber];
		}
	}

	//シャッフルの設定取得
	public boolean isShuffle(){
		return shuffleFlag;
	}

	//楽曲のループ設定
	public void setLooping(boolean flag){
		mediaPlayer.setLooping(flag);
		repeatFlag = flag;
	}

	//楽曲のループ設定取得
	public boolean isLooping(){
		return mediaPlayer.isLooping();
	}

	//楽曲がセットされているか
	public boolean isSetMusic(){
		return setFlag;
	}

	//プレイリストがセットされているか
	public boolean isSetPlayList(){
		return (playList.size() == 0)? false : true;
	}

	//MediaPlayerをIdleに移行する
	public void moveToIdle(){
		mediaPlayer.reset();
		setFlag = false;
		pauseFlag = false;
		if(seekBar != null)
			seekBar.setProgress(0);
	}

	//MediaPlayerの再生位置を変更する
	public void movePosition(int pos){
		mediaPlayer.seekTo(pos);
		seekBar.setProgress(pos);
	}

	//終了処理
	public void shutdown(){
		seekLoopFlag = false;

		if (mediaPlayer != null) {
	        mediaPlayer.reset();
	        mediaPlayer.release();
	        mediaPlayer = null;
	    }

	    this.stopSelf(startId);

		// ノーティフィケーションマネージャの取得
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// ノーティフィケーションのキャンセル
		nm.cancel(0);
	}
}
