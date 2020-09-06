package com.weiliu.library.sr;

/**
 * 作者：qumiao
 * 日期：2017/3/9 16:47
 * 说明：保存路径
 */
public class SavePath {
    /**分隔符。最好设置复杂一些，免得被key字串中包含*/
    private static final String SEPARATOR = " =====> ";

    private static final String PREFIX_KEY = "<";
    private static final String PREFIX_INDEX = "[";

    private StringBuilder mPath;

    public SavePath() {
        mPath = new StringBuilder();
    }

    public SavePath(String path) {
        mPath = new StringBuilder(path);
    }

    public SavePath(SavePath path) {
        mPath = new StringBuilder(path.mPath);
    }

    public SavePath appendKey(String key) {
        mPath.append(SEPARATOR).append(PREFIX_KEY).append(key);
        return this;
    }

    public SavePath appendIndex(int index) {
        mPath.append(SEPARATOR).append(PREFIX_INDEX).append(index);
        return this;
    }

    public String getPath() {
        return mPath.toString();
    }

    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || ((o instanceof SavePath) && ((SavePath) o).getPath().equals(getPath()));
    }

}
