#import "OpenCV/UIImageView+OpenCV.h"
#import "OpenCV/UIImage+fixOrientation.h"
#import <React/RCTBridgeModule.h>

@interface RNDocumentScanner : NSObject <RCTBridgeModule>

@property(nonatomic, strong) UIImageView *imageView;
@property(nonatomic, strong) UIImage *image;

@end
  
