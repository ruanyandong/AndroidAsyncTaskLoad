package com.example.ai.newsimooc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;


public class ImageLoader {

    private ImageView mImageView;

    private String mUrl;

    /**
     *使用缓存，增加用户体验，缓解网络加载压力，以内存换效率
     * key-value的形式
     *
     * key:缓存对象的名字
     * value:缓存的对象
     *
     * LruCache底层通过LinkHashMap实现
     */
    //创建caches
    private LruCache<String,Bitmap> mCaches;

    private ListView mListView;
    private Set<NewsAsyncTask> mTask;

    public ImageLoader(ListView listView){

        this.mListView=listView;
        this.mTask=new HashSet<>();

        /**
         * 获取当前可用的最大内存
         */
        int maxMemory=(int)Runtime.getRuntime().maxMemory();
        /**
         * 缓存的大小
         */
        int cacheSize=maxMemory/4;

        mCaches=new LruCache<String,Bitmap>(cacheSize){
            /**
             * 这个方法默认返回元素的个数
             * 这里要返回每个存进去的对象的大小
             * @param key
             * @param value
             * @return
             */
            //这个方法在每次存入内存缓存时调用
            @Override
            protected int sizeOf(String key, Bitmap value) {
                /**
                 * 返回每个存入内存缓存的bitmap的大小
                 */
                return value.getByteCount();
            }
        };
    }

    /**
     * 把Bitmap加入缓存
     * @param url
     * @param bitmap
     */
    public void addBitmapToCache(String url,Bitmap bitmap){

        if(getBitmapFromCache(url)==null){
            mCaches.put(url,bitmap);
        }

    }

    /**
     * 把Bitmap从缓存中取出来
     */
    public Bitmap getBitmapFromCache(String url){
        return mCaches.get(url);
    }


    private Handler handler=new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bitmap bitmap=(Bitmap)msg.obj;

            /**
             * 让URL与指定的ImageView绑定，防止因为convertView的缓存而造成的图片加载混乱
             */
            if(mImageView.getTag().equals(mUrl)){

                mImageView.setImageBitmap(bitmap);

            }
        }

    };

    /**
     *===下载图片的方式一====>用多线程的方法加载图片
     * >>>>>>>>这个方法没用缓存<<<<<<<<<<
     * @param imageView
     * @param urlIcon
     */
    public void showImageByThread(ImageView imageView,String urlIcon){
        mImageView=imageView;
        mUrl=urlIcon;
        new Thread(new Runnable() {

            @Override
            public void run() {
                Bitmap bitmap=getBitmapFromURL(mUrl);

                /**
                 * 通过这种方式可以获得已经回收的Message，提高Message的使用效率
                 */
                Message message=Message.obtain();
                message.obj=bitmap;
                handler.sendMessage(message);
            }
        }).start();

    }

    /**
     * 获取图片从URL中
     * @param urlIcon
     * @return
     */
    public Bitmap getBitmapFromURL(String urlIcon){
        Bitmap bitmap;
        InputStream is=null;
        BufferedInputStream bis=null;
        try {
            URL url=new URL(urlIcon);

            HttpURLConnection connection=
                    (HttpURLConnection)url.openConnection();

            is=connection.getInputStream();
            bis=new BufferedInputStream(is);
            bitmap= BitmapFactory.decodeStream(bis);

            connection.disconnect();

            Thread.sleep(1000);

            is.close();
            bis.close();

            return bitmap;
        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch(IOException e){

        }catch(InterruptedException e){

        }
        return null;
    }

    /**
     *===下载图片的方式二=====>通过AsyncTask的方法加载图片
     * 》》》》》》这个方法用了缓存《《《《《《《
     * @param imageView
     * @param url
     */
    public void showImageByAsyncTask(ImageView imageView,String url){

        /**
         * =====判断缓存中是否有bitmap，有就直接使用,设置给imageView，没有再去下载
         */
        //从缓存中取出对应哪个图片
        Bitmap bitmap=getBitmapFromCache(url);
        if (bitmap==null){
            //如果缓存中没有对应图片，就去下载
            //new NewsAsyncTask(url).execute(url);
            imageView.setImageResource(R.mipmap.ic_launcher);
        }else {
            imageView.setImageBitmap(bitmap);
        }
    }

    private class NewsAsyncTask extends AsyncTask<String,String,Bitmap>{

        //private ImageView mImageView;
        private String url;

        public NewsAsyncTask(String url){
            //this.mImageView=imageView;
            this.url=url;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            /**
             * 第一次下载后就把Bitmap保存到缓存中
             */
            String url=strings[0];
            Bitmap bitmap=getBitmapFromURL(url);
            if(bitmap!=null){
                /**
                 * 将不在缓存中的图片加入缓存
                 */
                addBitmapToCache(url,bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            ImageView imageView=(ImageView)mListView.findViewWithTag(url);

            if(imageView!=null&&bitmap!=null){
                imageView.setImageBitmap(bitmap);
            }
            /**
             * 移除自己，因为任务已经执行完了
             */
            mTask.remove(this);

        }
    }

    /**
     * 加载listView停止滚动时的 可见起始项 到 可见终止项 之间的所有item
     * @param start
     * @param end
     */
    public void loadImages(int start,int end){
        for (int i = start; i < end; i++) {
            String url=NewsAdapter.URLS[i];

            //从缓存中取出对应哪个图片
            Bitmap bitmap=getBitmapFromCache(url);
            if (bitmap==null){
                //如果缓存中没有对应图片，就去下载
                NewsAsyncTask task=new NewsAsyncTask(url);
                task.execute(url);
                mTask.add(task);
            }else {
                ImageView imageView=(ImageView)mListView.findViewWithTag(url);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    /**
     * 取消所有任务
     */
    public void cancelAllTasks(){
        if (mTask!=null){
            for (NewsAsyncTask task:mTask) {
                /**
                 * cancel(true)，还是 cancel(false)，调用之后 isCancelled() 都返回 true
                 * AsyncTask 的 cancel 方法需要一个布尔值的参数，参数名为 mayInterruptIfRunning,
                 * 意思是如果正在执行是否可以打断,如果这个值设置为 true ，
                 * 表示这个任务可以被打断，否则，正在执行的程序会继续执行直到完成。
                 * 如果在 doInBackground()方法中有一个循环操作，
                 * 我们应该在循环中使用 isCancelled()来判断，如果返回为 true ，我们应该避免执行后续无用的循环操作。
                 */
                task.cancel(false);
            }
        }
    }
}
