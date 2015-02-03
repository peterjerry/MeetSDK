//
//  PPViewController.m
//  PPlayer
//
//  Created by stephenzhang on 13-10-14.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "PPViewController.h"

@interface PPViewController ()

@end

@implementation PPViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self)
    {
        //tabBar背景
        self.tabBar.backgroundImage = [UIImage imageNamed:@"phone_bottom_button_BG"];
        self.tabBar.selectionIndicatorImage = [[UIImage alloc] init];
        //TabBarItem文字
        NSDictionary *normalTextAttrs = @{
                                          UITextAttributeTextColor : [UIColor colorWithRed:100/255.0
                                                                                     green:100/255.0 blue:100/255.0 alpha:1.0],
                                          UITextAttributeFont : [UIFont systemFontOfSize:11.0f]
                                          };
        [[UITabBarItem appearance] setTitleTextAttributes:normalTextAttrs forState:UIControlStateNormal];
        
        NSDictionary *selectedTextAttrs = @{
                                            UITextAttributeTextColor : [UIColor colorWithRed:0/255.0
                                                                                       green:107/255.0 blue:217/255.0 alpha:1.0],
                                            UITextAttributeFont : [UIFont systemFontOfSize:11.0f]
                                            };
        [[UITabBarItem appearance] setTitleTextAttributes:selectedTextAttrs forState:UIControlStateSelected];        
        [[UITabBarItem appearance] setTitlePositionAdjustment:UIOffsetMake(0.f, -4.f)];
        //titleBG
        UIImageView *topView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"iphone_home_titlebar"]];
        CGFloat width = self.view.frame.size.width;
        topView.frame = CGRectMake(0, 20, width, 43);
        [self.view addSubview:topView];
        //title
        UILabel *title = [[UILabel alloc] initWithFrame:CGRectMake((width - 170) / 2, 29, 170, 24)];
        title.backgroundColor = [UIColor clearColor];
        title.textAlignment = UITextAlignmentCenter;
        title.textColor = [UIColor whiteColor];
        title.text = @"我的资源";
        [self.view addSubview:title];

//        UIButton *backButton = [[UIButton alloc] initWithFrame:CGRectMake(0, 24, 36, 36)];
//        [backButton setImage:[UIImage imageNamed:@"iphone_channel_Longvideo_back_button"] forState:UIControlStateNormal];
//        [self.view addSubview:backButton];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (NSUInteger)supportedInterfaceOrientations
{
    return UIInterfaceOrientationMaskPortrait;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)orientation
{
    return NO;
}

@end
