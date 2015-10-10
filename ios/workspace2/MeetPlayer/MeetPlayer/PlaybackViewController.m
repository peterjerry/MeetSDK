//
//  PlaybackViewController.m
//  PPPlayer
//
//  Created by zenzhang on 14-11-26.
//  Copyright (c) 2014年 pplive. All rights reserved.
//

#import "PlaybackViewController.h"
#import "MTLog/MTLog.h"
#import "PPMediaPlayer/PPMediaPlayer.h"
#import <MediaPlayer/MediaPlayer.h>

#define PLAYER_TYPE_NAME @[@"PPMOVIE_SYSTEM_PLAYER", @"PPMOVIE_SELF_PLAYER", @"PPMOVIE_AUTO_PLAYER"];

@interface PlaybackViewController () <PPPlayerDelegate>
{
    PPPlayerController *player_;
    NSURL *url_;
    NSTimer *timer_;
    MPVolumeView *airPlayBtn_;
    __weak IBOutlet UISlider *scrubber_;
    __weak IBOutlet UILabel *durationLab_;
    __weak IBOutlet UILabel *playbackTimeLab_;
    __weak IBOutlet UIButton *pauseBtn_;
    __weak IBOutlet UIButton *playBtn_;
    __weak IBOutlet UILabel *messageLab_;
    __weak IBOutlet UILabel *titleLab_;
}

@property (nonatomic, strong) UIButton  *mpbutton;
@end

@implementation PlaybackViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    if ([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        self = [super initWithNibName:@"PlaybackViewController_ipad" bundle:nil];
    } else {
        self = [super initWithNibName:@"PlaybackViewController" bundle:nil];
    }
    if (self) {
        // Custom initialization
        [self registerLog];
    }
    return self;
}

- (id)initWithUrl:(NSURL *)url
{
    self = [super init];
    if (self) {
        url_ = [url copy];
    }
    return self;
}

- (void)registerLog
{
    //format
    NSLog(@"_prefix:set($timestamp:)");
    //NSLog(@"_route:file(player.log)");
}

- (void)unregisterLog
{
    NSLog(@"_prefix:set()");
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    //hide status bar
    if ([self respondsToSelector:@selector(setNeedsStatusBarAppearanceUpdate)]) {
        [self prefersStatusBarHidden];
        [self performSelector:@selector(setNeedsStatusBarAppearanceUpdate)];
    }
    messageLab_.baselineAdjustment = UIBaselineAdjustmentAlignBaselines;
    CGRect frame = CGRectMake(0, 0,
               [UIScreen mainScreen].bounds.size.height,
               [UIScreen mainScreen].bounds.size.width);
    //create player
    player_ = [PPPlayerController PPPlayerControllerWithUrl:url_
                                                      frame:frame
                                                       type:PPMOVIE_SELF_PLAYER];
    player_.delegate = self;
    player_.allowsAirPlay = YES;
    [self.view insertSubview:player_.view atIndex:0];
    //
    [self transformView:self.view];
    //add airplay button
    int btnWidget = 75;
    int btnHeight = 40;
    int btnX = frame.size.width - 8 - btnWidget;
    int btnY = frame.size.height - 8 - 100;
    CGRect rect = CGRectMake(btnX, btnY, btnWidget, btnHeight);
    MPVolumeView *volumeView = [[MPVolumeView alloc] initWithFrame:rect];
    volumeView.showsRouteButton = YES;
    volumeView.showsVolumeSlider = NO;
    [volumeView sizeToFit];
    [airPlayBtn_ sizeToFit];
    [self.view addSubview:volumeView];
    for (UIView *item in volumeView.subviews) {
        if (![item isKindOfClass:NSClassFromString(@"MPButton")]) {
			[item removeFromSuperview];
		} else {
            self.mpbutton = (UIButton *)item;
        }
	}
    //set title
    titleLab_.text = [url_ absoluteString];
    [player_ prepareToPlay];
}

- (BOOL)prefersStatusBarHidden
{
    return YES;
}

- (void)dealloc
{
    [player_ playerRelease];
}

- (IBAction)back
{
    [timer_ invalidate];
    [self stop];
    [self unregisterLog];
    [self dismissViewControllerAnimated:NO completion:nil];
}

- (IBAction)printScreen
{
    UIImage *image = [player_ screenShot:0];
    if (image) {
        [self saveImage:image];
    }
}

- (void)displayMovieMessage
{
    NSString *msg = [NSString stringWithFormat:@"Type: %@", [player_ playerType]==PPMOVIE_SYSTEM_PLAYER ? @"system" : @"ppplayer"];
    //NSString *msg1 = [NSString stringWithFormat:@"Url: %@", [url_ absoluteString]];
    NSString *msg2 = [NSString stringWithFormat:@"Duration: %.0f sec", player_.duration];
    NSString *msg3 = [NSString stringWithFormat:@"Resolution: %.0f * %.0f", player_.size.width, player_.size.height];
    NSString *msg4 = [NSString stringWithFormat:@"numberOfBytesTransfered: %lld", [player_ numberOfBytesTransfered]];
    NSString *msg5 = [NSString stringWithFormat:@"buffering time: %.0f msec", player_.bufferingTime];
    NSString *tmp = [NSString stringWithFormat:@"%@\r\n%@\r\n%@\r\n%@\r\n%@", msg, msg2, msg3, msg4, msg5];
    messageLab_.text = tmp;
}

- (BOOL)saveImage:(UIImage*)image
{
    // 获取系统当前时间
    NSDate * date = [NSDate date];
    NSTimeInterval sec = [date timeIntervalSinceNow];
    NSDate * currentDate = [[NSDate alloc] initWithTimeIntervalSinceNow:sec];
    
    //设置时间输出格式：
    NSDateFormatter * df = [[NSDateFormatter alloc] init ];
    [df setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
    NSString * na = [df stringFromDate:currentDate];
    NSString *fileName = [NSString stringWithFormat:@"ScreenShot/%@.gif", na];
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,NSUserDomainMask, YES);
    NSString *filePath = [[paths objectAtIndex:0] stringByAppendingPathComponent:fileName];   // 保存文件的名称
    BOOL result = [UIImagePNGRepresentation(image) writeToFile:filePath
                                                    atomically:YES]; // 保存成功会返回YES
    return result;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


#pragma mark PPPlayerDelegate

- (void)PPPlayerLoadFailed:(NSError*)err
{
    [self showAlert:@"媒体加载失败"
            message:err.localizedDescription];
    //NSLog(@"PPPlayerLoadFailed:%@", err);
}

- (IBAction)seek:(UISlider *)sender
{
    //NSLog(@"seek:%f", sender.value);
    player_.currentPlaybackTime = sender.value;
}

- (void)transformView:(UIView *)view
{
    CGAffineTransform at = CGAffineTransformMakeRotation(-M_PI/2);
    at = CGAffineTransformTranslate(at, 0, 0);
    [view setTransform:at];
}

- (IBAction)stop
{
    if (timer_) {
        [timer_ invalidate];
        timer_ = nil;
    }
    //NSLog(@"player stop");
    [player_ stop];
    //[player_ playerRelease];
    scrubber_.value = 0;
    scrubber_.maximumValue = 0;
    durationLab_.text = @"00:00";
    playbackTimeLab_.text = @"00:00";
    
    [self unregisterLog];
    [self dismissViewControllerAnimated:NO completion:nil];
}

- (void)PPPlayerLoadFinished
{
    int duration = player_.duration;
    //NSLog(@"duration:%f", duration);
    durationLab_.text = [NSString stringWithFormat:@"%d:%d", duration / 60, duration % 60];
    scrubber_.maximumValue = duration;
    //NSLog(@"airplay:%@", player_.allowsAirPlay ? @"yes" : @"no");
    //
    //NSLog(@"[PlaybackViewController] [PPPlayerLoadFinished] duration: %f", player_.duration);
    [player_ play];
    //NSLog(@"begin play");
    timer_ =  [NSTimer scheduledTimerWithTimeInterval:1.0
                                               target:self
                                             selector:@selector(updateScrubber)
                                             userInfo:nil
                                              repeats:YES];
    [self displayMovieMessage];
}

- (void)updateScrubber
{
    int playbackTime = player_.currentPlaybackTime;
    scrubber_.value = playbackTime;
    playbackTimeLab_.text = [NSString stringWithFormat:@"%d:%d", playbackTime / 60, playbackTime % 60];
    
    [self displayMovieMessage];
}

- (IBAction)pause:(UIButton *)sender
{
    [player_ pause];
}

- (IBAction)play
{
    if ([player_ isPaused]) {
        [player_ play];
    }
}

- (void)PPPlayerBufferFinished
{
    [player_ play];
    //NSLog(@"callback");
}

- (void)PPPlayerPlayBackDidFinished
{
    [self dismissViewControllerAnimated:NO completion:nil];
    //NSLog(@"[PlaybackViewController] [PPPlayerPlayBackDidFinished]");
    //NSLog(@"play finish");
}

- (void)PPPlayerBuffering
{
    //NSLog(@"[PlaybackViewController] [PPPlayerBuffering]");
    NSLog(@"player buffering");
}

- (void)PPPlayerPlayBackRateChanged:(NSInteger)rate
{
    //NSLog(@"[PlaybackViewController] [PPPlayerPlayBackStateChanged] rate:%ld", (long)rate);
    //NSLog(@"rate:%ld", (long)rate);
}

- (void)PPPlayerDurationAvailable:(NSTimeInterval)duration
{
    
}

- (void)PPPlayerSeekFinish:(NSError *)err
{
    if (!err) {
        NSLog(@"seek finish");
    }
}

- (void)showAlert:(NSString *)title message:(NSString*)message
{
    UIAlertController *alert = [UIAlertController
                                alertControllerWithTitle:title
                                message:message
                                preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *action = [UIAlertAction actionWithTitle:@"确定"
                                                     style:UIAlertActionStyleDefault
                                                   handler:^(UIAlertAction *action)
    {
        [self back];
    }];
    [alert addAction:action];
    [self transformView:alert.view];
    [self presentViewController:alert
                       animated:NO
                     completion:nil];

}

- (void)PPPlayerPlayBackError:(NSError*)err
{
    [self showAlert:@"播放失败"
            message:err.localizedDescription];
}

- (void)PPPlayerLog:(NSString *)msg
{
    NSLog(@"%@", msg);
}
@end
