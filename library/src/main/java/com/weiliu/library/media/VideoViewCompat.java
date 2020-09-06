package com.weiliu.library.media;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;
import java.util.Map;

public class VideoViewCompat extends SurfaceView implements MediaControllerCompat.MediaPlayerControl {
	@NonNull
	private static String TAG = "VideoView";
	// settable by the client
	private Uri mUri;
    private Map<String, String> mHeaders;

	// all possible internal states
	public static final int STATE_ERROR = -1;
	public static final int STATE_IDLE = 0;
	public static final int STATE_PREPARING = 1;
	public static final int STATE_PREPARED = 2;
	public static final int STATE_PLAYING = 3;
	public static final int STATE_PAUSED = 4;
	public static final int STATE_PLAYBACK_COMPLETED = 5;

	// mCurrentState is a VideoView object's current state.
	// mTargetState is the state that a method caller intends to reach.
	// For instance, regardless the VideoView object's current state,
	// calling pause() intends to bring the object to a target state
	// of STATE_PAUSED.
	private volatile int mCurrentState = STATE_IDLE;
	private volatile int mTargetState = STATE_IDLE;
	
	// All the stuff we need for playing and showing a video
	@Nullable
	private SurfaceHolder mSurfaceHolder;
	private MediaPlayer mMediaPlayer;
    private MediaControllerCompat.MediaPlayerControl mMediaPlayerControl;
	private int mAudioSession;
	private int mVideoWidth;
	private int mVideoHeight;
	private int mSurfaceWidth;
	private int mSurfaceHeight;
	private MediaControllerCompat mMediaController;
	private OnCompletionListener mOnCompletionListener;
	private MediaPlayer.OnPreparedListener mOnPreparedListener;
	private int mCurrentBufferPercentage;
	private OnErrorListener mOnErrorListener;
	private OnInfoListener mOnInfoListener;
	
	private volatile int mSeekWhenPrepared; // recording the seek position while
									// preparing
	private volatile int mDuration = -1;
	
//	private boolean mCanPause;
//	private boolean mCanSeekBack;
//	private boolean mCanSeekForward;
	
	private Handler mPlayHandler;
	private int mPlayThreadCount;
	
	private volatile boolean mHasDestroyed;

	public VideoViewCompat(Context context) {
		super(context);
		initVideoView();
	}

	public VideoViewCompat(@NonNull Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		initVideoView();
	}

	public VideoViewCompat(@NonNull Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initVideoView();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Log.i("@@@@", "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) +
		// ", "
		// + MeasureSpec.toString(heightMeasureSpec) + ")");

		int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
		int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
		if (mVideoWidth > 0 && mVideoHeight > 0) {

			int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
			int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
			int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
			int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

			if (widthSpecMode == MeasureSpec.EXACTLY
					&& heightSpecMode == MeasureSpec.EXACTLY) {
				// the size is fixed
				width = widthSpecSize;
				height = heightSpecSize;

				// for compatibility, we adjust size based on aspect ratio
				if (mVideoWidth * height < width * mVideoHeight) {
					// Log.i("@@@", "image too wide, correcting");
					width = height * mVideoWidth / mVideoHeight;
				} else if (mVideoWidth * height > width * mVideoHeight) {
					// Log.i("@@@", "image too tall, correcting");
					height = width * mVideoHeight / mVideoWidth;
				}
			} else if (widthSpecMode == MeasureSpec.EXACTLY) {
				// only the width is fixed, adjust the height to match aspect
				// ratio if possible
				width = widthSpecSize;
				height = width * mVideoHeight / mVideoWidth;
				if (heightSpecMode == MeasureSpec.AT_MOST
						&& height > heightSpecSize) {
					// couldn't match aspect ratio within the constraints
					height = heightSpecSize;
				}
			} else if (heightSpecMode == MeasureSpec.EXACTLY) {
				// only the height is fixed, adjust the width to match aspect
				// ratio if possible
				height = heightSpecSize;
				width = height * mVideoWidth / mVideoHeight;
				if (widthSpecMode == MeasureSpec.AT_MOST
						&& width > widthSpecSize) {
					// couldn't match aspect ratio within the constraints
					width = widthSpecSize;
				}
			} else {
				// neither the width nor the height are fixed, try to use actual
				// video size
				width = mVideoWidth;
				height = mVideoHeight;
				if (heightSpecMode == MeasureSpec.AT_MOST
						&& height > heightSpecSize) {
					// too tall, decrease both width and height
					height = heightSpecSize;
					width = height * mVideoWidth / mVideoHeight;
				}
				if (widthSpecMode == MeasureSpec.AT_MOST
						&& width > widthSpecSize) {
					// too wide, decrease both width and height
					width = widthSpecSize;
					height = width * mVideoHeight / mVideoWidth;
				}
			}
		} else {// SUPPRESS CHECKSTYLE
			// no size yet, just adopt the given spec sizes
		}
		setMeasuredDimension(width, height);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
		super.onInitializeAccessibilityEvent(event);
		event.setClassName(VideoViewCompat.class.getName());
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
		super.onInitializeAccessibilityNodeInfo(info);
		info.setClassName(VideoViewCompat.class.getName());
	}

	public int resolveAdjustedSize(int desiredSize, int measureSpec) {
		return getDefaultSize(desiredSize, measureSpec);
	}

	@SuppressWarnings("deprecation")
	private void initVideoView() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		getHolder().addCallback(mSHCallback);
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
		mCurrentState = STATE_IDLE;
		mTargetState = STATE_IDLE;
	}

    public void setMediaPlayerControl(MediaControllerCompat.MediaPlayerControl mediaPlayerControl) {
        mMediaPlayerControl = mediaPlayerControl;
    }

	/**
     * Sets video path.
     *
     * @param path the path of the video.
     */
	public void setVideoPath(String path) {
		setVideoURI(Uri.parse(path));
	}

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    public void setVideoURI(Uri uri, Map<String, String> headers) {
    	if (mHasDestroyed) {
			return;
		}
    	
    	mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

	public void stopPlayback() {
		if (mHasDestroyed) {
			return;
		}

		if (mPlayHandler != null) {
			mPlayHandler.sendEmptyMessage(MSG_STOP);
		}
	}
	
	private void openVideo() {
		if (mHasDestroyed) {
			return;
		}
		
		Runnable callBack = new Runnable() {
			
			@Override
			public void run() {
				if (mUri == null || mSurfaceHolder == null) {
					// not ready for playback just yet, will try again later
					return;
				}
				
				// Tell the music playback service to pause
				// TODO: these constants need to be published somewhere in the
				// framework.
				Intent i = new Intent("com.android.music.musicservicecommand");
				i.putExtra("command", "pause");
				getContext().sendBroadcast(i);
				
				HandlerThread thread = new HandlerThread("VideoViewCompat Play Thread #"
		    			+ (mPlayThreadCount++));
		    	thread.start();
		    	
				try {
			    	mMediaPlayer = new MediaPlayer();
			    	mPlayHandler = new PlayHandler(thread.getLooper(), new MainHandler(), 
			    			mMediaPlayer, mStateCallBack);
			    	
					if (mAudioSession != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
						setMediaPlayerAudioSessionId(mAudioSession);
					}
					mMediaPlayer.setOnPreparedListener(mPreparedListener);
					mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
					mMediaPlayer.setOnCompletionListener(mCompletionListener);
					mMediaPlayer.setOnErrorListener(mErrorListener);
					mMediaPlayer.setOnInfoListener(mInfoListener);
					mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
					mCurrentBufferPercentage = 0;
					setMediaPlayerDataSource(mUri, mHeaders);
					mMediaPlayer.setDisplay(mSurfaceHolder);
					mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					mMediaPlayer.setScreenOnWhilePlaying(true);

                    mPlayHandler.sendEmptyMessage(MSG_PREPARE);
                    // we don't set the target state here either, but preserve the
					// target state that was there before.
					mCurrentState = STATE_PREPARING;
					attachMediaController();
				} catch (IOException ex) {
					Log.w(TAG, "Unable to open content: " + mUri, ex);
					mCurrentState = STATE_ERROR;
					mTargetState = STATE_ERROR;
					mErrorListener.onError(mMediaPlayer,
							MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
				} catch (IllegalArgumentException ex) {
					Log.w(TAG, "Unable to open content: " + mUri, ex);
					mCurrentState = STATE_ERROR;
					mTargetState = STATE_ERROR;
					mErrorListener.onError(mMediaPlayer,
							MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
				}
			}
		};
		
		if (mPlayHandler != null) {
			mPlayHandler.removeCallbacksAndMessages(null);
			// 
			/*
			 * 在MSG_DESTROY后，通过MainHandler执行callBack（参考mPlayHandler中的MSG_DESTROY实现），
			 * 从而保证callBack一定在MSG_DESTROY后执行，并且在主线程中执行。
			 */
			mPlayHandler.obtainMessage(MSG_DESTROY, callBack).sendToTarget();
		} else {
			callBack.run();	//直接执行
		}
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void setMediaPlayerAudioSessionId(int audioSession) {
		mMediaPlayer.setAudioSessionId(audioSession);
	}
	
	private void setMediaPlayerDataSource(Uri uri, Map<String, String> headers) throws IOException {
        String url = uri.toString();
        if ((url.startsWith("http") || url.startsWith("rtsp")) && (headers == null || headers.isEmpty())) {
            mMediaPlayer.setDataSource(url);
            return;
        }
        /*
         * 魅族MX5上 MediaPlayer setDataSource只要使用header就会报空指针：
         * java.lang.NullPointerException: Attempt to invoke interface method
         * 'java.util.Map com.mediatek.common.media.IOmaSettingHelper.setSettingHeader(
         * android.content.Context, android.net.Uri, java.util.Map)' on a null object reference
         */
		boolean isMeizuMX5 = TextUtils.equals(Build.MANUFACTURER, "Meizu")
				&& TextUtils.equals(Build.PRODUCT, "MX5");
		if (isMeizuMX5) {
			mMediaPlayer.setDataSource(getContext(), uri);
		} else {
			mMediaPlayer.setDataSource(getContext(), uri, headers);
		}
	}

	public void setMediaController(MediaControllerCompat controller) {
		if (mMediaController != null) {
			mMediaController.hide();
		}
		mMediaController = controller;
		attachMediaController();
	}

	private void attachMediaController() {
		if (mMediaPlayer != null && mMediaController != null) {
            if (mMediaPlayerControl != null) {
                mMediaController.setMediaPlayer(mMediaPlayerControl);
            } else {
                mMediaController.setMediaPlayer(this);
            }
            View anchorView = this.getParent() instanceof View ? (View) this
                    .getParent() : this;
            mMediaController.setAnchorView(anchorView);
            mMediaController.setEnabled(isInPlaybackState());
        }
    }

	@NonNull
	MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
		public void onVideoSizeChanged(@NonNull MediaPlayer mp, int width, int height) {
			if (mHasDestroyed) {
				return;
			}

            try {
                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                    requestLayout();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
	};

	@Nullable
	MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
		public void onPrepared(@NonNull MediaPlayer mp) {
			mCurrentState = STATE_PREPARED;

			if (mHasDestroyed) {
				return;
			}
			
			mDuration = mp.getDuration();
			/*// Get the capabilities of the player for this stream
			Metadata data = mp.getMetadata(MediaPlayer.METADATA_ALL,
					MediaPlayer.BYPASS_METADATA_FILTER);

			if (data != null) {
				mCanPause = !data.has(Metadata.PAUSE_AVAILABLE)
						|| data.getBoolean(Metadata.PAUSE_AVAILABLE);
				mCanSeekBack = !data.has(Metadata.SEEK_BACKWARD_AVAILABLE)
						|| data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE);
				mCanSeekForward = !data.has(Metadata.SEEK_FORWARD_AVAILABLE)
						|| data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE);
			} else {
				mCanPause = mCanSeekBack = mCanSeekForward = true; 
			}*/

			if (mOnPreparedListener != null) {
				mOnPreparedListener.onPrepared(mMediaPlayer);
			}
			if (mMediaController != null) {
				mMediaController.setEnabled(true);
			}
			mVideoWidth = mp.getVideoWidth();
			mVideoHeight = mp.getVideoHeight();

			int seekToPosition = mSeekWhenPrepared; // mSeekWhenPrepared may be
													// changed after seekTo()
													// call
			if (seekToPosition != 0) {
				seekTo(seekToPosition);
			}
			if (mVideoWidth != 0 && mVideoHeight != 0) {
				// Log.i("@@@@", "video size: " + mVideoWidth +"/"+
				// mVideoHeight);
				getHolder().setFixedSize(mVideoWidth, mVideoHeight);
				if (mSurfaceWidth == mVideoWidth
						&& mSurfaceHeight == mVideoHeight) {
					// We didn't actually change the size (it was already at the
					// size
					// we need), so we won't get a "surface changed" callback,
					// so
					// start the video here instead of in the callback.
					if (mTargetState == STATE_PLAYING) {
						start();
						if (mMediaController != null) {
							mMediaController.show();
						}
					} else if (!isPlaying()
							&& (seekToPosition != 0/* || getCurrentPosition() > 0*/)) {
						if (mMediaController != null) {
							// Show the media controls when we're paused into a
							// video and make 'em stick.
							mMediaController.show(0);
						}
					}
				} else {
					if (mTargetState == STATE_PLAYING) {
						start();
					}
				}
			} else {
				// We don't know the video size yet, but should start anyway.
				// The video size might be reported to us later.
				if (mTargetState == STATE_PLAYING) {
					start();
				}
			}
		}
	};

	@Nullable
	private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mp) {
			mCurrentState = STATE_PLAYBACK_COMPLETED;
			mTargetState = STATE_PLAYBACK_COMPLETED;

			if (mHasDestroyed) {
				return;
			}

			if (mMediaController != null) {
				mMediaController.hide();
			}
			if (mOnCompletionListener != null) {
				mOnCompletionListener.onCompletion(mMediaPlayer);
			}
		}
	};

	@Nullable
	private MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
		@Override
		public boolean onInfo(MediaPlayer mp, int arg1, int arg2) {
			if (mHasDestroyed) {
				return true;
			}

			if (mOnInfoListener != null) {
				mOnInfoListener.onInfo(mp, arg1, arg2);
			}
			return true;
		}
	};

	@NonNull
	private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
		@Override
		public boolean onError(MediaPlayer mp, int frameworkErr, int implErr) {
			Log.d(TAG, "Error: " + frameworkErr + "," + implErr);
            if (frameworkErr == 1 && (implErr == -19 || implErr == -38)) {
                return true;
            }
            mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			
			if (mHasDestroyed) {
				return true;
			}
			
			if (mMediaController != null) {
				mMediaController.hide();
			}

			/* If an error handler has been supplied, use it and finish. */
			if (mOnErrorListener != null) {
				if (mOnErrorListener.onError(mMediaPlayer, frameworkErr,
						implErr)) {
					return true;
				}
			}

			return true;
		}
	};

	@NonNull
	private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener
	= new MediaPlayer.OnBufferingUpdateListener() {
		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			mCurrentBufferPercentage = percent;
		}
	};

	/**
	 * Register a callback to be invoked when the media file is loaded and ready
	 * to go.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
		mOnPreparedListener = l;
	}

	/**
	 * Register a callback to be invoked when the end of a media file has been
	 * reached during playback.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnCompletionListener(OnCompletionListener l) {
		mOnCompletionListener = l;
	}

	/**
	 * Register a callback to be invoked when an error occurs during playback or
	 * setup. If no listener is specified, or if the listener returned false,
	 * VideoView will inform the user of any errors.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnErrorListener(OnErrorListener l) {
		mOnErrorListener = l;
	}

	/**
	 * Register a callback to be invoked when an informational event occurs
	 * during playback or setup.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnInfoListener(OnInfoListener l) {
		mOnInfoListener = l;
	}

	@Nullable
	SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
		private boolean videoOpened;
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
			mSurfaceWidth = w;
			mSurfaceHeight = h;
			boolean isValidState = (mTargetState == STATE_PLAYING);
			boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
			if (mMediaPlayer != null && isValidState && hasValidSize) {
				if (mSeekWhenPrepared != 0) {
					seekTo(mSeekWhenPrepared);
				}
				start();
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mSurfaceHolder = holder;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				openVideo();
			} else {
				if (!videoOpened || mMediaPlayer == null) {
					openVideo();
					videoOpened = true;
				} else {
					changeSurface(holder);
				}
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// after we return from this we can't use the surface any more
			mSurfaceHolder = null;
			if (mMediaController != null) {
				mMediaController.hide();
			}
			
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				release(true);
			} else {
				pause();
			}
		}
	};

	/**
	 * release the media player in any state
	 */
	public void release(final boolean cleartargetstate) {
		if (mHasDestroyed) {
			return;
		}

		if (mPlayHandler != null) {
			mPlayHandler.removeCallbacksAndMessages(null);
			mPlayHandler.obtainMessage(MSG_RELEASE, cleartargetstate).sendToTarget();
		}
	}
	
	/**
	 * 彻底销毁，该VideoView无法再次播放任何视频
	 */
	public void destroy() {
		if (!mHasDestroyed) {
			mHasDestroyed = true;
			if (mPlayHandler != null) {
				mPlayHandler.removeCallbacksAndMessages(null);
				mPlayHandler.sendEmptyMessage(MSG_DESTROY);
			}
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (isInPlaybackState() && mMediaController != null) {
			toggleMediaControlsVisiblity();
		}
		return false;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
		if (isInPlaybackState() && mMediaController != null) {
			toggleMediaControlsVisiblity();
		}
		return false;
	}

	@SuppressLint("InlinedApi")
	@Override
	public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
		boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK
				&& keyCode != KeyEvent.KEYCODE_VOLUME_UP
				&& keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
				&& keyCode != KeyEvent.KEYCODE_VOLUME_MUTE
				&& keyCode != KeyEvent.KEYCODE_MENU
				&& keyCode != KeyEvent.KEYCODE_CALL
				&& keyCode != KeyEvent.KEYCODE_ENDCALL;
		if (isInPlaybackState() && isKeyCodeSupported
				&& mMediaController != null) {
			if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
					|| keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
				if (mMediaPlayer.isPlaying()) {
					pause();
					mMediaController.show();
				} else {
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
				if (!mMediaPlayer.isPlaying()) {
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
					|| keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
				if (mMediaPlayer.isPlaying()) {
					pause();
					mMediaController.show();
				}
				return true;
			} else {
				toggleMediaControlsVisiblity();
			}
		}

		return super.onKeyDown(keyCode, event);
	}
	
	private void toggleMediaControlsVisiblity() {
		if (mMediaController.isShowing()) {
			mMediaController.hide();
		} else {
			mMediaController.show();
		}
	}
	
	@Override
	public void start() {
		if (mHasDestroyed) {
			return;
		}
		if (mPlayHandler != null) {
			mPlayHandler.removeMessages(MSG_START);
			mPlayHandler.removeMessages(MSG_PAUSE);
			
			mPlayHandler.sendEmptyMessage(MSG_START);
		}
		mTargetState = STATE_PLAYING;
	}

	@Override
	public void pause() {
		if (mHasDestroyed) {
			return;
		}
		if (mPlayHandler != null) {
			mPlayHandler.removeMessages(MSG_START);
			mPlayHandler.removeMessages(MSG_PAUSE);
			
			mPlayHandler.sendEmptyMessage(MSG_PAUSE);
		}
		mTargetState = STATE_PAUSED;
	}

    @Override
	public boolean isGoingToPlay() {
		return mTargetState == VideoViewCompat.STATE_PLAYING;
	}

	@Override
	public int getDuration() {
		if (mDuration <= 0 && !mHasDestroyed) {
			try {
				if (isInPlaybackState()) {
					mDuration = mMediaPlayer.getDuration();
				}
			} catch (Exception e) {	// SUPPRESS CHECKSTYLE
				
			}
		}
		return mDuration;
	}

	@Override
	public void getCurrentPosition(MediaControllerCompat.OnResultListener<Integer> onPositionResult) {
		if (mHasDestroyed) {
			return;
		}

		if (mPlayHandler != null) {
			mPlayHandler.obtainMessage(MSG_GET_POSITION, onPositionResult).sendToTarget();
		}
	}
	
	@Override
	public void seekTo(int msec) {
		if (mHasDestroyed) {
			return;
		}
		
		if (msec >= mDuration) {
			msec = 0;
		}
		
		if (mPlayHandler != null) {
			// 可以删除未执行的seek操作，不影响流程
			mPlayHandler.removeMessages(MSG_SEEK);
			mPlayHandler.obtainMessage(MSG_SEEK, msec, 0).sendToTarget();
		} else {
			mSeekWhenPrepared = msec;
		}
	}
	
	@Override
	public boolean isPlaying() {
		try {
			return isInPlaybackState() && /*mMediaPlayer.isPlaying()*/mCurrentState == STATE_PLAYING;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public int getBufferPercentage() {
		if (mMediaPlayer != null) {
			return mCurrentBufferPercentage;
		}
		return 0;
	}
	
	private void changeSurface(SurfaceHolder holder) {
		if (mHasDestroyed) {
			return;
		}
		
		if (mPlayHandler != null) {
			mPlayHandler.obtainMessage(MSG_CHANGE_SURFACE, holder).sendToTarget();
		}
	}

	private boolean isInPlaybackState() {
		return (mMediaPlayer != null && mCurrentState != STATE_ERROR
				&& mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
	public int getAudioSessionId() {
		if (mAudioSession == 0) {
			MediaPlayer foo = new MediaPlayer();
			mAudioSession = foo.getAudioSessionId();
			foo.release();
		}
		return mAudioSession;
	}
	
	@NonNull
	private StateCallBack mStateCallBack = new StateCallBack() {
		
		@Override
		public void setTargetState(int state) {
			mTargetState = state;
		}
		
		@Override
		public void setSeekWhenPrepared(int seek) {
			mSeekWhenPrepared = seek;
		}
		
		@Override
		public void setDuration(int duration) {
			mDuration = duration;
		}
		
		@Override
		public void setCurrentState(int state) {
			mCurrentState = state;
		}
		
		@Override
		public int getTargetState() {
			return mTargetState;
		}
		
		@Override
		public int getCurrentState() {
			return mCurrentState;
		}
	};
	
	private static final int MSG_SHOW_CONTROLLER = 1;
	private static final int MSG_ON_POSITION_RESULT = 2;
	
	@SuppressLint("HandlerLeak")
	private class MainHandler extends Handler {
		
		@Override
		public void handleMessage(@NonNull Message msg) {
			switch (msg.what) {
			case MSG_SHOW_CONTROLLER:
				mMediaController.show();
				break;

			case MSG_ON_POSITION_RESULT:
				@SuppressWarnings("unchecked")
				MediaControllerCompat.OnResultListener<Integer> onPositionResult =
						(MediaControllerCompat.OnResultListener<Integer>) msg.obj;
				if (!mHasDestroyed && onPositionResult != null) {
					onPositionResult.onResult(msg.arg1);
				}
				break;
				
			default:
				break;
			}
		}
	}
	
	private interface StateCallBack {
		void setCurrentState(int state);
		int getCurrentState();
		void setTargetState(int state);
		int getTargetState();
		void setSeekWhenPrepared(int seek);
		void setDuration(int duration);
	}

	private static final int MSG_START = 1;
	private static final int MSG_PAUSE = 2;
	private static final int MSG_STOP = 3;
	private static final int MSG_SEEK = 4;
	private static final int MSG_RELEASE = 5;
	private static final int MSG_DESTROY = 6;
	private static final int MSG_GET_POSITION = 7;
	private static final int MSG_CHANGE_SURFACE = 8;
    private static final int MSG_PREPARE = 9;
	
	private static class PlayHandler extends Handler {
		
		private MainHandler mMainHandler;
		@NonNull
		private MediaPlayer mPlayer;
		private StateCallBack mStateCallBack;
        private boolean mHasReleased;
		
		PlayHandler(@NonNull Looper looper, MainHandler mainHandler, @NonNull MediaPlayer player,
                    StateCallBack callBack) {
			super(looper);
			mMainHandler = mainHandler;
			mPlayer = player;
			mStateCallBack = callBack;
		}
		
		@Override
		public void handleMessage(@NonNull Message msg) {
			switch (msg.what) {
			case MSG_START:
                if (inPlaybackState()) {
                    mPlayer.start();
                    mStateCallBack.setCurrentState(STATE_PLAYING);
                }
                mMainHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
                mStateCallBack.setTargetState(STATE_PLAYING);
                break;
				
			case MSG_PAUSE:
                if (inPlaybackState()) {
                    if (mPlayer.isPlaying()) {
                        mPlayer.pause();
                        mStateCallBack.setCurrentState(STATE_PAUSED);
                    }
                }
                mMainHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
                mStateCallBack.setTargetState(STATE_PAUSED);
                break;
				
			case MSG_STOP:
                if (!mHasReleased) {
                    mPlayer.stop();
                    mPlayer.release();
                    mHasReleased = true;
                    mStateCallBack.setCurrentState(STATE_IDLE);
                    mStateCallBack.setTargetState(STATE_IDLE);
                }
                break;

			case MSG_SEEK:
                int msec = msg.arg1;
                if (inPlaybackState()) {
                    mPlayer.seekTo(msec);
                    mStateCallBack.setSeekWhenPrepared(0);
                } else {
                    mStateCallBack.setSeekWhenPrepared(msec);
                }
                mMainHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
                break;

			case MSG_RELEASE:
                boolean clearTargetState = msg.obj != null ? (Boolean) msg.obj : true;
                if (!mHasReleased) {
                    mStateCallBack.setDuration(-1);
                    mPlayer.reset();
                    mPlayer.release();
                    mHasReleased = true;
                    mStateCallBack.setCurrentState(STATE_IDLE);
                    if (clearTargetState) {
                        mStateCallBack.setTargetState(STATE_IDLE);
                    }
                }

                break;
				
			case MSG_DESTROY:
				// 先直接（非异步）执行MSG_RELEASE
				handleMessage(obtainMessage(MSG_RELEASE, false));
				
				mMainHandler.removeCallbacksAndMessages(null);
				
				removeCallbacksAndMessages(null);
				HandlerThread thread = (HandlerThread) Thread.currentThread();
				thread.quit();
				
				if (msg.obj != null) {
					Runnable afterDestroyCallBack = (Runnable) msg.obj;
					mMainHandler.post(afterDestroyCallBack);
				}
				break;

			case MSG_GET_POSITION:
                try {
                    if (inPlaybackState()) {
                        int pos = mPlayer.getCurrentPosition();
                        mMainHandler.obtainMessage(MSG_ON_POSITION_RESULT, pos, 0, msg.obj).sendToTarget();
                    }
                } catch (Exception e) {	// SUPPRESS CHECKSTYLE
                }
                break;
				
			case MSG_CHANGE_SURFACE:
                SurfaceHolder holder = (SurfaceHolder) msg.obj;
                if (!mHasReleased) {
                    try {
                        mPlayer.setDisplay(holder);
                        mMainHandler.sendEmptyMessage(MSG_SHOW_CONTROLLER);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case MSG_PREPARE:
                if (!mHasReleased) {
                    try {
                        mPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;

			default:
				break;
			}
		}
		
		private boolean inPlaybackState() {
			int currentState = mStateCallBack.getCurrentState();
			return (!mHasReleased && currentState != STATE_ERROR
                    && currentState != STATE_IDLE && currentState != STATE_PREPARING);
		}
	}
}
