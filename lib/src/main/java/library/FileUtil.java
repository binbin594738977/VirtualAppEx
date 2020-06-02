package library;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件操作类
 * <p>
 * 缓存文件管理，去那吃，菜谱通用
 * <p>
 * 实现缓存文件更新，设置缓存过期时间
 * <p>
 * 图片缓存文件存入时做裁减
 * <p>
 * 缓存目录文件个数监控
 * <p>
 * 缓存目录使用空间监控
 */
public class FileUtil {

    private static final String TAG = "FileUtil";
    private static final int BUFF_SIZE = 8192;

    private FileUtil() {

    }

    /**
     * 文件或文件夹拷贝
     * 如果是文件夹拷贝 目标文件必须也是文件夹
     *
     * @param srcFile 源文件
     * @param dstFile 目标文件
     * @return
     */
    public static boolean copy(File srcFile, File dstFile) {
        if (!srcFile.exists()) { //源文件不存在
            return false;
        }

        if (srcFile.isDirectory()) { //整个文件夹拷贝
            if (dstFile.isFile()) {    //如果目标是文件，返回false
                return false;
            }

            if (!dstFile.exists()) {
                mkdirs(dstFile);
            }

            for (File f : srcFile.listFiles()) {
                if (!copy(f, new File(dstFile, f.getName()))) {
                    return false;
                }
            }
            return true;

        } else { //单个文价拷贝
            return copyFile(srcFile, dstFile);
        }

    }

    /**
     * 判断某个文件所在的文件夹是否存在，不存在时直接创建
     *
     * @param path
     */
    public static void parentFolder(String path) {
        File file = new File(path);
        String parent = file.getParent();

        File parentFile = new File(parent + File.separator);
        if (!parentFile.exists()) {
            mkdirs(parentFile);
        }
    }

    /**
     * 拷贝文件
     *
     * @param srcFile  源文件
     * @param destFile 目标文件，如果是目录，则生成该目录下的同名文件再拷贝
     */
    private static boolean copyFile(File srcFile, File destFile) {
        if (!destFile.exists()) {
            if (!mkdirs(destFile.getParentFile()) || !createNewFile(destFile)) {
                return false;
            }
        } else if (destFile.isDirectory()) {
            destFile = new File(destFile, srcFile.getName());
        }

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(destFile);
            FileChannel src = in.getChannel();
            FileChannel dst = out.getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();

            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //可能是权限问题导致目标文件无法写入，跳过即可
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utility.close(out);
            Utility.close(in);
        }

        return false;
    }

    /**
     * 创建目录（如果不存在）。
     *
     * @param dirPath 目录的路径
     * @return true表示创建，false表示该目录已经存在
     */
    public static boolean createDirIfMissed(String dirPath) {
        File dir = new File(dirPath);
        return !dir.exists() && dir.mkdirs();
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件，并且删除该目录
     *
     * @param path 将要删除的文件目录
     * @return boolean 成功清除目录及子文件返回true；
     * 若途中删除某一文件或清除目录失败，则终止清除工作并返回false.
     */
    public static boolean deleteDir(String path) {
        return deleteDir(new File(path));
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件，并且删除该目录
     *
     * @param dir 将要删除的文件目录
     * @return boolean 成功清除目录及子文件返回true；
     * 若途中删除某一文件或清除目录失败，则终止清除工作并返回false.
     */
    public static boolean deleteDir(File dir) {
        if (dir == null) {
            return false;
        }

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    /**
     * 递归清空目录下的所有文件及子目录下所有文件，但不删除目录（包括子目录）
     *
     * @param path 将要清空的文件目录
     * @return boolean 成功清除目录及子文件返回true；
     * 若途中清空某一文件或清除目录失败，则终止清除工作并返回false.
     */
    public static boolean clearDir(String path) {
        return clearDir(path, null);
    }

    /**
     * 递归清空目录下的所有文件及子目录下所有文件，但不删除目录（包括子目录）
     *
     * @param path    将要清空的文件目录
     * @param excepts 除去这些目录或者文件，可以为null
     * @return boolean 成功清除目录及子文件返回true；
     * 若途中清空某一文件或清除目录失败，则终止清除工作并返回false.
     */
    public static boolean clearDir(String path, List<String> excepts) {
        ArrayList<File> exceptFiles = new ArrayList<>();
        if (excepts != null) {
            for (String except : excepts) {
                exceptFiles.add(new File(except));
            }
        }
        return clearDir(new File(path), exceptFiles);
    }

    /**
     * 递归清空目录下的所有文件及子目录下所有文件，但不删除目录（包括子目录）
     *
     * @param dir 将要清空的文件目录
     * @return boolean 成功清除目录及子文件返回true；
     * 若途中清空某一文件或清除目录失败，则终止清除工作并返回false.
     */
    public static boolean clearDir(File dir) {
        return clearDir(dir, null);
    }

    /**
     * 递归清空目录下的所有文件及子目录下所有文件，但不删除目录（包括子目录）
     *
     * @param dir     将要清空的文件目录
     * @param excepts 除去这些目录或者文件，可以为null
     * @return boolean 成功清除目录及子文件返回true；
     * 若途中清空某一文件或清除目录失败，则终止清除工作并返回false.
     */
    public static boolean clearDir(File dir, List<File> excepts) {
        if (dir == null) {
            return false;
        }

        if (excepts != null && excepts.contains(dir)) {
            return true;
        }

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = clearDir(new File(dir, child), excepts);
                if (!success) {
                    return false;
                }
            }
            return true;
        }

        return dir.delete();
    }


    /**
     * 获取某个目录下所有文件的大小之和
     *
     * @param path
     * @return
     */
    public static float getDirSize(String path, boolean isRoot) {
        return getDirSize(path, null, isRoot);
    }

    /**
     * 获取某个目录下所有文件的大小之和
     *
     * @param path
     * @param excepts 除去这些目录或者文件，可以为null
     * @return
     */
    public static float getDirSize(String path, List<String> excepts, boolean isRoot) {
        if (TextUtils.isEmpty(path)) {
            return 0.f;
        }
        ArrayList<File> exceptFiles = new ArrayList<>();
        if (excepts != null) {
            for (String except : excepts) {
                exceptFiles.add(new File(except));
            }
        }
        return getDirSize(new File(path), exceptFiles, isRoot);
    }

    /**
     * 获取某个目录下所有文件的大小之和
     *
     * @return
     */
    public static float getDirSize(File dir, boolean isRoot) {
        return getDirSize(dir, null, isRoot);
    }

    /**
     * 获取某个目录下所有文件的大小之和
     *
     * @param excepts 除去这些目录或者文件，可以为null
     * @return
     */
    public static float getDirSize(File dir, List<File> excepts, boolean isRoot) {
        float size = 0.f;

        if (dir == null) {
            return size;
        }

        if (excepts != null && excepts.contains(dir)) {
            return size;
        }

        if (dir.exists()) {
            if (dir.isDirectory()) {
                File[] fs = dir.listFiles();
                for (File childFile : fs) {
                    if (childFile.isFile()) {
                        size += childFile.length();
                    } else {
                        size += getDirSize(childFile, excepts, false);
                    }
                }
            } else {
                if (!isRoot) {
                    size += dir.length();
                }
            }
        }

        return size;
    }


    /**
     * 删除文件。如果删除失败，则打出error级别的log
     *
     * @param file 文件
     * @return 成功与否
     */
    public static boolean deleteFile(File file) {
        if (file == null) {
            return false;
        }
        boolean result = file.delete();
        if (!result) {
            Log.e(TAG, "FileUtil cannot delete file: " + file);
        }
        return result;
    }

    /**
     * 创建文件。如果创建失败，则打出error级别的log
     *
     * @param file 文件
     * @return 成功与否
     */
    public static boolean createNewFile(File file) {
        if (file == null) {
            return false;
        }

        boolean result;
        try {
            result = file.createNewFile() || file.isFile();
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        }

        if (!result) {
            Log.e(TAG, "FileUtil cannot create file: " + file);
        }
        return result;
    }

    /**
     * 创建目录。如果创建失败，则打出error级别的log
     *
     * @param file 文件
     * @return 成功与否
     */
    public static boolean mkdir(File file) {
        if (file == null) {
            return false;
        }
        if (!file.mkdir() && !file.isDirectory()) {
            Log.e(TAG, "FileUtil cannot make dir: " + file);
            return false;
        }
        return true;
    }

    /**
     * 创建文件对应的所有父目录。如果创建失败，则打出error级别的log
     *
     * @param file 文件
     * @return 成功与否
     */
    public static boolean mkdirs(File file) {
        if (file == null) {
            return false;
        }
        if (!file.mkdirs() && !file.isDirectory()) {
            Log.e(TAG, "FileUtil cannot make dirs: " + file);
            return false;
        }
        return true;
    }

    /**
     * 文件或目录重命名。如果失败，则打出error级别的log
     *
     * @param srcFile 原始文件或目录
     * @param dstFile 重命名后的文件或目录
     * @return 成功与否
     */
    public static boolean renameTo(File srcFile, File dstFile) {
        if (srcFile == null || dstFile == null) {
            return false;
        }
        if (!srcFile.renameTo(dstFile)) {
            Log.e(TAG, "FileUtil cannot rename " + srcFile + " to " + dstFile);
            return false;
        }

        return true;
    }


    public static String getSizeString(long size) {
        if (size <= 0) {
            return "0B";
        }
        final String[] units = {"B", "K", "M", "G"};
        final DecimalFormat format = new DecimalFormat("0.##");
        final int kilo = 1024;
        double s = size;
        String finalUnit = null;
        for (String unit : units) {
            if (s < kilo) {
                finalUnit = unit;
                break;
            }
            s /= kilo;
        }
        if (finalUnit == null) {
            finalUnit = "T";
        }
        return format.format(s) + finalUnit;
    }

    public static void writeToFile(String content, String filename) throws Exception {
        FileWriter fw = new FileWriter(filename, false);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.newLine();
        bw.write(content);
        bw.close();
        fw.close();
    }

    public static String getFromFile(String filename) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        reader.close();
        return stringBuilder.toString();
    }

    /**
     *获取文件扩展名
     */
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }
}

