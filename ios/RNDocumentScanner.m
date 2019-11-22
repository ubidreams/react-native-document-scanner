#import "RNDocumentScanner.h"

@implementation RNDocumentScanner

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

RCT_REMAP_METHOD(detectEdges,
                 detectEdges:(NSString *)imagePath
                 layout:(NSDictionary *)layout
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    // get image from path
    UIImage *image = [UIImage imageWithContentsOfFile:imagePath];
    image = [image fixOrientation];

    // get layout
    CGFloat containerX = [[layout valueForKey:@"x"] doubleValue];
    CGFloat containerY = [[layout valueForKey:@"y"] doubleValue];
    CGFloat containerWidth = [[layout valueForKey:@"width"] doubleValue];
    CGFloat containerHeight = [[layout valueForKey:@"height"] doubleValue];

    // configure image
    self.imageView = [[UIImageView alloc] initWithFrame:CGRectMake(containerX, containerY, containerWidth, containerHeight)];
    self.imageView.image = image;

    // go opencv !
    CropRect cropRect = [self.imageView detectEdges];

    // build points array
    NSArray *points = @[
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
    ];

    // resolve promise
    resolve(points);
}

RCT_REMAP_METHOD(crop,
                 crop:(NSArray *)points
                 options:(NSDictionary *)options
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    // get points
    NSDictionary *topLeftPoint = [points objectAtIndex:0];
    NSDictionary *topRightPoint = [points objectAtIndex:1];
    NSDictionary *bottomRightPoint = [points objectAtIndex:2];
    NSDictionary *bottomLeftPoint = [points objectAtIndex:3];

    // get options
    float width = [[options valueForKey:@"width"] floatValue];
    float height = [[options valueForKey:@"height"] floatValue];

    // go opencv !
    CropRect cropRect;
    cropRect.topLeft = CGPointMake([[topLeftPoint objectForKey:@"x"] doubleValue], [[topLeftPoint objectForKey:@"y"] doubleValue]);
    cropRect.topRight = CGPointMake([[topRightPoint objectForKey:@"x"] doubleValue], [[topRightPoint objectForKey:@"y"] doubleValue]);
    cropRect.bottomRight = CGPointMake([[bottomRightPoint objectForKey:@"x"] doubleValue], [[bottomRightPoint objectForKey:@"y"] doubleValue]);
    cropRect.bottomLeft = CGPointMake([[bottomLeftPoint objectForKey:@"x"] doubleValue], [[bottomLeftPoint objectForKey:@"y"] doubleValue]);

    UIImage *croppedImage = [self.imageView crop:cropRect andApplyBW:false];

    // resize image ?
    if (width > 0 && height > 0) {
        float ratioX = width / croppedImage.size.width;
        float ratioY = height / croppedImage.size.height;
        float ratio = MIN(ratioX, ratioY);

        float finalWidth = croppedImage.size.width * ratio;
        float finalHeight = croppedImage.size.height * ratio;

        UIGraphicsBeginImageContext(CGSizeMake(finalWidth, finalHeight));
        [croppedImage drawInRect:CGRectMake(0, 0, finalWidth, finalHeight)];

        croppedImage = UIGraphicsGetImageFromCurrentImageContext();

        UIGraphicsEndImageContext();
    }

    // save image to cache directory
    NSString *cacheDirPath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
    NSString *croppedImageFilePath = [cacheDirPath stringByAppendingPathComponent:[[NSProcessInfo processInfo] globallyUniqueString]];

    [UIImageJPEGRepresentation(croppedImage, 1) writeToFile:croppedImageFilePath atomically:YES];

    // resolve promise
    resolve(croppedImageFilePath);
}

@end
