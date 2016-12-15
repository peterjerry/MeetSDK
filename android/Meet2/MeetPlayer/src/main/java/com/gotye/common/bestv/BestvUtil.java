package com.gotye.common.bestv;

import com.gotye.common.util.LogUtil;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Michael.Ma on 2016/7/19.
 */
public class BestvUtil {
    private final static String TAG ="BestvUtil";

    private final static String UserAgent =
            "Mozilla/5.0(Linux;U;Android4.4.4;zh-cn;HM1SBuild/KTU84P)" +
            "AppleWebKit/533.1(KHTML,likeGecko)Version/4.0MobileSafari/533.1";

    private final static String init_api_surfix =
            "bn=Xiaomi" +
                    "&mn=HM1S" +
                    "&os=Android4.4.4" +
                    "&ut=mac&mac=28e31f5c2506" +
                    "&rs=720x1280" +
                    "&net=WIFI" +
                    "&mnc=46001" +
                    "&pnm=com.bestv.app" +
                    "&anm=BesTV" +
                    "&ua=" + UserAgent +
                    "&lt=1" +
                    "&lct=31.282104,121.437172";

    public static String getToken() {

        String request_params =
                "app=android" +
                        "&channelid=a6d547d2-5ac8-40cc-a70c-71dd0a9e84c3" +
                        "&timestamp=" + System.currentTimeMillis() / 1000L +
                        "&" + init_api_surfix;
        request_params = sortParams(request_params);
        String magic_key = "45e245eef810a7194cad416858a04ba8";

        try {
            byte[] encoded_data = encodeParams(request_params.getBytes(), magic_key.getBytes());
            String signature = convertReadable(encoded_data).toLowerCase();
            LogUtil.info(TAG, "signature: " + signature);

            String post_url = "https://bestvapi.bestv.cn/app/init?" +
                    request_params +
                    "&signature=" + signature;

            JSONObject json = new JSONObject();
            json.put("device_id", "865624023658835");

            LogUtil.info(TAG, "post url: " + post_url);
            LogUtil.info(TAG, "post data: " + json.toString());

            String result = postBestvHttpPage(post_url, json.toString());
            if (result == null) {
                LogUtil.error(TAG, "failed to post");
                return null;
            }

            JSONObject root = new JSONObject(result);
            int code = root.getInt("code");
            if (code != 0) {
                LogUtil.info(TAG, "post code is error: " + code);
                return null;
            }
            // token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJkZXZpY2VJZCI6Ijg2NDY5MDAyODUzMDM5NSIsInRpbWVzdGFtcCI6MTQ2ODg5MjI4MiwiY2hhbm5lbElkIjoiYTZkNTQ3ZDItNWFjOC00MGNjLWE3MGMtNzFkZDBhOWU4NGMzIn0.AKz9HAa-rip4vqgI962xYNq_fOE1HLUldDogCYCKw9Q
            String token = root.getString("token");
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.error(TAG, "Exception: " + e.getMessage());
        }

        return null;
    }

    public static String sortParams(String params) {
        String[] param_items = params.split("&");
        Arrays.sort(param_items);
        StringBuilder sb = new StringBuilder();

        for (int i=0;i<param_items.length;i++) {
            String item = param_items[i];
            String[] tmp = item.split("=");
            if (i > 0)
                sb.append("&");
            if (tmp.length >= 2 && !isEmpty(tmp[1])) {
                sb.append(item);
            }
        }

        return sb.toString();
    }

    public static byte[] encodeParams(byte[] bytes1, byte[] bytes2) throws Exception {
        SecretKeySpec key = new SecretKeySpec(bytes2, "HmacSHA256");
        Mac localMac = Mac.getInstance(key.getAlgorithm());
        localMac.init(key);
        return localMac.doFinal(bytes1);
    }

    public static String convertReadable(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (int i=0;i<bytes.length;i++) {
            String str = Integer.toHexString(bytes[i] & 0xFF);
            if (str.length() == 1) {
                sb.append('0');
            }
            sb.append(str.toUpperCase());
        }

        return sb.toString();
    }

    private static String postBestvHttpPage(String url, String params) {
        InputStream is = null;
        ByteArrayOutputStream os = null;

        try {
            HttpURLConnection conn;

            URL realUrl = new URL(url);

            //ignore https certificate validation |忽略 https 证书验证
            if (realUrl.getProtocol().toUpperCase().equals("HTTPS")) {
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) realUrl.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) realUrl.openConnection();
            }

            conn.setRequestProperty("User-Agent", UserAgent);
            conn.setRequestProperty("app", "android");
            conn.setRequestProperty("channel", "standard");
            conn.setRequestProperty("release", "1");
            conn.setRequestProperty("version", "2.1.4");

            conn.setRequestMethod("POST");
            if (params != null) {
                byte[] bypes = params.getBytes();
                conn.getOutputStream().write(bypes);// 输入参数
            }
            conn.setReadTimeout(5000);// 设置超时的时间
            conn.setConnectTimeout(5000);// 设置链接超时的时间

            conn.connect();

            if (conn.getResponseCode() != 200) {
                LogUtil.error(TAG, "http response is not 200 " + conn.getResponseCode());
                return null;
            }

            // 获取响应的输入流对象
            is = conn.getInputStream();

            // 创建字节输出流对象
            os = new ByteArrayOutputStream();
            // 定义读取的长度
            int len = 0;
            // 定义缓冲区
            byte buffer[] = new byte[1024];
            // 按照缓冲区的大小，循环读取
            while ((len = is.read(buffer)) != -1) {
                // 根据读取的长度写入到os对象中
                os.write(buffer, 0, len);
            }

            // 返回字符串
            return new String(os.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // 释放资源
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        // Android use X509 cert
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private static boolean isEmpty(String str) {
        return (str == null) || (str.trim().length() == 0) || (str.trim().equals(""));
    }
}
