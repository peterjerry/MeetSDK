//
//  PPPlayerSubtitle.h
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-7-18.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <UIKit/UIKit.h>

@class PPMoviePlayerController;

//云字幕下载成功
#define PPCloudSubtitleSuccessNotification            @"PPCloudSubtitleSuccessNotification"

@interface PPPlayerSubtitle : NSObject

//字幕展示所在的播放器
@property(nonatomic,weak) PPMoviePlayerController *myPlayer;

//展示字幕的控件
@property(nonatomic,strong) UILabel *subtitleView;

//字幕同步定时器
@property(nonatomic,weak) NSTimer *myTimer;

//存放字幕地址的数组
@property(nonatomic,strong) NSMutableArray *subtitleArray;

//字幕类初始化
- (id)initWithPlayer:(PPMoviePlayerController*) nowPlayer;

//手动指定字幕
- (void)subtitleWithAddress:(NSString*) subtitleAddress;

//自动检测可用字幕
- (void)autoDetectSubtitles;

//自动匹配字幕
- (void)autoMatchSubtitle;

//云字幕
- (void)autoCloudSubtitle;

//关闭字幕
- (void)closeSubtitle;

@end
