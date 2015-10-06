package com.example.luke.chosepic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luke.chosepic.bean.FolderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends Activity {

    private static final int DATA_LOADED = 0x110;

    private GridView mGridView;
    private List<String> mImgs;//mGridView的Adapter的数据集

    private RelativeLayout mBottomly;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount = Integer.MIN_VALUE;

    //initData(）扫描完成后，集里边的FolderBean均会附上值，用于初始化popuewindow
    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

    private ProgressDialog mProgressDialog ;//initDatas()显示扫描图片的进度

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED){
                mProgressDialog.dismiss();//关闭进度提醒

                dataToView();
            }

        }
    };

    private void dataToView() {
        if (mCurrentDir == null){
            Toast.makeText(this,"未扫描到任何图片",Toast.LENGTH_SHORT).show();
            return;
        }

        mImgs = Arrays.asList(mCurrentDir.list());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initDatas();
        initEvent();
    }

    /**
     * 初始化组件
     */
    private void initView() {
        mGridView = (GridView) findViewById(R.id.id_gridView);
        mBottomly = (RelativeLayout) findViewById(R.id.id_bottom_ly);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
    }

    /**
     * 利用ContentProvider扫描手机中的所有图片
     */
    private void initDatas() {
        //检查外部储存空间是否可用，若不可用，直接返回以终止
        if (!  Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Toast.makeText(this,"当前储存卡不可用！",Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this,null,"正在加载...");

        new Thread(){
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();

                Cursor cursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + "= ? or" + MediaStore.Images.Media.MIME_TYPE + "= ?",
                        new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);

                //存放已经遍历过的文件夹的名称
                Set<String> mDirPaths = new HashSet<String>();

                if (cursor != null) {
                    while (cursor.moveToNext()){
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));//获取图片的Path
                        File parentFile = new File(path).getParentFile();//获取图片所在的文件夹对象
                        if (parentFile == null){
                            //防止部分情况无法获取图片的所在文件夹对象，即结束本次循环
                            continue;
                        }
                        String dirPath = parentFile.getAbsolutePath();
                        FolderBean folderBean = null;

                        if (mDirPaths.contains(dirPath)){
                            //若mDirPaths中已经存在要遍历的文件夹名称了，说明此文件夹已经遍历过了，遂结束本次循环
                            continue;
                        } else {
                            //遍历该文件夹，并进行标记，把该文件夹信息封装到folderBean中
                            mDirPaths.add(dirPath);

                            folderBean = new FolderBean();
                            folderBean.setDir(dirPath);
                            folderBean.setFirstImgPath(path);
                        }
                        if (parentFile.list() == null){
                            //对此这样的判断虽然很无解，估计是为了防止人品不够吧
                            continue;
                        }

                        //获取当前文件夹图片数量，并传给folderBean
                        folderBean.setCount(parentFile.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File file, String filename) {
                                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")){
                                    return true;
                                }
                                    return false;
                            }
                        }).length);

                        mFolderBeans.add(folderBean);

                        if (folderBean.getCount() > mMaxCount){
                            mMaxCount = folderBean.getCount();
                            mCurrentDir = parentFile;
                        }
                    }
                    cursor.close();//关闭游标，释放资源
                }
            }
        }.start();

        mHandler.sendEmptyMessage(DATA_LOADED);//通知UI线程进行数据加载，但此处会不会痴线线程的并发问题
                                               // --mCurrentDir 未在子线程里完成初始化，就被UI线程拿去用了，就出现了空指针报错，会不会呢？
    }

    private void initEvent() {

    }

}
