package com.example.luke.chosepic.bean;

/**
 * Created by LUKE on 2015/10/6.
 */
public class FolderBean {
    private String dir;//当前文件夹路径
    private String firstImgPath;//当前文件夹第一张图片路径
    private String name;//当前文件夹的名称
    private int count;//当前文件夹图片数量

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
