//
//  PPPlayerUtil.m
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-8-14.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "PPPlayerUtil.h"
#import <CommonCrypto/CommonDigest.h>
#import <zlib.h>

static PPPlayerUtil *sharedObj = nil;

@implementation PPPlayerUtil

+ (id)sharedInstance
{
    @synchronized(self)
    {
        if(sharedObj == nil)
        {
            sharedObj = [[self alloc] init];
        }
    }
    return sharedObj;
}

+ (id)allocWithZone:(NSZone *)zone
{
    @synchronized (self)
    {
        if (sharedObj == nil)
        {
            sharedObj = [super allocWithZone:zone];
            return sharedObj;
        }
    }
    return nil;
}

- (id)copyWithZone:(NSZone *)zone
{
    return self;
}

- (NSString *)MD5Hash:(Byte *)data bufferSize:(int)size
{
    CC_MD5_CTX hashObject;
    CC_MD5_Init(&hashObject);
    CC_MD5_Update(&hashObject,(const void *)data,size);
    unsigned char digest[CC_MD5_DIGEST_LENGTH];
    CC_MD5_Final(digest, &hashObject);
    char hash[2 * sizeof(digest) + 1];
    for (size_t i = 0;i < sizeof(digest);++i)
    {
        snprintf(hash + (2 * i), 3, "%02x", (int)(digest[i]));
    }
    NSString *result = [[NSString alloc]initWithUTF8String:(const char *)hash];
    return result;
}

- (NSData *)decompressGZIP:(NSData *)data dataLength:(unsigned)length
{    
    unsigned full_length = length;    
    unsigned half_length = length / 2;
    NSMutableData *decompressed = [NSMutableData dataWithLength: full_length + half_length];
    int status;
    z_stream strm;
    strm.next_in = (Bytef *)[data bytes];
    strm.avail_in = length;
    strm.total_out = 0;
    strm.zalloc = Z_NULL;
    strm.zfree = Z_NULL;
    if (inflateInit2(&strm, (15+32)) != Z_OK) return nil;
    BOOL done = NO;
    while (!done)
    {
        if (strm.total_out >= [decompressed length])
        {
            [decompressed increaseLengthBy:half_length];
        }
        strm.next_out = (Bytef*)[decompressed mutableBytes] + strm.total_out;
        strm.avail_out = [decompressed length] - strm.total_out;
        status = inflate (&strm, Z_SYNC_FLUSH);
        if (status == Z_STREAM_END) done = YES;
        else if (status != Z_OK) break;        
    }
    if (inflateEnd (&strm) != Z_OK) return nil;
    if (done)
    {
        [decompressed setLength: strm.total_out];
        NSData *data = [[NSData alloc] initWithData: decompressed];
        return data;
    }
    else
        return data;
}



@end
