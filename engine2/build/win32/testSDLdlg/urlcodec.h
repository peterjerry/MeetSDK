#ifndef URL_CODEC_H
#define URL_CODEC_H

/**
* @param s 需要编码的url字符串
* @param len 需要编码的url的长度
* @param new_length 编码后的url的长度
* @return char * 返回编码后的url
* @note 存储编码后的url存储在一个新审请的内存中，
* 用完后，调用者应该释放它
*/
char * urlencode(char const *s, int len, int *new_length);

/**
* @param str 需要解码的url字符串
* @param len 需要解码的url的长度
* @return int 返回解码后的url长度
*/
int urldecode(char *str, int len);

#endif //URL_CODEC_H

