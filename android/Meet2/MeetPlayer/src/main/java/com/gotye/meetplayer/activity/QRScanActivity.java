package com.gotye.meetplayer.activity;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.Util;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

public class QRScanActivity extends AppCompatActivity
        implements QRCodeView.Delegate {

    private ZXingView mQR;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);

        mQR = (ZXingView) this.findViewById(R.id.zx_view);
        mQR.setDelegate(this);

        //开始读取二维码
        mQR.startSpot();

//        mQR.startCamera();开启预览，但是并未开始识别
//        mQR.stopCamera();停止预览，并且隐藏扫描框
//        mQR.startSpot();开始识别二维码
//        mQR.stopSpot();停止识别
//        mQR.startSpotAndShowRect();开始识别并显示扫描框
//        mQR.stopSpotAndHiddenRect();停止识别并隐藏扫描框
//        mQR.showScanRect();显示扫描框
//        mQR.hiddenScanRect();隐藏扫描框
//        mQR.openFlashlight();开启闪光灯
//        mQR.closeFlashlight();关闭闪光灯
//        mQR.startSpotDelay(ms);延迟ms毫秒后开始识别
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        Toast.makeText(QRScanActivity.this, result, Toast.LENGTH_SHORT).show();

        if (result.startsWith("http://") || result.startsWith("rtmp://")) {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            String play_url = result;
            if (result.startsWith("rtmp://"))
                play_url += "?type=gotyelive";
            intent.setData(Uri.parse(play_url));
            int playerImpl = Util.readSettingsInt(this, "PlayerImpl");
            intent.putExtra("impl", playerImpl);
            if (result.startsWith("rtmp://")) {
                String title = play_url;
                int pos = play_url.lastIndexOf("/");
                if (pos > 0)
                    title = play_url.substring(pos + 1);
                intent.putExtra("title", title);
            }
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Toast.makeText(QRScanActivity.this,
                "打开相机出错！请检查是否开启权限！", Toast.LENGTH_SHORT).show();
    }
}
