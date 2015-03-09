//
//  PPPlayerController.h
//  PPMediaPlayer2
//
//  Created by zenzhang on 14-11-21.
//  Copyright (c) 2014年 pplive. All rights reserved.
//

#import <UIKit/UIKit.h>

#pragma mark PPPlayerDelegate

@protocol PPPlayerDelegate <NSObject>

@optional
- (void)PPPlayerLoadFailed:(NSError*)err;
- (void)PPPlayerLoadFinished;
- (void)PPPlayerBuffering;
- (void)PPPlayerBufferFinished;
- (void)PPPlayerPlayBackDidFinished;
- (void)PPPlayerDurationAvailable:(NSTimeInterval)duration;
- (void)PPPlayerPlayBackRateChanged:(NSInteger)state;
- (void)PPPlayerShowOnAirPlay:(BOOL)isActive;
- (void)PPPlayerPlayBackError:(NSError*)err;
- (void)PPPlayerSeekFinish:(NSError*)err;
- (void)PPPlayerLog:(NSString*)msg;
@end

#pragma mark PPPlayerController

@interface PPPlayerController : NSObject

enum {
    PPMOVIE_SYSTEM_PLAYER = 0,
    PPMOVIE_SELF_PLAYER,
    PPMOVIE_AUTO_PLAYER
};

typedef NSInteger PPMoviePlayerType;

+ (id)PPPlayerControllerWithUrl:(NSURL*) url frame:(CGRect)frame type:(PPMoviePlayerType)type;

@property (nonatomic, weak) id<PPPlayerDelegate> delegate;

//播放内容地址
@property(nonatomic, readonly) NSURL *url;

//渲染view
@property(nonatomic, strong) UIView *view;

//片长:单位s
@property(nonatomic, readonly) NSTimeInterval duration;

//是否准备完成
@property(nonatomic, readonly) BOOL isPreparedToPlay;

//当前播放时间
@property(nonatomic) NSTimeInterval currentPlaybackTime;

//缓存最小可以播放时间
@property(nonatomic) NSTimeInterval bufferMinTime;

//视频源尺寸
@property(nonatomic, readonly) CGSize size;

//网络缓存到的位置，单位：秒
@property(nonatomic, readonly) NSTimeInterval bufferSecondPos;

//缓存时长，单位：秒
@property(nonatomic, readonly) NSTimeInterval bufferingTime;

//airplay播放
@property(nonatomic) BOOL allowsAirPlay;

//累计传输的数据总和
@property(nonatomic, readonly) long long numberOfBytesTransfered;

//播放器类型
- (PPMoviePlayerType)playerType;

//主动释放播放器
- (void)playerRelease;

//异步准备
- (void)prepareToPlay;

//开始播放
- (void)play;

//暂停播放
- (void)pause;

//停止播放
- (void)stop;

//切换音轨
- (void)changeAudioChannel:(NSInteger)channel;

- (BOOL)isPlaying;

- (BOOL)isPaused;

- (BOOL)isBuffered;

//截图 time未0时截取当前播放器画面
- (UIImage*)screenShot:(NSTimeInterval)time;

- (void)printLog:(NSString*)msg;
@end
