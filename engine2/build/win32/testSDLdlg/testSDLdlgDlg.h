
// testSDLdlgDlg.h : ͷ�ļ�
//

#pragma once
#include "afxwin.h"
#include "afxcmn.h"
#include "apThread.h"
#include "apEPG.h"

class FFPlayer;
struct SDL_Window;
struct SDL_Renderer;
struct SDL_Texture;
struct SDL_Surface;

enum EPG_QUERY_TYPE {
	EPG_QUERY_FRONTPAGE,
	EPG_QUERY_CATALOG,
	EPG_QUERY_DATAIL,
	EPG_QUERY_CLIP,
	EPG_QUERY_CDN_URL,
};

// CtestSDLdlgDlg �Ի���
class CtestSDLdlgDlg : public CDialogEx, MediaPlayerListener, apThread
{
// ����
public:
	CtestSDLdlgDlg(CWnd* pParent = NULL);	// ��׼���캯��

// �Ի�������
	enum { IDD = IDD_TESTSDLDLG_DIALOG };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV ֧��


// ʵ��
protected:
	HICON m_hIcon;
	virtual void notify(int msg, int ext1, int ext2);
	BOOL PreTranslateMessage(MSG* pMsg);
	void Seek(int msec);
	virtual void thread_proc();

	// ���ɵ���Ϣӳ�亯��
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
	CComboBox mComboURL;
	CComboBox mComboEPGItem;
	CComboBox mCBbwType;
	CComboBox mCBft;
	CSliderCtrl mProgress;
private:
	CString mUrl;

	// epg
	enum EPG_QUERY_TYPE mEPGQueryType;
	apEPG mEPG;
	int mEPGValue;

	IPlayer *mPlayer;
	int mhttpPort;
	int mrtspPort;
	bool mPaused;
	bool mFinished;
	bool mBuffering;
	bool mPlayLive;
	bool mSeeking;
	int mBufferingOffset;

	int32_t mWidth;
	int32_t mHeight;
	int32_t mDuration;
	int mUserAddChnNum;
	int mUsedAudioChannel;

	int mBufferPercentage;

	int mLiveBackSeekSec;

	int mDecFPS;
	int mRenderFPS;
	int mDecAvgMsec;
	int mRenderAvgMsec;
	int mDropFrames;
	int mRenderFrames;
	int mLatency;
	int mIOBitrate;
	int mBitrate;
	int mBufferingTimeMsec;

	ISubtitles*	mSubtitleParser;
	int64_t mSubtitleStartTime;
	int64_t mSubtitleStopTime;
	CString mSubtitleText;
	char *mSubtitleTextUtf8;
	bool mSubtitleUpdated;
	bool mGotSub;
	bool mHasEmbeddingSub;

#ifdef USE_SDL2
	SDL_Window *mWindow;
	SDL_Renderer *mRenderer;
	SDL_Texture *mTexture;
#else
	SDL_Surface *mSurface2;
#endif
private:
	bool startP2P();
	bool play_url(const char *url);
	bool start_player(const char *url);
	void stop_player();
	int64_t getSec();
	void drawBuffering();
	bool OnPrepared();
	void FillMediaInfo(MediaInfo *info, int32_t *pic = NULL, int width = DEFAULT_THUMBNAIL_WIDTH, int height = DEFAULT_THUMBNAIL_HEIGHT);
	void Cleanup();
public:
	afx_msg void OnLButtonUp(UINT nFlags, CPoint point);
	afx_msg void OnKeyUp(UINT nChar, UINT nRepCnt, UINT nFlags);
	afx_msg void OnDropFiles(HDROP hDropInfo);
	afx_msg void OnChar(UINT nChar, UINT nRepCnt, UINT nFlags);
	afx_msg void OnBnClickedButtonGetsec();
	afx_msg void OnDestroy();
	afx_msg void OnBnClickedButtonResetEpg();
	afx_msg void OnCbnSelchangeComboCatalog();
	afx_msg void OnBnClickedButtonPlayEpg();
	afx_msg void OnHScroll(UINT nSBCode, UINT nPos, CScrollBar* pScrollBar);
	afx_msg void OnNMReleasedcaptureSliderProgress(NMHDR *pNMHDR, LRESULT *pResult);
	afx_msg void OnBnClickedButtonGetMediainfo();
};
