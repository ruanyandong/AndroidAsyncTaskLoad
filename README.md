# Android异步加载
newsBean类：

  public class NewsBean {

    public String newsIconUrl;
    public String newsTitle;
    public String newsContent;

}

NewsAdapter适配器：

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
/**
 * Android异步加载
 * 总结
 * 通过异步加载，避免阻塞UI线程
 * 通过LruCache，将已经下载的图片放到内存中=========》也叫一级缓存
 * 通过判断ListView滑动状态，决定何时加载图片
 * 不仅仅是ListView，任何控件都可以使用异步加载
 */

/**
 * 适配器实现 AbsListView.OnScrollListener接口，监听listView的滚动
 * 让ListView滑动时取消所有加载项，滑动停止后才加载所有可见项，以此来提高listView的加载效率
 */
public class NewsAdapter extends BaseAdapter implements AbsListView.OnScrollListener{

    private List<NewsBean> mList;

    private LayoutInflater mInflater;

    private Context context;

    /**
     * >>>>>>>>>>记录LitView的可见item的起始项下标和可见终止项下标
     */
    private int mStart;
    private int mEnd;

    //保存当前获得的图片的URL地址
    public static String[] URLS;
    /**
     * 判断是不是第一次打开应用
     */
    private boolean mFirstIn;

    //用于使用缓存加载图片的ImageLoader，只能new一次
    private ImageLoader mImageLoader;

    public NewsAdapter(Context context, List<NewsBean> mList, ListView listView){
        this.context=context;
        this.mList=mList;
        mInflater=LayoutInflater.from(context);
        mImageLoader=new ImageLoader(listView);

        URLS=new String[mList.size()];
        for (int i = 0; i <mList.size();i++) {
            URLS[i]=mList.get(i).newsIconUrl;
        }

        /**
         * true代表第一次启动
         */
        mFirstIn=true;

        /**
         * 给listView绑定滚动监听事件
         */
        listView.setOnScrollListener(this);

    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * 在这里ListView存在缓存机制，可能导致图片加载混乱、跳动，而加载之前缓存在contentView里的图片
     * 解决方法：让URL与指定的ImageView绑定
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position,
                        View convertView,
                        ViewGroup parent) {
        ViewHolder viewHolder=null;
        if(convertView==null){

            viewHolder=new ViewHolder();
            convertView=mInflater.inflate(R.layout.item_layout,null);

            viewHolder.mImageView=convertView.findViewById(R.id.image_view);
            viewHolder.mTitle=convertView.findViewById(R.id.news_title);
            viewHolder.mContent=convertView.findViewById(R.id.news_content);

            convertView.setTag(viewHolder);
        }else{
            viewHolder=(ViewHolder)convertView.getTag();
        }
        viewHolder.mImageView.setImageResource(R.mipmap.ic_launcher);

        /**
         * 让URL与指定的ImageView绑定，防止因为convertView的缓存而造成的图片加载混乱
         */
        String url=mList.get(position).newsIconUrl;

        viewHolder.mImageView.setTag(url);

        /**
         *========>用多线程的方法加载图片
         * >>>>>>>======这里没有用缓存
         */
        //new ImageLoader().showImageByThread(viewHolder.mImageView, url);
        /**
         * <=========通过AsyncTask的方法加载图片
         * >>>>>>====  这里用了缓存
         * =====》用了缓存，ImageLoader不能每次都new一个，这样每次都会new一个LruCache对象，达不到缓存效果
         */
        mImageLoader.showImageByAsyncTask(viewHolder.mImageView,url);

        viewHolder.mTitle.setText(mList.get(position).newsTitle);
        viewHolder.mContent.setText(mList.get(position).newsContent);

        return convertView;
    }

    class ViewHolder{

        public TextView mTitle;

        public TextView mContent;

        public ImageView mImageView;

    }


    /**
     * listView状态切换时调用
     * @param view
     * @param scrollState 当前ListView的滚动状态
     *scrollState：即滑动的状态。分为三种 0，1，2

    =0 表示停止滑动的状态 SCROLL_STATE_IDLE

    =1表示正在滚动，用户手指在屏幕上 SCROLL_STATE_TOUCH_SCROLL

    =2表示正在滑动。用户手指已经离开屏幕 SCROLL_STATE_FLING

     * scrollState 回调顺序如下：
     *

    手指触屏拉动准备滚动，只触发一次   顺序: 1
    scrollState = SCROLL_STATE_TOUCH_SCROLL(1)：表示正在滚动。当屏幕滚动且用户使用的触碰或手指还在屏幕上时为1

    持续滚动开始，只触发一次     顺序: 2
    scrollState =SCROLL_STATE_FLING(2) ：表示手指做了抛的动作（手指离开屏幕前，用力滑了一下，屏幕产生惯性滑动）。

    整个滚动事件结束，只触发一次    顺序: 4
    scrollState =SCROLL_STATE_IDLE(0) ：表示屏幕已停止。屏幕停止滚动时为0。

     */
    //onScrollStateChanged方法在初始化时不会被调用，所以第一次打开应用，
    //不会加载网络图片，只会加载默认本地图片,所以设置一个tag，判断是否是第一次访问，mFirst,如果是就在onScroll方法中加载网络数据
    //第一次打开，listView默认状态没有改变
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //当前listView处于停止状态 idle：闲置的
        if(scrollState==SCROLL_STATE_IDLE){

            /**
             * 把图片从网络的加载控制权从getView方法转移到这里
             */
            mImageLoader.loadImages(mStart,mEnd);
            //加载数据可见项
        }else {
            //处于其他状态，停止任务
            mImageLoader.cancelAllTasks();
        }

    }

    /**
     * listView的整个滑动过程中都会调用
     * @param view
     * @param firstVisibleItem 当前窗口中能看见的第一个列表项ID
     * @param visibleItemCount 当前窗口中能看见的列表项的个数（小半个也算）
     * @param totalItemCount 列表项的总数
     */
    //一直在滚动中，多次触发  顺序: 3
    //这个方法一直会调用，打开listView就会调用
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        mStart=firstVisibleItem;
        mEnd=firstVisibleItem+visibleItemCount;

        /**
         * 判断是不是第一次启动,并且item绘制完成，即可见item大于0，就加载网络数据
         */
        if (mFirstIn && visibleItemCount>0){

            mImageLoader.loadImages(mStart,mEnd);
            //设置为false，表示不是第一次访问
            mFirstIn=false;
        }

    }
}

ImageLoader类：

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

