package com.example.ai.newsimooc;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;


public class MainActivity extends AppCompatActivity {


    private ListView mListView;

    private static final String
            URL="http://www.imooc.com/api/teacher?type=4&num=30";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView=findViewById(R.id.list_view);

        new NewsAsyncTask().execute(URL);



    }

    /**
     * 异步访问
     */
    private class NewsAsyncTask extends AsyncTask<String,String,List<NewsBean>>{

        @Override
        protected List<NewsBean> doInBackground(String... strings) {
            return getJsonData(strings[0]);
        }

        @Override
        protected void onPostExecute(List<NewsBean> newsBeans) {
            super.onPostExecute(newsBeans);

            NewsAdapter mAdapter=new NewsAdapter(MainActivity.this,newsBeans,mListView);
            mListView.setAdapter(mAdapter);
        }
    }


    /**
     * 将获取到的JSON数据封装为List
     * @param url
     * @return
     */
    private List<NewsBean> getJsonData(String url){
        List<NewsBean> newsBeanList=new ArrayList<>();
        try {

            String jsonString=readStream(new URL(url).openStream());
            JSONObject jsonObject=new JSONObject(jsonString);
            NewsBean newsBean;

            JSONArray jsonArray=jsonObject.getJSONArray("data");
            for(int i=0;i<jsonArray.length();i++){

                newsBean=new NewsBean();

                JSONObject jsonObject1=jsonArray.getJSONObject(i);

                newsBean.newsIconUrl=jsonObject1.getString("picSmall");
                newsBean.newsTitle=jsonObject1.getString("name");
                newsBean.newsContent=jsonObject1.getString("description");

                newsBeanList.add(newsBean);

            }

            Log.d("JSON数据", jsonString);

        } catch (IOException e) {

            e.printStackTrace();

        }catch(JSONException e){

            e.printStackTrace();
        }
        return newsBeanList;

    }

    /**
     * 读取网络数据
     * @param is
     * @return
     */
    private String readStream(InputStream is){

        InputStreamReader isr;
        BufferedReader br;
        String result="";

        try {
            isr=new InputStreamReader(is,"UTF-8");
            br=new BufferedReader(isr);
            String line;
            while((line=br.readLine())!=null){
                result+=line;
            }

        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }catch (IOException e){
        }
        return result;
    }

}
