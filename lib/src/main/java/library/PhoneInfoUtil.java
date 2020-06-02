package library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;
import android.webkit.WebView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * 手机状态信息相关工具类
 */
public class PhoneInfoUtil {
    
    private static String ID;
	private static String DEVICE_ID;
    private static final String UUIDFILE = "UUIDFILE";
    private static final float CORRECT = 0.5f;
    private static final String DEFAULT_ENCODE = "UTF-8";
    
    /**用户网络类型，细分为wifi/2g/3g/4g*/
    public static final int NETTYPE_WIFI = 1;
    public static final int NETTYPE_2G = 2;
    public static final int NETTYPE_3G = 3;
    public static final int NETTYPE_4G = 4;
    public static final int NETTYPE_UNKNOW = 5;
	
    /**
     * 隐藏构造
     */
    private PhoneInfoUtil() {
    	
    }
    
	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + CORRECT);
	}

	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 */
	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + CORRECT);
	}
	
	/**
	  * 将px值转换为sp值，保证文字大小不变
	  *
	  * @param pxValue
	  * @param fontScale（DisplayMetrics类中属性scaledDensity）
	  * @return
	  */
	 public static int px2sp(float pxValue, float fontScale) {
		 return (int) (pxValue / fontScale + CORRECT);
	 }

	 /**
	  * 将sp值转换为px值，保证文字大小不变
	  *
	  * @param spValue
	  * @param fontScale（DisplayMetrics类中属性scaledDensity）
	  * @return
	  */
	 public static int sp2px(float spValue, float fontScale) {
		 return (int) (spValue * fontScale + CORRECT);
	 }
 
	/**
	 *
	 * Android程式碼只認得Pixel，
	 * 而Android Design的原則是希望大家能夠在元件大小用dp為主，
	 * 而文字大小則用sp為主。網路上也很多dp轉pixel的程式碼。但如果還是覺得不保險
	 * Converts an unpacked complex data value holding a dimension to its final floating
	 * point value. The two parametersunit andvalue* are as in {@link TypedValue#TYPE_DIMENSION}.
	 *
	 * @param unit The unit to convert from.
	 * @param value The value to apply the unit to.
	 * @param metrics Current display metrics to use in the conversion --
	 *                supplies display density and scaling information.
	 *
	 * @return The complex floating point value multiplied by the appropriate
	 * metrics depending on its unit.
	 */
	public static float applyDimension(int unit, float value,
	                                   DisplayMetrics metrics) {
		final float in = 72f;
		final float mm = 25.4f;
	    switch (unit) {
    	        case TypedValue.COMPLEX_UNIT_PX:
    	            return value;
    	        case TypedValue.COMPLEX_UNIT_DIP:
    	            return value * metrics.density;
    	        case TypedValue.COMPLEX_UNIT_SP:
    	            return value * metrics.scaledDensity;
    	        case TypedValue.COMPLEX_UNIT_PT:
    	            return value * metrics.xdpi * (1.0f / in);
    	        case TypedValue.COMPLEX_UNIT_IN:
    	            return value * metrics.xdpi;
    	        case TypedValue.COMPLEX_UNIT_MM:
    	            return value * metrics.xdpi * (1.0f / mm);
    	        default:
                return 0;
	    }
	}



	/**
	 * 获取当前分辨率下指定单位对应的像素大小（根据设备信息）
	 * px,dip,sp -> px
	 *
	 * Paint.setTextSize()单位为px
	 *
	 * 代码摘自：TextView.setTextSize()
	 *
	 * @param unit  TypedValue.COMPLEX_UNIT_*
	 * @param size
	 * @return
	 */
	public static float getRawSize(Context c, int unit, float size) {
	       Resources r;

	       if (c == null) {
	           r = Resources.getSystem();
	       } else {
	           r = c.getResources();
	       }

	       return TypedValue.applyDimension(unit, size, r.getDisplayMetrics());
	}

	/**
	 * 获取手机大小（分辨率）
	 * @param context
	 * @return
	 */
		public static DisplayMetrics getScreenPix(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        //DisplayMetrics 一个描述普通显示信息的结构，例如显示大小、密度、字体尺寸
        DisplayMetrics displaysMetrics = new DisplayMetrics();
        //获取手机窗口的Display 来初始化DisplayMetrics 对象
        //getManager()获取显示定制窗口的管理器。
        //获取默认显示Display对象
        //通过Display 对象的数据来初始化一个DisplayMetrics 对象
        windowManager.getDefaultDisplay().getMetrics(displaysMetrics);
        return displaysMetrics;
	}

    public static String getWifiMacAddress() throws SocketException {
        String address = null;
        // 把当前机器上的访问网络接口的存入 Enumeration集合中
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface netWork = interfaces.nextElement();
            // 从路由器上在线设备的MAC地址列表，可以印证设备Wifi的 name 是 wlan0
            if (!TextUtils.equals(netWork.getName(), "wlan0")) {
                continue;
            }
            // 如果存在硬件地址并可以使用给定的当前权限访问，则返回该硬件地址（通常是 MAC）。
            byte[] by = netWork.getHardwareAddress();
            if (by == null || by.length == 0) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : by) {
                builder.append(String.format("%02X:", b));
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            address = builder.toString();
        }
        return address;
    }

    /**
	 * 获取设备MAC地址
	 *
	 * @param context
	 * @return
	 */
    	public static String getMacAddress(Context context) {
        String macSerial = null;
        String str = "";
        InputStreamReader ir = null;
        LineNumberReader input = null;
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address");
            ir = new InputStreamReader(pp.getInputStream(), DEFAULT_ENCODE);
            input = new LineNumberReader(ir);

            for (; null != str;) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            Utility.close(input);
            Utility.close(ir);
        }

        return macSerial;
    }

	public static String getHostIp() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface
	                .getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> ipAddr = intf.getInetAddresses(); ipAddr
	                    .hasMoreElements();) {
	                InetAddress inetAddress = ipAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        ex.printStackTrace();
	    } catch (Exception e) {	// SUPPRESS CHECKSTYLE
	        e.printStackTrace();
	    }
	    return null;
	}

	/**
	 *
	 * 获取位移设备UUID
	 * 首先获取 TelephonyManager.getDeviceId
	 * 获取失败，用一个特定算法计算获得
	 *
	 * @param cxt
	 * @return string
	 */
		public static String getDeviceId(Context cxt) {
		if (DEVICE_ID == null) {
			DEVICE_ID = Md5Util.MD5Encode(createDeviceId(cxt));
		}

		return DEVICE_ID;
	}

	private static String createDeviceId(Context cxt) {
        String tmDeviceId = getAndroidId(cxt);

		final String prefix = "com.weiliu";
		final int per = 10;

		if (tmDeviceId != null && !tmDeviceId.equals("Unknown")
				&& !tmDeviceId.equals("000000000000000")
				&& !tmDeviceId.equals("0")
				&& !tmDeviceId.equals("1")
				&& !tmDeviceId.equals("unknown")) {
			return tmDeviceId;
		} else {
            //noinspection deprecation
            String devIDShort = "35"
					+ (Build.BOARD.length() % per) + (Build.BRAND.length() % per)
					+ (Build.CPU_ABI.length() % per) + (Build.DEVICE.length() % per)
					+ (Build.MANUFACTURER.length() % per)
					+ (Build.MODEL.length() % per)
					+ (Build.PRODUCT.length() % per);

			// Thanks to @Roman SL!
			// http://stackoverflow.com/a/4789483/950427
			// Only devices with API >= 9 have android.os.Build.SERIAL
			// http://developer.android.com/reference/android/os/Build.html#SERIAL
			// If a user upgrades software or roots their phone, there will be a duplicate entry
			String serial = null;
			try {
				serial = Build.class.getField("SERIAL").toString();
				// go ahead and return the serial for api => 9
				return new UUID(devIDShort.hashCode(), serial.hashCode()).toString();
			} catch (Exception ignored) {        // SUPPRESS CHECKSTYLE
				// String needs to be initialized
				serial = prefix; // some value
			}

			// Thanks @Joe!
			// http://stackoverflow.com/a/2853253/950427
			// Finally, combine the values we have found by using the UUID class to create a unique identifier
			return new UUID(devIDShort.hashCode(), serial.hashCode()).toString();
		}
	}

	/**
	 *
	 * 获取设备位移标识
	 *
	 * @param context
	 * @return
	 */
		public static synchronized String uuid(Context context) {
        if (ID == null) {
        	ID = readUUID(context);

            final int maxLen = 20;
            if (ID.length() <= maxLen) {
            	//长度不够的是遗留的无效UUID，重写
            	ID = writeUUID(context);
            }
        }

        return ID;
    }


	public static synchronized String md5Uuid(Context context) {
        return Md5Util.MD5Encode(uuid(context));
    }


    	public static String getDeviceSerialNumber() {
        try {
            return (String) Build.class.getField("SERIAL").get(null);
        } catch (Exception ignored) {
            return "";
        }
    }

    	private static String readUUID(Context context) {
    	String uuid = null;
    	try {
    		InputStream input = context.openFileInput(UUIDFILE);
    		uuid = Utility.streamToString(input);
		} catch (FileNotFoundException ignored) {	//SUPPRESS CHECKSTYLE
		}
    	return !TextUtils.isEmpty(uuid) ? uuid : writeUUID(context);
    }

    private static String writeUUID(Context context) {
		String uuid = UUID.randomUUID().toString();
		try {
			OutputStream output = context.openFileOutput(UUIDFILE, Context.MODE_PRIVATE);
			Utility.stringToStream(uuid, output);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return uuid;
    }

    public static String getNetType(Context context) {
    	ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    	if (networkInfo != null) {
    		return networkInfo.getTypeName();
    	}
    	return null;
    }

    @SuppressLint("HardwareIds")
    public static String getAndroidId(Context context) {
    	return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }

    /**获取设备UA，为webview的默认UA*/
    public static String getDefaultWebViewUA(Context context) {
    	WebView webView = new WebView(context);
    	String ua = webView.getSettings().getUserAgentString();
    	webView.destroy();
    	return ua;
    }

    public static int getOrientation(Context context) {
    	Configuration configuration = context.getResources().getConfiguration();
    	return configuration.orientation;
    }
    /**
     * 判断手机是否ROOT,不是绝对准确，su存在时，也可能已经root，也有su存在但是未root的情况
     * <br>作为广告接口的一个是否root的参数，够用了；
     */
    public static boolean isDeviceRoot() {

        boolean root = false;

        try {
            root = !((!new File(Environment.getRootDirectory() + "/bin/su").exists())
                    && (!new File(Environment.getRootDirectory() + "/xbin/su").exists()));
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return root;
    }


    public static float getDensity(Context context) {
		return context.getResources().getDisplayMetrics().density;
	}

    /**获取系统时区；CMT +8*/
    public static String getTimezone() {
		TimeZone timeZone = TimeZone.getDefault();
		return timeZone.getDisplayName(false, TimeZone.SHORT, Locale.ENGLISH);
	}

    /**获取系统语言；zh，ch。。。*/
	public static String getLanguage() {
		return Locale.getDefault().getLanguage();
	}

    /**获取运营商名称。例如：中国移动*/
    public static String getOperateName(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getNetworkOperatorName();
	}

    /**获取运营商代号。例如：46000*/
    public static String getOperateId(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getNetworkOperator();
	}

    /**获取网络类型，或是网络级别，分为wifi/2g/3g/4g*/
    public static int getNetWorkClass(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null) {
			int netWorkType = networkInfo.getType();
			if (netWorkType == ConnectivityManager.TYPE_WIFI) {
				return NETTYPE_WIFI;
			 } else if (netWorkType == ConnectivityManager.TYPE_MOBILE) {

				int rawNetType = telephonyManager.getNetworkType();
				switch (rawNetType) {
				case TelephonyManager.NETWORK_TYPE_GPRS:
				case TelephonyManager.NETWORK_TYPE_EDGE:
				case TelephonyManager.NETWORK_TYPE_CDMA:
				case TelephonyManager.NETWORK_TYPE_1xRTT:
				case TelephonyManager.NETWORK_TYPE_IDEN:
					return NETTYPE_2G;
				case TelephonyManager.NETWORK_TYPE_UMTS:
				case TelephonyManager.NETWORK_TYPE_EVDO_0:
				case TelephonyManager.NETWORK_TYPE_EVDO_A:
				case TelephonyManager.NETWORK_TYPE_HSDPA:
				case TelephonyManager.NETWORK_TYPE_HSUPA:
				case TelephonyManager.NETWORK_TYPE_HSPA:
				case TelephonyManager.NETWORK_TYPE_EVDO_B:
				case TelephonyManager.NETWORK_TYPE_EHRPD:
				case TelephonyManager.NETWORK_TYPE_HSPAP:
					return NETTYPE_3G;
				case TelephonyManager.NETWORK_TYPE_LTE:
					return NETTYPE_4G;
				default:
					return NETTYPE_UNKNOW;
				}
			} else {
				return NETTYPE_UNKNOW;
			}
		} else {
			return NETTYPE_UNKNOW;
		}
	}

	/**
	 * 是不是小米手机4.2以下的
	 *
	 * @return
	 */
	public static boolean isXiaomiAndLowLevel() {
		String xiaomi = "Xiaomi";
		// 手机品牌
		String brand = Build.BRAND;
		// MANUFACTURER 生产厂家
		String manufacture = Build.MANUFACTURER;

		String fingerprint = Build.FINGERPRINT;

		if (xiaomi.equals(brand) && xiaomi.equals(manufacture)) {
			if (fingerprint != null && fingerprint.contains(xiaomi)) {
				int sdklevel = Build.VERSION.SDK_INT;
				int lev411 = 16; // SUPPRESS CHECKSTYLE
				if (sdklevel <= lev411) { // 4.1.1及其以下
					return true;
				}
			}
		}
		return false;

	}
    
    
}
