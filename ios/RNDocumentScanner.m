#import "RNDocumentScanner.h"

@implementation RNDocumentScanner

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

RCT_REMAP_METHOD(getLargestSquarePoints,
                 forImage:(NSString *)imagePath
                 withConfig:(NSDictionary *)config
                 withResolver:(RCTPromiseResolveBlock)resolve
                 andRejecter:(RCTPromiseRejectBlock)reject)
{
    // get image from path
    UIImage *image = [UIImage imageWithContentsOfFile:imagePath];
    image = [image fixOrientation];
    
    // go opencv !
    CGFloat screenWidth = [[config valueForKey:@"screenWidth"] doubleValue];
    CGFloat screenHeight = [[config valueForKey:@"screenHeight"] doubleValue];
    
    UIImageView *imageView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, screenWidth, screenHeight)];
    imageView.contentMode = UIViewContentModeScaleAspectFit;
    imageView.image = image;
    
    CropRect cropRect = [imageView detectEdges];

    resolve(@[
      @{
          @"x": [NSNumber numberWithDouble:cropRect.topLeft.x],
          @"y": [NSNumber numberWithDouble:cropRect.topLeft.y]
      },
      @{
          @"x": [NSNumber numberWithDouble:cropRect.topRight.x],
          @"y": [NSNumber numberWithDouble:cropRect.topRight.y]
      },
      @{
          @"x": [NSNumber numberWithDouble:cropRect.bottomLeft.x],
          @"y": [NSNumber numberWithDouble:cropRect.bottomLeft.y]
      },
      @{
          @"x": [NSNumber numberWithDouble:cropRect.bottomRight.x],
          @"y": [NSNumber numberWithDouble:cropRect.bottomRight.y]
      }
    ]);
}

RCT_EXPORT_METHOD(getTransformedImage:(NSString *)imagePath withConfig:(NSDictionary *)config)
{
    
}

@end
