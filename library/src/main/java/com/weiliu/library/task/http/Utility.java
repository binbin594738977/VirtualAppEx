package com.weiliu.library.task.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 作者：qumiao
 * 日期：2017/3/1 17:35
 * 说明：
 */

class Utility {

    private Utility() {
        //no instance
    }

    /**
     * File buffer stream size.
     */
    public static final int FILE_STREAM_BUFFER_SIZE = 16 * 1024;

    /**
     * String to OutputStream
     * @param str String
     * @param stream OutputStream
     * @return success or not
     */
    public static boolean stringToStream(@Nullable String str, @NonNull OutputStream stream) {
        if (str == null) {
            return false;
        }

        byte[] data;
        try {
            data = str.getBytes("UTF-8");
            stream.write(data);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(stream);
        }

        return false;
    }

    /**
     * String to file
     * @param str String
     * @param file File
     * @return success or not
     */
    public static boolean stringToFile(@Nullable String str, @NonNull File file) {
        return stringToFile(str, file, false);
    }

    /**
     * String to file
     * @param str String
     * @param file File
     * @param append is append mode or not
     * @return success or not
     */
    public static boolean stringToFile(@Nullable String str, @NonNull File file, boolean append) {
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(file, append);
            return stringToStream(str, stream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } finally {
            close(stream);
        }
    }

    /**
     * stream to file
     * @param in Inputstream
     * @param file File
     * @return success or not
     */
    public static boolean streamToFile(@NonNull InputStream in, @NonNull File file) {
        return streamToFile(in, file, false);
    }

    /**
     * stream to file
     * @param in Inputstream
     * @param file File
     * @param append is append mode or not
     * @return success or not
     */
    public static boolean streamToFile(@NonNull InputStream in, @NonNull File file, boolean append) {
        OutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(file, append));
            final byte[] buffer = new byte[FILE_STREAM_BUFFER_SIZE];
            int n;
            while (-1 != (n = in.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utility.close(output);
            Utility.close(in);
        }
        return false;
    }

    /**
     * stream to bytes
     * @param is inputstream
     * @return bytes
     */
    public static byte[] streamToBytes(@NonNull InputStream is) {
        try {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final byte[] buffer = new byte[FILE_STREAM_BUFFER_SIZE];
            int n;
            while (-1 != (n = is.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(is);
        }
        return null;
    }
    /**
     * 转换Stream成string
     * @param is
     *      Stream源
     * @return
     *      目标String
     */
    @NonNull
    public static String streamToString(@NonNull InputStream is) {
        return streamToString(is, "UTF-8");
    }
    /**
     * 按照特定的编码格式转换Stream成string
     * @param is
     *      Stream源
     * @param enc
     *      编码格式
     * @return
     *      目标String
     */
    @NonNull
    public static String streamToString(@NonNull InputStream is, String enc) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] data = new byte[FILE_STREAM_BUFFER_SIZE];
        try {
            int count;
            while ((count = is.read(data)) > 0) {
                os.write(data, 0, count);
            }
            return new String(os.toByteArray(), enc);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            close(os);
            close(is);
        }
        return "";
    }

    /**
     * 转换文件内容成string
     * @param file
     *      File
     * @return
     *      目标String
     */
    @NonNull
    public static String fileToString(@NonNull File file) {
        try {
            return streamToString(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 按照特定的编码格式转换文件内容成string
     * @param file
     *      File
     * @param enc
     *      编码格式
     * @return
     *      目标String
     */
    @NonNull
    public static String fileToString(@NonNull File file, String enc) {
        try {
            return streamToString(new FileInputStream(file), enc);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }



    /**
     * 关闭，并捕获IOException
     * @param closeable Closeable
     */
    public static void close(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static String MD5Encode(@NonNull String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(
                    string.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                if ((b & 0xFF) < 0x10) {
                    hex.append("0");
                }
                hex.append(Integer.toHexString(b & 0xFF));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String fileMD5(@NonNull String inputFile) throws IOException {

        // 缓冲区大小（这个可以抽出一个参数）

        int bufferSize = 256 * 1024;

        FileInputStream fileInputStream = null;

        DigestInputStream digestInputStream = null;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(inputFile);

            digestInputStream = new DigestInputStream(fileInputStream,
                    messageDigest);

            // read的过程中进行MD5处理，直到读完文件
            byte[] buffer = new byte[bufferSize];
            while (digestInputStream.read(buffer) > 0);

            // 获取最终的MessageDigest
            messageDigest = digestInputStream.getMessageDigest();

            // 拿到结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();

            // 同样，把字节数组转换成字符串
            StringBuilder hex = new StringBuilder(resultByteArray.length * 2);
            for (byte b : resultByteArray) {
                if ((b & 0xFF) < 0x10) {
                    hex.append("0");
                }
                hex.append(Integer.toHexString(b & 0xFF));
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            return null;
        } finally {
            close(digestInputStream);
            close(fileInputStream);
        }

    }
}
