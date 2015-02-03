//
//  PPMovieViewCell.h
//  PPlayer
//
//  Created by stephenzhang on 13-10-17.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface PPMovieViewCell : UITableViewCell
@property (strong, nonatomic) IBOutlet UIImageView *movieImage;
@property (weak, nonatomic) IBOutlet UILabel *movieTitle;
@property (weak, nonatomic) IBOutlet UILabel *timeLabel;
@property (weak, nonatomic) IBOutlet UILabel *sizeLabel;
@property (weak, nonatomic) IBOutlet UILabel *videoCoding;
@property (weak, nonatomic) IBOutlet UILabel *audioCoding;
@end
