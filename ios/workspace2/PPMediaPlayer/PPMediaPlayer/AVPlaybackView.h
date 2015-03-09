//
//  AVPlaybackView.h
//  PPMediaPlayer
//
//  Created by zenzhang on 14-11-27.
//  Copyright (c) 2014年 Stephen Zhang. All rights reserved.
//

#import <UIKit/UIKit.h>

@class AVPlayer;

@interface AVPlaybackView : UIView

@property (nonatomic, strong) AVPlayer* player;

- (void)setPlayer:(AVPlayer*)player;
- (void)setVideoFillMode:(NSString *)fillMode;

@end
