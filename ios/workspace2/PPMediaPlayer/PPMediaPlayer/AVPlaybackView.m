//
//  AVPlaybackView.m
//  PPMediaPlayer
//
//  Created by zenzhang on 14-11-27.
//  Copyright (c) 2014年 Stephen Zhang. All rights reserved.
//

#import "AVPlaybackView.h"
#import <AVFoundation/AVFoundation.h>

@implementation AVPlaybackView

+ (Class)layerClass
{
	return [AVPlayerLayer class];
}

- (AVPlayer*)player
{
	return [(AVPlayerLayer*)[self layer] player];
}

- (void)setPlayer:(AVPlayer*)player
{
	[(AVPlayerLayer*)[self layer] setPlayer:player];
}

/* Specifies how the video is displayed within a player layer’s bounds.
 (AVLayerVideoGravityResizeAspect is default) */
- (void)setVideoFillMode:(NSString *)fillMode
{
	AVPlayerLayer *playerLayer = (AVPlayerLayer*)[self layer];
    playerLayer.videoGravity = fillMode;
}

@end
