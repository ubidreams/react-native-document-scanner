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
    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
    
    // get points
    NSDictionary *topLeftPoint = [points objectAtIndex:0];
    NSDictionary *topRightPoint = [points objectAtIndex:1];
    NSDictionary *bottomRightPoint = [points objectAtIndex:2];
    NSDictionary *bottomLeftPoint = [points objectAtIndex:3];
 
    // get options
    double width = [[options valueForKey:@"width"] doubleValue];
    double height = [[options valueForKey:@"height"] doubleValue];
    BOOL thumbnail = [[options valueForKey:@"thumbnail"] boolValue];
    
    // go opencv !
    CropRect cropRect;
    cropRect.topLeft = CGPointMake([[topLeftPoint objectForKey:@"x"] doubleValue], [[topLeftPoint objectForKey:@"y"] doubleValue]);
    cropRect.topRight = CGPointMake([[topRightPoint objectForKey:@"x"] doubleValue], [[topRightPoint objectForKey:@"y"] doubleValue]);
    cropRect.bottomRight = CGPointMake([[bottomRightPoint objectForKey:@"x"] doubleValue], [[bottomRightPoint objectForKey:@"y"] doubleValue]);
    cropRect.bottomLeft = CGPointMake([[bottomLeftPoint objectForKey:@"x"] doubleValue], [[bottomLeftPoint objectForKey:@"y"] doubleValue]);
    
    UIImage *croppedImage = [self.imageView crop:cropRect andApplyBW:false];
    
    // resize cropped image ?
    if (width > 0 && height > 0) {
        croppedImage = [self resizeImage:croppedImage toWidth:width andHeight:height];
    }
    
    // save image to cache directory
    NSString *croppedImageFilePath = [self saveUIImageToCacheDirectory:croppedImage];
    
    // add image file path to result
    [result setValue:[NSString stringWithFormat:@"file://%@", croppedImageFilePath] forKey:@"image"];
    
    // create thumbnail ?
    if (thumbnail) {
        // resize original image to create a thumbnail
        UIImage *thumbnailImage = [self resizeImage:croppedImage toWidth:width andHeight:height];
        
        // save thumbnail to cache directory
        NSString *thumbnailFilePath = [self saveUIImageToCacheDirectory:thumbnailImage];
        
        // add thumbnail file path to result
        [result setValue:[NSString stringWithFormat:@"file://%@", thumbnailFilePath] forKey:@"thumbnail"];
    }
    
    // resolve promise
    resolve(result);
}

- (UIImage *)resizeImage:(UIImage *)image toWidth:(double)width andHeight:(double)height
{
    CGSize size = CGSizeMake(width, height);
    CGFloat scale = MAX(size.width / image.size.width, size.height / image.size.height);
    CGFloat scaledWidth = image.size.width * scale;
    CGFloat scaledHeight = image.size.height * scale;
    CGRect imageRect = CGRectMake((size.width - scaledWidth)/2.0f,
                                  (size.height - scaledHeight)/2.0f,
                                  scaledWidth,
                                  scaledHeight);
    
    UIGraphicsBeginImageContextWithOptions(size, NO, 0);
    [image drawInRect:imageRect];
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return newImage;
}

- (NSString *)saveUIImageToCacheDirectory:(UIImage *)image
{
    NSString *fileName = [NSString stringWithFormat:@"%@.jpg", [[NSProcessInfo processInfo] globallyUniqueString]];
    NSString *cacheDirPath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
    NSString *imageFilePath = [cacheDirPath stringByAppendingPathComponent:fileName];
    
    [UIImageJPEGRepresentation(image, 1) writeToFile:imageFilePath atomically:YES];
    
    return imageFilePath;
}

@end
