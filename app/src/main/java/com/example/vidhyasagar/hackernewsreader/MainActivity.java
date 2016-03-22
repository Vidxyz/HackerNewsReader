package com.example.vidhyasagar.hackernewsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while(data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                articlesDB.execSQL("DELETE FROM articles");

                Log.i("JSON ARRAY LENGTH", Integer.toString(jsonArray.length()));

                for(int i = 0; i < 20; i++) {
                    String articleId = jsonArray.getString(i);
                    Log.i("Database result"+String.valueOf(i), articleId);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);

                    data = reader.read();
                    String articleInfo = "";

                    while(data != -1) {
                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    String articleTitle = jsonObject.getString("title");
                    String articleUrl;
                    try {
                        articleUrl = jsonObject.getString("url");
                    }
                    catch (JSONException e) {
                        articleUrl = "null";
                    }

//                    String articleContent = "";
////                    Getting actual content of url
//                    url = new URL(articleUrl);
//                    urlConnection = (HttpURLConnection) url.openConnection();
//                    in = urlConnection.getInputStream();
//                    reader = new InputStreamReader(in);
//
//                    data = reader.read();
//
//                    while(data != -1) {
//                        char current = (char) data;
//                        articleContent += current;
//                        data = reader.read();
//                    }

                    articleIds.add(Integer.valueOf(articleId));
                    articleTitles.put(Integer.valueOf(articleId), articleTitle);
                    articleUrls.put(Integer.valueOf(articleId), articleUrl);


                    //CLEANING DATA TO PREVENT ACCIDENts
                    String sql = "INSERT INTO articles (articleId, url, title) VALUES (?, ?, ?)";
                    SQLiteStatement statement = articlesDB.compileStatement(sql);
                    statement.bindString(1, articleId);
                    statement.bindString(2, articleUrl);
                    statement.bindString(3, articleTitle);
//                    statement.bindString(4, articleContent);

                    //Inserting into DATABASE
                    statement.execute();
                    Log.i("Database result", "Added one");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

    Map<Integer, String> articleUrls = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer, String>();
    ArrayList<Integer> articleIds = new ArrayList<Integer>();

    SQLiteDatabase articlesDB;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> urls = new ArrayList<String>();
    ArrayList<String> contents = new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("articleUrl", urls.get(position));
//                i.putExtra("articleContent", contents.get(position));
                startActivity(i);

            }
        });


        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articlesDB.execSQL(
                "CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
            //LISTVIEW UPDATE DONE IN ON POST EXECUTE
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateListView() {
        try {

            Cursor c = articlesDB.rawQuery("SELECT * FROM articles ORDER BY articleId DESC", null);
            c.moveToFirst();
            titles.clear();
            urls.clear();
            contents.clear();

            while (c != null) {
                titles.add(c.getString(c.getColumnIndex("title")));
                urls.add(c.getString(c.getColumnIndex("url")));
                if(c.moveToNext()) {
                    continue;
                }
                else {
                    break;
                }
            }

            arrayAdapter.notifyDataSetChanged();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
