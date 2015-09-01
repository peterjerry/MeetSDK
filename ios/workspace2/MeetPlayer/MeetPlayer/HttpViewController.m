//
//  HttpViewController.m
//  MeetPlayer
//
//  Created by Eason Zhao on 15/3/4.
//  Copyright (c) 2015年 Eason. All rights reserved.
//

#import "HttpViewController.h"
#import "PlaybackViewController.h"

@interface HttpViewController ()<UIWebViewDelegate>
@property (weak, nonatomic) IBOutlet UIWebView *webView_;
@end

@implementation HttpViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)loadWeb
{
    NSURL *url = [[NSURL alloc]initWithString:@"http://172.16.204.106/test/testcase"];
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
/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
