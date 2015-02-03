//
//  PPMoviePlayerController.m
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-6-28.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "PPCallbackListener.h"
#import "PPMediaPlayerInfo.h"

#define APPLEPLAYER NO  //系统播放器标识
#define SELFPLAYER YES  //自有播放器标识

@interface PPMoviePlayerController ()
{    
    playerListener *myListener;//自有播放器回调监听
    int currentPosition;//当前播放位置
    int currentTime;//切出时记录播放时间
    int audioChannel;//音轨号
    IPlayer *player;//自有播放器
    BOOL choosePlayerFlag;//当前播放器
    BOOL isSeeking;//是否开始seek
}
@property(nonatomic, readwrite) BOOL isPreparedToPlay;
@property(nonatomic, readwrite) double duration;
@end

@implementation PPMoviePlayerController

#pragma mark Init

- (id)init:(enum PPMoviePlayerType)type
       url:(NSURL*)url
{
    if (self = [super init]) {
        BOOL canSystem = YES;
        switch (type) {
            case PPMOVIE_SYSTEM_PLAYER:
                canSystem = YES;
                break;
            case PPMOVIE_SELF_PLAYER:
                canSystem = NO;
                break;
            case PPMOVIE_AUTO_PLAYER:
                canSystem = [PPMoviePlayerController canHardwareDecoding:url];
                break;
            default:
                assert(0);
                break;
        }
        if (canSystem) {
            self.applePlayer = [[MPMoviePlayerController alloc] init];
            choosePlayerFlag = APPLEPLAYER;
            NSLog(@"systemPlayer——————playerVer:%@", playerVersion);
        } else {
            player = getPlayer(NULL);
            choosePlayerFlag = SELFPLAYER;
            NSLog(@"selfPlayer——————playerVer:%@", playerVersion);
        }
        self.contentURL = url;
        CGFloat screenWidth = [[UIScreen mainScreen] bounds].size.width;
        CGFloat screenHeight = [[UIScreen mainScreen] bounds].size.height;
        self.view = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screenHeight, screenWidth)];
        currentPosition = 0;
        currentTime = 0;
        audioChannel = 999;
    }
    return self;
}

- (enum PPMoviePlayerType)playerType
{
    if (self.applePlayer) {
        return PPMOVIE_SYSTEM_PLAYER;
    } else if (player){
        return PPMOVIE_SELF_PLAYER;
    }
    return PPMOVIE_AUTO_PLAYER;
}

- (id)init
{
    choosePlayerFlag = APPLEPLAYER;
    if(self = [super init])
    {
        self.applePlayer = [[MPMoviePlayerController alloc] init];       
        CGFloat screenWidth = [[UIScreen mainScreen] bounds].size.width;
        CGFloat screenHeight = [[UIScreen mainScreen] bounds].size.height;
        self.view = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screenHeight, screenWidth)];
        currentPosition = 0;
        currentTime = 0;
        audioChannel = 999;
    }
    NSLog(@"systemPlayer——————playerVer:%@", playerVersion);
    return self;
}

- (id)initSplayer
{
    choosePlayerFlag = SELFPLAYER;
    if(self = [super init])
    {
        player = getPlayer(NULL);
        CGFloat screenWidth = [[UIScreen mainScreen] bounds].size.width;
        CGFloat screenHeight = [[UIScreen mainScreen] bounds].size.height;
        self.view = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screenHeight, screenWidth)];
        currentPosition = 0;
        currentTime = 0;
        audioChannel = 999;
    }
    NSLog(@"selfPlayer——————playerVer:%@", playerVersion);
    return self;
}

- (void)prepareToPlay
{
    [self addNotifications];
    if(choosePlayerFlag == SELFPLAYER)
    {
        player -> setDataSource([[[self.contentURL absoluteString] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding] UTF8String]);
        player -> setVideoSurface((__bridge void *)self.view);
        myListener = new playerListener();
        player -> setListener(myListener);
        if(audioChannel != 999)player -> selectAudioChannel(audioChannel);
        player -> prepareAsync();
    }
    else
    {
        CGFloat screenWidth = [[UIScreen mainScreen] bounds].size.width;
        CGFloat screenHeight = [[UIScreen mainScreen] bounds].size.height;
        self.applePlayer.view.frame = CGRectMake(0, 0, screenHeight,screenWidth);
        UIView *touchView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screenHeight,screenWidth)];
        touchView.backgroundColor = [UIColor clearColor];
        [self.view insertSubview:self.applePlayer.view atIndex:0];
        [self.view insertSubview:touchView atIndex:1];

        if([[self.contentURL absoluteString] hasPrefix:@"/var"])
        {
            [self.applePlayer setContentURL:[NSURL fileURLWithPath:[self.contentURL absoluteString]]];
        }        
        else
        {
            [self.applePlayer setContentURL:self.contentURL];
        }
        
        [self.applePlayer setControlStyle:MPMovieControlStyleNone];
        [self.applePlayer prepareToPlay];
    }
}

- (void)dealloc
{
    [self removeNotifications];
    
    if(choosePlayerFlag == SELFPLAYER)
    {
        if(player != NULL)
        {
            delete player;
            player = NULL;
        }
        if(myListener != NULL)
        {
            delete myListener;
            myListener = NULL;
        }
    }
    else
    {
        if(self.applePlayer != nil)
        {
            self.applePlayer = nil;
        }
    }
}

- (void)playerRelease
{
    [self removeNotifications];
    
    if(choosePlayerFlag == SELFPLAYER)
    {
        if(player != NULL)
        {
            delete player;
            player = NULL;
        }
        if(myListener != NULL)
        {
            delete myListener;
            myListener = NULL;
        }
    }
    else
    {
        if(self.applePlayer != nil)
        {
            self.applePlayer = nil;
        }
    }
}

#pragma mark Play Control
- (void)play
{
    self.isPreparedToPlay = YES;
    if(choosePlayerFlag == SELFPLAYER) {
        dispatch_async(dispatch_get_main_queue(), ^{
            player -> start();
        });
    } else {
        [self.applePlayer play];
    }
    [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerRestartNotification
                                                        object:nil];
}

- (void)pause
{
    self.isPreparedToPlay = NO;
    if(choosePlayerFlag == SELFPLAYER)  dispatch_async(dispatch_get_main_queue(), ^{player -> pause();});
    else                                [self.applePlayer pause];
    [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerPausedNotification object:nil];
}

- (void)stop
{
    self.isPreparedToPlay = NO;
    if(choosePlayerFlag == SELFPLAYER)  dispatch_async(dispatch_get_main_queue(), ^{player -> stop();});
    else                                [self.applePlayer stop];
}

- (void)setCurrentPlaybackTime:(double)second
{
    if(choosePlayerFlag == SELFPLAYER) {
        dispatch_async(dispatch_get_main_queue(), ^{
            player -> seekTo(1000*(second));
        });
    } else {
        isSeeking = YES;
        self.applePlayer.currentPlaybackTime = second;
    } 
}

- (double)currentPlaybackTime
{
    if(choosePlayerFlag == SELFPLAYER)
    {
        if(player != NULL)player -> getCurrentPosition(&currentPosition);
        return currentPosition/1000.0;
    }
    else
    {
        return self.applePlayer.currentPlaybackTime;
    }
}

- (void)changeAudioChannel:(int)channel
{
    self.isPreparedToPlay = NO;
    audioChannel = channel;
    if(choosePlayerFlag == SELFPLAYER)
    {
        player -> getCurrentPosition(&currentTime);
        if(player != NULL)
        {
            delete player;
            player = NULL;
        }
        if(myListener != NULL)
        {
            delete myListener;
            myListener = NULL;
        }
        player = getPlayer(NULL);
        player -> setDataSource([[self.contentURL absoluteString] UTF8String]);
        player -> setVideoSurface((__bridge void *)self.view);
        myListener = new playerListener();
        player -> setListener(myListener);
        player -> selectAudioChannel(channel);
        player -> prepareAsync();
    }
}

- (void)changeDecoding
{
    self.isPreparedToPlay = NO;
    if(choosePlayerFlag == SELFPLAYER)
    {
        player -> getCurrentPosition(&currentTime);
        currentTime = currentTime/1000;
        if(player != NULL)
        {
            delete player;
            player = NULL;
        }
        if(myListener != NULL)
        {
            delete myListener;
            myListener = NULL;
        }
        choosePlayerFlag = APPLEPLAYER;
        
        self.applePlayer = [[MPMoviePlayerController alloc] init];
        CGFloat screenWidth = [[UIScreen mainScreen] bounds].size.width;
        CGFloat screenHeight = [[UIScreen mainScreen] bounds].size.height;
        self.applePlayer.view.frame = CGRectMake(0, 0, screenHeight,screenWidth);
        UIView *touchView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screenHeight,screenWidth)];
        touchView.backgroundColor = [UIColor clearColor];
        [self.view insertSubview:self.applePlayer.view atIndex:0];
        [self.view insertSubview:touchView atIndex:1];
        
        if([[self.contentURL absoluteString] hasPrefix:@"/var"])
        {
            [self.applePlayer setContentURL:[NSURL fileURLWithPath:[self.contentURL absoluteString]]];
        }
        else
        {
            [self.applePlayer setContentURL:self.contentURL];
        }
        
        [self.applePlayer setControlStyle:MPMovieControlStyleNone];
        [self.applePlayer prepareToPlay];
    }
    else
    {
        currentTime = self.applePlayer.currentPlaybackTime;
        currentTime = currentTime*1000;
        for(UIView *view in [self.view subviews])
        {
            [view removeFromSuperview];
        }
        self.applePlayer = nil;
        choosePlayerFlag = SELFPLAYER;
        
        player = getPlayer(NULL);
        player -> setDataSource([[self.contentURL absoluteString] UTF8String]);
        player -> setVideoSurface((__bridge void *)self.view);
        myListener = new playerListener();
        player -> setListener(myListener);
        player -> prepareAsync();
    }
}

#pragma mark Notifications
- (void)removeNotifications
{
    [[NSNotificationCenter defaultCenter] removeObserver:self name:PPMoviePlayerPreparedNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:PPMoviePlayerSetVideoSizeNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillResignActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidBecomeActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:MPMoviePlayerReadyForDisplayDidChangeNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:MPMoviePlayerPlaybackDidFinishNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:MPMoviePlayerPlaybackStateDidChangeNotification object:nil];
}


- (void)addNotifications
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(applicationWillResignActive) name:UIApplicationWillResignActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(applicationDidBecomeActive) name:UIApplicationDidBecomeActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playerPrepared) name:PPMoviePlayerPreparedNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playerSetVideoSize:) name:PPMoviePlayerSetVideoSizeNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(applePlayerPrepared) name:MPMoviePlayerReadyForDisplayDidChangeNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(applePlayerPlaybackDidFinish:) name:MPMoviePlayerPlaybackDidFinishNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(applePlayerPlaybackStateDidChange) name:MPMoviePlayerPlaybackStateDidChangeNotification object:nil];
}

#pragma mark Handle Notifications
- (void)playerPrepared
{
    self.isPreparedToPlay = YES;
    if(choosePlayerFlag == SELFPLAYER)
    {
        dispatch_async(dispatch_get_main_queue(), ^{ player -> start();});
        int msec = 0;
        player -> getDuration(&msec);
        self.duration = msec/1000.0;
    }
}

- (void)applicationWillResignActive
{
    self.isPreparedToPlay = NO;
}

- (void)applicationDidBecomeActive
{
    
}

#pragma mark Systemplayer Notifications Transmit
//系统播放器消息转发
- (void)applePlayerPrepared
{
    self.duration = self.applePlayer.duration;
    
    [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerPreparedNotification object:nil];
}

- (void)applePlayerPlaybackDidFinish:(NSNotification *) notification
{
    NSDictionary *tmp = notification.userInfo;
	NSNumber *state = [tmp objectForKey:MPMoviePlayerPlaybackDidFinishReasonUserInfoKey];
    NSInteger finishState = [state integerValue];
    if(finishState == MPMovieFinishReasonPlaybackEnded)
    {
        [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerPlaybackCompleteNotification object:nil];
    }
    else if (finishState == MPMovieFinishReasonPlaybackError)
    {
        [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerErrorNotification object:nil];
    }
}

- (void)applePlayerPlaybackStateDidChange
{
    if(isSeeking)
    {
        if(self.applePlayer.playbackState == MPMoviePlaybackStatePlaying)
        {
            isSeeking = NO;
            [[NSNotificationCenter defaultCenter] postNotificationName:PPMoviePlayerSeekCompleteNotification object:nil];
        }
    }
}

+ (BOOL)canHardwareDecoding:(NSURL*)url
{
    NSArray *suffixs = [NSArray arrayWithObjects:@"mp4", @".mov", @".m4v", @".MP4", @".MOV", @".M4V", nil];
    for (NSString *suf in suffixs) {
        if ([url.path hasSuffix:suf]) {
            return YES;
        }
    }
    return NO;
}

@end
