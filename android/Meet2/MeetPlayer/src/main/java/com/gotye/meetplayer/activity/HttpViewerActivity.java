package com.gotye.meetplayer.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.YKUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.Util;

import java.net.URL;

public class HttpViewerActivity extends AppCompatActivity {
	private final static String TAG = "HttpViewerActivity";

    private Button mBtnGo;
    private AutoCompleteTextView mEtUrl;
	private WebView mBrowser;
    private ProgressBar mBar;
    private int mPlayerImpl = 1;
    private String initUrl;
    private String lastUrl;

    private final String[] urls = new String[] {
            "youku.com",
            "sina.com",
            "baidu.com",
            "iyiqi.com" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_http_viewer);

		Intent intent = getIntent();
		Uri uri = intent.getData();
        initUrl = uri.toString();

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");

        mBrowser = (WebView) findViewById(R.id.webView);
        mBtnGo = (Button) findViewById(R.id.btn_go);
        mEtUrl = (AutoCompleteTextView) findViewById(R.id.acet_url);
        mBar = (ProgressBar) findViewById(R.id.myProgressBar);

        mEtUrl.setText(initUrl);

        ArrayAdapter<String> av = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, urls);
        mEtUrl.setAdapter(av);

        mBrowser.getSettings().setJavaScriptEnabled(true);
        mBrowser.getSettings().setSupportZoom(true);
        mBrowser.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                LogUtil.info(TAG, "onProgressChanged(): " + newProgress);

                if (newProgress == 100) {
                    mBar.setVisibility(View.INVISIBLE);
                } else {
                    if (View.INVISIBLE == mBar.getVisibility()) {
                        mBar.setVisibility(View.VISIBLE);
                    }
                    mBar.setProgress(newProgress);
                }

                super.onProgressChanged(view, newProgress);
            }
        });
        mBrowser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.endsWith("mp4") || url.endsWith("flv") ||
                        url.endsWith("mp3") || url.endsWith("wmv") ||
                        url.endsWith("ts") || url.endsWith("avi") ||
                        url.endsWith("ape") || url.endsWith("flac") ||
                        url.endsWith("mov") || url.endsWith("mkv") ||
                        url.endsWith("wav") ||
                        url.startsWith("rtmp://") || url.startsWith("rtsp://")) {
                    Intent intent = new Intent(HttpViewerActivity.this, VideoPlayerActivity.class);
                    intent.setData(Uri.parse(url));
                    intent.putExtra("player_impl", mPlayerImpl);
                    intent.putExtra("title", "N/A");
                    startActivity(intent);
                    return true;
                }
                else if (url.contains("v.youku.com")) {
                    // http://v.youku.com/v_show/id_XMTU2NzY4NjM2NA==_ev_5.html

                    String vid = YKUtil.getVid(url);
                    if (vid != null) {
                        new ParseVideoTask().execute();
                        return true;
                    }
                }

                lastUrl = mBrowser.getUrl();
                mEtUrl.setText(url);
                LogUtil.info(TAG, "last url saved to: " + lastUrl);
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        LogUtil.info(TAG, "open url: " + initUrl);
        mBrowser.loadUrl(initUrl);

        mBtnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBrowser.loadUrl(mEtUrl.getText().toString());
            }
        });

        Util.checkNetworkType(this);
	}

    @Override
    public void onBackPressed() {
        if (mBrowser.canGoBack()) {
            mBrowser.goBack();
            return;
        }

        super.onBackPressed();
    }

    public class ParseVideoTask extends AsyncTask<String, Integer, Boolean> {

        YKUtil.ZGUrl zgUrl;
        String play_url;

        @Override
        protected void onPostExecute(Boolean ret) {
            if (ret) {
                //start_player("N/A", play_url);

                Intent intent = new Intent(HttpViewerActivity.this, PlayYoukuActivity.class);
                intent.putExtra("url_list", zgUrl.urls);
                intent.putExtra("duration_list", zgUrl.durations);
                intent.putExtra("title", "N/A");
                intent.putExtra("ft", 2);
                intent.putExtra("player_impl", mPlayerImpl);
                startActivity(intent);
            } else {
                Toast.makeText(HttpViewerActivity.this, "解析视频地址失败", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String video_url = params[0];

            if (video_url.contains("youku")) {
                String vid = YKUtil.getVid(video_url);
                zgUrl = YKUtil.getPlayUrl2(HttpViewerActivity.this, vid);
                if (zgUrl == null) {
                    LogUtil.error(TAG, "failed to get ZGUrl, vid " + vid);
                    return false;
                }

                return true;
            }

            return false;
        }
    }
}
