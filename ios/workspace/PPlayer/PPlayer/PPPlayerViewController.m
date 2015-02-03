//
//  PPPlayerViewController.m
//  PPlayer
//
//  Created by stephenzhang on 13-9-5.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "PPPlayerViewController.h"

@interface PPPlayerViewController ()
{
    CGFloat screenWidth;
    CGFloat screenHeight;
    CGPoint beginPoint;//手势开始点
    CGPoint movePoint;//手势移动点
    CGPoint previousPoint;//手势移动前一个点
    UITouch *touch;
    NSString *tabButtonFlag;
    NSMutableDictionary* audioDictionary;//音轨键值对
    NSArray *sortedAudioKey;//排序后的音轨键数组
    bool hideOrShow;//隐藏显示控制栏
    bool playOrPause;
    bool mulButtonClicked;
    bool isLongPress;
    bool isUpandDownBrightness;//手势调节亮度
    bool isUpandDownVoice;//手势调节音量
    bool isRightandLeft;//手势调节进度
    bool noSysVoiceChange;
    bool isTranslation;
    int duration;//影片时长
    int touchCount;
    int touchTime;
    int subSelectedCell;//记录字幕选取的行
    int audioSelectedCell;//记录音轨选取的行
    int decodingSelectedCell;//记录解码选取的行
    int DLNAcell;
    float volValue;//音量进度值
    float brightValue;//亮度进度值
    bool canHardDecoding;
    NSString *title;
	int dec_msec;
	int render_msec;
	int dec_fps;
	int render_fps;
	long long render_frames;
	int drop_frames;
	int latency_msec;
}
@property (nonatomic, strong)  NSTimer * showtime;
@property (nonatomic, strong) UIButton  *mpbutton;
@end

@implementation PPPlayerViewController

#pragma mark
- (id)initWithPlayURL:(NSString *)playURL
{
    if (self = [super init])
    {
        title = [NSString stringWithFormat:@"%@",[playURL lastPathComponent]];
        self.playURL = playURL;
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    [[UIApplication sharedApplication] setIdleTimerDisabled: YES];
    [[UIApplication sharedApplication] setStatusBarStyle:UIStatusBarStyleBlackTranslucent];
    [[UIApplication sharedApplication] setStatusBarHidden:YES];
    
    //声音改变通知
    [[MPMusicPlayerController applicationMusicPlayer] beginGeneratingPlaybackNotifications];
    //视频进度条
    [self.videoSlider setMinimumTrackImage:[UIImage imageNamed:@"iphone_play_progress_bar.png"] forState:UIControlStateNormal];
    [self.videoSlider setMaximumTrackImage:[UIImage imageNamed:@"iphone_play_progress_bar_BG.png"] forState:UIControlStateNormal];
    [self.videoSlider setThumbImage:[UIImage imageNamed:@"iphone_play_play_control.png"] forState:UIControlStateNormal];
    [self.videoSlider setThumbImage:[UIImage imageNamed:@"iphone_play_play_control_sel.png"] forState:UIControlStateHighlighted];
    [self.videoSlider addTarget:self action:@selector(videoSliderValueChanging)forControlEvents:UIControlEventValueChanged];
    [self.videoSlider addTarget:self action:@selector(videoSliderValueChangeBegin)forControlEvents:UIControlEventTouchDown];
    [self.videoSlider addTarget:self action:@selector(videoValueChanged)forControlEvents:UIControlEventTouchUpInside];
    
    //视频小进度条
    [self.smallSlider setMinimumTrackImage:[UIImage imageNamed:@"iphone_play_progress_bar.png"] forState:UIControlStateNormal];
    [self.smallSlider setMaximumTrackImage:[UIImage imageNamed:@"iphone_play_progress_bar_BG.png"] forState:UIControlStateNormal];
    [self.smallSlider setThumbImage:[UIImage imageNamed:@"iphone_play_play_control.png"] forState:UIControlStateNormal];
    
    //声音亮度浮层
    self.volBrightBG.frame = CGRectMake((screenHeight - 141)/2, (screenWidth - 119)/2-20, 141, 119);
    [self.view addSubview:self.volBrightBG];
    for(UIView * volBrightview in self.volBrightBG.subviews)
    {
        volBrightview.alpha = 0;
    }
    self.volBrightBG.alpha = 0;
    
    //小进度条
    self.smallProgressBG.frame = CGRectMake(10, screenWidth-50, screenHeight-20, 20);
    [self.view addSubview:self.smallProgressBG];
    for(UIView * smallProgressview in self.smallProgressBG.subviews)
    {
        smallProgressview.alpha = 0;
    }
    self.smallProgressBG.alpha = 0;
    
    //按钮table浮层
    [self.view addSubview:self.longButtonTable];
    self.longButtonTable.alpha = 0;
    self.buttonTableView.separatorColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:0.2];
    
    //多屏浮层
    self.multiScreenView.frame = CGRectMake(screenHeight - 177, 37, 175, 136);
    [self.view addSubview:self.multiScreenView];
    self.multiScreenView.alpha = 0;
    mulButtonClicked = NO;
    self.multiScreenTable.separatorColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:0.3];
    
    //去除系统音量控件,造airplay
    MPVolumeView *volumeView = [[MPVolumeView alloc] initWithFrame:CGRectMake(-200, -200, 20, 20)];
    volumeView.showsRouteButton = YES;
    [self.view addSubview:volumeView];
    for (UIView *item in volumeView.subviews)
    {
		if ([item isKindOfClass:NSClassFromString(@"MPButton")])
        {
            self.mpbutton = (UIButton *)item;
		}
	}
    self.movieTitle.text = title;
    self.movieTitle.alpha = 0;
    self.videoSlider.alpha = 0;
    self.curTimeLabel.alpha = 0;
    self.lastTimeLabel.alpha = 0;
    self.seekChangeLabel.alpha = 0;
    self.seekCurLabel.alpha = 0;
    for(UIImageView *collection in self.imageCollection)
    {
        collection.alpha = 0;
    }
    for(UIButton *collection in self.buttonCollection)
    {
        collection.alpha = 0;
    }
    for(UIImageView *collection in self.voiceCollection)
    {
        collection.alpha = 0;
    }
    for(UIImageView *collection in self.brightnessCollection)
    {
        collection.alpha = 0;
    }
    for(UIImageView * collection in self.seekImgCollection)
    {
        collection.alpha = 0;
    }
    subSelectedCell = 0;
    audioSelectedCell = 0;
    hideOrShow = YES;
    playOrPause = YES;
    isLongPress = NO;
    isUpandDownBrightness = NO;
    isUpandDownVoice = NO;
    isRightandLeft = NO;
    noSysVoiceChange = NO;
    isTranslation = NO;
    
    [self SingleTapGesture];
    
    dispatch_async(dispatch_get_global_queue(0, 0), ^{
    //影片信息获取
    [[PPMediaPlayerInfo sharedInstance] setMediaURL:self.playURL];
    canHardDecoding = [[PPMediaPlayerInfo sharedInstance] canHardwareDecoding];
    audioDictionary = [[PPMediaPlayerInfo sharedInstance] getAudioChannel];
    NSArray *audioNumber = [audioDictionary allKeys];
    sortedAudioKey = [audioNumber sortedArrayUsingComparator:^NSComparisonResult(id obj1, id obj2) {return [obj1 compare:obj2 options:NSNumericSearch];
    }];
    
    dispatch_async(dispatch_get_main_queue(), ^{
    //视频初始化播放
    //canHardDecoding = false;
    self.moviePlayer = [[PPMoviePlayerController alloc] init:PPMOVIE_AUTO_PLAYER
                                                         url:[NSURL URLWithString:[self.playURL stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]]];
    [self.view insertSubview:self.moviePlayer.view atIndex:0];
    screenWidth = [[UIScreen mainScreen] bounds].size.width;
    screenHeight = [[UIScreen mainScreen] bounds].size.height;
    [self.moviePlayer.view setFrame:CGRectMake(0, -20, screenHeight, screenWidth)];
    self.moviePlayer.view.autoresizingMask = UIViewAutoresizingFlexibleTopMargin;
    //字幕初始化
    self.movieSubtitle = [[PPPlayerSubtitle alloc] initWithPlayer:self.moviePlayer];
    [self.view insertSubview:self.movieSubtitle.subtitleView atIndex:1];
    [self.movieSubtitle autoDetectSubtitles];
    NSLog(@"%@",self.playURL);
    NSLog(@"开始播放");
    //开始播放
    [self.moviePlayer prepareToPlay];

    [self addGesture:self.moviePlayer.view];
    [self addNotifications];
    });
    });
}

- (void)viewDidUnload
{
    [self setVideoSlider:nil];
    [self setPlayButton:nil];
    [self setCurTimeLabel:nil];
    [self setLastTimeLabel:nil];    
    [self setVoiceProgress:nil];    
    [self setSeekCurLabel:nil];
    [self setSeekChangeLabel:nil];        
    [self setVolBrightNum:nil];
    [self setSmallSlider:nil];
    [self setSmallCurTime:nil];
    [self setSmallLastTime:nil];
    [self setBrightProgress:nil];
    
    [self setVolBrightBG:nil];
    [self setSmallProgressBG:nil];
    [self setImageCollection:nil];
    [self setButtonCollection:nil];
    [self setBrightnessCollection:nil];
    [self setVoiceCollection:nil];
    [self setSeekImgCollection:nil];
    [self setVoiceBG:nil];
    [self setBrightnessBG:nil];
    [self setLeftButton:nil];
    [self setMidButton:nil];
    [self setRightButton:nil];
    [self setLongButtonTable:nil];
    [self setButtonTableView:nil];
    [self setIndicatorView:nil];
    [self setMultiScreenView:nil];
    [self setMultiScreenTable:nil];
    [self setDLNAindicator:nil];
    [self setTitle:nil];
    [self setMovieTitle:nil];
    [self.moviePlayer playerRelease];
    [super viewDidUnload];
}

- (void)dealloc
{
    [self removeNotifications];
    self.playURL = nil;
    [self.moviePlayer stop];
    [self.moviePlayer playerRelease];
    [self.moviePlayer.view removeFromSuperview];
    self.moviePlayer = nil;
    [[UIApplication sharedApplication] setIdleTimerDisabled:NO];
    [[UIApplication sharedApplication] setStatusBarHidden:NO withAnimation:UIStatusBarAnimationNone];
    [[MPMusicPlayerController applicationMusicPlayer] endGeneratingPlaybackNotifications];
}

- (void)didReceiveMemoryWarning
{
    NSLog(@"didReceiveMemoryWarning");
    [super didReceiveMemoryWarning];
}

#pragma mark HandButton
- (IBAction)backClicked:(id)sender
{
    [self.showtime invalidate];
    self.moviePlayer = nil;
    [self dismissViewControllerAnimated:NO completion:nil];
}

- (IBAction)playorPause:(id)sender
{
    if(playOrPause == YES)
    {
        [self.moviePlayer pause];
        [self.playButton setImage:[UIImage imageNamed:@"iphone_play_bottom_play_icon.png"] forState:UIControlStateNormal];
        [self.playButton setImage:[UIImage imageNamed:@"iphone_play_bottom_play_icon_sel.png"] forState:UIControlStateHighlighted];
        playOrPause = NO;
    }
    else
    {
        [self.moviePlayer play];
        [self.playButton setImage:[UIImage imageNamed:@"iphone_play_bottom_Pause_icon.png"] forState:UIControlStateNormal];
        [self.playButton setImage:[UIImage imageNamed:@"iphone_play_bottom_Pause_icon_sel.png"] forState:UIControlStateHighlighted];
        playOrPause = YES;
    }
}

- (IBAction)multiScreenClicked:(id)sender
{
    if(!mulButtonClicked)
    {
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.multiScreenView.alpha = 1;
        [UIView commitAnimations];
        mulButtonClicked = YES;
        [self.multiScreenTable reloadData];
    }
    else
    {
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.multiScreenView.alpha = 0;
        [UIView commitAnimations];
        mulButtonClicked = NO;
    }
}

- (IBAction)airPlayClicked:(id)sender
{
    if(!canHardDecoding)
    {
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:nil message:@"本视频格式不支持AirPlay" delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
        [alert show];
    }
    else
    {
        if(decodingSelectedCell == 0)
        {
            [self.moviePlayer changeDecoding];
            [self.indicatorView startAnimating];
            [self.showtime invalidate];
            self.showtime = nil;
            self.leftButton.enabled = NO;
            decodingSelectedCell = 1;
        }
        [self.mpbutton sendActionsForControlEvents:UIControlEventTouchUpInside];
    }
}

- (IBAction)leftButtonClicked:(id)sender
{
    tabButtonFlag = @"left";
    [self.buttonTableView reloadData];
    if([self.leftButton isSelected] == NO)
    {
        self.leftButton.selected = YES;
        self.midButton.selected = NO;
        self.rightButton.selected = NO;
        if([audioDictionary count] <= 1)
        {
            if(isTranslation) self.longButtonTable.frame = CGRectMake(self.leftButton.frame.origin.x-32, 200, 120, 45);
            else self.longButtonTable.frame = CGRectMake(self.leftButton.frame.origin.x-32, 220, 120, 45);
        }
        else
        {
            //根据音轨数目调整tableView长度
            if(isTranslation) self.longButtonTable.frame = CGRectMake(self.leftButton.frame.origin.x-32, 240-[audioDictionary count]*40, 120, 40*[audioDictionary count]+5);
            else self.longButtonTable.frame = CGRectMake(self.leftButton.frame.origin.x-32, 260-[audioDictionary count]*40, 120, 40*[audioDictionary count]+5);
        }
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.longButtonTable.alpha = 1;
        [UIView commitAnimations];
    }
    else
    {
        self.leftButton.selected = NO;
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.longButtonTable.alpha = 0;
        [UIView commitAnimations];
    }
}

- (IBAction)midButtonClicked:(id)sender
{
    tabButtonFlag = @"mid";
    [self.buttonTableView reloadData];
    if([self.midButton isSelected] == NO)
    {
        self.midButton.selected = YES;
        self.leftButton.selected = NO;
        self.rightButton.selected = NO;
        if([self.movieSubtitle.subtitleArray count] == 0)
        {
            if(isTranslation) self.longButtonTable.frame = CGRectMake(self.midButton.frame.origin.x-32, 200, 120, 45);
            else self.longButtonTable.frame = CGRectMake(self.midButton.frame.origin.x-32, 220, 120, 45);
        }
        else
        {
            if(isTranslation) self.longButtonTable.frame = CGRectMake(self.midButton.frame.origin.x-68, 120, 190, 125);
            else self.longButtonTable.frame = CGRectMake(self.midButton.frame.origin.x-68, 140, 190, 125);
        }
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.longButtonTable.alpha = 1;
        [UIView commitAnimations];
    }
    else
    {
        self.midButton.selected = NO;
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.longButtonTable.alpha = 0;
        [UIView commitAnimations];
    }
}

- (IBAction)rightButtonClicked:(id)sender
{
    tabButtonFlag = @"right";
    [self.buttonTableView reloadData];
    if([self.rightButton isSelected] == NO)
    {
        self.rightButton.selected = YES;
        self.leftButton.selected = NO;
        self.midButton.selected = NO;
        if(canHardDecoding)
        {
            if(isTranslation) self.longButtonTable.frame = CGRectMake(self.rightButton.frame.origin.x-32, 160, 120, 85);
            else self.longButtonTable.frame = CGRectMake(self.rightButton.frame.origin.x-32, 180, 120, 85);
        }
        else
        {
            if(isTranslation) self.longButtonTable.frame = CGRectMake(self.rightButton.frame.origin.x-32, 200, 120, 45);
            else self.longButtonTable.frame = CGRectMake(self.rightButton.frame.origin.x-32, 220, 120, 45);
        }
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.longButtonTable.alpha = 1;
        [UIView commitAnimations];
    }
    else
    {
        self.rightButton.selected = NO;
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.longButtonTable.alpha = 0;
        [UIView commitAnimations];
    }
}

#pragma mark HandTableView
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if(tableView.tag == 38)//底部按钮的tableView
    {
        if([tabButtonFlag isEqualToString:@"left"])
        {
            if([audioDictionary count] ==0)return 1;
            else return [audioDictionary count];
        }
        else if([tabButtonFlag isEqualToString:@"mid"])
        {
            return [self.movieSubtitle.subtitleArray count]+1;
        }
        else
        {
            if(canHardDecoding)return 2;
            else return 1;
        }
    }
    else//多屏的tableView
    {
        return 0;
    }
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    if(tableView.tag == 38)//底部按钮的tableView
    {
        static NSString * identifier = @"buttonTableCell";
        UITableViewCell * cell = [tableView dequeueReusableCellWithIdentifier:identifier];
        if(cell == nil)
        {
            cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:identifier];
            cell.textLabel.font = [UIFont systemFontOfSize:12];
            cell.textLabel.textAlignment = UITextAlignmentCenter;
            cell.selectionStyle = UITableViewCellSelectionStyleNone;
        }
        cell.textLabel.textColor = [UIColor grayColor];
        
        if([tabButtonFlag isEqualToString:@"left"])
        {
            if([indexPath row] == audioSelectedCell)cell.textLabel.textColor = [UIColor whiteColor];
            if([audioDictionary count] == 0)cell.textLabel.text = @"默认音轨";
            else cell.textLabel.text = [audioDictionary objectForKey:[sortedAudioKey objectAtIndex:[indexPath row]]];
        }
        else if([tabButtonFlag isEqualToString:@"mid"])
        {
            if([indexPath row] == subSelectedCell)cell.textLabel.textColor = [UIColor whiteColor];
            if([indexPath row] == 0)
            {
                if([self.movieSubtitle.subtitleArray count] ==0)cell.textLabel.text = @"无外挂字幕";
                else cell.textLabel.text = @"关闭字幕";
            }
            else cell.textLabel.text = [[self.movieSubtitle.subtitleArray objectAtIndex:[indexPath row]-1] lastPathComponent];
        }
        else
        {
            if([indexPath row] == decodingSelectedCell)cell.textLabel.textColor = [UIColor whiteColor];
            if([indexPath row] == 0)
            {
                cell.textLabel.text = @"软解码";
            }
            else
            {
                cell.textLabel.text = @"硬解码";
            }
        }
        return cell;
    }
    else//多屏的tableView
    {
        static NSString * mulidentifier = @"multiTableCell";
        UITableViewCell * cell = [tableView dequeueReusableCellWithIdentifier:mulidentifier];
        if(cell == nil)
        {
            cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:mulidentifier];
            cell.textLabel.font = [UIFont systemFontOfSize:12];
            cell.selectionStyle = UITableViewCellSelectionStyleGray;
            cell.textLabel.textColor = [UIColor whiteColor];
        }
        return cell;
    }
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if(tableView.tag == 38)//底部按钮的tableView
    {
        if([tabButtonFlag isEqualToString:@"left"])
        {
            UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
            cell.textLabel.textColor = [UIColor whiteColor];
            [self.buttonTableView reloadData];
            if(audioSelectedCell != [indexPath row])
            {
                [self.moviePlayer changeAudioChannel:[[sortedAudioKey objectAtIndex:[indexPath row]] intValue]];
                [self.indicatorView startAnimating];
                [self.showtime invalidate];
                self.showtime = nil;
            }
            audioSelectedCell = [indexPath row];
        }
        
        if([tabButtonFlag isEqualToString:@"mid"])
        {
            UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
            cell.textLabel.textColor = [UIColor whiteColor];
            [self.buttonTableView reloadData];
            if(subSelectedCell != [indexPath row])
            {
                if([indexPath row] == 0) [self.movieSubtitle closeSubtitle];
                else [self.movieSubtitle subtitleWithAddress:[self.movieSubtitle.subtitleArray objectAtIndex:[indexPath row]-1]];
            }
            subSelectedCell = [indexPath row];
        }
        
        if([tabButtonFlag isEqualToString:@"right"])
        {
            UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
            cell.textLabel.textColor = [UIColor whiteColor];
            [self.buttonTableView reloadData];
            if( decodingSelectedCell != [indexPath row])
            {
                [self.moviePlayer changeDecoding];
                [self.indicatorView startAnimating];
                [self.showtime invalidate];
                self.showtime = nil;
                if([indexPath row] == 1)
                {
                    self.leftButton.enabled = NO;
                }
                else
                {
                    self.leftButton.enabled = YES;
                }
            }
            decodingSelectedCell = [indexPath row];
        }
    }
    else//多屏的tableView
    {
        
    }
}

- (void)deviceCapNotification
{
}

- (void)setUriNotification
{
}

- (void)tableView:(UITableView *)tableView didDeselectRowAtIndexPath:(NSIndexPath *)indexPath
{
    
    if(tableView.tag == 38)//底部按钮的tableView
    {
        if([tabButtonFlag isEqualToString:@"left"])
        {
            UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
            cell.textLabel.textColor = [UIColor grayColor];
        }
        
        if([tabButtonFlag isEqualToString:@"mid"])
        {
            UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
            cell.textLabel.textColor = [UIColor grayColor];
        }
        
        if([tabButtonFlag isEqualToString:@"right"])
        {
            UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
            cell.textLabel.textColor = [UIColor grayColor];
        }
    }
}

#pragma mark HandGesture
//处理手势
- (void)addGesture:(UIView *)view
{
    //点击
    UITapGestureRecognizer *gesture = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(SingleTapGesture)];
    gesture.numberOfTapsRequired = 1;
    [view addGestureRecognizer:gesture];
    gesture = nil;
    //滑动
    UIPanGestureRecognizer *longGesture = [[UIPanGestureRecognizer alloc]initWithTarget:self action:@selector(handleLongPressGesture)];
    longGesture.minimumNumberOfTouches = 1;
    longGesture.maximumNumberOfTouches = 1;
    [view addGestureRecognizer:longGesture];
    longGesture.cancelsTouchesInView = NO;
    longGesture = nil;
}

- (void)SingleTapGesture
{
    if(hideOrShow == YES)
    {
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.movieTitle.alpha = 1;
        self.videoSlider.alpha = 1;
        self.curTimeLabel.alpha = 0.8;
        self.lastTimeLabel.alpha = 0.8;
        for(UIImageView *imgCollection in self.imageCollection)
        {
            imgCollection.alpha = 1;
        }
        for(UIButton *btCollection in self.buttonCollection)
        {
            btCollection.alpha = 1;
        }
        [UIView commitAnimations];
        [[UIApplication sharedApplication] setStatusBarHidden:NO withAnimation:UIStatusBarAnimationFade];
        hideOrShow = NO;
    }
    else
    {
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:0.4];
        self.movieTitle.alpha = 0;
        self.videoSlider.alpha = 0;
        self.curTimeLabel.alpha = 0;
        self.lastTimeLabel.alpha = 0;
        self.longButtonTable.alpha = 0;
        for(UIImageView *imgCollection in self.imageCollection)
        {
            imgCollection.alpha = 0;
        }
        for(UIButton *btCollection in self.buttonCollection)
        {
            btCollection.alpha = 0;
        }
        self.multiScreenView.alpha = 0;
        [UIView commitAnimations];
        [[UIApplication sharedApplication] setStatusBarHidden:YES withAnimation:UIStatusBarAnimationFade];
        hideOrShow = YES;
        mulButtonClicked = NO;
        self.rightButton.selected = NO;
        self.leftButton.selected = NO;
        self.midButton.selected = NO;
    }
}

- (void)handleLongPressGesture
{
    isLongPress = YES;
}

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    touchCount = 0;
    touch = [touches anyObject];
    beginPoint = [touch locationInView:self.moviePlayer.view];
    touchTime = self.videoSlider.value;
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    if(isLongPress)
    {
        touch = [touches anyObject];
        previousPoint = [touch previousLocationInView:self.moviePlayer.view];
        movePoint = [touch locationInView:self.moviePlayer.view];
        
        //算手势趋势
        if (touchCount == 8)
        {
            if (fabs(beginPoint.y - movePoint.y) > fabs(beginPoint.x - movePoint.x))
            {
                if(beginPoint.x > screenHeight/2)
                {
                    isUpandDownVoice = YES;
                    for(UIImageView *voiceCollection in self.voiceCollection)
                    {
                        voiceCollection.alpha = 1;
                    }
                    self.volBrightBG.alpha = 1;
                    for(UIImageView *voicebg in self.voiceBG)
                    {
                        voicebg.alpha = 1;
                    }
                    self.volBrightNum.alpha =1;
                }
                else
                {
                    isUpandDownBrightness = YES;
                    for(UIImageView *brightnessCollection in self.brightnessCollection)
                    {
                        brightnessCollection.alpha = 1;
                    }
                    self.volBrightBG.alpha = 1;
                    for(UIImageView *brightnessbg in self.brightnessBG)
                    {
                        brightnessbg.alpha = 1;
                    }
                    self.volBrightNum.alpha =1;
                }
            }
            else if (fabs(beginPoint.x - movePoint.x) > fabs(beginPoint.y - movePoint.y))
            {
                isRightandLeft = YES;
            }
        }
        touchCount++;
        if(isUpandDownVoice)//上下调节音量
        {
            noSysVoiceChange = YES;
            volValue = volValue + (previousPoint.y - movePoint.y) / 300;
            if(volValue<0) volValue = 0;
            if(volValue>1) volValue = 1;
            [[MPMusicPlayerController applicationMusicPlayer] setVolume:volValue];            
            self.volBrightNum.text = [NSString stringWithFormat:@"%d%%",(int)(volValue*100)];
            if(isTranslation == NO)self.voiceProgress.frame = CGRectMake(525, 220-volValue*125, 4, volValue*125);
            else self.voiceProgress.frame = CGRectMake(525, 200-volValue*125, 4, volValue*125);

        }
        else if(isUpandDownBrightness)//上下调节亮度
        {
            brightValue = brightValue + (previousPoint.y - movePoint.y) / 300;
            if(brightValue<0) brightValue = 0;
            if(brightValue>1) brightValue = 1;
            [[UIScreen mainScreen] setBrightness:brightValue];
            self.volBrightNum.text = [NSString stringWithFormat:@"%d%%",(int)(brightValue*100)];
            if(isTranslation == NO)self.brightProgress.frame = CGRectMake(38, 220-brightValue*125, 4, brightValue*125);
            else self.brightProgress.frame = CGRectMake(38, 200-brightValue*125, 4, brightValue*125);
        }
        else if(isRightandLeft)//左右则调节进度
        {
            [self.showtime invalidate];
            self.showtime = nil;
            float gesValue = (movePoint.x - previousPoint.x)/(screenHeight*1.5)*duration;
            self.videoSlider.value = self.videoSlider.value + gesValue;
            self.videoSlider.highlighted = YES;
            int nowtime = self.videoSlider.value;
            int lasttime = duration - nowtime;
            self.lastTimeLabel.text = [NSString stringWithFormat:@"%02d:%02d", lasttime / 60, lasttime % 60];
            self.curTimeLabel.text = [NSString stringWithFormat:@"%02d:%02d", nowtime / 60, nowtime % 60];
            
            if(hideOrShow == YES)//控制控件隐藏条件下的情况
            {
                for(UIImageView * collection in self.seekImgCollection)
                {
                    collection.alpha = 1;
                }
                self.seekChangeLabel.alpha = 1;
                self.seekCurLabel.alpha = 1;
                for(UIView * smallProgressview in self.smallProgressBG.subviews)
                {
                    smallProgressview.alpha = 1;
                }
                self.smallProgressBG.alpha = 1;
                int nowtime = self.videoSlider.value;
                int changetime = nowtime - touchTime;
                self.smallSlider.value = nowtime;
                self.smallCurTime.text = [NSString stringWithFormat:@"%02d:%02d", nowtime / 60, nowtime % 60];
                self.smallLastTime.text = [NSString stringWithFormat:@"%02d:%02d", (duration - nowtime) / 60, (duration - nowtime) % 60];
                bool plusOrMinus = changetime > 0? YES:NO;
                changetime = fabs(changetime);
                self.seekCurLabel.text = [NSString stringWithFormat:@"%02d:%02d", nowtime / 60, nowtime % 60];
                if(plusOrMinus)self.seekChangeLabel.text = [NSString stringWithFormat:@"+%02d:%02d", changetime / 60, changetime % 60];
                else self.seekChangeLabel.text = [NSString stringWithFormat:@"- %02d:%02d", changetime / 60, changetime % 60];
            }
        }
    }
}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    if(isUpandDownVoice)
    {
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:1];
        for(UIImageView *voiceCollection in self.voiceCollection)
        {
            voiceCollection.alpha = 0;
        }
        self.volBrightBG.alpha = 0;
        for(UIView *bg in [self.volBrightBG subviews])
        {
            bg.alpha = 0;
        }
        [UIView commitAnimations];
    }
    if(isUpandDownBrightness)
    {
        [UIView beginAnimations:nil context:nil];
        [UIView setAnimationDuration:1];
        for(UIImageView *brightnessCollection in self.brightnessCollection)
        {
            brightnessCollection.alpha = 0;
        }
        self.volBrightBG.alpha = 0;
        for(UIView *bg in [self.volBrightBG subviews])
        {
            bg.alpha = 0;
        }
        [UIView commitAnimations];
    }
    if(isRightandLeft)
    {
        self.videoSlider.highlighted = NO;
        self.moviePlayer.currentPlaybackTime = self.videoSlider.value;
        if(hideOrShow == YES)
        {
            [UIView beginAnimations:nil context:nil];
            [UIView setAnimationDuration:1];
            for(UIImageView * collection in self.seekImgCollection)
            {
                collection.alpha = 0;
            }
            self.seekChangeLabel.alpha = 0;
            self.seekCurLabel.alpha = 0;
            for(UIView * smallProgressview in self.smallProgressBG.subviews)
            {
                smallProgressview.alpha = 0;
            }
            self.smallProgressBG.alpha = 0;
            [UIView commitAnimations];
        }
    }
    
    if(self.showtime == nil)
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.showtime = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(showTime) userInfo:nil repeats:YES];[self.showtime fire];});
    }    
    isRightandLeft = NO;
    isUpandDownVoice = NO;
    isUpandDownBrightness = NO;
    noSysVoiceChange = NO;
}

-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    if(self.showtime == nil)
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.showtime = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(showTime) userInfo:nil repeats:YES];[self.showtime fire];});
    }
    for(UIImageView *voiceCollection in self.voiceCollection)
    {
        voiceCollection.alpha = 0;
    }
    for(UIImageView *brightnessCollection in self.brightnessCollection)
    {
        brightnessCollection.alpha = 0;
    } 
    for(UIImageView * collection in self.seekImgCollection)
    {
        collection.alpha = 0;
    }
    self.seekChangeLabel.alpha = 0;
    self.seekCurLabel.alpha = 0;
    self.volBrightBG.alpha = 0;
    for(UIView *bg in [self.volBrightBG subviews])
    {
        bg.alpha = 0;
    }
    for(UIView * smallProgressview in self.smallProgressBG.subviews)
    {
        smallProgressview.alpha = 0;
    }
    self.smallProgressBG.alpha = 0;
    self.videoSlider.highlighted = NO;
    isUpandDownVoice = NO;
    isUpandDownBrightness = NO;
    isRightandLeft = NO;
    noSysVoiceChange = NO;
}

#pragma mark HandNotifacation
//进度条拖动开始
- (void)videoSliderValueChangeBegin
{
    [self.showtime invalidate];
    self.showtime = nil;
}
//进度条拖动中，显示数字跟随改变
-(void)videoSliderValueChanging
{
    int nowtime = self.videoSlider.value;
    int lasttime = duration - nowtime;
    dispatch_async(dispatch_get_main_queue(), ^{
        self.lastTimeLabel.text = [NSString stringWithFormat:@"%02d:%02d", lasttime / 60, lasttime % 60];
        self.curTimeLabel.text = [NSString stringWithFormat:@"%02d:%02d", nowtime / 60, nowtime % 60];
    });
}
//进度条拖动完成，seek视频时间
- (void)videoValueChanged
{
    self.moviePlayer.currentPlaybackTime = self.videoSlider.value;
    dispatch_async(dispatch_get_main_queue(), ^{self.showtime = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(showTime) userInfo:nil repeats:YES];[self.showtime fire];});
}
//系统声音变化
- (void)sysVoiceValueChanged
{
    if(volValue == 0)return;
    if(volValue == [[MPMusicPlayerController applicationMusicPlayer] volume])return;
    volValue = [[MPMusicPlayerController applicationMusicPlayer] volume];
    if(noSysVoiceChange)
    {
        noSysVoiceChange = NO;
        return;
    }
    for(UIImageView *voiceCollection in self.voiceCollection)
    {
        voiceCollection.alpha = 1;
    }
    if(isTranslation == NO)self.voiceProgress.frame = CGRectMake(525, 220-volValue*125, 4, volValue*125);
    else self.voiceProgress.frame = CGRectMake(525, 200-volValue*125, 4, volValue*125);
    self.volBrightBG.alpha = 1;
    for(UIImageView *voicebg in self.voiceBG)
    {
        voicebg.alpha = 1;
    }
    self.volBrightNum.alpha =1;
    self.volBrightNum.text = [NSString stringWithFormat:@"%d%%",(int)(volValue*100)];
    [UIView beginAnimations:nil context:nil];
    [UIView setAnimationDelay:0.5];
    [UIView setAnimationDuration:1];
    for(UIImageView *voiceCollection in self.voiceCollection)
    {
        voiceCollection.alpha = 0;
    }
    self.volBrightBG.alpha = 0;
    for(UIView *voicebg in [self.volBrightBG subviews])
    {
        voicebg.alpha = 0;
    }
    [UIView commitAnimations];
}

//视频准备完成，开始播放
- (void)moviePlayerPrepared
{
    NSLog(@"moviePlayerPrepared");
    duration = [self.moviePlayer duration];
    self.videoSlider.maximumValue = duration;
    self.smallSlider.maximumValue = duration;
    volValue = [[MPMusicPlayerController applicationMusicPlayer] volume];
    brightValue = [[UIScreen mainScreen] brightness];
    dispatch_async(dispatch_get_main_queue(), ^{
        self.showtime = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(showTime) userInfo:nil repeats:YES];
        [self.showtime fire];
    [self.indicatorView stopAnimating];});
    if(playOrPause == NO)
        [self.moviePlayer pause];
}

- (void)handleDecodeAvgMsec:(NSNotification *)note
{
    NSDictionary *dic = note.object;
    //NSLog(@"handleDecodeAvgMsec Received notification: %@", dic);
    dec_msec =  [[dic objectForKey:@"msec"] intValue];
    //NSLog(@"decoder msec %d", dec_msec);
}

- (void)handleRenderAvgMsec:(NSNotification *)note
{
    NSDictionary *dic = note.object;
	render_msec =  [[dic objectForKey:@"msec"] intValue];
	render_frames++;
}
- (void)handleDecodeFPS:(NSNotification *)note
{
    NSDictionary *dic = note.object;
    dec_fps =  [[dic objectForKey:@"fps"] intValue];
    
    __weak __typeof(self)weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        weakSelf.movieTitle.text = [NSString stringWithFormat:@"%@ drop %03d dec %02d(%03d)/%02d(%03d)",
                                    title, drop_frames % 1000, dec_fps, dec_msec, render_fps, render_msec];
    });
}
- (void)handleRenderFPS:(NSNotification *)note
{
    NSDictionary *dic = note.object;
    render_fps =  [[dic objectForKey:@"fps"] intValue];
    
    __weak __typeof(self)weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        weakSelf.movieTitle.text = [NSString stringWithFormat:@"%@ drop %03d dec %02d(%03d)/%02d(%03d)",
                                    title, drop_frames % 1000, dec_fps, dec_msec, render_fps, render_msec];
    });
}
- (void)handleLatencyMsec:(NSNotification *)note
{
    NSDictionary *dic = note.object;
    latency_msec =  [[dic objectForKey:@"msec"] intValue];
    
	__weak __typeof(self)weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        weakSelf.movieTitle.text = [NSString stringWithFormat:@"%@ drop %03d dec %02d(%03d)/%02d(%03d)",
                                    title, drop_frames % 1000, dec_fps, dec_msec, render_fps, render_msec];

    });
}
- (void)handleDropFrame:(NSNotification *)note
{
    NSDictionary *dic = note.object;
    latency_msec =  [[dic objectForKey:@"msec"] intValue];
	drop_frames++;
    
    __weak __typeof(self)weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        weakSelf.movieTitle.text = [NSString stringWithFormat:@"%@ drop %03d dec %02d(%03d)/%02d(%03d)",
                                    title, drop_frames % 1000, dec_fps, dec_msec, render_fps, render_msec];
    });
}
- (void)updateTitle
{
	__weak __typeof(self)weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        weakSelf.movieTitle.text = [NSString stringWithFormat:@"%@ drop %03d dec %02d(%03d)/%02d(%03d)",
                                    title, drop_frames % 1000, dec_fps, dec_msec, render_fps, render_msec];

    }); 
}
//展示进度条时间
- (void)showTime
{
    int nowtime = self.moviePlayer.currentPlaybackTime;
    int lasttime = duration - nowtime;
    dispatch_async(dispatch_get_main_queue(), ^{
        self.lastTimeLabel.text = [NSString stringWithFormat:@"%02d:%02d", lasttime / 60, lasttime % 60];
        self.curTimeLabel.text = [NSString stringWithFormat:@"%02d:%02d", nowtime / 60, nowtime % 60];
    });
    self.videoSlider.value = nowtime;
}

- (void)willResignActive
{
    [self.showtime invalidate];
    self.showtime = nil;
}

- (void)playbackComplete
{
    [self.showtime invalidate];
    [[UIApplication sharedApplication] setIdleTimerDisabled: NO];
    [[UIApplication sharedApplication] setStatusBarHidden:NO withAnimation:UIStatusBarAnimationFade];
    dispatch_async(dispatch_get_main_queue(), ^{
        if ([self respondsToSelector:@selector(dismissViewControllerAnimated:completion:)])
        {
            [self dismissViewControllerAnimated:NO completion:nil];
        }
        else
        {
            [self dismissModalViewControllerAnimated:NO];
        }
    });
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(SingleTapGesture) object:nil];
}

- (void)removeNotifications
{
    [[NSNotificationCenter defaultCenter] removeObserver:self name:PPMoviePlayerPreparedNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:PPMoviePlayerPlaybackCompleteNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillResignActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:MPMusicPlayerControllerVolumeDidChangeNotification object:nil];
	// new added
	/*[[NSNotificationCenter defaultCenter] removeObserver:self name:handleDecodeAvgMsec object:nil];
	[[NSNotificationCenter defaultCenter] removeObserver:self name:handleRenderAvgMsec object:nil];
	[[NSNotificationCenter defaultCenter] removeObserver:self name:handleDecodeFPS object:nil];
	[[NSNotificationCenter defaultCenter] removeObserver:self name:handleRenderFPS object:nil];
	[[NSNotificationCenter defaultCenter] removeObserver:self name:handleLatencyMsec object:nil];
	[[NSNotificationCenter defaultCenter] removeObserver:self name:handleDropFrame object:nil];*/
}

- (void)addNotifications
{
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(moviePlayerPrepared)
                                                 name:PPMoviePlayerPreparedNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(playbackComplete)
                                                 name:PPMoviePlayerPlaybackCompleteNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(willResignActive)
                                                 name:UIApplicationWillResignActiveNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(sysVoiceValueChanged)
                                                 name:MPMusicPlayerControllerVolumeDidChangeNotification object:nil];
	// new added
	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleDecodeAvgMsec:)
                                                 name:PPMoviePlayerDecodeAvgMsecNotification
                                               object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRenderAvgMsec:)
                                                 name:PPMoviePlayerRenderAvgMsecNotification
                                               object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleDecodeFPS:)
                                                 name:PPMoviePlayerDecodeFPSNotification
                                               object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRenderFPS:)
                                                 name:PPMoviePlayerRenderFPSNotification
                                               object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleLatencyMsec:)
                                                 name:PPMoviePlayerLatencyMsecNotification
                                               object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleDropFrame:)
                                                 name:PPMoviePlayerDropFrameNotification
                                               object:nil];
}

#pragma mark supportedOrientation

- (NSUInteger)supportedInterfaceOrientations
{
    return UIInterfaceOrientationMaskLandscape;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)orientation
{
    return UIInterfaceOrientationIsLandscape(orientation);
}

-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    if([[UIApplication sharedApplication] isStatusBarHidden])
    {
        isTranslation = NO;
    }
    else
    {
        isTranslation = YES;
    }
}

@end
