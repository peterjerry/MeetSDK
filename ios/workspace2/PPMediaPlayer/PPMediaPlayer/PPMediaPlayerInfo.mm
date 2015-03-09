//
//  PPMediaPlayerInfo.m
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-7-11.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "player.h"
#import "PPMediaPlayerInfo.h"

static PPMediaPlayerInfo *sharedObj = nil;
static IPlayer *player = nil;

@interface PPMediaPlayerInfo()
{
    PPMediaInfo myMediaInfo;
    MediaInfo info;
    NSString* myURL;
}
@end

@implementation PPMediaPlayerInfo

+ (id)sharedInstance
{
    @synchronized(self)
    {
        if(sharedObj == nil)
        {
            sharedObj = [[self alloc] init];
            player = getPlayer(NULL);
        
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

- (void)dealloc
{
    if(player != NULL)
    {
        delete player;
        player = NULL;
    }
}

- (UIImage*)getThumbnail:(NSString*)url
{
    MediaInfo mInfo;
    player -> getThumbnail([url UTF8String],&mInfo);
    int width = mInfo.thumbnail_width;
    int height = mInfo.thumbnail_height;
    CGContextRef bitmapContext = CGBitmapContextCreate(mInfo.thumbnail, width, height, 8, 4*height, CGColorSpaceCreateDeviceRGB(), kCGImageAlphaPremultipliedLast);
    
    CGImageRef imageRef = CGBitmapContextCreateImage(bitmapContext);
    UIImage *image = [[UIImage alloc] initWithCGImage:imageRef];
    
    CGImageRelease(imageRef);
    CGContextRelease(bitmapContext);
    
    return image;
}

- (void)setMediaURL:(NSString*)url
{
   player -> getMediaDetailInfo([url UTF8String],&info);
    myURL = url;
}

- (PPMediaInfo)getMediaInfo:(NSString*)url
{
    if(myURL == url) {
        myMediaInfo.height = info.height;
        myMediaInfo.width = info.width;
        myMediaInfo.duration = info.duration_ms;
        if (info.audio_channels>0) {
            strcpy(myMediaInfo.audio_name, info.audiocodec_names[0]);
        }
        strcpy(myMediaInfo.video_name, info.videocodec_name);
        myMediaInfo.audio_channels = info.audio_channels;
        myMediaInfo.video_channels = info.video_channels;
    } else {
        MediaInfo myInfo;
        player -> getMediaDetailInfo([url UTF8String],&myInfo);
        myMediaInfo.height = myInfo.height;
        myMediaInfo.width = myInfo.width;
        myMediaInfo.duration = myInfo.duration_ms;
        if (myInfo.audio_channels>0) {
            strcpy(myMediaInfo.audio_name, myInfo.audiocodec_names[0]);
        }
        strcpy(myMediaInfo.video_name, myInfo.videocodec_name);
        myMediaInfo.audio_channels = myInfo.audio_channels;
        myMediaInfo.video_channels = myInfo.video_channels;
        
    }
    return myMediaInfo;
}

- (NSMutableDictionary*)getAudioChannel
{
    NSMutableDictionary * audioDictionary = [[NSMutableDictionary alloc] init];
    for(int i = 0;i < info.channels;i++)
    {
        if(strlen(info.audio_languages[i]) != 0)
        {
            if(strlen(info.audio_titles[i]) != 0)[audioDictionary setValue:[NSString stringWithCString:info.audio_titles[i] encoding:NSUTF8StringEncoding] forKey:[NSString stringWithFormat:@"%d",i]];
            else[audioDictionary setValue:[NSString stringWithCString:info.audio_languages[i] encoding:NSUTF8StringEncoding] forKey:[NSString stringWithFormat:@"%d",i]];
        }
    }
    return audioDictionary;
}

- (NSString*)getPlayerVersion
{
    return playerVersion;
}

@end
