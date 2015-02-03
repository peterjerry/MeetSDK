//
//  PPMediaPlayerInfo.h
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-7-11.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>

typedef struct PPMediaInfo
{
    int duration;               //影片时长
	int width;                  //影片宽度
	int height;                 //影片高度
	const char* audio_name;     //音频格式
	const char* video_name;     //视频格式
    int audio_channels;         //音频通道数
	int video_channels;         //视频通道数
}PPMediaInfo;                   //影片信息结构体

//获取影片信息单例类
@interface PPMediaPlayerInfo : NSObject

//单例初始化
+ (id)sharedInstance;

//获取影片缩略图
- (UIImage*)getThumbnail:(NSString*)url;

//获取影片详细信息
- (PPMediaInfo)getMediaInfo:(NSString*)url;

//指定要获取信息的影片URL
- (void)setMediaURL:(NSString*)url;

//获取音轨信息
- (NSMutableDictionary*)getAudioChannel;

//获得播放器版本号
- (NSString*)getPlayerVersion;

@end
