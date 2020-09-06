package com.weiliu.library.proxy;

import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import com.weiliu.library.RootApplication;
import com.weiliu.library.util.FileUtil;
import com.weiliu.library.util.Md5Util;
import com.weiliu.library.util.NetUtil;
import com.weiliu.library.util.Utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StreamProxyCache extends Thread implements Closeable {
	
	private static final String TAG = "Proxy";
	private static final boolean DEBUG = true;
	/**最多可缓存多少个（视频）文件*/
	private static final int MAX_CACHE_FILE_COUNT = 5;
	
	/**预加载长度*/
	private static final long PRELOAD_SIZE = 16 * 1024;
	/**更新索引的下载长度*/
	private static final int UPDATE_SIZE = 1024 * 1024;
	
	private static final int BUFFER_SIZE = Utility.FILE_STREAM_BUFFER_SIZE;
	
	
	/*********************************缓存文件头格式*****************************************/
	/**检验位*/
	private static final int CHECK_FLAG = 1510220931;
	private static final int CHECK_FLAG_SIZE = Utility.SIZE_OF_INT;
	
	/**头部区域的版本号*/
	private static final int VERSION = 1;
	private static final int VERSION_SIZE = Utility.SIZE_OF_INT;
	
	/**视频总长度存放区域*/
	private static final int LENGTH_SIZE = Utility.SIZE_OF_LONG;
	/**视频ETag存放区域*/
	private static final int ETAG_SIZE = 128;
	
	private static final int OTHER_INFO_SIZE = 880;
	
	/**最多可有多少对索引（每对索引由两个long组成）*/
	private static final int INDEX_PAIR_MAX_COUNT = 64;
	private static final int INDEX_SIZE = Utility.SIZE_OF_LONG;

	/**视频缓存文件头部区域大小 */
	private static final int HEAD_SIZE = CHECK_FLAG_SIZE + VERSION_SIZE 
			+ LENGTH_SIZE + ETAG_SIZE + OTHER_INFO_SIZE
			+ INDEX_PAIR_MAX_COUNT * 2 * INDEX_SIZE;
	/************************************************************************************/
	
	
	private static final Map<String, StreamProxyCache> INSTANCE_MAP =
			new HashMap<String, StreamProxyCache>();
	
	private String mDownloadUrl;
	private String mUrl;
	private Map<String, String> mHeaders;
	private String mCacheDir;
	private File mFile;
	private final boolean mOutOfMap;

    @Nullable
	private OnDownloadingUpdateListener mOnDownloadingUpdateListener;

	private volatile boolean mQuit;
    private volatile boolean mPause;
	
	@Nullable
	private DownloadInfo mCurrentDownloadInfo;
	private final BlockingQueue<DownloadInfo> mDownloadQueue =
	        new LinkedBlockingQueue<DownloadInfo>();
	
	public static synchronized StreamProxyCache getInstance(String url) {
		StreamProxyCache cache = INSTANCE_MAP.get(url);
		if (cache == null) {
			cache = new StreamProxyCache(url, false);
		}
		
		return cache;
	}

	/**
	 * 读取总长度信息
	 * @param url
	 * @return
	 */
	public static long readCacheLength(String url) {
		StreamProxyCache cache = new StreamProxyCache(url, true);
		long length = cache.readLength();
		cache.close();
		return length;
	}

	/**
	 * 取ETag信息
	 * @param url
	 * @return
	 */
	@Nullable
	public static String readCacheETag(String url) {
		StreamProxyCache cache = new StreamProxyCache(url, true);
		String eTag = cache.readETag();
		cache.close();
		return eTag;
	}

	/**
	 * 读取其他信息
	 * @param url
	 * @return
	 */
	@Nullable
	public static String readCacheOtherInfo(String url) {
		StreamProxyCache cache = new StreamProxyCache(url, true);
		String eTag = cache.readOtherInfo();
		cache.close();
		return eTag;
	}
	
	/**
	 * 是否已经全部缓冲完毕
	 * @param url
	 * @return
	 */
	public static boolean isDownloadedCompletely(String url) {
		StreamProxyCache cache = new StreamProxyCache(url, true);
		Area area = cache.findContinueDownloadArea(0);
		cache.close();
		return area != null && area.start >= area.end;
	}
	
	/**
	 * 在seek位置处是否有足够缓存用来播放
	 * @param url
	 * @param seekPos
	 * @return
	 */
	public static boolean isCacheAvailable(String url, long seekPos) {
		StreamProxyCache cache = new StreamProxyCache(url, true);
		long length = cache.readLength();
		if (length <= 0) {
			cache.close();
			return false;
		}
		
		long available = cache.getAvailableCount(seekPos);
		cache.close();
		
		long waitCount = Math.min(PRELOAD_SIZE, length - seekPos);
		return available >= waitCount;
	}

    /**
     * 停止所有的Cache下载
     * @param except 除了该Cache（如果不为null）
     */
    public static void stopAll(StreamProxyCache except) {
        Map<String, StreamProxyCache> map = new HashMap<String, StreamProxyCache>(INSTANCE_MAP);
        for (Entry<String, StreamProxyCache> entry : map.entrySet()) {
            StreamProxyCache cache = entry.getValue();
            if (cache != null && cache != except) {
                cache.stopDownload();
            }
        }
    }
	
	/**
	 * 关闭所有Cache
	 * @param except 除了该Cache（如果不为null）
	 */
	public static void closeAll(StreamProxyCache except) {
		Map<String, StreamProxyCache> map = new HashMap<String, StreamProxyCache>(INSTANCE_MAP);
		for (Entry<String, StreamProxyCache> entry : map.entrySet()) {
			StreamProxyCache cache = entry.getValue();
			if (cache != null && cache != except) {
				cache.close();
			}
		}
	}
	
	private StreamProxyCache(String url, boolean outOfMap) {
		super("StreamProxyCache " + url);
        mUrl = url;
        mDownloadUrl = url;
        mOutOfMap = outOfMap;
		mCacheDir = RootApplication.getInstance().getCacheDir() + "/videocache";

		mFile = generateFile();
		
		if (!outOfMap) {
			INSTANCE_MAP.put(url, this);
            start();
        }
	}

    public void setDownloadUrl(String downloadUrl) {
        mDownloadUrl = downloadUrl;
    }

    public void setHeaders(Map<String, String> headers) {
        mHeaders = headers;
    }

    public void setOnDownloadingUpdateListener(@Nullable OnDownloadingUpdateListener listener) {
        mOnDownloadingUpdateListener = listener;

        if (listener == null) {
            return;
        }

        long length = readLength();
        if (length != 0) {
            listener.onDownloadingUpdate(mUrl, readCurrentDownloadLength(), length);
        }
    }

    /**
	 * 校验缓存文件，如果不存在或者格式非法，则执行修复。
	 * @return 校验或者修复成功，返回true；否则返回false，并意味着无法使用该缓存。
	 */
	public boolean checkCacheFile() {
		DataInputStream input = null;
		try {
			input = new DataInputStream(new FileInputStream(mFile));
            return input.readInt() == CHECK_FLAG || resetCacheFile();
        } catch (RuntimeException e) {
//			printStackTrace(e);
            return resetCacheFile();
        } catch (Exception e) {
//			printStackTrace(e);
			return resetCacheFile();
		} finally {
			Utility.close(input);
		}
	}
	
	public boolean resetCacheFile() {
		DataOutputStream output = null;
		try {
			output = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(mFile), HEAD_SIZE));
			output.writeInt(CHECK_FLAG);
			output.writeInt(VERSION);
			output.writeLong(0);
			byte[] etagInfo = new byte[ETAG_SIZE];
			output.write(etagInfo);
			byte[] otherInfo = new byte[OTHER_INFO_SIZE];
			output.write(otherInfo);
			byte[] indexBytes = new byte[INDEX_PAIR_MAX_COUNT * 2 * INDEX_SIZE];
			Arrays.fill(indexBytes, (byte) -1);
			output.write(indexBytes);
			return true;
		} catch (FileNotFoundException e) {
			printStackTrace(e);
		} catch (IOException e) {
			printStackTrace(e);
		} finally {
			Utility.close(output);
		}
		
		return false;
	}
	
	/**
	 * 写入ETag信息到缓存中
	 * @param eTag
	 * @return 是否成功写入
	 */
	public boolean writeETag(String eTag) {
        if (!checkCacheFile()) {
            return false;
        }

		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(mFile, "rw");	//SUPPRESS CHECKSTYLE
            int skip = CHECK_FLAG_SIZE + VERSION_SIZE + LENGTH_SIZE;
            if (file.skipBytes(skip) != skip) {
                return false;
            }
            file.writeUTF(eTag);
			return true;
		} catch (FileNotFoundException e) {
			printStackTrace(e);
		} catch (IOException e) {
			printStackTrace(e);
		} finally {
			Utility.close(file);
		}
		
		return false;
	}
	
	/**
	 * 读取ETag信息
	 * @return
	 */
	@Nullable
	public String readETag() {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(mFile, "r");	//SUPPRESS CHECKSTYLE
            int skip = CHECK_FLAG_SIZE + VERSION_SIZE + LENGTH_SIZE;
            return file.skipBytes(skip) != skip ? null : file.readUTF();
        } catch (FileNotFoundException e) {
			printStackTrace(e);
		} catch (IOException e) {
			printStackTrace(e);
		} finally {
			Utility.close(file);
		}
		
		return null;
	}
	
	/**
	 * 写入总长度信息到缓存中
	 * @param length
	 * @return 是否成功写入
	 */
	public boolean writeLength(long length) {
        if (!checkCacheFile()) {
            return false;
        }

        StatFs fs = new StatFs(mFile.getAbsolutePath());
        //noinspection deprecation
        long usable = ((long) fs.getAvailableBlocks()) * fs.getBlockSize();
        if (usable < (length << 1)) {
            return false;
        }

		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(mFile, "rw");
            int skip = CHECK_FLAG_SIZE + VERSION_SIZE;
            if (file.skipBytes(skip) != skip) {
                return false;
            }
            file.writeLong(length);
			file.setLength(HEAD_SIZE + length);
			return true;
		} catch (FileNotFoundException e) {
			printStackTrace(e);
		} catch (IOException e) {
			printStackTrace(e);
		} finally {
			Utility.close(file);
		}
		
		return false;
	}
	
	/**
	 * 读取总长度信息
	 * @return
	 */
	public long readLength() {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(mFile, "r");
            int skip = CHECK_FLAG_SIZE + VERSION_SIZE;
            return file.skipBytes(skip) != skip ? 0 : file.readLong();
        } catch (FileNotFoundException e) {
			printStackTrace(e);
		} catch (IOException e) {
			printStackTrace(e);
		} finally {
			Utility.close(file);
		}
		
		return 0;
	}
	
	/**
	 * 写入其他信息到缓存中
	 * @param otherInfo
	 * @return 是否成功写入
	 */
	public boolean writeOtherInfo(String otherInfo) {
        if (!checkCacheFile()) {
            return false;
        }

		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(mFile, "rw");	//SUPPRESS CHECKSTYLE
            int skip = CHECK_FLAG_SIZE + VERSION_SIZE + LENGTH_SIZE + ETAG_SIZE;
            if (file.skipBytes(skip) != skip) {
                return false;
            }
            file.writeUTF(otherInfo);
			return true;
		} catch (FileNotFoundException e) {
			printStackTrace(e);
		} catch (IOException e) {
			printStackTrace(e);
		} finally {
			Utility.close(file);
		}
		
		return false;
	}
	
	/**
	 * 读取其他信息
	 * @return
	 */
	@Nullable
	public String readOtherInfo() {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(mFile, "r");
            int skip = CHECK_FLAG_SIZE + VERSION_SIZE + LENGTH_SIZE + ETAG_SIZE;
            return file.skipBytes(skip) != skip ? null : file.readUTF();
        } catch (FileNotFoundException e) {
			printStackTrace(e);
		} catch (IOException e) {
			printStackTrace(e);
		} finally {
			Utility.close(file);
		}
		
		return null;
	}

    public void pauseDownload() {
        mPause = true;
    }

    public void resumeDownload() {
        mPause = false;
    }

    public synchronized void seekTo(long seekPos) {
        if (mQuit) {
            return;
        }

        boolean needDownload = true;
        if (mCurrentDownloadInfo != null) {
			DownloadInfo currentDownloadInfo = mCurrentDownloadInfo;
            Area downloadArea = findContinueDownloadArea(seekPos);
            if (downloadArea != null) {
                //seekPos后续内容都已经下载完了，不需要新开下载，继续current下载吧
                if (downloadArea.start < seekPos) {
                    logDebug("content after seekPos (" + seekPos + ") has downloaded completely!");

                    needDownload = false;
                } else if (currentDownloadInfo.seek < seekPos	//当前下载位置在seekPos前面而且离得不远，很快能赶上
                        && seekPos - currentDownloadInfo.seek < PRELOAD_SIZE) {
                    logDebug("current download seekPos (" + currentDownloadInfo.seek + ')'
                            + " is near needed seekPos(" + seekPos + ')');

                    needDownload = false;
                }
            }
        }

        if (needDownload) {
            stopDownload();     // 停止现有的下载，尽快腾出资源和带宽给新的下载

            DownloadInfo downloadInfo = new DownloadInfo();
            downloadInfo.seek = seekPos;
            mDownloadQueue.add(downloadInfo);
            logDebug("post new download task seekpos(" + seekPos + ')');
        }
    }

    /**
     * 强制下载（即使在不播放的情况下）
     * @param listener 下载进度监听
     */
    public void forceDownload(OnDownloadingUpdateListener listener) {
        setOnDownloadingUpdateListener(listener);

        synchronized (this) {
            if (mCurrentDownloadInfo == null && mDownloadQueue.peek() == null) {
                seekTo(0);
            }
        }
    }

    /**
     * 停止下载
     */
    public void stopDownload() {
        synchronized (this) {
            mDownloadQueue.clear();
            if (mCurrentDownloadInfo != null) {
                mCurrentDownloadInfo.stopped = true;
            }
        }
    }

    /**
	 * 向客户端（播放器）发送数据，如果还没有下载，则阻塞等待下载数据
	 * @param proxy 客户端（播放器）代理
	 * @param seekPos
	 */
	public void sendData(
            @NonNull StreamProxyService.Proxy proxy, long seekPos, long count, @Nullable String responseHeaders) {
        seekTo(seekPos);

        Socket clientSocket = proxy.socket;
		InputStream input = null;
		try {
            input = new FileInputStream(mFile);
            long skipCount = HEAD_SIZE + seekPos;
            if (input.skip(skipCount) != skipCount) {
                Log.e(TAG, "sendData skip count error! ");
                return;
            }

			OutputStream output = clientSocket.getOutputStream();

			while (!proxy.stopped && !Thread.currentThread().isInterrupted()
					&& !clientSocket.isOutputShutdown()) {
				if (seekPos >= readLength() || count <= 0) {
					break;
				}
				
				long available = getAvailableCount(seekPos);
				long waitCount = Math.min(PRELOAD_SIZE, count);
				if (available < waitCount) {    //等待下载量
                    long dis = waitCount - available;
                    final long v = 1000;    //假设1毫秒下载1000字节（1秒下载1M左右）
                    final long minWaitTime = 500;
                    long time = Math.max(dis / v, minWaitTime);
//					logDebug("waiting for available: time=" + time + ", count=" + dis);
                    Thread.sleep(time);
                } else {	//开始响应
					// 写入response头信息，只用写一次
					if (responseHeaders != null) {
						output.write(responseHeaders.getBytes(Utility.UTF8));

						logDebug("response:" + clientSocket);
						logDebug(responseHeaders);

						responseHeaders = null;
					}
					long sendCount = send(proxy, output, count, input, available);
					count -= sendCount;
                    seekPos += sendCount;
				}
			}
			
		} catch (IOException e) {
			logDebug("IOException: " + e.getMessage() + ", socket=" + clientSocket);
			printStackTrace(e);
		} catch (InterruptedException e) {
			logDebug("sendData Interrupted");
		} finally {
			Utility.close(input);
		}
	}
	
	private long send(@NonNull StreamProxyService.Proxy proxy, @NonNull OutputStream output, long count,
                      @NonNull InputStream input, long available)
			throws IOException {
		long sendCount = 0;
		byte[] buf = new byte[BUFFER_SIZE];
		long num = Math.min(available, count);
		
		while (!proxy.stopped && num > 0 && !Thread.currentThread().isInterrupted()) {
			int len = input.read(buf, 0, (int) Math.min(buf.length, num));
			if (len == -1) {
				logDebug("len == -1");
				break;
			}
			output.write(buf, 0, len);
			sendCount += len;
			num -= len;
		}
		
		return sendCount;
	}
	
	@Override
	public void run() {
//		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (!mQuit) {
        	try {
        		final long sleepTime = 200;
				sleep(sleepTime);
			} catch (InterruptedException e) {
				continue;
			}
        	
        	DownloadInfo downloadInfo;
        	
        	synchronized (this) {
        		if (mQuit) {
        			break;
        		}

                if (mPause) {
                    continue;
                }

                downloadInfo = mDownloadQueue.poll();
        		if (downloadInfo == null) {
        			continue;
        		}
        		
            	mCurrentDownloadInfo = downloadInfo;
			}
			
            logDebug("need download seekPos = " + downloadInfo.seek);
            
			Area downloadArea;
			while (!mQuit && !downloadInfo.stopped
					&& (downloadArea = findContinueDownloadArea(downloadInfo.seek)) != null	//SUPPRESS CHECKSTYLE
					&& downloadArea.start < downloadArea.end) {
                if (mPause || !NetUtil.enable(RootApplication.getInstance())) {
                    try {
                        final long sleepTime = 200;
                        sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                startDownload(downloadInfo, downloadArea);
            }

            synchronized (this) {
				mCurrentDownloadInfo = null;
			}
        }
        logDebug("StreamProxyCache thread quit.");
	}
	
	@Override
	public void close() {
		if (!mOutOfMap) {
			INSTANCE_MAP.remove(mUrl);

            mQuit = true;
            stopDownload();
            interrupt();
        }
	}
	
	private void startDownload(@NonNull DownloadInfo downloadInfo, @NonNull Area downloadArea) {
		logDebug("startDownload: " + downloadArea.start + " -> " + downloadArea.end);
		RandomAccessFile file = null;
		HttpURLConnection connection = null;
		InputStream input = null;
		try {
			file = new RandomAccessFile(mFile, "rw");
			
			URL uri = new URL(mDownloadUrl);
			connection = (HttpURLConnection) uri.openConnection();
			connection.setRequestMethod("GET");
			if (mHeaders != null && !mHeaders.isEmpty()) {
				for (Entry<String, String> entry : mHeaders.entrySet()) {
					connection.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

            if (downloadArea.start >= 0) {
                connection.setRequestProperty("Range",
                        "bytes=" + downloadArea.start + "-" + downloadArea.end);
            }
			connection.connect();
			
			int code = connection.getResponseCode();
			
			if (code == HttpURLConnection.HTTP_OK) {
				downloadArea.start = 0;
			} else if (code != HttpURLConnection.HTTP_PARTIAL) {
				throw new FileNotFoundException("url = " + mDownloadUrl + ", code = " + code);
			}
			file.seek(HEAD_SIZE + downloadArea.start);
			
			String enc = connection.getContentEncoding();
			input = connection.getInputStream();
//			if ("gzip".equalsIgnoreCase(enc)) {
//				input = new java.util.zip.GZIPInputStream(input);
//            }
			
			final long logInterval = 5 * DateUtils.SECOND_IN_MILLIS;
			long startTime = System.currentTimeMillis();
			long interval;
			
			boolean preload = true;
			
			final int bufSize = UPDATE_SIZE;
			byte[] buf = new byte[bufSize];
			
			long total = 0;
			long current = 0;
			long start = downloadArea.start;
			int offset = 0;
			int count;
			
			while (!mQuit && !mPause && !downloadInfo.stopped) {
				count = input.read(buf, offset, bufSize - offset);
				if (count == -1) {
					break;
				}
				
				total += count;
				current += count;
				offset += count;
				
				/*
				 * 第一次只装载PRELOAD_SIZE大小，从而尽快响应播放器。
				 * 后续必须达到bufSize，为防止反复写索引拖慢下载速度。
				 */
				if (offset == bufSize || (preload && offset >= PRELOAD_SIZE)) {
					preload = false;
					saveData(file, start, buf, 0, offset);
					start += offset;
					offset = 0;

					interval = System.currentTimeMillis() - startTime;
					if (interval >= logInterval) {
						logDebug("download start = " + downloadArea.start + ", total = " + total
								+ ", speed = " + (current / interval) + " kbps");
						current = 0;
						startTime = System.currentTimeMillis();
					}
				}
			}
			
			if (offset > 0) {
				saveData(file, start, buf, 0, offset);
//				start += offset;

				interval = System.currentTimeMillis() - startTime;
				logDebug("download start = " + downloadArea.start + ", total = " + total
						+ ", speed = " + (current / interval) + " kbps");
			}
			
			logDebug("startDownload: end");
			
			Utility.close(file);
			Utility.close(input);
			connection.disconnect();
        } catch (RuntimeException e) {
            printStackTrace(e);
        } catch (Exception e) {
			printStackTrace(e);
		} finally {
			Utility.close(file);
			Utility.close(input);
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	private void saveData(@NonNull RandomAccessFile file, long seek,
                          byte[] buf, int off, int count) throws IOException {
		file.write(buf, off, count);
		Area area = new Area();
		area.start = seek;
		area.end = seek + count;
		insertDownloadedArea(area);
	}
	
	/**
	 * 根据需要seek的位置，获取当前可用的（已下载的）数据量
	 * @param seekPos
	 * @return
	 */
	private long getAvailableCount(long seekPos) {
		List<Area> downloadedAreas = readDownloadedAreas();
		for (Area area : downloadedAreas) {
			if (area.start <= seekPos && seekPos < area.end) {
				return area.end - seekPos;
			}
		}
		return 0;
	}
	
	/**
	 * 根据需要seek的位置，寻找最佳断点续传区域，以保证seek位置能最快被下载。
	 * 如果seek位置已下载，则寻找其它未下载区域继续下载。
	 * @param seekPos seek位置
	 * @return 断点续传区域（起点和终点对）。
     *          若起点大于等于终点，则表示：该文件已全部下载完毕；若为null，则表示无法正常断点续传。
     *          若起点为-1，终点为0，则表示：连缓存文件都没建立，完全没开始下载，甚至连下载总长度都不知道。
	 */
	@Nullable
	private Area findContinueDownloadArea(long seekPos) {
		Area downloadArea = null;
		List<Area> downloadedAreas = readDownloadedAreas();
        if (downloadedAreas.isEmpty()) {    //连缓存文件都没建立，完全没开始下载，甚至连下载总长度都不知道
            return new Area(-1, 0);
        }

        Area lastArea = new Area();

        for (Area area : downloadedAreas) {
			if (lastArea.end < area.start) {	//找到未下载区   （ [lastEnd, start) 未下载）
				if (downloadArea == null) {
					downloadArea = new Area();
				}
				//该处可作为断点续传的候选区域
				downloadArea.start = lastArea.end;
				downloadArea.end = area.start;
				
				//如果seekPos刚好处于该区域，那此处刚好就是断点续传的最佳点（以保证seekPos处能及时下载并播放）
				if (seekPos >= lastArea.end && seekPos < area.start) {
					downloadArea.start = seekPos;
					return downloadArea;
				}
				
				//走到这步，表示无法找到上述最佳点。但该区是离seekPos之后最近的未下载区，应该优先下载，确保播放延续
				if (seekPos < lastArea.end) {
					return downloadArea;
				}
			}
			
			if (area.start == -1) {	//后续再无有效的下载位置索引
				long length = readLength();
				if (lastArea.end <= seekPos) {
					if (downloadArea == null) {
						downloadArea = new Area();
					}
					downloadArea.start = seekPos;
					downloadArea.end = length;
					
				} else if (lastArea.end < length || downloadArea == null) {
					//尾段没有完成下载，或者之前没找到断点续传的候选区域，那就使用尾段
					downloadArea = new Area();
					downloadArea.start = lastArea.end;
					downloadArea.end = length;
				}
				return downloadArea;
			}
			
			lastArea = area;
		}
		
		return downloadArea;
	}
	
	/**
	 * 在索引中插入一段下载区域。该操作会尽可能将区域索引合并，并且不会改变索引的顺序关系。
     * 插入之后，如果有监听，则通知下载进度发生改变。
	 * @param area
	 */
	private void insertDownloadedArea(@NonNull Area area) {
		if (area.start > area.end) {
			long temp = area.start;
			area.start = area.end;
			area.end = temp;
		}
		
		List<Area> currentAreas = readDownloadedAreas();
		List<Area> newAreas = new ArrayList<Area>();
		
		//上个扫描的已下载区域，为了逻辑连贯，初始化一个[0, 0]
		Area lastArea = new Area();
		newAreas.add(lastArea);
		int currentSize = currentAreas.size();
		for (int i = 0; i < currentSize; i++) {
			//当前扫描的已下载区域
			Area curArea = currentAreas.get(i);
			
			//检查未下载区域与插入区域是否有相交
			Area inp;
			if (curArea.start != -1) {
				inp = intersect(new Area(lastArea.end, curArea.start), area);
			} else {
				inp = intersect(new Area(lastArea.end, Long.MAX_VALUE), area);
			}
			
			if (inp != null) {
				//相交的起点  连接了  上个下载区域的终点
				if (inp.start == lastArea.end) {
					/*
					 * 相交的终点  还连接了  当前下载区域的起点，
					 * 这意味着这段未下载区域已经完全被填充成下载区域了，
					 * 所以应该执行两个下载区域的大合并
					 */
					if (inp.end == curArea.start) {
						lastArea.end = curArea.end;
						continue;
					} else {
						lastArea.end = inp.end;	//扩充上个下载区域的终点（到相交域的终点）
					}
					
				} else if (inp.end == curArea.start) {	//相交的终点  连接了  当前下载区域的起点
					curArea.start = inp.start;	//扩充当前下载区域的起点（到相交域的起点）
					
				} else {	//相交域与俩下载区域都不相连，则直接把相交域作为新的下载区域插入
					Area newArea = new Area(inp.start, inp.end);
					newAreas.add(newArea);
				}
			}
			
			if (curArea.start == -1) {
				break;
			}
			
			newAreas.add(curArea);
			lastArea = curArea;
		}
		
		Area firstNewArea = newAreas.get(0);
		//初始化时插入的[0, 0]如果未经任何处理，干掉它
		if (firstNewArea.start == 0 && firstNewArea.end == 0) {
			newAreas.remove(0);
		}

        if (writeDownloadedAreas(newAreas) && mOnDownloadingUpdateListener != null) {
            mOnDownloadingUpdateListener.onDownloadingUpdate(
                    mUrl, readCurrentDownloadLength(), readLength());
        }
    }
	
	/**
	 * 求俩区域的相交区
	 * @param area1
	 * @param area2
	 * @return 如果没有相交，返回null；否则，返回相交区
	 */
	private Area intersect(@NonNull Area area1, @NonNull Area area2) {
		if (area1.start > area1.end) {
			long temp = area1.start;
			area1.start = area1.end;
			area1.end = temp;
		}
		
		if (area2.start > area2.end) {
			long temp = area2.start;
			area2.start = area2.end;
			area2.end = temp;
		}
		
		Area area = new Area();
		area.start = Math.max(area1.start, area2.start);
		area.end = Math.min(area1.end, area2.end);
		
		if (area.start >= area.end) {
			return null;
		}
		return area;
	}

    private long readCurrentDownloadLength() {
        long current = 0;
        List<Area> areas = readDownloadedAreas();
        for (Area area : areas) {
            if (area.start == -1 || area.end == -1) {
                break;
            }
            current += area.end - area.start;
        }
        return current;
    }

    @NonNull
	private List<Area> readDownloadedAreas() {
		ArrayList<Area> areas = new ArrayList<Area>();
		DataInputStream input = null;
		try {
			BufferedInputStream bufferedInput = new BufferedInputStream(
					new FileInputStream(mFile), HEAD_SIZE);
			input = new DataInputStream(bufferedInput);
			if (input.readInt() != CHECK_FLAG) {
				return areas;
			}

            int skip = VERSION_SIZE + LENGTH_SIZE + ETAG_SIZE + OTHER_INFO_SIZE;
            if (input.skipBytes(skip) != skip) {
                return areas;
            }

            for (int i = 0; i < INDEX_PAIR_MAX_COUNT; i++) {
				Area area = new Area();
				area.start = input.readLong();
				area.end = input.readLong();
				areas.add(area);
			}
			input.close();
		} catch (FileNotFoundException e) {
			printStackTrace(e);
		} catch (IOException e) {
			printStackTrace(e);
		} finally {
			Utility.close(input);
		}
		
		return areas;
	}
	
	private boolean writeDownloadedAreas(@NonNull List<Area> areas) {
        if (!checkCacheFile()) {
            return false;
        }

        RandomAccessFile file = null;
        DataOutputStream output = null;
        try {
            file = new RandomAccessFile(mFile, "rw");
            int skip = CHECK_FLAG_SIZE + VERSION_SIZE + LENGTH_SIZE + ETAG_SIZE + OTHER_INFO_SIZE;
            if (file.skipBytes(skip) != skip) {
                return false;
            }

            ByteArrayOutputStream bufferOut = new ByteArrayOutputStream(HEAD_SIZE);
            output = new DataOutputStream(bufferOut);
            int size = areas.size();
            for (int i = 0; i < INDEX_PAIR_MAX_COUNT; i++) {
                if (i < size) {
                    Area area = areas.get(i);
                    output.writeLong(area.start);
                    output.writeLong(area.end);
                } else {
                    output.writeLong(-1L);
                    output.writeLong(-1L);
                }
            }

            file.write(bufferOut.toByteArray());

            return true;
        } catch (FileNotFoundException e) {
            printStackTrace(e);
        } catch (IOException e) {
            printStackTrace(e);
        } finally {
            Utility.close(output);
            Utility.close(file);
        }

        return false;
    }

    @NonNull
	private File generateFile() {
		String name = Md5Util.MD5Encode(mUrl);
		File file = new File(mCacheDir, name);
		if (!file.exists()) {
			File dir = file.getParentFile();
            FileUtil.mkdirs(dir);
            deleteOldCaches(dir);
            FileUtil.createNewFile(file);
		}
		
		return file;
	}
	
	/**
	 * 按照修改时间排序，删除缓存文件，直到缓存个数保持在MAX_CACHE_FILE_COUNT以内
	 * @param dir
	 */
	private void deleteOldCaches(@NonNull File dir) {
		File[] allFiles = dir.listFiles();
		if (allFiles == null) {
			return;
		}

        int count = MAX_CACHE_FILE_COUNT;

		if (allFiles.length >= count) {
			Arrays.sort(allFiles, new Comparator<File>() {

				@Override
				public int compare(@NonNull File lhs, @NonNull File rhs) {
					long lhsTime = lhs.lastModified();
					long rhsTime = rhs.lastModified();
					if (lhsTime == rhsTime) {
						return 0;
					}
					return lhsTime < rhsTime ? -1 : 1;
				}
			});
			
			int deleteCount = allFiles.length - count + 1;
			for (int i = 0; i < deleteCount; i++) {
				if (!allFiles[i].delete()) {
					Log.w(TAG, "cache: " + allFiles[i].getName() + " cannot delete!");
				} else {
					logDebug("delete cache: " + allFiles[i].getName());
				}
			}
		}
	}
	
	private static void logDebug(String log) {
		if (DEBUG) {
			Log.d(TAG, log);
		}
	}
	
	private static void printStackTrace(@NonNull Throwable e) {
		if (DEBUG) {
			e.printStackTrace();
		} else {
			Log.w(TAG, "" + e.getMessage());
		}
	}
	
	private static class DownloadInfo {
		public long seek;
        public volatile boolean stopped;
	}
	
	public static class Area {
		public long start;
		public long end;
		
		public Area() {
			
		}
		
		public Area(long s, long e) {
			start = Math.min(s, e);
			end = Math.max(s, e);
		}
	}

    /**
     * 下载进度更新通知
     */
    public interface OnDownloadingUpdateListener {

        /**
         * 下载进度更新
         * @param url 下载地址
         * @param current 当前下载长度（注意，是所有断续下载区间的总和）
         * @param length 总下载长度
         */
        void onDownloadingUpdate(String url, long current, long length);
    }
}
