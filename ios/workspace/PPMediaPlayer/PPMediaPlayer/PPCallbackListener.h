//
//  PPCallbackListener.h
//  PPMediaPlayer
//
//  Created by stephenzhang on 13-7-1.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "player.h"
#import "PPMoviePlayerController.h"


class playerListener : public MediaPlayerListener
{    
public:
    void notify(int msg, int ext1, int ext2);    
};