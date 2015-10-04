package com.example.luke.chosepic.util;

import android.graphics.Bitmap;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图片加载类
 * 单例模式
 * Created by LUKE on 2015/10/4.
 */
public class ImageLoader {

    private static ImageLoader mInstance;

    /**
     * 图片缓存的核心对象
     */
    private LruCache<String,Bitmap> mLruCache;

    /**
     *线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEAFULT_THREAD_COUNT = 1;
    /**
     * 队列的调度方式
     */
    private Type mType = Type.LIFO;
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;

    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private android.os.Handler mPoolThreadHandler;
    /**
     * UI线程中的Handler
     */
    private android.os.Handler mUIHandler;
    public enum Type{
        FIFO,LIFO;
    }



    private ImageLoader(int threadCount,Type type){
        init(threadCount, type);
    }

    /**
     * 初始化ImageLoader
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        //后台轮询线程
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new android.os.Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池去取出一个任务进行执行
                        mThreadPool.execute(getTask());
                    }
                };
                Looper.loop();
            }
        };
        mPoolThread.start();//启动后台轮询线程

        //获取应用的最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;
    }

    /**
     * 从任务队列取出一个方法
     * @return
     */
    private Runnable getTask() {
        if (mType == Type.FIFO){
            return mTaskQueue.removeFirst();
        }else if(mType == Type.LIFO){
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * 获取ImageLoader实例
     * @return
     */
    private static ImageLoader getInstance(){
        if (mInstance == null){
            synchronized (ImageLoader.class){
                if (mInstance == null){
                    mInstance = new ImageLoader(DEAFULT_THREAD_COUNT,Type.FIFO);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据Path为imageView设置图片
     * @param path
     * @param imageView
     */
    public void loadImage(String path, final ImageView imageView){
        imageView.setTag(path);
        if (mUIHandler == null){
            mUIHandler = new android.os.Handler(){
                @Override
                public void handleMessage(Message msg) {
                    //获取得到的图片，为imageView回调设置图片
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    String path = holder.path;
                    ImageView imageView = holder.imageView;
                    //将path与getTag存储路径比较
                    if (imageView.getTag().toString().equals(path)){
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }

        //根据Path在缓存中获取bitmap
        Bitmap bm = getBitmapFromLruCache(path);

        //新建message，内含有holder对象，发送message给mUIHolder
        if (bm != null){
            Message message = Message.obtain();
            ImgBeanHolder holder = new ImgBeanHolder();
            holder.path = path;
            holder.bitmap = bm;
            holder.imageView = imageView;
            message.obj = holder;
            mUIHandler.sendMessage(message);
        }else{
            addTask(new Runnable(){
                @Override
                public void run() {
                    //加载图片
                    //图片的压缩: 1.获得图片需要显示的大小
                    //
                   ImageSize imageSize = getImageViewSize(imageView);
                }
            });
        }
    }

    /**
     * 根据ImageView获取尺寸来适当的压缩宽和高
     * @param imageView
     * @return
     */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics =imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();

        int width = imageView.getWidth();
        if (width <= 0){
            width = lp.width;//获取imageView在layout中声明的宽度
        }
        if (width <= 0){
            width = imageView.getMaxWidth();//检查最大值，以应对wrap-content,fill-parent情况
        }
        if (width <=0){
            width = displayMetrics.widthPixels;//设置宽度为屏幕的宽度
        }

        int height = imageView.getHeight();
        if (height <= 0){
            height = lp.height;//获取imageView在layout中声明的高度
        }
        if (height <= 0){
            height = imageView.getMaxHeight();//检查最大值，以应对wrap-content,fill-parent情况
        }
        if (height <=0){
            height = displayMetrics.heightPixels;//设置高度为屏幕的高度
        }

        imageSize.width = width;
        imageSize.height = height;

        return  imageSize;
    }

    /**
     * 图片尺寸大小包装类
     */
    public class ImageSize{
        int width;
        int height;
    }

    //向TaskQueue里添加一个Task
    private void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 根据Path在缓存中获取bitmap
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    /**
     *封装持有当前的bitmap,imageView,path
     */
   private class ImgBeanHolder{
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }


}
