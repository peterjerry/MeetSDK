//
//  PPLocalMusicViewController.m
//  PPlayer
//
//  Created by stephenzhang on 13-10-15.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "PPMusicViewController.h"

@interface PPMusicViewController ()

@end

@implementation PPMusicViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self)
    {
        [self.tabBarItem setFinishedSelectedImage:[UIImage imageNamed:@"phone_bottom_icon_music_sel"] withFinishedUnselectedImage:[UIImage imageNamed:@"phone_bottom_icon_music"]];
        [self.tabBarItem setTitle:@"音乐"];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
