package com.example.luke.chosepic.bean;

/**
 * Created by LUKE on 2015/10/6.
 */
public class FolderBean {
    private String dir;//��ǰ�ļ���·��
    private String firstImgPath;//��ǰ�ļ��е�һ��ͼƬ·��
    private String name;//��ǰ�ļ��е�����
    private int count;//��ǰ�ļ���ͼƬ����

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf = dir.lastIndexOf('/');
        this.name = this.dir.substring(lastIndexOf);
    }

    public String getName() {
        return name;
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }



}
