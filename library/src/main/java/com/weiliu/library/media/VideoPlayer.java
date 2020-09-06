package com.weiliu.library.media;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.weiliu.library.R;
import com.weiliu.library.proxy.IStreamProxyService;
import com.weiliu.library.proxy.StreamProxyService;
import com.weiliu.library.widget.RatioRelativeLayout;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VideoPlayer extends RatioRelativeLayout implements OnPreparedListener,
        OnInfoListener, OnErrorListener, OnCompletionListener, OnSeekCompleteListener, MediaControllerCompat.MediaPlayerControl {

	private static final long CALLBACK_INTERVAL = 300;

	private VideoViewCompat mVideoView;
    private MediaControllerCompat mMediaController;
    @Nullable
    private Uri mUri;
    @Nullable
    @SuppressWarnings("FieldCanBeLocal")
    private Map<String, String> mHeaders;

    private OnCompletionListener mOnCompletionListener;
    private OnErrorListener mOnErrorListener;

    private ImageView mFullScreenButton;
    private boolean mFullScreenMode;
    private OnFullScreenChangedListener mOnFullScreenChangedListener;
    private boolean mBuffering;
    private boolean mSeeking;
    private int mLastErrorPos = -1;

    private boolean mEnableProxy = true;
    private IStreamProxyService mBinder;
    private boolean mCallBind;
    private boolean mBound;
    
    private boolean mHasDetached;
    private boolean mHasReleased;
    
    private ViewGroup mPreloadFrame;
    private ViewGroup mBufferFrame;
    private ViewGroup mErrorFrame;
    private ViewGroup mCoverFrame;
    
    private Activity mActivity;
    private int mActivityOrientation;
    private boolean mActivityOrientationChanged;
    
    private OnPlayPositionChangedListener mPlayPositionChangedListener;
    
	public VideoPlayer(Context context, AttributeSet attrs) {
		super(context, attrs);
		mActivity = (Activity) context;
        setRoundRadius(0);
        /*
         * 魅族MX5上 MediaPlayer setDataSource只要使用header就会报空指针：
         * java.lang.NullPointerException: Attempt to invoke interface method
         * 'java.util.Map com.mediatek.common.media.IOmaSettingHelper.setSettingHeader(
         * android.content.Context, android.net.Uri, java.util.Map)' on a null object reference
         */
        boolean isMeizuMX5 = TextUtils.equals(Build.MANUFACTURER, "Meizu") && TextUtils.equals(Build.PRODUCT, "MX5");
        setEnableProxy(!isMeizuMX5);
	}
	
	@Override
	protected void onFinishInflate() {
        super.onFinishInflate();
		mVideoView = (VideoViewCompat) findViewById(R.id.video);
        mVideoView.setMediaPlayerControl(this);
        mMediaController = (MediaControllerCompat) findViewById(R.id.media_controller);
        mVideoView.setMediaController(mMediaController);

        mVideoView.setOnPreparedListener(this);
//        mVideoView.setOnInfoListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnErrorListener(this);
        
        mPreloadFrame = (ViewGroup) findViewById(R.id.frame_preload);
        mPreloadFrame.setVisibility(VISIBLE);
        mBufferFrame = (ViewGroup) findViewById(R.id.frame_buffer);
        mBufferFrame.setVisibility(GONE);
        mErrorFrame = (ViewGroup) findViewById(R.id.frame_error);
        mErrorFrame.setVisibility(GONE);
        mCoverFrame = (ViewGroup) findViewById(R.id.frame_cover);
        
        mFullScreenButton = (ImageView) findViewById(R.id.fullscreen);
        mFullScreenButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				fullScreen(!mFullScreenMode);
			}
		});
	}

    /**
     * 控制器是否处于可用状态（初始视频未prepared时不可用）
     * @return
     */
    public boolean isControllerEnabled() {
        return mMediaController.isEnabled();
    }

    /**
	 * 进入或者退出全屏模式，同时切换横屏模式
	 * @param enable true表示进入全屏，false表示退出全屏
	 */
	public void fullScreen(boolean enable) {
		if (mFullScreenMode == enable) {
			return;
		}
		
		mFullScreenMode = enable;
		
		//按钮状态切换
		mFullScreenButton.setImageResource(
				enable ? R.drawable.selector_video_smallscreen
				: R.drawable.selector_video_fullscreen);
		
		//隐藏标题栏
		if (mActivity instanceof AppCompatActivity) {
            AppCompatActivity actionBarActivity = (AppCompatActivity) mActivity;
			ActionBar actionBar = actionBarActivity.getSupportActionBar();
            if (actionBar != null) {
                if (mFullScreenMode) {
                    actionBar.hide();
                } else {
                    actionBar.show();
                }
            }
		}

		//切换横屏
		if (mFullScreenMode) {
			mActivityOrientation = mActivity.getRequestedOrientation();
			mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			mActivityOrientationChanged = true;
		} else {
			if (mActivityOrientationChanged) {
				mActivity.setRequestedOrientation(mActivityOrientation);
				mActivityOrientationChanged = false;
			}
		}

        Window window = mActivity.getWindow();

        //全屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = window.getDecorView();
            int systemUiVisibility = getSystemUiVisibility();
            if (mFullScreenMode) {
                decorView.setSystemUiVisibility(systemUiVisibility
                        | SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                decorView.setSystemUiVisibility(systemUiVisibility
                        & ~SYSTEM_UI_FLAG_FULLSCREEN & ~SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        } else {
            WindowManager.LayoutParams attrs = window.getAttributes();
            if (mFullScreenMode) {
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                window.setAttributes(attrs);
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            } else {
                attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.setAttributes(attrs);
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
        }

        if (mOnFullScreenChangedListener != null) {
            mOnFullScreenChangedListener.onFullScreenChanged(this, mFullScreenMode);
        }
    }
	
	/**
	 * 当前是否在全屏状态
	 * @return
	 */
	public boolean isFullScreen() {
		return mFullScreenMode;
	}

    /**
     * 设置全屏状态转变的监听
     * @param listener
     */
    public void setOnFullScreenChangedListener(OnFullScreenChangedListener listener) {
        mOnFullScreenChangedListener = listener;
    }

    public void setVideoPath(@Nullable String path) {
        setVideoURI(path != null ? Uri.parse(path) : null);
    }

    public void setVideoURI(Uri uri) {
    	setVideoURI(uri, null);
    }

    public void setVideoURI(@Nullable Uri uri, @Nullable Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;

    	if (uri == null) {
    		onError(null, 0, 0);
    	} else {
    		start();	//预置目标状态为播放状态
    		
    		String scheme = uri.getScheme();
            if (scheme == null || scheme.equals("file")) {
            	setVideoURIWithoutProxy(uri.toString(), headers);
            } else {
//            	final String hostKey = "Host";
//            	HostAnalyzeResult result = HostManager.analyze(uri.toString());
//            	if (result != null) {
//            		uri = Uri.parse(result.replacedUrl);
//            		if (headers == null) {
//            			headers = new HashMap<String, String>();
//            		}
//            		if (!headers.containsKey(hostKey)) {
//            			headers.put(hostKey, result.host);
//            		}
//            	}
            	
            	if (mEnableProxy) {
            		setVideoURIWithProxy(uri.toString(), headers);
            	} else {
            		setVideoURIWithoutProxy(uri.toString(), headers);
            	}
            }
    	}
    }
    
    private void setVideoURIWithProxy(final String url, final Map<String, String> headers) {
    	mHasReleased = false;
    	bindProxyService();
    	
    	Thread thread = new Thread("video_player_wait_for_bind_thread") {
			
			@Override
			public void run() {
				
				try {
                    final long sleepTime = 100;
                    final int maxCount = 15;
                    int count = 0;

					while (mEnableProxy && !mBound && !mHasReleased) {
                        if (count++ >= maxCount) {  // 超过15次（即1.5秒），不等了，转非代理模式
                            mEnableProxy = false;
                            break;
                        }
                        Thread.sleep(sleepTime);
                    }
					
					setVideoURIWithoutProxyAsync(url, headers);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		
		thread.start();
    }
    
    private void setVideoURIWithoutProxyAsync(final String url, final Map<String, String> headers) {
    	// 使用post，以确保在Main Thread中执行
    	post(new Runnable() {

            @Override
            public void run() {
                if (!mHasReleased) {
                    try {
                        String playUrl;
                        if (mEnableProxy) {
                            // 把真实请求地址包装成path，塞到代理地址中
                            playUrl = String.format(Locale.US, "http://%s/%s",
                                    mBinder.getAddress(), Uri.encode(url));
                        } else {
                            playUrl = url;
                        }
                        setVideoURIWithoutProxy(playUrl, headers);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    
    private void setVideoURIWithoutProxy(String url, Map<String, String> headers) {
    	mHasReleased = false;
		mVideoView.setVideoURI(Uri.parse(url), headers);
		reset();
    }
    
    public void setEnableProxy(boolean enable) {
    	mEnableProxy = enable;
    }
    
    public void reset() {
        mFullScreenButton.setEnabled(true);
    	mPreloadFrame.setVisibility(VISIBLE);
        mBufferFrame.setVisibility(GONE);
        mErrorFrame.setVisibility(GONE);
    }

    @Override
    public void start() {
        mVideoView.start();
        if (!mHasDetached) {
            removeCallbacks(mStateCallBack);
            postDelayed(mStateCallBack, CALLBACK_INTERVAL);
        }

        if (mBound && mUri != null) {
            try {
                mBinder.resumeProxy(mUri.toString());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void pause() {
    	mVideoView.pause();
    	removeCallbacks(mStateCallBack);
    }

    public void pauseAndStopDownload() {
        pause();

        if (mBound && mUri != null) {
            try {
                mBinder.pauseProxy(mUri.toString());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopPlayback() {
    	mVideoView.stopPlayback();
    	removeCallbacks(mStateCallBack);
    }

    public void destroy() {
        release(true);
    }
    
    private void release(boolean destroy) {
    	mVideoView.release(true);
    	removeCallbacks(mStateCallBack);
    	
    	mHasReleased = true;
    	unbindProxyService();
    	
    	if (destroy) {
    		mVideoView.destroy();
    		mMediaController.destroy();
    	}
    }

    @Override
    public int getDuration() {
        return mVideoView.getDuration();
    }

    @Override
    public void getCurrentPosition(MediaControllerCompat.OnResultListener<Integer> onPositionResult) {
        mVideoView.getCurrentPosition(onPositionResult);
    }

    @Override
    public void seekTo(int msec) {
    	mVideoView.seekTo(msec);
    	
    	mSeeking = true;

    	if (isPlaying()) {
    		// 延后执行，以避免每次“正在加载”闪一下就消失
    		final long delayMillis = 500;
    		postDelayed(mShowBufferingAction, delayMillis);
    	}
    }

    @Override
    public boolean isPlaying() {
        return mVideoView.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return mVideoView.getBufferPercentage();
    }

    @Override
    public boolean canPause() {
        return mVideoView.canPause();
    }

    @Override
    public boolean canSeekBackward() {
        return mVideoView.canSeekBackward();
    }

    @Override
    public boolean canSeekForward() {
        return mVideoView.canSeekForward();
    }

    @Override
    public int getAudioSessionId() {
        return mVideoView.getAudioSessionId();
    }

    @Override
    public boolean isGoingToPlay() {
        return mVideoView.isGoingToPlay();
    }

    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        mOnCompletionListener = onCompletionListener;
    }

    /**
     * 设置错误监听
     * @param onErrorListener
     */
    public void setOnErrorListener(OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
    }

    /**
     * 添加加载界面（发生在获取视频基本信息的时候），若为空则表示移除
     * @param view
     */
    public void addPreloadView(@Nullable View view) {
    	mPreloadFrame.removeAllViews();
    	if (view != null) {
    		mPreloadFrame.addView(view);
    	}
    }
    
    /**
     * 添加加载界面（发生在获取视频基本信息的时候），若为空则表示移除
     * @param view
     * @param layoutParams
     */
    public void addPreloadView(@Nullable View view, RelativeLayout.LayoutParams layoutParams) {
    	mPreloadFrame.removeAllViews();
    	if (view != null) {
    		mPreloadFrame.addView(view, layoutParams);
    	}
    }
    
    /**
     * 添加加载界面（发生在获取视频基本信息的时候），若layoutRes为0则表示移除
     * @param layoutRes
     */
    public void addPreloadView(int layoutRes) {
    	mPreloadFrame.removeAllViews();
    	if (layoutRes != 0) {
    		inflate(mActivity, layoutRes, mPreloadFrame);
    	}
    }
    
    /**
     * 添加缓冲界面，若为空则表示移除
     * @param view
     */
    public void addBufferView(@Nullable View view) {
    	mBufferFrame.removeAllViews();
    	if (view != null) {
    		mBufferFrame.addView(view);
    	}
    }
    
    /**
     * 添加缓冲界面，若为空则表示移除
     * @param view
     * @param layoutParams
     */
    public void addBufferView(@Nullable View view, RelativeLayout.LayoutParams layoutParams) {
    	mBufferFrame.removeAllViews();
    	if (view != null) {
    		mBufferFrame.addView(view, layoutParams);
    	}
    }
    
    /**
     * 添加缓冲界面，若layoutRes为0则表示移除
     * @param layoutRes
     */
    public void addBufferView(int layoutRes) {
    	mBufferFrame.removeAllViews();
    	if (layoutRes != 0) {
    		inflate(mActivity, layoutRes, mBufferFrame);
    	}
    }
    
    /**
     * 添加错误界面，若为空则表示移除
     * @param view
     */
    public void addErrorView(@Nullable View view) {
    	mErrorFrame.removeAllViews();
    	if (view != null) {
    		mErrorFrame.addView(view);
    	}
    }
    
    /**
     * 添加错误界面，若为空则表示移除
     * @param view
     * @param layoutParams
     */
    public void addErrorView(@Nullable View view, RelativeLayout.LayoutParams layoutParams) {
    	mErrorFrame.removeAllViews();
    	if (view != null) {
    		mErrorFrame.addView(view, layoutParams);
    	}
    }
    
    /**
     * 添加错误界面，若layoutRes为0则表示移除
     * @param layoutRes
     */
    public void addErrorView(int layoutRes) {
    	mErrorFrame.removeAllViews();
    	if (layoutRes != 0) {
    		inflate(mActivity, layoutRes, mErrorFrame);
    	}
    }
    
    /**
     * 添加封面界面，若为空则表示移除。注意，VideoPlayer并不调节封面的隐藏与显示，请自行控制！
     * @param view
     */
    public void addCoverView(@Nullable View view) {
    	mCoverFrame.removeAllViews();
    	if (view != null) {
    		mCoverFrame.addView(view);
    	}
    }
    
    /**
     * 添加封面界面，若为空则表示移除。注意，VideoPlayer并不调节封面的隐藏与显示，请自行控制！
     * @param view
     * @param layoutParams
     */
    public void addCoverView(@Nullable View view, RelativeLayout.LayoutParams layoutParams) {
    	mCoverFrame.removeAllViews();
    	if (view != null) {
    		mCoverFrame.addView(view, layoutParams);
    	}
    }
    
    /**
     * 添加封面界面，若layoutRes为0则表示移除。注意，VideoPlayer并不调节封面的隐藏与显示，请自行控制！
     * @param layoutRes
     */
    public void addCoverView(int layoutRes) {
    	mCoverFrame.removeAllViews();
    	if (layoutRes != 0) {
    		inflate(mActivity, layoutRes, mCoverFrame);
    	}
    }

    /**
     * 控制封面的显示或者隐藏
     * @param visibility
     */
    public void setCoverVisibility(int visibility) {
        mCoverFrame.setVisibility(visibility);
    }
    
    /**
     * 设置播放进度监听
     * @param listener
     */
    public void setOnPlayPositionChangedListener(OnPlayPositionChangedListener listener) {
    	mPlayPositionChangedListener = listener;
    }

    /**
     * 在不播放的情况下，强制开始视频下载
     * @param url
     * @param  headers
     */
    public void forceDownload(String url, @Nullable Map<String, String> headers) {
        Context context = getContext();
        Intent intent = new Intent(context, StreamProxyService.class);
        intent.setAction(StreamProxyService.ACTION_FORCE_DOWNLOAD);
        intent.putExtra(StreamProxyService.EXTRA_DOWNLOAD_URL, url);
        if (headers != null) {
            intent.putExtra(StreamProxyService.EXTRA_DOWNLOAD_HEADERS, new HashMap<String, String>(headers));
        }
        context.startService(intent);
    }

    @Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (mFullScreenMode && mActivity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			int width = View.MeasureSpec.getSize(widthMeasureSpec);
			int height = View.MeasureSpec.getSize(heightMeasureSpec);
            Log.e("VideoPlayer", "width = " + width);
            Log.e("VideoPlayer", "height = " + height);
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
			heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
			float oldRatio = mRatio;
			mRatio = 0;
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			mRatio = oldRatio;
			return;
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mHasDetached = false;
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mHasDetached = true;
		removeCallbacks(mStateCallBack);
	}

	@Override
	public void onPrepared(@NonNull MediaPlayer mp) {
		if (mLastErrorPos != -1) {
			seekTo(mLastErrorPos);
			start();
			mLastErrorPos = -1;
		}
		mMediaController.show();
		
		mp.setOnInfoListener(this);
		mp.setOnSeekCompleteListener(this);
		
		mBufferFrame.setVisibility(GONE);
		mErrorFrame.setVisibility(GONE);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(mp);
        }

		mPreloadFrame.setVisibility(GONE);
		mBufferFrame.setVisibility(GONE);
		mErrorFrame.setVisibility(GONE);
//		fullScreen(false);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(mp, what, extra);
        }

        mLastErrorPos = mLastPlayPosition;

        release(false);
        mErrorFrame.setVisibility(VISIBLE);
        mPreloadFrame.setVisibility(GONE);
        mBufferFrame.setVisibility(GONE);
//        fullScreen(false);
        mFullScreenButton.setEnabled(false);
        mMediaController.show(0);

        return true;
    }

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		mPreloadFrame.setVisibility(GONE);
		boolean ret = true;
		switch (what) {
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			showBufferingUI();
			mBuffering = true;
			break;
			
		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			// 此接口每次回调完START就回调END, 若不加上判断就会出现缓冲图标一闪一闪的卡顿现象
			if (mVideoView.isPlaying()) {
				hideBufferingUI();
			}
			mBuffering = false;
			break;
			
		case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
			mErrorFrame.setVisibility(GONE);
			break;

		default:
			ret = false;
			break;
		}
        return ret;
	}
	
	@Override
	public void onSeekComplete(MediaPlayer mp) {
//		if (!mBuffering && isPlaying()) {
//			removeCallbacks(mShowBufferingAction);
//			hideBufferingUI();
//		}
		mSeeking = false;
	}

	@NonNull
    private Runnable mShowBufferingAction = new Runnable() {

		@Override
		public void run() {
			showBufferingUI();
		}
	};

	private void showBufferingUI() {
		mBufferFrame.setVisibility(View.VISIBLE);
		mErrorFrame.setVisibility(GONE);
	}
	
	private void hideBufferingUI() {
		removeCallbacks(mShowBufferingAction);
		mBufferFrame.setVisibility(View.GONE);
	}
	
	private void bindProxyService() {
        if (mCallBind) {
            return;
        }

        Intent intent = new Intent(getContext(), StreamProxyService.class);
        mCallBind = getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
	
	private void unbindProxyService() {
        if (!mCallBind) {
            return;
        }

        getContext().unbindService(mConnection);
        mCallBind = false;

        mBound = false;
	}
	
    @NonNull
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
        	mBinder = IStreamProxyService.Stub.asInterface(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
	
	private int mLastPlayPosition;
	@NonNull
    private Runnable mStateCallBack = new Runnable() {

		@Override
		public void run() {
			getCurrentPosition(mOnPositionResultListener);
			postDelayed(mStateCallBack, CALLBACK_INTERVAL);
		}
		
	};
	
	@Nullable
    private MediaControllerCompat.OnResultListener<Integer> mOnPositionResultListener
            = new MediaControllerCompat.OnResultListener<Integer>() {

		@Override
		public void onResult(Integer result) {
			int position = result;
			
			if (!mSeeking) {
				if (isPlaying()) {
					if (!mBuffering && mLastPlayPosition != position) {	//播放位置发生了变化，证明真正在播，没有在缓冲
						hideBufferingUI();
						mPreloadFrame.setVisibility(GONE);
					} else {
						showBufferingUI();
					}
				} else {	//不是播放的时候就不要显示缓冲了吧
					hideBufferingUI();
				}
			}
			
			if (mLastPlayPosition != position) {
				mLastPlayPosition = position;
				if (mPlayPositionChangedListener != null) {
					mPlayPositionChangedListener.onPlayPositionChanged(VideoPlayer.this, position);
				}
			}
			
		}
	};
	
	/**
	 * 播放进度监听
	 * @author qumiao
	 *
	 */
	public interface OnPlayPositionChangedListener {
		/**
		 * 播放进度发生改变
		 * @param player 播放器
		 * @param pos 播放的当前位置
		 */
		void onPlayPositionChanged(VideoPlayer player, int pos);
	}

    /**
     * 全屏监听
     */
    public interface OnFullScreenChangedListener {
        /**
         * 全屏状态发生改变
         * @param player 播放器
         * @param fullScreen 改变之后，是否全屏
         */
        void onFullScreenChanged(VideoPlayer player, boolean fullScreen);
    }
}
