#import "ObjCExceptionCatcher.h"

@implementation ObjCExceptionCatcher

+ (nullable id)catchException:(id (^)(void))block error:(NSError **)error {
    @try {
        return block();
    } @catch (NSException *exception) {
        if (error) {
            *error = [NSError errorWithDomain:@"com.jworks.kanjiquest"
                                         code:1
                                     userInfo:@{
                NSLocalizedDescriptionKey: [NSString stringWithFormat:@"%@: %@",
                    exception.name, exception.reason ?: @"(no reason)"]
            }];
        }
        return nil;
    }
}

@end
