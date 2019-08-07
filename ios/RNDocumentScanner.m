#import "RNDocumentScanner.h"

@implementation RNDocumentScanner

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

RCT_REMAP_METHOD(detectEdges,
                 detectEdges:(NSString *)imagePath
                 config:(NSDictionary *)config
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    // get image from path
    self.image = [UIImage imageWithContentsOfFile:imagePath];
    self.image = [self.image fixOrientation];
    
    // get config
    CGFloat screenWidth = [[config valueForKey:@"screenWidth"] doubleValue];
    CGFloat screenHeight = [[config valueForKey:@"screenHeight"] doubleValue];
    
    // configure image
    self.imageView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, screenWidth, screenHeight)];
    self.imageView.contentMode = UIViewContentModeScaleAspectFit;
    self.imageView.image = self.image;
    
    // go opencv !
    CropRect cropRect = [self.imageView detectEdges];

    // resolve promise
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
          @"x": [NSNumber numberWithDouble:cropRect.bottomRight.x],
          @"y": [NSNumber numberWithDouble:cropRect.bottomRight.y]
      },
      @{
          @"x": [NSNumber numberWithDouble:cropRect.bottomLeft.x],
          @"y": [NSNumber numberWithDouble:cropRect.bottomLeft.y]
      }
    ]);
}

RCT_EXPORT_METHOD(getTransformedImage:(NSString *)imagePath withConfig:(NSDictionary *)config)
{
    
}

@end
