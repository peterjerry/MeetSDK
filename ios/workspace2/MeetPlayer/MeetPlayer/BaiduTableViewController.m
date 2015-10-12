//
//  BaiduTableViewController.m
//  MeetPlayer
//
//  Created by pptv on 15/10/12.
//  Copyright (c) 2015年 Eason. All rights reserved.
//

#import "BaiduTableViewController.h"

#define KEY_ACCESSTOKEN @"access_token"
//
// user has to replace this root with own path, if your app name is "abc", just set ROOT "/apps/abc/"
//
#define VALUE_DEFAULT_PATH @"/我的视频/测试"
//
// User has to replace this api key with own api key, this key is invalid hKUZTfsigrwRlXfs0uvDdHb
//
#define BAIDU_API_KEY @"4YchBAkgxfWug3KRYCGOv8EK"

@interface BaiduTableViewController ()

@end

@implementation BaiduTableViewController

@synthesize client;
@synthesize mpToken;

- (void)viewDidLoad {
    [super viewDidLoad];
    
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
    
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
    
    mpToken = @"23.a136481b1f63223c900ca9792e14204c.2592000.1446009101.184740130-266719";
    self.client = [[BaiduPCSClient alloc] init];
    [self.client setAccessToken:mpToken];
    
    [self performSelectorInBackground:@selector(list_file:) withObject:VALUE_DEFAULT_PATH];

    self.listItem = [NSMutableArray arrayWithCapacity:128];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    // Return the number of sections.
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    // Return the number of rows in the section.
    return [self.listItem count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    /*UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:<#@"reuseIdentifier"#> forIndexPath:indexPath];
    
    // Configure the cell...
    
    return cell;*/
    if ([self.listItem count] < indexPath.row + 1)
        return nil;
    
    static NSString *SimpleTableIdentifier = @"SimpleTableIdentifier";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:SimpleTableIdentifier];
    if (cell == nil) {
        cell = [[UITableViewCell alloc]initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:SimpleTableIdentifier];
    }
    
    UIImage *image = [UIImage imageNamed:@"apple.png"];
    cell.imageView.image = image;
    UIImage *imageHighlight = [UIImage imageNamed:@"apple_highlight.png"];
    cell.imageView.highlightedImage = imageHighlight;
    
    cell.textLabel.text = [self.listItem objectAtIndex:indexPath.row];
    
    return cell;
}


/*
// Override to support conditional editing of the table view.
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    // Return NO if you do not want the specified item to be editable.
    return YES;
}
*/

/*
// Override to support editing the table view.
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        // Delete the row from the data source
        [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
    } else if (editingStyle == UITableViewCellEditingStyleInsert) {
        // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
    }   
}
*/

/*
// Override to support rearranging the table view.
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
}
*/

/*
// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    // Return NO if you do not want the item to be re-orderable.
    return YES;
}
*/

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

-(void)list_file:(NSString*)path
{
    NSLog(@"test_list");
    
    PCSListInfoResponse *response = [self.client list:path:@"name":@"asc"];
    
    if (response){
        NSString *message = [[NSString alloc] initWithFormat:@"error code: %d\nmessage: %@\nitem_num: %d\n", response.status.errorCode, response.status.message, [response.list count]];
        NSMutableString *string = [[NSMutableString alloc] init];
        [string appendString:message];
        
        for(int i = 0; i < [response.list count]; ++i){
            PCSCommonFileInfo *tmp = [response.list objectAtIndex:i];
            
            if(tmp){
                [string appendFormat:@"%@\n", tmp.path];
                [self performSelectorOnMainThread:@selector(myAddListItem:) withObject:tmp.path waitUntilDone:YES];
            }
        }
        NSLog(@"result %@", string);
        
        [self performSelectorOnMainThread:@selector(updateDisplayMessage:) withObject:string waitUntilDone:NO];
    }
    else {
        NSLog(@"failed to call list()");
    }
}

-(void)updateDisplayMessage:(NSString*)message
{
    //labelInfo.text = message;
}

-(void)myAddListItem:(NSString*)title
{
    NSLog(@"myAddListItem() %@", title);
    
    [self.listItem addObject:title];
    [self.tableView reloadData];
}

@end
