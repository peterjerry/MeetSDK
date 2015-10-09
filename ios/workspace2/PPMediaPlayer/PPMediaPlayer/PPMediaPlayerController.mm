//
//  PPMediaPlayerController.m
//  PPMediaPlayer
//
//  Created by zenzhang on 14-12-4.
//  Copyright (c) 2014年 Stephen Zhang. All rights reserved.
//

#import <AVFoundation/AVFoundation.h>

#import "PPMediaPlayerController.h"
#import "player.h"

#define LOG_FORMAT(msg) [NSString stringWithFormat:@"%s %@", __FUNCTION__, (msg)]

class playerListener;

@implementation PPMediaPlayerController
{
    NSURL *url_;
    IPlayer *player_;//自有播放器
    playerListener *listener_;
    int32_t currentPos_;
    BOOL isPause_;
}


- (id)initWithUrl:(NSURL*)url frame:(CGRect)frame
{
    self = [super init];
    if (self) {
        url_ = [url copy];
        self.view = [[UIView alloc] initWithFrame:frame];
        [self.view setBackgroundColor:[UIColor blackColor]];
        isPause_ = NO;
        self.isBuffered_ = NO;
    }
    return self;
}

- (NSURL *)getUrl
{
    return url_;
}

- (void)prepareToPlay
{
    NSString *str = [NSString stringWithFormat:@"prepareToPlay url:%@", [url_ absoluteString]];
    [self printLog:LOG_FORMAT(str)];
	
    if (player_ == NULL) {
        player_ = getPlayer(NULL);
        if ([url_.scheme isEqualToString:@"file"]) {
            player_->setDataSource([url_.path UTF8String]);
        } else {
            player_->setDataSource([[url_ absoluteString] UTF8String]);
        }
        
        player_->setVideoSurface((__bridge void *)self.view);
        listener_ = new playerListener();
        listener_->playerController_ = self;
        player_->setListener(listener_);
    }
    player_->prepareAsync();
}

- (void)playerRelease
{
    if (player_) {
        [self printLog:LOG_FORMAT(@"playerRelease")];
        player_->stop();
        player_->reset();
        releasePlayer(player_);
        player_ = NULL;
    }
}

- (PPMoviePlayerType)playerType
{
    return PPMOVIE_SELF_PLAYER;
}

- (void)play
{
    [self printLog:LOG_FORMAT(@"play")];
    isPause_ = NO;
    player_->start();
}

- (void)pause
{
    [self printLog:LOG_FORMAT(@"pause")];
    isPause_ = YES;
    player_->pause();
}

//停止播放
- (void)stop
{
    [self printLog:LOG_FORMAT(@"stop")];
    isPause_ = NO;
    player_->stop();
}

- (void)setCurrentPlaybackTime:(NSTimeInterval)second
{
    player_->seekTo(second * 1000);
}

//获得当前播放时间 sec
- (NSTimeInterval)currentPlaybackTime
{
    player_->getCurrentPosition(&currentPos_);
    return currentPos_ / 1000.0;
}

//获得媒体时长 sec
- (NSTimeInterval)duration
{
    int32_t msec = 0;
    player_->getDuration(&msec);
    return msec / 1000.0;
}

- (BOOL)isPlaying
{
    return player_->isPlaying() ? YES : NO;
}

- (BOOL)isPaused
{
    return isPause_;
}

//切换音轨
- (void)changeAudioChannel:(NSInteger)channel
{
    player_->selectAudioChannel(channel);
}

- (void)setAllowsAirPlay:(BOOL)allowsAirPlay
{
    return;
    assert(0 && @"not support");
}

- (BOOL)allowsAirPlay
{
    return NO;
}

- (BOOL)isBuffered
{
    return self.isBuffered_;
}

- (CGSize)size
{
    return listener_->size_;
}

- (UIImage*)screenShot:(NSTimeInterval)time
{
    NSString *str = [NSString stringWithFormat:@"screenShot second pos:%f", time];
    [self printLog:LOG_FORMAT(str)];
    SnapShot *sShot = player_->getSnapShot(0, 0, 0, -1);
    if (sShot == nil)
        return nil;
    
    int width = self.size.width;
    int height = self.size.height;
    CGContextRef bitmapContext = CGBitmapContextCreate(sShot->picture_data
                                                       ,width
                                                       ,height
                                                       ,8
                                                       ,4*(sShot->stride)
                                                       ,CGColorSpaceCreateDeviceRGB()
                                                       ,kCGImageAlphaPremultipliedLast);
    
    CGImageRef imageRef = CGBitmapContextCreateImage(bitmapContext);
    UIImage *image = [[UIImage alloc] initWithCGImage:imageRef];
    
    CGImageRelease(imageRef);
    CGContextRelease(bitmapContext);
    return image;
}

- (long long)numberOfBytesTransfered
{
    int64_t num = 0;
    player_->getProcessBytes(&num);
    return num;
}

- (NSTimeInterval)bufferingTime
{
    int msec;
    player_->getBufferingTime(&msec);
    return (NSTimeInterval)msec;
}

#pragma mark callback class for player

class playerListener : public MediaPlayerListener
{
public:
    void notify(int msg, int ext1, int ext2);
    PPMediaPlayerController *playerController_;
    CGSize size_;
    
};

void playerListener::notify(int msg, int ext1, int ext2)
{
    if (playerController_.delegate==nil) {
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
    switch (msg) {
        case MEDIA_ERROR:
        {
            NSError *error = [NSError errorWithDomain:@"Player error" code:ext1 userInfo:nil];
            if ([playerController_ isPlaying]) {
                [playerController_ printLog:LOG_FORMAT(@"PPPlayerPlayBackError")];
                if ([playerController_.delegate respondsToSelector:@selector(PPPlayerPlayBackError:)]) {
                    [playerController_.delegate PPPlayerPlayBackError:error];
                }
            } else {
                [playerController_ printLog:LOG_FORMAT(@"PPPlayerPlayBackError")];
                if ([playerController_.delegate respondsToSelector:@selector(PPPlayerLoadFailed:)]) {
                    //create error
//                    NSString *localizedDescription = NSLocalizedString(@"Media cannot be played", @"Media cannot be played description");
//                    NSString *localizedFailureReason = NSLocalizedString(@"The assets tracks were loaded, but could not be made playable.", @"Item cannot be played failure reason");
//                    NSDictionary *errorDict = [NSDictionary dictionaryWithObjectsAndKeys:
//                                               localizedDescription, NSLocalizedDescriptionKey,
//                                               localizedFailureReason, NSLocalizedFailureReasonErrorKey,
//                                               nil];
//                    NSError *assetCannotBePlayedError = [NSError errorWithDomain:@"StitchedStreamPlayer" code:0 userInfo:errorDict];
                    [playerController_.delegate PPPlayerLoadFailed:error];
                }
            }
            
        }
            break;
        case MEDIA_SET_VIDEO_SIZE:
        {
            NSString *str = [NSString stringWithFormat:@"MEDIA_SET_VIDEO_SIZE:%d %d", ext1, ext2];
            [playerController_ printLog:LOG_FORMAT(str)];
            size_.width = ext1;
            size_.height = ext2;
        }
            break;
        case MEDIA_PREPARED:
        {
            NSString *str = [NSString stringWithFormat:@"MEDIA_PREPARED duration: %f", playerController_.duration];
            [playerController_ printLog:LOG_FORMAT(str)];
            if ([playerController_.delegate respondsToSelector:@selector(PPPlayerDurationAvailable:)]) {
                float duration = playerController_.duration;
                [playerController_.delegate PPPlayerDurationAvailable:duration];
            }
            if ([playerController_.delegate respondsToSelector:@selector(PPPlayerLoadFinished)]) {
                [playerController_.delegate PPPlayerLoadFinished];
            }
        }
            break;
        case MEDIA_PLAYBACK_COMPLETE:
        {
            [playerController_ printLog:LOG_FORMAT(@"PPPlayerPlayBackDidFinished")];
            if ([playerController_.delegate respondsToSelector:@selector(PPPlayerPlayBackDidFinished)]) {
                [playerController_.delegate PPPlayerPlayBackDidFinished];
            }
            
        }
            break;
        case MEDIA_SEEK_COMPLETE:
        {
            [playerController_ printLog:LOG_FORMAT(@"PPPlayerSeekFinish")];
            if ([playerController_.delegate respondsToSelector:@selector(PPPlayerSeekFinish:)]) {
                [playerController_.delegate PPPlayerSeekFinish:nil];
            }
        }
            break;
        case MEDIA_BUFFERING_UPDATE:
        {
        }
            break;
        
        case MEDIA_INFO:
        {
            switch (ext1) {
                case MEDIA_INFO_BUFFERING_START:
                {
                    [playerController_ printLog:LOG_FORMAT(@"PPPlayerBuffering")];
                    playerController_.isBuffered_ = NO;
                    if ([playerController_.delegate respondsToSelector:@selector(PPPlayerBuffering)]) {
                        [playerController_.delegate PPPlayerBuffering];
                    }
                }
                    break;
                case MEDIA_INFO_BUFFERING_END:
                {
                    [playerController_ printLog:LOG_FORMAT(@"PPPlayerBufferFinished")];
                    playerController_.isBuffered_ = YES;
                    if ([playerController_.delegate respondsToSelector:@selector(PPPlayerBufferFinished)]) {
                        [playerController_.delegate PPPlayerBufferFinished];
                    }
                }
                    break;
                    
                default:
                    break;
            }
        }
            break;
        default:
            break;
    }
    });
}
@end
