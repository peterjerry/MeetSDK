#include "urlcodec.h"
#include <stdio.h>  
#include <string.h>
#include <stdlib.h>
#include <ctype.h>

/**
* @param s 需要编码的url字符串
* @param len 需要编码的url的长度
* @param new_length 编码后的url的长度
* @return char * 返回编码后的url
* @note 存储编码后的url存储在一个新审请的内存中，
* 用完后，调用者应该释放它
*/
char * urlencode(char const *s, int len, int *new_length)
{
    printf( s);
    printf("\n");

    unsigned char const *from, *end;
	unsigned char *start, *to;
	char c;

    from = (unsigned char const *)s;
    end = (unsigned char const *)s + len;
    start = to = (unsigned char *) malloc(3 * len + 1);

    unsigned char hexchars[] = "0123456789ABCDEF";

    while (from < end) {
        c = *from++;

        if (c == ' ') {
            *to++ = '+';
        } else if ((c < '0' && c != '-' && c != '.')
                   ||(c < 'A' && c > '9')
                   ||(c > 'Z' && c < 'a' && c != '_')
                   ||(c > 'z')) {
            to[0] = '%';
            to[1] = hexchars[c >> 4];
            to[2] = hexchars[c & 15];
            to += 3;
        } else {
            *to++ = c;
        }
    }
    *to = 0;
    if (new_length) {
        *new_length = to - start;
    }
    return (char *) start;

}

/**
* @param str 需要解码的url字符串
* @param len 需要解码的url的长度
* @return int 返回解码后的url长度
*/
int urldecode(char *str, int len)
{
    char *dest = str;
    char *data = str;

    int value;
    int c;

    while (len--) {
        if (*data == '+') {
        *dest = ' ';
        }
        else if (*data == '%' && len >= 2 && isxdigit((int) *(data + 1))
                 && isxdigit((int) *(data + 2)))
        {

            c = ((unsigned char *)(data+1))[0];
            if (isupper(c))
                c = tolower(c);
            value = (c >= '0' && c <= '9' ? c - '0' : c - 'a' + 10) * 16;
            c = ((unsigned char *)(data+1))[1];
            if (isupper(c))
                c = tolower(c);
            value += c >= '0' && c <= '9' ? c - '0' : c - 'a' + 10;

            *dest = (char)value ;
            data += 2;
            len -= 2;
        } else {
            *dest = *data;
        }
        data++;
        dest++;
    }
    *dest = '\0';
    return dest - str;
}
