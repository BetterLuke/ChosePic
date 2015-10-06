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
    private List<String> mImgs;//mGridView��Adapter�����ݼ�

    private RelativeLayout mBottomly;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount = Integer.MIN_VALUE;

    //initData(��ɨ����ɺ󣬼���ߵ�FolderBean���ḽ��ֵ�����ڳ�ʼ��popuewindow
    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

    private ProgressDialog mProgressDialog ;//initDatas()��ʾɨ��ͼƬ�Ľ���

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED){
                mProgressDialog.dismiss();//�رս�������

                dataToView();
            }

        }
    };

    private void dataToView() {
        if (mCurrentDir == null){
            Toast.makeText(this,"δɨ�赽�κ�ͼƬ",Toast.LENGTH_SHORT).show();
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
     * ��ʼ�����
     */
    private void initView() {
        mGridView = (GridView) findViewById(R.id.id_gridView);
        mBottomly = (RelativeLayout) findViewById(R.id.id_bottom_ly);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
    }

    /**
     * ����ContentProviderɨ���ֻ��е�����ͼƬ
     */
    private void initDatas() {
        //����ⲿ����ռ��Ƿ���ã��������ã�ֱ�ӷ�������ֹ
        if (!  Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Toast.makeText(this,"��ǰ���濨�����ã�",Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this,null,"���ڼ���...");

        new Thread(){
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();

                Cursor cursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + "= ? or" + MediaStore.Images.Media.MIME_TYPE + "= ?",
                        new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);

                //����Ѿ����������ļ��е�����
                Set<String> mDirPaths = new HashSet<String>();

                if (cursor != null) {
                    while (cursor.moveToNext()){
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));//��ȡͼƬ��Path
                        File parentFile = new File(path).getParentFile();//��ȡͼƬ���ڵ��ļ��ж���
                        if (parentFile == null){
                            //��ֹ��������޷���ȡͼƬ�������ļ��ж��󣬼���������ѭ��
                            continue;
                        }
                        String dirPath = parentFile.getAbsolutePath();
                        FolderBean folderBean = null;

                        if (mDirPaths.contains(dirPath)){
                            //��mDirPaths���Ѿ�����Ҫ�������ļ��������ˣ�˵�����ļ����Ѿ��������ˣ����������ѭ��
                            continue;
                        } else {
                            //�������ļ��У������б�ǣ��Ѹ��ļ�����Ϣ��װ��folderBean��
                            mDirPaths.add(dirPath);

                            folderBean = new FolderBean();
                            folderBean.setDir(dirPath);
                            folderBean.setFirstImgPath(path);
                        }
                        if (parentFile.list() == null){
                            //�Դ��������ж���Ȼ���޽⣬������Ϊ�˷�ֹ��Ʒ������
                            continue;
                        }

                        //��ȡ��ǰ�ļ���ͼƬ������������folderBean
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
                    cursor.close();//�ر��α꣬�ͷ���Դ
                }
            }
        }.start();

        mHandler.sendEmptyMessage(DATA_LOADED);//֪ͨUI�߳̽������ݼ��أ����˴��᲻������̵߳Ĳ�������
                                               // --mCurrentDir δ�����߳�����ɳ�ʼ�����ͱ�UI�߳���ȥ���ˣ��ͳ����˿�ָ�뱨���᲻���أ�
    }

    private void initEvent() {

    }

}
