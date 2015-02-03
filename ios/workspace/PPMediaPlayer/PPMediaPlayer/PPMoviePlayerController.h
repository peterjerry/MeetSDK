//
//  PPMoviePlayerController.h
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-6-28.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#import <MediaPlayer/MediaPlayer.h>

//播放器准备完成通知
#define PPMoviePlayerPreparedNotification           @"PPMoviePlayerPreparedNotification"
//播放器准备错误通知
#define PPMoviePlayerErrorNotification              @"PPMoviePlayerErrorNotification"
//设置播放器大小通知
#define PPMoviePlayerSetVideoSizeNotification       @"PPMoviePlayerSetVideoSizeNotification"
//影片播放完成通知
#define PPMoviePlayerPlaybackCompleteNotification   @"PPMoviePlayerPlaybackCompleteNotification"
//影片拖动完成通知
#define PPMoviePlayerSeekCompleteNotification       @"PPMoviePlayerSeekCompleteNotification"
//影片缓冲开始通知
#define PPMoviePlayerBufferingStartNotification     @"PPMoviePlayerBufferingStartNotification"
//影片缓冲完成通知
#define PPMoviePlayerBufferingEndNotification       @"PPMoviePlayerBufferingEndNotification"
//影片缓冲百分比通知
#define PPMoviePlayerBufferingUpdateNotification    @"PPMoviePlayerBufferingUpdateNotification"
//影片暂停通知
#define PPMoviePlayerPausedNotification             @"PPMoviePlayerPausedNotification"
//影片播放通知
#define PPMoviePlayerRestartNotification            @"PPMoviePlayerRestartNotification"
// new added
#define PPMoviePlayerDecodeAvgMsecNotification		@"PPMoviePlayerDecodeAvgMsecNotification"
#define PPMoviePlayerRenderAvgMsecNotification		@"PPMoviePlayerRenderAvgMsecNotification"
#define PPMoviePlayerDecodeFPSNotification			@"PPMoviePlayerDecocdeFPSNotification"
#define PPMoviePlayerRenderFPSNotification			@"PPMoviePlayerRenderFPSNotification"
#define PPMoviePlayerLatencyMsecNotification		@"PPMoviePlayerLatencyMsecNotification"
#define PPMoviePlayerDropFrameNotification			@"PPMoviePlayerDropFrameNotification"

enum PPMoviePlayerType
{
    PPMOVIE_SYSTEM_PLAYER = 0,
    PPMOVIE_SELF_PLAYER,
    PPMOVIE_AUTO_PLAYER
};

@interface PPMoviePlayerController : NSObject

//系统播放器
@property(nonatomic, strong) MPMoviePlayerController * applePlayer;

//播放内容地址
@property(nonatomic, copy) NSURL *contentURL;

//渲染view
@property(nonatomic, strong) UIView *view;

//片长:单位s
@property(nonatomic, readonly) double duration;

//是否准备完成
@property(nonatomic, readonly) BOOL isPreparedToPlay;

//当前播放时间
@property(nonatomic) double currentPlaybackTime;

//视频源宽
@property(nonatomic) int width;

//视频源高
@property(nonatomic) int height;

//初始化函数
- (id)init:(enum PPMoviePlayerType) type url:(NSURL*)url;

//获取当前播放器类型
- (enum PPMoviePlayerType)playerType;

//默认初始化,使用系统播放器(日后版本将废弃）
- (id)init;

//使用自有播放器初始化(日后版本将废弃）
- (id)initSplayer;

//异步准备
- (void)prepareToPlay;

//开始播放
- (void)play;

//暂停播放
- (void)pause;

//停止播放
- (void)stop;

//拖动到第second秒
- (void)setCurrentPlaybackTime:(double)second;

//获得当前播放时间
- (double)currentPlaybackTime;

//切换音轨
- (void)changeAudioChannel:(int)channel;

//软硬解切换
- (void)changeDecoding;

//主动释放播放器
- (void)playerRelease;

+ (BOOL)canHardwareDecoding:(NSURL*)url;

@end

