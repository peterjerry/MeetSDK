
// testSDLdlgDlg.cpp : 实现文件
//

#include "stdafx.h"
#include <math.h>
#include "testSDLdlg.h"
#include "testSDLdlgDlg.h"
#include "afxdialogex.h"

#include "ffplayer.h"
#define LOG_TAG "libplayer"
#include "log.h"
#include "apFileLog.h"
#include "surface.h"
#include "sdl.h"
#include "IPpbox.h"
#include "IDemuxer.h"
#include "approcessbmp.h" // for snapshot
#include "apEPG.h"
#include "urlcodec.h"
//#include <vld.h>

#pragma comment(lib, "sdl")
#pragma comment(lib, "libppbox")

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

#define WM_PREPARED_MESSAGE (WM_USER+100)
#define WM_NOTIFY_MESSAGE (WM_USER+101)

#define PROG_MAX_NUM	64
#define NORMAL_URL_OFFSET 0
#define PPTV_RTSP_URL_OFFSET 9
#define PPTV_HLS_URL_OFFSET (PPTV_RTSP_URL_OFFSET + 12)
#define USER_LIST_OFFSET (PPTV_HLS_URL_OFFSET + 12)

#define HOST "127.0.0.1"
#define HTTP_PORT 9008

const char* url_desc[PROG_MAX_NUM] = {
	_T("变形金刚2 720p"),
	_T("因为爱情 hls"),
	_T("因为爱情 hls noend"),
	_T("圣斗士星矢Ω 480p"),
	_T("NA_Secret 1080p"),

	_T("浙江卫视 高清"),
	_T("东方卫视 高清"),
	_T("东方卫视 标清"),
	_T("安徽卫视"),

	_T("rtsp 安徽卫视"),
	_T("rtsp 江苏卫视"),
	_T("rtsp 第一财经"),
	_T("rtsp 新娱乐"),
	_T("rtsp 星尚"),
	_T("rtsp 艺术人文"),
	_T("rtsp 上视纪实"),
	_T("rtsp 电视剧"),
	_T("rtsp ICS"),
	_T("rtsp 东方电影"),
	_T("rtsp 新闻综合"),
	_T("rtsp 东方购物"),

	_T("hls 安徽卫视"),
	_T("hls 江苏卫视"),
	_T("hls 第一财经"),
	_T("hls 新娱乐"),
	_T("hls 星尚"),
	_T("hls 艺术人文"),
	_T("hls 上视纪实"),
	_T("hls 电视剧"),
	_T("hls ICS"),
	_T("hls 东方电影"),
	_T("hls 新闻综合"),
	_T("hls 东方购物"),
};

const char* url_list[PROG_MAX_NUM] = {
	_T("E:\\Work\\HEVC\\Transformers3-720p.mp4"),
	_T("http://172.16.204.106/test/hls/600000/index.m3u8"),
	_T("http://172.16.204.106/test/hls/600000/noend.m3u8"),
	_T("D:\\Archive\\media\\[圣斗士星矢Ω].[hysub]Saint.Seiya.Omega_11_[GB_mp4][480p].mp4"),
	//_T("D:\\Archive\\media\\mv\\G.NA_Secret.mp4"),
	_T("D:\\Archive\\media\\test\\liuyan\\PPBOX-3417_wmv播放卡顿_ASF_WMV3_MP@ML_640×480@29.97_723K_WMA2_48K_Stereo_06.wmv"),

	_T("http://zb.v.qq.com:1863/?progid=1975434150"),
	_T("http://zb.v.qq.com:1863/?progid=3900155972"),
	_T("http://zb.v.qq.com:1863/?progid=3661744838"),
	_T("http://zb.v.qq.com:1863/?progid=623043810"),
};

int pptv_channel_id[] = {
	300162,// 安徽卫视
	300163,// 江苏卫视
	300154,// 第一财经"),
	300151,// 新娱乐"),
	300155,// 星尚"),
	300454,// 艺术人文"),
	300152,// 上视纪实"),
	300153,// 电视剧"),
	300214,// ICS"),
	300149,// 东方电影"),
	300156,// 新闻综合"),
	300254,// 东方购物"), /*&m3u8seekback=true*/
};

const char *pptv_rtsp_playlink_fmt = "rtsp://%s:%d/play.es?type=pplive3&playlink=%d";
const char *pptv_http_playlink_fmt = "http://%s:%d/play.m3u8?type=pplive3&playlink=%d";
const char *pptv_playlink_surfix = "%3Fft%3D1%26bwtype%3D0%26platform%3Dandroid3%26type%3Dphone.android.vip";
const char *pptv_playlink_ppvod2_fmt = "http://%s:%d/record.m3u8?type=ppvod2&playlink=%s&mux.M3U8.segment_duration=5";

void genHMSM(int pos_msec, int *hour, int *minute, int *sec, int *msec);

// 用于应用程序“关于”菜单项的 CAboutDlg 对话框

class CAboutDlg : public CDialogEx
{
public:
	CAboutDlg();

// 对话框数据
	enum { IDD = IDD_ABOUTBOX };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV 支持

// 实现
protected:
	DECLARE_MESSAGE_MAP()
};

CAboutDlg::CAboutDlg() : CDialogEx(CAboutDlg::IDD)
{
}

void CAboutDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialogEx::DoDataExchange(pDX);
}

BEGIN_MESSAGE_MAP(CAboutDlg, CDialogEx)
END_MESSAGE_MAP()


// CtestSDLdlgDlg 对话框

CtestSDLdlgDlg::CtestSDLdlgDlg(CWnd* pParent /*=NULL*/)
	: CDialogEx(CtestSDLdlgDlg::IDD, pParent), 
	mPlayer(NULL), mSurface2(NULL), mrtspPort(0), mhttpPort(0), 
	mFinished(false), mPaused(false), mPlayLive(false), mBuffering(false),
	mBufferingOffset(0),
	mWidth(0), mHeight(0), mDuration(0), mUsedAudioChannel(1),
	mDecFPS(0), mRenderFPS(0), mDecAvgMsec(0), mRenderAvgMsec(0), 
	mDropFrames(0), mRenderFrames(0), mLatency(0), mIOBitrate(0), mBitrate(0),
	mBufferingTimeMsec(0), mBufferPercentage(0), 
	mUserAddChnNum(0), 
	mEPGQueryType(EPG_QUERY_CATALOG), mEPGValue(-1)
{
	m_hIcon = AfxGetApp()->LoadIcon(IDR_MAINFRAME);
}

void CtestSDLdlgDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialogEx::DoDataExchange(pDX);
	DDX_Control(pDX, IDC_CHECK_LOOP, mCheckLooping);
	DDX_Control(pDX, IDC_PROGRESS_CLIP, mProgClip);
	DDX_Control(pDX, IDC_COMBO_URL, mComboURL);
	DDX_Control(pDX, IDC_COMBO_CATALOG, mComboEPGItem);
}

BEGIN_MESSAGE_MAP(CtestSDLdlgDlg, CDialogEx)
	ON_WM_SYSCOMMAND()
	ON_WM_PAINT()
	ON_WM_QUERYDRAGICON()
	ON_WM_TIMER()
	ON_BN_CLICKED(IDC_BUTTON1, &CtestSDLdlgDlg::OnBnClickedStart)
	ON_WM_LBUTTONUP()
	ON_WM_KEYUP()
	ON_WM_DROPFILES()
	ON_WM_CHAR()
	ON_MESSAGE(WM_NOTIFY_MESSAGE, OnNotify)
	ON_BN_CLICKED(IDC_BUTTON_GETSEC, &CtestSDLdlgDlg::OnBnClickedButtonGetsec)
	ON_WM_DESTROY()
	ON_BN_CLICKED(IDC_BUTTON_RESET_EPG, &CtestSDLdlgDlg::OnBnClickedButtonResetEpg)
	ON_CBN_SELCHANGE(IDC_COMBO_CATALOG, &CtestSDLdlgDlg::OnCbnSelchangeComboCatalog)
	ON_BN_CLICKED(IDC_BUTTON_PLAY_EPG, &CtestSDLdlgDlg::OnBnClickedButtonPlayEpg)
END_MESSAGE_MAP()


// CtestSDLdlgDlg 消息处理程序

BOOL CtestSDLdlgDlg::OnInitDialog()
{
	CDialogEx::OnInitDialog();

	// 将“关于...”菜单项添加到系统菜单中。

	// IDM_ABOUTBOX 必须在系统命令范围内。
	ASSERT((IDM_ABOUTBOX & 0xFFF0) == IDM_ABOUTBOX);
	ASSERT(IDM_ABOUTBOX < 0xF000);

	CMenu* pSysMenu = GetSystemMenu(FALSE);
	if (pSysMenu != NULL)
	{
		BOOL bNameValid;
		CString strAboutMenu;
		bNameValid = strAboutMenu.LoadString(IDS_ABOUTBOX);
		ASSERT(bNameValid);
		if (!strAboutMenu.IsEmpty())
		{
			pSysMenu->AppendMenu(MF_SEPARATOR);
			pSysMenu->AppendMenu(MF_STRING, IDM_ABOUTBOX, strAboutMenu);
		}
	}

	// 设置此对话框的图标。当应用程序主窗口不是对话框时，框架将自动
	//  执行此操作
	SetIcon(m_hIcon, TRUE);			// 设置大图标
	SetIcon(m_hIcon, FALSE);		// 设置小图标

	// TODO: 在此添加额外的初始化代码
#ifdef SAVE_LOG_FILE
	apLog::init("c:\\log\\libplayer.log");
#endif

	if (!startP2P())
		return FALSE;

	for (int i=0;i<sizeof(pptv_channel_id) / sizeof(int);i++) {
		char *new_item = (char *)malloc(256);
		_snprintf(new_item, 256, pptv_rtsp_playlink_fmt, HOST, mrtspPort, pptv_channel_id[i]);
		strcat(new_item, pptv_playlink_surfix);
		url_list[PPTV_RTSP_URL_OFFSET + i] = new_item;
	}

	for (int i=0;i<sizeof(pptv_channel_id) / sizeof(int);i++) {
		char *new_item = (char *)malloc(256);
		_snprintf(new_item, 256, pptv_http_playlink_fmt, HOST, mhttpPort, pptv_channel_id[i]);
		strcat(new_item, pptv_playlink_surfix);
		url_list[PPTV_HLS_URL_OFFSET + i] = new_item;
	}

	FILE *pFile = NULL;
	pFile = fopen("tvlist.txt", "r");
	if(pFile) {
		char *data = NULL;
		fseek(pFile, 0, SEEK_END);
		int filesize = ftell(pFile);
		fseek(pFile, 0, SEEK_SET);
		data = new char[filesize + 1];
		memset(data, 0, filesize + 1);
		fread(data, 1, filesize, pFile);
		char *p = NULL;
		p = strtok(data, "\n");
		int i=0;
		while (p) {
			char *new_ptr = new char[strlen(p) + 1];
			strcpy(new_ptr, p);
			if (i%2 == 0)
				url_desc[USER_LIST_OFFSET + i/2] = new_ptr;
			else
				url_list[USER_LIST_OFFSET + i/2] = new_ptr;
			p = strtok(NULL, "\n");
			i++;
		}
		mUserAddChnNum = i / 2;

		fclose(pFile);
		delete data;
	}

	for (int i=0;i<PROG_MAX_NUM;i++) {
		if (url_desc[i] == NULL)
			break;

		mComboURL.AddString(url_desc[i]);
	}

	mComboURL.SetCurSel(3/*PPTV_HLS_URL_OFFSET + 3*/);

	//mCheckLooping.SetCheck(TRUE);

	DragAcceptFiles(TRUE);

	CTime timeNow = CTime::GetCurrentTime();
	CString timeFmt = timeNow.Format("%Y-%m-%d");
	timeFmt += " 00:00:00";
	SetDlgItemText(IDC_EDIT_TIMECODE, timeFmt);
	SetDlgItemText(IDC_EDIT_VOD_DURATION, "90");
	//SetDlgItemText(IDC_EDIT_VLC_PATH, "C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe");

	SetDlgItemText(IDC_EDIT_PLAYLINK, "19534153");
	SetDlgItemText(IDC_EDIT_FT, "1");
	SetDlgItemText(IDC_EDIT_BWTYPE, "3");
	mEPGQueryType = EPG_QUERY_FRONTPAGE;
	mEPGValue = -1;
	start();

	return TRUE;  // 除非将焦点设置到控件，否则返回 TRUE
}

void CtestSDLdlgDlg::OnSysCommand(UINT nID, LPARAM lParam)
{
	if ((nID & 0xFFF0) == IDM_ABOUTBOX)
	{
		CAboutDlg dlgAbout;
		dlgAbout.DoModal();
	}
	else
	{
		CDialogEx::OnSysCommand(nID, lParam);
	}
}

// 如果向对话框添加最小化按钮，则需要下面的代码
//  来绘制该图标。对于使用文档/视图模型的 MFC 应用程序，
//  这将由框架自动完成。

void CtestSDLdlgDlg::OnPaint()
{
	if (IsIconic())
	{
		CPaintDC dc(this); // 用于绘制的设备上下文

		SendMessage(WM_ICONERASEBKGND, reinterpret_cast<WPARAM>(dc.GetSafeHdc()), 0);

		// 使图标在工作区矩形中居中
		int cxIcon = GetSystemMetrics(SM_CXICON);
		int cyIcon = GetSystemMetrics(SM_CYICON);
		CRect rect;
		GetClientRect(&rect);
		int x = (rect.Width() - cxIcon + 1) / 2;
		int y = (rect.Height() - cyIcon + 1) / 2;

		// 绘制图标
		dc.DrawIcon(x, y, m_hIcon);
	}
	else
	{
		CDialogEx::OnPaint();
	}
}

//当用户拖动最小化窗口时系统调用此函数取得光标
//显示。
HCURSOR CtestSDLdlgDlg::OnQueryDragIcon()
{
	return static_cast<HCURSOR>(m_hIcon);
}

void CtestSDLdlgDlg::OnTimer(UINT_PTR nIDEvent)
{
	// TODO: 在此添加消息处理程序代码和/或调用默认值

	status_t stat;
	int32_t curr_pos;

	stat = mPlayer->getCurrentPosition(&curr_pos);
	if (stat == OK) {
		CString title;
		CString timeinfo;
		CString filename;

		filename = mUrl;
		if (filename.GetLength() > 32) {
			filename.Truncate(32);
			filename += "...";
		}

		int32_t duration;
		mPlayer->getDuration(&duration);
		int cache_msec = duration * mBufferPercentage / 100 - curr_pos;
		if (cache_msec < 0)
			cache_msec = 0;

		title.Format("%s, v-a %03d, drop %d, render %d, %02d(%03d)/%02d(%03d), %d/%d kbps| buf %d msec |pct %d%%(cache %d msec)", 
			filename.GetBuffer(), 
			mLatency, mDropFrames, mRenderFrames, 
			mDecFPS, mDecAvgMsec, mRenderFPS, mRenderAvgMsec, mIOBitrate, mBitrate, 
			mBufferingTimeMsec, mBufferPercentage, cache_msec);
		SetWindowText(title);
		if (mPlayLive) {
			int32_t new_duration;
			mPlayer->getDuration(&new_duration);
			int32_t offset = new_duration - 1800 * 1000;
			int32_t correct_pos = curr_pos - offset;
			mProgClip.SetPos(correct_pos / 1000);
		}
		else {
			mProgClip.SetPos(curr_pos / 1000);
		}

		// pos and duration
		int hour, minute, sec, msec;
		genHMSM(curr_pos, &hour, &minute, &sec, &msec);

		int du_hour, du_minute, du_sec, du_msec;
		genHMSM(duration, &du_hour, &du_minute, &du_sec, &du_msec);
		
		timeinfo.Format("%02d:%02d:%02d:%03d/%02d:%02d:%02d:%03d", 
			hour, minute, sec, msec,
			du_hour, du_minute, du_sec, du_msec);
		SetDlgItemText(IDC_STATIC_TIMEINFO, timeinfo);

		if (mPlayLive) {
			CTime tm_end = CTime::GetCurrentTime();
			CTime tm_start = tm_end - CTimeSpan( 0, 0, 30, 0 ); // 30 min before
			CString strStartTime, strEndTime;
			strStartTime = tm_start.Format("%H:%M:%S");
			SetDlgItemText(IDC_STATIC_START_TIME, strStartTime);
		}
	}

	drawBuffering();

	CDialogEx::OnTimer(nIDEvent);
}

void CtestSDLdlgDlg::drawBuffering()
{
	// buffering animation
	if (mBuffering) {
		// draw picture

		CWnd* pWnd = this->GetDlgItem(IDC_STATIC_RENDER);
		CDC *pDC = pWnd->GetDC();

		CPen newpen;
		newpen.CreatePen(PS_SOLID, 25, RGB(255,0,0));
		CPen* pOldPen = pDC->SelectObject(&newpen);

		CFont newfont;
		newfont.CreateFont(64,            // nHeight
			0,           // nWidth
			0,           // nEscapement
			0,           // nOrientation
			FW_BOLD,     // nWeight
			FALSE,        // bItalic
			FALSE,       // bUnderline
			0,           // cStrikeOut
			ANSI_CHARSET,              // nCharSet
			OUT_DEFAULT_PRECIS,        // nOutPrecision
			CLIP_DEFAULT_PRECIS,       // nClipPrecision
			DEFAULT_QUALITY,           // nQuality
			DEFAULT_PITCH | FF_SWISS, // nPitchAndFamily
			_T("微软雅黑"));              // lpszFac
		CFont* pOldFont = pDC->SelectObject(&newfont);
		pDC->SetTextColor(RGB(255,0,0));
		pDC->SetBkMode(TRANSPARENT);

		//pDC->MoveTo(50,300);
		//pDC->LineTo(500,300);
		//pDC->DrawText("Buffering...", CRect(50, 200, 400, 300), DT_CENTER);

		int x,y,r;
		int x1,y1,x2,y2;

		x	= mWidth / 2;
		y	= mHeight / 2;
		r	= mHeight / 4;
		x1	= x - r;
		x2	= x + r;
		y1	= y - r;
		y2	= y + r;
		
		CPoint p1(x1 + mBufferingOffset, y2);
		CPoint p2(x1, y1);
		CRect rt(x-r,y-r,x+r,y+r);
		pDC->Arc(rt, p2, p1); 
		mBufferingOffset += 10;

		pDC->SelectObject(&pOldPen);
		pDC->SelectObject(&pOldFont);
		pWnd->ReleaseDC(pDC);
	}
}

void CtestSDLdlgDlg::OnBnClickedStart()
{
	int sel = mComboURL.GetCurSel();
	if (sel < 0) {
		AfxMessageBox("not select item");
		return;
	}

	if (sel < USER_LIST_OFFSET + mUserAddChnNum) {
		mUrl = url_list[sel];
	}
	else {
		mComboURL.GetLBText(sel, mUrl);
	}
	
	mPlayLive = false;
	SetDlgItemText(IDC_STATIC_START_TIME, "N/A");

	if(mUrl.Find(HOST) != -1 && sel < USER_LIST_OFFSET) {
		int64_t start_time = getSec();
		int duration_min = GetDlgItemInt(IDC_EDIT_VOD_DURATION);
		CString strTime;

		if (duration_min > 0) {
			// fake vod
			strTime.Format("%I64d", start_time);
			mUrl += "%26begin_time%3D";
			mUrl += strTime;
			mUrl += "%26end_time%3D";
			strTime.Format("%I64d", start_time + 60 * duration_min);
			mUrl += strTime;
		}
		else {
			// live
			mUrl += "&m3u8seekback=true";
			mPlayLive = true;
		}
	}

	CString vlcPath;
	GetDlgItemText(IDC_EDIT_VLC_PATH, vlcPath);
	if (vlcPath.IsEmpty()) {
		stop_player();
		start_player(mUrl.GetBuffer(0));
	}
	else {
		CString strArgu;
		strArgu.Format("\"%s\" \"%s\"", vlcPath, mUrl.GetBuffer(0));
		LOGI("vlc arg: %s", strArgu.GetBuffer(0));

		PROCESS_INFORMATION pi;
		memset(&pi,0,sizeof(pi));

		STARTUPINFO si;
		memset(&si,0,sizeof(si));
		si.cb			= sizeof(si);
		//si.wShowWindow	= SW_SHOW;
		//si.dwFlags		= STARTF_USESHOWWINDOW;

		BOOL fRet = CreateProcess(
			vlcPath.GetBuffer(0),
			strArgu.GetBuffer(0),
			NULL,
			FALSE,
			NULL,
			NULL,
			NULL,
			NULL,
			&si,
			&pi
			);
		if (!fRet) {
			MessageBox("failed to launch vlc");
		}
	}
}

bool CtestSDLdlgDlg::start_player(const char *url)
{
	status_t status;

	mPlayer = new FFPlayer;
	mPlayer->setListener(this);
	mPlayer->setDataSource(url);
	status = mPlayer->prepareAsync();
	if (status != OK) {
		delete mPlayer;
		mPlayer = NULL;
		AfxMessageBox("failed to prepareAsync");
	}

	mBuffering = true;
	mBufferingOffset = 0;

	return true;
}

void CtestSDLdlgDlg::stop_player()
{
	if (mPlayer) {
		KillTimer(0);

		LOGI("before call player stop");
		mPlayer->stop();
		LOGI("after call player stop");
		delete mPlayer;
		mPlayer = NULL;

		if (mSurface2) {
			SDL_FreeSurface(mSurface2);
			mSurface2 = NULL;
		}
	}
}

void CtestSDLdlgDlg::notify(int msg, int ext1, int ext2)
{
	//printf("notify %d %d %d\n", msg, ext1, ext2);
	WPARAM wp;
	LPARAM lp;
	wp = (msg << 16) | ext1;
	lp = ext2;
	PostMessage(WM_NOTIFY_MESSAGE, wp, lp);
}

void CtestSDLdlgDlg::OnLButtonUp(UINT nFlags, CPoint point)
{
	// TODO: 在此添加消息处理程序代码和/或调用默认值

	if (mPlayer) {
		int64_t new_pos;
		RECT rect;
		int width;

		GetClientRect(&rect);
		width = rect.right - rect.left;
		if (mPlayLive) {
			int32_t new_duration;
			mPlayer->getDuration(&new_duration);
			new_pos = new_duration - 1800 * 1000;
			new_pos += point.x * (int64_t)(1800 * 1000) / width;
		}
		else {
			new_pos = point.x * (int64_t)mDuration / width;
		}
		mPlayer->seekTo((int32_t)new_pos);
		mProgClip.SetPos((int32_t)(new_pos / 1000));
	}

	__super::OnLButtonUp(nFlags, point);
}

void CtestSDLdlgDlg::OnKeyUp(UINT nChar, UINT nRepCnt, UINT nFlags)
{
	// TODO: 在此添加消息处理程序代码和/或调用默认值

	__super::OnKeyUp(nChar, nRepCnt, nFlags);
}

BOOL CtestSDLdlgDlg::PreTranslateMessage(MSG* pMsg)
{
	if (pMsg->message==WM_KEYUP)
	{
		switch (pMsg->wParam)
		{
		case VK_F1:
			if (::GetKeyState(VK_CONTROL)<0)
			{
				AfxMessageBox("F1");
			}
			break;
		case VK_F2:
			if (::GetKeyState(VK_CONTROL)<0)
			{
				AfxMessageBox("F2");
			}
			break;
		default:
			break;
		}
	}

	if (pMsg->message == WM_CHAR)
	{
		if (mPlayer) {
			status_t stat;

			switch (pMsg->wParam)
			{
			case 's': // toggle pause
				if (mPaused)
					mPlayer->start();
				else
					mPlayer->pause();

				mPaused = !mPaused;
				break;
			case 'a': // backward 10sec
				Shuttle(-10);
				break;
			case 'd': // forward 10sec
				Shuttle(10);
				break;
			case 'x': // take a snapshot
				{
					int32_t width, height;
					mPlayer->getVideoWidth(&width);
					mPlayer->getVideoHeight(&height);
					SnapShot *ss = mPlayer->getSnapShot(0, 0, 0, -1);
					if (ss) {
						CTime tm = CTime::GetCurrentTime();
						CString filename;
						filename.Format("c:\\log\\%s%I64d.bmp", tm.Format("%Y-%m-%d_").GetBuffer(0), tm.GetTime());
						LOGI("save bmp file: %s in %s", filename.GetBuffer(0), tm.Format("%Y-%m-%d_%H:%M:%S").GetBuffer(0));
						apProcessBMP::WriteBMP(filename.GetBuffer(0), width, height, 32, (const char *)ss->picture_data, ss->stride * ss->height * 4);
					}
				}
				break;
			case 'g':
				if (mPlayer) {
					stat = mPlayer->selectAudioChannel(++mUsedAudioChannel);
					if (stat != OK) {
						mUsedAudioChannel = 1;
						stat = mPlayer->selectAudioChannel(mUsedAudioChannel);
						if (OK != stat)
							AfxMessageBox("failed to set audio track");
					}
				}
				break;
			default:
				break;
			}
		}
	}

	return CDialog::PreTranslateMessage(pMsg);
}

void CtestSDLdlgDlg::Shuttle(int sec)
{
	int32_t pos;
	status_t stat;

	stat = mPlayer->getCurrentPosition(&pos);
	if (stat == OK) {
		pos += (sec * 1000);
		if (pos >= 0 && pos <= mDuration) {
			mPlayer->seekTo((int32_t)pos);
			mProgClip.SetPos((int32_t)(pos / 1000));
		}
	}
}

void CtestSDLdlgDlg::OnDropFiles(HDROP hDropInfo)
{
	// TODO: 在此添加消息处理程序代码和/或调用默认值

	// 定义一个缓冲区来存放读取的文件名信息
	char szFileName[MAX_PATH + 1] = {0};
	// 通过设置iFiles参数为0xFFFFFFFF,可以取得当前拖动的文件数量，
	// 当设置为0xFFFFFFFF,函数间忽略后面连个参数。
	UINT nFiles = DragQueryFile(hDropInfo, 0xFFFFFFFF, NULL, 0);
	// 通过循环依次取得拖动文件的File Name信息，并把它添加到ListBox中
	for(UINT i=0; i<nFiles; i++)
	{
		DragQueryFile(hDropInfo, i, szFileName, MAX_PATH);
		//mComboURL.SetWindowText(szFileName);
		mComboURL.AddString(szFileName);
		mComboURL.SetCurSel(mComboURL.GetCount() - 1);
	}
	// 结束此次拖拽操作，并释放分配的资源
	DragFinish(hDropInfo);

	__super::OnDropFiles(hDropInfo);
}


void CtestSDLdlgDlg::OnChar(UINT nChar, UINT nRepCnt, UINT nFlags)
{
	// TODO: 在此添加消息处理程序代码和/或调用默认值

	__super::OnChar(nChar, nRepCnt, nFlags);
}

LRESULT CtestSDLdlgDlg::OnNotify(WPARAM wParam, LPARAM lParam)
{
	int msg;
	int ext1, ext2;

	msg = (wParam >> 16);
	ext1 = (wParam & 0xffff);
	ext2 = lParam;

	if (MEDIA_PREPARED == msg) {
		OnPrepared();
	}
	else if (MEDIA_BUFFERING_UPDATE == msg) {
		mBufferPercentage = ext1;
	}
	else if (MEDIA_PLAYBACK_COMPLETE == msg) {
		LOGI("MEDIA_PLAYBACK_COMPLETE");
		mFinished = true;
		stop_player();
		
		AfxMessageBox("player complete");
	}
	else if (MEDIA_ERROR == msg) {
		LOGE("MEDIA_ERROR %d", ext1);
		AfxMessageBox("player error");
		
		mFinished = true;
		if (mPlayer) {
			mPlayer->stop();
			delete mPlayer;
			mPlayer = NULL;
		}
	}
	else if (MEDIA_INFO == msg) {
		//LOGI("MEDIA_INFO ext1: %d, ext2: %d", ext1, ext2);

		if (MEDIA_INFO_BUFFERING_START == ext1) {
			LOGI("MEDIA_INFO_BUFFERING_START");
			mBuffering = true;
			mBufferingOffset = 0;
		}
		else if (MEDIA_INFO_BUFFERING_END == ext1) {
			LOGI("MEDIA_INFO_BUFFERING_END");
			// hide picture
			Invalidate();
			mBuffering = false;
		}

		if(MEDIA_INFO_TEST_DECODE_AVG_MSEC == ext1) {
			mDecAvgMsec = ext2;
		}
		else if(MEDIA_INFO_TEST_RENDER_AVG_MSEC == ext1) {
			mRenderAvgMsec = ext2;
		}
		else if(MEDIA_INFO_TEST_DECODE_FPS == ext1) {
			mDecFPS = ext2;
		}
		else if(MEDIA_INFO_TEST_RENDER_FPS == ext1) {
			mRenderFPS = ext2;
		}
		else if(MEDIA_INFO_TEST_RENDER_FRAME == ext1) {
			mRenderFrames = ext2;
		}
		else if(MEDIA_INFO_TEST_LATENCY_MSEC == ext1) {
			mLatency = ext2;
		}
		else if(MEDIA_INFO_TEST_DROP_FRAME == ext1) {
			mDropFrames++;
		}
		else if(MEDIA_INFO_TEST_BUFFERING_MSEC == ext1) {
			mBufferingTimeMsec = ext2;
		}
		else if(MEDIA_INFO_TEST_IO_BITRATE == ext1) {
			mIOBitrate = ext2;
		}
		else if(MEDIA_INFO_TEST_MEDIA_BITRATE == ext1) {
			mBitrate = ext2;
		}
	}
	else if (MEDIA_SEEK_COMPLETE == msg) {
		LOGI("MEDIA_SEEK_COMPLETE");
	}

	return 0;
}

bool CtestSDLdlgDlg::OnPrepared()
{
	mBuffering = false;

	if (mCheckLooping.GetCheck())
		mPlayer->setLooping(1);

	mPlayer->getDuration(&mDuration);
	mPlayer->getVideoWidth(&mWidth);
	mPlayer->getVideoHeight(&mHeight);
	LOGI("duration %d sec, %dx%d", mDuration / 1000, mWidth, mHeight);

	mProgClip.SetRange(0, mDuration / 1000);
	mProgClip.SetPos(0);

	char variable[256];
	CWnd* pWnd = this->GetDlgItem(IDC_STATIC_RENDER);
	sprintf_s(variable,"SDL_WINDOWID=0x%1x", pWnd->GetSafeHwnd());     
	SDL_putenv(variable);

	if( SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_TIMER)) {
		TCHAR msg[256] = {0};
		wsprintf(msg, _T("Could not initialize SDL - %s\n"), SDL_GetError());
		AfxMessageBox(msg);
		LOGE(msg);
		return false;
	}

	SDL_Rect rect;
	rect.x = 0;
	rect.y = 0;
	rect.w = mWidth;
	rect.h = mHeight;

	RECT rc;
	GetWindowRect(&rc);
	if (mWidth + 50 > 900)
		rc.right = rc.left + mWidth + 50;
	rc.bottom = rc.top + mHeight + 200;
	
	// adjust position
	int scr_width = GetSystemMetrics ( SM_CXSCREEN ); 
	int scr_height= GetSystemMetrics ( SM_CYSCREEN ); 
	if(rc.bottom > scr_height) {
		int tmp = rc.top - 50;
		rc.top = 50;
		rc.bottom -= tmp;
	}
	int new_w, new_h;
	new_w = rc.right - rc.left;
	new_h = rc.bottom - rc.top;
	MoveWindow(rc.left, rc.top, new_w, new_h);

	mSurface2 = SDL_SetVideoMode(rect.w, rect.h, 32, 
		SDL_HWSURFACE | SDL_DOUBLEBUF/* | SDL_RESIZABLE*/);
	if (!mSurface2) {
		AfxMessageBox("failed to create surface");
		return false;
	}

	// way2
	//SDL_Window * pWindow = SDL_CreateWindowFrom( (void *)( GetDlgItem(IDC_STATIC1)->GetSafeHwnd() ) );
	Surface_open2((void *)mSurface2);

	mPlayer->start();
	mDropFrames = 0;
	mPaused = false;
	SetTimer(0, 100, NULL);

	Invalidate();
	return true;
}

void genHMSM(int pos_msec, int *hour, int *minute, int *sec, int *msec)
{
	int tmp = pos_msec;

	*msec	= tmp % 1000;
	tmp		= tmp / 1000; //around to sec
	*hour	= tmp / 3600;
	tmp		= tmp - 3600 * (*hour);
	*minute	= tmp / 60;
	*sec = tmp % 60;
}

void CtestSDLdlgDlg::thread_proc()
{
	CString strPrefix;
	int sel;

	EPG_MODULE_LIST*	modulelist	= NULL;
	EPG_CATALOG_LIST*	cataloglist = NULL;
	EPG_PLAYLINK_LIST*	playlinklist = NULL;

	switch (mEPGQueryType) {
	case EPG_QUERY_FRONTPAGE:
		modulelist = mEPG.frontpage();
		if (!modulelist) {
			LOGE(_T("failed to connect to epg server")); // tchar.h compatible with ansi and unicode
			AfxMessageBox(_T("failed to connect to epg server"));
			return;
		}

		mComboEPGItem.ResetContent();
		for (int i=0;i<(int)modulelist->size();i++) {
			mComboEPGItem.AddString((*modulelist)[i].get_title());
		}
		
		mComboEPGItem.SetCurSel(0);
		break;
	case EPG_QUERY_CATALOG:
		cataloglist = mEPG.catalog(mEPGValue);
		if (!cataloglist) {
			LOGE(_T("failed to connect to epg server")); // tchar.h compatible with ansi and unicode
			AfxMessageBox(_T("failed to connect to epg server"));
			return;
		}

		mComboEPGItem.ResetContent();
		for (int i=0;i<(int)cataloglist->size();i++) {
			mComboEPGItem.AddString((*cataloglist)[i].get_title());
		}
		
		mComboEPGItem.SetCurSel(0);
		break;
	case EPG_QUERY_DATAIL:
		sel = mComboEPGItem.GetCurSel();
		mComboEPGItem.GetLBText(sel, strPrefix);
		playlinklist = mEPG.detail(mEPGValue);

		if (!playlinklist) {
			LOGE(_T("failed to connect to epg server")); // tchar.h compatible with ansi and unicode
			AfxMessageBox(_T("failed to connect to epg server"));
			return;
		}

		mComboEPGItem.ResetContent();
		for (int i=0;i<(int)playlinklist->size();i++) {
			mComboEPGItem.AddString((*playlinklist)[i].get_title());
		}
		
		mComboEPGItem.SetCurSel(0);
		
		if (playlinklist->size() == 1)
			SetDlgItemInt(IDC_EDIT_PLAYLINK, playlinklist->at(0).get_id());

		break;
	case EPG_QUERY_CDN_URL:
		break;
	default:
		break;
	}
}

void CtestSDLdlgDlg::OnBnClickedButtonGetsec()
{
	// TODO: 在此添加控件通知处理程序代码
	CString strMsg;
	int64_t nTSeconds = getSec();
	strMsg.Format(_T("time: %I64d"), nTSeconds);
	AfxMessageBox(strMsg);
}

int64_t CtestSDLdlgDlg::getSec()
{
	CString strTime;
	GetDlgItemText(IDC_EDIT_TIMECODE, strTime);

	// 201411241800
	const char* charFormat = _T("%d-%d-%d %d:%d:%d");
	int nYear;
	int nMonth;
	int nDate;
	int nHour;
	int nMin;
	int nSec; 
	_stscanf(strTime.GetBuffer(0), charFormat, &nYear, &nMonth, &nDate, &nHour, &nMin, &nSec);

	int timezone_diff = 60 * 60 * 8;
	CString strMsg;
	//CTime time1 = CTime::GetCurrentTime(); 
	//CTime time2(1970,1,2,0,0,0);
	CTime time2(nYear,nMonth,nDate,nHour,nMin,nSec);
	return time2.GetTime();
}

bool CtestSDLdlgDlg::startP2P()
{
	const char *gid = "13";
	const char *pid = "162";
	const char *auth = "08ae1acd062ea3ab65924e07717d5994";

	//PPBOX_SetConfig("", "HttpManager", "addr", "127.0.0.1:9106+");
	//PPBOX_SetConfig("", "RtspManager", "addr", "127.0.0.1:5156+");

	int32_t ec = PPBOX_StartP2PEngine(gid, pid, auth);
    if (ppbox_success != ec) {
		CString msg;
		msg.Format("start p2p engine: %s", PPBOX_GetLastErrorMsg());
        AfxMessageBox(msg);
        return false;
    }

	mrtspPort = PPBOX_GetPort("rtsp");
	mhttpPort = PPBOX_GetPort("http");

	LOGI("p2pEngine: rtsp port %d, http port %d", mrtspPort, mhttpPort);

	return true;
}

void CtestSDLdlgDlg::Cleanup()
{
	LOGI("Cleanup()");

	KillTimer(0);

	if (mPlayer) {
		LOGI("stop player");
		mPlayer->stop();
		delete mPlayer;
		mPlayer = NULL;
	}

	if (mSurface2) {
		LOGI("free sdl surface");
		SDL_FreeSurface(mSurface2);
		mSurface2 = NULL;
	}

	LOGI("SDL_Quit()");
	SDL_Quit();

	LOGI("PPBOX_StopP2PEngine()");
	PPBOX_StopP2PEngine();

	// PPTV_RTSP_URL_OFFSET PPTV_HLS_URL_OFFSET USER_LIST_OFFSET
	for (int i= USER_LIST_OFFSET; i<PROG_MAX_NUM;i++) {
		if (url_desc[i]) {
			delete url_desc[i];
			url_desc[i] = NULL;
		}
	}

	for (int i= PPTV_RTSP_URL_OFFSET; i<PROG_MAX_NUM;i++) {
		if (url_list[i]) {
			delete url_list[i];
			url_list[i] = NULL;
		}
	}
}

void CtestSDLdlgDlg::OnDestroy()
{
	__super::OnDestroy();

	// TODO: 在此处添加消息处理程序代码
	Cleanup();
}


void CtestSDLdlgDlg::OnBnClickedButtonResetEpg()
{
	// TODO: 在此添加控件通知处理程序代码
	mEPGQueryType = EPG_QUERY_FRONTPAGE;
	start();
}


void CtestSDLdlgDlg::OnCbnSelchangeComboCatalog()
{
	// TODO: 在此添加控件通知处理程序代码
	int sel = mComboEPGItem.GetCurSel();

	if (EPG_QUERY_FRONTPAGE == mEPGQueryType) {
		mEPGValue = mEPG.get_module()->at(sel).get_index();
		mEPGQueryType = EPG_QUERY_CATALOG;
	}
	else if(EPG_QUERY_CATALOG == mEPGQueryType) {
		mEPGValue = mEPG.get_catalog()->at(sel).get_vid();
		mEPGQueryType = EPG_QUERY_DATAIL;
	}
	else if (EPG_QUERY_DATAIL == mEPGQueryType) {
		if (mEPG.get_playlink()->size() < 2)
			return;

		mEPGValue = mEPG.get_playlink()->at(sel).get_id();
	}
	else {
		mEPGValue = -1;
		mEPGQueryType = EPG_QUERY_CATALOG;
	}

	start();
}


void CtestSDLdlgDlg::OnBnClickedButtonPlayEpg()
{
	// TODO: 在此添加控件通知处理程序代码
	stop_player();
	char str_url[1024] = {0};
	char str_playlink[512] = {0};

	int link, ft, bw_type;
	link = GetDlgItemInt(IDC_EDIT_PLAYLINK);
	ft = GetDlgItemInt(IDC_EDIT_FT);
	bw_type = GetDlgItemInt(IDC_EDIT_BWTYPE);

	_snprintf(str_playlink, 512, "%d?ft=%d&bwtype=%d&platform=android3&type=phone.android.vip&sv=4.1.3&param=userType%3D1", 
		link, ft, bw_type);
	LOGI("playlink before urlencode: %s", str_playlink);
	int out_len = 0;
	char *encoded_playlink = urlencode(str_playlink, strlen(str_playlink), &out_len);
	LOGI("playlink after urlencode: %s", encoded_playlink);

	_snprintf(str_url, 1024, pptv_playlink_ppvod2_fmt, HOST, mhttpPort, encoded_playlink);
	LOGI("final vod url: %s", str_url);
	start_player(str_url);
}
