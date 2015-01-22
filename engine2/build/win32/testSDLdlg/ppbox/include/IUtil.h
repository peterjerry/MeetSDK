// IUtil.h

#ifndef _PPBOX_PPBOX_I_UTIL_H_
#define _PPBOX_PPBOX_I_UTIL_H_

#include "IPpbox.h"

#if __cplusplus
extern "C" {
#endif // __cplusplus

PPBOX_DECL PP_char const * PPBOX_Key_Base64Encode(
    PP_char const * str, 
    PP_char const * key);

PPBOX_DECL PP_char const * PPBOX_Base64Encode(
    PP_char const * str);

//3des 加密
PPBOX_DECL PP_int32 PPBOX_3DES_EnCipher(const PP_char *In, PP_uint32 in_len, const PP_char *Key, PP_uint32 key_len, PP_char* Out, PP_uint32 out_len);

//3des 解密
PPBOX_DECL PP_int32 PPBOX_3DES_DnCipher(const PP_char *In, PP_uint32 in_len, const PP_char *Key, PP_uint32 key_len, PP_char* Out, PP_uint32 out_len);


//pptv url加密
PPBOX_DECL PP_int32 PPBOX_PPTVURL_EnCipher(const PP_char *In, PP_uint32 in_len,PP_uint32 keyIndex, PP_char* Out, PP_uint32 out_len);

//pptv url 解密
PPBOX_DECL PP_int32 PPBOX_PPTVURL_DnCipher(const PP_char *In, PP_uint32 in_len,PP_uint32 keyIndex, PP_char* Out, PP_uint32 out_len);

PPBOX_DECL PP_int32 PPBOX_MergeMoive(const PP_char* source, const PP_char* format, const PP_char* dest);
#if __cplusplus
}
#endif // __cplusplus

#endif // _PPBOX_PPBOX_I_UTIL_H_
