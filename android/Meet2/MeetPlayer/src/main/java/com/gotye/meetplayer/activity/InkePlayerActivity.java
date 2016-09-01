package com.gotye.meetplayer.activity;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.inke.InkeChatRoomClient;
import com.gotye.common.inke.InkeUtil;
import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.ui.MyPreView2;
import com.gotye.meetplayer.util.Util;
import com.gotye.meetsdk.player.MediaPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InkePlayerActivity extends AppCompatActivity
        implements SurfaceHolder.Callback {

    private final static String TAG = "InkePlayerActivity";

    private MediaPlayer mPlayer;
    private MyPreView2 mView;
    private SurfaceHolder mHolder;
    private ProgressBar mBufferingProgressBar;
    private TextView mTvInfo;

    private String mPlayUrl;
    private int mPlayerImpl;
    private int mVideoWidth, mVideoHeight;
    private boolean mIsBuffering = false;
    private long mStartMsec;

    private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnErrorListener	 mOnErrorListener;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnInfoListener mOnInfoListener;

    private InkeChatRoomClient mChatClient;
    private InkeChatRoomClient.WebSocketChannelEvents events;
    private int mRetry = 0;

    private String mCreatorName;
    private int mCreatorUid;
    private String mRoomId;// id=1468316035495368
    private int mSlot;
    private boolean mbSimpleAll;
    private List<String> mMessageList;
    private String mRelation;

    private Button mBtnSendMessage;
    private EditText mEtMessage;
    private TextView mTvMessageHistory;
    private ScrollView mScrollView;
    private boolean mbMessageShowing = true;

    private boolean mbActivityRunning = false;

    /**
     * Possible WebSocket connection states.
     */
    public enum WebSocketConnectionState {
        NEW, CONNECTED, LOGIN, LOGOUT, CLOSED, ERROR
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inke_player);

        try {
            super.getWindow().addFlags(
                    WindowManager.LayoutParams.class.
                            getField("FLAG_NEEDS_MENU_KEY").getInt(null));
        } catch (NoSuchFieldException e) {
            // Ignore since this field won't exist in most versions of Android
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Intent intent = getIntent();
        mCreatorName = intent.getStringExtra("creator_name");
        mCreatorUid = intent.getIntExtra("uid", -1);
        mPlayUrl = intent.getStringExtra("play_url");
        if (intent.hasExtra("rid")) {
            mRoomId     = intent.getStringExtra("rid");
            mSlot       = intent.getIntExtra("slot", -1);
            mbSimpleAll = intent.getBooleanExtra("simpleall", true);
            LogUtil.info(TAG, "roomId: " + mRoomId +
                    ", slot: " + mSlot +
                    ", simple_all: " + (mbSimpleAll ? "Y" : "N"));
        }

        this.mView = (MyPreView2)this.findViewById(R.id.player_view);
        this.mBufferingProgressBar = (ProgressBar)this.findViewById(R.id.progressbar_buffering);
        this.mTvInfo = (TextView)this.findViewById(R.id.tv_info);

        this.mBtnSendMessage = (Button)this.findViewById(R.id.btn_send_message);
        this.mEtMessage = (EditText)this.findViewById(R.id.et_message);
        this.mTvMessageHistory = (TextView)this.findViewById(R.id.tv_message_history);
        this.mScrollView = (ScrollView)this.findViewById(R.id.sv_msg);

        mView.setLongClickable(true); // MUST set to enable double-tap and single-tap-confirm
        mView.setOnTouchListener(mOnTouchListener);

        mTvInfo.setText(String.format(Locale.US, "%s(%d), 在线人数:",
                mCreatorName, mCreatorUid));

        mBtnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = mEtMessage.getText().toString();
                if (!msg.isEmpty()) {
                    if (mChatClient != null) {
                        mChatClient.sendChatMessage(msg);
                        mEtMessage.setText("");

                        addMessgage("我说:", msg);
                    }
                    else {
                        Toast.makeText(InkePlayerActivity.this, "聊天室未连接", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(InkePlayerActivity.this, "无法发送空消息", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Util.initMeetSDK(this);

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");
        if (mPlayerImpl == 0)
            mPlayerImpl = 2;

        SurfaceHolder holder = mView.getHolder();
        if (mPlayerImpl == 3) {
            holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
            holder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
        }
        holder.addCallback(this);

        mOnInfoListener = new MediaPlayer.OnInfoListener() {

            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                // TODO Auto-generated method stub
                if ((MediaPlayer.MEDIA_INFO_BUFFERING_START == what) && !mIsBuffering) {
                    LogUtil.info(TAG, "Java: onInfo MEDIA_INFO_BUFFERING_START");
                    mIsBuffering = true;
                    mBufferingProgressBar.setVisibility(View.VISIBLE);
                }
                else if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_END) && mIsBuffering) {
                    LogUtil.info(TAG, "Java: onInfo MEDIA_INFO_BUFFERING_END");
                    mIsBuffering = false;
                    mBufferingProgressBar.setVisibility(View.GONE);
                }
                else if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    LogUtil.info(TAG, "Java: onInfo MEDIA_INFO_VIDEO_RENDERING_START");
                }
                else if (what == MediaPlayer.MEDIA_INFO_TEST_DROP_FRAME) {
                    LogUtil.info(TAG, String.format(Locale.US,
                            "Java: onInfo MEDIA_INFO_TEST_DROP_FRAME %d msec", extra));
                }

                return true;
            }
        };

        mOnVideoSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {

            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
                // TODO Auto-generated method stub
                mVideoWidth		= w;
                mVideoHeight	= h;

                mHolder.setFixedSize(w, h);
                mView.SetVideoRes(w, h);
            }
        };

        mOnPreparedListener = new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                // TODO Auto-generated method stub
                LogUtil.info(TAG, "Java: onPrepared()");

                mIsBuffering = false;
                mBufferingProgressBar.setVisibility(View.GONE);

                mp.start();

                long load_msec = System.currentTimeMillis() - mStartMsec;
                Toast.makeText(InkePlayerActivity.this,
                        String.format(Locale.US, "加载时间: %d msec", load_msec),
                        Toast.LENGTH_SHORT).show();
            }
        };

        mOnErrorListener = new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int error, int extra) {
                // TODO Auto-generated method stub
                LogUtil.error(TAG, "Java: onError what " + error + " , extra " + extra);

                mIsBuffering = false;
                mBufferingProgressBar.setVisibility(View.GONE);

                Toast.makeText(InkePlayerActivity.this, "Error " + error + " , extra " + extra,
                        Toast.LENGTH_SHORT).show();
                finish();

                return true;
            }
        };

        mOnCompletionListener = new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                finish();
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                mRelation = InkeUtil.relation(mCreatorUid);
            }
        }).start();

        if (mRoomId != null) {
            mMessageList = new ArrayList<>();

            events = new InkeChatRoomClient.WebSocketChannelEvents() {
                @Override
                public void onWebSocketMessage(String message) {
                    //LogUtil.info(TAG, "onWebSocketMessage(): " + message);

                    //3:::{
                    // "dest":4,
                    // "gid":"2cbsNNs6eRhat-EsRpcw",
                    // "b":{
                    //      "c":"账号信息过期，请至个人主页点［退出登录］后重新登录",
                    //      "ev":"c.jr",
                    //      "err":-3
                    // },
                    // "userid":65286584,
                    // "liveid":"1471399692134248"
                    // }

                    //3:::{
                    // "c":"账号信息过期，请至个人主页点［退出登录］后重新登录",
                    // "b":{
                    //      "ev":"s.d"
                    // },
                    // "liveid":"1471401593460980",
                    // "dest":7,
                    // "userid":65286584,
                    // "gid":"I51uQhv4w1n0w0K0ePU1"
                    // }

                    //3:::{
                    // "b":{
                    //      "ev":"s.m"
                    // },
                    // "dest":3,
                    // "userid":62083553,
                    // "p":"r",
                    // "liveid":"1468387593189654",
                    // "ms":[
                    //      {"tp":"like",
                    //      "cl":[252,154,81]}]}

                    //"ms": [
                    //{
                    //    "c": "我们提倡绿色直播，封面和直播内容含吸烟、低俗、引诱、暴露等都将会被封停账号，网警24小时在线巡查哦！",
                    //    "tp": "sys"
                    //},
                    //{
                    //    "c": "亲爱的小主，由于今天微信提现故障，导致多次提现未扣除相应映票。系统将自动扣除相应映票，对此给您带来的困扰表示歉意。",
                    //    "tp": "sys"
                    //},

                    //3:::{
                    // "b":{
                    //      "ev":"s.m"
                    // },
                    // "dest":3,
                    // "userid":115234184,
                    // "p":"r",
                    // "liveid":"1468387593189654",
                    // "ms":[
                    //      {"to":0,
                    //      "c":"拿高点。二维码",
                    //      "from":
                    //          {"dsc":"成全是让别人解脱，而解脱是成全自己的开始。",
                    //          "rvfr":"",
                    //          "lc":"",
                    //          "vf":0,
                    //          "nic":"小潘潘",
                    //          "ptr":"http://wx.qlogo.cn/mmopen/C9QznKczMm1HTKe1VxqiaBLg9MCCFKJnz0iabRDX3qJ4sXoNuwxVMEFGicwsqN1icKxXxH6OALvgVib2zLZYy6Ike3f3n6JoYtD9a/0",
                    //          "id":115234184,
                    //          "gd":0,
                    //          "lvl":5,
                    //          "vfr":"",
                    //          "rvf":3},
                    //      "tp":"pub"}]}

                    int index = message.indexOf(":::");
                    if (index > 0) {
                        String json_str = message.substring(index + 3, message.length());
                        try {
                            JSONObject json = new JSONObject(json_str);
                            int userid = json.getInt("userid");
                            JSONObject brief = json.getJSONObject("b");
                            String event = brief.getString("ev");
                            int err = brief.optInt("err");
                            if (err != 0) {
                                String c = brief.optString("c");
                                LogUtil.error(TAG, "chat room error: " + c);
                                addMessgage("系统错误: ", c);

                                mRetry = 10;// no more retry
                                return;
                            }

                            JSONArray ms = json.optJSONArray("ms");
                            if (ms != null) {
                                for (int j=0;j<ms.length();j++) {
                                    JSONObject ms_item = ms.getJSONObject(j);
                                    String type = ms_item.getString("tp");
                                    if (type.equals("pub")) {
                                        String comment = ms_item.optString("c");
                                        JSONObject from = ms_item.getJSONObject("from");
                                        String nic = from.getString("nic");
                                        int id = from.getInt("id");
                                        int level = from.getInt("lvl");

                                        String who = String.format(Locale.US,
                                                "%s[%d]说: ", nic, level);

                                        addMessgage(who, comment);

                                        LogUtil.info(TAG, String.format(Locale.US,
                                                "user comment: %s(%d)[%d]: %s",
                                                nic, level, id, comment));
                                    }
                                    else if (type.equals("sys")) {
                                        String comment = ms_item.optString("c");
                                        addMessgage("系统消息:", comment);
                                    }
                                    else if (type.equals("like")) {
                                        //"cl":[252,154,81]}]}
                                        JSONArray color = ms_item.getJSONArray("cl");
                                        int r = color.getInt(0);
                                        int g = color.getInt(1);
                                        int b = color.getInt(2);
                                        LogUtil.info(TAG, String.format(Locale.US,
                                                "点亮了：(%d %d %d)", r, g, b));
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }

                @Override
                public void onWebSocketClose() {
                    if (!mbActivityRunning)
                        return;

                    mRetry++;
                    if (mRetry < 5) {
                        InkePlayerActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                LogUtil.info(TAG, "retrying to connect to chat room...");
                                Toast.makeText(InkePlayerActivity.this,
                                        "聊天室连接重试中...", Toast.LENGTH_SHORT).show();

                                //new ChatroomTask().execute(mSlot);
                                new ChatroomTask().execute(String.valueOf(mCreatorUid), mRoomId);
                            }
                        });
                    }

                }

                @Override
                public void onWebSocketError(String description) {

                }

                @Override
                public void onWebSocketJoinRoom(String roomId) {
                    InkePlayerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mScrollView.setVisibility(View.VISIBLE);
                            mBtnSendMessage.setEnabled(true);

                            addMessgage("系统:", "聊天室已连接");
                        }
                    });
                }

                @Override
                public void onWebSocketLeaveRoom() {

                }
            };

            new ChatroomTask().execute(String.valueOf(mCreatorUid), mRoomId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mbActivityRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        mbActivityRunning = false;

        if (mChatClient != null) {
            mChatClient.disconnect();
        }

        if (mPlayer != null) {
            try {
                mPlayer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mPlayer.release();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(getApplication());
        menuInflater.inflate(R.menu.inke_player_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isFollowing())
            menu.getItem(0).setTitle("取消关注");
        else
            menu.getItem(0).setTitle("关注");

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.follow:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String action = isFollowing() ? "取消关注" : "关注";
                        final boolean success = isFollowing() ?
                                InkeUtil.unfollow(mCreatorUid) : InkeUtil.follow(mCreatorUid);

                        mRelation = InkeUtil.relation(mCreatorUid);

                        InkePlayerActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                Toast.makeText(InkePlayerActivity.this,
                                        action + " " + (success ? "成功" : "失败"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();
            default:
                break;
        }

        return true;
    }

    private boolean isFollowing() {
        return (mRelation != null && mRelation.equals("following"));
    }

    private class ChatroomTask extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                if (mChatClient != null) {
                    mChatClient.disconnect();
                }

                //String ws_url = result.replace("http://", "ws://");
                String ws_url = result;
                mChatClient = new InkeChatRoomClient(
                        ws_url, mRoomId, mbSimpleAll ? "hot" : "new");
                mChatClient.setEventLisener(events);
                if (mChatClient.connect())
                    return;
            }

            Toast.makeText(InkePlayerActivity.this, "连接聊天室失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... params) {
            /*List<String> chatIpList = InkeUtil.chatList(params[0]);
            if (chatIpList == null || chatIpList.isEmpty())
                return null;

            String url = chatIpList.get(0);
            return InkeUtil.chatServer(url);*/

            return InkeUtil.chatServer2(params[0], params[1]);
        }
    }


    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    };

    private GestureDetector mGestureDetector =
            new GestureDetector(getApplication(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onDoubleTap(MotionEvent event) {
                    mbMessageShowing = !mbMessageShowing;

                    if (mbMessageShowing)
                        mTvMessageHistory.setVisibility(View.VISIBLE);
                    else
                        mTvMessageHistory.setVisibility(View.GONE);
                    return true;
                }
            });

    private void addMessgage(String who, String msg) {
        if (mMessageList.size() > 30)
            mMessageList.remove(0);

        String str = String.format(Locale.US,
                "<font color=\"red\">%s</font> <font color=\"white\">%s</font>", who, msg);
        mMessageList.add(str);

        final StringBuffer sb = new StringBuffer();
        for (int i=0;i<mMessageList.size();i++) {
            sb.append(mMessageList.get(i));
            sb.append("<br/>");
        }

        /*SpannableStringBuilder style = new SpannableStringBuilder("备注:签收人(张三)");
        style.setSpan(
                new ForegroundColorSpan(Color.BLUE),
                0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        style.setSpan(
                new ForegroundColorSpan(Color.RED),
                7, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mTvMessageHistory.setText(style);*/

        //String str = String.format("状态 ：<font color=\"#666666\">%s", "已售");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvMessageHistory.setText(Html.fromHtml(sb.toString()));

                mScrollView.smoothScrollTo(0, mTvMessageHistory.getBottom());

                int users = 0;
                if (mChatClient != null)
                    users = mChatClient.getCurrentUsers();
                mTvInfo.setText(
                        String.format(Locale.US, "%s(%d), 在线人数: %d",
                                mCreatorName, mCreatorUid, users));
            }
        });
    }

    private boolean SetupPlayer() {
        mStartMsec = System.currentTimeMillis();

        MediaPlayer.DecodeMode mode;
        if (mPlayerImpl == 1)
			mode = MediaPlayer.DecodeMode.HW_SYSTEM;
		else if (mPlayerImpl == 2)
            mode = MediaPlayer.DecodeMode.HW_XOPLAYER;
        else
            mode = MediaPlayer.DecodeMode.SW;

        mPlayer = new MediaPlayer(mode);
        mPlayer.reset();

        mPlayer.setDisplay(mHolder);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setScreenOnWhilePlaying(true);

        mPlayer.setOnInfoListener(mOnInfoListener);
        mPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mPlayer.setOnPreparedListener(mOnPreparedListener);
        mPlayer.setOnErrorListener(mOnErrorListener);
        mPlayer.setOnCompletionListener(mOnCompletionListener);

        try {
            mPlayer.setDataSource(mPlayUrl);
            mIsBuffering = true;
            mBufferingProgressBar.setVisibility(View.VISIBLE);

            mPlayer.prepareAsync();
            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;

        SetupPlayer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
