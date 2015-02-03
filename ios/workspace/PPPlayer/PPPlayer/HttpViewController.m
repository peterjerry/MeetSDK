//
//  HttpViewController.m
//  PPPlayer
//
//  Created by zenzhang on 14-12-10.
//  Copyright (c) 2014年 pplive. All rights reserved.
//

#import "HttpViewController.h"
#import "PlaybackViewController.h"

@interface HttpViewController ()<UIWebViewDelegate>

@property (weak, nonatomic) IBOutlet UIWebView *webView_;

@end

@implementation HttpViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    if ([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        self = [super initWithNibName:@"HttpViewController_ipad" bundle:nil];
    } else {
        self = [super initWithNibName:@"HttpViewController" bundle:nil];
    }
    if (self) {
        // Custom initialization
        [self.tabBarItem setFinishedSelectedImage:[UIImage imageNamed:@"phone_bottom_icon_video_sel"] withFinishedUnselectedImage:[UIImage imageNamed:@"phone_bottom_icon_video"]];
        [self.tabBarItem setTitle:@"http"];
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

- (void)loadWeb
{
    NSURL *url = [[NSURL alloc]initWithString:@"http://172.16.204.104"];
    [self.webView_ loadRequest:[NSURLRequest requestWithURL:url]];
}

- (void)viewDidAppear:(BOOL)animated
{
    [self loadWeb];
}

- (BOOL)webView:(UIWebView *)webView
shouldStartLoadWithRequest:(NSURLRequest *)request
 navigationType:(UIWebViewNavigationType)navigationType
{
    switch (navigationType) {
        case UIWebViewNavigationTypeLinkClicked:
        {
            if ([request.URL.pathExtension isEqualToString:@""]) {
                return YES;
            }
            NSLog(@"[HttpViewController] [webView]: url:%@", [request.URL absoluteString]);
            PlaybackViewController *playerViewContorller = [[PlaybackViewController alloc] initWithUrl:request.URL];
            [self presentViewController:playerViewContorller animated:NO completion:nil];
            return NO;
        }
            break;
            
        default:
            break;
    }
    return YES;
}

@end
