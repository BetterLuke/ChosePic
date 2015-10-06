package com.example.luke.chosepic;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.luke.chosepic.bean.FolderBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    private GridView mGridView;
    private List<String> mImgs;//mGridView的Adapter的数据集

    private RelativeLayout mBottomly;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

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

    private void initDatas() {

    }

    private void initEvent() {

    }

}
