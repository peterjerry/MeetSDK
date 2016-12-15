package com.gotye.common.inke;

import android.net.Uri;
import android.text.TextUtils;

import com.gotye.common.util.LogUtil;
import com.gotye.common.util.Rc4Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michael.Ma on 2016/9/1.
 */
public class UserAccountTokenManager {
    private static final String HEADER_NONCE = "x-ik-nonce";
    private static final String HEADER_SEC = "x-ik-sec";
    private static final String HEADER_TIME = "x-ik-time";

    private static final String TAG = "UserAccountTokenManager";

    private static long TOKEN_EXPIRE = 0L;
    private static String TOKEN_KEY;
    private static long TOKEN_TIMESTAMP = 0L;
    private static UserAccountTokenManager instance = null;
    private long lastRefreshTokenTime = 0L;
    private long refreshTokenTime = 0L;

    static
    {
        TOKEN_KEY = "";
    }

    private long getCurrentTimeMillis() {
        return System.currentTimeMillis() / 1000L;
    }

    public static UserAccountTokenManager ins() {
        if (instance == null) {
            instance = new UserAccountTokenManager();
        }
        return instance;
    }

    public void checkRefreshToken() {
        if ((TOKEN_EXPIRE != 0L) && (TOKEN_TIMESTAMP != 0L)
                && (getCurrentTimeMillis() - TOKEN_TIMESTAMP > TOKEN_EXPIRE - 3L)) {
            refreshToken();
        }
    }

    public Map<String, String> getRequestHeaders(String url) {
        LogUtil.info(TAG, "addHeaders:TOKEN_KEY:" + TOKEN_KEY);

        if (TextUtils.isEmpty(TOKEN_KEY)) {
            refreshToken();
            return null;
        }

        LogUtil.info(TAG, "addHeaders:time:" + String.valueOf(TOKEN_TIMESTAMP));
        Map<String, String> headers = new HashMap<>();
        headers.put("x-ik-time", String.valueOf(TOKEN_TIMESTAMP));

        long curr = getCurrentTimeMillis();
        long refresh = this.refreshTokenTime;
        String nonce = String.valueOf(TOKEN_TIMESTAMP + (curr - refresh));
        LogUtil.info(TAG, "addHeaders:nonce:" + nonce);
        headers.put("x-ik-nonce", nonce);

        url = Uri.parse(url).getPath();
        int uid = 177947461; // UserManager.ins().getUserID()
        String rc4_str = url + "#" + uid + "#" + nonce;
        LogUtil.info(TAG, "rc4加密的Str=" + rc4_str);

        //headers.put("x-ik-sec",
        //        MD5Util.charsToHexString(Rc4Util.HloveyRC4(rc4_str, TOKEN_KEY).toCharArray()));
        return headers;
    }

    public void refreshToken() {
        LogUtil.info(TAG, "refreshToken");

        if (System.currentTimeMillis() - lastRefreshTokenTime <= 500) {
            LogUtil.warn(TAG, "refreshToken:太过频繁");
            return;
        }
        lastRefreshTokenTime = System.currentTimeMillis();

        //userAccountTokenListener:onSuccess:
        // token:1b026fa44e7ed9ed17e2ebf4d3716fb71861a1017bedc7d79885f1be

        TOKEN_KEY = "1b026fa44e7ed9ed17e2ebf4d3716fb71861a1017bedc7d79885f1be";
    }
}
