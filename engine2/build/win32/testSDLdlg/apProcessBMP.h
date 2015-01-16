#pragma once

class apProcessBMP
{
public:
	apProcessBMP(void);
	~apProcessBMP(void);
	static bool ReadBMPInfo(const char* filename);
	static bool ReadBMP(const char* filename, char **pData, int* pDatalen, bool bFlip = false);
	static bool WriteBMP(const char* filename, int width, int height, 
		int bpp, const char *pData, int datalen, bool bFlip = true);
};
