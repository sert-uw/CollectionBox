package vc.ddns.luna.sert.collectionbox;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MusicPlayerService extends Service{

	//サービス生成時に呼ばれる
	@Override
	public void onCreate(){
		super.onCreate();
	}

	//サービス開始時に呼ばれる
	public int onStartCommand(Intent intent, int flags, int startId){
		onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
