package vc.ddns.luna.sert.collectionbox;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLite extends SQLiteOpenHelper {

	private static final String[] allBoxKeys = {"project", "title", "imPath"};
	private static final String[] boxKeys = {"category", "bgPath", "dataPath"};
	private 			 String[] useKeys;

	//コンストラクタ
	public MySQLite(Context context) {
		//任意のデータベースファイル名と、バージョンを指定する
		super(context, "collection.db", null, 1);

	}

	/**
	 * このデータベースを初めて使用するときに実行される処理
	 * テーブルの作成や初期データの投入を行う
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		//テーブルを作成
		db.execSQL(
				"create table allBox ("
						+ "_id integer primary key autoincrement not null, "
						+ "project text not null, "
						+ "title TEXT ," + "imPath TEXT)");

	}

	/**
	 * アプリケーションの更新などによって、
	 * データベースのバージョンが上がった場合の処理
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	//テーブルの新規作成
	public void createTable(SQLiteDatabase db, String tableName){
		db.execSQL("create table " + tableName + " ("
				+ "_id integer primary key autoincrement not null, "
				+ "category text not null, "
				+ "bgPath TEXT, " + "dataPath TEXT)");
	}

	//ボックス管理テーブルへの追加
	public void createNewBox(SQLiteDatabase db, String title, String imPath){
		ContentValues val = new ContentValues();

		val.put("project", "allBox");
		val.put("title", title);
		val.put("imPath", imPath);

		setEntry(db, val, "allBox");
	}

	//テーブルへ挿入
	private void setEntry(SQLiteDatabase db, ContentValues val, String tag){
		db.insert(tag, null, val);
	}

	//データを検索
	//ボックス一覧を取得
	public String[] searchAllBox(SQLiteDatabase db){
		useKeys = allBoxKeys;
		return searchByData(db, "allBox", "project = ?", new String[]{"allBox"});
	}

	public String[] searchBoxByTitle(SQLiteDatabase db, String title){
		useKeys = allBoxKeys;
		return searchByData(db, "allBox", "title = ?", new String[]{title});
	}

	//カテゴリのデータを取得
	public String[] searchByCategory(SQLiteDatabase db, String projectName,
			String category){
		useKeys = boxKeys;
		return searchByData(db, projectName, "category = ?", new String[]{category});
	}

	//指定されたテーブルからデータを検索
	private String[] searchByData(SQLiteDatabase db, String tableName,
			String searchStr, String[] searchValue){
		Cursor cursor = null;

		try{
			cursor = db.query(tableName, null, searchStr, searchValue,
					null, null, null);
			return readCursor(cursor);
		}finally{
			if(cursor != null)
				cursor.close();
		}
	}

	//検索結果から読み込み
	private String[] readCursor(Cursor cursor) {
		String[] result;

		int[] readId = new int[useKeys.length];
		int recordNum = cursor.getCount();
		int readNum = 0;

		result = new String[recordNum];

		for(int i=0; i<useKeys.length; i++){
			readId[i] = cursor.getColumnIndex(useKeys[i]);
		}

		if (readId.length != 0) {
			while (cursor.moveToNext()) {
				result[readNum] = "";
				//データを読み込み文字列にまとめる
				for (int i = 0; i < readId.length; i++) {
					result[readNum] += cursor.getString(readId[i]) + ",";
				}

				readNum++;
			}
		}
		return result;
	}

	//データ追加
	public void inputEntry(SQLiteDatabase db, String tableName, String[] data){
		useKeys = (tableName.equals("allBox"))? allBoxKeys : boxKeys;

		ContentValues val = new ContentValues();

		for(int i=0; i<useKeys.length; i++){
			val.put(useKeys[i], data[i]);
		}

		db.insert(tableName, null, val);
	}

	//データを削除する
	public void deleteEntry(SQLiteDatabase db, String tableName,
			String searchStr, String[] searchValue) {

		db.delete(tableName, searchStr, searchValue);
	}
}
