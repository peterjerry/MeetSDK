//
//  PPPlayerLog.m
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-8-8.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <fstream>
#import <sys/sysctl.h>
#import <UIKit/UIKit.h>
#import "PPPlayerLog.h"

@interface PPPlayerLog()
{
    NSUserDefaults *userPlist;
    bool notFirst;//app是否第一次使用
    NSString *logFilePath;//日志文件存储位置
    int logFileCurPos;//写入文件位置游标
}
@end

@implementation PPPlayerLog

- (id)init
{
    if(self = [super init])
    {
        userPlist = [NSUserDefaults standardUserDefaults];
        notFirst = [userPlist boolForKey:@"notFirst"];
        if(notFirst == NO)//第一次使用app时创建日志文件及信息
        {
            NSArray *appPaths = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES);
            NSString *appPath = [appPaths objectAtIndex:0];
            NSString *playerLogFilePath = [appPath stringByAppendingPathComponent:@"playerlog.text"];
            [userPlist setObject:playerLogFilePath forKey:@"logFilePath"];
            [userPlist setInteger:5000 forKey:@"logFileCurPos"];
            [userPlist setBool:YES forKey:@"notFirst"];
            [[NSFileManager defaultManager] createFileAtPath:playerLogFilePath contents:nil attributes:nil];
        }
        logFilePath = [userPlist objectForKey:@"logFilePath"];
        logFileCurPos = [userPlist integerForKey:@"logFileCurPos"];
    }
    return self;
}

- (void)setLogPath:(NSString*)logPath
{
    NSError *error;
    if([[NSFileManager defaultManager] moveItemAtPath:logFilePath toPath:logPath error:&error])
    {
        [[NSFileManager defaultManager] removeItemAtPath:logFilePath error:&error];
        [userPlist setObject:logPath forKey:@"logFilePath"];
        logFilePath = [logPath copy];
    }
}

- (void)writeLog:(NSString*)logContent
{
    //log信息写入文件，使用c++语法
    std::ofstream openLogFile([logFilePath UTF8String],std::ios::out|std::ios::ate|std::ios::in);
    if(!openLogFile)return;
    openLogFile.seekp(logFileCurPos);
    openLogFile<<[logContent UTF8String]<<std::endl;
    if((int)openLogFile.tellp()>99000)
    {
        [userPlist setInteger:5000 forKey:@"logFileCurPos"];
    }
    else
    {
        [userPlist setInteger:(int)openLogFile.tellp() forKey:@"logFileCurPos"];
    }
    openLogFile.close();
}

- (bool)test
{
    NSString *iosVer = [[UIDevice currentDevice] systemVersion];
    NSString *appVer = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"];
    size_t size;
    sysctlbyname("hw.machine", NULL, &size, NULL, 0);
    char *machine = (char *)malloc(size);
    sysctlbyname("hw.machine", machine, &size, NULL, 0);
    NSString *platform = [NSString stringWithUTF8String:machine];
    free(machine);
    
    std::ofstream openLogFile([logFilePath UTF8String],std::ios::out|std::ios::ate|std::ios::in);
    if(openLogFile)
    {
        openLogFile.seekp(0);
        openLogFile<<"iosVer:"<<[iosVer UTF8String]<<std::endl;
        openLogFile<<"appVer:"<<[appVer UTF8String]<<std::endl;
        openLogFile<<"platform:"<<[platform UTF8String]<<std::endl;
        openLogFile<<"playerVer:"<<[playerVersion UTF8String]<<std::endl;
        openLogFile.close();
    }    
    return YES;
}

@end
