//
//  AvPlayerContoller.m
//  PPMediaPlayer2
//
//  Created by zenzhang on 14-11-21.
//  Copyright (c) 2014年 pplive. All rights reserved.
//

#import <AVFoundation/AVFoundation.h>

#import "AVPlayerController.h"
#import "AVPlaybackView.h"

#define BOOL2MSG(bret) bret ? @"YES" : @"NO"

#define BUFFER_MIN_SECONDS 4

#define LOG_FORMAT(msg) [NSString stringWithFormat:@"%s %@", __FUNCTION__, (msg)]

//player context
static void *PPPlayerCurrentItemObservationContext  = &PPPlayerCurrentItemObservationContext;
static void *PPPlayerRateObservationContext         = &PPPlayerRateObservationContext;
static void *PPPlayerAirPlayOutputContext           = &PPPlayerAirPlayOutputContext;

//player item context
static void *PPPlayerItemStatusObserverContext          = &PPPlayerItemStatusObserverContext;
static void *PPPlayerItemBufferEmptyObserverContext     = &PPPlayerItemBufferEmptyObserverContext;
static void *PPPlaybackLikelyToKeepUpObserverContext    = &PPPlaybackLikelyToKeepUpObserverContext;
static void *PPPlaybackLoadedTimeRangesContext          = &PPPlaybackLoadedTimeRangesContext;
static void *PPPlayerItemDurationObservationContext     = &PPPlayerItemDurationObservationContext;

//player key value
NSString *kCurrentItemKey	= @"currentItem";
NSString *kVideoDuration    = @"duration";
NSString *kRateKey			= @"rate";
NSString *kAirPlayOutput    = @"";

//player item ke value
NSString *kStatusKey		= @"status";
NSString *kBufferEmpty      = @"playbackBufferEmpty";
NSString *kPlayToKeepUP     = @"playbackLikelyToKeepUp";
NSString *kLoadedTimeRanges = @"loadedTimeRanges";


NSString *kTracksKey        = @"tracks";
NSString *kPlayableKey		= @"playable";

@implementation AVPlayerController
{
    AVPlayer *avPlayer_;
    AVPlayerItem *currentItem_;
    NSURL *url_;
    AVURLAsset *asset_;
    BOOL isWaitForBuffer_;
    //BOOL isStop_;
}

- (id)initWithUrl:(NSURL *)url
            frame:(CGRect)frame
{
    self = [super init];
    if (self) {
        url_ = [url copy];
        self.view = [[AVPlaybackView alloc] initWithFrame:frame];
        [self.view setBackgroundColor:[UIColor blackColor]];
        
        float iv = [[[UIDevice currentDevice] systemVersion] floatValue];
        if (iv >= 6.0) {
            kAirPlayOutput = @"externalPlaybackActive";
        } else {
            kAirPlayOutput = @"airPlayVideoActive";
        }
        
        //init value
        self.bufferMinTime = BUFFER_MIN_SECONDS;
        isWaitForBuffer_ = NO;
    }
    NSString *msg = [NSString stringWithFormat:@"create player url:%@ size:%@",
                     [url_ absoluteString],
                     NSStringFromCGSize(frame.size)];
    [self printLog:LOG_FORMAT(msg)];
    return self;
}

- (NSURL*)getUrl
{
    return url_;
}

- (NSString*)mediaDescription
{
    NSString *infoStr = nil;
    if (currentItem_) {
        for (AVPlayerItemTrack *itemTrack in currentItem_.tracks) {
            AVAssetTrack *track = itemTrack.assetTrack;
            NSString *str = [NSString stringWithFormat:@"track%d (type:%@, playable: %d)",
                track.trackID, track.mediaType, track.playable];
            if (infoStr == nil) {
                infoStr = str;
            } else {
                infoStr = [infoStr stringByAppendingString:[NSString stringWithFormat:@"\t%@", str]];
            }
        }
    }    
    return infoStr;
}

//主动释放播放器
- (void)playerRelease
{
    [self stop];
    asset_ = nil;
    avPlayer_ = nil;
}

- (void)removePlayerObservers
{
    [avPlayer_ removeObserver:self forKeyPath:kCurrentItemKey];
    //[avPlayer_ removeObserver:self forKeyPath:kVideoDuration];
    //[avPlayer_ removeObserver:self forKeyPath:kRateKey];
    [avPlayer_ removeObserver:self forKeyPath:kAirPlayOutput];
}

- (void)removePlayerItemObservers
{
    [currentItem_ removeObserver:self forKeyPath:kStatusKey];
    [currentItem_ removeObserver:self forKeyPath:kLoadedTimeRanges];
    [currentItem_ removeObserver:self forKeyPath:kVideoDuration];
    //[currentItem_ removeObserver:self forKeyPath:kBufferEmpty];
    //[currentItem_ removeObserver:self forKeyPath:kPlayToKeepUP];
}

- (void)addPlayerObservers
{
    if (avPlayer_) {
        /* Observe the AVPlayer "currentItem" property to find out when any
         AVPlayer replaceCurrentItemWithPlayerItem: replacement will/did
         occur.*/
        [self printLog:LOG_FORMAT(@"add player item observers")];
        [avPlayer_ addObserver:self
                    forKeyPath:kCurrentItemKey
                       options:NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew
                       context:PPPlayerCurrentItemObservationContext];
        
        [avPlayer_ addObserver:self
                    forKeyPath:kAirPlayOutput
                       options:NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew
                       context:PPPlayerAirPlayOutputContext];
    }
}

- (void)addPlayerItemObservers
{
    if (currentItem_) {
        [self printLog:LOG_FORMAT(@"add player item observers")];
        /* Observe the player item "status" key to determine when it is ready to play. */
        [currentItem_ addObserver:self
                       forKeyPath:kStatusKey
                          options:NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew
                          context:PPPlayerItemStatusObserverContext];
        
        [currentItem_ addObserver:self
                       forKeyPath:kVideoDuration
                          options:NSKeyValueObservingOptionNew
                          context:PPPlayerItemDurationObservationContext];
        /*
         [currentItem_ addObserver:self
         forKeyPath:kBufferEmpty
         options:NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew
         context:PPPlayerItemBufferEmptyObserverContext];
         
         [currentItem_ addObserver:self
         forKeyPath:kPlayToKeepUP
         options:NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew
         context:PPPlaybackLikelyToKeepUpObserverContext];*/
        
        [currentItem_ addObserver:self
                       forKeyPath:kLoadedTimeRanges
                          options:NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew
                          context:PPPlaybackLoadedTimeRangesContext];
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(playbackToEnd)
                                                     name:AVPlayerItemDidPlayToEndTimeNotification
                                                   object:currentItem_];
        
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(newErrorLog)
                                                     name:AVPlayerItemNewErrorLogEntryNotification
                                                   object:currentItem_];
        /*
         [[NSNotificationCenter defaultCenter] addObserver:self
         selector:@selector(newAccessLog)
         name:AVPlayerItemNewAccessLogEntryNotification
         object:currentItem_];*/
        
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(playbackStall)
                                                     name:AVPlayerItemPlaybackStalledNotification
                                                   object:currentItem_];
    }
}

//影片播放结束
- (void)playbackToEnd
{
    [self printLog:LOG_FORMAT(@"movie play finish")];
    if (self.delegate
        && [self.delegate respondsToSelector:@selector(PPPlayerPlayBackDidFinished)]) {
        [self.delegate PPPlayerPlayBackDidFinished];
    }
}

//异步准备
- (void)prepareToPlay
{
    [self printLog:LOG_FORMAT(@"prepareToPlay")];
    asset_ = [AVURLAsset URLAssetWithURL:url_ options:nil];
    
    NSArray *requestedKeys = @[@"playable"];
    
    /* Tells the asset to load the values of any of the specified keys that are not already loaded. */
    [asset_ loadValuesAsynchronouslyForKeys:requestedKeys completionHandler:^{
        dispatch_async( dispatch_get_main_queue(), ^{
            /* IMPORTANT: Must dispatch to main queue in order to operate on the AVPlayer and AVPlayerItem. */
            [self prepareToPlayAsset:asset_ withKeys:requestedKeys];
            asset_ = nil;
        });
    }];
}

- (void)prepareToPlayAsset:(AVURLAsset *)asset withKeys:(NSArray *)requestedKeys
{
    /* Make sure that the value of each key has loaded successfully. */
	for (NSString *thisKey in requestedKeys) {
		NSError *error = nil;
		AVKeyValueStatus keyStatus = [asset statusOfValueForKey:thisKey
                                                          error:&error];
		if (keyStatus == AVKeyValueStatusFailed) {
            NSString *msg = [NSString stringWithFormat:@"load failure:%@", error];
            [self printLog:LOG_FORMAT(msg)];
            if (self.delegate
                && [self.delegate respondsToSelector:@selector(PPPlayerLoadFailed:)]) {
                [self.delegate PPPlayerLoadFailed:error];
            }
			return;
        } else if (keyStatus == AVKeyValueStatusCancelled) {    //user cancel
            [self printLog:LOG_FORMAT(@"User cancel loading")];
            return;
        }
		/* If you are also implementing -[AVAsset cancelLoading], add your code here to bail out properly in the case of cancellation. */
	}
    
    /* Use the AVAsset playable property to detect whether the asset can be played. */
    if (!asset.playable) {
        /* Generate an error describing the failure. */
		NSString *localizedDescription = NSLocalizedString(@"Item cannot be played", @"Item cannot be played description");
		NSString *localizedFailureReason = NSLocalizedString(@"The assets tracks were loaded, but could not be made playable.", @"Item cannot be played failure reason");
		NSDictionary *errorDict = [NSDictionary dictionaryWithObjectsAndKeys:
								   localizedDescription, NSLocalizedDescriptionKey,
								   localizedFailureReason, NSLocalizedFailureReasonErrorKey,
								   nil];
		NSError *assetCannotBePlayedError = [NSError errorWithDomain:@"StitchedStreamPlayer" code:0 userInfo:errorDict];
        if (self.delegate
            && [self.delegate respondsToSelector:@selector(PPPlayerLoadFailed:)]) {
            [self.delegate PPPlayerLoadFailed:assetCannotBePlayedError];
        }
        return;
    }
    
    /* Create a new instance of AVPlayerItem from the now successfully loaded AVAsset. */
    currentItem_ = [AVPlayerItem playerItemWithAsset:asset];
    
    /* Observe the player item "status" key to determine when it is ready to play. */
    [self addPlayerItemObservers];
	
    /* Create new player, if we don't already have one. */
    if (avPlayer_ == nil) {
        /* Get a new AVPlayer initialized to play the specified player item. */
        avPlayer_ = [AVPlayer playerWithPlayerItem:currentItem_];
        [self addPlayerObservers];
    } else {
        assert(0);
    }
    
    /* Make our new AVPlayerItem the AVPlayer's current item. */
    if (avPlayer_.currentItem != currentItem_) {
        /* Replace the player item with a new player item. The item replacement occurs
         asynchronously; observe the currentItem property to find out when the
         replacement will/did occur
		 
		 If needed, configure player item here (example: adding outputs, setting text style rules,
		 selecting media options) before associating it with a player
		 */
        [avPlayer_ replaceCurrentItemWithPlayerItem:currentItem_];
        //[self addPlayerObservers];
    }
}

//开始播放
- (void)play
{
    if ([self isPlaying]) {
        return;
    }
    [self printLog:LOG_FORMAT(@"play")];
    [avPlayer_ play];
}

//暂停播放
- (void)pause
{
    if ([self isPaused]) {
        return;
    }
    [self printLog:LOG_FORMAT(@"pause")];
    [avPlayer_ pause];
}

//停止播放
- (void)stop
{
    if (currentItem_) {
        [self printLog:LOG_FORMAT(@"stop")];
        [avPlayer_ pause];
        [self removePlayerObservers];
        [self printLog:LOG_FORMAT(@"remove player observers.")];
        [self removePlayerItemObservers];
        [self printLog:LOG_FORMAT(@"remove player item observers.")];
        [[NSNotificationCenter defaultCenter] removeObserver:self];
        [avPlayer_ replaceCurrentItemWithPlayerItem:nil];
        currentItem_ = nil;
    } else if (asset_) {
        [self printLog:LOG_FORMAT(@"cancel loading")];
        [asset_ cancelLoading];
    }
}

- (NSTimeInterval)bufferingTime
{
    return self.bufferSecondPos - self.currentPlaybackTime;
}

//拖动到第second秒
- (void)setCurrentPlaybackTime:(NSTimeInterval)second
{
    [self pause];
    NSString *str = [NSString stringWithFormat:@"seek to %f", second];
    [self printLog:LOG_FORMAT(str)];
    [avPlayer_ seekToTime:CMTimeMakeWithSeconds(second, NSEC_PER_SEC)
        completionHandler:^(BOOL finish){
            if (finish) {
                //通知seek finish
                if (self.delegate
                    && [self.delegate respondsToSelector:@selector(PPPlayerSeekFinish:)]) {
                    [self.delegate PPPlayerSeekFinish:nil];
                }
                if ([self bufferingTime] >= self.bufferMinTime
                    || self.bufferSecondPos == self.duration) {
                    [self play];
                } else { //可播放时间不够
                    if (self.delegate
                        && [self.delegate respondsToSelector:@selector(PPPlayerBuffering)]) {
                        [self.delegate PPPlayerBuffering];
                    }
                    isWaitForBuffer_ = YES;
                }
            } else {
                if (self.delegate
                    && [self.delegate respondsToSelector:@selector(PPPlayerSeekFinish:)]) {
                    NSError *err = [NSError errorWithDomain:@"unknow" code:-1 userInfo:nil];
                    [self.delegate PPPlayerSeekFinish:err];
                }
            }
        }];
}

//获得当前播放时间
- (NSTimeInterval)currentPlaybackTime
{
    if (currentItem_) {
        return CMTimeGetSeconds([currentItem_ currentTime]);
    }
    return 0;
}

- (NSTimeInterval)duration
{
    if (currentItem_) {
        return CMTimeGetSeconds([currentItem_ duration]);
    }
    return 0;
}

- (BOOL)isPlaying
{
    static float PLAYING_RATE_VALUE = 1.0;
    float rate = avPlayer_.rate;
    return rate == PLAYING_RATE_VALUE;
}

- (BOOL)isPaused
{
    static float PAUSE_RATE_VALUE = 0.0;
    float rate = avPlayer_.rate;
    return rate == PAUSE_RATE_VALUE;
}

- (CGSize)size
{
    NSArray *arr = [avPlayer_ currentItem].tracks;
    CGSize size;
    for (AVPlayerItemTrack *track in arr) {
        if ([track.assetTrack.mediaType isEqualToString:@"vide"]) {
            size = track.assetTrack.naturalSize;
            break;
        }
    }
    return size;
}

- (long long)numberOfBytesTransfered
{
    AVPlayerItemAccessLogEvent *logEvent = [avPlayer_.currentItem.accessLog.events lastObject];
    if (logEvent) {
        return logEvent.numberOfBytesTransferred;
    } else {
        return 0;
    }
}

- (PPMoviePlayerType)playerType
{
    return PPMOVIE_SYSTEM_PLAYER;
}

//切换音轨
- (void)changeAudioChannel:(NSInteger)channel
{
    [self printLog:LOG_FORMAT(@"not support")];
    assert(0 && @"not support");
}

- (void)setAllowsAirPlay:(BOOL)allowsAirPlay
{
    if ([[[UIDevice currentDevice] systemVersion] floatValue]>=6.0) {
        avPlayer_.allowsExternalPlayback = allowsAirPlay;
    } else {
        avPlayer_.allowsAirPlayVideo = allowsAirPlay;
    }
}

- (BOOL)allowsAirPlay
{
    if ([[[UIDevice currentDevice] systemVersion] floatValue]>=6.0) {
        return avPlayer_.allowsExternalPlayback;
    }
    return avPlayer_.allowsAirPlayVideo;
}

- (BOOL)isBuffered
{
    return avPlayer_.currentItem.playbackLikelyToKeepUp;
}

- (NSTimeInterval)bufferSecondPos
{
    if ([currentItem_.loadedTimeRanges count] > 0) {
        CMTimeRange range = [[currentItem_.loadedTimeRanges objectAtIndex:0] CMTimeRangeValue];
        return CMTimeGetSeconds(CMTimeAdd(range.start, range.duration));
    } else {
        return self.currentPlaybackTime;
    }
}

- (UIImage*)screenShot:(NSTimeInterval)time
{
    CMTime actualTime;
    NSError *error;
    
    AVAssetImageGenerator *generator = [[AVAssetImageGenerator alloc] initWithAsset:avPlayer_.currentItem.asset];
    
    // Setting a maximum size is not necessary for this code to
    // successfully get a screenshot, but it was useful for my project.
    generator.maximumSize = self.size;
    
    CGImageRef cgIm = [generator copyCGImageAtTime:avPlayer_.currentTime
                                        actualTime:&actualTime
                                             error:&error];
    if (nil != error) {
        NSString *msg =
        [NSString stringWithFormat:@"screenshot error: %@", [error localizedDescription]];
        [self printLog:LOG_FORMAT(msg)];
        return nil;
    }
    UIImage *image = [UIImage imageWithCGImage:cgIm];
    CFRelease(cgIm);
    return image;
}

-(void) newErrorLog
{
    NSString *errStr = [NSString stringWithFormat:@"get error %@", currentItem_.error.description];
    [self printLog:LOG_FORMAT(errStr)];
}


-(void) newAccessLog
{
    NSString *str = [NSString stringWithFormat:@"info: %@", currentItem_.accessLog.description];
    [self printLog:LOG_FORMAT(str)];
}


-(void) playbackStall
{
    isWaitForBuffer_ = YES;
    [self printLog:LOG_FORMAT(@"start buffering")];
    if (self.delegate
        && [self.delegate respondsToSelector:@selector(PPPlayerBuffering)]) {
        [self.delegate PPPlayerBuffering];
    }
}

#pragma mark Key Value Observer for player rate, currentItem, player item status
/* ---------------------------------------------------------
 **  Called when the value at the specified key path relative
 **  to the given object has changed.
 **  Adjust the movie play and pause button controls when the
 **  player item "status" value changes. Update the movie
 **  scrubber control when the player item is ready to play.
 **  Adjust the movie scrubber control when the player item
 **  "rate" value changes. For updates of the player
 **  "currentItem" property, set the AVPlayer for which the
 **  player layer displays visual output.
 **  NOTE: this method is invoked on the main queue.
 ** ------------------------------------------------------- */

- (void)observeValueForKeyPath:(NSString*)path
                      ofObject:(id)object
                        change:(NSDictionary*)change
                       context:(void*)context
{
    id value = [change objectForKey:NSKeyValueChangeNewKey];
    if (avPlayer_ == object) {      //player key
        if (context == PPPlayerCurrentItemObservationContext) {
            
        }  else if (context == PPPlayerRateObservationContext) {
            float rate = [value floatValue];
            NSString *str = [NSString stringWithFormat:@"play rate change:%f", rate];
            [self printLog:LOG_FORMAT(str)];
            if (self.delegate
                && [self.delegate respondsToSelector:@selector(PPPlayerPlayBackRateChanged:)]) {
                [self.delegate PPPlayerPlayBackRateChanged:(NSInteger)rate];
            }
        } else if (context == PPPlayerAirPlayOutputContext) {
            AVPlaybackView *playbackView = (AVPlaybackView*)self.view ;
            [playbackView setPlayer:avPlayer_];
            [playbackView setVideoFillMode:AVLayerVideoGravityResizeAspect];
        } else {
            assert(0);
        }
    } else if (currentItem_ == object){     //player item key
        if (avPlayer_.currentItem != currentItem_) {
            //NSLog(@"[AVPlayerController] [observeValueForKeyPath] player's currentitem err!");
            return;
        }
        if (context == PPPlayerItemStatusObserverContext) {
            AVPlayerItemStatus status = [value integerValue];
            switch (status) {
                    /* Indicates that the status of the player is not yet known because
                     it has not tried to load new media resources for playback */
                case AVPlayerItemStatusUnknown:
                {
                    //assert(0 && @"AVPlayerItemStatusUnknown");
                    [self printLog:LOG_FORMAT(@"AVPlayerStatusUnknown")];
                }
                    break;
                case AVPlayerItemStatusReadyToPlay:
                {
                    //打印媒体信息
                    NSString *mediaDesStr = [self mediaDescription];
                    [self printLog:LOG_FORMAT(mediaDesStr)];
                    if (self.delegate
                        && [self.delegate respondsToSelector:@selector(PPPlayerLoadFinished)]) {
                        [self.delegate PPPlayerLoadFinished];
                    }
                    //启动播放
                    if (!self.isPlaying) {
                        [self play];
                    }
                    [self printLog:LOG_FORMAT(@"load finish")];
                }
                    break;
                case AVPlayerItemStatusFailed:
                {
                    AVPlayerItem *thePlayerItem = (AVPlayerItem *)object;
                    NSString *str = [NSString stringWithFormat:@"load movie err:%@", thePlayerItem.error];
                    [self printLog:LOG_FORMAT(str)];
                    if (self.delegate
                        && [self.delegate respondsToSelector:@selector(PPPlayerPlayBackError:)]) {
                        [self.delegate PPPlayerPlayBackError:thePlayerItem.error];
                    }
                }
                    break;
            }
        } else if (context == PPPlayerItemDurationObservationContext) {
            if ((NSNull*)value == [NSNull null])
                return;
            CMTime duration = [value CMTimeValue];
            Float64 interval = CMTimeGetSeconds(duration);
            if (isnan(interval)) {
                return;
            }
            NSString *str = [NSString stringWithFormat:@"get duration:%f s", interval];
            [self printLog:LOG_FORMAT(str)];
            if (self.delegate
                && [self.delegate respondsToSelector:@selector(PPPlayerDurationAvailable:)]) {
                [self.delegate PPPlayerDurationAvailable:interval];
            }
        } else if (context == PPPlayerItemBufferEmptyObserverContext) {
            if (avPlayer_.currentItem.playbackBufferEmpty) {
                /*
                if (self.delegate
                    && [self.delegate respondsToSelector:@selector(PPPlayerBuffering:)]) {
                    [self.delegate PPPlayerBuffering];
                }*/
            } else {
            }
        } else if (context == PPPlaybackLikelyToKeepUpObserverContext) {
            if (avPlayer_.currentItem.playbackLikelyToKeepUp) {
//                if (self.delegate
//                    && [self.delegate respondsToSelector:@selector(PPPlayerBufferFinished:)]) {
//                    [self.delegate PPPlayerLoadFinished];
//                    //[self.delegate performSelector:@selector(PPPlayerBufferFinished)];
//                }
            } else {
            }
        } else if (context == PPPlaybackLoadedTimeRangesContext) {
            if (!isWaitForBuffer_) {
                return;
            }
            //当缓存数据大小大于特定值时
            if ( [self bufferingTime] >= self.bufferMinTime
                || self.bufferSecondPos == self.duration) {
                isWaitForBuffer_ = NO;
                [self printLog:LOG_FORMAT(@"buffer finish")];
                if (self.delegate
                    && [self.delegate respondsToSelector:@selector(PPPlayerBufferFinished)]) {
                    [self.delegate PPPlayerBufferFinished];
                }
            }
        } else {
            assert(0);
        }
    } else {
        assert(0);
        [super observeValueForKeyPath:path ofObject:object change:change context:context];
    }
    return;
}

@end
