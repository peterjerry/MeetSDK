//
//  PPPlayerSubtitle.m
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-7-18.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <fstream>
#import "subtitle.h"
#import "PPPlayerUtil.h"
#import "PPPlayerSubtitle.h"
#import "PPMoviePlayerController.h"

@interface PPPlayerSubtitle()
{
    int nowTime;                    //0.1s
    int64_t startTime;              //0.1s
    int64_t stopTime;               //0.1s
    char subtitleText[1024];
    ISubtitles* subtitle;
    STSSegment* segment;
    NSMutableData *responseData;    //接收服务器数据
    bool delay;                     //解决上一个结束时间等于下一个开始时间时展示问题
    bool notificationFlag;          //只加载一次notification
    bool cloudSubFlag;              //云字幕是否下载成功
}
@end

@implementation PPPlayerSubtitle

- (id)initWithPlayer:(PPMoviePlayerController*) nowPlayer
{
    if(self = [super init])
    {
        self.myPlayer = nowPlayer;
        notificationFlag = NO;
        cloudSubFlag = NO;
        if(self.subtitleView != nil)return self;
        /****************************label控件定义****************************/
        int screenWidth = [[UIScreen mainScreen] bounds].size.width;
        int screenHeight = [[UIScreen mainScreen] bounds].size.height;
        int longEdge = (screenWidth > screenHeight)? screenWidth: screenHeight;
        int shortEdge = (screenWidth < screenHeight)? screenWidth: screenHeight;
        int textSize = longEdge/35;
        self.subtitleView = [[UILabel alloc] initWithFrame:CGRectMake(20, shortEdge-84, longEdge-40, 80)];
        self.subtitleView.font = [UIFont boldSystemFontOfSize:textSize];
        self.subtitleView.textAlignment = NSTextAlignmentCenter;
        self.subtitleView.textColor = [UIColor whiteColor];
        self.subtitleView.shadowColor = [UIColor blackColor];
        self.subtitleView.shadowOffset = CGSizeMake(1.0,1.0);
        self.subtitleView.backgroundColor = [UIColor clearColor];
        self.subtitleView.numberOfLines = 0;
        self.subtitleView.autoresizingMask = UIViewAutoresizingFlexibleTopMargin;
        /*******************************************************************/
    }
    return self;
}

- (void)closeSubtitle
{
    dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"";});
    self.subtitleView.alpha = 0;
    if(self.myTimer != nil)
    {
        [self.myTimer invalidate];
        self.myTimer = nil;
    }
    if(subtitle != NULL) subtitle -> close();
    subtitle = NULL;
    segment = NULL;
    [self removeNotifications];
    notificationFlag = NO;
}

#pragma mark subtitleInit
- (void)subtitleWithAddress:(NSString*) subtitleAddress
{
    if(self.myTimer != nil)
    {
        [self.myTimer invalidate];
        self.myTimer = nil;
    }
/******************************字幕加载******************************/
    if(subtitle != NULL) subtitle -> close();
    subtitle = NULL;
    segment = NULL;
    if (!ISubtitles::create(&subtitle))
    {
        NSLog(@"false to create ISubtitles");
        return;
    }
    if(!(subtitle -> loadSubtitle([subtitleAddress UTF8String], false)))
    {
        NSLog(@"false to loadSubtitle");
        return;
    }
    nowTime = self.myPlayer.currentPlaybackTime*10;
    subtitle -> seekTo(nowTime*100);
    if(!(subtitle -> getNextSubtitleSegment(&segment)))return;
    startTime = (segment -> getStartTime())/100;
    stopTime = (segment -> getStopTime())/100;
    segment -> getSubtitleText(subtitleText, 1024);
/********************************************************************/
    if((self.myPlayer.isPreparedToPlay == YES)&&(self.myTimer == nil))
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.myTimer = [NSTimer scheduledTimerWithTimeInterval:0.1 target:self selector:@selector(showSubtitle) userInfo:nil repeats:YES];[self.myTimer fire];
            nowTime = self.myPlayer.currentPlaybackTime*10;
            subtitle -> seekTo(nowTime*100);
            if(!(subtitle -> getNextSubtitleSegment(&segment)))return;
            startTime = (segment -> getStartTime())/100;
            stopTime = (segment -> getStopTime())/100;
            segment -> getSubtitleText(subtitleText, 1024);});
    }
    if(notificationFlag == NO)
    {
        [self addNotifications];
        notificationFlag = YES;
    }
    self.subtitleView.alpha = 1;
}

- (void)autoDetectSubtitles
{
/******************************字幕检测*******************************/
    NSString *URLString = [self.myPlayer.contentURL absoluteString];
    NSString *fileFullName = [URLString lastPathComponent];
    NSString *filePath = [[[URLString componentsSeparatedByString:fileFullName]objectAtIndex:0] mutableCopy];
    NSString *fileName = [[[fileFullName componentsSeparatedByString:[fileFullName pathExtension]]objectAtIndex:0] mutableCopy];
    NSDirectoryEnumerator *fileEnumerator = [[NSFileManager defaultManager] enumeratorAtPath:filePath];
    NSString *file = nil;
    if(self.subtitleArray != nil)self.subtitleArray = nil;
    self.subtitleArray = [NSMutableArray array];
    while (file = [fileEnumerator nextObject])
    {
        if([file hasSuffix:@".ass"]||[file hasSuffix:@".srt"])
        {
            if([file hasPrefix:fileName])
            {
                NSMutableString *subtitleUrl = [filePath mutableCopy];
                [subtitleUrl appendString:file];
                [self.subtitleArray addObject:subtitleUrl];
            }
        }
    }
/********************************************************************/
}

- (void)autoMatchSubtitle
{
/******************************自动加载*******************************/
    [self autoDetectSubtitles];
    NSString* subtitleURL;
    if(self.subtitleArray.count>0)
    {
        subtitleURL = [self.subtitleArray objectAtIndex:0];
        [self subtitleWithAddress:subtitleURL];
    }
/********************************************************************/
}

- (void)dealloc
{
    self.subtitleView = nil;
    if(subtitle != NULL) subtitle -> close();
    if(self.myTimer != nil)
    {
        [self.myTimer invalidate];
        self.myTimer = nil;
    }
    [self removeNotifications];
    notificationFlag = NO;
}

#pragma mark Cloud Subtitle
/****************************云字幕实现****************************/
- (void)autoCloudSubtitle
{
    dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"正在连接服务器";});
    /****************************头部信息*******************************/
    NSURL *myUrl=[NSURL URLWithString:@"http://svplayer.shooter.cn/api/subapi.php"];//射手服务器URL
    NSMutableURLRequest *myRequest=[NSMutableURLRequest requestWithURL:myUrl];
    
    NSString *userAgent=@"SPlayer Build 2437";//伪装射手播放器
    [myRequest setValue:userAgent forHTTPHeaderField:@"User-Agent"];
    NSString *myBoundary=@"--------------------------767a02e50d82";
    NSString *myContent=[NSString stringWithFormat:@"multipart/form-data;boundary=%@",myBoundary];
    [myRequest setValue:myContent forHTTPHeaderField:@"Content-type"];
    NSString *host=@"svplayer.shooter.cn";
    [myRequest setValue:host forHTTPHeaderField:@"Host"];
    NSString *expect=@"100-continue";
    [myRequest setValue:expect forHTTPHeaderField:@"Expect"];
    NSString *connection=@"Keep-Alive";
    [myRequest setValue:connection forHTTPHeaderField:@"Connection"];
    /*******************************************************************/
    NSMutableData * body=[NSMutableData data];
    NSString *filePath = [self.myPlayer.contentURL absoluteString];
    Byte buffer[4096];
    NSMutableString * hashString = [[NSMutableString alloc]init];
    std::fstream openFile([filePath UTF8String],std::ios::in|std::ios::binary);
    if(openFile)
    {
        openFile.seekp(0,openFile.end);
        int length = (int)openFile.tellg();
        openFile.seekp(4096);
        openFile.read((char *)buffer, 4096);
        [hashString appendString:[[PPPlayerUtil sharedInstance] MD5Hash:buffer bufferSize:4096]];
        [hashString appendString:@";"];
        openFile.seekp(length/3*2);
        openFile.read((char *)buffer, 4096);
        [hashString appendString:[[PPPlayerUtil sharedInstance] MD5Hash:buffer bufferSize:4096]];
        [hashString appendString:@";"];
        openFile.seekp(length/3);
        openFile.read((char *)buffer, 4096);
        [hashString appendString:[[PPPlayerUtil sharedInstance] MD5Hash:buffer bufferSize:4096]];
        [hashString appendString:@";"];
        openFile.seekp(length - 8192);
        openFile.read((char *)buffer, 4096);
        [hashString appendString:[[PPPlayerUtil sharedInstance] MD5Hash:buffer bufferSize:4096]];
        openFile.close();
    }
    [body appendData:[[NSString stringWithFormat:@"\n--%@\n",myBoundary] dataUsingEncoding:NSUTF8StringEncoding]];
    [body appendData:[@"Content-Disposition:form-data;name='filehash'\n\n" dataUsingEncoding:NSUTF8StringEncoding]];
    [body appendData:[hashString dataUsingEncoding:NSUTF8StringEncoding]];
    [body appendData:[[NSString stringWithFormat:@"\n--%@--\n",myBoundary]dataUsingEncoding:NSUTF8StringEncoding]];
    
    NSString *contentLength=[NSString stringWithFormat:@"%d",[body length]];
    [myRequest setValue:contentLength forHTTPHeaderField:@"Content-Length"];
    
    [myRequest setHTTPMethod:@"POST"];
    [myRequest setHTTPBody:body];
    
    NSURLConnection *myconnection = [[NSURLConnection alloc] initWithRequest:myRequest delegate:self];
}

//接收到服务器回应的时候调用此方法
- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
    NSHTTPURLResponse *res = (NSHTTPURLResponse *)response;
    responseData = [NSMutableData data];
    int responseCode = [res statusCode];
    if(responseCode != 200)
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"服务器连接失败";
        [NSTimer scheduledTimerWithTimeInterval:2.0 target:self selector:@selector(DelayShield) userInfo:nil repeats:NO];});
    }
//    NSLog(@"%ld",(long)[res statusCode]);
//    NSLog(@"%@",[res allHeaderFields]);
}

//接收到服务器传输数据的时候调用，此方法根据数据大小执行若干次
-(void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{    
    [responseData appendData:data];    
}

//数据传完之后调用此方法
-(void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    Byte *dataBytes = (Byte *)[responseData bytes];
    SignedByte statCode = dataBytes[0];
    if(statCode < 0)
    {
        if(statCode == -1)
        {
            dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"没有找到字幕";});
        }
        else
        {
            dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"数据传输失败";});
        }
        dispatch_async(dispatch_get_main_queue(), ^{[NSTimer scheduledTimerWithTimeInterval:2.0 target:self selector:@selector(DelayShield) userInfo:nil repeats:NO];});
    }
     else
     {
        dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"已连接到服务器";});
         dataBytes = &dataBytes[1];
         for(int i = 0;i<statCode;i++)
         {
             [self handleSubPackege:dataBytes number:i];
         }
         if(cloudSubFlag == YES)
         {
             cloudSubFlag = NO;
             [self autoMatchSubtitle];
            [[NSNotificationCenter defaultCenter] postNotificationName:PPCloudSubtitleSuccessNotification object:nil];
         }
     }
}

- (void)handleSubPackege:(Byte *)data number:(int)num
{
    long packegeLen = data[0]*16777216+data[1]*65536+data[2]*256+data[3];
    if(packegeLen < 10000)
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"没有找到字幕";
            [NSTimer scheduledTimerWithTimeInterval:2.0 target:self selector:@selector(DelayShield) userInfo:nil repeats:NO];});
        data = &data[packegeLen];
        return;
    }
    else
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"字幕已找到，即将载入";});
        cloudSubFlag = YES;
    }
    long descLen = data[4]*16777216+data[5]*65536+data[6]*256+data[7];
    NSUInteger cursor = 8+descLen;
    cursor = cursor +4;
    NSUInteger numOfFiles = data[cursor++];
    data = &data[cursor];
    for(int i = 0;i<numOfFiles;i++)
    {
        [self handleSingleSub:data number:i+num*10];
    }
}

- (void)handleSingleSub:(Byte *)data number:(int)num
{ 
    NSString *URLString = [self.myPlayer.contentURL absoluteString];
    NSString *fileFullName = [URLString lastPathComponent];
    NSString *filePath = [[[URLString componentsSeparatedByString:fileFullName]objectAtIndex:0] mutableCopy];
    NSString *fileName = [[[fileFullName componentsSeparatedByString:[fileFullName pathExtension]]objectAtIndex:0] mutableCopy];
    NSMutableString *subFilePath = [[NSMutableString alloc] initWithString:filePath];
    [subFilePath appendString:fileName];
    [subFilePath appendFormat:@"%d.",num+1];
    
    NSUInteger extLen = data[7];
    Byte extName[extLen];
    for(int i=0;i<extLen;i++)extName[i] = data[i+8];
    NSString *extString = [[NSString alloc]initWithBytes:extName length:extLen encoding:NSUTF8StringEncoding];
    [subFilePath appendString:extString];
    NSUInteger cursor = 8+extLen;
    NSUInteger fileLen = data[cursor]*16777216+data[cursor+1]*65536+data[cursor+2]*256+data[cursor+3];
    cursor = cursor +4;
    if((data[cursor] == 31)&&(data[cursor+1] == 139)&&(data[cursor+2] == 8))
    {
        NSData * tmp = [[NSData alloc] initWithBytes:&data[cursor] length:fileLen];
        tmp = [[PPPlayerUtil sharedInstance] decompressGZIP:tmp dataLength:fileLen];
        [tmp writeToFile:subFilePath atomically:YES];
    }
    else
    {
        NSData * tmp = [[NSData alloc] initWithBytes:&data[cursor] length:fileLen];
        [tmp writeToFile:subFilePath atomically:YES];
    }
    data = &data[cursor+fileLen];
}

//网络请求过程中，出现任何错误（断网，连接超时等）会进入此方法
-(void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
    dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"网络传输错误";
    [NSTimer scheduledTimerWithTimeInterval:2.0 target:self selector:@selector(DelayShield) userInfo:nil repeats:NO];});
}

- (void)DelayShield
{
    dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"";});
}

#pragma mark Handle Events
- (void)playerPrepared
{
    dispatch_async(dispatch_get_main_queue(), ^{self.myTimer = [NSTimer scheduledTimerWithTimeInterval:0.1 target:self selector:@selector(showSubtitle) userInfo:nil repeats:YES];[self.myTimer fire];});
}

- (void)seekComplete
{
    dispatch_async(dispatch_get_main_queue(), ^{[NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(seekDelay) userInfo:nil repeats:NO];});
}

- (void)seekDelay
{
    if(subtitle == NULL)return;
    nowTime = self.myPlayer.currentPlaybackTime*10;
    subtitle -> seekTo(nowTime*100);
    subtitle -> getNextSubtitleSegment(&segment);
    startTime = (segment -> getStartTime())/100;
    stopTime = (segment -> getStopTime())/100;
    while(stopTime - startTime <10)
    {
        subtitle -> getNextSubtitleSegment(&segment);
        startTime = (segment -> getStartTime())/100;
        stopTime = (segment -> getStopTime())/100;
    }
    segment -> getSubtitleText(subtitleText, 1024);
    if((nowTime >= startTime)&&(nowTime < stopTime))
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = [NSString stringWithUTF8String:subtitleText];});
    }
    else
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"";});
    }
}

- (void)showSubtitle
{
    if(nowTime == startTime)
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = [NSString stringWithUTF8String:subtitleText];});
        if(delay == YES)
        {
            nowTime++;
            delay = NO;
        }
    }
    if(nowTime == stopTime)
    {
        dispatch_async(dispatch_get_main_queue(), ^{self.subtitleView.text = @"";});
        subtitle -> getNextSubtitleSegment(&segment);
        startTime = (segment -> getStartTime())/100;
        stopTime = (segment -> getStopTime())/100;
        while(stopTime - startTime <10)//去掉展示时间小于1s的
        {
            subtitle -> getNextSubtitleSegment(&segment);
            startTime = (segment -> getStartTime())/100;
            stopTime = (segment -> getStopTime())/100;
        }
        segment -> getSubtitleText(subtitleText, 1024);
        nowTime--;
        delay = YES;
    }
    nowTime++;
}

- (void)playbackComplete
{
    if(self.myTimer != nil)
    {
        [self.myTimer invalidate];
        self.myTimer = nil;
    }
}

- (void)applicationWillResignActive
{
    if(self.myTimer != nil)
    {
        [self.myTimer invalidate];
        self.myTimer = nil;
    }
}

- (void)playerPaused
{
    if(self.myTimer != nil)
    {
        [self.myTimer invalidate];
        self.myTimer = nil;
    }
}

- (void)playerRestart
{
    dispatch_async(dispatch_get_main_queue(), ^{self.myTimer = [NSTimer scheduledTimerWithTimeInterval:0.1 target:self selector:@selector(showSubtitle) userInfo:nil repeats:YES];[self.myTimer fire];});
}


#pragma mark Notifications
- (void)removeNotifications
{
    [[NSNotificationCenter defaultCenter] removeObserver:self name:PPMoviePlayerPausedNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:PPMoviePlayerRestartNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:PPMoviePlayerPreparedNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:PPMoviePlayerSeekCompleteNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:PPMoviePlayerPlaybackCompleteNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillResignActiveNotification object:nil];
}

- (void)addNotifications
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playerPaused) name:PPMoviePlayerPausedNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playerRestart) name:PPMoviePlayerRestartNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playerPrepared) name:PPMoviePlayerPreparedNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(seekComplete) name:PPMoviePlayerSeekCompleteNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playbackComplete) name:PPMoviePlayerPlaybackCompleteNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(applicationWillResignActive) name:UIApplicationWillResignActiveNotification object:nil];
}

@end
