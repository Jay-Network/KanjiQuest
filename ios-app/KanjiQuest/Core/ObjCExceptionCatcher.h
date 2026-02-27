#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface ObjCExceptionCatcher : NSObject
+ (nullable id)catchException:(id (^)(void))block error:(NSError **)error;
@end

NS_ASSUME_NONNULL_END
