#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include <memory.h>
#include "subtitle.h"
#include "stssegment.h"
#include "simpletextsubtitle.h"

#define LOG_TAG "simple_subtitle"
#ifdef _TEST_SUBTITLE
#include "log.h"
#else
#include "logutil.h"
#endif

#ifdef _MSC_VER 
#define strdup _strdup
static char* ass_remove_format_tag(char* src);
static int mystrtoi(char **p, int *res);
#endif

#include <set>

CSimpleTextSubtitle::~CSimpleTextSubtitle()
{
    if (mAssTrack) {
        ass_free_track(mAssTrack);
        mAssTrack = NULL;
    }
    std::vector<CSTSSegment*>::iterator itr = mSegments.begin();
    for (; itr != mSegments.end(); ++itr) {
        delete *itr;
    }
    mSegments.clear();
    if (mFileName) {
        free((void*)mFileName);
        mFileName = NULL;
    }
    if (mLanguageName) {
        free((void*)mLanguageName);
        mLanguageName = NULL;
    }
    if (mEmbeddingLock != NULL) {
        pthread_mutex_destroy(mEmbeddingLock);
        free(mEmbeddingLock);
        mEmbeddingLock = NULL;
    }
}

void CSimpleTextSubtitle::setLanguageName(const char* name)
{
    if (mLanguageName) {
        free((void*)mLanguageName);
        mLanguageName = NULL;
    }
    if (name) {
        mLanguageName = strdup(name);
    }
}

bool CSimpleTextSubtitle::loadFile(const char* fileName)
{
    if (!mAssLibrary) {
		LOGE("ass lib not loaded");
        return false;
    }

#ifdef _MSC_VER
    const char *codepage = "UTF-8";
#else
	const char *codepage = "enca:zh:gb2312";
#endif
    ASS_Track* track = ass_read_file(mAssLibrary, (char *)fileName, (char *)codepage);
	if (!track) {
		LOGE("track init failed: %s", fileName);
        return false;
    }

    if (!arrangeTrack(track)) {
        ass_free_track(track);
		LOGE("failed to arrange track %s", fileName);
        return false;
    }

    mAssTrack = track;
    mFileName = strdup(fileName);

    return true;
}

/*
 * 分析pptv私有字幕格式
 * 文档: http://sharepoint/tech/mediapipelinedivision/SitePages/sub.aspx
 */
bool CSimpleTextSubtitle::parseXMLNode(const char* fileName, tinyxml2::XMLElement* element)
{
    if (element->Attribute("title")) {
        mLanguageName = strdup(element->Attribute("title"));
    }

    ASS_Track* track = ass_new_track(mAssLibrary);
    tinyxml2::XMLElement* child = element->FirstChildElement("item");
    while(child) 
    {
        tinyxml2::XMLElement* stEle  = child->FirstChildElement("st");
        tinyxml2::XMLElement* etEle  = child->FirstChildElement("et");
        tinyxml2::XMLElement* subEle = child->FirstChildElement("sub");
        if (stEle && etEle && subEle) {
            unsigned int st, et;
            const char *sub;
            if (stEle->QueryUnsignedText(&st) == tinyxml2::XML_SUCCESS
                && etEle->QueryUnsignedText(&et) == tinyxml2::XML_SUCCESS
                && et > st
                && (sub = subEle->GetText()) != NULL) {
                    int eid;
                    ASS_Event *event;

                    eid = ass_alloc_event(track);
                    event = track->events + eid;

                    event->Start = st;
                    event->Duration = et - st;
                    event->Text = ass_remove_format_tag(strdup(sub));

                    if (strlen(event->Text) == 0) {
                        ass_free_event(track, eid);
                        track->n_events--;
                    }
            }
        }

        child = child->NextSiblingElement("item");
    }

    if (!arrangeTrack(track)) {
        ass_free_track(track);
        return false;
    }
    mAssTrack = track;
    mFileName = strdup(fileName);

    return true;
}

bool CSimpleTextSubtitle::arrangeTrack(ASS_Track* track)
{
	LOGD("arrangeTrack()");

    std::set<int64_t> breakpoints;
    for (int i = 0; i < track->n_events; ++i) {
        ASS_Event* event = &track->events[i];
        int64_t startTime = event->Start;
        int64_t stopTime  = event->Start + event->Duration;

        breakpoints.insert(startTime);
        breakpoints.insert(stopTime);
    }

    std::set<int64_t>::iterator itr = breakpoints.begin();
    int64_t prev = 0;
    if (itr != breakpoints.end()) {
        prev = *itr;
        ++itr;
    }
    for (; itr != breakpoints.end(); ++itr) {
        CSTSSegment* segment = new CSTSSegment(this, prev, *itr);
        mSegments.push_back(segment);
        prev = *itr;
    }

    for (int i = 0; i < track->n_events; ++i) {
        ASS_Event* event = &track->events[i];
        int64_t startTime = event->Start;
        int64_t stopTime  = event->Start + event->Duration;
		LOGD("arrangeTrack = %s", event->Text);

        size_t j = 0;
        for (j = 0; j < mSegments.size() && mSegments[j]->mStartTime < startTime; ++j) {
			// do nothing
        }

        for (; j < mSegments.size() && mSegments[j]->mStopTime <= stopTime; ++j) {
            CSTSSegment* s = mSegments[j];
            for (int l = 0, m = s->mSubs.size(); l <= m; l++) {
                if (l == m || event->ReadOrder < track->events[s->mSubs[l]].ReadOrder) {
                    s->mSubs.insert(s->mSubs.begin() + l, i);
                    break;
                }
            }
        }
    }

    // 删除空segment
    for (int i = mSegments.size() - 1; i >= 0; --i) {
        if (mSegments[i]->mSubs.size() <= 0) {
            CSTSSegment* p = mSegments[i];
            mSegments.erase(mSegments.begin() + i);
            delete p;
        }
    }
    return true;
}

bool CSimpleTextSubtitle::seekTo(int64_t time)
{
    size_t nextPos = 0;
    for (size_t i = 0; i < mSegments.size(); ++i, ++nextPos) {
        CSTSSegment* segment = mSegments.at(i);
        if (segment->mStopTime >= time) {
            break;
        }
    }

    mNextSegment = nextPos;
    return true;
}

bool CSimpleTextSubtitle::getNextSubtitleSegment(STSSegment** segment)
{
    if (!segment) {
        return false;
    }

    if (isEmbedding() && mDirty) {
		LOGD("getNextSubtitleSegment mDirty ");
        pthread_mutex_lock(mEmbeddingLock);
        arrangeTrack(mAssTrack);
		// 2015.4.30 guoliangma added to fix duplicated text problem
		mDirty = false;
        pthread_mutex_unlock(mEmbeddingLock);
    }
	
	LOGD("getNextSubtitleSegment mNextSegment %d, size %d", mNextSegment, mSegments.size());

    if (mNextSegment >= mSegments.size()) {
		LOGE("no more segment is available need %d, size %d", mNextSegment, mSegments.size());
		return false;
	}
	
	*segment = mSegments[mNextSegment];
	mNextSegment++;
	return true;
}

ASS_Event* CSimpleTextSubtitle::getEventAt(int pos)
{
    ASS_Event* event = NULL;
    if (mEmbeddingLock != NULL) {
        pthread_mutex_lock(mEmbeddingLock);
    }
    if (mAssTrack && pos >= 0 && pos < mAssTrack->n_events) {
        event = &mAssTrack->events[pos];
    }
    if (mEmbeddingLock != NULL) {
        pthread_mutex_unlock(mEmbeddingLock);
    }

    return event;
}

bool CSimpleTextSubtitle::loadEmbedding(SubtitleCodecId codecId, const char* extraData, int dataLen)
{
    mCodecId = codecId;
    ASS_Track* track = ass_new_track(mAssLibrary);
    if (!track) {
        return false;
    }

    if (mCodecId == SUBTITLE_CODEC_ID_ASS) {
        ass_process_codec_private(track, (char*)extraData, dataLen);
    }

    mAssTrack = track;
    mEmbeddingLock = (pthread_mutex_t*)malloc(sizeof(pthread_mutex_t));
    pthread_mutex_init(mEmbeddingLock, NULL);
    return true;
}

bool CSimpleTextSubtitle::addEmbeddingEntity(int64_t startTime, int64_t duration,
    const char* text, int textLen)
{
    pthread_mutex_lock(mEmbeddingLock);
    if (mCodecId == SUBTITLE_CODEC_ID_TEXT) {
        int eid;
        ASS_Event *event;

        eid = ass_alloc_event(mAssTrack);
        event = mAssTrack->events + eid;

        event->Start = startTime;
        event->Duration = duration;
        event->Text = ass_remove_format_tag(strdup(text));

        if (strlen(event->Text) == 0) {
            ass_free_event(mAssTrack, eid);
            mAssTrack->n_events--;
        }
    } else if (mCodecId == SUBTITLE_CODEC_ID_ASS){
        LOGD("addEmbeddingEntity ass_process_chunk = %s", text);
		// 2015.4.30 guoliangma modify function call to fix add event problem
        //ass_process_chunk(mAssTrack, (char*)text, textLen, startTime, duration);
		ass_process_data(mAssTrack, (char*)text, textLen);
    }

    mDirty = true;
    pthread_mutex_unlock(mEmbeddingLock);

    return true;
}

#ifdef _MSC_VER
static int mystrtoi(char **p, int *res)
{
    double temp_res;
    char *start = *p;
    temp_res = strtod(*p, p);
    *res = (int) (temp_res + (temp_res > 0 ? 0.5 : -0.5));
    if (*p != start)
        return 1;
    else
        return 0;
}

static char* ass_remove_format_tag(char* src)
{
    char *p1 = src;
    char *p2 = src;
    int drawing_mode = 0;

    if (!src) {
        return src;
    }

    while(*p2) {
        int in_tag = 0;
        if (*p2 == '{') {
            p2++;
            in_tag = 1;
        }
        if (in_tag) {
            while (*p2 != '}' && *p2 != 0) {
                if (*p2 == '\\' && *(p2 + 1) == 'p'
                    && (*(p2 + 2) >= '0' && *(p2 + 2) <= '9')) {
                    int val = 0;
                    p2 += 2;
                    if (!mystrtoi(&p2, &val)) {
                        val = 0;
                    }
                    drawing_mode = !!val;
                }
                ++p2;
            }
            if (*p2 == '}') { p2++; }
        } else if (*p2 == '\\' && (*(p2 + 1) == 'n' || *(p2 + 1) == 'N')) {
            *p1++ = '\n';
            p2 += 2;
        } else {
            if (!drawing_mode) {
                *p1++ = *p2++;
            } else {
                p2++;
            }
        }
    }
    
    *p1 = '\x0';
    return src;
}
#endif