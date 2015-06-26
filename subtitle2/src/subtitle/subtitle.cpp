// subtitle.cpp : Defines the entry point for the console application.
//
#include <stdio.h>
#include <string.h>
#include <map>
#include <vector>
#include <list>
#include "subtitle.h"
#include <stdarg.h>

#define LOG_TAG "subtitle"
#ifdef _TEST_SUBTITLE
#include "log.h"
#else
#include "logutil.h"
#endif

extern "C" {
#include "libass/ass.h"
};

#ifdef _MSC_VER
#define strcasecmp _stricmp
#endif

const char* path_find_extension(const char* path)
{
    if (!path) {
        return NULL;
    }

    const char* pch = strrchr(path, '.');
    if (!pch) {
        return NULL;
    }
    return pch + 1;
}

// forward declared
class CSubtitleManager;
class CSimpleTextSubtitle;
class CSTSSegment;

#include "simpletextsubtitle.h"

class CSubtitleManager :
    public ISubtitles
{
public:
    CSubtitleManager();
    virtual ~CSubtitleManager();

    // ISubtitles
public:
    virtual void close();
    virtual int  getLanguageCount();
    virtual bool getLanguageName(int language, char* name);
    virtual bool getLanguageCode(int language, char* code);
    virtual int  getLanguageFlags(int language);
    virtual bool getSelectedLanguage(int* selected);
    virtual bool setSelectedLanguage(int selected);
    virtual bool getSubtitleSegment(int64_t time, STSSegment** segment);
    virtual bool seekTo(int64_t time);
    virtual bool getNextSubtitleSegment(STSSegment** segment);
    virtual bool loadSubtitle(const char* fileName, bool isMediaFile);
    virtual int  getSubtitleIndex(const char* fileName);
    virtual int  addEmbeddingSubtitle(SubtitleCodecId codecId, const char* langCode, const char* langName,
        const char* extraData, int dataLen);
    virtual bool addEmbeddingSubtitleEntity(int index, int64_t startTime, int64_t duration,
        const char* text, int textLen);
protected:
    bool loadPPSubtitle(const char* fileName);

	static void ass_log(int, const char *, va_list, void *);
	
    CSimpleTextSubtitle* getSelectedSimpleTextSubtitle()
    {
		LOGD("getSelectedSimpleTextSubtitle mSubtitles size: %d", mSubtitles.size());
		LOGD("getSelectedSimpleTextSubtitle mSelected: %d", mSelected);
        if (mSelected >= 0 && mSelected < mSubtitles.size()) {
            return mSubtitles[mSelected];
        }
        return NULL;
    }

    ASS_Library*  mAssLibrary;
    std::vector<CSimpleTextSubtitle*>  mSubtitles;
    int           mSelected;
};

CSubtitleManager::CSubtitleManager()
{
    mAssLibrary = ass_library_init();
	ass_set_message_cb(mAssLibrary, ass_log, this);
    mSelected = 0;
}

CSubtitleManager::~CSubtitleManager()
{
    close();
}

void CSubtitleManager::ass_log(int level, const char *fmt, va_list va, void *data)
{
/*
#define MSGL_FATAL 0
#define MSGL_ERR 1
#define MSGL_WARN 2
#define MSGL_INFO 4
#define MSGL_V 6
#define MSGL_DBG2 7
*/

	if (level > 4)
		return;

	static char msg[1024] = {0};
    const char *header = "[ass] ";
    vsnprintf(msg, 1024, fmt, va);
	LOGI("%s %s", header, msg);
}

void CSubtitleManager::close() 
{
	if (mAssLibrary) {
		ass_library_done(mAssLibrary);
		mAssLibrary = NULL;
	}

    std::vector<CSimpleTextSubtitle*>::iterator itr = mSubtitles.begin();
    for (; itr != mSubtitles.end(); ++itr) {
        delete *itr;
    }
    mSubtitles.clear();
}

int  CSubtitleManager::getLanguageCount()
{
    return mSubtitles.size();
}

bool CSubtitleManager::getLanguageName(int language, char* name)
{
    if (language < 0 || language >= mSubtitles.size()) {
        return false;
    }

    const char* languageName = mSubtitles[language]->getLanguageName();
    if (languageName) {
        strncpy(name, languageName, 511);
        name[511] = '\x0';
        return true;
    }

    return false;
}

bool CSubtitleManager::getLanguageCode(int language, char* code)
{
    return false;
}

int  CSubtitleManager::getLanguageFlags(int language)
{
    return 0;
}

bool CSubtitleManager::getSelectedLanguage(int* selected)
{
    if (!selected) {
        return false;
    }

    if (mSelected >= 0 && mSelected < mSubtitles.size()) {
        *selected = mSelected;
        return true;
    }

    return false;
}

bool CSubtitleManager::setSelectedLanguage(int selected)
{
    if (selected < 0 && selected >= mSubtitles.size()) {
        return false;
    }
    mSelected = selected;
    return true;
}

bool CSubtitleManager::getSubtitleSegment(int64_t time, STSSegment** segment)
{
    return false;
}

bool CSubtitleManager::seekTo(int64_t time)
{
    CSimpleTextSubtitle* subtitle = getSelectedSimpleTextSubtitle();
    if (subtitle) {
        return subtitle->seekTo(time);
    }

    return false;
}

bool CSubtitleManager::getNextSubtitleSegment(STSSegment** segment)
{
	LOGD("getNextSubtitleSegment()");

    CSimpleTextSubtitle* subtitle = getSelectedSimpleTextSubtitle();
	
    if (subtitle == NULL) {
        LOGE("textsubtitle is NULL");
		return false;
    }

	//mSelected ++;
	return subtitle->getNextSubtitleSegment(segment);
}

bool CSubtitleManager::loadSubtitle(const char* fileName, bool isMediaFile)
{
    std::vector<CSimpleTextSubtitle*>::iterator itr = mSubtitles.begin();
    for (; itr != mSubtitles.end(); ++itr) {
        if (strcmp((*itr)->getFileName(), fileName) == 0) {
            return true;
        }
    }

    const char *extension = path_find_extension(fileName);
    if (extension && strcasecmp(extension, "ppsrt") == 0) {
        return loadPPSubtitle(fileName);
    }
    
    CSimpleTextSubtitle* subtitle = new CSimpleTextSubtitle(mAssLibrary);
    if (!subtitle->loadFile(fileName)) {
        delete subtitle;
		LOGE("failed to load subtitle: %s", fileName);
        return false;
    }

    subtitle->seekTo(0);

    mSubtitles.push_back(subtitle);

    return true;
}

bool CSubtitleManager::loadPPSubtitle(const char* fileName)
{
    tinyxml2::XMLDocument xmlDoc;
    if (xmlDoc.LoadFile(fileName) != tinyxml2::XML_SUCCESS) {
		LOGE("failed to load xmlfile: %s", fileName);
        return false;
    }

    tinyxml2::XMLElement* subEle = NULL;
    if (xmlDoc.RootElement()) {
        subEle = xmlDoc.RootElement()->FirstChildElement("sub");
    }
    while (subEle) {
        CSimpleTextSubtitle* subtitle = new CSimpleTextSubtitle(mAssLibrary);
        if (subtitle->parseXMLNode(fileName, subEle)) {
            subtitle->seekTo(0);
            mSubtitles.push_back(subtitle);
        } else {
            delete subtitle;
        }

        subEle = subEle->NextSiblingElement("sub");
    }

    return (mSubtitles.size() != 0);
}

int  CSubtitleManager::getSubtitleIndex(const char* fileName)
{
    std::vector<CSimpleTextSubtitle*>::iterator itr = mSubtitles.begin();
    for (int index = 0; itr != mSubtitles.end(); ++itr) {
        if (strcmp((*itr)->getFileName(), fileName) == 0) {
            return index;
        }
    }

    return -1;
}

int CSubtitleManager::addEmbeddingSubtitle(SubtitleCodecId codecId, const char* langCode, const char* langName,
    const char* extraData, int dataLen)
{
    CSimpleTextSubtitle* subtitle = new CSimpleTextSubtitle(mAssLibrary);
    if (!subtitle) {
        return -1;
    };
    
    subtitle->setLanguageName(langName);
    if (!subtitle->loadEmbedding(codecId, extraData, dataLen)) {
        delete subtitle;
        return -1;
    }
    mSubtitles.push_back(subtitle);
	LOGD("addEmbeddingSubtitle push_back = %d", mSubtitles.size());
    return mSubtitles.size() - 1;
}

bool CSubtitleManager::addEmbeddingSubtitleEntity(int index, int64_t startTime, int64_t duration,
    const char* text, int textLen)
{
    if (index < 0 || index >= mSubtitles.size()) {
        return false;
    }

    CSimpleTextSubtitle* subtitle = mSubtitles[index];
    if (!subtitle->isEmbedding()) {
        return false;
    }
	
	LOGD("addEmbeddingSubtitle: #%d startTime %lld, text %s", index, startTime, text);
    return subtitle->addEmbeddingEntity(startTime, duration, text, textLen);
}

bool ISubtitles::create(ISubtitles** subtitle)
{
    if (!subtitle) {
		LOGE("subtitle pointer is null");
        return false;
	}

    *subtitle = new CSubtitleManager;
    if (*subtitle == NULL) {
		LOGE("failed to new CSubtitleManager");
        return false;
	}

    return true;
}

