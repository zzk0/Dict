package cn.yummmy.dict;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.view.WindowManager;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

import cz.msebera.android.httpclient.Header;

public class DictService extends Service {

    class DictBinder extends Binder {
        // Query a word, show in dialog, add to word list
        public void queryWord(String word) {
            if (!word.equals(lastWord)) {
                String content = searchWordSql(word);
                showDialog(word, content);
                lastWord = word;
            }
        }

        // Query a word, return a message
        public String queryWordMessage(String word) {
            return searchWordSql(word);
        }
    }

    // Sql
    private SQLiteDatabase database;
    private SQLiteDatabase wordsDatabase;

    private String lastWord = null;
    private String cookieData = "";

    public DictService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + getResources().getString(R.string.database_path)
                + "/" + getResources().getString(R.string.database_name));
        database = SQLiteDatabase.openOrCreateDatabase(file, null);

        File file1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + getResources().getString(R.string.database_path)
                + "/" + getResources().getString(R.string.words_name));
        wordsDatabase = SQLiteDatabase.openOrCreateDatabase(file1, null);
        wordsDatabase.execSQL("create table if not exists words(" +
                "id integer primary key autoincrement not null," +
                "word char(50) not null);");
        getCookieData();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        database.close();
        wordsDatabase.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DictBinder();
    }

    private void showDialog(final String title, final String message) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title)
                .setMessage(message)
                .setPositiveButton("add", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        doOneSync(title);
                    }
                })
                .setNegativeButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }else {
            dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        }
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private String searchWordSql(String word) {
        Cursor cursor = database.rawQuery("select * from stardict where word = ?", new String[]{word});
        boolean result = cursor.moveToFirst();
        if (result) {
            String translation = cursor.getString(cursor.getColumnIndex("translation"));
            String pron = cursor.getString(cursor.getColumnIndex("phonetic"));
            return "[" + pron + "]\n" + translation;
        }
        else {
            return "";
        }
    }

    private void getCookieData() {
        SharedPreferences sharedPreferences = getSharedPreferences("account_info", 0);
        String mAuthToken = sharedPreferences.getString("auth_token", "");
        Long mUserId = sharedPreferences.getLong("user_id", 0);

        String csrftoken = "csrftoken=DGojASqnaJGWNPfOEg0n1RPZbPzCaA5l; ";
        String _ga = "_ga=GA1.2.779543603.1557886651; ";
        String __utmz = "__utmz=183787513.1558059702.5.2.utmcsr=web.shanbay.com|utmccn=(referral)|utmcmd=referral|utmcct=/web/account/login; ";
        String userId = "userid=" + mUserId.toString() + "; ";
        String __utma = "__utma=183787513.779543603.1557886651.1557886685.1557893669.2; ";
        String __utmc = "__utmc=183787513; ";
        String auth_token = "auth_token=" + mAuthToken;

        String __utmt = "__utmt=1; ";
        String __utmb = "__utmb=183787513.1.10.1558075417";

        cookieData = csrftoken + _ga + __utmz + userId + __utma + __utmc + auth_token;
    }

    private void doOneSync(String word) {
        String url = "https://www.shanbay.com/bdc/vocabulary/add/batch/?words=" + word;
        String path = "/bdc/vocabulary/add/batch/?words=" + word;

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader(":authority", "www.shanbay.com");
        client.addHeader(":method", "GET");
        client.addHeader(":path", path);
        client.addHeader(":scheme", "https");
        client.addHeader("accept", "application/json, text/javascript, */*; q=0.01");
        client.addHeader("accept-encoding", "gzip, deflate, br");
        client.addHeader("accept-language","zh-CN,zh;q=0.9,zh-TW;q=0.8,en;q=0.7");
        client.addHeader("cookie", cookieData);
        client.addHeader("Referer", "https://www.shanbay.com/bdc/vocabulary/add/batch/");
        client.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");
        client.addHeader("x-requested-with", "XMLHttpRequest");
        try {
            client.get(this, url, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    String json = new String(responseBody);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        JSONArray array = jsonObject.getJSONArray("notfound_words");
                        if (array.length() >= 1) {
                            String notFoundWord = (String) array.get(0);
                            wordsDatabase.execSQL("insert into words (word) values (?)", new Object[]{notFoundWord});
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    System.out.println();
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
