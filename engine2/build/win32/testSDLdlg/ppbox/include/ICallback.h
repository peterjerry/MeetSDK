// ICallback.h

#ifndef _PPBOX_PPBOX_I_CALLBACK_H_
#define _PPBOX_PPBOX_I_CALLBACK_H_

#include "IPpbox.h"

#if __cplusplus
extern "C" {
#endif // __cplusplus

PPBOX_DECL PP_handle PPBOX_ScheduleCallback(
    PP_uint32 delay, 
    void* user_data, 
    PPBOX_Callback callback);

PPBOX_DECL PP_int32 PPBOX_CancelCallback(PP_handle handle);

#if __cplusplus
}
#endif // __cplusplus

#endif // _PPBOX_PPBOX_I_CALLBACK_H_
