package library;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * 字符串MD5加密工具类
 * 
 */
public class Md5Util {

    public static String MD5Encode(String string) {
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

    public static String fileMD5(String inputFile) throws IOException {

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
            Utility.close(digestInputStream);
            Utility.close(fileInputStream);
        }

    }
    
}
// CHECKSTYLE:ON