//
//  PPPlayerLog.h
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-8-8.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <Foundation/Foundation.h>


@interface PPPlayerLog : NSObject

//诊断信息类初始化
- (id)init;

//改变诊断信息文件存放地址
- (void)setLogPath:(NSString*)logPath;

//写入log信息
- (void)writeLog:(NSString*)logContent;

//诊断播放器
- (bool)test;

@end
