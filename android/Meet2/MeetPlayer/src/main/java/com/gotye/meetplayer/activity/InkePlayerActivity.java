package com.gotye.meetplayer.activity;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.inke.InkeUtil;
import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.ui.MyPreView2;
import com.gotye.meetplayer.util.Util;
import com.gotye.meetsdk.player.MediaPlayer;

import de.tavendo.autobahn.WebSocket.WebSocketConnectionObserver;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
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

    private String wsServerUrl;
    private WebSocketConnection ws;
    private WebSocketObserver wsObserver;
    private WebSocketChannelEvents events;
    private WebSocketConnectionState state = WebSocketConnectionState.NEW;
    private Thread mHeartBeatThr;

    private String mRoomId;// id=1468316035495368
    private int mSlot;
    private boolean mbSimpleAll;
    private List<String> mMessageList;

    private Button mBtnSendMessage;
    private EditText mEtMessage;
    private TextView mTvMessageHistory;
    private ScrollView mScrollView;

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

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Intent intent = getIntent();
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

        mBtnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEtMessage.getText() != null) {
                    String msg = mEtMessage.getText().toString();
                    sendChatMessage(msg);
                    mEtMessage.setText("");
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

        if (mRoomId != null) {
            mMessageList = new ArrayList<>();

            events = new WebSocketChannelEvents() {
                @Override
                public void onWebSocketMessage(String message) {
                    //LogUtil.info(TAG, "onWebSocketMessage(): " + message);

                    //3:::{
                    // "b":{"ev":"s.m"},
                    // "dest":3,
                    // "userid":62083553,
                    // "p":"r",
                    // "liveid":"1468387593189654",
                    // "ms":[
                    //      {"tp":"like",
                    //      "cl":[252,154,81]}]}

                    //3:::{
                    // "b":{"ev":"s.m"},
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
                            JSONArray ms = json.optJSONArray("ms");
                            if (ms != null) {
                                JSONObject ms_item = ms.getJSONObject(0);
                                String comment = ms_item.optString("c");
                                if (!comment.isEmpty()) {
                                    JSONObject from = ms_item.getJSONObject("from");
                                    String nic = from.getString("nic");
                                    int id = from.getInt("id");
                                    int level = from.getInt("lvl");

                                    String item = String.format(Locale.US,
                                            "%s[%d]说: %s", nic, level, comment);

                                    addMessgage(item);

                                    LogUtil.info(TAG, String.format(Locale.US,
                                            "user comment: %s(%d)[%d]: %s", nic, level, id, comment));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }

                @Override
                public void onWebSocketClose() {

                }

                @Override
                public void onWebSocketError(String description) {

                }
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<String> chatIpList = InkeUtil.chatList(mSlot);
                    if (chatIpList == null) {
                        InkePlayerActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(InkePlayerActivity.this,
                                        "获取聊天室信息失败", Toast.LENGTH_SHORT).show();
                            }
                        });

                        return;
                    }

                    for (int i=0;i<chatIpList.size();i++) {
                        String url = chatIpList.get(i); // "http://60.205.82.28:81"
                        LogUtil.info(TAG, String.format(Locale.US,
                                "chat room #%d: %s", i, url));

                        String chat_url = InkeUtil.chatServer(url);
                        if (chat_url == null) {
                            LogUtil.error(TAG, "failed to get chatroom server url");
                            break;
                        }

                        final String ws_url = chat_url.replace("http://", "ws://");
                        InkePlayerActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connect(ws_url);
                            }
                        });

                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (state == WebSocketConnectionState.CONNECTED ||
                                state == WebSocketConnectionState.LOGIN) {
                            LogUtil.info(TAG, "ws server connected, stop trying");
                            break;
                        }
                    }


                }
            }).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        disconnect();

        if (mPlayer != null) {
            try {
                mPlayer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mPlayer.release();
        }
    }

    private void addMessgage(String msg) {
        if (mMessageList.size() > 30)
            mMessageList.remove(0);
        mMessageList.add(msg);

        StringBuffer sb = new StringBuffer();
        for (int i=0;i<mMessageList.size();i++) {
            sb.append(mMessageList.get(i));
            sb.append("\n");
        }
        mTvMessageHistory.setText(sb.toString());

        mScrollView.smoothScrollTo(0, mTvMessageHistory.getBottom());
    }

    private void sendChatMessage(String msg) {
        if (state != WebSocketConnectionState.LOGIN) {
            LogUtil.error(TAG, "WebSocket sendMessage() in state " + state);
            return;
        }

        JSONObject json = new JSONObject();
        try {
            //json:{
            // "to":0,
            // "b":{"ev":"c.jr"},
            // "c":"hello",
            // "tp":"pub"}
            JSONObject b = new JSONObject();
            b.put("ev", "c.ch");

            json.put("to", 0);
            json.put("b", b);
            json.put("c", msg);
            json.put("tp", "pub");
            LogUtil.info(TAG, "sendMessage: " + json.toString());
            ws.sendTextMessage("3:::" + json.toString());

            addMessgage("我说: " + msg);
        } catch (JSONException e) {
            LogUtil.error(TAG, "WebSocket register JSON error: " + e.getMessage());
        }
    }

    private void leaveRoom() {
        if (state != WebSocketConnectionState.CONNECTED) {
            LogUtil.error(TAG, "WebSocket login() in state " + state);
            return;
        }

        JSONObject json = new JSONObject();
        try {
            JSONObject b = new JSONObject();
            b.put("ev", "c.lr"); //leave
            json.put("b", b);
            LogUtil.info(TAG, "leaveRoom: " + json.toString());
            ws.sendTextMessage("3:::" + json.toString());
            state = WebSocketConnectionState.LOGOUT;
        } catch (JSONException e) {
            LogUtil.error(TAG, "WebSocket register JSON error: " + e.getMessage());
        }
    }

    private void joinRoom() {
        setupHeartBeat();

        JSONObject json = new JSONObject();
        try {
            //json:{"b":{"ev":"c.jr"},
            // "rid":"1468311806818655",
            // "city":"",
            // "from":"new"}
            JSONObject b = new JSONObject();
            b.put("ev", "c.jr"); // join
            json.put("b", b);
            json.put("rid", mRoomId);
            json.put("city", "beijing");
            json.put("from", mbSimpleAll ? "hot" : "new");
            LogUtil.info(TAG, "joinRoom: " + json.toString());
            ws.sendTextMessage("3:::" + json.toString());
            state = WebSocketConnectionState.LOGIN;
        } catch (JSONException e) {
            LogUtil.error(TAG, "WebSocket joinRoom JSON error: " + e.getMessage());
        }
    }

    void setupHeartBeat() {
        mHeartBeatThr = new Thread(new Runnable() {
            @Override
            public void run() {
                LogUtil.info(TAG, "heart beat thread started");

                while(!mHeartBeatThr.isInterrupted() &&
                        (state == WebSocketConnectionState.LOGIN ||
                                state == WebSocketConnectionState.CONNECTED)) {
                    ws.sendTextMessage("2:::");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        mHeartBeatThr.interrupt();
                    }
                }

                LogUtil.info(TAG, "hear beat thread exited");
            }
        });
        mHeartBeatThr.start();
    }

    void resetHeartBeat() {
        if (mHeartBeatThr != null) {
            mHeartBeatThr.interrupt();
            try {
                LogUtil.info(TAG, "before join mHeartBeatThr");
                mHeartBeatThr.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mHeartBeatThr = null;
            LogUtil.info(TAG, "after join mHeartBeatThr");
        }
    }

    /**
     * Callback interface for messages delivered on WebSocket.
     * All events are dispatched from a looper executor thread.
     */
    public interface WebSocketChannelEvents {
        public void onWebSocketMessage(final String message);
        public void onWebSocketClose();
        public void onWebSocketError(final String description);
    }

    private class WebSocketObserver implements WebSocketConnectionObserver {
        @Override
        public void onOpen() {
            LogUtil.info(TAG, "WebSocket connection opened to: " + wsServerUrl);
            InkePlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    state = WebSocketConnectionState.CONNECTED;
                    // Check if we have pending register request.
                    joinRoom();
                }
            });
        }

        @Override
        public void onClose(WebSocketCloseNotification code, String reason) {
            LogUtil.info(TAG, "WebSocket connection closed. Code: " + code
                    + ". Reason: " + reason + ". State: " + state);
            InkePlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (state != WebSocketConnectionState.CLOSED) {
                        state = WebSocketConnectionState.CLOSED;
                        events.onWebSocketClose();
                    }
                }
            });
        }

        @Override
        public void onTextMessage(final String payload) {
            //LogUtil.info(TAG, "onTextMessage: " + payload);
            InkePlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (state == WebSocketConnectionState.LOGIN) {
                        events.onWebSocketMessage(payload);
                    }
                }
            });
        }

        @Override
        public void onRawTextMessage(byte[] payload) {
        }

        @Override
        public void onBinaryMessage(byte[] payload) {
        }
    }

    private void connect(final String wsUrl) {
        if (state != WebSocketConnectionState.NEW) {
            LogUtil.error(TAG, "WebSocket is already connected.");
            return;
        }

        wsServerUrl = wsUrl;
        LogUtil.info(TAG, "Connecting WebSocket to: " + wsUrl);
        ws = new WebSocketConnection();
        wsObserver = new WebSocketObserver();
        try {
            ws.connect(new URI(wsServerUrl), wsObserver);
        } catch (URISyntaxException e) {
            LogUtil.error(TAG, "URI error: " + e.getMessage());
        } catch (WebSocketException e) {
            LogUtil.error(TAG, "WebSocket connection error: " + e.getMessage());
        }
    }

    public void disconnect() {
        LogUtil.info(TAG, "Disonnect WebSocket. State: " + state);

        resetHeartBeat();

        leaveRoom();

        // Close WebSocket in CONNECTED or ERROR states only.
        if (state == WebSocketConnectionState.CONNECTED
                || state == WebSocketConnectionState.ERROR) {
            ws.disconnect();
            state = WebSocketConnectionState.CLOSED;
        }
        LogUtil.info(TAG, "Disonnecting WebSocket done.");
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
