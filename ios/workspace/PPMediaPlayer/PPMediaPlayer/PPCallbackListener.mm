//
//  PPCallbackListener.m
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-7-1.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "PPCallbackListener.h"

void playerListener::notify(int msg, int ext1, int ext2)
{
    switch (msg)
    {
        case MEDIA_ERROR:
            [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerErrorNotification object:nil];
            break;
            
        case MEDIA_SET_VIDEO_SIZE:
        {
            
//            NSDictionary *dic = [[NSDictionary alloc] init];
//            [dic setValue:[NSString stringWithFormat:@"%d",ext1] forKey:@"width"];
//            [dic setValue:[NSString stringWithFormat:@"%d",ext2] forKey:@"height"];
//            [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerSetVideoSizeNotification object:dic];
        }
            break;
            
        case MEDIA_PREPARED:
            [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerPreparedNotification object:nil];
            break;
            
        case MEDIA_PLAYBACK_COMPLETE:
            [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerPlaybackCompleteNotification object:nil];
            break;
            
        case MEDIA_SEEK_COMPLETE:
            [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerSeekCompleteNotification object:nil];
            break;
            
        case MEDIA_BUFFERING_UPDATE:
            [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerBufferingUpdateNotification object:nil];
            break;
        
        case MEDIA_INFO:
            if(ext1 == MEDIA_INFO_BUFFERING_START)
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerBufferingStartNotification object:nil];
            }
            else if(ext1 == MEDIA_INFO_BUFFERING_END)
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerBufferingEndNotification object:nil];
            }
			else if(ext1 == MEDIA_INFO_TEST_DECODE_AVG_MSEC)
            {
				//NSDictionary *dic = [[NSDictionary alloc] init];
                //[dic setValue:[NSString stringWithFormat:@"%d",ext2] forKey:@"width"];
				NSMutableDictionary *dic = [NSMutableDictionary dictionary ];
                //[NSDictionary dictionaryWithObjectsAndKeys:@"123", @"key", [NSNumber numberWithInt:ext2],@"ext2",nil];
                //[dic setObject:@"123" forKey:@"key"];
                [dic setObject:[NSNumber numberWithInt:ext2] forKey:@"msec"];
                //int a =  [[dic objectForKey:@"msec"] intValue];
                //NSLog(@"dic1: %@", dic);
                [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerDecodeAvgMsecNotification object:dic];
            }
			else if(ext1 == MEDIA_INFO_TEST_RENDER_AVG_MSEC)
            {
                NSMutableDictionary *dic = [NSMutableDictionary dictionary ];
                [dic setObject:[NSNumber numberWithInt:ext2] forKey:@"msec"];
				[[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerRenderAvgMsecNotification object:dic];
            }
			else if(ext1 == MEDIA_INFO_TEST_DECODE_FPS)
            {
				NSMutableDictionary *dic = [NSMutableDictionary dictionary ];
                [dic setObject:[NSNumber numberWithInt:ext2] forKey:@"fps"];
                [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerDecodeFPSNotification object:dic];
            }
			else if(ext1 == MEDIA_INFO_TEST_RENDER_FPS)
            {
				NSMutableDictionary *dic = [NSMutableDictionary dictionary ];
                [dic setObject:[NSNumber numberWithInt:ext2] forKey:@"fps"];
                [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerRenderFPSNotification object:dic];
            }
			else if(ext1 == MEDIA_INFO_TEST_LATENCY_MSEC)
            {
				NSMutableDictionary *dic = [NSMutableDictionary dictionary ];
                [dic setObject:[NSNumber numberWithInt:ext2] forKey:@"msec"];
                [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerLatencyMsecNotification object:dic];
            }
			else if(ext1 == MEDIA_INFO_TEST_DROP_FRAME)
            {
				NSMutableDictionary *dic = [NSMutableDictionary dictionary ];
                [dic setObject:[NSNumber numberWithInt:ext2] forKey:@"msec"];
                [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerDropFrameNotification object:dic];
            }
            break;
            
        default:
            break;
    }
}
