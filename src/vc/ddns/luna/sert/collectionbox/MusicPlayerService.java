package vc.ddns.luna.sert.collectionbox;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MusicPlayerService extends Service{

	private final		IBinder iBinder = new ServiceLocalBinder();//Binderの生成
	private int			startId;//サービスID
	private MediaPlayer	mediaPlayer;//音楽再生用
	private boolean		setFlag = false;//楽曲のセットが完了したかどうか
	private boolean		pauseFlag = false;//一時停止しているかどうか
	private boolean		stop = false;//ストップフラグ
	private SeekBar		seekBar;//レイアウトのSeekBar
	private List<Uri>	playList = new ArrayList<Uri>();//プレイリスト
	private int			trackNumber;//再生しているトラックナンバー
	private Context		context;

	//サービス生成時に呼ばれる
	@Override
	public void onCreate(){
		super.onCreate();
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				if(!mediaPlayer.isLooping() && playList.size() != 0){
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

	//Contextのセット
	public void setContext(Context context){
		this.context = context;
	}

	//Activityへトラックナンバーを通知する
	public void sendTrackNumber(){
		SheetActivity activity = (SheetActivity)context;
		if(activity != null)
			activity.setTrackNumber(trackNumber);
	}

	//SeekBarのセット
	public void setSeekBar(SeekBar seekbar){
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

		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
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
		}).start();
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
			System.out.println(trackNumber);
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
			}catch (Exception e) {
				e.printStackTrace();
				mediaPlayer.reset();
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

	//次の楽曲へ
	public void nextMusic(){
		mediaPlayer.reset();
		trackNumber++;
		if(playList != null)
			if(trackNumber == playList.size())
				trackNumber = 0;

		System.out.println(trackNumber);
		sendTrackNumber();

		try {
			mediaPlayer.setDataSource(context, playList.get(trackNumber));
			mediaPlayer.prepare();
			mediaPlayer.start();

			if(seekBar != null){
				seekBar.setProgress(0);
				seekBar.setMax(mediaPlayer.getDuration());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//楽曲の停止
	public void stopMusic(){
		if(setFlag)
			mediaPlayer.stop();
	}

	//楽曲のループ設定
	public void setLooping(boolean flag){
		mediaPlayer.setLooping(flag);
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
	    if (mediaPlayer != null) {
	        mediaPlayer.reset();
	        mediaPlayer.release();
	        mediaPlayer = null;
	    }
	}
}
