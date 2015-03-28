
extern "C" {
#include "libass/ass.h"
};
#include <vector>
#include <pthread.h>

#include "tinyxml2.h"

class CSTSSegment;

class CSimpleTextSubtitle
{
public:
    friend class STSSegment;
    CSimpleTextSubtitle(ASS_Library* assLibrary):mAssLibrary(assLibrary),mAssTrack(NULL)
    {
        mNextSegment = 0;
        mFileName = NULL;
        mLanguageName = NULL;
        mCodecId = SUBTITLE_CODEC_ID_NONE;
        mDirty = false;
        mEmbeddingLock = NULL;
    }
    virtual ~CSimpleTextSubtitle();

    bool loadFile(const char* fileName);
    bool parseXMLNode(const char* fileName, tinyxml2::XMLElement* element);
    bool getSubtitleSegment(int64_t time, STSSegment** segment)
    {
        return false;
    }
    bool seekTo(int64_t time);
    bool getNextSubtitleSegment(STSSegment** segment);
    ASS_Event* getEventAt(int pos);

    const char* getFileName()
    {
        return mFileName;
    }
    void setLanguageName(const char* name);
    const char* getLanguageName()
    {
        return mLanguageName;
    }
    // embedding subtitle
    bool isEmbedding() const
    {
        return (mCodecId != SUBTITLE_CODEC_ID_NONE);
    }
    bool loadEmbedding(SubtitleCodecId codecId, const char* extraData, int dataLen);
    bool addEmbeddingEntity(int64_t startTime, int64_t duration,
        const char* text, int textLen);
protected:
    bool arrangeTrack(ASS_Track* track);

    std::vector<CSTSSegment*>  mSegments;
    ASS_Library*            mAssLibrary;
    ASS_Track*              mAssTrack;
    size_t                  mNextSegment;
    const char*             mFileName;
    const char*             mLanguageName;
    SubtitleCodecId         mCodecId;
    bool                    mDirty;
    pthread_mutex_t*        mEmbeddingLock;
};
