package vc.ddns.luna.sert.collectionbox;

import java.io.IOException;

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

	private final IBinder iBinder = new ServiceLocalBinder();//Binderの生成
	private MediaPlayer mediaPlayer;//音楽再生用
	private boolean setFlag = false;//楽曲のセットが完了したかどうか
	private boolean stop = false;//ストップフラグ
	private SeekBar seekBar;//レイアウトのSeekBar

	//サービス生成時に呼ばれる
	@Override
	public void onCreate(){
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {

			}
		});
		super.onCreate();
	}

	//サービス開始時に呼ばれる
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	//サービス終了時に呼ばれる
	@Override
	public void onDestroy(){

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
	public void onRebind(Intent intent){

	}

	//サービスに接続するためのBinder
	public class ServiceLocalBinder extends Binder{
		//サービスの取得
		MusicPlayerService getService(){
			return MusicPlayerService.this;
		}
	}

	//SeekBarのセット
	public void setSeekBar(SeekBar seekbar){
		seekBar = seekbar;
		seekBar.setMax(mediaPlayer.getDuration());

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar paramSeekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar paramSeekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar paramSeekBar, int paramInt, boolean fromUser) {
				if(fromUser)
					mediaPlayer.seekTo(paramSeekBar.getProgress());
			}
		});

		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					if(mediaPlayer.isPlaying()){
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

	//楽曲のセット
	public void setMusic(Context context, Uri uri){
		if(mediaPlayer == null)
			return;

		try {
			mediaPlayer.setDataSource(context, uri);
			setFlag = true;

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//楽曲の再生
	public void startMusic(){
		if(setFlag){
			try{
				mediaPlayer.prepare();
				mediaPlayer.start();
			}catch(IllegalStateException e){
				e.printStackTrace();
			}catch(IOException e){
				e.printStackTrace();
			}
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

	//MediaPlayerをIdleに移行する
	public void moveToIdle(){
		mediaPlayer.reset();
		setFlag = false;
		if(seekBar != null)
			seekBar.setProgress(0);
	}
}
