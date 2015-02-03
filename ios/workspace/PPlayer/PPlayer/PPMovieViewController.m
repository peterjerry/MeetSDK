//
//  PPViewController.m
//  PPlayer
//
//  Created by stephenzhang on 13-9-5.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "PPMovieViewController.h"
#import "PPPlayerViewController.h"
#import "PPMovieViewCell.h"
#import <PPMediaPlayer/PPMediaPlayer.h>

@interface PPMovieViewController ()
{
    NSString *documentPath;
    NSString *time;
    NSString *size;
    NSString *video;
    NSString *audio;
    PPMediaInfo info;
    UIImageView *mImage;
    NSMutableArray *images_;
    NSMutableArray *infos_;
}
@end

@implementation PPMovieViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
        self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    } else {
        self = [super initWithNibName:@"PPMovieViewController_ipad" bundle:nibBundleOrNil];
    }
    
    if (self)
    {
        [self.tabBarItem setFinishedSelectedImage:[UIImage imageNamed:@"phone_bottom_icon_video_sel"] withFinishedUnselectedImage:[UIImage imageNamed:@"phone_bottom_icon_video"]];
        [self.tabBarItem setTitle:@"视频"];
    }
    return self;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
        return [[tableView dequeueReusableCellWithIdentifier:@"PPMovieViewCell"] size].height;
    } else {
        return [[tableView dequeueReusableCellWithIdentifier:@"PPMovieViewCell_ipad"] size].height + 20;
    }
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    [[UIApplication sharedApplication] setStatusBarStyle:UIStatusBarStyleLightContent];
    UINib *nib = [UINib nibWithNibName:@"PPMovieViewCell" bundle:nil];
    [[self myTableView] registerNib:nib forCellReuseIdentifier:@"PPMovieViewCell"];
    UINib *nib_ipad = [UINib nibWithNibName:@"PPMovieViewCell_ipad" bundle:nil];
    [[self myTableView] registerNib:nib_ipad forCellReuseIdentifier:@"PPMovieViewCell_ipad"];
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    documentPath = [paths objectAtIndex:0];
	[self searchFiles];
    images_ = [[NSMutableArray alloc] init];
    infos_ = [[NSMutableArray alloc] init];
}

- (void)searchFiles
{
    if(self.movieArray != nil)
    {
        self.movieArray = nil;
    }
    self.movieArray = [NSMutableArray array];
    
    NSDirectoryEnumerator *fileEnumerator = [[NSFileManager defaultManager] enumeratorAtPath:documentPath];
    NSString *fileName = nil;
    NSArray *format = @[@".mp4", @".mkv", @".rmvb", @".avi", @".m3u8", @".wmv", @".flv", @".rm", @".m4v", @".divx", @".3gp", @".m2ts", @".mpeg", @".ts"];
    while (fileName = [fileEnumerator nextObject])
    {
        __block BOOL rel = NO;
        [format enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop)
        {
            if ([[fileName lowercaseString] hasSuffix:obj])
            {
                *stop = YES;
                rel = YES;
            }
        }];
        if(rel == YES)[self.movieArray addObject:fileName];
    }
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

- (void)viewDidUnload
{
    [self setMyTableView:nil];
    [super viewDidUnload];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    PPMovieViewCell *cell = nil;
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
        cell = [tableView dequeueReusableCellWithIdentifier:@"PPMovieViewCell"];
    } else {
        cell =  [tableView dequeueReusableCellWithIdentifier:@"PPMovieViewCell_ipad"];
    }
    cell.movieTitle.text = [[self.movieArray objectAtIndex:[indexPath row]] lastPathComponent];
    NSString *url = [documentPath stringByAppendingPathComponent:[self.movieArray
                                                                  objectAtIndex:[indexPath row]]];
    if ([indexPath row] >= images_.count) {
        UIImage *img = [[PPMediaPlayerInfo sharedInstance] getThumbnail:url];
        [images_ addObject:img];
        info = [[PPMediaPlayerInfo sharedInstance] getMediaInfo:url];
        NSValue *userValue = [NSValue valueWithBytes:&info objCType:@encode(PPMediaInfo)];
        [infos_ addObject:userValue];
    } else {
        NSValue *userValue = [infos_ objectAtIndex:[indexPath row]];
        [userValue getValue:&info];
    }
    cell.imageView.image = [images_ objectAtIndex:[indexPath row]];

    NSLog(@"%f*%f", cell.imageView.frame.size.width, cell.imageView.frame.size.height);
    time = [NSString stringWithFormat:@"时间：%d min",(info.duration)/60000];
    size = [NSString stringWithFormat:@"分辨率：%d×%d",info.width,info.height];
    video = [NSString stringWithFormat:@"视频编码：%s",info.video_name];
    audio = [NSString stringWithFormat:@"音频编码：%s",info.audio_name];
    cell.timeLabel.text = time;
    cell.sizeLabel.text = size;
    cell.videoCoding.text = video;
    cell.audioCoding.text = audio;
    return cell;
}

+ (UIImage *)imageWithImage:(UIImage *)image scaledToSize:(CGSize)newSize {
    //UIGraphicsBeginImageContext(newSize);
    // In next line, pass 0.0 to use the current device's pixel scaling factor (and thus account for Retina resolution).
    // Pass 1.0 to force exact pixel size.
    UIGraphicsBeginImageContextWithOptions(newSize, NO, 0.0);
    [image drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return newImage;
}

- (NSInteger)tableView:(UITableView*)tableView numberOfRowsInSection:(NSInteger)section
{
    return [self.movieArray count];
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSString *filePath = [documentPath stringByAppendingPathComponent:[self.movieArray objectAtIndex:[indexPath row]]];
    PPPlayerViewController *playerViewController = [[PPPlayerViewController alloc] initWithPlayURL:filePath];
    [self presentViewController:playerViewController animated:NO completion:nil];
}



@end
