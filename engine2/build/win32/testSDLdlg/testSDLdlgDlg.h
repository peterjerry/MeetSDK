
// testSDLdlgDlg.h : 头文件
//

#pragma once
#include "afxwin.h"
#include "player.h"
#include "afxcmn.h"
#include "apThread.h"

class FFPlayer;
struct SDL_Surface;

// CtestSDLdlgDlg 对话框
class CtestSDLdlgDlg : public CDialogEx, MediaPlayerListener, apThread
{
// 构造
public:
	CtestSDLdlgDlg(CWnd* pParent = NULL);	// 标准构造函数

// 对话框数据
	enum { IDD = IDD_TESTSDLDLG_DIALOG };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV 支持


// 实现
protected:
	HICON m_hIcon;
	virtual void notify(int msg, int ext1, int ext2);
	BOOL PreTranslateMessage(MSG* pMsg);
	void Shuttle(int sec);
	virtual void thread_proc();

	// 生成的消息映射函数
	virtual BOOL OnInitDialog();
	afx_msg void OnSysCommand(UINT nID, LPARAM lParam);
	afx_msg void OnPaint();
	afx_msg HCURSOR OnQueryDragIcon();
	afx_msg LRESULT OnNotify(WPARAM wParam, LPARAM lParam);
	DECLARE_MESSAGE_MAP()
public:
	afx_msg void OnTimer(UINT_PTR nIDEvent);
	afx_msg void OnBnClickedStart();
	afx_msg void OnNMCustomdrawSliderClip(NMHDR *pNMHDR, LRESULT *pResult);
	afx_msg void OnEndTrack(NMHDR *pNMHDR, LRESULT *pResult);
public:
	CEdit mEditURL;
	CButton mCheckLooping;
	CProgressCtrl mProgClip;
	CComboBox mComboURL;
private:
	CString mUrl;
	FFPlayer *mPlayer;
	SDL_Surface *mSurface2;
	int mhttpPort;
	int mrtspPort;
	bool mPaused;
	bool mFinished;
	bool mBuffering;
	bool mPlayLive;
	int mBufferingOffset;

	int32_t mWidth;
	int32_t mHeight;
	int32_t mDuration;
	int mUserAddChnNum;
	int mUsedAudioChannel;

	int mDecFPS;
	int mRenderFPS;
	int mDecAvgMsec;
	int mRenderAvgMsec;
	int mDropFrames;
	int mRenderFrames;
	int mLatency;
	int mIOBitrate;
	int mBitrate;
private:
	bool startP2P();
	int64_t getSec();
	void drawBuffering();
	bool OnPrepared();
	void Cleanup();
public:
	afx_msg void OnLButtonUp(UINT nFlags, CPoint point);
	afx_msg void OnKeyUp(UINT nChar, UINT nRepCnt, UINT nFlags);
	afx_msg void OnDropFiles(HDROP hDropInfo);
	afx_msg void OnChar(UINT nChar, UINT nRepCnt, UINT nFlags);
	afx_msg void OnBnClickedButtonGetsec();
	afx_msg void OnDestroy();
};
