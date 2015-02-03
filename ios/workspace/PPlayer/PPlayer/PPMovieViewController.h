//
//  PPViewController.h
//  PPlayer
//
//  Created by stephenzhang on 13-9-5.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface PPMovieViewController : UIViewController

@property (nonatomic, strong) NSMutableArray *movieArray;
@property (strong, nonatomic) IBOutlet UITableView *myTableView;

@end
