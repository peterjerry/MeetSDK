#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include <memory.h>
#include "subtitle.h"
#include "stssegment.h"
#include "simpletextsubtitle.h"

int CSTSSegment::getSubtitleText(char* text, int maxLength)
{
    int textLength = 0;
    for (size_t i = 0; i < mSubs.size(); ++i) {
        ASS_Event* one_event = mSubtitle->getEventAt(mSubs[i]);
		if (!one_event)
			continue;

        if (!text) {
            if (textLength) {
                textLength += 1;
            }
            textLength += strlen(one_event->Text);
        } else {
            if (textLength && maxLength > 1) {
                *text++ = '\n';
                textLength ++;
                maxLength--;
            }

			// handle ansi and unicode charset
			const char* p = one_event->Text;
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
