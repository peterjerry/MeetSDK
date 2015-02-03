//
//  MoviesViewController.m
//  PPPlayer
//
//  Created by zenzhang on 14-11-25.
//  Copyright (c) 2014年 pplive. All rights reserved.
//

#import "MoviesViewController.h"
#import "PPMovieViewCell.h"
#import "PPMediaPlayer/PPMediaPlayer.h"
#import "PlaybackViewController.h"

@interface MoviesViewController ()
{
    NSString *documentPath_;
    NSMutableArray *images_;
    NSMutableArray *infos_;
}

@property (weak, nonatomic) IBOutlet UITableView *tableView_;

@end

@implementation MoviesViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
        [self.tabBarItem setFinishedSelectedImage:[UIImage imageNamed:@"phone_bottom_icon_video_sel"] withFinishedUnselectedImage:[UIImage imageNamed:@"phone_bottom_icon_video"]];
        [self.tabBarItem setTitle:@"视频"];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [[UIApplication sharedApplication] setStatusBarStyle:UIStatusBarStyleLightContent];
    UINib *nib = [UINib nibWithNibName:@"PPMovieViewCell" bundle:nil];
    [self.tableView_ registerNib:nib forCellReuseIdentifier:@"PPMovieViewCell"];
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    documentPath_ = [paths objectAtIndex:0];
	[self searchFiles];
    images_ = [[NSMutableArray alloc] init];
    infos_ = [[NSMutableArray alloc] init];
    // Do any additional setup after loading the view from its nib.
}

- (void)searchFiles
{
    if(self.movieArray != nil) {
        self.movieArray = nil;
    }
    self.movieArray = [NSMutableArray array];
    
    NSDirectoryEnumerator *fileEnumerator = [[NSFileManager defaultManager] enumeratorAtPath:documentPath_];
    NSString *fileName = nil;
    NSArray *format = @[@".mp4", @".mkv", @".rmvb", @".avi", @".m3u8", @".wmv", @".flv", @".rm", @".m4v", @".divx", @".3gp", @".m2ts", @".mpeg", @".ts", @".mp3"];
    while (fileName = [fileEnumerator nextObject]) {
        __block BOOL rel = NO;
        [format enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop){
             if ([[fileName lowercaseString] hasSuffix:obj]) {
                 *stop = YES;
                 rel = YES;
             }
        }];
        if(rel == YES) {
            [self.movieArray addObject:fileName];
        }
    }
}

- (NSInteger)tableView:(UITableView*)tableView numberOfRowsInSection:(NSInteger)section
{
    return [self.movieArray count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    PPMovieViewCell *cell = nil;
    /*
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
        cell = [tableView dequeueReusableCellWithIdentifier:@"PPMovieViewCell"];
    } else {
        cell =  [tableView dequeueReusableCellWithIdentifier:@"PPMovieViewCell_ipad"];
    }*/
    cell = [tableView dequeueReusableCellWithIdentifier:@"PPMovieViewCell"];
    [cell.movieImage setContentMode:UIViewContentModeScaleToFill];
    cell.movieTitle.text = [[self.movieArray objectAtIndex:[indexPath row]] lastPathComponent];
    NSString *url = [documentPath_ stringByAppendingPathComponent:[self.movieArray
                                                                 objectAtIndex:[indexPath row]]];
    PPMediaInfo info;
    if ([indexPath row] >= images_.count) {
        UIImage *img = [[PPMediaPlayerInfo sharedInstance] getThumbnail:url];
        NSLog(@"%f * %f", img.size.width, img.size.height);
        [images_ addObject:img];
        info = [[PPMediaPlayerInfo sharedInstance] getMediaInfo:url];
        NSValue *userValue = [NSValue valueWithBytes:&info objCType:@encode(PPMediaInfo)];
        [infos_ addObject:userValue];
    } else {
        NSValue *userValue = [infos_ objectAtIndex:[indexPath row]];
        [userValue getValue:&info];
    }
    cell.imageView.image = [images_ objectAtIndex:[indexPath row]];
    
    //NSLog(@"%f*%f", cell.imageView.frame.size.width, cell.imageView.frame.size.height);
    cell.timeLabel.text = [NSString stringWithFormat:@"时间：%d min",(info.duration)/60000];
    cell.sizeLabel.text = [NSString stringWithFormat:@"分辨率：%d×%d",info.width,info.height];
    cell.videoCoding.text = [NSString stringWithFormat:@"视频编码：%s",info.video_name];
    cell.audioCoding.text = [NSString stringWithFormat:@"音频编码：%s",info.audio_name];
    return cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    //PPMovieViewCell *cell = [self.tableView_ dequeueReusableCellWithIdentifier:@"PPMovieViewCell"];
    return 115;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    //NSString *filePath = [NSString stringWithFormat:@"file:%@/%@", documentPath_, [self.movieArray objectAtIndex:indexPath.row]];
    NSString *filePath = [documentPath_ stringByAppendingPathComponent:[self.movieArray objectAtIndex:[indexPath row]]];
    NSURL *url = [NSURL fileURLWithPath:filePath];
    PlaybackViewController *playerViewContorller = [[PlaybackViewController alloc] initWithUrl:url];
    [self presentViewController:playerViewContorller animated:NO completion:nil];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
