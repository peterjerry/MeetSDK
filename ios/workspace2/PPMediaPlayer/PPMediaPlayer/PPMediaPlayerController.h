//
//  PPMediaPlayerController.h
//  PPMediaPlayer
//
//  Created by zenzhang on 14-12-4.
//  Copyright (c) 2014年 Stephen Zhang. All rights reserved.
//

#import "PPPlayerController.h"

@interface PPMediaPlayerController : PPPlayerController

- (id)initWithUrl:(NSURL*)url frame:(CGRect)frame;

@property(nonatomic) BOOL isBuffered_;

@end
