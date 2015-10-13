//
//  PlaybackViewController.h
//  PPPlayer
//
//  Created by zenzhang on 14-11-26.
//  Copyright (c) 2014年 pplive. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface PlaybackViewController : UIViewController

- (id)initWithUrl:(NSURL*)url;

- (id)initWithUrl:(NSURL*)url player_type:(int)type;
@end
