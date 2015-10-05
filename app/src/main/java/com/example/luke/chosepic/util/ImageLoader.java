package com.example.luke.chosepic.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

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

    /**
     * 后台轮询线程的信号量，并设置许可数为0
     */
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);

    /**
     * 线程池的信号量，许可值在init（）类初始化
     */
    private Semaphore mSemaphoreThreadPool;

    /**
     * 自定义的两种枚举类型
     */
    public enum Type{
        FIFO,LIFO;
    }



    private ImageLoader(int threadCount,Type type){
        init(threadCount, type);
    }

    /**
     * 初始化ImageLoader
     * @param threadCount 线程数
     * @param type 加载策略
     */
    private void init(int threadCount, Type type) {
        //初始化后台轮询线程
        mPoolThread = new Thread(){
            @Override
            public void run() {

                Looper.prepare();

                mPoolThreadHandler = new android.os.Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池去取出一个任务进行执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //mPoolThreadHandler完成初始化，已不为null，即释放一个信号量，增加了一个许可
                mSemaphorePoolThreadHandler.release();

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

        //初始化线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        //初始化任务队列
        mTaskQueue = new LinkedList<Runnable>();

        mType = type;

        //初始化线程池信号量的许可值
        mSemaphoreThreadPool = new Semaphore(threadCount);
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
    public void loadImage(final String path, final ImageView imageView){
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

        //根据Path在缓存中获取对应的bitmap
        Bitmap bm = getBitmapFromLruCache(path);

        //加载图片的显示
        if (bm != null){
            refreshBitmap(path, imageView, bm);
        }else{
            //向任务队列增加一个任务
            addTask(new Runnable(){
                @Override
                public void run() {
                    //任务是要加载图片，就要先对图片进行压缩
                    //图片的压缩:
                    //   1.获得图片需要显示的大小
                   ImageSize imageSize = getImageViewSize(imageView);
                    //   2.压缩图片
                    Bitmap bm = decodeSampledBitmapFromPath(path,imageSize.width,imageSize.height);
                    //   3.把图片加入到缓存
                    addBitmapToLruCache(path,bm);
                    refreshBitmap(path,imageView,bm);

                    mSemaphoreThreadPool.release();//每增加一个任务，就释放一个信号量
                }
            });
        }
    }

    /**
     * 加载图片的显示
     * @param path
     * @param imageView
     * @param bm
     */
    private void refreshBitmap(String path, ImageView imageView, Bitmap bm) {
        Message message = Message.obtain();
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.path = path;
        holder.bitmap = bm;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    /**
     * 将图片加入LruCache
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null){
            mLruCache.put(path,bm);
        }
    }

    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        //获得图片的宽和高,并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);

        options.inSampleSize = caculateInSampleSize(options,width,height);

        //使用获得到的InSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);

        return bitmap;
    }

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     * @param options
     * @param reqwidth
     * @param reqheight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqwidth, int reqheight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;

        if (width > reqwidth || height > reqheight){
            int widthRadio = Math.round(width * 1.0f / reqwidth);
            int heightRadio = Math.round(height * 1.0f / reqheight);

            inSampleSize = Math.min(widthRadio,heightRadio);//采用min()是想保持比例失真率最小
        }
        return inSampleSize;
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
            //width = imageView.getMaxWidth();//检查最大值，以应对wrap-content,fill-parent情况
            width = getImageViewFieldValue(imageView,"maxWidth");//检查最大值
        }
        if (width <=0){
            width = displayMetrics.widthPixels;//设置宽度为屏幕的宽度
        }

        int height = imageView.getHeight();
        if (height <= 0){
            height = lp.height;//获取imageView在layout中声明的高度
        }
        if (height <= 0){
            //height = imageView.getMaxHeight();//检查最大值，以应对wrap-content,fill-parent情况
            height = getImageViewFieldValue(imageView,"maxHeight");//检查最大值
        }
        if (height <=0){
            height = displayMetrics.heightPixels;//设置高度为屏幕的高度
        }

        imageSize.width = width;
        imageSize.height = height;

        return  imageSize;
    }

    /**
     * 头或反射获取imageview的某个属性值
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object,String fieldName){
        int value = 0;

        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE){
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return value;
    }

    /**
     * 图片尺寸大小包装类
     */
    public class ImageSize{
        int width;
        int height;
    }

    //向TaskQueue里添加一个Task,加同步
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);//向线程池中增加Task,也是要addTask()方法声明synchronized同步的原因

        //此处若mPoolThreadHandler没有完成初始化，线程可能会发生异常（空指针），所以需要确保
        //mPoolThreadHandler在调用前不为null,这里使用线程信号量机制控制mPoolThreadHandler的初始化
        //在被调用前执行,loadImage()方法内有对addTask()方法的调用

        try {
            if (mPoolThreadHandler == null) {
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
