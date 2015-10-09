//
//  PPPlayerController.m
//  PPMediaPlayer2
//
//  Created by zenzhang on 14-11-21.
//  Copyright (c) 2014年 pplive. All rights reserved.
//

#import "PPPlayerController.h"
#import "PPMediaPlayerController.h"
#import "SysPlayerController.h"
#import "PPMediaPlayerInfo.h"



@implementation PPPlayerController

@synthesize delegate = delegate_;

+ (id)PPPlayerControllerWithUrl:(NSURL*)url
                          frame:(CGRect)frame
                           type:(PPMoviePlayerType)type
{
    //system or self
    PPPlayerController *player = nil;
    switch (type) {
        case PPMOVIE_SYSTEM_PLAYER:
            player = [[SysPlayerController alloc] initWithUrl:url frame:frame];
            NSLog(@"Create AvPlayer");
            break;
        case PPMOVIE_SELF_PLAYER:
            player = [[PPMediaPlayerController alloc] initWithUrl:url frame:frame];
            NSLog(@"Create FFPlayer");
            break;
        case PPMOVIE_AUTO_PLAYER:
        {
            BOOL canHardDecoding = NO;
            //check name
            NSArray *suffixs = @[@".mp4", @".mov", @".m4v", @".3gp", @".m3u8"];
            for (NSString *suf in suffixs) {
                NSString *tmp = [url.path lowercaseString];
                if ([tmp hasSuffix:suf]) {
                    canHardDecoding = YES;
                    break;
                }
            }
            if (canHardDecoding
                && url.isFileURL) { //check codec type
                NSLog(@"check suffixs pass");
                NSString *path = url.path;
                PPMediaInfo info = [[PPMediaPlayerInfo sharedInstance]
                                    getMediaInfo:path];
                NSString *videoType = [NSString stringWithUTF8String:info.video_name];
                NSString *audioType = [NSString stringWithUTF8String:info.audio_name];
                canHardDecoding = [PPPlayerController videoSupport:videoType]
                && [PPPlayerController audioSupport:audioType];
            }
            if (canHardDecoding) {
                player = [[SysPlayerController alloc] initWithUrl:url frame:frame];
                NSLog(@"Create AvPlayer");
            } else {
                player = [[PPMediaPlayerController alloc] initWithUrl:url frame:frame];
                NSLog(@"Create FFPlayer");
            }
        }
            break;
        default:
            assert(0);
            break;
    }
    return player;
}

+ (BOOL)videoSupport:(NSString*)videoCodec
{
    NSArray *supporTypes = @[@"h264"];
    if ([supporTypes containsObject:[videoCodec lowercaseString]]) {
        return YES;
    }
    return NO;
}

+ (BOOL)audioSupport:(NSString*)audioCodec
{
    NSArray *supporTypes = @[@"aac"];
    if ([supporTypes containsObject:[audioCodec lowercaseString]]) {
        return YES;
    }
    return NO;
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
