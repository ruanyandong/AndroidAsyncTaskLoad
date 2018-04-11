package com.example.ai.newsimooc;

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
