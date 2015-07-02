#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include <memory.h>
#include "subtitle.h"
#include "stssegment.h"
#include "simpletextsubtitle.h"
#define LOG_TAG "CSTSSegment"
#ifdef _TEST_SUBTITLE
#include "log.h"
#else
#include "logutil.h"
#endif

extern "C" {
#include "libass/ass.h"
};

int CSTSSegment::getSubtitleText(char* text, int maxLength)
{
    int textLength = 0;
	LOGI("mSubs.size() %d", mSubs.size());
    for (size_t i = 0; i < mSubs.size(); ++i) {
        ASS_Event* event = mSubtitle->getEventAt(mSubs[i]);
        if (!text) {
            if (textLength) {
                textLength += 1;
            }
            textLength += strlen(event->Text);
        } else {
            if (textLength && maxLength > 1) {
                *text++ = '\n';
                textLength ++;
                maxLength--;
            }

			// handle ansi and unicode charset
			const char* p = event->Text;
			while (maxLength > 1 && *p) {
				--maxLength;
				*text++ = *p++;
				++textLength;
            }
        }
    }
    if (text) {
        *text = '\0';
    }

    return textLength;
}
