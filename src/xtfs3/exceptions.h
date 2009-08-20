#ifndef _XTFS3_EXCEPTIONS_H
#define _XTFS3_EXCEPTIONS_H

#include "yield.h"

namespace xtfs3
{
  class Exception : public YIELD::ExceptionResponse
  {
  public:
    virtual const char* get_code() const = 0;
    virtual uint16_t get_http_status_code() const = 0;
    virtual const char* get_message() const = 0;

    // yidl::Object
    void marshal( yidl::Marshaller& marshaller ) const
    {    
      marshaller.writeString( "Code", 0, get_code() );
      marshaller.writeString( "Message", 0, get_message() );
    }
  };

#define XTFS3_EXCEPTION( Code, HTTPStatusCode, Message ) \
  class Code : public xtfs3::Exception \
  { \
  public: \
    const char* get_code() const { return #Code; } \
    uint16_t get_http_status_code() const { return HTTPStatusCode; } \
    const char* get_message() const { return Message; } \
  };


  namespace exceptions
  {
    XTFS3_EXCEPTION( AccessDenied, 403, "Access Denied" );
    XTFS3_EXCEPTION( AccountProblem, 403, "There is a problem with your AWS account that prevents the operatio from completing successfully." );
    XTFS3_EXCEPTION( AmbiguousGrantByEmailAddress, 400, "The e-mail address you provided is associated with more than one account." );
    XTFS3_EXCEPTION( BadDigest, 400, "The Content-MD5 you specified did not match what we received." );
    XTFS3_EXCEPTION( BucketAlreadyExists, 409, "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again." );
    XTFS3_EXCEPTION( BucketAlreadyOwnedByYou, 409, "Your previous request to create the named bucket succeeded and you already own it." );
    XTFS3_EXCEPTION( BucketNotEmpty, 409, "The bucket you tried to delete is not empty." );
    XTFS3_EXCEPTION( CredentialsNotSupported, 400, "This request does not support credentials." );
    XTFS3_EXCEPTION( CrossLocationLoggingProhibited, 403, "Cross location logging not allowed. Buckets in one geographic location cannot log information to a bucket in another location." );
    XTFS3_EXCEPTION( EntityTooSmall, 400, "Your proposed upload is smaller than the minimum allowed object size." );
    XTFS3_EXCEPTION( EntityTooLarge, 400, "Your proposed upload exceeds the maximum allowed object size." );
    XTFS3_EXCEPTION( ExpiredToken, 400, "The provided token has expired." );
    XTFS3_EXCEPTION( IncompleteBody, 400, "You did not provide the number of bytes specified by the Content-Length HTTP header" );
    XTFS3_EXCEPTION( IncorrectNumberOfFilesInPostRequest, 400, "POST requires exactly one file upload per request." );
    XTFS3_EXCEPTION( InlineDataTooLarge, 400, "Inline data exceeds the maximum allowed size." );
    XTFS3_EXCEPTION( InternalError, 500, "We encountered an internal error. Please try again." );
    XTFS3_EXCEPTION( InvalidAccessKeyId, 403, "The AWS Access Key Id you provided does not exist in our records." );
    XTFS3_EXCEPTION( InvalidAddressingHeader, 0, "You must specify the Anonymous role." );
    XTFS3_EXCEPTION( InvalidArgument, 400, "Invalid Argument" );
    XTFS3_EXCEPTION( InvalidBucketName, 400, "The specified bucket is not valid." );
    XTFS3_EXCEPTION( InvalidDigest, 400, "The Content-MD5 you specified was an invalid." );
    XTFS3_EXCEPTION( InvalidLocationConstraint, 400, "The specified location constraint is not valid." );
    XTFS3_EXCEPTION( InvalidPayer, 403, "All access to this object has been disabled." );
    XTFS3_EXCEPTION( InvalidPolicyDocument, 400, "The content of the form does not meet the conditions specified in the policy document." );
    XTFS3_EXCEPTION( InvalidRange, 416, "The requested range cannot be satisfied." );
    XTFS3_EXCEPTION( InvalidSecurity, 403, "The provided security credentials are not valid." );
    XTFS3_EXCEPTION( InvalidSOAPRequest, 400, "The SOAP request body is invalid." );
    XTFS3_EXCEPTION( InvalidStorageClass, 400, "The storage class you specified is not valid." );
    XTFS3_EXCEPTION( InvalidTargetBucketForLogging, 400, "The target bucket for logging does not exist, is not owned by you, or does not have the appropriate grants for the log-delivery group." );
    XTFS3_EXCEPTION( InvalidToken, 400, "The provided token is malformed or otherwise invalid." );
    XTFS3_EXCEPTION( InvalidURI, 400, "Couldn't parse the specified URI." );
    XTFS3_EXCEPTION( KeyTooLong, 400, "Your key is too long." );
    XTFS3_EXCEPTION( MalformedACLError, 400, "The XML you provided was not well-formed or did not validate against our published schema." );
    XTFS3_EXCEPTION( MalformedPOSTRequest, 400, "The body of your POST request is not well-formed multipart/form-data." );
    XTFS3_EXCEPTION( MalformedXML, 400, "The XML you provided was not well-formed or did not validate against our published schema." );
    XTFS3_EXCEPTION( MaxMessageLengthExceeded, 400, "Your request was too big." );
    XTFS3_EXCEPTION( MaxPostPreDataLengthExceededError, 400, "Your POST request fields preceding the upload file were too large." );
    XTFS3_EXCEPTION( MetadataTooLarge, 400, "Your metadata headers exceed the maximum allowed metadata size." );
    XTFS3_EXCEPTION( MethodNotAllowed, 400, "The specified method is not allowed against this resource." );
    XTFS3_EXCEPTION( MissingAttachment, 0, "A SOAP attachment was expected but none were found." );
    XTFS3_EXCEPTION( MissingContentLength, 411, "You must provide the Content-Length HTTP header." );
    XTFS3_EXCEPTION( MissingRequestBodyError, 400, "Request body is empty." );
    XTFS3_EXCEPTION( MissingSecurityElement, 400, "The SOAP 1.1 request is missing a security element." );
    XTFS3_EXCEPTION( MissingSecurityHeader, 400, "Your request was missing a required header." );
    XTFS3_EXCEPTION( NoLoggingStatusForKey, 400, "There is no such thing as a logging status sub-resource for a key." );
    XTFS3_EXCEPTION( NoSuchBucket, 404, "The specified bucket does not exist." );
    XTFS3_EXCEPTION( NoSuchKey, 404, "The specified key does not exist." );
    XTFS3_EXCEPTION( NotImplemented, 501, "A header you provided implies functionality that is not implemented." );
    XTFS3_EXCEPTION( NotSignedUp, 403, "Your account is not signed up for the Amazon S3 service. You must sign up before you can use Amazon S3." );
    XTFS3_EXCEPTION( OperationAborted, 409, "A conflicting conditional operation is currently in progress against this resource. Please try again." );
    XTFS3_EXCEPTION( PermanentRedirect, 301, "The bucket you are attempting to access must be addressed using the specified endpoint. Please send all future requests to this endpoint." );
    XTFS3_EXCEPTION( PreconditionFailed, 412, "At least one of the pre-conditions you specified did not hold." );
    XTFS3_EXCEPTION( Redirect, 307, "Temporary redirect." );
    XTFS3_EXCEPTION( RequestIsNotMultiPartContent, 400, "Bucket POST must be of the enclosure-type multipart/form-data." );
    XTFS3_EXCEPTION( RequestTimeout, 400, "Your socket connection to the server was not read from or written to within the timeout period." );
    XTFS3_EXCEPTION( RequestTimeTooSkewed, 403, "The difference between the request time and the server's time is too large." );
    XTFS3_EXCEPTION( RequestTorrentOfBucketError, 400, "Requesting the torrent file of a bucket is not permitted."  );
    XTFS3_EXCEPTION( SignatureDoesNotMatch, 403, "The request signature we calculated does not match the signature you provided." );
    XTFS3_EXCEPTION( SlowDown, 503, "Please reduce your request rate." );
    XTFS3_EXCEPTION( TemporaryRedirect, 307, "You are being redirected to the bucket while DNS updates." );
    XTFS3_EXCEPTION( TokenRefreshRequired, 400, "The provided token must be refreshed." );
    XTFS3_EXCEPTION( TooManyBuckets, 400, "You have attempted to create more buckets than allowed." );
    XTFS3_EXCEPTION( UnexpectedContent, 400, "This request does not support content." );
    XTFS3_EXCEPTION( UnresolvableGrantByEmailAddress, 400, "The e-mail address you provided does not match any account on record." );
    XTFS3_EXCEPTION( UserKeyMustBeSpecified, 400, "The bucket POST must contain the specified field name. If it is specified, please check the order of the fields." );
  };
};

#endif
