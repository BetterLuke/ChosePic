package com.example.luke.chosepic.util;

import android.graphics.Bitmap;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;

/**
 * ͼƬ������
 * ����ģʽ
 * Created by LUKE on 2015/10/4.
 */
public class ImageLoader {

    private static ImageLoader mInstance;

    /**
     * ͼƬ����ĺ��Ķ���
     */
    private LruCache<String,Bitmap> mLruCache;

    /**
     *�̳߳�
     */
    private ExecutorService mThreadPool;
    private static final int DEAFULT_THREAD_COUNT = 1;
    /**
     * ���еĵ��ȷ�ʽ
     */
    private Type mType = Type.LIFO;
    /**
     * �������
     */
    private LinkedList<Runnable> mTaskQueue;

    /**
     * ��̨��ѯ�߳�
     */
    private Thread mPoolThread;
    private android.os.Handler mPoolThreadHandler;
    /**
     * UI�߳��е�Handler
     */
    private Handler mUIHandler;
    public enum Type{
        FIFO,LIFO;
    }



    private ImageLoader(int threadCount,Type type){
        init(threadCount, type);
    }

    /**
     * ��ʼ��ImageLoader
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        //��̨��ѯ�߳�
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new android.os.Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //�̳߳�ȥȡ��һ���������ִ��
                    }
                };
                Looper.loop();
            }
        };

        mPoolThread.start();//������̨��ѯ�߳�

        //��ȡӦ�õ��������ڴ�
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        //�����̳߳�
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;
    }

    /**
     * ��ȡImageLoaderʵ��
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

}
