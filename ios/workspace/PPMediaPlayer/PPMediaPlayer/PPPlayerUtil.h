//
//  PPPlayerUtil.h
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-8-14.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface PPPlayerUtil : NSObject

//单例初始化
+ (id)sharedInstance;

//MD5哈希算法
- (NSString *)MD5Hash:(Byte *)data bufferSize:(int)size;

//GZIP解压
- (NSData *)decompressGZIP:(NSData *)data dataLength:(unsigned)length;

@end
