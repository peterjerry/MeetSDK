//
//  PPboxViewController.m
//  MeetPlayer
//
//  Created by Eason Zhao on 15/3/9.
//  Copyright (c) 2015年 Eason. All rights reserved.
//

#import "PPboxViewController.h"
#import "MovieCell.h"
#import "PlaybackViewController.h"

@interface PPboxViewController ()
@property (weak, nonatomic) IBOutlet UITableView *tableView_;
@end

@implementation PPboxViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    UINib *nib = [UINib nibWithNibName:@"MovieCell" bundle:nil];
    [self.tableView_ registerNib:nib
          forCellReuseIdentifier:@"MovieCell"];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return 1;
}

- (UITableViewCell *)tableView:(UITableView *)tableView
         cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    MovieCell *cell = nil;
    cell = [tableView dequeueReusableCellWithIdentifier:@"MovieCell"];
    cell.title.text = @"test";
    return cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    //PPMovieViewCell *cell = [self.tableView_ dequeueReusableCellWithIdentifier:@"PPMovieViewCell"];
    return 115;
}

- (void)tableView:(UITableView *)tableView
didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSString *str = [NSString stringWithUTF8String:"http://127.0.0.1:9006/record.m3u8?type=ppvod2&mux.M3U8.segment_duration=5&playlink=19973855%3Fft%3D1%26accessType%3Dwifi%26k_ver%3D2.5.0.8565%26bakplayhost%3D211.151.82.252%26type%3Dphone.ios.vip%26sv%3D4.2.3%26channel%3D1002%26p2p.vip%3D1%26username%3Daplman%26param%3Dgeoid%253D0000000000%2526userType%253D1%26platform%3Diphone4%26gslbversion%3D2%26vvid%3D0CE87B07-1343-41E6-8181-615164DD8F05%26version%3D4%26userLevel%3D1%26sv%3D4.2.3%26k_ver%3D1.1.0.8565%26auth%3D55b7c50dc1adfc3bcabe2d9b2015e35c%26content%3Dneed_drag&vvid=0CE87B07-1343-41E6-8181-615164DD8F05&serialnum=12"];
    NSURL *url = [NSURL URLWithString:str];
    NSString *str2 = [url absoluteString];
    PlaybackViewController *playerViewContorller = [[PlaybackViewController alloc] initWithUrl:url];
    [self presentViewController:playerViewContorller
                       animated:NO
                     completion:nil];
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
