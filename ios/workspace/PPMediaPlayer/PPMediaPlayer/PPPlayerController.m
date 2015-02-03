//
//  PPPlayerController.m
//  PPMediaPlayer2
//
//  Created by zenzhang on 14-11-21.
//  Copyright (c) 2014年 pplive. All rights reserved.
//

#import "PPPlayerController.h"
#import "PPMediaPlayerController.h"
#import "AvPlayerController.h"

@implementation PPPlayerController

@synthesize delegate = delegate_;

+ (id)PPPlayerControllerWithUrl:(NSURL*)url
                          frame:(CGRect)frame
                           type:(PPMoviePlayerType)type
{
    //system or self
    BOOL canHardDecoding = NO;
    switch (type) {
        case PPMOVIE_SYSTEM_PLAYER:
            canHardDecoding = YES;
            break;
        case PPMOVIE_SELF_PLAYER:
            canHardDecoding = NO;
            break;
        case PPMOVIE_AUTO_PLAYER:
        {
            NSArray *suffixs = @[@".mp4", @".mov", @".m4v", @".m3u8"];
            for (NSString *suf in suffixs) {
                //NSLog(@"%@", url.path);
                NSString *tmp = [url.path lowercaseString];
                if ([tmp hasSuffix:suf]) {
                    canHardDecoding = YES;
                    break;
                }
            }
        }
            break;
        default:
            assert(0);
            break;
    }
    PPPlayerController *player = nil;
    if (canHardDecoding) {
        player = [[AVPlayerController alloc] initWithUrl:url frame:frame];
        NSLog(@"Create AvPlayer");
    } else {
        player = [[PPMediaPlayerController alloc] initWithUrl:url frame:frame];
        NSLog(@"Create FFPlayer");
    }
    return player;
}

- (void)printLog:(NSString*)msg
{
    if (self.delegate
        && [self.delegate respondsToSelector:@selector(PPPlayerLog:)]) {
        [self.delegate performSelector:@selector(PPPlayerLog:)
                            withObject:msg];
    } else {
        NSLog(@"%@", msg);
    }
}

/*
- (id)init
{
    self = [super init];
    if (self) {
        self.isPreparedToPlay = NO;
    }
    return self;
}*/

@end
