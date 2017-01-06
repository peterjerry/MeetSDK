package com.able;

import com.pplive.common.bestv.BestvUtil2;
import com.pplive.common.util.LogUtil;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;

import javax.swing.JFrame;

public class BestvPanel extends JFrame {

    private final static int APP_WIDTH	= 800;
    private final static int APP_HEIGHT	= 600;

    private final static String TAG = "BestvPanel";

    public BestvPanel() {
        super();

        int[] channel_ids = new int[]{
                BestvUtil2.CHANNEL_ID_SHANGHAI,
                BestvUtil2.CHANNEL_ID_WEISHI,
                BestvUtil2.CHANNEL_ID_CCTV
        };

        List<BestvUtil2.BestvChannel> list =  BestvUtil2.getChannel(BestvUtil2.CHANNEL_ID_SHANGHAI);
        int size = list.size();
        for (int i=0;i<size;i++) {
            BestvUtil2.BestvChannel chn = list.get(i);
            LogUtil.info(TAG, "chn: " + chn.title + " " + chn.nowPlay);
        }

        //this.setLayout(null);
    }

    public static void main(String[] args) {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int screen_width = dim.width;
        int screen_height = dim.height;
        int x, y;

        if (screen_width <= APP_WIDTH || screen_height <= APP_HEIGHT) {
            x = 0;
            y = 0;
        }
        else {
            x = (screen_width - APP_WIDTH) / 2;
            y = (screen_height - APP_HEIGHT) / 2;
        }

        BestvPanel panel = new BestvPanel();
        panel.setBounds(x, y, APP_WIDTH, APP_HEIGHT);
        panel.setVisible(true);
    }
}
