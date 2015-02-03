//
//  PPWebViewController.m
//  PPlayer
//
//  Created by zenzhang on 14-10-22.
//  Copyright (c) 2014年 Stephen Zhang. All rights reserved.
//

#import "PPWebViewController.h"
#import "PPPlayerViewController.h"

@interface PPWebViewController ()<UIWebViewDelegate>
{
    __weak IBOutlet UIWebView *webView_;

}

@end

@implementation PPWebViewController



- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
        [self.tabBarItem setFinishedSelectedImage:[UIImage imageNamed:@"phone_bottom_icon_video_sel"] withFinishedUnselectedImage:[UIImage imageNamed:@"phone_bottom_icon_video"]];
        [self.tabBarItem setTitle:@"网络"];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.
}

- (void)loadWeb
{
    NSURL *url = [[NSURL alloc]initWithString:@"http://192.168.14.205/test.htm"];
    //NSURL *url = [[NSURL alloc]initWithString:@"http://172.16.204.104"];
    [webView_ loadRequest:[NSURLRequest requestWithURL:url]];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)viewDidAppear:(BOOL)animated
{
    [self loadWeb];
}

- (void)webView:(UIWebView *)webView
shouldStartLoadWithRequest:(NSURLRequest *)request
 navigationType:(UIWebViewNavigationType)navigationType
{
    if (navigationType == UIWebViewNavigationTypeLinkClicked) {
        NSString *url = [NSString stringWithFormat:@"%@://%s%@?%@",
                         [request URL].scheme,
                         "127.0.0.1:9006",
                         [request URL].path,
                         [request URL].query];
        PPPlayerViewController *playerViewController = [[PPPlayerViewController alloc] initWithPlayURL:url];
        [self presentViewController:playerViewController animated:NO completion:nil];
    }
}
@end
