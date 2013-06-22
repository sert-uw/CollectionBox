package vc.ddns.luna.sert.collectionbox;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLite extends SQLiteOpenHelper {

	private static final String[] allBoxKeys	= {"project", "boxName", "imPath"};
	private static final String[] boxKeys		= {"boxName", "sheetName", "comment", "bgImPath"};
	private static final String[] sheetKeys		= {"sheetName", "dataType", "data"};
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
						+ "boxName TEXT ," + "imPath TEXT)");

		db.execSQL(
				"create table boxSheet ("
				+ "_id integer primary key autoincrement not null, "
				+ "boxName text not null, "
				+ "sheetName TEXT, " + "comment TEXT, " + "bgImPath TEXT)");

		db.execSQL(
				"create table sheetData ("
				+ "_id integer primary key autoincrement not null, "
				+ "sheetName text not null, "
				+ "dataType TEXT, " + "data TEXT)");
	}

	/**
	 * アプリケーションの更新などによって、
	 * データベースのバージョンが上がった場合の処理
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	//////////////////////
	////テーブルへ追加////
	//////////////////////
	//ボックス管理テーブルへの追加
	public void createNewBox(SQLiteDatabase db, String boxName, String imPath){
		ContentValues val = new ContentValues();

		val.put("project", "allBox");
		val.put("boxName", boxName);
		val.put("imPath", imPath);

		setEntry(db, val, "allBox");
	}

	//シートの作成
	public void createNewData(SQLiteDatabase db, String tableName, String[] data){
		ContentValues val = new ContentValues();

		if(tableName.equals("boxSheet"))
			useKeys = boxKeys;
		else if(tableName.equals("sheetData"))
			useKeys = sheetKeys;

		for(int i=0; i<useKeys.length; i++)
			val.put(useKeys[i], data[i]);

		setEntry(db, val, tableName);
	}

	//テーブルへ挿入
	private void setEntry(SQLiteDatabase db, ContentValues val, String tag){
		db.insert(tag, null, val);
	}

	////////////////////
	////データを検索////
	////////////////////
	//ボックス一覧を取得
	public String[] searchAllBox(SQLiteDatabase db){
		useKeys = allBoxKeys;
		return searchByData(db, "allBox", "project = ?", new String[]{"allBox"});
	}

	//ボックスの有無を確認
	public String[] searchBoxByBoxName(SQLiteDatabase db, String boxName){
		useKeys = allBoxKeys;
		return searchByData(db, "allBox", "boxName = ?", new String[]{boxName});
	}

	//ボックスのすべてのシートを取得
	public String[] searchSheetByBoxName(SQLiteDatabase db, String boxName){
		useKeys = boxKeys;
		return searchByData(db, "boxSheet", "boxName = ?", new String[]{boxName});
	}

	//シートのすべてのデータを取得
	public String[] searchDataBySheetNameAndType(SQLiteDatabase db, String sheetName, String dataType){
		useKeys = sheetKeys;

		String serchKey = "sheetName = ?";
		String[] serchValue;
		if(dataType != null){
			serchKey += " and dataType = ?";
			serchValue = new String[]{sheetName, dataType};
		}else
			serchValue = new String[]{sheetName};

		return searchByData(db, "sheetData", serchKey, serchValue);
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

	////////////////////////
	////データを削除する////
	////////////////////////
	public void deleteEntry(SQLiteDatabase db, String tableName,
			String searchStr, String[] searchValue) {

		db.delete(tableName, searchStr, searchValue);
	}
}
