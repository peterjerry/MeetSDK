#include "StdAfx.h"
#include ".\approcessbmp.h"

apProcessBMP::apProcessBMP(void)
{
}

apProcessBMP::~apProcessBMP(void)
{
}

bool apProcessBMP::ReadBMPInfo(const char* filename)
{
	FILE *pFile = NULL;
	fopen_s(&pFile, filename, "rb");
	if(!pFile)
		return false;

	BITMAPFILEHEADER filehead;
	BITMAPINFOHEADER infohead;
	memset(&filehead, sizeof(BITMAPFILEHEADER), 0);
	memset(&infohead, sizeof(BITMAPINFOHEADER), 0);

	fread(&filehead, sizeof(BITMAPFILEHEADER), 1, pFile);
	printf("bfSize: %d\nbfOffBits: %d\n", filehead.bfSize, filehead.bfOffBits);

	fread(&infohead, sizeof(BITMAPINFOHEADER), 1, pFile);
	printf("biBitCount: %d\nbiClrImportant: %d\nbiClrUsed: %d\nbiCompression %d\n",
		infohead.biBitCount,
		infohead.biClrImportant,
		infohead.biClrUsed,
		infohead.biCompression);
	printf("biWidth: %d\nbiHeight: %d\nbiPlanes: %d\nbiSize %d\nbiSizeImage: %d\n",
		infohead.biWidth,
		infohead.biHeight,
		infohead.biPlanes,
		infohead.biSize,
		infohead.biSizeImage);
	printf("biXPelsPerMeter: %d\nbiYPelsPerMeter: %d\n",
		infohead.biXPelsPerMeter,
		infohead.biYPelsPerMeter);

	fclose(pFile);
	return true;
}

bool apProcessBMP::ReadBMP(const char* filename, char **pData, int* pDatalen, bool bFlip)
{
	FILE *pFile = NULL;
	fopen_s(&pFile, filename, "rb");
	if(!pFile)
		return false;

	int bmpWidth,bmpHeight;
	int bmpBpp;
	int lineLen;
	int bmpDatalen;
	int ret;

	//skip BMP file header
	fseek(pFile, sizeof(BITMAPFILEHEADER), SEEK_SET);

	BITMAPINFOHEADER infohead;
	fread(&infohead, sizeof(BITMAPINFOHEADER), 1, pFile);
	bmpWidth = infohead.biWidth;
	bmpHeight = infohead.biHeight;
	bmpBpp = infohead.biBitCount;
	lineLen = (bmpWidth * bmpBpp / 8 + 3 ) / 4 * 4;//aligned to 4 bytes 
	bmpDatalen = lineLen * bmpHeight;
	*pDatalen = bmpDatalen;

	//bpp 8,16 not supported, only support 24 and 32
	if(bmpBpp < 24)
		return false;

	*pData = new char[bmpDatalen];
	char *p_img = *pData;

	ret = fread(p_img, 1, bmpDatalen, pFile);

	//flip img
	if(bFlip)
	{
		char *tmpLine = new char[lineLen];
		for(int i=0; i < bmpHeight / 2; i++)
		{
			memcpy(tmpLine, p_img + i*lineLen, lineLen);
			memcpy(p_img + i*lineLen, //des
				p_img + (bmpHeight - i - 1)*lineLen,//src
				lineLen);//count
			memcpy(p_img + (bmpHeight - i - 1)*lineLen, tmpLine, lineLen);
		}
		delete tmpLine;
	}
	fclose(pFile);
	return true;
}

bool apProcessBMP::WriteBMP(const char* filename, int width, int height, 
			  int bpp, const char *pData, int datalen, bool bFlip)
{
	FILE *pFile = NULL;
	fopen_s(&pFile, filename, "wb");
	if(!pFile)
		return false;

	int bmpWidth,bmpHeight;
	int bmpBpp;
	int lineLen;
	char *pImgBuf = NULL;
	int bmpDatalen;
	int ret;

	bmpWidth = width;
	bmpHeight = height;
	bmpBpp = bpp;
	lineLen = (bmpWidth * bmpBpp / 8 + 3 ) / 4 * 4;//aligned to 4 bytes
	bmpDatalen = lineLen * bmpHeight;

	if(bmpBpp < 24)
		return false;

	pImgBuf = new char[bmpDatalen];
	if(bFlip)
	{
		for(int i=0; i < bmpHeight; i++)
		{
			memcpy(pImgBuf + i*lineLen, //des
				pData + (bmpHeight - i - 1) * lineLen,//src
				lineLen);//count
		}
	}
	else
	{
		memcpy(pImgBuf, pData, bmpDatalen);
	}

	BITMAPFILEHEADER filehead;
	memset(&filehead, 0, sizeof(BITMAPFILEHEADER));
	filehead.bfType = MAKEWORD('B', 'M');//0x4D42;
	filehead.bfSize = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER)
		+ lineLen * bmpHeight;
	filehead.bfReserved1 = 0;
	filehead.bfReserved2 = 0;
	filehead.bfOffBits = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER);

	ret = fwrite(&filehead, sizeof(BITMAPFILEHEADER),1, pFile);

	BITMAPINFOHEADER infohead;
	memset(&infohead, 0 ,sizeof(BITMAPINFOHEADER));
	infohead.biBitCount = bmpBpp;
	infohead.biClrImportant = 0;
	infohead.biClrUsed = 0;
	infohead.biCompression = 0;
	infohead.biHeight = bmpHeight;
	infohead.biPlanes = 1;
	infohead.biSize = sizeof(BITMAPINFOHEADER);
	infohead.biSizeImage = lineLen * bmpHeight;
	infohead.biWidth = bmpWidth;
	infohead.biXPelsPerMeter = 0;
	infohead.biYPelsPerMeter = 0;

	ret = fwrite(&infohead, sizeof(BITMAPINFOHEADER), 1, pFile);

	ret = fwrite(pImgBuf, bmpDatalen, 1, pFile);
	
	fclose(pFile);
	delete pImgBuf;
	return true;
}
