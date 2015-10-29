//
//  BaiduTableViewController.h
//  MeetPlayer
//
//  Created by pptv on 15/10/12.
//  Copyright (c) 2015年 Eason. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "BaiduOAuth.h"
#import "BaiduPCSClient.h"
#import "baiduPCSStatusListener.h"
@interface BaiduTableViewController : UITableViewController

@property (copy, nonatomic) NSArray *dwarves;
@property (retain, nonatomic) NSMutableArray *listItem;

// token
@property (strong, nonatomic) NSString *mpToken;
@property (strong, nonatomic) BaiduPCSClient *client;
@end
