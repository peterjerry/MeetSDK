//
//  PPMovieViewCell.m
//  PPlayer
//
//  Created by stephenzhang on 13-10-17.
//  Copyright (c) 2013年 Stephen Zhang. All rights reserved.
//

#import "PPMovieViewCell.h"

@implementation PPMovieViewCell

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        // Initialization code
        assert(self.movieImage!=nil);
        [self.movieImage setContentMode:UIViewContentModeScaleToFill];
    }
    return self;
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated
{
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

@end
