package com.gotye.common.inke;

import com.gotye.common.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

/**
 * Created by Michael.Ma on 2016/7/14.
 */
public class InkeChatRoomClient {

    /**
     * Possible WebSocket connection states.
     */
    private enum WebSocketConnectionState {
        NEW, CONNECTED, LOGIN, LOGOUT, CLOSED, ERROR
    }

    private final static String TAG = "InkeChatRoomClient";

    private String mServerUrl;
    private String mRoomId;
    private String mFromCatalog;
    private int mUsers;

    private WebSocketConnection ws;
    private WebSocketObserver wsObserver;
    private WebSocketChannelEvents events;
    private WebSocketConnectionState state = WebSocketConnectionState.NEW;
    private Thread mHeartBeatThr;

    public InkeChatRoomClient(String wsUrl, String roomId, String from) {
        this.mServerUrl     = wsUrl;
        this.mRoomId        = roomId;
        this.mFromCatalog   = from;
        this.mUsers         = 0;
    }

    public void setEventLisener(WebSocketChannelEvents listener) {
        events = listener;
    }

    public int getCurrentUsers() {
        return mUsers;
    }

    public boolean connect() {
        if (state != WebSocketConnectionState.NEW) {
            LogUtil.error(TAG, "WebSocket is already connected.");
            return false;
        }

        LogUtil.info(TAG, "Connecting WebSocket to: " + mServerUrl);
        ws = new WebSocketConnection();
        wsObserver = new WebSocketObserver();
        try {
            ws.connect(new URI(mServerUrl), wsObserver);
            return true;
        } catch (URISyntaxException e) {
            LogUtil.error(TAG, "URI error: " + e.getMessage());
        } catch (WebSocketException e) {
            LogUtil.error(TAG, "WebSocket connection error: " + e.getMessage());
        }

        return false;
    }

    public void disconnect() {
        LogUtil.info(TAG, "Disonnect WebSocket. State: " + state);

        stopHeartBeat();

        leaveRoom();

        // Close WebSocket in CONNECTED or ERROR states only.
        if (state == WebSocketConnectionState.CONNECTED ||
                state == WebSocketConnectionState.ERROR ||
                state == WebSocketConnectionState.LOGIN ||
                state == WebSocketConnectionState.LOGOUT
                ) {
            ws.disconnect();
            state = WebSocketConnectionState.CLOSED;
        }
        LogUtil.info(TAG, "Disonnecting WebSocket done.");
    }

    public void sendChatMessage(String msg) {
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
        } catch (JSONException e) {
            LogUtil.error(TAG, "WebSocket register JSON error: " + e.getMessage());
        }
    }

    private void joinRoom() {
        startHeartBeat();

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
            json.put("from", mFromCatalog); // hot(simpleall) new
            LogUtil.info(TAG, "joinRoom: " + json.toString());
            ws.sendTextMessage("3:::" + json.toString());
        } catch (JSONException e) {
            LogUtil.error(TAG, "WebSocket joinRoom JSON error: " + e.getMessage());
        }
    }

    private void leaveRoom() {
        if (state != WebSocketConnectionState.LOGIN) {
            LogUtil.error(TAG, "WebSocket leave() in state " + state);
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

    void startHeartBeat() {
        mHeartBeatThr = new Thread(new Runnable() {
            @Override
            public void run() {
                LogUtil.info(TAG, "heart beat thread started");

                while(!mHeartBeatThr.isInterrupted() &&
                        (state == WebSocketConnectionState.LOGIN ||
                                state == WebSocketConnectionState.CONNECTED)) {
                    ws.sendTextMessage("2:::");

                    mUsers = InkeUtil.getUsers(mRoomId);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
						LogUtil.info(TAG, "heart beat thread interrupted");
                        break;
                    }
                }

                LogUtil.info(TAG, "hear beat thread exited");
            }
        });
        mHeartBeatThr.start();
    }

    void stopHeartBeat() {
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
        public void onWebSocketMessage(String message);
        public void onWebSocketClose();
        public void onWebSocketError(String description);
        public void onWebSocketJoinRoom(String roomId);
        public void onWebSocketLeaveRoom();
    }

    private class WebSocketObserver implements WebSocket.WebSocketConnectionObserver {
        @Override
        public void onOpen() {
            LogUtil.info(TAG, "WebSocket connection opened to: " + mServerUrl);

            state = WebSocketConnectionState.CONNECTED;

            joinRoom();
        }

        @Override
        public void onClose(WebSocketCloseNotification code, String reason) {
            LogUtil.info(TAG, "WebSocket connection closed. Code: " + code
                    + ". Reason: " + reason + ". State: " + state);

            if (state != WebSocketConnectionState.CLOSED) {
                state = WebSocketConnectionState.CLOSED;
                events.onWebSocketClose();
            }
        }

        @Override
        public void onTextMessage(final String payload) {
            //LogUtil.info(TAG, "onTextMessage: " + payload);

            // 3:::{
            // "dest":4,
            // "gid":"2cbsNNs6eRhat-EsRpcw",
            // "b":{
            //      "c":"操作成功",
            //      "ev":"c.lg",
            //      "err":0
            //      },
            // "userid":65286584,
            // "liveid":"0"
            // }

            if (state == WebSocketConnectionState.CONNECTED) {
                if (payload.startsWith("3:::")) {
                    try {
                        JSONObject json = new JSONObject(payload.substring(4));

                        int dest = json.getInt("dest");
                        JSONObject b = json.getJSONObject("b");
                        int err =  b.optInt("err");
                        String c = b.optString("c");
                        if (err == 0) {
                            state = WebSocketConnectionState.LOGIN;

                            if (events != null)
                                events.onWebSocketJoinRoom(mRoomId);
                        }

                        String userid = json.getString("userid");
                        String liveid = json.getString("liveid");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (state == WebSocketConnectionState.LOGIN) {
                events.onWebSocketMessage(payload);
            }
        }

        @Override
        public void onRawTextMessage(byte[] payload) {
        }

        @Override
        public void onBinaryMessage(byte[] payload) {
        }
    }
}
