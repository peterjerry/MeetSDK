//
//  PPPlayerViewController.h
//  PPlayer
//
//  Created by stephenzhang on 13-9-5.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <PPMediaPlayer/PPMediaPlayer.h>

@interface PPPlayerViewController : UIViewController

@property (nonatomic, strong) NSString *playURL;//播放地址
@property (nonatomic, strong) PPMoviePlayerController *moviePlayer;//播放器
@property (nonatomic, strong) PPPlayerSubtitle *movieSubtitle;//字幕
@property (strong, nonatomic) IBOutlet UIView *volBrightBG;//声音亮度浮层
@property (strong, nonatomic) IBOutlet UIView *smallProgressBG;
@property (strong, nonatomic) IBOutlet UISlider *videoSlider;//进度条
@property (strong, nonatomic) IBOutlet UISlider *smallSlider;
@property (strong, nonatomic) IBOutlet UIButton *playButton;//播放暂停
@property (strong, nonatomic) IBOutlet UILabel *curTimeLabel;//当前时间
@property (strong, nonatomic) IBOutlet UILabel *smallCurTime;
@property (strong, nonatomic) IBOutlet UILabel *lastTimeLabel;//剩余时间
@property (strong, nonatomic) IBOutlet UILabel *smallLastTime;
@property (strong, nonatomic) IBOutletCollection(UIImageView) NSArray *imageCollection;//播放控制图像集
@property (strong, nonatomic) IBOutletCollection(UIButton) NSArray *buttonCollection;//播放控制按钮集
@property (strong, nonatomic) IBOutletCollection(UIImageView) NSArray *voiceCollection;//声音控制图像集
@property (strong, nonatomic) IBOutletCollection(UIImageView) NSArray *brightnessCollection;//亮度控制图像集
@property (strong, nonatomic) IBOutletCollection(UIImageView) NSArray *seekImgCollection;
@property (strong, nonatomic) IBOutlet UILabel *seekCurLabel;
@property (strong, nonatomic) IBOutlet UILabel *seekChangeLabel;
@property (strong, nonatomic) IBOutlet UIImageView *voiceProgress;//声音进度图像
@property (strong, nonatomic) IBOutlet UIImageView *brightProgress;//亮度进度图像
@property (strong, nonatomic) IBOutletCollection(UIImageView) NSArray *voiceBG;//声音控制浮层图像集
@property (strong, nonatomic) IBOutletCollection(UIImageView) NSArray *brightnessBG;//亮度控制浮层图像集
@property (strong, nonatomic) IBOutlet UILabel *volBrightNum;//声音亮度百分值
@property (strong, nonatomic) IBOutlet UIButton *leftButton;
@property (strong, nonatomic) IBOutlet UIButton *midButton;
@property (strong, nonatomic) IBOutlet UIButton *rightButton;
@property (strong, nonatomic) IBOutlet UIView *longButtonTable;
@property (weak, nonatomic) IBOutlet UITableView *buttonTableView;
@property (strong, nonatomic) IBOutlet UIActivityIndicatorView *indicatorView;
@property (strong, nonatomic) IBOutlet UIView *multiScreenView;
@property (strong, nonatomic) IBOutlet UITableView *multiScreenTable;
@property (strong, nonatomic) IBOutlet UIActivityIndicatorView *DLNAindicator;
@property (strong, nonatomic) IBOutlet UILabel *movieTitle;

- (IBAction)backClicked:(id)sender;
- (IBAction)playorPause:(id)sender;
- (IBAction)multiScreenClicked:(id)sender;
- (IBAction)leftButtonClicked:(id)sender;
- (IBAction)midButtonClicked:(id)sender;
- (IBAction)rightButtonClicked:(id)sender;
- (IBAction)airPlayClicked:(id)sender;

- (id)initWithPlayURL:(NSString *)playURL;

@end
