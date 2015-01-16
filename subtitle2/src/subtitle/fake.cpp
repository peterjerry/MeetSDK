#include "subtitle.h"

int main(int argc, char* argv[])
{
	ISubtitles *pSub = NULL;
	ISubtitles::create(&pSub);
	if (pSub) {
		pSub->loadSubtitle("", true);
		pSub->close();
	}
	
	return 0;
}