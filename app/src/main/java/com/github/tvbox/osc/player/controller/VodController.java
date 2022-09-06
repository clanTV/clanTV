package com.github.tvbox.osc.player.controller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.player.thirdparty.MXPlayer;
import com.github.tvbox.osc.player.thirdparty.ReexPlayer;
import com.github.tvbox.osc.player.thirdparty.Kodi;
import com.github.tvbox.osc.ui.adapter.ParseAdapter;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.PlayerHelper;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.util.PlayerUtils;

import static xyz.doikki.videoplayer.util.PlayerUtils.stringForTime;

public class VodController extends BaseController {
    public VodController(@NonNull @NotNull Context context) {
        super(context);
        mHandlerCallback = new HandlerCallback() {
            @Override
            public void callback(Message msg) {
                switch (msg.what) {
                    case 1000: { // seek 刷新
                        mProgressRoot.setVisibility(VISIBLE);
                        break;
                    }
                    case 1001: { // seek 关闭
                        mProgressRoot.setVisibility(GONE);
                        break;
                    }
                    case 1002: { // 显示底部菜单
//                        mTopHide.setVisibility(GONE);
//                        mTopRoot.setVisibility(VISIBLE);
//                        TranslateAnimation animateT = new TranslateAnimation(
//                                0,                // fromXDelta
//                                0,                  // toXDelta
//                                -mTopRoot.getHeight(),       // fromYDelta
//                                0);                 // toYDelta
//                        animateT.setDuration(400);
//                        animateT.setFillAfter(true);
//                        mTopRoot.startAnimation(animateT);
//
//                        mBottomRoot.setVisibility(VISIBLE);
//                        TranslateAnimation animateB = new TranslateAnimation(
//                                0,                // fromXDelta
//                                0,                  // toXDelta
//                                mBottomRoot.getHeight(),    // fromYDelta
//                                0);                 // toYDelta
//                        animateB.setDuration(400);
//                        animateB.setFillAfter(true);
//                        mBottomRoot.startAnimation(animateB);
//                        mBottomRoot.requestFocus();

                        // takagen99 : Revamp Show & Hide Logic with alpha
                        mTopHide.setVisibility(GONE);
                        mTopRoot.setVisibility(VISIBLE);
                        mTopRoot.setAlpha(0.0f);
                        mTopRoot.setTranslationY(-mTopRoot.getHeight());
                        mTopRoot.animate()
                                .translationY(0)
                                .alpha(1.0f)
                                .setDuration(400)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(null);

                        mBottomRoot.setVisibility(VISIBLE);
                        mBottomRoot.setAlpha(0.0f);
                        mBottomRoot.setTranslationY(mBottomRoot.getHeight());
                        mBottomRoot.animate()
                                .translationY(0)
                                .alpha(1.0f)
                                .setDuration(400)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(null);
                        mBottomRoot.requestFocus();
                        break;
                    }
                    case 1003: { // 隐藏底部菜单
//                        TranslateAnimation animateT = new TranslateAnimation(
//                                0,                 // fromXDelta
//                                0,                   // toXDelta
//                                0,                 // fromYDelta
//                                -mTopRoot.getHeight());
//                        animateT.setDuration(400);
//                        animateT.setFillAfter(true);
//                        mTopRoot.startAnimation(animateT);
//                        mTopRoot.setVisibility(GONE);
//
//                        TranslateAnimation animateB = new TranslateAnimation(
//                                0,                 // fromXDelta
//                                0,                   // toXDelta
//                                0,                 // fromYDelta
//                                //mBottomRoot.getHeight());  // toYDelta
//                                // takagen99: Quick fix VOD controller shows after PIP
//                                mBottomRoot.getHeight());
//                        animateB.setDuration(400);
//                        animateB.setFillAfter(true);
//                        mBottomRoot.startAnimation(animateB);
//                        mBottomRoot.setVisibility(GONE);
//
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                mBottomRoot.clearAnimation();
//                            }
//                        }, 450);

                        // takagen99 : Revamp Show & Hide Logic with alpha
                        mTopRoot.animate()
                                .translationY(-mTopRoot.getHeight())
                                .alpha(0.0f)
                                .setDuration(400)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        mTopRoot.setVisibility(View.GONE);
                                        mTopRoot.clearAnimation();
                                    }
                                });

                        mBottomRoot.animate()
                                .translationY(mBottomRoot.getHeight())
                                .alpha(0.0f)
                                .setDuration(400)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        mBottomRoot.setVisibility(View.GONE);
                                        mBottomRoot.clearAnimation();
                                    }
                                });
                        break;
                    }
                    case 1004: { // 设置速度
                        if (isInPlaybackState()) {
                            try {
                                float speed = (float) mPlayerConfig.getDouble("sp");
                                mControlWrapper.setSpeed(speed);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else
                            mHandler.sendEmptyMessageDelayed(1004, 100);
                        break;
                    }
                }
            }
        };
    }

    SeekBar mSeekBar;
    TextView mCurrentTime;
    TextView mTotalTime;
    boolean mIsDragging;
    LinearLayout mProgressRoot;
    TextView mProgressText;
    ImageView mProgressIcon;
    LinearLayout mTopRoot;
    LinearLayout mTopHide;
    LinearLayout mBottomRoot;
    LinearLayout mParseRoot;
    TvRecyclerView mGridView;
    TextView mPlayTitle;
    TextView mNextBtn;
    TextView mPreBtn;
    TextView mPlayerScaleBtn;
    TextView mPlayerSpeedBtn;
    TextView mPlayerBtn;
    TextView mPlayerIJKBtn;
    TextView mPlayerRetry;
    TextView mPlayerTimeStartBtn;
    TextView mPlayerTimeSkipBtn;
    TextView mPlayerTimeStepBtn;
    TextView mPlayerResolution;

    TextView mTime;
    TextView mTimeEnd;

    // takagen99 : Added for Fast Forward Button
    TextView mPlayerFFwd;
    float mSpeed;
    Drawable dPlay = getResources().getDrawable(R.drawable.play_play);
    Drawable dFFwd = getResources().getDrawable(R.drawable.play_ffwd);

    // takagen99 : To get system time
    private Runnable mTimeRunnable = new Runnable() {
        @Override
        public void run() {
            Date date = new Date();
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
            mTime.setText(timeFormat.format(date));
            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void initView() {
        super.initView();
        mCurrentTime = findViewById(R.id.curr_time);
        mTotalTime = findViewById(R.id.total_time);
        mPlayTitle = findViewById(R.id.tv_title_top);
        mTime = findViewById(R.id.tv_time);
        mTimeEnd = findViewById(R.id.tv_time_end);
        mSeekBar = findViewById(R.id.seekBar);
        mProgressRoot = findViewById(R.id.tv_progress_container);
        mProgressIcon = findViewById(R.id.tv_progress_icon);
        mProgressText = findViewById(R.id.tv_progress_text);
        mTopRoot = findViewById(R.id.top_container);
        mTopHide = findViewById(R.id.top_container_hide);
        mBottomRoot = findViewById(R.id.bottom_container);
        mParseRoot = findViewById(R.id.parse_root);
        mGridView = findViewById(R.id.mGridView);
        mPlayerRetry = findViewById(R.id.play_retry);
        mNextBtn = findViewById(R.id.play_next);
        mPreBtn = findViewById(R.id.play_pre);
        mPlayerScaleBtn = findViewById(R.id.play_scale);
        mPlayerSpeedBtn = findViewById(R.id.play_speed);
        mPlayerBtn = findViewById(R.id.play_player);
        mPlayerIJKBtn = findViewById(R.id.play_ijk);
        mPlayerTimeStartBtn = findViewById(R.id.play_time_start);
        mPlayerTimeSkipBtn = findViewById(R.id.play_time_end);
        mPlayerTimeStepBtn = findViewById(R.id.play_time_step);
        mPlayerFFwd = findViewById(R.id.play_ff);
        mPlayerResolution = findViewById(R.id.tv_resolution);

        mTopRoot.setVisibility(INVISIBLE);
        mBottomRoot.setVisibility(INVISIBLE);

        mGridView.setLayoutManager(new V7LinearLayoutManager(getContext(), 0, false));
        ParseAdapter parseAdapter = new ParseAdapter();
        parseAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                ParseBean parseBean = parseAdapter.getItem(position);
                // 当前默认解析需要刷新
                int currentDefault = parseAdapter.getData().indexOf(ApiConfig.get().getDefaultParse());
                parseAdapter.notifyItemChanged(currentDefault);
                ApiConfig.get().setDefaultParse(parseBean);
                parseAdapter.notifyItemChanged(position);
                listener.changeParse(parseBean);
                hideBottom();
            }
        });
        mGridView.setAdapter(parseAdapter);
        parseAdapter.setNewData(ApiConfig.get().getParseBeanList());

        mParseRoot.setVisibility(VISIBLE);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * progress) / seekBar.getMax();
                if (mCurrentTime != null)
                    mCurrentTime.setText(stringForTime((int) newPosition));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragging = true;
                mControlWrapper.stopProgress();
                mControlWrapper.stopFadeOut();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * seekBar.getProgress()) / seekBar.getMax();
                mControlWrapper.seekTo((int) newPosition);
                mIsDragging = false;
                mControlWrapper.startProgress();
                mControlWrapper.startFadeOut();
            }
        });
        mPlayerRetry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.replay(true);
                hideBottom();
            }
        });
        mNextBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPaused) {
                    togglePlay();
                } else {
                    listener.playNext(false);
                }
                hideBottom();
            }
        });
        mPreBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.playPre();
                hideBottom();
            }
        });
        mPlayerScaleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mHideBottomRunnable);
                mHandler.postDelayed(mHideBottomRunnable, 10000);
                try {
                    int scaleType = mPlayerConfig.getInt("sc");
                    scaleType++;
                    if (scaleType > 5)
                        scaleType = 0;
                    mPlayerConfig.put("sc", scaleType);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    mControlWrapper.setScreenScaleType(scaleType);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerSpeedBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mHideBottomRunnable);
                mHandler.postDelayed(mHideBottomRunnable, 10000);
                try {
                    float speed = (float) mPlayerConfig.getDouble("sp");
                    speed += 0.25f;
                    if (speed > 3)
                        speed = 0.5f;
                    if (speed == 1)
                        mPlayerFFwd.setCompoundDrawablesWithIntrinsicBounds(dFFwd, null, null, null);
                    mPlayerConfig.put("sp", speed);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    mControlWrapper.setSpeed(speed);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // takagen99: Add long press to reset speed
        mPlayerSpeedBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerFFwd.setCompoundDrawablesWithIntrinsicBounds(dFFwd, null, null, null);
                    mPlayerConfig.put("sp", 1.0f);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    mControlWrapper.setSpeed(1.0f);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        mPlayerBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int playerType = mPlayerConfig.getInt("pl");
                    boolean playerVail = false;
                    do {
                        playerType++;
                        if (playerType <= 2) {
                            playerVail = true;
                        } else if (playerType == 10) {
                            playerVail = mxPlayerExist;
                        } else if (playerType == 11) {
                            playerVail = reexPlayerExist;
                        } else if (playerType == 12) {
                            playerVail = KodiExist;
                        } else if (playerType > 12) {
                            playerType = 0;
                            playerVail = true;
                        }
                    } while (!playerVail);
                    mPlayerConfig.put("pl", playerType);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    listener.replay(false);
                    view.requestFocus();
                    // hideBottom();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerIJKBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String ijk = mPlayerConfig.getString("ijk");
                    List<IJKCode> codecs = ApiConfig.get().getIjkCodes();
                    for (int i = 0; i < codecs.size(); i++) {
                        if (ijk.equals(codecs.get(i).getName())) {
                            if (i >= codecs.size() - 1)
                                ijk = codecs.get(0).getName();
                            else {
                                ijk = codecs.get(i + 1).getName();
                            }
                            break;
                        }
                    }
                    mPlayerConfig.put("ijk", ijk);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    listener.replay(false);
                    view.requestFocus();
                    // hideBottom();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerTimeStartBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mHideBottomRunnable);
                mHandler.postDelayed(mHideBottomRunnable, 10000);
                try {
                    int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
                    int st = mPlayerConfig.getInt("st");
                    st += step;
                    if (st > 60 * 10)
                        st = 0;
                    mPlayerConfig.put("st", st);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // takagen99: Add long press to reset counter
        mPlayerTimeStartBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerConfig.put("st", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        mPlayerTimeSkipBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mHideBottomRunnable);
                mHandler.postDelayed(mHideBottomRunnable, 10000);
                try {
                    int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
                    int et = mPlayerConfig.getInt("et");
                    et += step;
                    if (et > 60 * 10)
                        et = 0;
                    mPlayerConfig.put("et", et);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // takagen99: Add long press to reset counter
        mPlayerTimeSkipBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerConfig.put("et", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        mPlayerTimeStepBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
                step += 5;
                if (step > 30) {
                    step = 5;
                }
                Hawk.put(HawkConfig.PLAY_TIME_STEP, step);
                updatePlayerCfgView();
            }
        });
        // takagen99: Add long press to reset counter
        mPlayerTimeStepBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Hawk.put(HawkConfig.PLAY_TIME_STEP, 5);
                updatePlayerCfgView();
                return true;
            }
        });
        // takagen99: Add fastforward button
        mPlayerFFwd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSpeed == 5.0f) {
                    mSpeed = 1.0f;
                    mPlayerFFwd.setCompoundDrawablesWithIntrinsicBounds(dFFwd, null, null, null);
                } else {
                    mSpeed = 5.0f;
                    mPlayerFFwd.setCompoundDrawablesWithIntrinsicBounds(dPlay, null, null, null);
                }
                ;
                try {
                    mPlayerConfig.put("sp", mSpeed);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    mControlWrapper.setSpeed(mSpeed);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    protected int getLayoutId() {
        return R.layout.player_vod_control_view;
    }

    public void showParse(boolean userJxList) {
        mParseRoot.setVisibility(userJxList ? VISIBLE : GONE);
    }

    private JSONObject mPlayerConfig = null;

    private boolean mxPlayerExist = false;
    private boolean reexPlayerExist = false;
    private boolean KodiExist = false;

    public void setPlayerConfig(JSONObject playerCfg) {
        this.mPlayerConfig = playerCfg;
        updatePlayerCfgView();
        mxPlayerExist = MXPlayer.getPackageInfo() != null;
        reexPlayerExist = ReexPlayer.getPackageInfo() != null;
        KodiExist = Kodi.getPackageInfo() != null;
    }

    void updatePlayerCfgView() {
        try {
            int playerType = mPlayerConfig.getInt("pl");
            mPlayerBtn.setText(PlayerHelper.getPlayerName(playerType));
            mPlayerScaleBtn.setText(PlayerHelper.getScaleName(mPlayerConfig.getInt("sc")));
            mPlayerIJKBtn.setText(mPlayerConfig.getString("ijk"));
            mPlayerIJKBtn.setVisibility(playerType == 1 ? VISIBLE : GONE);
            mPlayerScaleBtn.setText(PlayerHelper.getScaleName(mPlayerConfig.getInt("sc")));
            mPlayerSpeedBtn.setText("x" + mPlayerConfig.getDouble("sp"));
            mPlayerTimeStartBtn.setText(PlayerUtils.stringForTime(mPlayerConfig.getInt("st") * 1000));
            mPlayerTimeSkipBtn.setText(PlayerUtils.stringForTime(mPlayerConfig.getInt("et") * 1000));
            mPlayerTimeStepBtn.setText(Hawk.get(HawkConfig.PLAY_TIME_STEP, 5) + "s");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setTitle(String playTitleInfo) {
        mPlayTitle.setText(playTitleInfo);
    }

    public void resetSpeed() {
        skipEnd = true;
        mHandler.removeMessages(1004);
        mHandler.sendEmptyMessageDelayed(1004, 100);
    }

    public interface VodControlListener {
        void playNext(boolean rmProgress);

        void playPre();

        void changeParse(ParseBean pb);

        void updatePlayerCfg();

        void replay(boolean replay);

        void errReplay();
    }

    public void setListener(VodControlListener listener) {
        this.listener = listener;
    }

    private VodControlListener listener;

    private boolean skipEnd = true;

    @SuppressLint("SetTextI18n")
    @Override
    protected void setProgress(int duration, int position) {
        if (mIsDragging) {
            return;
        }
        super.setProgress(duration, position);
        if (skipEnd && position != 0 && duration != 0) {
            int et = 0;
            try {
                et = mPlayerConfig.getInt("et");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (et > 0 && position + (et * 1000) >= duration) {
                skipEnd = false;
                listener.playNext(true);
            }
        }
        // takagen99 : Calculate finish time
        long TimeRemaining = mControlWrapper.getDuration() - mControlWrapper.getCurrentPosition();
        Calendar date = Calendar.getInstance();
        long t = date.getTimeInMillis();
        Date afterAdd = new Date(t + TimeRemaining);
        SimpleDateFormat timeEnd = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
        mTimeEnd.setText("Ends at " + timeEnd.format(afterAdd));

        mCurrentTime.setText(PlayerUtils.stringForTime(position));
        mTotalTime.setText(PlayerUtils.stringForTime(duration));
        if (duration > 0) {
            mSeekBar.setEnabled(true);
            int pos = (int) (position * 1.0 / duration * mSeekBar.getMax());
            mSeekBar.setProgress(pos);
        } else {
            mSeekBar.setEnabled(false);
        }
        int percent = mControlWrapper.getBufferedPercentage();
        if (percent >= 95) {
            mSeekBar.setSecondaryProgress(mSeekBar.getMax());
        } else {
            mSeekBar.setSecondaryProgress(percent * 10);
        }
    }

    private boolean simSlideStart = false;
    private int simSeekPosition = 0;
    private long simSlideOffset = 0;

    public void tvSlideStop() {
        if (!simSlideStart)
            return;
        mControlWrapper.seekTo(simSeekPosition);
        if (!mControlWrapper.isPlaying())
            mControlWrapper.start();
        simSlideStart = false;
        simSeekPosition = 0;
        simSlideOffset = 0;
    }

    public void tvSlideStart(int dir) {
        int duration = (int) mControlWrapper.getDuration();
        if (duration <= 0)
            return;
        if (!simSlideStart) {
            simSlideStart = true;
        }
        // 每次10秒
        simSlideOffset += (10000.0f * dir);
        int currentPosition = (int) mControlWrapper.getCurrentPosition();
        int position = (int) (simSlideOffset + currentPosition);
        if (position > duration) position = duration;
        if (position < 0) position = 0;
        updateSeekUI(currentPosition, position, duration);
        simSeekPosition = position;
    }

    @Override
    protected void updateSeekUI(int curr, int seekTo, int duration) {
        super.updateSeekUI(curr, seekTo, duration);
        if (seekTo > curr) {
            mProgressIcon.setImageResource(R.drawable.play_ffwd);
        } else {
            mProgressIcon.setImageResource(R.drawable.play_rewind);
        }
        mProgressText.setText(PlayerUtils.stringForTime(seekTo) + " / " + PlayerUtils.stringForTime(duration));
        mHandler.sendEmptyMessage(1000);
        mHandler.removeMessages(1001);
        mHandler.sendEmptyMessageDelayed(1001, 1000);
    }

    @Override
    protected void onPlayStateChanged(int playState) {
        super.onPlayStateChanged(playState);
        switch (playState) {
            case VideoView.STATE_IDLE:
                break;
            case VideoView.STATE_PLAYING:
                isPaused = false;
                startProgress();
                break;
            case VideoView.STATE_PAUSED:
                isPaused = true;
                break;
            case VideoView.STATE_ERROR:
                listener.errReplay();
                break;
            case VideoView.STATE_PREPARED:
                // takagen99 : Add Video Resolution
                if (mControlWrapper.getVideoSize().length >= 2) {
                    mPlayerResolution.setText(mControlWrapper.getVideoSize()[0] + " x " + mControlWrapper.getVideoSize()[1]);
                }
            case VideoView.STATE_BUFFERED:
                break;
            case VideoView.STATE_PREPARING:
            case VideoView.STATE_BUFFERING:
                break;
            case VideoView.STATE_PLAYBACK_COMPLETED:
                listener.playNext(true);
                break;
        }
    }

    boolean isBottomVisible() {
        return mBottomRoot.getVisibility() == VISIBLE;
    }

    void showBottom() {
        mHandler.removeMessages(1003);
        mHandler.sendEmptyMessage(1002);
        mHandler.post(mTimeRunnable);
        mHandler.postDelayed(mHideBottomRunnable, 10000);
    }

    Runnable mHideBottomRunnable = new Runnable() {
        @Override
        public void run() {
            hideBottom();
        }
    };

    void hideBottom() {
        mHandler.removeMessages(1002);
        mHandler.sendEmptyMessage(1003);
        mHandler.removeCallbacks(mHideBottomRunnable);
    }

    // takagen99 : Check Pause
    private boolean isPaused = false;

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        boolean isInPlayback = isInPlaybackState();

        if (super.onKeyEvent(event)) {
            return true;
        }
        if (isBottomVisible()) {
            mHandler.removeCallbacks(mHideBottomRunnable);
            mHandler.postDelayed(mHideBottomRunnable, 10000);
            return super.dispatchKeyEvent(event);
        }
        if (action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (isInPlayback) {
                    tvSlideStart(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (isInPlayback) {
                    togglePlay();
                    if (!isBottomVisible() && isPaused) {
                        showBottom();
                    }
                    return true;
                }
//            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {   // takagen99 : Up to show
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (!isBottomVisible()) {
                    showBottom();
                    return true;
                }
            }
        } else if (action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (isInPlayback) {
                    tvSlideStop();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (!isBottomVisible()) {
            showBottom();
        } else {
            hideBottom();
        }
        return true;
    }

    @Override
    public boolean onBackPressed() {
        if (super.onBackPressed()) {
            return true;
        }
        if (isBottomVisible()) {
            hideBottom();
            return true;
        }
        return false;
    }
}