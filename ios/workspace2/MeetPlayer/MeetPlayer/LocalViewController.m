//
//  LocalViewController.m
//  MeetPlayer
//
//  Created by Eason Zhao on 15/3/4.
//  Copyright (c) 2015年 Eason. All rights reserved.
//

#import "LocalViewController.h"
#import "MovieCell.h"
#import "PlaybackViewController.h"

#define DOCUMENT_PATH [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0]

@interface LocalViewController ()<UITableViewDelegate, UITableViewDataSource>
{
    NSMutableArray *moviePaths_;
}

@property (weak, nonatomic) IBOutlet UITableView *tableView_;

@end

@implementation LocalViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    UINib *nib = [UINib nibWithNibName:@"MovieCell" bundle:nil];
    [self.tableView_ registerNib:nib
          forCellReuseIdentifier:@"MovieCell"];

    [self searchFiles:DOCUMENT_PATH];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)searchFiles:(NSString *)documentPath
{
    if(moviePaths_ == nil) {
        moviePaths_ = [NSMutableArray array];
    } else {
        [moviePaths_ removeAllObjects];
    }
    
    NSDirectoryEnumerator *fileEnumerator = [[NSFileManager defaultManager]
                                             enumeratorAtPath:documentPath];
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
            [moviePaths_ addObject:fileName];
        }
    }
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return moviePaths_.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView
         cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    MovieCell *cell = nil;
    cell = [tableView dequeueReusableCellWithIdentifier:@"MovieCell"];
    cell.title.text = [[moviePaths_ objectAtIndex:[indexPath row]] lastPathComponent];
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
    NSString *filePath = [DOCUMENT_PATH stringByAppendingPathComponent:[moviePaths_ objectAtIndex:[indexPath row]]];
    NSURL *url = [NSURL fileURLWithPath:filePath];
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
