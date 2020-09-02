package library.util;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;

public class ArrayUtil {


    public static String deepToString(Object array) {
        if (array == null)
            return "null";
        StringBuilder buf = new StringBuilder();
        deepToString(array, buf, new HashSet<>());
        return buf.toString();
    }

    private static void deepToString(Object array, StringBuilder buf,
                                     Set<Object> dejaVu) {
        if (array == null) {
            buf.append("null");
            return;
        }
        int iMax = Array.getLength(array) - 1;
        if (iMax == -1) {
            buf.append("[]");
            return;
        }

        dejaVu.add(array);
        buf.append('[');
        for (int i = 0; ; i++) {

            Object element = Array.get(array, i);
            if (element == null) {
                buf.append("null");
            } else {
                Class<?> eClass = element.getClass();

                if (eClass.isArray()) {
                    if (eClass == byte[].class)
                        buf.append(toShortString((byte[]) element));
                    else if (eClass == short[].class)
                        buf.append(toShortString((short[]) element));
                    else if (eClass == int[].class)
                        buf.append(toShortString((int[]) element));
                    else if (eClass == long[].class)
                        buf.append(toShortString((long[]) element));
                    else if (eClass == char[].class)
                        buf.append(toShortString((char[]) element));
                    else if (eClass == float[].class)
                        buf.append(toShortString((float[]) element));
                    else if (eClass == double[].class)
                        buf.append(toShortString((double[]) element));
                    else if (eClass == boolean[].class)
                        buf.append(toShortString((boolean[]) element));
                    else { // element is an array of object references
                        if (dejaVu.contains(element))
                            buf.append("[...]");
                        else
                            deepToString((Object[])element, buf, dejaVu);
                    }
                } else {  // element is non-null and not an array
                    if (array instanceof byte[]) {
                        buf.append(byteToHex((byte) element));
                    } else {
                        buf.append(element.toString());
                    }
                }
            }
            if (i == iMax)
                break;
            if (i >= 20) {
                buf.append(" ... len_" + (iMax + 1));
                break;
            }
            buf.append(", ");
        }
        buf.append(']');
        dejaVu.remove(array);
    }

    private static String toShortString(Object array) {
        if (array == null)
            return "null";
        int iMax = Array.getLength(array) - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            if (array instanceof byte[]) {
                b.append(byteToHex((Byte) Array.get(array, i)));
            } else {
                b.append(Array.get(array, i));
            }
            if (i == iMax)
                return b.append(']').toString();
            if (i >= 20) {
                b.append(" ... len_" + (iMax + 1) + "]");
                return b.toString();
            }
            b.append(", ");
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String byteToHex(byte b) {
        char[] hexChars = new char[2];
        int v = b & 0xFF;
        hexChars[0] = HEX_ARRAY[v >>> 4];
        hexChars[1] = HEX_ARRAY[v & 0x0F];
        return "0x" + new String(hexChars);
    }
}
