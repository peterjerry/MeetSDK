package com.able;

import com.able.util.Util;
import com.pplive.common.bestv.BestvUtil2;
import com.pplive.common.util.LogUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

public class BestvPanel extends JFrame {

    private final static int APP_WIDTH	= 800;
    private final static int APP_HEIGHT	= 600;

    private final static String TAG = "BestvPanel";

    private static String player_path = "D:/Program Files/ffmpeg/ffplay.exe";
    private static String play_opt = "";

    private JLabel mLabelInfo;

    private JList<String> mListItemChannel              = null;
    private DefaultListModel<String> mListModelChannel  = null;
    private JScrollPane mScrollPaneChannel              = null;

    private JList<String> mListItemDay                  = null;
    private DefaultListModel<String> mListModelDay      = null;

    private JList<String> mListItemProg                 = null;
    private DefaultListModel<String> mListModelProg 	= null;
    private JScrollPane mScrollPaneProg                 = null;

    private JCheckBox mCbHd                             = null;

    private int cid = BestvUtil2.CHANNEL_ID_SHANGHAI;
    private int last_tid;
    private List<Map<String, Object>> channelList;
    private List<Map<String, Object>> progList;
    private String token;

    private Font f = new Font("宋体", 0, 18);

    public BestvPanel() {
        super();

        String strConfig = Util.readFileContent("./config.txt");
        if (strConfig.length() > 0) {
            StringTokenizer st = new StringTokenizer(strConfig, "\n", false);
            while (st.hasMoreElements()) {
                String strLine = (String) st.nextElement();
                if (strLine.startsWith("#"))
                    continue;

                int pos = strLine.indexOf("=");
                if (pos > 0 && pos != strLine.length() - 1) {
                    String key = strLine.substring(0, pos);
                    String value = strLine.substring(pos + 1);
                    LogUtil.info(TAG, String.format("Java: key %s, value %s", key ,value));
                    if (key.equals("player_path")) {
                        player_path = value;
                        LogUtil.info(TAG, "Java: set player_path to " + player_path);
                    }
                    else if (key.equals("player_opt")) {
                        play_opt = value;
                        LogUtil.info(TAG, "Java: set play_opt to " + play_opt);
                    }
                    else if (key.equals("cid")) {
                        cid = Integer.valueOf(value);
                        LogUtil.info(TAG, "Java: set cid to " + cid);
                    }
                    else {
                        LogUtil.warn(TAG, "Java: unknown key " + key);
                    }
                }
            }
        }
        else {
            JOptionPane.showMessageDialog(
                    null, "配置文件未找到", "百事通电视",
                    JOptionPane.ERROR_MESSAGE);
        }

        mLabelInfo = new JLabel("信息");
        mLabelInfo.setBounds(20, 20, 500, 20);
        this.add(mLabelInfo);

        mListModelChannel = new DefaultListModel<>();
        mListItemChannel = new JList<>();
        mListItemChannel.setFont(f);
        mListItemChannel.setModel(mListModelChannel);
        mListItemChannel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mListItemChannel.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent event) {
                // TODO Auto-generated method stub
                int index = mListItemChannel.getSelectedIndex();
                if (index == -1)
                    return;

                int button = event.getButton();
                if (button == 1) { // Left button
                    if (event.getClickCount() == 1) {
                        Map<String, Object> channel = channelList.get(index);
                        int tid = (Integer) channel.get("tid");
                        last_tid = tid;
                        update_program(tid, -1);
                    }
                }

            }
        });

        mScrollPaneChannel = new JScrollPane(mListItemChannel);
        mScrollPaneChannel.setBounds(20, 80, 300, 350);
        this.add(mScrollPaneChannel);

        // day
        mListModelDay = new DefaultListModel<>();
        mListItemDay = new JList<>();
        mListItemDay.setFont(f);
        mListItemDay.setModel(mListModelDay);
        mListItemDay.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //mListItemChannel.setCellRenderer(new MyRender());
        mListItemDay.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent event) {
                // TODO Auto-generated method stub
                int index = mListItemDay.getSelectedIndex();
                if (index == -1)
                    return;

                int button = event.getButton();
                if (button == 1) { // Left button
                    if (event.getClickCount() == 1) {
                        update_program(last_tid, index);
                    }
                }

            }
        });
        mListItemDay.setBounds(20, 450, 150, 100);
        this.add(mListItemDay);

        // prog
        mListModelProg = new DefaultListModel<>();
        mListItemProg = new JList<>();
        mListItemProg.setFont(f);
        mListItemProg.setModel(mListModelProg);
        mListItemProg.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mListItemProg.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent event) {
                // TODO Auto-generated method stub
                int index = mListItemProg.getSelectedIndex();
                if (index == -1)
                    return;

                int button = event.getButton();
                if (button == 1) { // Left button
                    if (event.getClickCount() == 2) {
                        Map<String, Object> channel = progList.get(index);
                        String url = (String) channel.get("url");

                        if (mCbHd.isSelected())
                            url = url.replace("&ct=2", "&ct=4");

                        play_url(url);
                    }
                }

            }
        });

        mScrollPaneProg = new JScrollPane(mListItemProg);
        mScrollPaneProg.setBounds(350, 80, 300, 350);
        this.add(mScrollPaneProg);

        mCbHd = new JCheckBox("高清");
        mCbHd.setBounds(200, 450, 80, 20);
        this.add(mCbHd);

        this.setLayout(null);

        init();
    }

    private void init() {
        channelList = new ArrayList<>();
        progList = new ArrayList<>();

        mListModelChannel.clear();

        token = BestvUtil2.getToken();
        if (token != null && token.length() > 0) {
            mLabelInfo.setText("Token获取成功");
        }
        else {
            mLabelInfo.setText("获取Token失败");
        }

        List<BestvUtil2.BestvChannel> list =  BestvUtil2.getChannel(cid);
        int size = list.size();
        for (int i=0;i<size;i++) {
            BestvUtil2.BestvChannel chn = list.get(i);
            //LogUtil.info(TAG, "chn: " + chn.title + " " + chn.nowPlay);

            HashMap channel_item = new HashMap();
            channel_item.put("title", chn.title);
            channel_item.put("tid", chn.tid);
            channel_item.put("now_play", chn.nowPlay);
            channel_item.put("will_play", chn.willPlay);
            channel_item.put("icon", chn.icon);
            channelList.add(channel_item);

            mListModelChannel.addElement(chn.title);
        }
    }

    private void update_program(int tid, int index) {
        progList.clear();

        mListModelProg.clear();

        if (index == -1)
            mListModelDay.clear();

        List<BestvUtil2.BestvProgramList> list = BestvUtil2.getPrograms(tid);
        int size = list.size();
        int show_index = size - 1;
        if (index != -1)
            show_index = index;
        for (int i=0;i<size;i++) {
            BestvUtil2.BestvProgramList prog_list = list.get(i);
            if (index == -1)
                mListModelDay.addElement(prog_list.day);

            if (i == show_index) {
                int sizeProg = prog_list.programs.size();
                for (int j=1;j<sizeProg;j++) {
                    BestvUtil2.BestvProgram prog = prog_list.programs.get(j);

                    HashMap prog_item = new HashMap();
                    prog_item.put("title", prog.title);
                    prog_item.put("time", prog.time);
                    prog_item.put("isNow", prog.isNow);
                    prog_item.put("url", prog.url);
                    progList.add(prog_item);

                    //LogUtil.info(TAG, "title: " + prog.title + " , url: " + prog.url);

                    String title = String.format(Locale.US, "%s %s", prog.time, prog.title);
                    mListModelProg.addElement(title);
                }
            }
        }

        if (index == -1)
            mListItemDay.setSelectedIndex(size - 1);
    }

    private class MyRender extends JLabel implements ListCellRenderer<String> {
        public MyRender() {
            this.setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends String> list, String value,
                int index, boolean isSelected, boolean cellHasFocus) {
            // TODO Auto-generated method stub
            this.setText(value);
            this.setFont(f);

            Color background = Color.WHITE;
            Color foreground = Color.BLACK;

            Map<String, Object> fileinfo = progList.get(index);
            boolean isdir = (Boolean) fileinfo.get("isdir");

            if (isdir) {
                foreground = Color.BLUE;
            }

            if (isSelected) {
                background = new Color(170, 170, 255);
            }

            setBackground(background);
            setForeground(foreground);

            return this;
        }
    }

    private void play_url(String url) {
        LogUtil.info(TAG, "ready to play url: " + url);
        mLabelInfo.setText("ready to play url: " + url);

        List<String> cmdList = new ArrayList<>();
        cmdList.add(player_path);
        cmdList.add("-i");
        cmdList.add(url);
        String []opts = play_opt.split(" ");
        if (opts.length > 0) {
            for (int i=0;i<opts.length;i++)
                cmdList.add(opts[i]);
        }
        String[] cmd = cmdList.toArray(new String[cmdList.size()]);
        //String[] cmd = new String[] {player_path, url};

        openExe(cmd);
    }

    private void openExe(String... params) {
        Runtime rn = Runtime.getRuntime();
        Process proc = null;
        try {
            proc = rn.exec(params);

            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "Error");
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "Output");
            errorGobbler.start();
            outputGobbler.start();
            //proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.error(TAG, "failed to run exec");
            mLabelInfo.setText("failed to run exec");
        }
    }

    private class StreamGobbler extends Thread {
        InputStream is;

        String type;

        StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (type.equals("Error")) {
                        System.out.println("[error] " + line);
                        mLabelInfo.setText("[error] " + line);
                    }
                    else {
                        System.out.println("[info] " + line);
                        mLabelInfo.setText("[info] " + line);
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
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
        panel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
