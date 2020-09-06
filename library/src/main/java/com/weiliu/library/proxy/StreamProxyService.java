// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.weiliu.library.proxy;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.weiliu.library.R;
import com.weiliu.library.util.Utility;

import org.apache.http.Header;
import org.apache.http.HttpRequest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * （视频）代理服务
 * @author qumiao
 *
 */
public class StreamProxyService extends Service implements Runnable {

    /**强制下载（即使在不播放的情况下）*/
    public static final String ACTION_FORCE_DOWNLOAD = "com.weiliu.action.FORCE_DOWNLOAD";
    /**下载地址，String*/
    public static final String EXTRA_DOWNLOAD_URL = "EXTRA_DOWNLOAD_URL";
    public static final String EXTRA_DOWNLOAD_HEADERS = "EXTRA_DOWNLOAD_HEADERS";
    /**是否通过通知栏显示下载进度，boolean*/
    public static final String EXTRA_SHOW_NOTIFICATION = "EXTRA_SHOW_NOTIFICATION";

	private static final boolean DEBUG = true;
	private static final String LOG_TAG = "Proxy";
	private static final int BUFFER_SIZE = Utility.FILE_STREAM_BUFFER_SIZE;
	private static final String NEW_LINE = "\r\n";
	private static final String HTTP = "HTTP";
	private static final String HTTP_OK = "HTTP/1.1 200 OK";
	private static final String HTTP_PARTIAL = "HTTP/1.1 206 Partial Content";
    private static final String HOST = "Host";
    private static final String HOST_PREF = "Host: ";
	private static final int DEFAULT_PORT = 80;

	private String mAddress;

	/**
	 * 获取该代理Server的地址
	 */
	public String getAddress() {
		return mAddress;
	}
	
	private final IBinder mBinder = new BinderImpl();
    private Handler mMainHandler;
    private HandlerThread mAsyncThread;
    private Handler mAsyncHandler;
	
	private volatile boolean mQuit;
	/**代理服务器*/
	private ServerSocket mProxyServerSocket;
	/**正在运行的代理*/
	private final List<Proxy> mProxyList = new LinkedList<Proxy>();
	
	@Override
	public void onCreate() {
		super.onCreate();

        mMainHandler = new Handler();
        mAsyncThread = new HandlerThread("Stream Proxy Service Async Thread");
        mAsyncThread.start();
        mAsyncHandler = new Handler(mAsyncThread.getLooper());

		try {
			String ip;
			if (TextUtils.isEmpty(System.getProperty("http.proxyHost"))) {
				ip = "127.0.0.1";
			} else {
				ip = Utility.getIpInfo();
			}
			mProxyServerSocket = new ServerSocket(0, 0, InetAddress.getByName(ip));
//			mProxyServerSocket.setSoTimeout(90000);	//SUPPRESS CHECKSTYLE
			mAddress = ip + ":" + mProxyServerSocket.getLocalPort();
			logDebug("StreamProxyService create: address = " + mAddress);
			
			new Thread(this, "ServerThread").start();
			
		} catch (Throwable e) {
			printStackTrace(e);
			Utility.close(mProxyServerSocket);
			stopSelf();
		}
	}

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && TextUtils.equals(intent.getAction(), ACTION_FORCE_DOWNLOAD)) {
            final String url = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
			//noinspection unchecked
			final Map<String, String> headers =
					(Map<String, String>) intent.getSerializableExtra(EXTRA_DOWNLOAD_HEADERS);
//            final boolean showNotification = intent.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, false);

            mAsyncHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mQuit) {
                        return;
                    }
                    String redirectUrl = getRedirectUrl(url, headers, null);
                    StreamProxyCache cache = StreamProxyCache.getInstance(url);
                    cache.setDownloadUrl(redirectUrl);

                    if (requestBaseInfo(cache, url, redirectUrl, null, null)) {
                        cache.forceDownload(mOnDownloadingUpdateListener);
                    }
                }
            });
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private final StreamProxyCache.OnDownloadingUpdateListener mOnDownloadingUpdateListener
            = new StreamProxyCache.OnDownloadingUpdateListener() {
        @Override
        public void onDownloadingUpdate(final String url, final long current, final long length) {
            if (mQuit) {
                return;
            }

            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mQuit) {
                        return;
                    }

                    final int percent = 100;
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(StreamProxyService.this);
                    builder.setContentTitle(String.valueOf(length == 0 ? 0 : current * percent / length));
                    if (current < length) {
                        builder.setOngoing(true);
                        builder.setSmallIcon(R.drawable.downloading);
                    } else {
                        stopSelf();
                        builder.setOngoing(false);
                        builder.setSmallIcon(R.drawable.downloaded_ok);
                    }

                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.notify(R.id.downloading_notification_id, builder.build());
                }
            });
        }
    };

    @Override
	public void onDestroy() {
		super.onDestroy();

		logDebug("StreamProxyService onDestroy");

        synchronized (this) {
            mQuit = true;
            Utility.close(mProxyServerSocket);

            for (Proxy proxy : mProxyList) {
                proxy.stopped = true;
            }
        }

        StreamProxyCache.closeAll(null);

        mMainHandler.removeCallbacksAndMessages(null);
        mAsyncHandler.removeCallbacksAndMessages(null);
        mAsyncThread.quit();
    }

	@NonNull
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class BinderImpl extends IStreamProxyService.Stub {
		@Override
		public String getAddress() throws RemoteException {
			return StreamProxyService.this.getAddress();
		}

        @Override
        public void pauseProxy(String url) throws RemoteException {
            StreamProxyCache cache = StreamProxyCache.getInstance(url);
            cache.pauseDownload();
        }

        @Override
        public void resumeProxy(String url) throws RemoteException {
            StreamProxyCache cache = StreamProxyCache.getInstance(url);
            cache.resumeDownload();
        }
    }
	
	@Override
	public void run() {
		logDebug("running");
		while (!mQuit) {
			Socket mediaToProxySocket = null;
			try {
				mediaToProxySocket = mProxyServerSocket.accept();
				if (mediaToProxySocket == null) {
					continue;
				}
				logDebug("socket connected: " + mediaToProxySocket 
						+ "keep-alive=" + mediaToProxySocket.getKeepAlive());
				
				Proxy proxy;
				synchronized (this) {
					if (mQuit) {
						break;
					}
					
					proxy = new Proxy();
					proxy.socket = mediaToProxySocket;
					mProxyList.add(proxy);
					THREAD_POOL_EXECUTOR.execute(new ProxyRunnable(proxy));
				}
				
			} catch (Exception e) {
				printStackTrace(e);
				Utility.close(mediaToProxySocket);
			}
		}
		Utility.close(mProxyServerSocket);
		logDebug("Proxy interrupted. Shutting down.");
	}
	
	/**
	 * 代理请求
	 */
	private class ProxyRunnable implements Runnable {
		
		private Proxy mProxy;
		
		public ProxyRunnable(Proxy proxy) {
			mProxy = proxy;
		}
		
		@Override
		public void run() {
			try {
				String[] urlHolder = new String[2];
				String request = transRequest(mProxy.socket, urlHolder);
				startProxy(urlHolder[0], urlHolder[1], request, mProxy);
//				startProxyWithoutCache(urlHolder[1], request, mProxy.socket);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Utility.close(mProxy.socket);
				logDebug("close socket: " + mProxy.socket);
				synchronized (StreamProxyService.this) {
					mProxyList.remove(mProxy);
				}
			}
		}
	}
	

	/**
	 * 解析客户端（播放器）的请求，并将其转化成代理server发向Remote Server的请求。
	 * @param mediaToProxySocket 播放器客户端请求
	 * @param urlHolder 解析完请求后，会将原始请求url和重定向 url存到urlHolder[0]和urlHolder[1]中
	 * @return 转化后的请求，用来向Remote Server请求
	 */
	private String transRequest(@NonNull Socket mediaToProxySocket, String[] urlHolder) {
		InputStream is;
		StringBuilder stringBuffer = new StringBuilder();
		try {
			is = mediaToProxySocket.getInputStream();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is, Utility.UTF8), BUFFER_SIZE);
			
			ArrayList<String> lines = new ArrayList<String>();
			int hostLineIndex = -1;
			String host = null;
			int i = 0;
			String readLine;
			while ((readLine = reader.readLine()) != null) {
			    lines.add(readLine + NEW_LINE);
                if (readLine.trim().length() == 0) {
                    break;
                }
                
                if (readLine.startsWith(HOST_PREF)) {
                	if (hostLineIndex != -1) {	//可能有多个Host的情况（如Android2.3.3上），只是用最后一个
                		lines.remove(hostLineIndex);
                		i--;
                	}
                    hostLineIndex = i;
                    host = readLine.substring(HOST_PREF.length());
                }
                
                i++;
			}
			
			logDebug("origin request: " + mediaToProxySocket);
			StringBuilder originBuf = new StringBuilder();
			for (String line : lines) {
			    originBuf.append(line);
			}
			String originRequest = originBuf.toString();
            logDebug(originRequest);
            
			/*
             * 第一行的格式诸如GET /9/v.hoto.cn/71/cd/839025.mp4?v=23 HTTP/1.1
             * 可以从中获取视频真实请求地址
             */
			String firstLine = lines.get(0);
			StringTokenizer st = new StringTokenizer(firstLine.trim());
            // 第一个是method，如GET或POST
            /*String method = */st.nextToken(); 
            // 第二个是url除去authority后的部分，
            String token = st.nextToken();
            // 此部分其实就是真实请求地址，只不过被包装成为了path，所以前面多了个"/"，故substring(1)一下
            final String originUrl = Uri.decode(token.substring(1));
            // 去除重定向，否则接下来MediaPlayer会直接跟重定向后的url打交道，无视我们的代理机制……
            final String redirectUrl = getRedirectUrl(originUrl, requestToHeaders(originRequest), null);

            urlHolder[0] = originUrl;
            urlHolder[1] = redirectUrl;

            Uri redirectURI = Uri.parse(redirectUrl);

            int index = redirectUrl.toLowerCase(Locale.US).indexOf(
                    redirectURI.getEncodedPath().toLowerCase(Locale.US));
            // 只要path（含）之后的内容，参考http request格式
            String replacement = redirectUrl.substring(index);
            firstLine = firstLine.replace(token, replacement);
            
            lines.set(0, firstLine);
            
            /*
             * 如果Host值不是本服务器地址，那就是请求报文中特意指定的，不应该被替换，
             * 否则，就替换成真正的remote server host
             */
            if (hostLineIndex != -1 && mAddress.startsWith(host)) {
            	// 将localhost替换成真正的host
            	String hostLine = HOST_PREF + redirectURI.getHost() + NEW_LINE;
            	lines.set(hostLineIndex, hostLine);
            }
            
            for (String line : lines) {
                stringBuffer.append(line);
            }
		} catch (IOException e) {
			printStackTrace(e);
		}
		return stringBuffer.toString();
	}
	
	/**
	 * 开始运行代理机制：从Remote Server抓取数据，存储到本地，然后通过代理将数据发送给客户端
	 * @param url 原请求地址（可用来做cache key）
	 * @param redirectUrl 去重定向后的直接请求地址（直接指向Remote Server）
	 * @param request Remote请求信息
	 * @param proxy 代理信息
	 */
	private void startProxy(String url, @NonNull String redirectUrl, @NonNull String request, @NonNull Proxy proxy) {
		try {
		    logDebug("request: " + proxy.socket);
            logDebug(request);

            StreamProxyCache cache = StreamProxyCache.getInstance(url);
			if (!cache.checkCacheFile()) {
                Log.e(LOG_TAG, "Check cache file failed!");
                // 改为非缓存模式再来试一试
                startProxyWithoutCache(redirectUrl, request, proxy.socket);
                return;
            }

            cache.setDownloadUrl(redirectUrl);
            Map<String, String> headers = requestToHeaders(request);
            cache.setHeaders(headers);

			if (cache.readLength() == 0 && !requestBaseInfo(cache, url, redirectUrl, request, proxy)) {
				// 改为非缓存模式再来试一试
			    startProxyWithoutCache(redirectUrl, request, proxy.socket);
                return;
			}
			
			long maxLength = cache.readLength();
			long seekPos = findSeekPos(headers, maxLength);
			long contentLength = seekPos != -1L ? maxLength - seekPos : maxLength;
			
			String response = createResponse(cache, seekPos, contentLength, maxLength);
			
			if (!mQuit && !proxy.stopped) {
				cache.sendData(proxy, seekPos != -1L ? seekPos : 0, contentLength, response);
			}
		} catch (Exception e) {
			printStackTrace(e);
		}
		
	}
	
	private void startProxyWithoutCache(@NonNull String url, @NonNull String request, @NonNull Socket mediaToProxySocket) {
		Socket proxyToRealSocket = null;

		try {
			proxyToRealSocket = new Socket();
			URI uri = new URI(url);
			String host = uri.getHost();
			int port = uri.getPort();
			proxyToRealSocket.connect(new InetSocketAddress(
					host, port != -1 ? port : DEFAULT_PORT));
			logDebug("proxy to real server connected");
			
			proxyToRealSocket.getOutputStream().write(request.getBytes(Utility.UTF8));
			logDebug("request: " + mediaToProxySocket);
			logDebug(request);
			
			byte[] buffer = new byte[BUFFER_SIZE];
			
			int readBytes;
			while (!mQuit
					&& (readBytes = proxyToRealSocket.getInputStream().read(buffer)) //SUPPRESS CHECKSTYLE
					!= -1) {
				mediaToProxySocket.getOutputStream().write(buffer, 0, readBytes);
				
				StringBuilder responseBuffer = new StringBuilder();
				
				final int headLen = HTTP.length();
				String headStr = new String(buffer, 0, headLen, Utility.UTF8).toUpperCase(Locale.US);
				// 第一包，是response data
				if (headStr.equals(HTTP)) {
					InputStreamReader reader = new InputStreamReader(
							new ByteArrayInputStream(buffer, 0, readBytes), Utility.UTF8);
					BufferedReader br = new BufferedReader(reader);
					String line;
					while ((line = br.readLine()) != null) {
						responseBuffer.append(line);
						responseBuffer.append(NEW_LINE);
						if (line.trim().length() == 0) {
							break;
						}
					}
					
					logDebug("response:");
					logDebug(responseBuffer.toString());
				}
			}
			
		} catch (Exception e) {
			Log.e("", e.getMessage(), e);
		} finally {
			Utility.close(proxyToRealSocket);
		}
	}
	
	/**
	 * 获取请求的头信息
	 */
	@SuppressWarnings("deprecation")
    @NonNull
	private Map<String, String> requestToHeaders(@NonNull String request) {
		HashMap<String, String> headers = new HashMap<String, String>();
		HttpRequest httpRequest = Utility.stringToHttpRequest(request);
		Header[] httpHeaders = httpRequest.getAllHeaders();
		if (httpHeaders != null) {
			for (Header h : httpHeaders) {
			    // 如果host就是代理Server的地址，那就直接跳过
			    if (TextUtils.equals(h.getName(), HOST)
			            && mAddress.startsWith("" + h.getValue())) {
			        continue;
			    }
				headers.put(h.getName(), h.getValue());
			}
		}
		return headers;
	}

    /**
	 * 通过请求获取基本信息，如长度、ETAG等，获取到后保存到本地的缓存文件头中
	 * @return 获取信息并保存成功，返回true，否则返回false
	 */
	private boolean requestBaseInfo(@NonNull StreamProxyCache cache,
                                    String url, @NonNull String redirectUrl, @Nullable String request, @Nullable Proxy proxy) {
		Socket proxyToRealSocket = null;
        String responseStr = null;

		try {
            String key = generateRedirectCacheKey(url, request != null ? requestToHeaders(request) : null);
            if (URL_RESPONSE_MAP.containsKey(key)) {
                logDebug("find response fields from cache: ");
                responseStr = URL_RESPONSE_MAP.get(key);
                logDebug(responseStr);

            } else if (!TextUtils.isEmpty(request) && proxy != null) {
                proxyToRealSocket = new Socket();
                logDebug("send request to real server");
                responseStr = requestResponseHeaders(proxyToRealSocket, redirectUrl, request);
                int code = responseStr == null ? 0 : getResponseCode(responseStr);
                if (code != HttpURLConnection.HTTP_OK) {
                    proxyToRealSocket.close();
                    throw new RuntimeException("response code = " + code);
                }
            }

            if (responseStr != null) {
                String eTag = getETag(responseStr);
                if (eTag != null && !cache.writeETag(eTag)) {
                    return false;
                }
                String lengthStr = getContentLength(responseStr);
                if (lengthStr != null && !cache.writeLength(Long.parseLong(lengthStr))) {
                    return false;
                }

                return cache.writeOtherInfo(responseStr);
            }
		} catch (Exception e) {
			Log.e("", e.getMessage(), e);
		} finally {
			Utility.close(proxyToRealSocket);
		}
		
		return false;
	}
	
	private String getETag(@NonNull String responseStr) {
		final String startFlag = "ETag: \"";
		final String endFlag = "\"";
		int start = responseStr.indexOf(startFlag);
		if (start != -1) {
			int end = responseStr.indexOf(endFlag, start + startFlag.length());
			return responseStr.substring(start + startFlag.length(), end);
		}
		return null;
	}
	
	private static final Pattern CONTENT_RANGE_PATTERN = Pattern.compile(
			"Content-Range: bytes (\\d*)-(\\d*)/(\\d*)");
	private static final Pattern CONTENT_LENGTH_PATTERN = Pattern.compile(
			"Content-Length: (\\d*)");
	
	private String getContentLength(@NonNull String responseStr) {
		Matcher matcher = CONTENT_RANGE_PATTERN.matcher(responseStr);
		if (matcher.find()) {
			return matcher.group(3);	//SUPPRESS CHECKSTYLE
		}
		
		matcher = CONTENT_LENGTH_PATTERN.matcher(responseStr);
		if (matcher.find()) {
			return matcher.group(1);
		}
		
		return null;
	}
	
	@NonNull
	private static Pattern RANGE_PATTERN = Pattern.compile("bytes=(\\d*)-(\\d*).*");
	
	private long findSeekPos(@NonNull Map<String, String> headers, long maxLength) {
		long seekPos = -1;
		final String rangeKey = "Range";
		if (headers.containsKey(rangeKey)) {
			String value = headers.get(rangeKey);
			Matcher matcher = RANGE_PATTERN.matcher(value);
			if (matcher.matches()) {
				String group1 = matcher.group(1);
				if (!TextUtils.isEmpty(group1)) {
					seekPos = Long.parseLong(group1);
				} else {
					String group2 = matcher.group(2);
					if (!TextUtils.isEmpty(group2)) {
						seekPos = maxLength - Long.parseLong(group2);
					}
				}
			}
		}
		return seekPos;
	}
	
	@Nullable
	private String createResponse(@NonNull StreamProxyCache cache, long seekPos, long contentLength, long maxLength) {
		String response = cache.readOtherInfo();
		if (response == null) {
			return null;
		}

		//如果没有Status Line这一行，临时在第一行追加一个200的Status Line
        if (!response.startsWith(HTTP)) {
            response = HTTP_OK + NEW_LINE + response;
        }
        if (seekPos != -1L) {
			long endPos = seekPos + contentLength - 1;
			response = response.replaceAll("Content-Range: bytes \\d*-\\d*/\\d*\\s*", "");
			response = response.replaceAll("Content-Length: \\d*", 
					String.format("Content-Range: bytes %s-%s/%s\r\n",
							seekPos, endPos, maxLength)
					+ "Content-Length: " + contentLength);	//SUPPRESS CHECKSTYLE
			
			response = response.replace(HTTP_OK, HTTP_PARTIAL);
		} else {
			response = response.replaceAll("Content-Range: bytes \\d*-\\d*/\\d*\\s*", "");
			response = response.replaceAll("Content-Length: \\d*", 
					"Content-Length: " + contentLength);
			
			response = response.replace(HTTP_PARTIAL, HTTP_OK);
		}
		
		SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		String dateStr = df.format(new Date());
		// 修改时间
		response = response.replaceAll("Date: .+?\\r\\n", "Date: " + dateStr + "\r\n");
		
		return response;
	}
	
	
	private static void logDebug(String log) {
		if (DEBUG) {
			Log.d(LOG_TAG, log);
		}
	}
	
	private static void printStackTrace(@NonNull Throwable e) {
		if (DEBUG) {
			e.printStackTrace();
		} else {
			Log.w(LOG_TAG, "" + e.getMessage());
		}
	}
	
	private static final Hashtable<String, String> URL_REDIRECT_MAP
            = new Hashtable<String, String>();
	private static final Hashtable<String, String> URL_RESPONSE_MAP
            = new Hashtable<String, String>();
	/**
	 * 获取重定向后的URL，即真正有效的链接
	 * 
	 * @param urlString
	 * @param headers
     * @param responseHolder 如果不为null，responseHolder[0]处存储最终url访问返回的响应头信息
	 * @return
	 */
	@NonNull
	public static String getRedirectUrl(
            @NonNull String urlString, Map<String, String> headers, @Nullable String[] responseHolder) {
		String redirect = null;
		String key = generateRedirectCacheKey(urlString, headers);
		if (URL_REDIRECT_MAP.containsKey(key)) {
			redirect = URL_REDIRECT_MAP.get(key);
            if (responseHolder != null) {
                responseHolder[0] = URL_RESPONSE_MAP.get(key);
            }
			logDebug("find redirect from cache: " + redirect);
			return redirect;
		}

        Socket socket = new Socket();
		try {
            String request = createRequest(urlString, headers);
            String response = requestResponseHeaders(socket, urlString, request);
            socket.close();

			int code = 0;
            if (response != null) {
                code = getResponseCode(response);
                redirect = getHeaderField(response, "Location");
            }

            if (responseHolder == null) {
                responseHolder = new String[1];
            } else {
                responseHolder[0] = null;   //先清空一下，以便后续更好作判断
            }

            boolean success = false;
            if ((code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP)
                    && redirect != null) {
                // 防止再度跳转
                redirect = getRedirectUrl(redirect, headers, responseHolder);
                success = responseHolder[0] != null;    //最后一次跳转成功命中了200或206

            } else if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL) {
                redirect = urlString;
                responseHolder[0] = response;    //只有命中了200或206才有效，此时保存响应头信息
                success = true;
            }

            if (success) {
                URL_REDIRECT_MAP.put(key, redirect);
                URL_RESPONSE_MAP.put(key, responseHolder[0]);
            }
        } catch (Exception ignored) {	//SUPPRESS CHECKSTYLE
		} finally {
			Utility.close(socket);
		}

		if (TextUtils.isEmpty(redirect)) {
			redirect = urlString;
		}
		return redirect;
	}
	
	@NonNull
	private static String generateRedirectCacheKey(String urlString, @Nullable Map<String, String> headers) {
		return "Url=" + urlString + (headers != null ? ", Host=" + headers.get("Host") : "");
	}

    private static String createRequest(@NonNull String urlString, @Nullable Map<String, String> headers)
            throws URISyntaxException {
        URI url = new URI(urlString);
        StringBuilder requestBuilder = new StringBuilder();
        String withoutHost = urlString.substring(urlString.indexOf(url.getPath()));
        if (withoutHost.charAt(0) == '/') {
            withoutHost = withoutHost.substring(1);
        }
        requestBuilder.append("GET /").append(withoutHost).append(" HTTP/1.1").append(NEW_LINE);
        if (headers != null && headers.containsKey(HOST)) {
            requestBuilder.append(HOST_PREF).append(headers.get(HOST)).append(NEW_LINE);
        } else {
            requestBuilder.append(HOST_PREF).append(url.getHost()).append(NEW_LINE);
        }
        if (headers != null) {
            for (Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append(NEW_LINE);
            }
        }
        requestBuilder.append(NEW_LINE);
        return requestBuilder.toString();
    }

    private static String requestResponseHeaders(@NonNull Socket socket, @NonNull String url, @NonNull String request)
            throws IOException, URISyntaxException {
        URI uri = new URI(url);
        String host = uri.getHost();
        int port = uri.getPort();
        // 连接 remote服务器
        socket.connect(new InetSocketAddress(
                host, port != -1 ? port : DEFAULT_PORT));
        logDebug("proxy to real server connected");

        String method = request.substring(0, request.indexOf(' '));
        // 发送请求（只能按原来的method请求（保证信息一致性），不能取巧地将method改为HEAD）
        socket.getOutputStream().write(
                request/*.replaceFirst(method, "HEAD")*/.getBytes(Utility.UTF8));

        byte[] buffer = new byte[BUFFER_SIZE];
        int readBytes;
        if ((readBytes = socket.getInputStream().read(buffer)) //SUPPRESS CHECKSTYLE
                != -1) {

            final int headLen = HTTP.length();
            String headStr = new String(buffer, 0, headLen, Utility.UTF8).toUpperCase(Locale.US);
            // 第一包，是response header
            if (headStr.equals(HTTP)) {
                StringBuilder responseBuffer = new StringBuilder();
                InputStreamReader reader = new InputStreamReader(
                        new ByteArrayInputStream(buffer, 0, readBytes), Utility.UTF8);
                BufferedReader br = new BufferedReader(reader);
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuffer.append(line);
                    responseBuffer.append(NEW_LINE);
                    if (line.trim().length() == 0) {
                        break;
                    }
                }

                return responseBuffer.toString();
            }
        }
        return null;
    }

    private static int getResponseCode(@NonNull String response) {
        int startIndex = response.indexOf(' ');
        if (startIndex == -1) {
            return -1;
        }
        startIndex += 1;
        int endIndex = response.indexOf(' ', startIndex);
        if (endIndex == -1) {
            return -1;
        }
        return Integer.parseInt(response.substring(startIndex, endIndex));
    }

    private static String getHeaderField(@NonNull String response, String key) {
        String pref = key + ": ";
        int startIndex = response.indexOf(pref);
        if (startIndex == -1) {
            return null;
        }
        startIndex += pref.length();
        int endIndex = response.indexOf(NEW_LINE, startIndex);
        if (endIndex == -1) {
            return null;
        }
        return response.substring(startIndex, endIndex);
    }

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAXIMUM_POOL_SIZE = 128;
    private static final int KEEP_ALIVE = 1;
    private static final int QUEUE_SIZE = 10;

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @NonNull
		public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "Proxy #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> POOL_WORK_QUEUE =
            new LinkedBlockingQueue<>(QUEUE_SIZE);

    private static final ExecutorService THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
                    TimeUnit.SECONDS, POOL_WORK_QUEUE, THREAD_FACTORY);
	
	public static class Proxy {
		public Socket socket;
		public volatile boolean stopped;
	}
}