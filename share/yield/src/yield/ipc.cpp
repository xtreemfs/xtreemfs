#include "yield/ipc.h"


// uriparser.h
/*
 * uriparser - RFC 3986 URI parsing library
 *
 * Copyright (C) 2007, Weijia Song <songweijia@gmail.com>
 * Copyright (C) 2007, Sebastian Pipping <webmaster@hartwork.org>
 * All rights reserved.
 *
 * Redistribution  and use in source and binary forms, with or without
 * modification,  are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions   of  source  code  must  retain  the   above
 *       copyright  notice, this list of conditions and the  following
 *       disclaimer.
 *
 *     * Redistributions  in  binary  form must  reproduce  the  above
 *       copyright  notice, this list of conditions and the  following
 *       disclaimer   in  the  documentation  and/or  other  materials
 *       provided with the distribution.
 *
 *     * Neither  the name of the <ORGANIZATION> nor the names of  its
 *       contributors  may  be  used to endorse  or  promote  products
 *       derived  from  this software without specific  prior  written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT  NOT
 * LIMITED  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND  FITNESS
 * FOR  A  PARTICULAR  PURPOSE ARE DISCLAIMED. IN NO EVENT  SHALL  THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL,    SPECIAL,   EXEMPLARY,   OR   CONSEQUENTIAL   DAMAGES
 * (INCLUDING,  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES;  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT  LIABILITY,  OR  TORT (INCLUDING  NEGLIGENCE  OR  OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @file UriDefsConfig.h
 * Adjusts the internal configuration after processing external definitions.
 */

#ifndef URIPARSER_H
#define URIPARSER_H

# include <stdio.h> /* For NULL, snprintf */
# include <ctype.h> /* For wchar_t */
# include <string.h> /* For strlen, memset, memcpy */
# include <stdlib.h> /* For malloc */


/* Header: UriDefsAnsi.h */
#undef URI_CHAR
#define URI_CHAR char

#undef _UT
#define _UT(x) x

#undef URI_FUNC
#define URI_FUNC(x) uri##x##A

#undef URI_TYPE
#define URI_TYPE(x) Uri##x##A



#undef URI_STRLEN
#define URI_STRLEN strlen
#undef URI_STRCPY
#define URI_STRCPY strcpy
#undef URI_STRCMP
#define URI_STRCMP strcmp
#undef URI_STRNCMP
#define URI_STRNCMP strncmp

/* TODO Remove on next source-compatibility break */
#undef URI_SNPRINTF
#if (defined(__WIN32__) || defined(_WIN32) || defined(WIN32))
# define URI_SNPRINTF _snprintf
#else
# define URI_SNPRINTF snprintf
#endif


/* Header: UriBase.h */

/* Version helper macro */
#define URI_ANSI_TO_UNICODE(x) L##x



/* Version */
#define URI_VER_MAJOR           0
#define URI_VER_MINOR           7
#define URI_VER_RELEASE         5
#define URI_VER_SUFFIX_ANSI     ""
#define URI_VER_SUFFIX_UNICODE  URI_ANSI_TO_UNICODE(URI_VER_SUFFIX_ANSI)



/* More version helper macros */
#define URI_INT_TO_ANSI_HELPER(x) #x
#define URI_INT_TO_ANSI(x) URI_INT_TO_ANSI_HELPER(x)

#define URI_INT_TO_UNICODE_HELPER(x) URI_ANSI_TO_UNICODE(#x)
#define URI_INT_TO_UNICODE(x) URI_INT_TO_UNICODE_HELPER(x)

#define URI_VER_ANSI_HELPER(ma, mi, r, s) \
	URI_INT_TO_ANSI(ma) "." \
	URI_INT_TO_ANSI(mi) "." \
	URI_INT_TO_ANSI(r) \
	s

#define URI_VER_UNICODE_HELPER(ma, mi, r, s) \
	URI_INT_TO_UNICODE(ma) L"." \
	URI_INT_TO_UNICODE(mi) L"." \
	URI_INT_TO_UNICODE(r) \
	s



/* Full version strings */
#define URI_VER_ANSI     URI_VER_ANSI_HELPER(URI_VER_MAJOR, URI_VER_MINOR, URI_VER_RELEASE, URI_VER_SUFFIX_ANSI)
#define URI_VER_UNICODE  URI_VER_UNICODE_HELPER(URI_VER_MAJOR, URI_VER_MINOR, URI_VER_RELEASE, URI_VER_SUFFIX_UNICODE)



/* Unused parameter macro */
#ifdef __GNUC__
# define URI_UNUSED(x) unused_##x __attribute__((unused))
#else
# define URI_UNUSED(x) x
#endif



typedef int UriBool; /**< Boolean type */

#define URI_TRUE     1
#define URI_FALSE    0



/* Shared errors */
#define URI_SUCCESS                        0
#define URI_ERROR_SYNTAX                   1 /* Parsed text violates expected format */
#define URI_ERROR_NULL                     2 /* One of the params passed was NULL
                                                although it mustn't be */
#define URI_ERROR_MALLOC                   3 /* Requested memory could not be allocated */
#define URI_ERROR_OUTPUT_TOO_LARGE         4 /* Some output is to large for the receiving buffer */
#define URI_ERROR_NOT_IMPLEMENTED          8 /* The called function is not implemented yet */
#define URI_ERROR_RANGE_INVALID            9 /* The parameters passed contained invalid ranges */


/* Errors specific to ToString */
#define URI_ERROR_TOSTRING_TOO_LONG        URI_ERROR_OUTPUT_TOO_LARGE /* Deprecated, test for URI_ERROR_OUTPUT_TOO_LARGE instead */

/* Errors specific to AddBaseUri */
#define URI_ERROR_ADDBASE_REL_BASE         5 /* Given base is not absolute */

/* Errors specific to RemoveBaseUri */
#define URI_ERROR_REMOVEBASE_REL_BASE      6 /* Given base is not absolute */
#define URI_ERROR_REMOVEBASE_REL_SOURCE    7 /* Given base is not absolute */


/* Function inlining, not ANSI/ISO C! */
#if (defined(URI_DOXYGEN) || defined(URI_SIZEDOWN))
# define URI_INLINE
#elif defined(__INTEL_COMPILER)
/* Intel C/C++ */
/* http://predef.sourceforge.net/precomp.html#sec20 */
/* http://www.intel.com/support/performancetools/c/windows/sb/CS-007751.htm#2 */
# define URI_INLINE __force_inline
#elif defined(_MSC_VER)
/* Microsoft Visual C++ */
/* http://predef.sourceforge.net/precomp.html#sec32 */
/* http://msdn2.microsoft.com/en-us/library/ms882281.aspx */
# define URI_INLINE __forceinline
#elif (__GNUC__ >= 4)
/* GCC C/C++ 4.x.x */
/* http://predef.sourceforge.net/precomp.html#sec13 */
# define URI_INLINE __attribute__((always_inline))
#elif (__STDC_VERSION__ >= 199901L)
/* C99, "inline" is a keyword */
# define URI_INLINE inline
#else
/* No inlining */
# define URI_INLINE
#endif



/* Header: UriBase.h */
/**
 * Holds an IPv4 address.
 */
typedef struct UriIp4Struct {
	unsigned char data[4]; /**< Each octet in one byte */
} UriIp4; /**< @copydoc UriIp4Struct */



/**
 * Holds an IPv6 address.
 */
typedef struct UriIp6Struct {
	unsigned char data[16]; /**< Each quad in two bytes */
} UriIp6; /**< @copydoc UriIp6Struct */



/**
 * Specifies a line break conversion mode
 */
typedef enum UriBreakConversionEnum {
	URI_BR_TO_LF, /**< Convert to Unix line breaks ("\\x0a") */
	URI_BR_TO_CRLF, /**< Convert to Windows line breaks ("\\x0d\\x0a") */
	URI_BR_TO_CR, /**< Convert to Macintosh line breaks ("\\x0d") */
	URI_BR_TO_UNIX = URI_BR_TO_LF, /**< @copydoc UriBreakConversionEnum::URI_BR_TO_LF */
	URI_BR_TO_WINDOWS = URI_BR_TO_CRLF, /**< @copydoc UriBreakConversionEnum::URI_BR_TO_CRLF */
	URI_BR_TO_MAC = URI_BR_TO_CR, /**< @copydoc UriBreakConversionEnum::URI_BR_TO_CR */
	URI_BR_DONT_TOUCH /**< Copy line breaks unmodified */
} UriBreakConversion; /**< @copydoc UriBreakConversionEnum */



/**
 * Specifies which component of a %URI has to be normalized.
 */
typedef enum UriNormalizationMaskEnum {
	URI_NORMALIZED = 0, /**< Do not normalize anything */
	URI_NORMALIZE_SCHEME = 1 << 0, /**< Normalize scheme (fix uppercase letters) */
	URI_NORMALIZE_USER_INFO = 1 << 1, /**< Normalize user info (fix uppercase percent-encodings) */
	URI_NORMALIZE_HOST = 1 << 2, /**< Normalize host (fix uppercase letters) */
	URI_NORMALIZE_PATH = 1 << 3, /**< Normalize path (fix uppercase percent-encodings and redundant dot segments) */
	URI_NORMALIZE_QUERY = 1 << 4, /**< Normalize query (fix uppercase percent-encodings) */
	URI_NORMALIZE_FRAGMENT = 1 << 5 /**< Normalize fragment (fix uppercase percent-encodings) */
} UriNormalizationMask; /**< @copydoc UriNormalizationMaskEnum */


/* Header: Uri.h */
/**
 * Specifies a range of characters within a string.
 * The range includes all characters from <c>first</c>
 * to one before <c>afterLast</c>. So if both are
 * non-NULL the difference is the length of the text range.
 *
 * @see UriUriA
 * @see UriPathSegmentA
 * @see UriHostDataA
 * @since 0.3.0
 */
typedef struct URI_TYPE(TextRangeStruct) {
	const URI_CHAR * first; /**< Pointer to first character */
	const URI_CHAR * afterLast; /**< Pointer to character after the last one still in */
} URI_TYPE(TextRange); /**< @copydoc UriTextRangeStructA */



/**
 * Represents a path segment within a %URI path.
 * More precisely it is a node in a linked
 * list of path segments.
 *
 * @see UriUriA
 * @since 0.3.0
 */
typedef struct URI_TYPE(PathSegmentStruct) {
	URI_TYPE(TextRange) text; /**< Path segment name */
	struct URI_TYPE(PathSegmentStruct) * next; /**< Pointer to the next path segment in the list, can be NULL if last already */

	void * reserved; /**< Reserved to the parser */
} URI_TYPE(PathSegment); /**< @copydoc UriPathSegmentStructA */



/**
 * Holds structured host information.
 * This is either a IPv4, IPv6, plain
 * text for IPvFuture or all zero for
 * a registered name.
 *
 * @see UriUriA
 * @since 0.3.0
 */
typedef struct URI_TYPE(HostDataStruct) {
	UriIp4 * ip4; /**< IPv4 address */
	UriIp6 * ip6; /**< IPv6 address */
	URI_TYPE(TextRange) ipFuture; /**< IPvFuture address */
} URI_TYPE(HostData); /**< @copydoc UriHostDataStructA */



/**
 * Represents an RFC 3986 %URI.
 * Missing components can be {NULL, NULL} ranges.
 *
 * @see uriParseUriA
 * @see uriFreeUriMembersA
 * @see UriParserStateA
 * @since 0.3.0
 */
typedef struct URI_TYPE(UriStruct) {
	URI_TYPE(TextRange) scheme; /**< Scheme (e.g. "http") */
	URI_TYPE(TextRange) userInfo; /**< User info (e.g. "user:pass") */
	URI_TYPE(TextRange) hostText; /**< Host text (set for all hosts, excluding square brackets) */
	URI_TYPE(HostData) hostData; /**< Structured host type specific data */
	URI_TYPE(TextRange) portText; /**< Port (e.g. "80") */
	URI_TYPE(PathSegment) * pathHead; /**< Head of a linked list of path segments */
	URI_TYPE(PathSegment) * pathTail; /**< Tail of the list behind pathHead */
	URI_TYPE(TextRange) query; /**< Query without leading "?" */
	URI_TYPE(TextRange) fragment; /**< Query without leading "#" */
	UriBool absolutePath; /**< Absolute path flag, distincting "a" and "/a" */
	UriBool owner; /**< Memory owner flag */

	void * reserved; /**< Reserved to the parser */
} URI_TYPE(Uri); /**< @copydoc UriUriStructA */



/**
 * Represents a state of the %URI parser.
 * Missing components can be NULL to reflect
 * a components absence.
 *
 * @see uriFreeUriMembersA
 * @since 0.3.0
 */
typedef struct URI_TYPE(ParserStateStruct) {
	URI_TYPE(Uri) * uri; /**< Plug in the %URI structure to be filled while parsing here */
	int errorCode; /**< Code identifying the occured error */
	const URI_CHAR * errorPos; /**< Pointer to position in case of a syntax error */

	void * reserved; /**< Reserved to the parser */
} URI_TYPE(ParserState); /**< @copydoc UriParserStateStructA */



/**
 * Represents a query element.
 * More precisely it is a node in a linked
 * list of query elements.
 *
 * @since 0.7.0
 */
typedef struct URI_TYPE(QueryListStruct) {
	URI_CHAR * key; /**< Key of the query element */
	URI_CHAR * value; /**< Value of the query element, can be NULL */

	struct URI_TYPE(QueryListStruct) * next; /**< Pointer to the next key/value pair in the list, can be NULL if last already */
} URI_TYPE(QueryList); /**< @copydoc UriQueryListStructA */



/**
 * Parses a RFC 3986 URI.
 *
 * @param state       <b>INOUT</b>: Parser state with set output %URI, must not be NULL
 * @param first       <b>IN</b>: Pointer to the first character to parse, must not be NULL
 * @param afterLast   <b>IN</b>: Pointer to the character after the last to parse, must not be NULL
 * @return            0 on success, error code otherwise
 *
 * @see uriParseUriA
 * @see uriToStringA
 * @since 0.3.0
 */
int URI_FUNC(ParseUriEx)(URI_TYPE(ParserState) * state,
		const URI_CHAR * first, const URI_CHAR * afterLast);



/**
 * Parses a RFC 3986 %URI.
 *
 * @param state   <b>INOUT</b>: Parser state with set output %URI, must not be NULL
 * @param text    <b>IN</b>: Text to parse, must not be NULL
 * @return        0 on success, error code otherwise
 *
 * @see uriParseUriExA
 * @see uriToStringA
 * @since 0.3.0
 */
int URI_FUNC(ParseUri)(URI_TYPE(ParserState) * state,
		const URI_CHAR * text);



/**
 * Frees all memory associated with the members
 * of the %URI structure. Note that the structure
 * itself is not freed, only its members.
 *
 * @param uri   <b>INOUT</b>: %URI structure whose members should be freed
 *
 * @since 0.3.0
 */
void URI_FUNC(FreeUriMembers)(URI_TYPE(Uri) * uri);



/**
 * Percent-encodes all unreserved characters from the input string and
 * writes the encoded version to the output string.
 * Be sure to allocate <b>3 times</b> the space of the input buffer for
 * the output buffer for <c>normalizeBreaks == URI_FALSE</c> and <b>6 times</b>
 * the space for <c>normalizeBreaks == URI_TRUE</c>
 * (since e.g. "\x0d" becomes "%0D%0A" in that case)
 *
 * @param inFirst           <b>IN</b>: Pointer to first character of the input text
 * @param inAfterLast       <b>IN</b>: Pointer after the last character of the input text
 * @param out               <b>OUT</b>: Encoded text destination
 * @param spaceToPlus       <b>IN</b>: Wether to convert ' ' to '+' or not
 * @param normalizeBreaks   <b>IN</b>: Wether to convert CR and LF to CR-LF or not.
 * @return                  Position of terminator in output string
 *
 * @see uriEscapeA
 * @see uriUnescapeInPlaceExA
 * @since 0.5.2
 */
URI_CHAR * URI_FUNC(EscapeEx)(const URI_CHAR * inFirst,
		const URI_CHAR * inAfterLast, URI_CHAR * out,
		UriBool spaceToPlus, UriBool normalizeBreaks);



/**
 * Percent-encodes all unreserved characters from the input string and
 * writes the encoded version to the output string.
 * Be sure to allocate <b>3 times</b> the space of the input buffer for
 * the output buffer for <c>normalizeBreaks == URI_FALSE</c> and <b>6 times</b>
 * the space for <c>normalizeBreaks == URI_FALSE</c>
 * (since e.g. "\x0d" becomes "%0D%0A" in that case)
 *
 * @param in                <b>IN</b>: Text source
 * @param out               <b>OUT</b>: Encoded text destination
 * @param spaceToPlus       <b>IN</b>: Wether to convert ' ' to '+' or not
 * @param normalizeBreaks   <b>IN</b>: Wether to convert CR and LF to CR-LF or not.
 * @return                  Position of terminator in output string
 *
 * @see uriEscapeExA
 * @see uriUnescapeInPlaceA
 * @since 0.5.0
 */
URI_CHAR * URI_FUNC(Escape)(const URI_CHAR * in, URI_CHAR * out,
		UriBool spaceToPlus, UriBool normalizeBreaks);



/**
 * Unescapes percent-encoded groups in a given string.
 * E.g. "%20" will become " ". Unescaping is done in place.
 * The return value will be point to the new position
 * of the terminating zero. Use this value to get the new
 * length of the string. NULL is only returned if <c>inout</c>
 * is NULL.
 *
 * @param inout             <b>INOUT</b>: Text to unescape/decode
 * @param plusToSpace       <b>IN</b>: Whether to convert '+' to ' ' or not
 * @param breakConversion   <b>IN</b>: Line break conversion mode
 * @return                  Pointer to new position of the terminating zero
 *
 * @see uriUnescapeInPlaceA
 * @see uriEscapeExA
 * @since 0.5.0
 */
const URI_CHAR * URI_FUNC(UnescapeInPlaceEx)(URI_CHAR * inout,
		UriBool plusToSpace, UriBreakConversion breakConversion);



/**
 * Unescapes percent-encoded groups in a given string.
 * E.g. "%20" will become " ". Unescaping is done in place.
 * The return value will be point to the new position
 * of the terminating zero. Use this value to get the new
 * length of the string. NULL is only returned if <c>inout</c>
 * is NULL.
 *
 * NOTE: '+' is not decoded to ' ' and line breaks are not converted.
 * Use the more advanced UnescapeInPlaceEx for that features instead.
 *
 * @param inout   <b>INOUT</b>: Text to unescape/decode
 * @return        Pointer to new position of the terminating zero
 *
 * @see uriUnescapeInPlaceExA
 * @see uriEscapeA
 * @since 0.3.0
 */
const URI_CHAR * URI_FUNC(UnescapeInPlace)(URI_CHAR * inout);



/**
 * Performs reference resolution as described in
 * <a href="http://tools.ietf.org/html/rfc3986#section-5.2.2">section 5.2.2 of RFC 3986</a>.
 * NOTE: On success you have to call uriFreeUriMembersA on \p absoluteDest manually later.
 *
 * @param absoluteDest     <b>OUT</b>: Result %URI
 * @param relativeSource   <b>IN</b>: Reference to resolve
 * @param absoluteBase     <b>IN</b>: Base %URI to apply
 * @return                 Error code or 0 on success
 *
 * @see uriRemoveBaseUriA
 * @since 0.4.0
 */
int URI_FUNC(AddBaseUri)(URI_TYPE(Uri) * absoluteDest,
		const URI_TYPE(Uri) * relativeSource,
		const URI_TYPE(Uri) * absoluteBase);



/**
 * Tries to make a relative %URI (a reference) from an
 * absolute %URI and a given base %URI. This can only work if
 * the absolute %URI shares scheme and authority with
 * the base %URI. If it does not the result will still be
 * an absolute URI (with scheme part if necessary).
 * NOTE: On success you have to call uriFreeUriMembersA on
 * \p dest manually later.
 *
 * @param dest             <b>OUT</b>: Result %URI
 * @param absoluteSource   <b>IN</b>: Absolute %URI to make relative
 * @param absoluteBase     <b>IN</b>: Base %URI
 * @param domainRootMode   <b>IN</b>: Create %URI with path relative to domain root
 * @return                 Error code or 0 on success
 *
 * @see uriAddBaseUriA
 * @since 0.5.2
 */
int URI_FUNC(RemoveBaseUri)(URI_TYPE(Uri) * dest,
		const URI_TYPE(Uri) * absoluteSource,
		const URI_TYPE(Uri) * absoluteBase,
		UriBool domainRootMode);



/**
 * Checks two URIs for equivalence. Comparison is done
 * the naive way, without prior normalization.
 * NOTE: Two <c>NULL</c> URIs are equal as well.
 *
 * @param a   <b>IN</b>: First %URI
 * @param b   <b>IN</b>: Second %URI
 * @return    <c>URI_TRUE</c> when equal, <c>URI_FAlSE</c> else
 *
 * @since 0.4.0
 */
UriBool URI_FUNC(EqualsUri)(const URI_TYPE(Uri) * a, const URI_TYPE(Uri) * b);



/**
 * Calculates the number of characters needed to store the
 * string representation of the given %URI excluding the
 * terminator.
 *
 * @param uri             <b>IN</b>: %URI to measure
 * @param charsRequired   <b>OUT</b>: Length of the string representation in characters <b>excluding</b> terminator
 * @return                Error code or 0 on success
 *
 * @see uriToStringA
 * @since 0.5.0
 */
int URI_FUNC(ToStringCharsRequired)(const URI_TYPE(Uri) * uri,
		int * charsRequired);



/**
 * Converts a %URI structure back to text as described in
 * <a href="http://tools.ietf.org/html/rfc3986#section-5.3">section 5.3 of RFC 3986</a>.
 *
 * @param dest           <b>OUT</b>: Output destination
 * @param uri            <b>IN</b>: %URI to convert
 * @param maxChars       <b>IN</b>: Maximum number of characters to copy <b>including</b> terminator
 * @param charsWritten   <b>OUT</b>: Number of characters written, can be lower than maxChars even if the %URI is too long!
 * @return               Error code or 0 on success
 *
 * @see uriToStringCharsRequiredA
 * @since 0.4.0
 */
int URI_FUNC(ToString)(URI_CHAR * dest, const URI_TYPE(Uri) * uri, int maxChars, int * charsWritten);



/**
 * Determines the components of a %URI that are not normalized.
 *
 * @param uri   <b>IN</b>: %URI to check
 * @return      Normalization job mask
 *
 * @see uriNormalizeSyntaxA
 * @since 0.5.0
 */
unsigned int URI_FUNC(NormalizeSyntaxMaskRequired)(const URI_TYPE(Uri) * uri);



/**
 * Normalizes a %URI using a normalization mask.
 * The normalization mask decides what components are normalized.
 *
 * NOTE: If necessary the %URI becomes owner of all memory
 * behind the text pointed to. Text is duplicated in that case.
 *
 * @param uri    <b>INOUT</b>: %URI to normalize
 * @param mask   <b>IN</b>: Normalization mask
 * @return       Error code or 0 on success
 *
 * @see uriNormalizeSyntaxA
 * @see uriNormalizeSyntaxMaskRequiredA
 * @since 0.5.0
 */
int URI_FUNC(NormalizeSyntaxEx)(URI_TYPE(Uri) * uri, unsigned int mask);



/**
 * Normalizes all components of a %URI.
 *
 * NOTE: If necessary the %URI becomes owner of all memory
 * behind the text pointed to. Text is duplicated in that case.
 *
 * @param uri   <b>INOUT</b>: %URI to normalize
 * @return      Error code or 0 on success
 *
 * @see uriNormalizeSyntaxExA
 * @see uriNormalizeSyntaxMaskRequiredA
 * @since 0.5.0
 */
int URI_FUNC(NormalizeSyntax)(URI_TYPE(Uri) * uri);



/**
 * Converts a Unix filename to a %URI string.
 * The destination buffer must be large enough to hold 7 + 3 * len(filename) + 1
 * characters in case of an absolute filename or 3 * len(filename) + 1 in case
 * of a relative filename.
 *
 * EXAMPLE
 *   Input:  "/bin/bash"
 *   Output: "file:///bin/bash"
 *
 * @param filename     <b>IN</b>: Unix filename to convert
 * @param uriString    <b>OUT</b>: Destination to write %URI string to
 * @return             Error code or 0 on success
 *
 * @see uriUriStringToUnixFilenameA
 * @see uriWindowsFilenameToUriStringA
 * @since 0.5.2
 */
int URI_FUNC(UnixFilenameToUriString)(const URI_CHAR * filename,
		URI_CHAR * uriString);



/**
 * Converts a Windows filename to a %URI string.
 * The destination buffer must be large enough to hold 8 + 3 * len(filename) + 1
 * characters in case of an absolute filename or 3 * len(filename) + 1 in case
 * of a relative filename.
 *
 * EXAMPLE
 *   Input:  "E:\\Documents and Settings"
 *   Output: "file:///E:/Documents%20and%20Settings"
 *
 * @param filename     <b>IN</b>: Windows filename to convert
 * @param uriString    <b>OUT</b>: Destination to write %URI string to
 * @return             Error code or 0 on success
 *
 * @see uriUriStringToWindowsFilenameA
 * @see uriUnixFilenameToUriStringA
 * @since 0.5.2
 */
int URI_FUNC(WindowsFilenameToUriString)(const URI_CHAR * filename,
		URI_CHAR * uriString);



/**
 * Extracts a Unix filename from a %URI string.
 * The destination buffer must be large enough to hold len(uriString) + 1 - 7
 * characters in case of an absolute %URI or len(uriString) + 1 in case
 * of a relative %URI.
 *
 * @param uriString    <b>IN</b>: %URI string to convert
 * @param filename     <b>OUT</b>: Destination to write filename to
 * @return             Error code or 0 on success
 *
 * @see uriUnixFilenameToUriStringA
 * @see uriUriStringToWindowsFilenameA
 * @since 0.5.2
 */
int URI_FUNC(UriStringToUnixFilename)(const URI_CHAR * uriString,
		URI_CHAR * filename);



/**
 * Extracts a Windows filename from a %URI string.
 * The destination buffer must be large enough to hold len(uriString) + 1 - 8
 * characters in case of an absolute %URI or len(uriString) + 1 in case
 * of a relative %URI.
 *
 * @param uriString    <b>IN</b>: %URI string to convert
 * @param filename     <b>OUT</b>: Destination to write filename to
 * @return             Error code or 0 on success
 *
 * @see uriWindowsFilenameToUriStringA
 * @see uriUriStringToUnixFilenameA
 * @since 0.5.2
 */
int URI_FUNC(UriStringToWindowsFilename)(const URI_CHAR * uriString,
		URI_CHAR * filename);



/**
 * Calculates the number of characters needed to store the
 * string representation of the given query list excluding the
 * terminator. It is assumed that line breaks are will be
 * normalized to "%0D%0A".
 *
 * @param queryList         <b>IN</b>: Query list to measure
 * @param charsRequired     <b>OUT</b>: Length of the string representation in characters <b>excluding</b> terminator
 * @return                  Error code or 0 on success
 *
 * @see uriComposeQueryCharsRequiredExA
 * @see uriComposeQueryA
 * @since 0.7.0
 */
int URI_FUNC(ComposeQueryCharsRequired)(const URI_TYPE(QueryList) * queryList,
		int * charsRequired);



/**
 * Calculates the number of characters needed to store the
 * string representation of the given query list excluding the
 * terminator.
 *
 * @param queryList         <b>IN</b>: Query list to measure
 * @param charsRequired     <b>OUT</b>: Length of the string representation in characters <b>excluding</b> terminator
 * @param spaceToPlus       <b>IN</b>: Wether to convert ' ' to '+' or not
 * @param normalizeBreaks   <b>IN</b>: Wether to convert CR and LF to CR-LF or not.
 * @return                  Error code or 0 on success
 *
 * @see uriComposeQueryCharsRequiredA
 * @see uriComposeQueryExA
 * @since 0.7.0
 */
int URI_FUNC(ComposeQueryCharsRequiredEx)(const URI_TYPE(QueryList) * queryList,
		int * charsRequired, UriBool spaceToPlus, UriBool normalizeBreaks);



/**
 * Converts a query list structure back to a query string.
 * The composed string does not start with '?',
 * on the way ' ' is converted to '+' and line breaks are
 * normalized to "%0D%0A".
 *
 * @param dest              <b>OUT</b>: Output destination
 * @param queryList         <b>IN</b>: Query list to convert
 * @param maxChars          <b>IN</b>: Maximum number of characters to copy <b>including</b> terminator
 * @param charsWritten      <b>OUT</b>: Number of characters written, can be lower than maxChars even if the query list is too long!
 * @return                  Error code or 0 on success
 *
 * @see uriComposeQueryExA
 * @see uriComposeQueryMallocA
 * @see uriComposeQueryCharsRequiredA
 * @see uriDissectQueryMallocA
 * @since 0.7.0
 */
int URI_FUNC(ComposeQuery)(URI_CHAR * dest,
		const URI_TYPE(QueryList) * queryList, int maxChars, int * charsWritten);



/**
 * Converts a query list structure back to a query string.
 * The composed string does not start with '?'.
 *
 * @param dest              <b>OUT</b>: Output destination
 * @param queryList         <b>IN</b>: Query list to convert
 * @param maxChars          <b>IN</b>: Maximum number of characters to copy <b>including</b> terminator
 * @param charsWritten      <b>OUT</b>: Number of characters written, can be lower than maxChars even if the query list is too long!
 * @param spaceToPlus       <b>IN</b>: Wether to convert ' ' to '+' or not
 * @param normalizeBreaks   <b>IN</b>: Wether to convert CR and LF to CR-LF or not.
 * @return                  Error code or 0 on success
 *
 * @see uriComposeQueryA
 * @see uriComposeQueryMallocExA
 * @see uriComposeQueryCharsRequiredExA
 * @see uriDissectQueryMallocExA
 * @since 0.7.0
 */
int URI_FUNC(ComposeQueryEx)(URI_CHAR * dest,
		const URI_TYPE(QueryList) * queryList, int maxChars, int * charsWritten,
		UriBool spaceToPlus, UriBool normalizeBreaks);



/**
 * Converts a query list structure back to a query string.
 * Memory for this string is allocated internally.
 * The composed string does not start with '?',
 * on the way ' ' is converted to '+' and line breaks are
 * normalized to "%0D%0A".
 *
 * @param dest              <b>OUT</b>: Output destination
 * @param queryList         <b>IN</b>: Query list to convert
 * @return                  Error code or 0 on success
 *
 * @see uriComposeQueryMallocExA
 * @see uriComposeQueryA
 * @see uriDissectQueryMallocA
 * @since 0.7.0
 */
int URI_FUNC(ComposeQueryMalloc)(URI_CHAR ** dest,
		const URI_TYPE(QueryList) * queryList);



/**
 * Converts a query list structure back to a query string.
 * Memory for this string is allocated internally.
 * The composed string does not start with '?'.
 *
 * @param dest              <b>OUT</b>: Output destination
 * @param queryList         <b>IN</b>: Query list to convert
 * @param spaceToPlus       <b>IN</b>: Wether to convert ' ' to '+' or not
 * @param normalizeBreaks   <b>IN</b>: Wether to convert CR and LF to CR-LF or not.
 * @return                  Error code or 0 on success
 *
 * @see uriComposeQueryMallocA
 * @see uriComposeQueryExA
 * @see uriDissectQueryMallocExA
 * @since 0.7.0
 */
int URI_FUNC(ComposeQueryMallocEx)(URI_CHAR ** dest,
		const URI_TYPE(QueryList) * queryList,
		UriBool spaceToPlus, UriBool normalizeBreaks);



/**
 * Constructs a query list from the raw query string of a given URI.
 * On the way '+' is converted back to ' ', line breaks are not modified.
 *
 * @param dest              <b>OUT</b>: Output destination
 * @param itemCount         <b>OUT</b>: Number of items found, can be NULL
 * @param first             <b>IN</b>: Pointer to first character <b>after</b> '?'
 * @param afterLast         <b>IN</b>: Pointer to character after the last one still in
 * @return                  Error code or 0 on success
 *
 * @see uriDissectQueryMallocExA
 * @see uriComposeQueryA
 * @see uriFreeQueryListA
 * @since 0.7.0
 */
int URI_FUNC(DissectQueryMalloc)(URI_TYPE(QueryList) ** dest, int * itemCount,
		const URI_CHAR * first, const URI_CHAR * afterLast);



/**
 * Constructs a query list from the raw query string of a given URI.
 *
 * @param dest              <b>OUT</b>: Output destination
 * @param itemCount         <b>OUT</b>: Number of items found, can be NULL
 * @param first             <b>IN</b>: Pointer to first character <b>after</b> '?'
 * @param afterLast         <b>IN</b>: Pointer to character after the last one still in
 * @param plusToSpace       <b>IN</b>: Whether to convert '+' to ' ' or not
 * @param breakConversion   <b>IN</b>: Line break conversion mode
 * @return                  Error code or 0 on success
 *
 * @see uriDissectQueryMallocA
 * @see uriComposeQueryExA
 * @see uriFreeQueryListA
 * @since 0.7.0
 */
int URI_FUNC(DissectQueryMallocEx)(URI_TYPE(QueryList) ** dest, int * itemCount,
		const URI_CHAR * first, const URI_CHAR * afterLast,
		UriBool plusToSpace, UriBreakConversion breakConversion);



/**
 * Frees all memory associated with the given query list.
 * The structure itself is freed as well.
 *
 * @param queryList   <b>INOUT</b>: Query list to free
 *
 * @since 0.7.0
 */
void URI_FUNC(FreeQueryList)(URI_TYPE(QueryList) * queryList);


/* Header: UriIp4.h */
/**
 * Converts a IPv4 text representation into four bytes.
 *
 * @param octetOutput  Output destination
 * @param first        First character of IPv4 text to parse
 * @param afterLast    Position to stop parsing at
 * @return Error code or 0 on success
 */
int URI_FUNC(ParseIpFourAddress)(unsigned char * octetOutput,
		const URI_CHAR * first, const URI_CHAR * afterLast);

#endif


// yajl.h
/*
 * Copyright 2007, Lloyd Hilaiel.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *  3. Neither the name of Lloyd Hilaiel nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef YAJL_H
#define YAJL_H


/* Header: yajl_common.h */
#define YAJL_MAX_DEPTH 128

/* msft dll export gunk.  To build a DLL on windows, you
 * must define WIN32, YAJL_SHARED, and YAJL_BUILD.  To use a shared
 * DLL, you must define YAJL_SHARED and WIN32 */
#if defined(WIN32) && defined(YAJL_SHARED)
#  ifdef YAJL_BUILD
#    define YAJL_API __declspec(dllexport)
#  else
#    define YAJL_API __declspec(dllimport)
#  endif
#else
#  define YAJL_API
#endif


/* Header: yajl_buf.h */
/**
 * yajl_buf is a buffer with exponential growth.  the buffer ensures that
 * you are always null padded.
 */
typedef struct yajl_buf_t * yajl_buf;

/* allocate a new buffer */
yajl_buf yajl_buf_alloc(void);

/* free the buffer */
void yajl_buf_free(yajl_buf buf);

/* append a number of bytes to the buffer */
void yajl_buf_append(yajl_buf buf, const void * data, unsigned int len);

/* empty the buffer */
void yajl_buf_clear(yajl_buf buf);

/* get a pointer to the beginning of the buffer */
const unsigned char * yajl_buf_data(yajl_buf buf);

/* get the length of the buffer */
unsigned int yajl_buf_len(yajl_buf buf);

/* truncate the buffer */
void yajl_buf_truncate(yajl_buf buf, unsigned int len);


/* Header: yajl_encode.h */
void yajl_string_encode(yajl_buf buf, const unsigned char * str,
                        unsigned int length);

void yajl_string_decode(yajl_buf buf, const unsigned char * str,
                        unsigned int length);


/* yajl_gen.h */
    /** generator status codes */
    typedef enum {
        /** no error */
        yajl_gen_status_ok = 0,
        /** at a point where a map key is generated, a function other than
         *  yajl_gen_string was called */
        yajl_gen_keys_must_be_strings,
        /** YAJL's maximum generation depth was exceeded.  see
         *  YAJL_MAX_DEPTH */
        yajl_max_depth_exceeded,
        /** A generator function (yajl_gen_XXX) was called while in an error
         *  state */
        yajl_gen_in_error_state,
        /** A complete JSON document has been generated */
        yajl_gen_generation_complete
    } yajl_gen_status;

    /** an opaque handle to a generator */
    typedef struct yajl_gen_t * yajl_gen;

    /** configuration structure for the generator */
    typedef struct {
        /** generate indented (beautiful) output */
        unsigned int beautify;
        /** an opportunity to define an indent string.  such as \\t or
         *  some number of spaces.  default is four spaces '    '.  This
         *  member is only relevant when beautify is true */
        const char * indentString;
    } yajl_gen_config;

    /** allocate a generator handle */
    yajl_gen YAJL_API yajl_gen_alloc(const yajl_gen_config * config);

    /** free a generator handle */
    void YAJL_API yajl_gen_free(yajl_gen handle);

    yajl_gen_status YAJL_API yajl_gen_integer(yajl_gen hand, long int number);
    yajl_gen_status YAJL_API yajl_gen_double(yajl_gen hand, double number);
    yajl_gen_status YAJL_API yajl_gen_number(yajl_gen hand,
                                             const char * num,
                                             unsigned int len);
    yajl_gen_status YAJL_API yajl_gen_string(yajl_gen hand,
                                             const unsigned char * str,
                                             unsigned int len);
    yajl_gen_status YAJL_API yajl_gen_null(yajl_gen hand);
    yajl_gen_status YAJL_API yajl_gen_bool(yajl_gen hand, int boolean);
    yajl_gen_status YAJL_API yajl_gen_map_open(yajl_gen hand);
    yajl_gen_status YAJL_API yajl_gen_map_close(yajl_gen hand);
    yajl_gen_status YAJL_API yajl_gen_array_open(yajl_gen hand);
    yajl_gen_status YAJL_API yajl_gen_array_close(yajl_gen hand);

    /** access the null terminated generator buffer.  If incrementally
     *  outputing JSON, one should call yajl_gen_clear to clear the
     *  buffer.  This allows stream generation. */
    yajl_gen_status YAJL_API yajl_gen_get_buf(yajl_gen hand,
                                              const unsigned char ** buf,
                                              unsigned int * len);

    /** clear yajl's output buffer, but maintain all internal generation
     *  state.  This function will not "reset" the generator state, and is
     *  intended to enable incremental JSON outputing. */
    void YAJL_API yajl_gen_clear(yajl_gen hand);


/* yajl_lex.h */
    typedef enum {
    yajl_tok_bool,
    yajl_tok_colon,
    yajl_tok_comma,
    yajl_tok_eof,
    yajl_tok_error,
    yajl_tok_left_brace,
    yajl_tok_left_bracket,
    yajl_tok_null,
    yajl_tok_right_brace,
    yajl_tok_right_bracket,

    /* we differentiate between integers and doubles to allow the
     * parser to interpret the number without re-scanning */
    yajl_tok_integer,
    yajl_tok_double,

    /* we differentiate between strings which require further processing,
     * and strings that do not */
    yajl_tok_string,
    yajl_tok_string_with_escapes,

    /* comment tokens are not currently returned to the parser, ever */
    yajl_tok_comment
} yajl_tok;

typedef struct yajl_lexer_t * yajl_lexer;

yajl_lexer yajl_lex_alloc(unsigned int allowComments,
                          unsigned int validateUTF8);

void yajl_lex_free(yajl_lexer lexer);

/**
 * run/continue a lex. "offset" is an input/output parameter.
 * It should be initialized to zero for a
 * new chunk of target text, and upon subsetquent calls with the same
 * target text should passed with the value of the previous invocation.
 *
 * the client may be interested in the value of offset when an error is
 * returned from the lexer.  This allows the client to render useful
n * error messages.
 *
 * When you pass the next chunk of data, context should be reinitialized
 * to zero.
 *
 * Finally, the output buffer is usually just a pointer into the jsonText,
 * however in cases where the entity being lexed spans multiple chunks,
 * the lexer will buffer the entity and the data returned will be
 * a pointer into that buffer.
 *
 * This behavior is abstracted from client code except for the performance
 * implications which require that the client choose a reasonable chunk
 * size to get adequate performance.
 */
yajl_tok yajl_lex_lex(yajl_lexer lexer, const unsigned char * jsonText,
                      unsigned int jsonTextLen, unsigned int * offset,
                      const unsigned char ** outBuf, unsigned int * outLen);

/** have a peek at the next token, but don't move the lexer forward */
yajl_tok yajl_lex_peek(yajl_lexer lexer, const unsigned char * jsonText,
                       unsigned int jsonTextLen, unsigned int offset);


typedef enum {
    yajl_lex_e_ok = 0,
    yajl_lex_string_invalid_utf8,
    yajl_lex_string_invalid_escaped_char,
    yajl_lex_string_invalid_json_char,
    yajl_lex_string_invalid_hex_char,
    yajl_lex_invalid_char,
    yajl_lex_invalid_string,
    yajl_lex_missing_integer_after_decimal,
    yajl_lex_missing_integer_after_exponent,
    yajl_lex_missing_integer_after_minus,
    yajl_lex_unallowed_comment
} yajl_lex_error;

const char * yajl_lex_error_to_string(yajl_lex_error error);

/** allows access to more specific information about the lexical
 *  error when yajl_lex_lex returns yajl_tok_error. */
yajl_lex_error yajl_lex_get_error(yajl_lexer lexer);

/** get the current offset into the most recently lexed json string. */
unsigned int yajl_lex_current_offset(yajl_lexer lexer);

/** get the number of lines lexed by this lexer instance */
unsigned int yajl_lex_current_line(yajl_lexer lexer);

/** get the number of chars lexed by this lexer instance since the last
 *  \n or \r */
unsigned int yajl_lex_current_char(yajl_lexer lexer);


/* yajl_parse.h */
    /** error codes returned from this interface */
    typedef enum {
        /** no error was encountered */
        yajl_status_ok,
        /** a client callback returned zero, stopping the parse */
        yajl_status_client_canceled,
        /** The parse cannot yet complete because more json input text
         *  is required, call yajl_parse with the next buffer of input text.
         *  (pertinent only when stream parsing) */
        yajl_status_insufficient_data,
        /** An error occured during the parse.  Call yajl_get_error for
         *  more information about the encountered error */
        yajl_status_error
    } yajl_status;

    /** attain a human readable, english, string for an error */
    const char * YAJL_API yajl_status_to_string(yajl_status code);

    /** an opaque handle to a parser */
    typedef struct yajl_handle_t * yajl_handle;

    /** yajl is an event driven parser.  this means as json elements are
     *  parsed, you are called back to do something with the data.  The
     *  functions in this table indicate the various events for which
     *  you will be called back.  Each callback accepts a "context"
     *  pointer, this is a void * that is passed into the yajl_parse
     *  function which the client code may use to pass around context.
     *
     *  All callbacks return an integer.  If non-zero, the parse will
     *  continue.  If zero, the parse will be canceled and
     *  yajl_status_client_canceled will be returned from the parse.
     *
     *  Note about handling of numbers:
     *    yajl will only convert numbers that can be represented in a double
     *    or a long int.  All other numbers will be passed to the client
     *    in string form using the yajl_number callback.  Furthermore, if
     *    yajl_number is not NULL, it will always be used to return numbers,
     *    that is yajl_integer and yajl_double will be ignored.  If
     *    yajl_number is NULL but one of yajl_integer or yajl_double are
     *    defined, parsing of a number larger than is representable
     *    in a double or long int will result in a parse error.
     */
    typedef struct {
        int (* yajl_null)(void * ctx);
        int (* yajl_boolean)(void * ctx, int boolVal);
        int (* yajl_integer)(void * ctx, long integerVal);
        int (* yajl_double)(void * ctx, double doubleVal);
        /** A callback which passes the string representation of the number
         *  back to the client.  Will be used for all numbers when present */
        int (* yajl_number)(void * ctx, const char * numberVal,
                            unsigned int numberLen);

        /** strings are returned as pointers into the JSON text when,
         * possible, as a result, they are _not_ null padded */
        int (* yajl_string)(void * ctx, const unsigned char * stringVal,
                            unsigned int stringLen);

        int (* yajl_start_map)(void * ctx);
        int (* yajl_map_key)(void * ctx, const unsigned char * key,
                             unsigned int stringLen);
        int (* yajl_end_map)(void * ctx);

        int (* yajl_start_array)(void * ctx);
        int (* yajl_end_array)(void * ctx);
    } yajl_callbacks;

    /** configuration structure for the generator */
    typedef struct {
        /** if nonzero, javascript style comments will be allowed in
         *  the json input, both slash star and slash slash */
        unsigned int allowComments;
        /** if nonzero, invalid UTF8 strings will cause a parse
         *  error */
        unsigned int checkUTF8;
    } yajl_parser_config;

    /** allocate a parser handle
     *  \param callbacks  a yajl callbacks structure specifying the
     *                    functions to call when different JSON entities
     *                    are encountered in the input text.  May be NULL,
     *                    which is only useful for validation.
     *  \param config     configuration parameters for the parse.
     *  \param ctx        a context pointer that will be passed to callbacks.
     */
    yajl_handle YAJL_API yajl_alloc(const yajl_callbacks * callbacks,
                                    const yajl_parser_config * config,
                                    void * ctx);

    /** free a parser handle */
    void YAJL_API yajl_free(yajl_handle handle);

    /** Parse some json!
     *  \param hand - a handle to the json parser allocated with yajl_alloc
     *  \param jsonText - a pointer to the UTF8 json text to be parsed
     *  \param jsonTextLength - the length, in bytes, of input text
     */
    yajl_status YAJL_API yajl_parse(yajl_handle hand,
                                    const unsigned char * jsonText,
                                    unsigned int jsonTextLength);

    /** get an error string describing the state of the
     *  parse.
     *
     *  If verbose is non-zero, the message will include the JSON
     *  text where the error occured, along with an arrow pointing to
     *  the specific char.
     *
     *  A dynamically allocated string will be returned which should
     *  be freed with yajl_free_error
     */
    unsigned char * YAJL_API yajl_get_error(yajl_handle hand, int verbose,
                                            const unsigned char * jsonText,
                                            unsigned int jsonTextLength);

    /** free an error returned from yajl_get_error */
    void YAJL_API yajl_free_error(unsigned char * str);


/* yajl_parser.h */
    typedef enum {
    yajl_state_start = 0,
    yajl_state_parse_complete,
    yajl_state_parse_error,
    yajl_state_lexical_error,
    yajl_state_map_start,
    yajl_state_map_sep,
    yajl_state_map_need_val,
    yajl_state_map_got_val,
    yajl_state_map_need_key,
    yajl_state_array_start,
    yajl_state_array_got_val,
    yajl_state_array_need_val
} yajl_state;

struct yajl_handle_t {
    const yajl_callbacks * callbacks;
    void * ctx;
    yajl_lexer lexer;
    const char * parseError;
    unsigned int errorOffset;
    /* temporary storage for decoded strings */
    yajl_buf decodeBuf;
    /* a stack of states.  access with yajl_state_XXX routines */
    yajl_buf stateBuf;
};

yajl_status
yajl_do_parse(yajl_handle handle, unsigned int * offset,
              const unsigned char * jsonText, unsigned int jsonTextLen);

unsigned char *
yajl_render_error_string(yajl_handle hand, const unsigned char * jsonText,
                         unsigned int jsonTextLen, int verbose);

yajl_state yajl_state_current(yajl_handle handle);

void yajl_state_push(yajl_handle handle, yajl_state state);

yajl_state yajl_state_pop(yajl_handle handle);

unsigned int yajl_parse_depth(yajl_handle handle);

void yajl_state_set(yajl_handle handle, yajl_state state);


#endif


// uriparser.c
/*
 * uriparser - RFC 3986 URI parsing library
 *
 * Copyright (C) 2007, Weijia Song <songweijia@gmail.com>
 * Copyright (C) 2007, Sebastian Pipping <webmaster@hartwork.org>
 * All rights reserved.
 *
 * Redistribution  and use in source and binary forms, with or without
 * modification,  are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions   of  source  code  must  retain  the   above
 *       copyright  notice, this list of conditions and the  following
 *       disclaimer.
 *
 *     * Redistributions  in  binary  form must  reproduce  the  above
 *       copyright  notice, this list of conditions and the  following
 *       disclaimer   in  the  documentation  and/or  other  materials
 *       provided with the distribution.
 *
 *     * Neither  the name of the <ORGANIZATION> nor the names of  its
 *       contributors  may  be  used to endorse  or  promote  products
 *       derived  from  this software without specific  prior  written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT  NOT
 * LIMITED  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND  FITNESS
 * FOR  A  PARTICULAR  PURPOSE ARE DISCLAIMED. IN NO EVENT  SHALL  THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL,    SPECIAL,   EXEMPLARY,   OR   CONSEQUENTIAL   DAMAGES
 * (INCLUDING,  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES;  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT  LIABILITY,  OR  TORT (INCLUDING  NEGLIGENCE  OR  OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */



/* Prototypes: UriCommon.h */
/* Used to point to from empty path segments.
 * X.first and X.afterLast must be the same non-NULL value then. */
extern const URI_CHAR * const URI_FUNC(SafeToPointTo);
extern const URI_CHAR * const URI_FUNC(ConstPwd);
extern const URI_CHAR * const URI_FUNC(ConstParent);



void URI_FUNC(ResetUri)(URI_TYPE(Uri) * uri);

UriBool URI_FUNC(RemoveDotSegmentsAbsolute)(URI_TYPE(Uri) * uri);
UriBool URI_FUNC(RemoveDotSegments)(URI_TYPE(Uri) * uri, UriBool relative);
UriBool URI_FUNC(RemoveDotSegmentsEx)(URI_TYPE(Uri) * uri,
        UriBool relative, UriBool pathOwned);

unsigned char URI_FUNC(HexdigToInt)(URI_CHAR hexdig);
URI_CHAR URI_FUNC(HexToLetter)(unsigned int value);
URI_CHAR URI_FUNC(HexToLetterEx)(unsigned int value, UriBool uppercase);

UriBool URI_FUNC(IsHostSet)(const URI_TYPE(Uri) * uri);

UriBool URI_FUNC(CopyPath)(URI_TYPE(Uri) * dest, const URI_TYPE(Uri) * source);
UriBool URI_FUNC(CopyAuthority)(URI_TYPE(Uri) * dest, const URI_TYPE(Uri) * source);

UriBool URI_FUNC(FixAmbiguity)(URI_TYPE(Uri) * uri);
void URI_FUNC(FixEmptyTrailSegment)(URI_TYPE(Uri) * uri);

/* Prototypes: UriCommon.c */
/*extern*/ const URI_CHAR * const URI_FUNC(SafeToPointTo) = _UT("X");
/*extern*/ const URI_CHAR * const URI_FUNC(ConstPwd) = _UT(".");
/*extern*/ const URI_CHAR * const URI_FUNC(ConstParent) = _UT("..");


/* Prototypes: UriCompare.c */
static int URI_FUNC(CompareRange)(const URI_TYPE(TextRange) * a,
    const URI_TYPE(TextRange) * b);


/* Prototypes: UriIpBase.h */
typedef struct UriIp4ParserStruct {
  unsigned char stackCount;
  unsigned char stackOne;
  unsigned char stackTwo;
  unsigned char stackThree;
} UriIp4Parser;



void uriPushToStack(UriIp4Parser * parser, unsigned char digit);
void uriStackToOctet(UriIp4Parser * parser, unsigned char * octet);

/* Prototypes: UriIp4.c */
static const URI_CHAR * URI_FUNC(ParseDecOctet)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseDecOctetOne)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseDecOctetTwo)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseDecOctetThree)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseDecOctetFour)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast);


/* Prototypes: UriNormalizeBase.h */
UriBool uriIsUnreserved(int code);

/* Prototypes: UriNormalize.c */
static int URI_FUNC(NormalizeSyntaxEngine)(URI_TYPE(Uri) * uri, unsigned int inMask,
    unsigned int * outMask);

static UriBool URI_FUNC(MakeRangeOwner)(unsigned int * doneMask,
    unsigned int maskTest, URI_TYPE(TextRange) * range);
static UriBool URI_FUNC(MakeOwner)(URI_TYPE(Uri) * uri,
    unsigned int * doneMask);

static void URI_FUNC(FixPercentEncodingInplace)(const URI_CHAR * first,
    const URI_CHAR ** afterLast);
static UriBool URI_FUNC(FixPercentEncodingMalloc)(const URI_CHAR ** first,
    const URI_CHAR ** afterLast);
static void URI_FUNC(FixPercentEncodingEngine)(
    const URI_CHAR * inFirst, const URI_CHAR * inAfterLast,
    const URI_CHAR * outFirst, const URI_CHAR ** outAfterLast);

static UriBool URI_FUNC(ContainsUppercaseLetters)(const URI_CHAR * first,
    const URI_CHAR * afterLast);
static UriBool URI_FUNC(ContainsUglyPercentEncoding)(const URI_CHAR * first,
    const URI_CHAR * afterLast);

static void URI_FUNC(LowercaseInplace)(const URI_CHAR * first,
    const URI_CHAR * afterLast);
static UriBool URI_FUNC(LowercaseMalloc)(const URI_CHAR ** first,
    const URI_CHAR ** afterLast);

static void URI_FUNC(PreventLeakage)(URI_TYPE(Uri) * uri,
    unsigned int revertMask);


/* Prototypes: UriParseBase.h */
void uriWriteQuadToDoubleByte(const unsigned char * hexDigits, int digitCount,
    unsigned char * output);
unsigned char uriGetOctetValue(const unsigned char * digits, int digitCount);

/* Prototypes: UriParse.c */
static const URI_CHAR * URI_FUNC(ParseAuthority)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseAuthorityTwo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseHexZero)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseHierPart)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseIpFutLoop)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseIpFutStopGo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseIpLit2)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseIPv6address2)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseMustBeSegmentNzNc)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseOwnHost)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseOwnHost2)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseOwnHostUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseOwnHostUserInfoNz)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseOwnPortUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseOwnUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParsePartHelperTwo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParsePathAbsEmpty)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParsePathAbsNoLeadSlash)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParsePathRootless)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParsePchar)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParsePctEncoded)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParsePctSubUnres)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParsePort)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseQueryFrag)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseSegment)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseSegmentNz)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseSegmentNzNcOrScheme2)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseUriReference)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseUriTail)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseUriTailTwo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);
static const URI_CHAR * URI_FUNC(ParseZeroMoreSlashSegs)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);

static UriBool URI_FUNC(OnExitOwnHost2)(URI_TYPE(ParserState) * state, const URI_CHAR * first);
static UriBool URI_FUNC(OnExitOwnHostUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first);
static UriBool URI_FUNC(OnExitOwnPortUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first);
static UriBool URI_FUNC(OnExitSegmentNzNcOrScheme2)(URI_TYPE(ParserState) * state, const URI_CHAR * first);
static void URI_FUNC(OnExitPartHelperTwo)(URI_TYPE(ParserState) * state);

static void URI_FUNC(ResetParserState)(URI_TYPE(ParserState) * state);

static UriBool URI_FUNC(PushPathSegment)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast);

static void URI_FUNC(StopSyntax)(URI_TYPE(ParserState) * state, const URI_CHAR * errorPos);
static void URI_FUNC(StopMalloc)(URI_TYPE(ParserState) * state);


/* Prototypes: UriQuery.c */
static int URI_FUNC(ComposeQueryEngine)(URI_CHAR * dest,
    const URI_TYPE(QueryList) * queryList,
    int maxChars, int * charsWritten, int * charsRequired,
    UriBool spaceToPlus, UriBool normalizeBreaks);

static UriBool URI_FUNC(AppendQueryItem)(URI_TYPE(QueryList) ** prevNext,
    int * itemCount, const URI_CHAR * keyFirst, const URI_CHAR * keyAfter,
    const URI_CHAR * valueFirst, const URI_CHAR * valueAfter,
    UriBool plusToSpace, UriBreakConversion breakConversion);


/* Prototypes: UriRecompose.c */
static int URI_FUNC(ToStringEngine)(URI_CHAR * dest, const URI_TYPE(Uri) * uri,
    int maxChars, int * charsWritten, int * charsRequired);




/* Source: UriCommon.c */
void URI_FUNC(ResetUri)(URI_TYPE(Uri) * uri) {
  memset(uri, 0, sizeof(URI_TYPE(Uri)));
}



/* Properly removes "." and ".." path segments */
UriBool URI_FUNC(RemoveDotSegments)(URI_TYPE(Uri) * uri,
    UriBool relative) {
  if (uri == NULL) {
    return URI_TRUE;
  }
  return URI_FUNC(RemoveDotSegmentsEx)(uri, relative, uri->owner);
}



UriBool URI_FUNC(RemoveDotSegmentsEx)(URI_TYPE(Uri) * uri,
        UriBool relative, UriBool pathOwned) {
  URI_TYPE(PathSegment) * walker;
  if ((uri == NULL) || (uri->pathHead == NULL)) {
    return URI_TRUE;
  }

  walker = uri->pathHead;
  walker->reserved = NULL; /* Prev pointer */
  do {
    UriBool removeSegment = URI_FALSE;
    int len = (int)(walker->text.afterLast - walker->text.first);
    switch (len) {
    case 1:
      if ((walker->text.first)[0] == _UT('.')) {
        /* "." segment -> remove if not essential */
        URI_TYPE(PathSegment) * const prev = ( URI_TYPE(PathSegment) * const )walker->reserved;
        URI_TYPE(PathSegment) * const nextBackup = ( URI_TYPE(PathSegment) * const )walker->next;

        /* Is this dot segment essential? */
        removeSegment = URI_TRUE;
        if (relative && (walker == uri->pathHead) && (walker->next != NULL)) {
          const URI_CHAR * ch = walker->next->text.first;
          for (; ch < walker->next->text.afterLast; ch++) {
            if (*ch == _UT(':')) {
              removeSegment = URI_FALSE;
              break;
            }
          }
        }

        if (removeSegment) {
          /* Last segment? */
          if (walker->next != NULL) {
            /* Not last segment */
            walker->next->reserved = prev;

            if (prev == NULL) {
              /* First but not last segment */
              uri->pathHead = walker->next;
            } else {
              /* Middle segment */
              prev->next = walker->next;
            }

            if (pathOwned && (walker->text.first != walker->text.afterLast)) {
              free((URI_CHAR *)walker->text.first);
            }
            free(walker);
          } else {
            /* Last segment */
            if (pathOwned && (walker->text.first != walker->text.afterLast)) {
              free((URI_CHAR *)walker->text.first);
            }

            if (prev == NULL) {
              /* Last and first */
              if (URI_FUNC(IsHostSet)(uri)) {
                /* Replace "." with empty segment to represent trailing slash */
                walker->text.first = URI_FUNC(SafeToPointTo);
                walker->text.afterLast = URI_FUNC(SafeToPointTo);
              } else {
                free(walker);

                uri->pathHead = NULL;
                uri->pathTail = NULL;
              }
            } else {
              /* Last but not first, replace "." with empty segment to represent trailing slash */
              walker->text.first = URI_FUNC(SafeToPointTo);
              walker->text.afterLast = URI_FUNC(SafeToPointTo);
            }
          }

          walker = nextBackup;
        }
      }
      break;

    case 2:
      if (((walker->text.first)[0] == _UT('.'))
          && ((walker->text.first)[1] == _UT('.'))) {
        /* Path ".." -> remove this and the previous segment */
        URI_TYPE(PathSegment) * const prev = ( URI_TYPE(PathSegment) * const )walker->reserved;
        URI_TYPE(PathSegment) * prevPrev;
        URI_TYPE(PathSegment) * const nextBackup = ( URI_TYPE(PathSegment) * const )walker->next;

        removeSegment = URI_TRUE;
        if (relative) {
          if (prev == NULL) {
            removeSegment = URI_FALSE;
          } else if ((prev != NULL)
              && ((prev->text.afterLast - prev->text.first) == 2)
              && ((prev->text.first)[0] == _UT('.'))
              && ((prev->text.first)[1] == _UT('.'))) {
            removeSegment = URI_FALSE;
          }
        }

        if (removeSegment) {
          if (prev != NULL) {
            /* Not first segment */
            prevPrev = ( URI_TYPE(PathSegment) * const )prev->reserved;
            if (prevPrev != NULL) {
              /* Not even prev is the first one */
              prevPrev->next = walker->next;
              if (walker->next != NULL) {
                walker->next->reserved = prevPrev;
              } else {
                /* Last segment -> insert "" segment to represent trailing slash, update tail */
                URI_TYPE(PathSegment) * const segment = ( URI_TYPE(PathSegment) * const )malloc(1 * sizeof(URI_TYPE(PathSegment)));
                if (segment == NULL) {
                  if (pathOwned && (walker->text.first != walker->text.afterLast)) {
                    free((URI_CHAR *)walker->text.first);
                  }
                  free(walker);

                  if (pathOwned && (prev->text.first != prev->text.afterLast)) {
                    free((URI_CHAR *)prev->text.first);
                  }
                  free(prev);

                  return URI_FALSE; /* Raises malloc error */
                }
                memset(segment, 0, sizeof(URI_TYPE(PathSegment)));
                segment->text.first = URI_FUNC(SafeToPointTo);
                segment->text.afterLast = URI_FUNC(SafeToPointTo);
                prevPrev->next = segment;
                uri->pathTail = segment;
              }

              if (pathOwned && (walker->text.first != walker->text.afterLast)) {
                free((URI_CHAR *)walker->text.first);
              }
              free(walker);

              if (pathOwned && (prev->text.first != prev->text.afterLast)) {
                free((URI_CHAR *)prev->text.first);
              }
              free(prev);

              walker = nextBackup;
            } else {
              /* Prev is the first segment */
              if (walker->next != NULL) {
                uri->pathHead = walker->next;
                walker->next->reserved = NULL;

                if (pathOwned && (walker->text.first != walker->text.afterLast)) {
                  free((URI_CHAR *)walker->text.first);
                }
                free(walker);
              } else {
                /* Re-use segment for "" path segment to represent trailing slash, update tail */
                URI_TYPE(PathSegment) * const segment = walker;
                if (pathOwned && (segment->text.first != segment->text.afterLast)) {
                  free((URI_CHAR *)segment->text.first);
                }
                segment->text.first = URI_FUNC(SafeToPointTo);
                segment->text.afterLast = URI_FUNC(SafeToPointTo);
                uri->pathHead = segment;
                uri->pathTail = segment;
              }

              if (pathOwned && (prev->text.first != prev->text.afterLast)) {
                free((URI_CHAR *)prev->text.first);
              }
              free(prev);

              walker = nextBackup;
            }
          } else {
            URI_TYPE(PathSegment) * const nextBackup = walker->next;
            /* First segment -> update head pointer */
            uri->pathHead = walker->next;
            if (walker->next != NULL) {
              walker->next->reserved = NULL;
            } else {
              /* Last segment -> update tail */
              uri->pathTail = NULL;
            }

            if (pathOwned && (walker->text.first != walker->text.afterLast)) {
              free((URI_CHAR *)walker->text.first);
            }
            free(walker);

            walker = nextBackup;
          }
        }
      }
      break;

    }

    if (!removeSegment) {
      if (walker->next != NULL) {
        walker->next->reserved = walker;
      } else {
        /* Last segment -> update tail */
        uri->pathTail = walker;
      }
      walker = walker->next;
    }
  } while (walker != NULL);

  return URI_TRUE;
}



/* Properly removes "." and ".." path segments */
UriBool URI_FUNC(RemoveDotSegmentsAbsolute)(URI_TYPE(Uri) * uri) {
  const UriBool ABSOLUTE = URI_FALSE;
  return URI_FUNC(RemoveDotSegments)(uri, ABSOLUTE);
}



unsigned char URI_FUNC(HexdigToInt)(URI_CHAR hexdig) {
  switch (hexdig) {
  case _UT('0'):
  case _UT('1'):
  case _UT('2'):
  case _UT('3'):
  case _UT('4'):
  case _UT('5'):
  case _UT('6'):
  case _UT('7'):
  case _UT('8'):
  case _UT('9'):
    return (unsigned char)(9 + hexdig - _UT('9'));

  case _UT('a'):
  case _UT('b'):
  case _UT('c'):
  case _UT('d'):
  case _UT('e'):
  case _UT('f'):
    return (unsigned char)(15 + hexdig - _UT('f'));

  case _UT('A'):
  case _UT('B'):
  case _UT('C'):
  case _UT('D'):
  case _UT('E'):
  case _UT('F'):
    return (unsigned char)(15 + hexdig - _UT('F'));

  default:
    return 0;
  }
}



URI_CHAR URI_FUNC(HexToLetter)(unsigned int value) {
  /* Uppercase recommended in section 2.1. of RFC 3986 *
   * http://tools.ietf.org/html/rfc3986#section-2.1    */
  return URI_FUNC(HexToLetterEx)(value, URI_TRUE);
}



URI_CHAR URI_FUNC(HexToLetterEx)(unsigned int value, UriBool uppercase) {
  switch (value) {
  case  0: return _UT('0');
  case  1: return _UT('1');
  case  2: return _UT('2');
  case  3: return _UT('3');
  case  4: return _UT('4');
  case  5: return _UT('5');
  case  6: return _UT('6');
  case  7: return _UT('7');
  case  8: return _UT('8');
  case  9: return _UT('9');

  case 10: return (uppercase == URI_TRUE) ? _UT('A') : _UT('a');
  case 11: return (uppercase == URI_TRUE) ? _UT('B') : _UT('b');
  case 12: return (uppercase == URI_TRUE) ? _UT('C') : _UT('c');
  case 13: return (uppercase == URI_TRUE) ? _UT('D') : _UT('d');
  case 14: return (uppercase == URI_TRUE) ? _UT('E') : _UT('e');
  default: return (uppercase == URI_TRUE) ? _UT('F') : _UT('f');
  }
}



/* Checks if a URI has the host component set. */
UriBool URI_FUNC(IsHostSet)(const URI_TYPE(Uri) * uri) {
  return (uri != NULL)
      && ((uri->hostText.first != NULL)
        || (uri->hostData.ip4 != NULL)
        || (uri->hostData.ip6 != NULL)
        || (uri->hostData.ipFuture.first != NULL)
      );
}



/* Copies the path segment list from one URI to another. */
UriBool URI_FUNC(CopyPath)(URI_TYPE(Uri) * dest,
    const URI_TYPE(Uri) * source) {
  if (source->pathHead == NULL) {
    /* No path component */
    dest->pathHead = NULL;
    dest->pathTail = NULL;
  } else {
    /* Copy list but not the text contained */
    URI_TYPE(PathSegment) * sourceWalker = source->pathHead;
    URI_TYPE(PathSegment) * destPrev = NULL;
    do {
      URI_TYPE(PathSegment) * cur = ( URI_TYPE(PathSegment) * const )malloc(sizeof(URI_TYPE(PathSegment)));
      if (cur == NULL) {
        /* Fix broken list */
        if (destPrev != NULL) {
          destPrev->next = NULL;
        }
        return URI_FALSE; /* Raises malloc error */
      }

      /* From this functions usage we know that *
       * the dest URI cannot be uri->owner      */
      cur->text = sourceWalker->text;
      if (destPrev == NULL) {
        /* First segment ever */
        dest->pathHead = cur;
      } else {
        destPrev->next = cur;
      }
      destPrev = cur;
      sourceWalker = sourceWalker->next;
    } while (sourceWalker != NULL);
    dest->pathTail = destPrev;
    dest->pathTail->next = NULL;
  }

  dest->absolutePath = source->absolutePath;
  return URI_TRUE;
}



/* Copies the authority part of an URI over to another. */
UriBool URI_FUNC(CopyAuthority)(URI_TYPE(Uri) * dest,
    const URI_TYPE(Uri) * source) {
  /* From this functions usage we know that *
   * the dest URI cannot be uri->owner      */

  /* Copy userInfo */
  dest->userInfo = source->userInfo;

  /* Copy hostText */
  dest->hostText = source->hostText;

  /* Copy hostData */
  if (source->hostData.ip4 != NULL) {
    dest->hostData.ip4 = ( UriIp4* )malloc(sizeof(UriIp4));
    if (dest->hostData.ip4 == NULL) {
      return URI_FALSE; /* Raises malloc error */
    }
    *(dest->hostData.ip4) = *(source->hostData.ip4);
    dest->hostData.ip6 = NULL;
    dest->hostData.ipFuture.first = NULL;
    dest->hostData.ipFuture.afterLast = NULL;
  } else if (source->hostData.ip6 != NULL) {
    dest->hostData.ip4 = NULL;
    dest->hostData.ip6 = ( UriIp6* )malloc(sizeof(UriIp6));
    if (dest->hostData.ip6 == NULL) {
      return URI_FALSE; /* Raises malloc error */
    }
    *(dest->hostData.ip6) = *(source->hostData.ip6);
    dest->hostData.ipFuture.first = NULL;
    dest->hostData.ipFuture.afterLast = NULL;
  } else {
    dest->hostData.ip4 = NULL;
    dest->hostData.ip6 = NULL;
    dest->hostData.ipFuture = source->hostData.ipFuture;
  }

  /* Copy portText */
  dest->portText = source->portText;

  return URI_TRUE;
}



UriBool URI_FUNC(FixAmbiguity)(URI_TYPE(Uri) * uri) {
  URI_TYPE(PathSegment) * segment;

  if (  /* Case 1: absolute path, empty first segment */
      (uri->absolutePath
      && (uri->pathHead != NULL)
      && (uri->pathHead->text.afterLast == uri->pathHead->text.first))

      /* Case 2: relative path, empty first and second segment */
      || (!uri->absolutePath
      && (uri->pathHead != NULL)
      && (uri->pathHead->next != NULL)
      && (uri->pathHead->text.afterLast == uri->pathHead->text.first)
      && (uri->pathHead->next->text.afterLast == uri->pathHead->next->text.first))) {
    /* NOOP */
  } else {
    return URI_TRUE;
  }

  segment = ( URI_TYPE(PathSegment) * const )malloc(1 * sizeof(URI_TYPE(PathSegment)));
  if (segment == NULL) {
    return URI_FALSE; /* Raises malloc error */
  }

  /* Insert "." segment in front */
  segment->next = uri->pathHead;
  segment->text.first = URI_FUNC(ConstPwd);
  segment->text.afterLast = URI_FUNC(ConstPwd) + 1;
  uri->pathHead = segment;
  return URI_TRUE;
}



void URI_FUNC(FixEmptyTrailSegment)(URI_TYPE(Uri) * uri) {
  /* Fix path if only one empty segment */
  if (!uri->absolutePath
      && !URI_FUNC(IsHostSet)(uri)
      && (uri->pathHead != NULL)
      && (uri->pathHead->next == NULL)
      && (uri->pathHead->text.first == uri->pathHead->text.afterLast)) {
    free(uri->pathHead);
    uri->pathHead = NULL;
    uri->pathTail = NULL;
  }
}


/* Source: UriIp4Base.c */
void uriStackToOctet(UriIp4Parser * parser, unsigned char * octet) {
  switch (parser->stackCount) {
  case 1:
    *octet = parser->stackOne;
    break;

  case 2:
    *octet = parser->stackOne * 10
        + parser->stackTwo;
    break;

  case 3:
    *octet = parser->stackOne * 100
        + parser->stackTwo * 10
        + parser->stackThree;
    break;

  default:
    ;
  }
  parser->stackCount = 0;
}



void uriPushToStack(UriIp4Parser * parser, unsigned char digit) {
  switch (parser->stackCount) {
  case 0:
    parser->stackOne = digit;
    parser->stackCount = 1;
    break;

  case 1:
    parser->stackTwo = digit;
    parser->stackCount = 2;
    break;

  case 2:
    parser->stackThree = digit;
    parser->stackCount = 3;
    break;

  default:
    ;
  }
}


/* Source: UriIp4.c */

/*
 * [ipFourAddress]->[decOctet]<.>[decOctet]<.>[decOctet]<.>[decOctet]
 */
int URI_FUNC(ParseIpFourAddress)(unsigned char * octetOutput,
    const URI_CHAR * first, const URI_CHAR * afterLast) {
  const URI_CHAR * after;
  UriIp4Parser parser;

  /* Essential checks */
  if ((octetOutput == NULL) || (first == NULL)
      || (afterLast <= first)) {
    return URI_ERROR_SYNTAX;
  }

  /* Reset parser */
  parser.stackCount = 0;

  /* Octet #1 */
  after = URI_FUNC(ParseDecOctet)(&parser, first, afterLast);
  if ((after == NULL) || (after >= afterLast) || (*after != _UT('.'))) {
    return URI_ERROR_SYNTAX;
  }
  uriStackToOctet(&parser, octetOutput);

  /* Octet #2 */
  after = URI_FUNC(ParseDecOctet)(&parser, after + 1, afterLast);
  if ((after == NULL) || (after >= afterLast) || (*after != _UT('.'))) {
    return URI_ERROR_SYNTAX;
  }
  uriStackToOctet(&parser, octetOutput + 1);

  /* Octet #3 */
  after = URI_FUNC(ParseDecOctet)(&parser, after + 1, afterLast);
  if ((after == NULL) || (after >= afterLast) || (*after != _UT('.'))) {
    return URI_ERROR_SYNTAX;
  }
  uriStackToOctet(&parser, octetOutput + 2);

  /* Octet #4 */
  after = URI_FUNC(ParseDecOctet)(&parser, after + 1, afterLast);
  if (after != afterLast) {
    return URI_ERROR_SYNTAX;
  }
  uriStackToOctet(&parser, octetOutput + 3);

  return URI_SUCCESS;
}



/*
 * [decOctet]-><0>
 * [decOctet]-><1>[decOctetOne]
 * [decOctet]-><2>[decOctetTwo]
 * [decOctet]-><3>[decOctetThree]
 * [decOctet]-><4>[decOctetThree]
 * [decOctet]-><5>[decOctetThree]
 * [decOctet]-><6>[decOctetThree]
 * [decOctet]-><7>[decOctetThree]
 * [decOctet]-><8>[decOctetThree]
 * [decOctet]-><9>[decOctetThree]
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseDecOctet)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return NULL;
  }

  switch (*first) {
  case _UT('0'):
    uriPushToStack(parser, 0);
    return first + 1;

  case _UT('1'):
    uriPushToStack(parser, 1);
    return (const URI_CHAR *)URI_FUNC(ParseDecOctetOne)(parser, first + 1, afterLast);

  case _UT('2'):
    uriPushToStack(parser, 2);
    return (const URI_CHAR *)URI_FUNC(ParseDecOctetTwo)(parser, first + 1, afterLast);

  case _UT('3'):
  case _UT('4'):
  case _UT('5'):
  case _UT('6'):
  case _UT('7'):
  case _UT('8'):
  case _UT('9'):
    uriPushToStack(parser, (unsigned char)(9 + *first - _UT('9')));
    return (const URI_CHAR *)URI_FUNC(ParseDecOctetThree)(parser, first + 1, afterLast);

  default:
    return NULL;
  }
}



/*
 * [decOctetOne]-><NULL>
 * [decOctetOne]->[DIGIT][decOctetThree]
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseDecOctetOne)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('0'):
  case _UT('1'):
  case _UT('2'):
  case _UT('3'):
  case _UT('4'):
  case _UT('5'):
  case _UT('6'):
  case _UT('7'):
  case _UT('8'):
  case _UT('9'):
    uriPushToStack(parser, (unsigned char)(9 + *first - _UT('9')));
    return (const URI_CHAR *)URI_FUNC(ParseDecOctetThree)(parser, first + 1, afterLast);

  default:
    return first;
  }
}



/*
 * [decOctetTwo]-><NULL>
 * [decOctetTwo]-><0>[decOctetThree]
 * [decOctetTwo]-><1>[decOctetThree]
 * [decOctetTwo]-><2>[decOctetThree]
 * [decOctetTwo]-><3>[decOctetThree]
 * [decOctetTwo]-><4>[decOctetThree]
 * [decOctetTwo]-><5>[decOctetFour]
 * [decOctetTwo]-><6>
 * [decOctetTwo]-><7>
 * [decOctetTwo]-><8>
 * [decOctetTwo]-><9>
*/
static URI_INLINE const URI_CHAR * URI_FUNC(ParseDecOctetTwo)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('0'):
  case _UT('1'):
  case _UT('2'):
  case _UT('3'):
  case _UT('4'):
    uriPushToStack(parser, (unsigned char)(9 + *first - _UT('9')));
    return (const URI_CHAR *)URI_FUNC(ParseDecOctetThree)(parser, first + 1, afterLast);

  case _UT('5'):
    uriPushToStack(parser, 5);
    return (const URI_CHAR *)URI_FUNC(ParseDecOctetFour)(parser, first + 1, afterLast);

  case _UT('6'):
  case _UT('7'):
  case _UT('8'):
  case _UT('9'):
    uriPushToStack(parser, (unsigned char)(9 + *first - _UT('9')));
    return first + 1;

  default:
    return first;
  }
}



/*
 * [decOctetThree]-><NULL>
 * [decOctetThree]->[DIGIT]
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseDecOctetThree)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('0'):
  case _UT('1'):
  case _UT('2'):
  case _UT('3'):
  case _UT('4'):
  case _UT('5'):
  case _UT('6'):
  case _UT('7'):
  case _UT('8'):
  case _UT('9'):
    uriPushToStack(parser, (unsigned char)(9 + *first - _UT('9')));
    return first + 1;

  default:
    return first;
  }
}



/*
 * [decOctetFour]-><NULL>
 * [decOctetFour]-><0>
 * [decOctetFour]-><1>
 * [decOctetFour]-><2>
 * [decOctetFour]-><3>
 * [decOctetFour]-><4>
 * [decOctetFour]-><5>
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseDecOctetFour)(UriIp4Parser * parser,
    const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('0'):
  case _UT('1'):
  case _UT('2'):
  case _UT('3'):
  case _UT('4'):
  case _UT('5'):
    uriPushToStack(parser, (unsigned char)(9 + *first - _UT('9')));
    return first + 1;

  default:
    return first;
  }
}


/* Source: UriCompare.c */
static int URI_FUNC(CompareRange)(const URI_TYPE(TextRange) * a,
    const URI_TYPE(TextRange) * b);



/* Compares two text ranges for equal text content */
static URI_INLINE int URI_FUNC(CompareRange)(const URI_TYPE(TextRange) * a,
    const URI_TYPE(TextRange) * b) {
  int diff;

  /* NOTE: Both NULL means equal! */
  if ((a == NULL) || (b == NULL)) {
    return ((a == NULL) && (b == NULL)) ? URI_TRUE : URI_FALSE;
  }

  diff = ((int)(a->afterLast - a->first) - (int)(b->afterLast - b->first));
  if (diff > 0) {
    return 1;
  } else if (diff < 0) {
    return -1;
  }

  return URI_STRNCMP(a->first, b->first, (a->afterLast - a->first));
}



UriBool URI_FUNC(EqualsUri)(const URI_TYPE(Uri) * a,
    const URI_TYPE(Uri) * b) {
  /* NOTE: Both NULL means equal! */
  if ((a == NULL) || (b == NULL)) {
    return ((a == NULL) && (b == NULL)) ? URI_TRUE : URI_FALSE;
  }

  /* scheme */
  if (URI_FUNC(CompareRange)(&(a->scheme), &(b->scheme))) {
    return URI_FALSE;
  }

  /* absolutePath */
  if ((a->scheme.first == NULL)&& (a->absolutePath != b->absolutePath)) {
    return URI_FALSE;
  }

  /* userInfo */
  if (URI_FUNC(CompareRange)(&(a->userInfo), &(b->userInfo))) {
    return URI_FALSE;
  }

  /* Host */
  if (((a->hostData.ip4 == NULL) != (b->hostData.ip4 == NULL))
      || ((a->hostData.ip6 == NULL) != (b->hostData.ip6 == NULL))
      || ((a->hostData.ipFuture.first == NULL)
        != (b->hostData.ipFuture.first == NULL))) {
    return URI_FALSE;
  }

  if (a->hostData.ip4 != NULL) {
    if (memcmp(a->hostData.ip4->data, b->hostData.ip4->data, 4)) {
      return URI_FALSE;
    }
  }

  if (a->hostData.ip6 != NULL) {
    if (memcmp(a->hostData.ip6->data, b->hostData.ip6->data, 16)) {
      return URI_FALSE;
    }
  }

  if (a->hostData.ipFuture.first != NULL) {
    if (URI_FUNC(CompareRange)(&(a->hostData.ipFuture), &(b->hostData.ipFuture))) {
      return URI_FALSE;
    }
  }

  if ((a->hostData.ip4 == NULL)
      && (a->hostData.ip6 == NULL)
      && (a->hostData.ipFuture.first == NULL)) {
    if (URI_FUNC(CompareRange)(&(a->hostText), &(b->hostText))) {
      return URI_FALSE;
    }
  }

  /* portText */
  if (URI_FUNC(CompareRange)(&(a->portText), &(b->portText))) {
    return URI_FALSE;
  }

  /* Path */
  if ((a->pathHead == NULL) != (b->pathHead == NULL)) {
    return URI_FALSE;
  }

  if (a->pathHead != NULL) {
    URI_TYPE(PathSegment) * walkA = a->pathHead;
    URI_TYPE(PathSegment) * walkB = b->pathHead;
    do {
      if (URI_FUNC(CompareRange)(&(walkA->text), &(walkB->text))) {
        return URI_FALSE;
      }
      if ((walkA->next == NULL) != (walkB->next == NULL)) {
        return URI_FALSE;
      }
      walkA = walkA->next;
      walkB = walkB->next;
    } while (walkA != NULL);
  }

  /* query */
  if (URI_FUNC(CompareRange)(&(a->query), &(b->query))) {
    return URI_FALSE;
  }

  /* fragment */
  if (URI_FUNC(CompareRange)(&(a->fragment), &(b->fragment))) {
    return URI_FALSE;
  }

  return URI_TRUE; /* Equal*/
}


/* Source: UriEscape.c */
URI_CHAR * URI_FUNC(Escape)(const URI_CHAR * in, URI_CHAR * out,
    UriBool spaceToPlus, UriBool normalizeBreaks) {
  return URI_FUNC(EscapeEx)(in, NULL, out, spaceToPlus, normalizeBreaks);
}



URI_CHAR * URI_FUNC(EscapeEx)(const URI_CHAR * inFirst,
    const URI_CHAR * inAfterLast, URI_CHAR * out,
    UriBool spaceToPlus, UriBool normalizeBreaks) {
  const URI_CHAR * read = inFirst;
  URI_CHAR * write = out;
  UriBool prevWasCr = URI_FALSE;
  if ((out == NULL) || (inFirst == out)) {
    return NULL;
  } else if (inFirst == NULL) {
    if (out != NULL) {
      out[0] = _UT('\0');
    }
    return out;
  }

  for (;;) {
    if ((inAfterLast != NULL) && (read >= inAfterLast)) {
      write[0] = _UT('\0');
      return write;
    }

    switch (read[0]) {
    case _UT('\0'):
      write[0] = _UT('\0');
      return write;

    case _UT(' '):
      if (spaceToPlus) {
        write[0] = _UT('+');
        write++;
      } else {
        write[0] = _UT('%');
        write[1] = _UT('2');
        write[2] = _UT('0');
        write += 3;
      }
      prevWasCr = URI_FALSE;
      break;

    case _UT('a'): /* ALPHA */
    case _UT('A'):
    case _UT('b'):
    case _UT('B'):
    case _UT('c'):
    case _UT('C'):
    case _UT('d'):
    case _UT('D'):
    case _UT('e'):
    case _UT('E'):
    case _UT('f'):
    case _UT('F'):
    case _UT('g'):
    case _UT('G'):
    case _UT('h'):
    case _UT('H'):
    case _UT('i'):
    case _UT('I'):
    case _UT('j'):
    case _UT('J'):
    case _UT('k'):
    case _UT('K'):
    case _UT('l'):
    case _UT('L'):
    case _UT('m'):
    case _UT('M'):
    case _UT('n'):
    case _UT('N'):
    case _UT('o'):
    case _UT('O'):
    case _UT('p'):
    case _UT('P'):
    case _UT('q'):
    case _UT('Q'):
    case _UT('r'):
    case _UT('R'):
    case _UT('s'):
    case _UT('S'):
    case _UT('t'):
    case _UT('T'):
    case _UT('u'):
    case _UT('U'):
    case _UT('v'):
    case _UT('V'):
    case _UT('w'):
    case _UT('W'):
    case _UT('x'):
    case _UT('X'):
    case _UT('y'):
    case _UT('Y'):
    case _UT('z'):
    case _UT('Z'):
    case _UT('0'): /* DIGIT */
    case _UT('1'):
    case _UT('2'):
    case _UT('3'):
    case _UT('4'):
    case _UT('5'):
    case _UT('6'):
    case _UT('7'):
    case _UT('8'):
    case _UT('9'):
    case _UT('-'): /* "-" / "." / "_" / "~" */
    case _UT('.'):
    case _UT('_'):
    case _UT('~'):
      /* Copy unmodified */
      write[0] = read[0];
      write++;

      prevWasCr = URI_FALSE;
      break;

    case _UT('\x0a'):
      if (normalizeBreaks) {
        if (!prevWasCr) {
          write[0] = _UT('%');
          write[1] = _UT('0');
          write[2] = _UT('D');
          write[3] = _UT('%');
          write[4] = _UT('0');
          write[5] = _UT('A');
          write += 6;
        }
      } else {
        write[0] = _UT('%');
        write[1] = _UT('0');
        write[2] = _UT('A');
        write += 3;
      }
      prevWasCr = URI_FALSE;
      break;

    case _UT('\x0d'):
      if (normalizeBreaks) {
        write[0] = _UT('%');
        write[1] = _UT('0');
        write[2] = _UT('D');
        write[3] = _UT('%');
        write[4] = _UT('0');
        write[5] = _UT('A');
        write += 6;
      } else {
        write[0] = _UT('%');
        write[1] = _UT('0');
        write[2] = _UT('D');
        write += 3;
      }
      prevWasCr = URI_TRUE;
      break;

    default:
      /* Percent encode */
      {
        const unsigned char code = (unsigned char)read[0];
        write[0] = _UT('%');
        write[1] = URI_FUNC(HexToLetter)(code >> 4);
        write[2] = URI_FUNC(HexToLetter)(code & 0x0f);
        write += 3;
      }
      prevWasCr = URI_FALSE;
      break;
    }

    read++;
  }
}



const URI_CHAR * URI_FUNC(UnescapeInPlace)(URI_CHAR * inout) {
  return URI_FUNC(UnescapeInPlaceEx)(inout, URI_FALSE, URI_BR_DONT_TOUCH);
}



const URI_CHAR * URI_FUNC(UnescapeInPlaceEx)(URI_CHAR * inout,
    UriBool plusToSpace, UriBreakConversion breakConversion) {
  URI_CHAR * read = inout;
  URI_CHAR * write = inout;
  UriBool prevWasCr = URI_FALSE;

  if (inout == NULL) {
    return NULL;
  }

  for (;;) {
    switch (read[0]) {
    case _UT('\0'):
      if (read > write) {
        write[0] = _UT('\0');
      }
      return write;

    case _UT('%'):
      switch (read[1]) {
      case _UT('0'):
      case _UT('1'):
      case _UT('2'):
      case _UT('3'):
      case _UT('4'):
      case _UT('5'):
      case _UT('6'):
      case _UT('7'):
      case _UT('8'):
      case _UT('9'):
      case _UT('a'):
      case _UT('b'):
      case _UT('c'):
      case _UT('d'):
      case _UT('e'):
      case _UT('f'):
      case _UT('A'):
      case _UT('B'):
      case _UT('C'):
      case _UT('D'):
      case _UT('E'):
      case _UT('F'):
        switch (read[2]) {
        case _UT('0'):
        case _UT('1'):
        case _UT('2'):
        case _UT('3'):
        case _UT('4'):
        case _UT('5'):
        case _UT('6'):
        case _UT('7'):
        case _UT('8'):
        case _UT('9'):
        case _UT('a'):
        case _UT('b'):
        case _UT('c'):
        case _UT('d'):
        case _UT('e'):
        case _UT('f'):
        case _UT('A'):
        case _UT('B'):
        case _UT('C'):
        case _UT('D'):
        case _UT('E'):
        case _UT('F'):
          {
            /* Percent group found */
            const unsigned char left = URI_FUNC(HexdigToInt)(read[1]);
            const unsigned char right = URI_FUNC(HexdigToInt)(read[2]);
            const int code = 16 * left + right;
            switch (code) {
            case 10:
              switch (breakConversion) {
              case URI_BR_TO_LF:
                if (!prevWasCr) {
                  write[0] = (URI_CHAR)10;
                  write++;
                }
                break;

              case URI_BR_TO_CRLF:
                if (!prevWasCr) {
                  write[0] = (URI_CHAR)13;
                  write[1] = (URI_CHAR)10;
                  write += 2;
                }
                break;

              case URI_BR_TO_CR:
                if (!prevWasCr) {
                  write[0] = (URI_CHAR)13;
                  write++;
                }
                break;

              case URI_BR_DONT_TOUCH:
              default:
                write[0] = (URI_CHAR)10;
                write++;

              }
              prevWasCr = URI_FALSE;
              break;

            case 13:
              switch (breakConversion) {
              case URI_BR_TO_LF:
                write[0] = (URI_CHAR)10;
                write++;
                break;

              case URI_BR_TO_CRLF:
                write[0] = (URI_CHAR)13;
                write[1] = (URI_CHAR)10;
                write += 2;
                break;

              case URI_BR_TO_CR:
                write[0] = (URI_CHAR)13;
                write++;
                break;

              case URI_BR_DONT_TOUCH:
              default:
                write[0] = (URI_CHAR)13;
                write++;

              }
              prevWasCr = URI_TRUE;
              break;

            default:
              write[0] = (URI_CHAR)(code);
              write++;

              prevWasCr = URI_FALSE;

            }
            read += 3;
          }
          break;

        default:
          /* Copy two chars unmodified and */
          /* look at this char again */
          if (read > write) {
            write[0] = read[0];
            write[1] = read[1];
          }
          read += 2;
          write += 2;

          prevWasCr = URI_FALSE;
        }
        break;

      default:
        /* Copy one char unmodified and */
        /* look at this char again */
        if (read > write) {
          write[0] = read[0];
        }
        read++;
        write++;

        prevWasCr = URI_FALSE;
      }
      break;

    case _UT('+'):
      if (plusToSpace) {
        /* Convert '+' to ' ' */
        write[0] = _UT(' ');
      } else {
        /* Copy one char unmodified */
        if (read > write) {
          write[0] = read[0];
        }
      }
      read++;
      write++;

      prevWasCr = URI_FALSE;
      break;

    default:
      /* Copy one char unmodified */
      if (read > write) {
        write[0] = read[0];
      }
      read++;
      write++;

      prevWasCr = URI_FALSE;
    }
  }
}


/* Source: UriFile.c */

static URI_INLINE int URI_FUNC(FilenameToUriString)(const URI_CHAR * filename,
    URI_CHAR * uriString, UriBool fromUnix) {
  const URI_CHAR * input = filename;
  const URI_CHAR * lastSep = input - 1;
  UriBool firstSegment = URI_TRUE;
  URI_CHAR * output = uriString;
  const UriBool absolute = (filename != NULL) && ((fromUnix && (filename[0] == _UT('/')))
      || (!fromUnix && (filename[0] != _UT('\0')) && (filename[1] == _UT(':'))));

  if ((filename == NULL) || (uriString == NULL)) {
    return URI_ERROR_NULL;
  }

  if (absolute) {
    const URI_CHAR * const prefix = fromUnix ? _UT("file://") : _UT("file:///");
    const int prefixLen = fromUnix ? 7 : 8;

    /* Copy prefix */
    memcpy(uriString, prefix, prefixLen * sizeof(URI_CHAR));
    output += prefixLen;
  }

  /* Copy and escape on the fly */
  for (;;) {
    if ((input[0] == _UT('\0'))
        || (fromUnix && input[0] == _UT('/'))
        || (!fromUnix && input[0] == _UT('\\'))) {
      /* Copy text after last seperator */
      if (lastSep + 1 < input) {
        if (!fromUnix && absolute && (firstSegment == URI_TRUE)) {
          /* Quick hack to not convert "C:" to "C%3A" */
          const int charsToCopy = (int)(input - (lastSep + 1));
          memcpy(output, lastSep + 1, charsToCopy * sizeof(URI_CHAR));
          output += charsToCopy;
        } else {
          output = URI_FUNC(EscapeEx)(lastSep + 1, input, output,
              URI_FALSE, URI_FALSE);
        }
      }
      firstSegment = URI_FALSE;
    }

    if (input[0] == _UT('\0')) {
      output[0] = _UT('\0');
      break;
    } else if (fromUnix && (input[0] == _UT('/'))) {
      /* Copy separators unmodified */
      output[0] = _UT('/');
      output++;
      lastSep = input;
    } else if (!fromUnix && (input[0] == _UT('\\'))) {
      /* Convert backslashes to forward slashes */
      output[0] = _UT('/');
      output++;
      lastSep = input;
    }
    input++;
  }

  return URI_SUCCESS;
}



static URI_INLINE int URI_FUNC(UriStringToFilename)(const URI_CHAR * uriString,
    URI_CHAR * filename, UriBool toUnix) {
  const URI_CHAR * const prefix = toUnix ? _UT("file://") : _UT("file:///");
  const int prefixLen = toUnix ? 7 : 8;
  URI_CHAR * walker = filename;
  size_t charsToCopy;
  const UriBool absolute = (URI_STRNCMP(uriString, prefix, prefixLen) == 0);
  const int charsToSkip = (absolute ? prefixLen : 0);

  charsToCopy = URI_STRLEN(uriString + charsToSkip) + 1;
  memcpy(filename, uriString + charsToSkip, charsToCopy * sizeof(URI_CHAR));
  URI_FUNC(UnescapeInPlaceEx)(filename, URI_FALSE, URI_BR_DONT_TOUCH);

  /* Convert forward slashes to backslashes */
  if (!toUnix) {
    while (walker[0] != _UT('\0')) {
      if (walker[0] == _UT('/')) {
        walker[0] = _UT('\\');
      }
      walker++;
    }
  }

  return URI_SUCCESS;
}



int URI_FUNC(UnixFilenameToUriString)(const URI_CHAR * filename, URI_CHAR * uriString) {
  return URI_FUNC(FilenameToUriString)(filename, uriString, URI_TRUE);
}



int URI_FUNC(WindowsFilenameToUriString)(const URI_CHAR * filename, URI_CHAR * uriString) {
  return URI_FUNC(FilenameToUriString)(filename, uriString, URI_FALSE);
}



int URI_FUNC(UriStringToUnixFilename)(const URI_CHAR * uriString, URI_CHAR * filename) {
  return URI_FUNC(UriStringToFilename)(uriString, filename, URI_TRUE);
}



int URI_FUNC(UriStringToWindowsFilename)(const URI_CHAR * uriString, URI_CHAR * filename) {
  return URI_FUNC(UriStringToFilename)(uriString, filename, URI_FALSE);
}


/* Source: UriNormalizeBase.c */
UriBool uriIsUnreserved(int code) {
  switch (code) {
  case L'a': /* ALPHA */
  case L'A':
  case L'b':
  case L'B':
  case L'c':
  case L'C':
  case L'd':
  case L'D':
  case L'e':
  case L'E':
  case L'f':
  case L'F':
  case L'g':
  case L'G':
  case L'h':
  case L'H':
  case L'i':
  case L'I':
  case L'j':
  case L'J':
  case L'k':
  case L'K':
  case L'l':
  case L'L':
  case L'm':
  case L'M':
  case L'n':
  case L'N':
  case L'o':
  case L'O':
  case L'p':
  case L'P':
  case L'q':
  case L'Q':
  case L'r':
  case L'R':
  case L's':
  case L'S':
  case L't':
  case L'T':
  case L'u':
  case L'U':
  case L'v':
  case L'V':
  case L'w':
  case L'W':
  case L'x':
  case L'X':
  case L'y':
  case L'Y':
  case L'z':
  case L'Z':
  case L'0': /* DIGIT */
  case L'1':
  case L'2':
  case L'3':
  case L'4':
  case L'5':
  case L'6':
  case L'7':
  case L'8':
  case L'9':
  case L'-': /* "-" / "." / "_" / "~" */
  case L'.':
  case L'_':
  case L'~':
    return URI_TRUE;

  default:
    return URI_FALSE;
  }
}


/* Source: UriNormalize.c */

static URI_INLINE void URI_FUNC(PreventLeakage)(URI_TYPE(Uri) * uri,
    unsigned int revertMask) {
  if (revertMask & URI_NORMALIZE_SCHEME) {
    free((URI_CHAR *)uri->scheme.first);
    uri->scheme.first = NULL;
    uri->scheme.afterLast = NULL;
  }

  if (revertMask & URI_NORMALIZE_USER_INFO) {
    free((URI_CHAR *)uri->userInfo.first);
    uri->userInfo.first = NULL;
    uri->userInfo.afterLast = NULL;
  }

  if (revertMask & URI_NORMALIZE_HOST) {
    if (uri->hostData.ipFuture.first != NULL) {
      /* IPvFuture */
      free((URI_CHAR *)uri->hostData.ipFuture.first);
      uri->hostData.ipFuture.first = NULL;
      uri->hostData.ipFuture.afterLast = NULL;
      uri->hostText.first = NULL;
      uri->hostText.afterLast = NULL;
    } else if ((uri->hostText.first != NULL)
        && (uri->hostData.ip4 == NULL)
        && (uri->hostData.ip6 == NULL)) {
      /* Regname */
      free((URI_CHAR *)uri->hostText.first);
      uri->hostText.first = NULL;
      uri->hostText.afterLast = NULL;
    }
  }

  /* NOTE: Port cannot happen! */

  if (revertMask & URI_NORMALIZE_PATH) {
    URI_TYPE(PathSegment) * walker = uri->pathHead;
    while (walker != NULL) {
      URI_TYPE(PathSegment) * const next = walker->next;
      if (walker->text.afterLast > walker->text.first) {
        free((URI_CHAR *)walker->text.first);
      }
      free(walker);
      walker = next;
    }
    uri->pathHead = NULL;
    uri->pathTail = NULL;
  }

  if (revertMask & URI_NORMALIZE_QUERY) {
    free((URI_CHAR *)uri->query.first);
    uri->query.first = NULL;
    uri->query.afterLast = NULL;
  }

  if (revertMask & URI_NORMALIZE_FRAGMENT) {
    free((URI_CHAR *)uri->fragment.first);
    uri->fragment.first = NULL;
    uri->fragment.afterLast = NULL;
  }
}



static URI_INLINE UriBool URI_FUNC(ContainsUppercaseLetters)(const URI_CHAR * first,
    const URI_CHAR * afterLast) {
  if ((first != NULL) && (afterLast != NULL) && (afterLast > first)) {
    const URI_CHAR * i = first;
    for (; i < afterLast; i++) {
      /* 6.2.2.1 Case Normalization: uppercase letters in scheme or host */
      if ((*i >= _UT('A')) && (*i <= _UT('Z'))) {
        return URI_TRUE;
      }
    }
  }
  return URI_FALSE;
}



static URI_INLINE UriBool URI_FUNC(ContainsUglyPercentEncoding)(const URI_CHAR * first,
    const URI_CHAR * afterLast) {
  if ((first != NULL) && (afterLast != NULL) && (afterLast > first)) {
    const URI_CHAR * i = first;
    for (; i + 2 < afterLast; i++) {
      if (i[0] == _UT('%')) {
        /* 6.2.2.1 Case Normalization: *
         * lowercase percent-encodings */
        if (((i[1] >= _UT('a')) && (i[1] <= _UT('f')))
            || ((i[2] >= _UT('a')) && (i[2] <= _UT('f')))) {
          return URI_TRUE;
        } else {
          /* 6.2.2.2 Percent-Encoding Normalization: *
           * percent-encoded unreserved characters   */
          const unsigned char left = URI_FUNC(HexdigToInt)(i[1]);
          const unsigned char right = URI_FUNC(HexdigToInt)(i[2]);
          const int code = 16 * left + right;
          if (uriIsUnreserved(code)) {
            return URI_TRUE;
          }
        }
      }
    }
  }
  return URI_FALSE;
}



static URI_INLINE void URI_FUNC(LowercaseInplace)(const URI_CHAR * first,
    const URI_CHAR * afterLast) {
  if ((first != NULL) && (afterLast != NULL) && (afterLast > first)) {
    URI_CHAR * i = (URI_CHAR *)first;
    const int lowerUpperDiff = (_UT('a') - _UT('A'));
    for (; i < afterLast; i++) {
      if ((*i >= _UT('A')) && (*i <=_UT('Z'))) {
        *i = (URI_CHAR)(*i + lowerUpperDiff);
      }
    }
  }
}



static URI_INLINE UriBool URI_FUNC(LowercaseMalloc)(const URI_CHAR ** first,
    const URI_CHAR ** afterLast) {
  int lenInChars;
  const int lowerUpperDiff = (_UT('a') - _UT('A'));
  URI_CHAR * buffer;
  int i = 0;

  if ((first == NULL) || (afterLast == NULL) || (*first == NULL)
      || (*afterLast == NULL)) {
    return URI_FALSE;
  }

  lenInChars = (int)(*afterLast - *first);
  if (lenInChars == 0) {
    return URI_TRUE;
  } else if (lenInChars < 0) {
    return URI_FALSE;
  }

  buffer = ( URI_CHAR* )malloc(lenInChars * sizeof(URI_CHAR));
  if (buffer == NULL) {
    return URI_FALSE;
  }

  for (; i < lenInChars; i++) {
    if (((*first)[i] >= _UT('A')) && ((*first)[i] <=_UT('Z'))) {
      buffer[i] = (URI_CHAR)((*first)[i] + lowerUpperDiff);
    } else {
      buffer[i] = (*first)[i];
    }
  }

  *first = buffer;
  *afterLast = buffer + lenInChars;
  return URI_TRUE;
}



/* NOTE: Implementation must stay inplace-compatible */
static URI_INLINE void URI_FUNC(FixPercentEncodingEngine)(
    const URI_CHAR * inFirst, const URI_CHAR * inAfterLast,
    const URI_CHAR * outFirst, const URI_CHAR ** outAfterLast) {
  URI_CHAR * write = (URI_CHAR *)outFirst;
  const int lenInChars = (int)(inAfterLast - inFirst);
  int i = 0;

  /* All but last two */
  for (; i + 2 < lenInChars; i++) {
    if (inFirst[i] != _UT('%')) {
      write[0] = inFirst[i];
      write++;
    } else {
      /* 6.2.2.2 Percent-Encoding Normalization: *
       * percent-encoded unreserved characters   */
      const URI_CHAR one = inFirst[i + 1];
      const URI_CHAR two = inFirst[i + 2];
      const unsigned char left = URI_FUNC(HexdigToInt)(one);
      const unsigned char right = URI_FUNC(HexdigToInt)(two);
      const int code = 16 * left + right;
      if (uriIsUnreserved(code)) {
        write[0] = (URI_CHAR)(code);
        write++;
      } else {
        /* 6.2.2.1 Case Normalization: *
         * lowercase percent-encodings */
        write[0] = _UT('%');
        write[1] = URI_FUNC(HexToLetter)(left);
        write[2] = URI_FUNC(HexToLetter)(right);
        write += 3;
      }

      i += 2; /* For the two chars of the percent group we just ate */
    }
  }

  /* Last two */
  for (; i < lenInChars; i++) {
    write[0] = inFirst[i];
    write++;
  }

  *outAfterLast = write;
}



static URI_INLINE void URI_FUNC(FixPercentEncodingInplace)(const URI_CHAR * first,
    const URI_CHAR ** afterLast) {
  /* Death checks */
  if ((first == NULL) || (afterLast == NULL) || (*afterLast == NULL)) {
    return;
  }

  /* Fix inplace */
  URI_FUNC(FixPercentEncodingEngine)(first, *afterLast, first, afterLast);
}



static URI_INLINE UriBool URI_FUNC(FixPercentEncodingMalloc)(const URI_CHAR ** first,
    const URI_CHAR ** afterLast) {
  int lenInChars;
  URI_CHAR * buffer;

  /* Death checks */
  if ((first == NULL) || (afterLast == NULL)
      || (*first == NULL) || (*afterLast == NULL)) {
    return URI_FALSE;
  }

  /* Old text length */
  lenInChars = (int)(*afterLast - *first);
  if (lenInChars == 0) {
    return URI_TRUE;
  } else if (lenInChars < 0) {
    return URI_FALSE;
  }

  /* New buffer */
  buffer = ( URI_CHAR* )malloc(lenInChars * sizeof(URI_CHAR));
  if (buffer == NULL) {
    return URI_FALSE;
  }

  /* Fix on copy */
  URI_FUNC(FixPercentEncodingEngine)(*first, *afterLast, buffer, afterLast);
  *first = buffer;
  return URI_TRUE;
}



static URI_INLINE UriBool URI_FUNC(MakeRangeOwner)(unsigned int * doneMask,
    unsigned int maskTest, URI_TYPE(TextRange) * range) {
  if (((*doneMask & maskTest) == 0)
      && (range->first != NULL)
      && (range->afterLast != NULL)
      && (range->afterLast > range->first)) {
    const int lenInChars = (int)(range->afterLast - range->first);
    const int lenInBytes = lenInChars * sizeof(URI_CHAR);
    URI_CHAR * dup = ( URI_CHAR* )malloc(lenInBytes);
    if (dup == NULL) {
      return URI_FALSE; /* Raises malloc error */
    }
    memcpy(dup, range->first, lenInBytes);
    range->first = dup;
    range->afterLast = dup + lenInChars;
    *doneMask |= maskTest;
  }
  return URI_TRUE;
}



static URI_INLINE UriBool URI_FUNC(MakeOwner)(URI_TYPE(Uri) * uri,
    unsigned int * doneMask) {
  URI_TYPE(PathSegment) * walker = uri->pathHead;
  if (!URI_FUNC(MakeRangeOwner)(doneMask, URI_NORMALIZE_SCHEME,
        &(uri->scheme))
      || !URI_FUNC(MakeRangeOwner)(doneMask, URI_NORMALIZE_USER_INFO,
        &(uri->userInfo))
      || !URI_FUNC(MakeRangeOwner)(doneMask, URI_NORMALIZE_QUERY,
        &(uri->query))
      || !URI_FUNC(MakeRangeOwner)(doneMask, URI_NORMALIZE_FRAGMENT,
        &(uri->fragment))) {
    return URI_FALSE; /* Raises malloc error */
  }

  /* Host */
  if ((*doneMask & URI_NORMALIZE_HOST) == 0) {
    if ((uri->hostData.ip4 == NULL)
        && (uri->hostData.ip6 == NULL)) {
      if (uri->hostData.ipFuture.first != NULL) {
        /* IPvFuture */
        if (!URI_FUNC(MakeRangeOwner)(doneMask, URI_NORMALIZE_HOST,
            &(uri->hostData.ipFuture))) {
          return URI_FALSE; /* Raises malloc error */
        }
        uri->hostText.first = uri->hostData.ipFuture.first;
        uri->hostText.afterLast = uri->hostData.ipFuture.afterLast;
      } else if (uri->hostText.first != NULL) {
        /* Regname */
        if (!URI_FUNC(MakeRangeOwner)(doneMask, URI_NORMALIZE_HOST,
            &(uri->hostText))) {
          return URI_FALSE; /* Raises malloc error */
        }
      }
    }
  }

  /* Path */
  if ((*doneMask & URI_NORMALIZE_PATH) == 0) {
    while (walker != NULL) {
      if (!URI_FUNC(MakeRangeOwner)(doneMask, 0, &(walker->text))) {
        /* Kill path to one before walker */
        URI_TYPE(PathSegment) * ranger = uri->pathHead;
        while (ranger->next != walker) {
          URI_TYPE(PathSegment) * const next = ranger->next;
          if ((ranger->text.first != NULL)
              && (ranger->text.afterLast != NULL)
              && (ranger->text.afterLast > ranger->text.first)) {
            free((URI_CHAR *)ranger->text.first);
            free(ranger);
          }
          ranger = next;
        }

        /* Kill path from walker */
        while (walker != NULL) {
          URI_TYPE(PathSegment) * const next = walker->next;
          free(walker);
          walker = next;
        }

        uri->pathHead = NULL;
        uri->pathTail = NULL;
        return URI_FALSE; /* Raises malloc error */
      }
      walker = walker->next;
    }
    *doneMask |= URI_NORMALIZE_PATH;
  }

  /* Port text, must come last so we don't have to undo that one if it fails. *
   * Otherwise we would need and extra enum flag for it although the port      *
   * cannot go unnormalized...                                                */
  if (!URI_FUNC(MakeRangeOwner)(doneMask, 0, &(uri->portText))) {
    return URI_FALSE; /* Raises malloc error */
  }

  return URI_TRUE;
}



unsigned int URI_FUNC(NormalizeSyntaxMaskRequired)(const URI_TYPE(Uri) * uri) {
  unsigned int res;
#if defined(__GNUC__) && ((__GNUC__ > 4) \
        || ((__GNUC__ == 4) && defined(__GNUC_MINOR__) && (__GNUC_MINOR__ >= 2)))
    /* Slower code that fixes a warning, not sure if this is a smart idea */
  URI_TYPE(Uri) writeableClone;
  memcpy(&writeableClone, uri, 1 * sizeof(URI_TYPE(Uri)));
  URI_FUNC(NormalizeSyntaxEngine)(&writeableClone, 0, &res);
#else
  URI_FUNC(NormalizeSyntaxEngine)((URI_TYPE(Uri) *)uri, 0, &res);
#endif
  return res;
}



int URI_FUNC(NormalizeSyntaxEx)(URI_TYPE(Uri) * uri, unsigned int mask) {
  return URI_FUNC(NormalizeSyntaxEngine)(uri, mask, NULL);
}



int URI_FUNC(NormalizeSyntax)(URI_TYPE(Uri) * uri) {
  return URI_FUNC(NormalizeSyntaxEx)(uri, (unsigned int)-1);
}



static URI_INLINE int URI_FUNC(NormalizeSyntaxEngine)(URI_TYPE(Uri) * uri, unsigned int inMask, unsigned int * outMask) {
  unsigned int doneMask = URI_NORMALIZED;
  if (uri == NULL) {
    if (outMask != NULL) {
      *outMask = URI_NORMALIZED;
      return URI_SUCCESS;
    } else {
      return URI_ERROR_NULL;
    }
  }

  if (outMask != NULL) {
    /* Reset mask */
    *outMask = URI_NORMALIZED;
  } else if (inMask == URI_NORMALIZED) {
    /* Nothing to do */
    return URI_SUCCESS;
  }

  /* Scheme, host */
  if (outMask != NULL) {
    const UriBool normalizeScheme = URI_FUNC(ContainsUppercaseLetters)(
        uri->scheme.first, uri->scheme.afterLast);
    const UriBool normalizeHostCase = URI_FUNC(ContainsUppercaseLetters)(
      uri->hostText.first, uri->hostText.afterLast);
    if (normalizeScheme) {
      *outMask |= URI_NORMALIZE_SCHEME;
    }

    if (normalizeHostCase) {
      *outMask |= URI_NORMALIZE_HOST;
    } else {
      const UriBool normalizeHostPrecent = URI_FUNC(ContainsUglyPercentEncoding)(
          uri->hostText.first, uri->hostText.afterLast);
      if (normalizeHostPrecent) {
        *outMask |= URI_NORMALIZE_HOST;
      }
    }
  } else {
    /* Scheme */
    if ((inMask & URI_NORMALIZE_SCHEME) && (uri->scheme.first != NULL)) {
      if (uri->owner) {
        URI_FUNC(LowercaseInplace)(uri->scheme.first, uri->scheme.afterLast);
      } else {
        if (!URI_FUNC(LowercaseMalloc)(&(uri->scheme.first), &(uri->scheme.afterLast))) {
          URI_FUNC(PreventLeakage)(uri, doneMask);
          return URI_ERROR_MALLOC;
        }
        doneMask |= URI_NORMALIZE_SCHEME;
      }
    }

    /* Host */
    if (inMask & URI_NORMALIZE_HOST) {
      if (uri->hostData.ipFuture.first != NULL) {
        /* IPvFuture */
        if (uri->owner) {
          URI_FUNC(LowercaseInplace)(uri->hostData.ipFuture.first,
              uri->hostData.ipFuture.afterLast);
        } else {
          if (!URI_FUNC(LowercaseMalloc)(&(uri->hostData.ipFuture.first),
              &(uri->hostData.ipFuture.afterLast))) {
            URI_FUNC(PreventLeakage)(uri, doneMask);
            return URI_ERROR_MALLOC;
          }
          doneMask |= URI_NORMALIZE_HOST;
        }
        uri->hostText.first = uri->hostData.ipFuture.first;
        uri->hostText.afterLast = uri->hostData.ipFuture.afterLast;
      } else if ((uri->hostText.first != NULL)
          && (uri->hostData.ip4 == NULL)
          && (uri->hostData.ip6 == NULL)) {
        /* Regname */
        if (uri->owner) {
          URI_FUNC(FixPercentEncodingInplace)(uri->hostText.first,
              &(uri->hostText.afterLast));
        } else {
          if (!URI_FUNC(FixPercentEncodingMalloc)(
              &(uri->hostText.first),
              &(uri->hostText.afterLast))) {
            URI_FUNC(PreventLeakage)(uri, doneMask);
            return URI_ERROR_MALLOC;
          }
          doneMask |= URI_NORMALIZE_HOST;
        }

        URI_FUNC(LowercaseInplace)(uri->hostText.first,
            uri->hostText.afterLast);
      }
    }
  }

  /* User info */
  if (outMask != NULL) {
    const UriBool normalizeUserInfo = URI_FUNC(ContainsUglyPercentEncoding)(
      uri->userInfo.first, uri->userInfo.afterLast);
    if (normalizeUserInfo) {
      *outMask |= URI_NORMALIZE_USER_INFO;
    }
  } else {
    if ((inMask & URI_NORMALIZE_USER_INFO) && (uri->userInfo.first != NULL)) {
      if (uri->owner) {
        URI_FUNC(FixPercentEncodingInplace)(uri->userInfo.first, &(uri->userInfo.afterLast));
      } else {
        if (!URI_FUNC(FixPercentEncodingMalloc)(&(uri->userInfo.first),
            &(uri->userInfo.afterLast))) {
          URI_FUNC(PreventLeakage)(uri, doneMask);
          return URI_ERROR_MALLOC;
        }
        doneMask |= URI_NORMALIZE_USER_INFO;
      }
    }
  }

  /* Path */
  if (outMask != NULL) {
    const URI_TYPE(PathSegment) * walker = uri->pathHead;
    while (walker != NULL) {
      const URI_CHAR * const first = walker->text.first;
      const URI_CHAR * const afterLast = walker->text.afterLast;
      if ((first != NULL)
          && (afterLast != NULL)
          && (afterLast > first)
          && (
            (((afterLast - first) == 1)
              && (first[0] == _UT('.')))
            ||
            (((afterLast - first) == 2)
              && (first[0] == _UT('.'))
              && (first[1] == _UT('.')))
            ||
            URI_FUNC(ContainsUglyPercentEncoding)(first, afterLast)
          )) {
        *outMask |= URI_NORMALIZE_PATH;
        break;
      }
      walker = walker->next;
    }
  } else if (inMask & URI_NORMALIZE_PATH) {
    URI_TYPE(PathSegment) * walker;
    const UriBool relative = ((uri->scheme.first == NULL)
        && !uri->absolutePath) ? URI_TRUE : URI_FALSE;

    /* Fix percent-encoding for each segment */
    walker = uri->pathHead;
    if (uri->owner) {
      while (walker != NULL) {
        URI_FUNC(FixPercentEncodingInplace)(walker->text.first, &(walker->text.afterLast));
        walker = walker->next;
      }
    } else {
      while (walker != NULL) {
        if (!URI_FUNC(FixPercentEncodingMalloc)(&(walker->text.first),
            &(walker->text.afterLast))) {
          URI_FUNC(PreventLeakage)(uri, doneMask);
          return URI_ERROR_MALLOC;
        }
        walker = walker->next;
      }
      doneMask |= URI_NORMALIZE_PATH;
    }

    /* 6.2.2.3 Path Segment Normalization */
    if (!URI_FUNC(RemoveDotSegmentsEx)(uri, relative,
        (uri->owner == URI_TRUE)
        || ((doneMask & URI_NORMALIZE_PATH) != 0)
        )) {
      URI_FUNC(PreventLeakage)(uri, doneMask);
      return URI_ERROR_MALLOC;
    }
    URI_FUNC(FixEmptyTrailSegment)(uri);
  }

  /* Query, fragment */
  if (outMask != NULL) {
    const UriBool normalizeQuery = URI_FUNC(ContainsUglyPercentEncoding)(
        uri->query.first, uri->query.afterLast);
    const UriBool normalizeFragment = URI_FUNC(ContainsUglyPercentEncoding)(
        uri->fragment.first, uri->fragment.afterLast);
    if (normalizeQuery) {
      *outMask |= URI_NORMALIZE_QUERY;
    }

    if (normalizeFragment) {
      *outMask |= URI_NORMALIZE_FRAGMENT;
    }
  } else {
    /* Query */
    if ((inMask & URI_NORMALIZE_QUERY) && (uri->query.first != NULL)) {
      if (uri->owner) {
        URI_FUNC(FixPercentEncodingInplace)(uri->query.first, &(uri->query.afterLast));
      } else {
        if (!URI_FUNC(FixPercentEncodingMalloc)(&(uri->query.first),
            &(uri->query.afterLast))) {
          URI_FUNC(PreventLeakage)(uri, doneMask);
          return URI_ERROR_MALLOC;
        }
        doneMask |= URI_NORMALIZE_QUERY;
      }
    }

    /* Fragment */
    if ((inMask & URI_NORMALIZE_FRAGMENT) && (uri->fragment.first != NULL)) {
      if (uri->owner) {
        URI_FUNC(FixPercentEncodingInplace)(uri->fragment.first, &(uri->fragment.afterLast));
      } else {
        if (!URI_FUNC(FixPercentEncodingMalloc)(&(uri->fragment.first),
            &(uri->fragment.afterLast))) {
          URI_FUNC(PreventLeakage)(uri, doneMask);
          return URI_ERROR_MALLOC;
        }
        doneMask |= URI_NORMALIZE_FRAGMENT;
      }
    }
  }

  /* Dup all not duped yet */
  if ((outMask == NULL) && !uri->owner) {
    if (!URI_FUNC(MakeOwner)(uri, &doneMask)) {
      URI_FUNC(PreventLeakage)(uri, doneMask);
      return URI_ERROR_MALLOC;
    }
    uri->owner = URI_TRUE;
  }

  return URI_SUCCESS;
}


/* Source: UriParseBase.c */



void uriWriteQuadToDoubleByte(const unsigned char * hexDigits, int digitCount, unsigned char * output) {
  switch (digitCount) {
  case 1:
    /* 0x___? -> \x00 \x0? */
    output[0] = 0;
    output[1] = hexDigits[0];
    break;

  case 2:
    /* 0x__?? -> \0xx \x?? */
    output[0] = 0;
    output[1] = 16 * hexDigits[0] + hexDigits[1];
    break;

  case 3:
    /* 0x_??? -> \0x? \x?? */
    output[0] = hexDigits[0];
    output[1] = 16 * hexDigits[1] + hexDigits[2];
    break;

  case 4:
    /* 0x???? -> \0?? \x?? */
    output[0] = 16 * hexDigits[0] + hexDigits[1];
    output[1] = 16 * hexDigits[2] + hexDigits[3];
    break;

  }
}



unsigned char uriGetOctetValue(const unsigned char * digits, int digitCount) {
  switch (digitCount) {
  case 1:
    return digits[0];

  case 2:
    return 10 * digits[0] + digits[1];

  case 3:
  default:
    return 100 * digits[0] + 10 * digits[1] + digits[2];

  }
}


/* Source: UriParse.c */

#define URI_SET_DIGIT \
       _UT('0'): \
  case _UT('1'): \
  case _UT('2'): \
  case _UT('3'): \
  case _UT('4'): \
  case _UT('5'): \
  case _UT('6'): \
  case _UT('7'): \
  case _UT('8'): \
  case _UT('9')

#define URI_SET_HEX_LETTER_UPPER \
       _UT('A'): \
  case _UT('B'): \
  case _UT('C'): \
  case _UT('D'): \
  case _UT('E'): \
  case _UT('F')

#define URI_SET_HEX_LETTER_LOWER \
       _UT('a'): \
  case _UT('b'): \
  case _UT('c'): \
  case _UT('d'): \
  case _UT('e'): \
  case _UT('f')

#define URI_SET_HEXDIG \
  URI_SET_DIGIT: \
  case URI_SET_HEX_LETTER_UPPER: \
  case URI_SET_HEX_LETTER_LOWER

#define URI_SET_ALPHA \
  URI_SET_HEX_LETTER_UPPER: \
  case URI_SET_HEX_LETTER_LOWER: \
  case _UT('g'): \
  case _UT('G'): \
  case _UT('h'): \
  case _UT('H'): \
  case _UT('i'): \
  case _UT('I'): \
  case _UT('j'): \
  case _UT('J'): \
  case _UT('k'): \
  case _UT('K'): \
  case _UT('l'): \
  case _UT('L'): \
  case _UT('m'): \
  case _UT('M'): \
  case _UT('n'): \
  case _UT('N'): \
  case _UT('o'): \
  case _UT('O'): \
  case _UT('p'): \
  case _UT('P'): \
  case _UT('q'): \
  case _UT('Q'): \
  case _UT('r'): \
  case _UT('R'): \
  case _UT('s'): \
  case _UT('S'): \
  case _UT('t'): \
  case _UT('T'): \
  case _UT('u'): \
  case _UT('U'): \
  case _UT('v'): \
  case _UT('V'): \
  case _UT('w'): \
  case _UT('W'): \
  case _UT('x'): \
  case _UT('X'): \
  case _UT('y'): \
  case _UT('Y'): \
  case _UT('z'): \
  case _UT('Z')

static URI_INLINE void URI_FUNC(StopSyntax)(URI_TYPE(ParserState) * state,
    const URI_CHAR * errorPos) {
  URI_FUNC(FreeUriMembers)(state->uri);
  state->errorPos = errorPos;
  state->errorCode = URI_ERROR_SYNTAX;
}



static URI_INLINE void URI_FUNC(StopMalloc)(URI_TYPE(ParserState) * state) {
  URI_FUNC(FreeUriMembers)(state->uri);
  state->errorPos = NULL;
  state->errorCode = URI_ERROR_MALLOC;
}



/*
 * [authority]-><[>[ipLit2][authorityTwo]
 * [authority]->[ownHostUserInfoNz]
 * [authority]-><NULL>
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseAuthority)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    /* "" regname host */
    state->uri->hostText.first = URI_FUNC(SafeToPointTo);
    state->uri->hostText.afterLast = URI_FUNC(SafeToPointTo);
    return afterLast;
  }

  switch (*first) {
  case _UT('['):
    {
      const URI_CHAR * const afterIpLit2
          = URI_FUNC(ParseIpLit2)(state, first + 1, afterLast);
      if (afterIpLit2 == NULL) {
        return NULL;
      }
      state->uri->hostText.first = first + 1; /* HOST BEGIN */
      return URI_FUNC(ParseAuthorityTwo)(state, afterIpLit2, afterLast);
    }

  case _UT('!'):
  case _UT('$'):
  case _UT('%'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(':'):
  case _UT(';'):
  case _UT('@'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    state->uri->userInfo.first = first; /* USERINFO BEGIN */
    return URI_FUNC(ParseOwnHostUserInfoNz)(state, first, afterLast);

  default:
    /* "" regname host */
    state->uri->hostText.first = URI_FUNC(SafeToPointTo);
    state->uri->hostText.afterLast = URI_FUNC(SafeToPointTo);
    return first;
  }
}



/*
 * [authorityTwo]-><:>[port]
 * [authorityTwo]-><NULL>
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseAuthorityTwo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT(':'):
    {
      const URI_CHAR * const afterPort = URI_FUNC(ParsePort)(state, first + 1, afterLast);
      if (afterPort == NULL) {
        return NULL;
      }
      state->uri->portText.first = first + 1; /* PORT BEGIN */
      state->uri->portText.afterLast = afterPort; /* PORT END */
      return afterPort;
    }

  default:
    return first;
  }
}



/*
 * [hexZero]->[HEXDIG][hexZero]
 * [hexZero]-><NULL>
 */
static const URI_CHAR * URI_FUNC(ParseHexZero)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case URI_SET_HEXDIG:
    return URI_FUNC(ParseHexZero)(state, first + 1, afterLast);

  default:
    return first;
  }
}



/*
 * [hierPart]->[pathRootless]
 * [hierPart]-></>[partHelperTwo]
 * [hierPart]-><NULL>
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseHierPart)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('%'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(':'):
  case _UT(';'):
  case _UT('@'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    return URI_FUNC(ParsePathRootless)(state, first, afterLast);

  case _UT('/'):
    return URI_FUNC(ParsePartHelperTwo)(state, first + 1, afterLast);

  default:
    return first;
  }
}



/*
 * [ipFutLoop]->[subDelims][ipFutStopGo]
 * [ipFutLoop]->[unreserved][ipFutStopGo]
 * [ipFutLoop]-><:>[ipFutStopGo]
 */
static const URI_CHAR * URI_FUNC(ParseIpFutLoop)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(':'):
  case _UT(';'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    return URI_FUNC(ParseIpFutStopGo)(state, first + 1, afterLast);

  default:
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }
}



/*
 * [ipFutStopGo]->[ipFutLoop]
 * [ipFutStopGo]-><NULL>
 */
static const URI_CHAR * URI_FUNC(ParseIpFutStopGo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(':'):
  case _UT(';'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    return URI_FUNC(ParseIpFutLoop)(state, first, afterLast);

  default:
    return first;
  }
}



/*
 * [ipFuture]-><v>[HEXDIG][hexZero]<.>[ipFutLoop]
 */
static const URI_CHAR * URI_FUNC(ParseIpFuture)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }

  /*
  First character has already been
  checked before entering this rule.

  switch (*first) {
  case _UT('v'):
  */
    if (first + 1 >= afterLast) {
      URI_FUNC(StopSyntax)(state, first + 1);
      return NULL;
    }

    switch (first[1]) {
    case URI_SET_HEXDIG:
      {
        const URI_CHAR * afterIpFutLoop;
        const URI_CHAR * const afterHexZero
            = URI_FUNC(ParseHexZero)(state, first + 2, afterLast);
        if (afterHexZero == NULL) {
          return NULL;
        }
        if ((afterHexZero >= afterLast)
            || (*afterHexZero != _UT('.'))) {
          URI_FUNC(StopSyntax)(state, afterHexZero);
          return NULL;
        }
        state->uri->hostText.first = first; /* HOST BEGIN */
        state->uri->hostData.ipFuture.first = first; /* IPFUTURE BEGIN */
        afterIpFutLoop = URI_FUNC(ParseIpFutLoop)(state, afterHexZero + 1, afterLast);
        if (afterIpFutLoop == NULL) {
          return NULL;
        }
        state->uri->hostText.afterLast = afterIpFutLoop; /* HOST END */
        state->uri->hostData.ipFuture.afterLast = afterIpFutLoop; /* IPFUTURE END */
        return afterIpFutLoop;
      }

    default:
      URI_FUNC(StopSyntax)(state, first + 1);
      return NULL;
    }

  /*
  default:
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }
  */
}



/*
 * [ipLit2]->[ipFuture]<]>
 * [ipLit2]->[IPv6address2]
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseIpLit2)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }

  switch (*first) {
  case _UT('v'):
    {
      const URI_CHAR * const afterIpFuture
          = URI_FUNC(ParseIpFuture)(state, first, afterLast);
      if (afterIpFuture == NULL) {
        return NULL;
      }
      if ((afterIpFuture >= afterLast)
          || (*afterIpFuture != _UT(']'))) {
        URI_FUNC(StopSyntax)(state, first);
        return NULL;
      }
      return afterIpFuture + 1;
    }

  case _UT(':'):
  case _UT(']'):
  case URI_SET_HEXDIG:
    state->uri->hostData.ip6 = ( UriIp6* )malloc(1 * sizeof(UriIp6)); /* Freed when stopping on parse error */
    if (state->uri->hostData.ip6 == NULL) {
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    return URI_FUNC(ParseIPv6address2)(state, first, afterLast);

  default:
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }
}



/*
 * [IPv6address2]->..<]>
 */
static const URI_CHAR * URI_FUNC(ParseIPv6address2)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  int zipperEver = 0;
  int quadsDone = 0;
  int digitCount = 0;
  unsigned char digitHistory[4];
  int ip4OctetsDone = 0;

  unsigned char quadsAfterZipper[14];
  int quadsAfterZipperCount = 0;


  for (;;) {
    if (first >= afterLast) {
      URI_FUNC(StopSyntax)(state, first);
      return NULL;
    }

    /* Inside IPv4 part? */
    if (ip4OctetsDone > 0) {
      /* Eat rest of IPv4 address */
      for (;;) {
        switch (*first) {
        case URI_SET_DIGIT:
          if (digitCount == 4) {
            URI_FUNC(StopSyntax)(state, first);
            return NULL;
          }
          digitHistory[digitCount++] = (unsigned char)(9 + *first - _UT('9'));
          break;

        case _UT('.'):
          if ((ip4OctetsDone == 4) /* NOTE! */
              || (digitCount == 0)
              || (digitCount == 4)) {
            /* Invalid digit or octet count */
            URI_FUNC(StopSyntax)(state, first);
            return NULL;
          } else if ((digitCount > 1)
              && (digitHistory[0] == 0)) {
            /* Leading zero */
            URI_FUNC(StopSyntax)(state, first - digitCount);
            return NULL;
          } else if ((digitCount > 2)
              && (digitHistory[1] == 0)) {
            /* Leading zero */
            URI_FUNC(StopSyntax)(state, first - digitCount + 1);
            return NULL;
          } else if ((digitCount == 3)
              && (100 * digitHistory[0]
                + 10 * digitHistory[1]
                + digitHistory[2] > 255)) {
            /* Octet value too large */
            if (digitHistory[0] > 2) {
              URI_FUNC(StopSyntax)(state, first - 3);
            } else if (digitHistory[1] > 5) {
              URI_FUNC(StopSyntax)(state, first - 2);
            } else {
              URI_FUNC(StopSyntax)(state, first - 1);
            }
            return NULL;
          }

          /* Copy IPv4 octet */
          state->uri->hostData.ip6->data[16 - 4 + ip4OctetsDone] = uriGetOctetValue(digitHistory, digitCount);
          digitCount = 0;
          ip4OctetsDone++;
          break;

        case _UT(']'):
          if ((ip4OctetsDone != 3) /* NOTE! */
              || (digitCount == 0)
              || (digitCount == 4)) {
            /* Invalid digit or octet count */
            URI_FUNC(StopSyntax)(state, first);
            return NULL;
          } else if ((digitCount > 1)
              && (digitHistory[0] == 0)) {
            /* Leading zero */
            URI_FUNC(StopSyntax)(state, first - digitCount);
            return NULL;
          } else if ((digitCount > 2)
              && (digitHistory[1] == 0)) {
            /* Leading zero */
            URI_FUNC(StopSyntax)(state, first - digitCount + 1);
            return NULL;
          } else if ((digitCount == 3)
              && (100 * digitHistory[0]
                + 10 * digitHistory[1]
                + digitHistory[2] > 255)) {
            /* Octet value too large */
            if (digitHistory[0] > 2) {
              URI_FUNC(StopSyntax)(state, first - 3);
            } else if (digitHistory[1] > 5) {
              URI_FUNC(StopSyntax)(state, first - 2);
            } else {
              URI_FUNC(StopSyntax)(state, first - 1);
            }
            return NULL;
          }

          state->uri->hostText.afterLast = first; /* HOST END */

          /* Copy missing quads right before IPv4 */
          memcpy(state->uri->hostData.ip6->data + 16 - 4 - 2 * quadsAfterZipperCount,
                quadsAfterZipper, 2 * quadsAfterZipperCount);

          /* Copy last IPv4 octet */
          state->uri->hostData.ip6->data[16 - 4 + 3] = uriGetOctetValue(digitHistory, digitCount);

          return first + 1;

        default:
          URI_FUNC(StopSyntax)(state, first);
          return NULL;
        }
        first++;
      }
    } else {
      /* Eat while no dot in sight */
      int letterAmong = 0;
      int walking = 1;
      do {
        switch (*first) {
        case URI_SET_HEX_LETTER_LOWER:
          letterAmong = 1;
          if (digitCount == 4) {
            URI_FUNC(StopSyntax)(state, first);
            return NULL;
          }
          digitHistory[digitCount] = (unsigned char)(15 + *first - _UT('f'));
          digitCount++;
          break;

        case URI_SET_HEX_LETTER_UPPER:
          letterAmong = 1;
          if (digitCount == 4) {
            URI_FUNC(StopSyntax)(state, first);
            return NULL;
          }
          digitHistory[digitCount] = (unsigned char)(15 + *first - _UT('F'));
          digitCount++;
          break;

        case URI_SET_DIGIT:
          if (digitCount == 4) {
            URI_FUNC(StopSyntax)(state, first);
            return NULL;
          }
          digitHistory[digitCount] = (unsigned char)(9 + *first - _UT('9'));
          digitCount++;
          break;

        case _UT(':'):
          {
            int setZipper = 0;

            /* Too many quads? */
            if (quadsDone > 8 - zipperEver) {
              URI_FUNC(StopSyntax)(state, first);
              return NULL;
            }

            /* "::"? */
            if (first + 1 >= afterLast) {
              URI_FUNC(StopSyntax)(state, first + 1);
              return NULL;
            }
            if (first[1] == _UT(':')) {
              const int resetOffset = 2 * (quadsDone + (digitCount > 0));

              first++;
              if (zipperEver) {
                URI_FUNC(StopSyntax)(state, first);
                return NULL; /* "::.+::" */
              }

              /* Zero everything after zipper */
              memset(state->uri->hostData.ip6->data + resetOffset, 0, 16 - resetOffset);
              setZipper = 1;

              /* ":::+"? */
              if (first + 1 >= afterLast) {
                URI_FUNC(StopSyntax)(state, first + 1);
                return NULL; /* No ']' yet */
              }
              if (first[1] == _UT(':')) {
                URI_FUNC(StopSyntax)(state, first + 1);
                return NULL; /* ":::+ "*/
              }
            }
            if (digitCount > 0) {
              if (zipperEver) {
                uriWriteQuadToDoubleByte(digitHistory, digitCount, quadsAfterZipper + 2 * quadsAfterZipperCount);
                quadsAfterZipperCount++;
              } else {
                uriWriteQuadToDoubleByte(digitHistory, digitCount, state->uri->hostData.ip6->data + 2 * quadsDone);
              }
              quadsDone++;
              digitCount = 0;
            }
            letterAmong = 0;

            if (setZipper) {
              zipperEver = 1;
            }
          }
          break;

        case _UT('.'):
          if ((quadsDone > 6) /* NOTE */
              || (!zipperEver && (quadsDone < 6))
              || letterAmong
              || (digitCount == 0)
              || (digitCount == 4)) {
            /* Invalid octet before */
            URI_FUNC(StopSyntax)(state, first);
            return NULL;
          } else if ((digitCount > 1)
              && (digitHistory[0] == 0)) {
            /* Leading zero */
            URI_FUNC(StopSyntax)(state, first - digitCount);
            return NULL;
          } else if ((digitCount > 2)
              && (digitHistory[1] == 0)) {
            /* Leading zero */
            URI_FUNC(StopSyntax)(state, first - digitCount + 1);
            return NULL;
          } else if ((digitCount == 3)
              && (100 * digitHistory[0]
                + 10 * digitHistory[1]
                + digitHistory[2] > 255)) {
            /* Octet value too large */
            if (digitHistory[0] > 2) {
              URI_FUNC(StopSyntax)(state, first - 3);
            } else if (digitHistory[1] > 5) {
              URI_FUNC(StopSyntax)(state, first - 2);
            } else {
              URI_FUNC(StopSyntax)(state, first - 1);
            }
            return NULL;
          }

          /* Copy first IPv4 octet */
          state->uri->hostData.ip6->data[16 - 4] = uriGetOctetValue(digitHistory, digitCount);
          digitCount = 0;

          /* Switch over to IPv4 loop */
          ip4OctetsDone = 1;
          walking = 0;
          break;

        case _UT(']'):
          /* Too little quads? */
          if (!zipperEver && !((quadsDone == 7) && (digitCount > 0))) {
            URI_FUNC(StopSyntax)(state, first);
            return NULL;
          }

          if (digitCount > 0) {
            if (zipperEver) {
              uriWriteQuadToDoubleByte(digitHistory, digitCount, quadsAfterZipper + 2 * quadsAfterZipperCount);
              quadsAfterZipperCount++;
            } else {
              uriWriteQuadToDoubleByte(digitHistory, digitCount, state->uri->hostData.ip6->data + 2 * quadsDone);
            }
            /*
            quadsDone++;
            digitCount = 0;
            */
          }

          /* Copy missing quads to the end */
          memcpy(state->uri->hostData.ip6->data + 16 - 2 * quadsAfterZipperCount,
                quadsAfterZipper, 2 * quadsAfterZipperCount);

          state->uri->hostText.afterLast = first; /* HOST END */
          return first + 1; /* Fine */

        default:
          URI_FUNC(StopSyntax)(state, first);
          return NULL;
        }
        first++;

        if (first >= afterLast) {
          URI_FUNC(StopSyntax)(state, first);
          return NULL; /* No ']' yet */
        }
      } while (walking);
    }
  }
}



/*
 * [mustBeSegmentNzNc]->[pctEncoded][mustBeSegmentNzNc]
 * [mustBeSegmentNzNc]->[subDelims][mustBeSegmentNzNc]
 * [mustBeSegmentNzNc]->[unreserved][mustBeSegmentNzNc]
 * [mustBeSegmentNzNc]->[uriTail] // can take <NULL>
 * [mustBeSegmentNzNc]-></>[segment][zeroMoreSlashSegs][uriTail]
 * [mustBeSegmentNzNc]-><@>[mustBeSegmentNzNc]
 */
static const URI_CHAR * URI_FUNC(ParseMustBeSegmentNzNc)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    if (!URI_FUNC(PushPathSegment)(state, state->uri->scheme.first, first)) { /* SEGMENT BOTH */
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    state->uri->scheme.first = NULL; /* Not a scheme, reset */
    return afterLast;
  }

  switch (*first) {
  case _UT('%'):
    {
      const URI_CHAR * const afterPctEncoded
          = URI_FUNC(ParsePctEncoded)(state, first, afterLast);
      if (afterPctEncoded == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseMustBeSegmentNzNc)(state, afterPctEncoded, afterLast);
    }

  case _UT('@'):
  case _UT('!'):
  case _UT('$'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('*'):
  case _UT(','):
  case _UT(';'):
  case _UT('\''):
  case _UT('+'):
  case _UT('='):
  case _UT('-'):
  case _UT('.'):
  case _UT('_'):
  case _UT('~'):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    return URI_FUNC(ParseMustBeSegmentNzNc)(state, first + 1, afterLast);

  case _UT('/'):
    {
      const URI_CHAR * afterZeroMoreSlashSegs;
      const URI_CHAR * afterSegment;
      if (!URI_FUNC(PushPathSegment)(state, state->uri->scheme.first, first)) { /* SEGMENT BOTH */
        URI_FUNC(StopMalloc)(state);
        return NULL;
      }
      state->uri->scheme.first = NULL; /* Not a scheme, reset */
      afterSegment = URI_FUNC(ParseSegment)(state, first + 1, afterLast);
      if (afterSegment == NULL) {
        return NULL;
      }
      if (!URI_FUNC(PushPathSegment)(state, first + 1, afterSegment)) { /* SEGMENT BOTH */
        URI_FUNC(StopMalloc)(state);
        return NULL;
      }
      afterZeroMoreSlashSegs
          = URI_FUNC(ParseZeroMoreSlashSegs)(state, afterSegment, afterLast);
      if (afterZeroMoreSlashSegs == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseUriTail)(state, afterZeroMoreSlashSegs, afterLast);
    }

  default:
    if (!URI_FUNC(PushPathSegment)(state, state->uri->scheme.first, first)) { /* SEGMENT BOTH */
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    state->uri->scheme.first = NULL; /* Not a scheme, reset */
    return URI_FUNC(ParseUriTail)(state, first, afterLast);
  }
}



/*
 * [ownHost]-><[>[ipLit2][authorityTwo]
 * [ownHost]->[ownHost2] // can take <NULL>
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseOwnHost)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('['):
    {
      const URI_CHAR * const afterIpLit2
          = URI_FUNC(ParseIpLit2)(state, first + 1, afterLast);
      if (afterIpLit2 == NULL) {
        return NULL;
      }
      state->uri->hostText.first = first + 1; /* HOST BEGIN */
      return URI_FUNC(ParseAuthorityTwo)(state, afterIpLit2, afterLast);
    }

  default:
    return URI_FUNC(ParseOwnHost2)(state, first, afterLast);
  }
}



static URI_INLINE UriBool URI_FUNC(OnExitOwnHost2)(URI_TYPE(ParserState) * state, const URI_CHAR * first) {
  state->uri->hostText.afterLast = first; /* HOST END */

  /* Valid IPv4 or just a regname? */
  state->uri->hostData.ip4 = ( UriIp4* )malloc(1 * sizeof(UriIp4)); /* Freed when stopping on parse error */
  if (state->uri->hostData.ip4 == NULL) {
    return URI_FALSE; /* Raises malloc error */
  }
  if (URI_FUNC(ParseIpFourAddress)(state->uri->hostData.ip4->data,
      state->uri->hostText.first, state->uri->hostText.afterLast)) {
    /* Not IPv4 */
    free(state->uri->hostData.ip4);
    state->uri->hostData.ip4 = NULL;
  }
  return URI_TRUE; /* Success */
}



/*
 * [ownHost2]->[authorityTwo] // can take <NULL>
 * [ownHost2]->[pctSubUnres][ownHost2]
 */
static const URI_CHAR * URI_FUNC(ParseOwnHost2)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    if (!URI_FUNC(OnExitOwnHost2)(state, first)) {
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    return afterLast;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('%'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(';'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    {
      const URI_CHAR * const afterPctSubUnres
          = URI_FUNC(ParsePctSubUnres)(state, first, afterLast);
      if (afterPctSubUnres == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseOwnHost2)(state, afterPctSubUnres, afterLast);
    }

  default:
    if (!URI_FUNC(OnExitOwnHost2)(state, first)) {
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    return URI_FUNC(ParseAuthorityTwo)(state, first, afterLast);
  }
}



static URI_INLINE UriBool URI_FUNC(OnExitOwnHostUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first) {
  state->uri->hostText.first = state->uri->userInfo.first; /* Host instead of userInfo, update */
  state->uri->userInfo.first = NULL; /* Not a userInfo, reset */
  state->uri->hostText.afterLast = first; /* HOST END */

  /* Valid IPv4 or just a regname? */
  state->uri->hostData.ip4 = ( UriIp4* )malloc(1 * sizeof(UriIp4)); /* Freed when stopping on parse error */
  if (state->uri->hostData.ip4 == NULL) {
    return URI_FALSE; /* Raises malloc error */
  }
  if (URI_FUNC(ParseIpFourAddress)(state->uri->hostData.ip4->data,
      state->uri->hostText.first, state->uri->hostText.afterLast)) {
    /* Not IPv4 */
    free(state->uri->hostData.ip4);
    state->uri->hostData.ip4 = NULL;
  }
  return URI_TRUE; /* Success */
}



/*
 * [ownHostUserInfo]->[ownHostUserInfoNz]
 * [ownHostUserInfo]-><NULL>
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseOwnHostUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    if (!URI_FUNC(OnExitOwnHostUserInfo)(state, first)) {
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    return afterLast;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('%'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(':'):
  case _UT(';'):
  case _UT('@'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    return URI_FUNC(ParseOwnHostUserInfoNz)(state, first, afterLast);

  default:
    if (!URI_FUNC(OnExitOwnHostUserInfo)(state, first)) {
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    return first;
  }
}



/*
 * [ownHostUserInfoNz]->[pctSubUnres][ownHostUserInfo]
 * [ownHostUserInfoNz]-><:>[ownPortUserInfo]
 * [ownHostUserInfoNz]-><@>[ownHost]
 */
static const URI_CHAR * URI_FUNC(ParseOwnHostUserInfoNz)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('%'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(';'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    {
      const URI_CHAR * const afterPctSubUnres
          = URI_FUNC(ParsePctSubUnres)(state, first, afterLast);
      if (afterPctSubUnres == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseOwnHostUserInfo)(state, afterPctSubUnres, afterLast);
    }

  case _UT(':'):
    state->uri->hostText.afterLast = first; /* HOST END */
    state->uri->portText.first = first + 1; /* PORT BEGIN */
    return URI_FUNC(ParseOwnPortUserInfo)(state, first + 1, afterLast);

  case _UT('@'):
    state->uri->userInfo.afterLast = first; /* USERINFO END */
    state->uri->hostText.first = first + 1; /* HOST BEGIN */
    return URI_FUNC(ParseOwnHost)(state, first + 1, afterLast);

  default:
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }
}



static URI_INLINE UriBool URI_FUNC(OnExitOwnPortUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first) {
  state->uri->hostText.first = state->uri->userInfo.first; /* Host instead of userInfo, update */
  state->uri->userInfo.first = NULL; /* Not a userInfo, reset */
  state->uri->portText.afterLast = first; /* PORT END */

  /* Valid IPv4 or just a regname? */
  state->uri->hostData.ip4 = ( UriIp4* )malloc(1 * sizeof(UriIp4)); /* Freed when stopping on parse error */
  if (state->uri->hostData.ip4 == NULL) {
    return URI_FALSE; /* Raises malloc error */
  }
  if (URI_FUNC(ParseIpFourAddress)(state->uri->hostData.ip4->data,
      state->uri->hostText.first, state->uri->hostText.afterLast)) {
    /* Not IPv4 */
    free(state->uri->hostData.ip4);
    state->uri->hostData.ip4 = NULL;
  }
  return URI_TRUE; /* Success */
}



/*
 * [ownPortUserInfo]->[ALPHA][ownUserInfo]
 * [ownPortUserInfo]->[DIGIT][ownPortUserInfo]
 * [ownPortUserInfo]-><.>[ownUserInfo]
 * [ownPortUserInfo]-><_>[ownUserInfo]
 * [ownPortUserInfo]-><~>[ownUserInfo]
 * [ownPortUserInfo]-><->[ownUserInfo]
 * [ownPortUserInfo]-><@>[ownHost]
 * [ownPortUserInfo]-><NULL>
 */
static const URI_CHAR * URI_FUNC(ParseOwnPortUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    if (!URI_FUNC(OnExitOwnPortUserInfo)(state, first)) {
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    return afterLast;
  }

  switch (*first) {
  case _UT('.'):
  case _UT('_'):
  case _UT('~'):
  case _UT('-'):
  case URI_SET_ALPHA:
    state->uri->hostText.afterLast = NULL; /* Not a host, reset */
    state->uri->portText.first = NULL; /* Not a port, reset */
    return URI_FUNC(ParseOwnUserInfo)(state, first + 1, afterLast);

  case URI_SET_DIGIT:
    return URI_FUNC(ParseOwnPortUserInfo)(state, first + 1, afterLast);

  case _UT('@'):
    state->uri->hostText.afterLast = NULL; /* Not a host, reset */
    state->uri->portText.first = NULL; /* Not a port, reset */
    state->uri->userInfo.afterLast = first; /* USERINFO END */
    state->uri->hostText.first = first + 1; /* HOST BEGIN */
    return URI_FUNC(ParseOwnHost)(state, first + 1, afterLast);

  default:
    if (!URI_FUNC(OnExitOwnPortUserInfo)(state, first)) {
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    return first;
  }
}



/*
 * [ownUserInfo]->[pctSubUnres][ownUserInfo]
 * [ownUserInfo]-><:>[ownUserInfo]
 * [ownUserInfo]-><@>[ownHost]
 */
static const URI_CHAR * URI_FUNC(ParseOwnUserInfo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('%'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(';'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    {
      const URI_CHAR * const afterPctSubUnres
          = URI_FUNC(ParsePctSubUnres)(state, first, afterLast);
      if (afterPctSubUnres == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseOwnUserInfo)(state, afterPctSubUnres, afterLast);
    }

  case _UT(':'):
    return URI_FUNC(ParseOwnUserInfo)(state, first + 1, afterLast);

  case _UT('@'):
    /* SURE */
    state->uri->userInfo.afterLast = first; /* USERINFO END */
    state->uri->hostText.first = first + 1; /* HOST BEGIN */
    return URI_FUNC(ParseOwnHost)(state, first + 1, afterLast);

  default:
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }
}



static URI_INLINE void URI_FUNC(OnExitPartHelperTwo)(URI_TYPE(ParserState) * state) {
  state->uri->absolutePath = URI_TRUE;
}



/*
 * [partHelperTwo]->[pathAbsNoLeadSlash] // can take <NULL>
 * [partHelperTwo]-></>[authority][pathAbsEmpty]
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParsePartHelperTwo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    URI_FUNC(OnExitPartHelperTwo)(state);
    return afterLast;
  }

  switch (*first) {
  case _UT('/'):
    {
      const URI_CHAR * const afterAuthority
          = URI_FUNC(ParseAuthority)(state, first + 1, afterLast);
      const URI_CHAR * afterPathAbsEmpty;
      if (afterAuthority == NULL) {
        return NULL;
      }
      afterPathAbsEmpty = URI_FUNC(ParsePathAbsEmpty)(state, afterAuthority, afterLast);

      URI_FUNC(FixEmptyTrailSegment)(state->uri);

      return afterPathAbsEmpty;
    }

  default:
    URI_FUNC(OnExitPartHelperTwo)(state);
    return URI_FUNC(ParsePathAbsNoLeadSlash)(state, first, afterLast);
  }
}



/*
 * [pathAbsEmpty]-></>[segment][pathAbsEmpty]
 * [pathAbsEmpty]-><NULL>
 */
static const URI_CHAR * URI_FUNC(ParsePathAbsEmpty)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('/'):
    {
      const URI_CHAR * const afterSegment
          = URI_FUNC(ParseSegment)(state, first + 1, afterLast);
      if (afterSegment == NULL) {
        return NULL;
      }
      if (!URI_FUNC(PushPathSegment)(state, first + 1, afterSegment)) { /* SEGMENT BOTH */
        URI_FUNC(StopMalloc)(state);
        return NULL;
      }
      return URI_FUNC(ParsePathAbsEmpty)(state, afterSegment, afterLast);
    }

  default:
    return first;
  }
}



/*
 * [pathAbsNoLeadSlash]->[segmentNz][zeroMoreSlashSegs]
 * [pathAbsNoLeadSlash]-><NULL>
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParsePathAbsNoLeadSlash)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('%'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(':'):
  case _UT(';'):
  case _UT('@'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    {
      const URI_CHAR * const afterSegmentNz
          = URI_FUNC(ParseSegmentNz)(state, first, afterLast);
      if (afterSegmentNz == NULL) {
        return NULL;
      }
      if (!URI_FUNC(PushPathSegment)(state, first, afterSegmentNz)) { /* SEGMENT BOTH */
        URI_FUNC(StopMalloc)(state);
        return NULL;
      }
      return URI_FUNC(ParseZeroMoreSlashSegs)(state, afterSegmentNz, afterLast);
    }

  default:
    return first;
  }
}



/*
 * [pathRootless]->[segmentNz][zeroMoreSlashSegs]
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParsePathRootless)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  const URI_CHAR * const afterSegmentNz
      = URI_FUNC(ParseSegmentNz)(state, first, afterLast);
  if (afterSegmentNz == NULL) {
    return NULL;
  } else {
    if (!URI_FUNC(PushPathSegment)(state, first, afterSegmentNz)) { /* SEGMENT BOTH */
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
  }
  return URI_FUNC(ParseZeroMoreSlashSegs)(state, afterSegmentNz, afterLast);
}



/*
 * [pchar]->[pctEncoded]
 * [pchar]->[subDelims]
 * [pchar]->[unreserved]
 * [pchar]-><:>
 * [pchar]-><@>
 */
static const URI_CHAR * URI_FUNC(ParsePchar)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }

  switch (*first) {
  case _UT('%'):
    return URI_FUNC(ParsePctEncoded)(state, first, afterLast);

  case _UT(':'):
  case _UT('@'):
  case _UT('!'):
  case _UT('$'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('*'):
  case _UT(','):
  case _UT(';'):
  case _UT('\''):
  case _UT('+'):
  case _UT('='):
  case _UT('-'):
  case _UT('.'):
  case _UT('_'):
  case _UT('~'):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    return first + 1;

  default:
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }
}



/*
 * [pctEncoded]-><%>[HEXDIG][HEXDIG]
 */
static const URI_CHAR * URI_FUNC(ParsePctEncoded)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }

  /*
  First character has already been
  checked before entering this rule.

  switch (*first) {
  case _UT('%'):
  */
    if (first + 1 >= afterLast) {
      URI_FUNC(StopSyntax)(state, first + 1);
      return NULL;
    }

    switch (first[1]) {
    case URI_SET_HEXDIG:
      if (first + 2 >= afterLast) {
        URI_FUNC(StopSyntax)(state, first + 2);
        return NULL;
      }

      switch (first[2]) {
      case URI_SET_HEXDIG:
        return first + 3;

      default:
        URI_FUNC(StopSyntax)(state, first + 2);
        return NULL;
      }

    default:
      URI_FUNC(StopSyntax)(state, first + 1);
      return NULL;
    }

  /*
  default:
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }
  */
}



/*
 * [pctSubUnres]->[pctEncoded]
 * [pctSubUnres]->[subDelims]
 * [pctSubUnres]->[unreserved]
 */
static const URI_CHAR * URI_FUNC(ParsePctSubUnres)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }

  switch (*first) {
  case _UT('%'):
    return URI_FUNC(ParsePctEncoded)(state, first, afterLast);

  case _UT('!'):
  case _UT('$'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('*'):
  case _UT(','):
  case _UT(';'):
  case _UT('\''):
  case _UT('+'):
  case _UT('='):
  case _UT('-'):
  case _UT('.'):
  case _UT('_'):
  case _UT('~'):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    return first + 1;

  default:
    URI_FUNC(StopSyntax)(state, first);
    return NULL;
  }
}



/*
 * [port]->[DIGIT][port]
 * [port]-><NULL>
 */
static const URI_CHAR * URI_FUNC(ParsePort)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case URI_SET_DIGIT:
    return URI_FUNC(ParsePort)(state, first + 1, afterLast);

  default:
    return first;
  }
}



/*
 * [queryFrag]->[pchar][queryFrag]
 * [queryFrag]-></>[queryFrag]
 * [queryFrag]-><?>[queryFrag]
 * [queryFrag]-><NULL>
 */
static const URI_CHAR * URI_FUNC(ParseQueryFrag)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('%'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(':'):
  case _UT(';'):
  case _UT('@'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    {
      const URI_CHAR * const afterPchar
          = URI_FUNC(ParsePchar)(state, first, afterLast);
      if (afterPchar == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseQueryFrag)(state, afterPchar, afterLast);
    }

  case _UT('/'):
  case _UT('?'):
    return URI_FUNC(ParseQueryFrag)(state, first + 1, afterLast);

  default:
    return first;
  }
}



/*
 * [segment]->[pchar][segment]
 * [segment]-><NULL>
 */
static const URI_CHAR * URI_FUNC(ParseSegment)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('!'):
  case _UT('$'):
  case _UT('%'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('-'):
  case _UT('*'):
  case _UT(','):
  case _UT('.'):
  case _UT(':'):
  case _UT(';'):
  case _UT('@'):
  case _UT('\''):
  case _UT('_'):
  case _UT('~'):
  case _UT('+'):
  case _UT('='):
  case URI_SET_DIGIT:
  case URI_SET_ALPHA:
    {
      const URI_CHAR * const afterPchar
          = URI_FUNC(ParsePchar)(state, first, afterLast);
      if (afterPchar == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseSegment)(state, afterPchar, afterLast);
    }

  default:
    return first;
  }
}



/*
 * [segmentNz]->[pchar][segment]
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseSegmentNz)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  const URI_CHAR * const afterPchar
      = URI_FUNC(ParsePchar)(state, first, afterLast);
  if (afterPchar == NULL) {
    return NULL;
  }
  return URI_FUNC(ParseSegment)(state, afterPchar, afterLast);
}



static URI_INLINE UriBool URI_FUNC(OnExitSegmentNzNcOrScheme2)(URI_TYPE(ParserState) * state, const URI_CHAR * first) {
  if (!URI_FUNC(PushPathSegment)(state, state->uri->scheme.first, first)) { /* SEGMENT BOTH */
    return URI_FALSE; /* Raises malloc error*/
  }
  state->uri->scheme.first = NULL; /* Not a scheme, reset */
  return URI_TRUE; /* Success */
}



/*
 * [segmentNzNcOrScheme2]->[ALPHA][segmentNzNcOrScheme2]
 * [segmentNzNcOrScheme2]->[DIGIT][segmentNzNcOrScheme2]
 * [segmentNzNcOrScheme2]->[pctEncoded][mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]->[uriTail] // can take <NULL>
 * [segmentNzNcOrScheme2]-><!>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><$>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><&>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><(>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><)>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><*>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><,>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><.>[segmentNzNcOrScheme2]
 * [segmentNzNcOrScheme2]-></>[segment][zeroMoreSlashSegs][uriTail]
 * [segmentNzNcOrScheme2]-><:>[hierPart][uriTail]
 * [segmentNzNcOrScheme2]-><;>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><@>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><_>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><~>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><+>[segmentNzNcOrScheme2]
 * [segmentNzNcOrScheme2]-><=>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><'>[mustBeSegmentNzNc]
 * [segmentNzNcOrScheme2]-><->[segmentNzNcOrScheme2]
 */
static const URI_CHAR * URI_FUNC(ParseSegmentNzNcOrScheme2)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    if (!URI_FUNC(OnExitSegmentNzNcOrScheme2)(state, first)) {
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    return afterLast;
  }

  switch (*first) {
  case _UT('.'):
  case _UT('+'):
  case _UT('-'):
  case URI_SET_ALPHA:
  case URI_SET_DIGIT:
    return URI_FUNC(ParseSegmentNzNcOrScheme2)(state, first + 1, afterLast);

  case _UT('%'):
    {
      const URI_CHAR * const afterPctEncoded
          = URI_FUNC(ParsePctEncoded)(state, first, afterLast);
      if (afterPctEncoded == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseMustBeSegmentNzNc)(state, afterPctEncoded, afterLast);
    }

  case _UT('!'):
  case _UT('$'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('*'):
  case _UT(','):
  case _UT(';'):
  case _UT('@'):
  case _UT('_'):
  case _UT('~'):
  case _UT('='):
  case _UT('\''):
    return URI_FUNC(ParseMustBeSegmentNzNc)(state, first + 1, afterLast);

  case _UT('/'):
    {
      const URI_CHAR * afterZeroMoreSlashSegs;
      const URI_CHAR * const afterSegment
          = URI_FUNC(ParseSegment)(state, first + 1, afterLast);
      if (afterSegment == NULL) {
        return NULL;
      }
      if (!URI_FUNC(PushPathSegment)(state, state->uri->scheme.first, first)) { /* SEGMENT BOTH */
        URI_FUNC(StopMalloc)(state);
        return NULL;
      }
      state->uri->scheme.first = NULL; /* Not a scheme, reset */
      if (!URI_FUNC(PushPathSegment)(state, first + 1, afterSegment)) { /* SEGMENT BOTH */
        URI_FUNC(StopMalloc)(state);
        return NULL;
      }
      afterZeroMoreSlashSegs
          = URI_FUNC(ParseZeroMoreSlashSegs)(state, afterSegment, afterLast);
      if (afterZeroMoreSlashSegs == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseUriTail)(state, afterZeroMoreSlashSegs, afterLast);
    }

  case _UT(':'):
    {
      const URI_CHAR * const afterHierPart
          = URI_FUNC(ParseHierPart)(state, first + 1, afterLast);
      state->uri->scheme.afterLast = first; /* SCHEME END */
      if (afterHierPart == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseUriTail)(state, afterHierPart, afterLast);
    }

  default:
    if (!URI_FUNC(OnExitSegmentNzNcOrScheme2)(state, first)) {
      URI_FUNC(StopMalloc)(state);
      return NULL;
    }
    return URI_FUNC(ParseUriTail)(state, first, afterLast);
  }
}



/*
 * [uriReference]->[ALPHA][segmentNzNcOrScheme2]
 * [uriReference]->[DIGIT][mustBeSegmentNzNc]
 * [uriReference]->[pctEncoded][mustBeSegmentNzNc]
 * [uriReference]->[subDelims][mustBeSegmentNzNc]
 * [uriReference]->[uriTail] // can take <NULL>
 * [uriReference]-><.>[mustBeSegmentNzNc]
 * [uriReference]-></>[partHelperTwo][uriTail]
 * [uriReference]-><@>[mustBeSegmentNzNc]
 * [uriReference]-><_>[mustBeSegmentNzNc]
 * [uriReference]-><~>[mustBeSegmentNzNc]
 * [uriReference]-><->[mustBeSegmentNzNc]
 */
static const URI_CHAR * URI_FUNC(ParseUriReference)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case URI_SET_ALPHA:
    state->uri->scheme.first = first; /* SCHEME BEGIN */
    return URI_FUNC(ParseSegmentNzNcOrScheme2)(state, first + 1, afterLast);

  case URI_SET_DIGIT:
  case _UT('!'):
  case _UT('$'):
  case _UT('&'):
  case _UT('('):
  case _UT(')'):
  case _UT('*'):
  case _UT(','):
  case _UT(';'):
  case _UT('\''):
  case _UT('+'):
  case _UT('='):
  case _UT('.'):
  case _UT('_'):
  case _UT('~'):
  case _UT('-'):
  case _UT('@'):
    state->uri->scheme.first = first; /* SEGMENT BEGIN, ABUSE SCHEME POINTER */
    return URI_FUNC(ParseMustBeSegmentNzNc)(state, first + 1, afterLast);

  case _UT('%'):
    {
      const URI_CHAR * const afterPctEncoded
          = URI_FUNC(ParsePctEncoded)(state, first, afterLast);
      if (afterPctEncoded == NULL) {
        return NULL;
      }
      state->uri->scheme.first = first; /* SEGMENT BEGIN, ABUSE SCHEME POINTER */
      return URI_FUNC(ParseMustBeSegmentNzNc)(state, afterPctEncoded, afterLast);
    }

  case _UT('/'):
    {
      const URI_CHAR * const afterPartHelperTwo
          = URI_FUNC(ParsePartHelperTwo)(state, first + 1, afterLast);
      if (afterPartHelperTwo == NULL) {
        return NULL;
      }
      return URI_FUNC(ParseUriTail)(state, afterPartHelperTwo, afterLast);
    }

  default:
    return URI_FUNC(ParseUriTail)(state, first, afterLast);
  }
}



/*
 * [uriTail]-><#>[queryFrag]
 * [uriTail]-><?>[queryFrag][uriTailTwo]
 * [uriTail]-><NULL>
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseUriTail)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('#'):
    {
      const URI_CHAR * const afterQueryFrag = URI_FUNC(ParseQueryFrag)(state, first + 1, afterLast);
      if (afterQueryFrag == NULL) {
        return NULL;
      }
      state->uri->fragment.first = first + 1; /* FRAGMENT BEGIN */
      state->uri->fragment.afterLast = afterQueryFrag; /* FRAGMENT END */
      return afterQueryFrag;
    }

  case _UT('?'):
    {
      const URI_CHAR * const afterQueryFrag
          = URI_FUNC(ParseQueryFrag)(state, first + 1, afterLast);
      if (afterQueryFrag == NULL) {
        return NULL;
      }
      state->uri->query.first = first + 1; /* QUERY BEGIN */
      state->uri->query.afterLast = afterQueryFrag; /* QUERY END */
      return URI_FUNC(ParseUriTailTwo)(state, afterQueryFrag, afterLast);
    }

  default:
    return first;
  }
}



/*
 * [uriTailTwo]-><#>[queryFrag]
 * [uriTailTwo]-><NULL>
 */
static URI_INLINE const URI_CHAR * URI_FUNC(ParseUriTailTwo)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('#'):
    {
      const URI_CHAR * const afterQueryFrag = URI_FUNC(ParseQueryFrag)(state, first + 1, afterLast);
      if (afterQueryFrag == NULL) {
        return NULL;
      }
      state->uri->fragment.first = first + 1; /* FRAGMENT BEGIN */
      state->uri->fragment.afterLast = afterQueryFrag; /* FRAGMENT END */
      return afterQueryFrag;
    }

  default:
    return first;
  }
}



/*
 * [zeroMoreSlashSegs]-></>[segment][zeroMoreSlashSegs]
 * [zeroMoreSlashSegs]-><NULL>
 */
static const URI_CHAR * URI_FUNC(ParseZeroMoreSlashSegs)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  if (first >= afterLast) {
    return afterLast;
  }

  switch (*first) {
  case _UT('/'):
    {
      const URI_CHAR * const afterSegment
          = URI_FUNC(ParseSegment)(state, first + 1, afterLast);
      if (afterSegment == NULL) {
        return NULL;
      }
      if (!URI_FUNC(PushPathSegment)(state, first + 1, afterSegment)) { /* SEGMENT BOTH */
        URI_FUNC(StopMalloc)(state);
        return NULL;
      }
      return URI_FUNC(ParseZeroMoreSlashSegs)(state, afterSegment, afterLast);
    }

  default:
    return first;
  }
}



static URI_INLINE void URI_FUNC(ResetParserState)(URI_TYPE(ParserState) * state) {
  URI_TYPE(Uri) * const uriBackup = state->uri;
  memset(state, 0, sizeof(URI_TYPE(ParserState)));
  state->uri = uriBackup;
}



static URI_INLINE UriBool URI_FUNC(PushPathSegment)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  URI_TYPE(PathSegment) * segment = ( URI_TYPE(PathSegment) * )malloc(1 * sizeof(URI_TYPE(PathSegment)));
  if (segment == NULL) {
    return URI_FALSE; /* Raises malloc error */
  }
  memset(segment, 0, sizeof(URI_TYPE(PathSegment)));
  if (first == afterLast) {
    segment->text.first = URI_FUNC(SafeToPointTo);
    segment->text.afterLast = URI_FUNC(SafeToPointTo);
  } else {
    segment->text.first = first;
    segment->text.afterLast = afterLast;
  }

  /* First segment ever? */
  if (state->uri->pathHead == NULL) {
    /* First segement ever, set head and tail */
    state->uri->pathHead = segment;
    state->uri->pathTail = segment;
  } else {
    /* Append, update tail */
    state->uri->pathTail->next = segment;
    state->uri->pathTail = segment;
  }

  return URI_TRUE; /* Success */
}



int URI_FUNC(ParseUriEx)(URI_TYPE(ParserState) * state, const URI_CHAR * first, const URI_CHAR * afterLast) {
  const URI_CHAR * afterUriReference;
  URI_TYPE(Uri) * uri;

  /* Check params */
  if ((state == NULL) || (first == NULL) || (afterLast == NULL)) {
    return URI_ERROR_NULL;
  }
  uri = state->uri;

  /* Init parser */
  URI_FUNC(ResetParserState)(state);
  URI_FUNC(ResetUri)(uri);

  /* Parse */
  afterUriReference = URI_FUNC(ParseUriReference)(state, first, afterLast);
  if (afterUriReference == NULL) {
    return state->errorCode;
  }
  if (afterUriReference != afterLast) {
    return URI_ERROR_SYNTAX;
  }
  return URI_SUCCESS;
}



int URI_FUNC(ParseUri)(URI_TYPE(ParserState) * state, const URI_CHAR * text) {
  if ((state == NULL) || (text == NULL)) {
    return URI_ERROR_NULL;
  }
  return URI_FUNC(ParseUriEx)(state, text, text + URI_STRLEN(text));
}



void URI_FUNC(FreeUriMembers)(URI_TYPE(Uri) * uri) {
  if (uri == NULL) {
    return;
  }

  if (uri->owner) {
    /* Scheme */
    if (uri->scheme.first != NULL) {
      if (uri->scheme.first != uri->scheme.afterLast) {
        free((URI_CHAR *)uri->scheme.first);
      }
      uri->scheme.first = NULL;
      uri->scheme.afterLast = NULL;
    }

    /* User info */
    if (uri->userInfo.first != NULL) {
      if (uri->userInfo.first != uri->userInfo.afterLast) {
        free((URI_CHAR *)uri->userInfo.first);
      }
      uri->userInfo.first = NULL;
      uri->userInfo.afterLast = NULL;
    }

    /* Host data - IPvFuture */
    if (uri->hostData.ipFuture.first != NULL) {
      if (uri->hostData.ipFuture.first != uri->hostData.ipFuture.afterLast) {
        free((URI_CHAR *)uri->hostData.ipFuture.first);
      }
      uri->hostData.ipFuture.first = NULL;
      uri->hostData.ipFuture.afterLast = NULL;
      uri->hostText.first = NULL;
      uri->hostText.afterLast = NULL;
    }

    /* Host text (if regname, after IPvFuture!) */
    if ((uri->hostText.first != NULL)
        && (uri->hostData.ip4 == NULL)
        && (uri->hostData.ip6 == NULL)) {
      /* Real regname */
      if (uri->hostText.first != uri->hostText.afterLast) {
        free((URI_CHAR *)uri->hostText.first);
      }
      uri->hostText.first = NULL;
      uri->hostText.afterLast = NULL;
    }
  }

  /* Host data - IPv4 */
  if (uri->hostData.ip4 != NULL) {
    free(uri->hostData.ip4);
    uri->hostData.ip4 = NULL;
  }

  /* Host data - IPv6 */
  if (uri->hostData.ip6 != NULL) {
    free(uri->hostData.ip6);
    uri->hostData.ip6 = NULL;
  }

  /* Port text */
  if (uri->owner && (uri->portText.first != NULL)) {
    if (uri->portText.first != uri->portText.afterLast) {
      free((URI_CHAR *)uri->portText.first);
    }
    uri->portText.first = NULL;
    uri->portText.afterLast = NULL;
  }

  /* Path */
  if (uri->pathHead != NULL) {
    URI_TYPE(PathSegment) * segWalk = uri->pathHead;
    while (segWalk != NULL) {
      URI_TYPE(PathSegment) * const next = segWalk->next;
      if (uri->owner && (segWalk->text.first != NULL)
          && (segWalk->text.first < segWalk->text.afterLast)) {
        free((URI_CHAR *)segWalk->text.first);
      }
      free(segWalk);
      segWalk = next;
    }
    uri->pathHead = NULL;
    uri->pathTail = NULL;
  }

  if (uri->owner) {
    /* Query */
    if (uri->query.first != NULL) {
      if (uri->query.first != uri->query.afterLast) {
        free((URI_CHAR *)uri->query.first);
      }
      uri->query.first = NULL;
      uri->query.afterLast = NULL;
    }

    /* Fragment */
    if (uri->fragment.first != NULL) {
      if (uri->fragment.first != uri->fragment.afterLast) {
        free((URI_CHAR *)uri->fragment.first);
      }
      uri->fragment.first = NULL;
      uri->fragment.afterLast = NULL;
    }
  }
}



UriBool URI_FUNC(_TESTING_ONLY_ParseIpSix)(const URI_CHAR * text) {
  URI_TYPE(Uri) uri;
  URI_TYPE(ParserState) parser;
  const URI_CHAR * const afterIpSix = text + URI_STRLEN(text);
  const URI_CHAR * res;

  URI_FUNC(ResetParserState)(&parser);
  URI_FUNC(ResetUri)(&uri);
  parser.uri = &uri;
  parser.uri->hostData.ip6 = ( UriIp6* )malloc(1 * sizeof(UriIp6));
  res = URI_FUNC(ParseIPv6address2)(&parser, text, afterIpSix);
  URI_FUNC(FreeUriMembers)(&uri);
  return res == afterIpSix ? URI_TRUE : URI_FALSE;
}



UriBool URI_FUNC(_TESTING_ONLY_ParseIpFour)(const URI_CHAR * text) {
  unsigned char octets[4];
  int res = URI_FUNC(ParseIpFourAddress)(octets, text, text + URI_STRLEN(text));
  return (res == URI_SUCCESS) ? URI_TRUE : URI_FALSE;
}



#undef URI_SET_DIGIT
#undef URI_SET_HEX_LETTER_UPPER
#undef URI_SET_HEX_LETTER_LOWER
#undef URI_SET_HEXDIG
#undef URI_SET_ALPHA


/* Source: UriQuery.c */
static int URI_FUNC(ComposeQueryEngine)(URI_CHAR * dest,
    const URI_TYPE(QueryList) * queryList,
    int maxChars, int * charsWritten, int * charsRequired,
    UriBool spaceToPlus, UriBool normalizeBreaks);

static UriBool URI_FUNC(AppendQueryItem)(URI_TYPE(QueryList) ** prevNext,
    int * itemCount, const URI_CHAR * keyFirst, const URI_CHAR * keyAfter,
    const URI_CHAR * valueFirst, const URI_CHAR * valueAfter,
    UriBool plusToSpace, UriBreakConversion breakConversion);



int URI_FUNC(ComposeQueryCharsRequired)(const URI_TYPE(QueryList) * queryList,
    int * charsRequired) {
  const UriBool spaceToPlus = URI_TRUE;
  const UriBool normalizeBreaks = URI_TRUE;

  return URI_FUNC(ComposeQueryCharsRequiredEx)(queryList, charsRequired,
      spaceToPlus, normalizeBreaks);
}



int URI_FUNC(ComposeQueryCharsRequiredEx)(const URI_TYPE(QueryList) * queryList,
    int * charsRequired, UriBool spaceToPlus, UriBool normalizeBreaks) {
  if ((queryList == NULL) || (charsRequired == NULL)) {
    return URI_ERROR_NULL;
  }

  return URI_FUNC(ComposeQueryEngine)(NULL, queryList, 0, NULL,
      charsRequired, spaceToPlus, normalizeBreaks);
}



int URI_FUNC(ComposeQuery)(URI_CHAR * dest,
               const URI_TYPE(QueryList) * queryList, int maxChars, int * charsWritten) {
  const UriBool spaceToPlus = URI_TRUE;
  const UriBool normalizeBreaks = URI_TRUE;

  return URI_FUNC(ComposeQueryEx)(dest, queryList, maxChars, charsWritten,
      spaceToPlus, normalizeBreaks);
}



int URI_FUNC(ComposeQueryEx)(URI_CHAR * dest,
    const URI_TYPE(QueryList) * queryList, int maxChars, int * charsWritten,
    UriBool spaceToPlus, UriBool normalizeBreaks) {
  if ((dest == NULL) || (queryList == NULL)) {
    return URI_ERROR_NULL;
  }

  if (maxChars < 1) {
    return URI_ERROR_OUTPUT_TOO_LARGE;
  }

  return URI_FUNC(ComposeQueryEngine)(dest, queryList, maxChars,
      charsWritten, NULL, spaceToPlus, normalizeBreaks);
}



int URI_FUNC(ComposeQueryMalloc)(URI_CHAR ** dest,
    const URI_TYPE(QueryList) * queryList) {
  const UriBool spaceToPlus = URI_TRUE;
  const UriBool normalizeBreaks = URI_TRUE;

  return URI_FUNC(ComposeQueryMallocEx)(dest, queryList,
      spaceToPlus, normalizeBreaks);
}



int URI_FUNC(ComposeQueryMallocEx)(URI_CHAR ** dest,
    const URI_TYPE(QueryList) * queryList,
    UriBool spaceToPlus, UriBool normalizeBreaks) {
  int charsRequired;
  int res;
  URI_CHAR * queryString;

  if (dest == NULL) {
    return URI_ERROR_NULL;
  }

  /* Calculate space */
  res = URI_FUNC(ComposeQueryCharsRequiredEx)(queryList, &charsRequired,
      spaceToPlus, normalizeBreaks);
  if (res != URI_SUCCESS) {
    return res;
  }
  charsRequired++;

  /* Allocate space */
  queryString = ( URI_CHAR* )malloc(charsRequired * sizeof(URI_CHAR));
  if (queryString == NULL) {
    return URI_ERROR_MALLOC;
  }

  /* Put query in */
  res = URI_FUNC(ComposeQueryEx)(queryString, queryList, charsRequired,
      NULL, spaceToPlus, normalizeBreaks);
  if (res != URI_SUCCESS) {
    free(queryString);
    return res;
  }

  *dest = queryString;
  return URI_SUCCESS;
}



int URI_FUNC(ComposeQueryEngine)(URI_CHAR * dest,
    const URI_TYPE(QueryList) * queryList,
    int maxChars, int * charsWritten, int * charsRequired,
    UriBool spaceToPlus, UriBool normalizeBreaks) {
  UriBool firstItem = URI_TRUE;
  int ampersandLen = 0;
  URI_CHAR * write = dest;

  /* Subtract terminator */
  if (dest == NULL) {
    *charsRequired = 0;
  } else {
    maxChars--;
  }

  while (queryList != NULL) {
    const URI_CHAR * const key = queryList->key;
    const URI_CHAR * const value = queryList->value;
    const int worstCase = (normalizeBreaks == URI_TRUE ? 6 : 3);
    const int keyLen = (key == NULL) ? 0 : (int)URI_STRLEN(key);
    const int keyRequiredChars = worstCase * keyLen;
    const int valueLen = (value == NULL) ? 0 : (int)URI_STRLEN(value);
    const int valueRequiredChars = worstCase * valueLen;

    if (dest == NULL) {
      if (firstItem == URI_TRUE) {
        ampersandLen = 1;
        firstItem = URI_FALSE;
      }

      (*charsRequired) += ampersandLen + keyRequiredChars + ((value == NULL)
            ? 0
            : 1 + valueRequiredChars);
    } else {
      URI_CHAR * afterKey;

      if ((write - dest) + ampersandLen + keyRequiredChars > maxChars) {
        return URI_ERROR_OUTPUT_TOO_LARGE;
      }

      /* Copy key */
      if (firstItem == URI_TRUE) {
        firstItem = URI_FALSE;
      } else {
        write[0] = _UT('&');
        write++;
      }
      afterKey = URI_FUNC(EscapeEx)(key, key + keyLen,
          write, spaceToPlus, normalizeBreaks);
      write += (afterKey - write);

      if (value != NULL) {
        URI_CHAR * afterValue;

        if ((write - dest) + 1 + valueRequiredChars > maxChars) {
          return URI_ERROR_OUTPUT_TOO_LARGE;
        }

        /* Copy value */
        write[0] = _UT('=');
        write++;
        afterValue = URI_FUNC(EscapeEx)(value, value + valueLen,
            write, spaceToPlus, normalizeBreaks);
        write += (afterValue - write);
      }
    }

    queryList = queryList->next;
  }

  if (dest != NULL) {
    write[0] = _UT('\0');
    if (charsWritten != NULL) {
      *charsWritten = (int)(write - dest) + 1; /* .. for terminator */
    }
  }

  return URI_SUCCESS;
}



UriBool URI_FUNC(AppendQueryItem)(URI_TYPE(QueryList) ** prevNext,
    int * itemCount, const URI_CHAR * keyFirst, const URI_CHAR * keyAfter,
    const URI_CHAR * valueFirst, const URI_CHAR * valueAfter,
    UriBool plusToSpace, UriBreakConversion breakConversion) {
  const int keyLen = (int)(keyAfter - keyFirst);
  const int valueLen = (int)(valueAfter - valueFirst);
  URI_CHAR * key;
  URI_CHAR * value;

  if ((prevNext == NULL) || (itemCount == NULL)
      || (keyFirst == NULL) || (keyAfter == NULL)
      || (keyFirst > keyAfter) || (valueFirst > valueAfter)
      || ((keyFirst == keyAfter)
        && (valueFirst == NULL) && (valueAfter == NULL))) {
    return URI_TRUE;
  }

  /* Append new empty item */
  *prevNext = ( URI_TYPE(QueryList) * )malloc(1 * sizeof(URI_TYPE(QueryList)));
  if (*prevNext == NULL) {
    return URI_FALSE; /* Raises malloc error */
  }
  (*prevNext)->next = NULL;


  /* Fill key */
  key = ( URI_CHAR* )malloc((keyLen + 1) * sizeof(URI_CHAR));
  if (key == NULL) {
    free(*prevNext);
    *prevNext = NULL;
    return URI_FALSE; /* Raises malloc error */
  }

  key[keyLen] = _UT('\0');
  if (keyLen > 0) {
    /* Copy 1:1 */
    memcpy(key, keyFirst, keyLen * sizeof(URI_CHAR));

    /* Unescape */
    URI_FUNC(UnescapeInPlaceEx)(key, plusToSpace, breakConversion);
  }
  (*prevNext)->key = key;


  /* Fill value */
  if (valueFirst != NULL) {
    value = ( URI_CHAR* )malloc((valueLen + 1) * sizeof(URI_CHAR));
    if (value == NULL) {
      free(key);
      free(*prevNext);
      *prevNext = NULL;
      return URI_FALSE; /* Raises malloc error */
    }

    value[valueLen] = _UT('\0');
    if (valueLen > 0) {
      /* Copy 1:1 */
      memcpy(value, valueFirst, valueLen * sizeof(URI_CHAR));

      /* Unescape */
      URI_FUNC(UnescapeInPlaceEx)(value, plusToSpace, breakConversion);
    }
    (*prevNext)->value = value;
  } else {
    value = NULL;
  }
  (*prevNext)->value = value;

  (*itemCount)++;
  return URI_TRUE;
}



void URI_FUNC(FreeQueryList)(URI_TYPE(QueryList) * queryList) {
  while (queryList != NULL) {
    URI_TYPE(QueryList) * nextBackup = queryList->next;
    free(queryList->key);
    free(queryList->value);
    free(queryList);
    queryList = nextBackup;
  }
}



int URI_FUNC(DissectQueryMalloc)(URI_TYPE(QueryList) ** dest, int * itemCount,
    const URI_CHAR * first, const URI_CHAR * afterLast) {
  const UriBool plusToSpace = URI_TRUE;
  const UriBreakConversion breakConversion = URI_BR_DONT_TOUCH;

  return URI_FUNC(DissectQueryMallocEx)(dest, itemCount, first, afterLast,
      plusToSpace, breakConversion);
}



int URI_FUNC(DissectQueryMallocEx)(URI_TYPE(QueryList) ** dest, int * itemCount,
    const URI_CHAR * first, const URI_CHAR * afterLast,
    UriBool plusToSpace, UriBreakConversion breakConversion) {
  const URI_CHAR * walk = first;
  const URI_CHAR * keyFirst = first;
  const URI_CHAR * keyAfter = NULL;
  const URI_CHAR * valueFirst = NULL;
  const URI_CHAR * valueAfter = NULL;
  URI_TYPE(QueryList) ** prevNext = dest;
  int nullCounter;
  int * itemsAppended = (itemCount == NULL) ? &nullCounter : itemCount;

  if ((dest == NULL) || (first == NULL) || (afterLast == NULL)) {
    return URI_ERROR_NULL;
  }

  if (first > afterLast) {
    return URI_ERROR_RANGE_INVALID;
  }

  *dest = NULL;
  *itemsAppended = 0;

  /* Parse query string */
  for (; walk < afterLast; walk++) {
    switch (*walk) {
    case _UT('&'):
      if (valueFirst != NULL) {
        valueAfter = walk;
      } else {
        keyAfter = walk;
      }

      if (URI_FUNC(AppendQueryItem)(prevNext, itemsAppended,
          keyFirst, keyAfter, valueFirst, valueAfter,
          plusToSpace, breakConversion)
          == URI_FALSE) {
        /* Free list we built */
        *itemsAppended = 0;
        URI_FUNC(FreeQueryList)(*dest);
        return URI_ERROR_MALLOC;
      }

      /* Make future items children of the current */
      if ((prevNext != NULL) && (*prevNext != NULL)) {
        prevNext = &((*prevNext)->next);
      }

      if (walk + 1 < afterLast) {
        keyFirst = walk + 1;
      } else {
        keyFirst = NULL;
      }
      keyAfter = NULL;
      valueFirst = NULL;
      valueAfter = NULL;
      break;

    case _UT('='):
      /* NOTE: WE treat the first '=' as a separator, */
      /*       all following go into the value part   */
      if (keyAfter == NULL) {
        keyAfter = walk;
        if (walk + 1 < afterLast) {
          valueFirst = walk + 1;
          valueAfter = walk + 1;
        }
      }
      break;

    default:
      break;
    }
  }

  if (valueFirst != NULL) {
    /* Must be key/value pair */
    valueAfter = walk;
  } else {
    /* Must be key only */
    keyAfter = walk;
  }

  if (URI_FUNC(AppendQueryItem)(prevNext, itemsAppended, keyFirst, keyAfter,
      valueFirst, valueAfter, plusToSpace, breakConversion)
      == URI_FALSE) {
    /* Free list we built */
    *itemsAppended = 0;
    URI_FUNC(FreeQueryList)(*dest);
    return URI_ERROR_MALLOC;
  }

  return URI_SUCCESS;
}


/* Source: UriRecompose.c */


int URI_FUNC(ToStringCharsRequired)(const URI_TYPE(Uri) * uri,
    int * charsRequired) {
  const int MAX_CHARS = ((unsigned int)-1) >> 1;
  return URI_FUNC(ToStringEngine)(NULL, uri, MAX_CHARS, NULL, charsRequired);
}



int URI_FUNC(ToString)(URI_CHAR * dest, const URI_TYPE(Uri) * uri,
    int maxChars, int * charsWritten) {
  return URI_FUNC(ToStringEngine)(dest, uri, maxChars, charsWritten, NULL);
}



static URI_INLINE int URI_FUNC(ToStringEngine)(URI_CHAR * dest,
    const URI_TYPE(Uri) * uri, int maxChars, int * charsWritten,
    int * charsRequired) {
  int written = 0;
  if ((uri == NULL) || ((dest == NULL) && (charsRequired == NULL))) {
    if (charsWritten != NULL) {
      *charsWritten = 0;
    }
    return URI_ERROR_NULL;
  }

  if (maxChars < 1) {
    if (charsWritten != NULL) {
      *charsWritten = 0;
    }
    return URI_ERROR_TOSTRING_TOO_LONG;
  }
  maxChars--; /* So we don't have to substract 1 for '\0' all the time */

  /* [01/19]  result = "" */
        if (dest != NULL) {
          dest[0] = _UT('\0');
        } else {
          (*charsRequired) = 0;
        }
  /* [02/19]  if defined(scheme) then */
        if (uri->scheme.first != NULL) {
  /* [03/19]    append scheme to result; */
          const int charsToWrite
              = (int)(uri->scheme.afterLast - uri->scheme.first);
          if (dest != NULL) {
            if (written + charsToWrite <= maxChars) {
              memcpy(dest + written, uri->scheme.first,
                  charsToWrite * sizeof(URI_CHAR));
              written += charsToWrite;
            } else {
              dest[0] = _UT('\0');
              if (charsWritten != NULL) {
                *charsWritten = 0;
              }
              return URI_ERROR_TOSTRING_TOO_LONG;
            }
          } else {
            (*charsRequired) += charsToWrite;
          }
  /* [04/19]    append ":" to result; */
          if (dest != NULL) {
            if (written + 1 <= maxChars) {
              memcpy(dest + written, _UT(":"),
                  1 * sizeof(URI_CHAR));
              written += 1;
            } else {
              dest[0] = _UT('\0');
              if (charsWritten != NULL) {
                *charsWritten = 0;
              }
              return URI_ERROR_TOSTRING_TOO_LONG;
            }
          } else {
            (*charsRequired) += 1;
          }
  /* [05/19]  endif; */
        }
  /* [06/19]  if defined(authority) then */
        if (URI_FUNC(IsHostSet)(uri)) {
  /* [07/19]    append "//" to result; */
          if (dest != NULL) {
            if (written + 2 <= maxChars) {
              memcpy(dest + written, _UT("//"),
                  2 * sizeof(URI_CHAR));
              written += 2;
            } else {
              dest[0] = _UT('\0');
              if (charsWritten != NULL) {
                *charsWritten = 0;
              }
              return URI_ERROR_TOSTRING_TOO_LONG;
            }
          } else {
            (*charsRequired) += 2;
          }
  /* [08/19]    append authority to result; */
          /* UserInfo */
          if (uri->userInfo.first != NULL) {
            const int charsToWrite = (int)(uri->userInfo.afterLast - uri->userInfo.first);
            if (dest != NULL) {
              if (written + charsToWrite <= maxChars) {
                memcpy(dest + written, uri->userInfo.first,
                    charsToWrite * sizeof(URI_CHAR));
                written += charsToWrite;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }

              if (written + 1 <= maxChars) {
                memcpy(dest + written, _UT("@"),
                    1 * sizeof(URI_CHAR));
                written += 1;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }
            } else {
              (*charsRequired) += charsToWrite + 1;
            }
          }

          /* Host */
          if (uri->hostData.ip4 != NULL) {
            /* IPv4 */
            int i = 0;
            for (; i < 4; i++) {
              const unsigned char value = uri->hostData.ip4->data[i];
              const int charsToWrite = (value > 99) ? 3 : ((value > 9) ? 2 : 1);
              if (dest != NULL) {
                if (written + charsToWrite <= maxChars) {
                  URI_CHAR text[4];
                  if (value > 99) {
                    text[0] = _UT('0') + (value / 100);
                    text[1] = _UT('0') + ((value % 100) / 10);
                    text[2] = _UT('0') + (value % 10);
                  } else if (value > 9)  {
                    text[0] = _UT('0') + (value / 10);
                    text[1] = _UT('0') + (value % 10);
                  } else {
                    text[0] = _UT('0') + value;
                  }
                  text[charsToWrite] = _UT('\0');
                  memcpy(dest + written, text, charsToWrite * sizeof(URI_CHAR));
                  written += charsToWrite;
                } else {
                  dest[0] = _UT('\0');
                  if (charsWritten != NULL) {
                    *charsWritten = 0;
                  }
                  return URI_ERROR_TOSTRING_TOO_LONG;
                }
                if (i < 3) {
                  if (written + 1 <= maxChars) {
                    memcpy(dest + written, _UT("."),
                        1 * sizeof(URI_CHAR));
                    written += 1;
                  } else {
                    dest[0] = _UT('\0');
                    if (charsWritten != NULL) {
                      *charsWritten = 0;
                    }
                    return URI_ERROR_TOSTRING_TOO_LONG;
                  }
                }
              } else {
                (*charsRequired) += charsToWrite + 1;
              }
            }
          } else if (uri->hostData.ip6 != NULL) {
            /* IPv6 */
            int i = 0;
            if (dest != NULL) {
              if (written + 1 <= maxChars) {
                memcpy(dest + written, _UT("["),
                    1 * sizeof(URI_CHAR));
                written += 1;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }
            } else {
              (*charsRequired) += 1;
            }

            for (; i < 16; i++) {
              const unsigned char value = uri->hostData.ip6->data[i];
              if (dest != NULL) {
                if (written + 2 <= maxChars) {
                  URI_CHAR text[3];
                  text[0] = URI_FUNC(HexToLetterEx)(value / 16, URI_FALSE);
                  text[1] = URI_FUNC(HexToLetterEx)(value % 16, URI_FALSE);
                  text[2] = _UT('\0');
                  memcpy(dest + written, text, 2 * sizeof(URI_CHAR));
                  written += 2;
                } else {
                  dest[0] = _UT('\0');
                  if (charsWritten != NULL) {
                    *charsWritten = 0;
                  }
                  return URI_ERROR_TOSTRING_TOO_LONG;
                }
              } else {
                (*charsRequired) += 2;
              }
              if (((i & 1) == 1) && (i < 15)) {
                if (dest != NULL) {
                  if (written + 1 <= maxChars) {
                    memcpy(dest + written, _UT(":"),
                        1 * sizeof(URI_CHAR));
                    written += 1;
                  } else {
                    dest[0] = _UT('\0');
                    if (charsWritten != NULL) {
                      *charsWritten = 0;
                    }
                    return URI_ERROR_TOSTRING_TOO_LONG;
                  }
                } else {
                  (*charsRequired) += 1;
                }
              }
            }

            if (dest != NULL) {
              if (written + 1 <= maxChars) {
                memcpy(dest + written, _UT("]"),
                    1 * sizeof(URI_CHAR));
                written += 1;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }
            } else {
              (*charsRequired) += 1;
            }
          } else if (uri->hostData.ipFuture.first != NULL) {
            /* IPvFuture */
            const int charsToWrite = (int)(uri->hostData.ipFuture.afterLast
                - uri->hostData.ipFuture.first);
            if (dest != NULL) {
              if (written + 1 <= maxChars) {
                memcpy(dest + written, _UT("["),
                    1 * sizeof(URI_CHAR));
                written += 1;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }

              if (written + charsToWrite <= maxChars) {
                memcpy(dest + written, uri->hostData.ipFuture.first, charsToWrite * sizeof(URI_CHAR));
                written += charsToWrite;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }

              if (written + 1 <= maxChars) {
                memcpy(dest + written, _UT("]"),
                    1 * sizeof(URI_CHAR));
                written += 1;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }
            } else {
              (*charsRequired) += 1 + charsToWrite + 1;
            }
          } else if (uri->hostText.first != NULL) {
            /* Regname */
            const int charsToWrite = (int)(uri->hostText.afterLast - uri->hostText.first);
            if (dest != NULL) {
              if (written + charsToWrite <= maxChars) {
                memcpy(dest + written, uri->hostText.first,
                    charsToWrite * sizeof(URI_CHAR));
                written += charsToWrite;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }
            } else {
              (*charsRequired) += charsToWrite;
            }
          }

          /* Port */
          if (uri->portText.first != NULL) {
            const int charsToWrite = (int)(uri->portText.afterLast - uri->portText.first);
            if (dest != NULL) {
              /* Leading ':' */
              if (written + 1 <= maxChars) {
                  memcpy(dest + written, _UT(":"),
                      1 * sizeof(URI_CHAR));
                  written += 1;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }

              /* Port number */
              if (written + charsToWrite <= maxChars) {
                memcpy(dest + written, uri->portText.first,
                    charsToWrite * sizeof(URI_CHAR));
                written += charsToWrite;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }
            } else {
              (*charsRequired) += 1 + charsToWrite;
            }
          }
  /* [09/19]  endif; */
        }
  /* [10/19]  append path to result; */
        /* Slash needed here? */
        if (uri->absolutePath || ((uri->pathHead != NULL)
            && URI_FUNC(IsHostSet)(uri))) {
          if (dest != NULL) {
            if (written + 1 <= maxChars) {
              memcpy(dest + written, _UT("/"),
                  1 * sizeof(URI_CHAR));
              written += 1;
            } else {
              dest[0] = _UT('\0');
              if (charsWritten != NULL) {
                *charsWritten = 0;
              }
              return URI_ERROR_TOSTRING_TOO_LONG;
            }
          } else {
            (*charsRequired) += 1;
          }
        }

        if (uri->pathHead != NULL) {
          URI_TYPE(PathSegment) * walker = uri->pathHead;
          do {
            const int charsToWrite = (int)(walker->text.afterLast - walker->text.first);
            if (dest != NULL) {
              if (written + charsToWrite <= maxChars) {
                memcpy(dest + written, walker->text.first,
                    charsToWrite * sizeof(URI_CHAR));
                written += charsToWrite;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }
            } else {
              (*charsRequired) += charsToWrite;
            }

            /* Not last segment -> append slash */
            if (walker->next != NULL) {
              if (dest != NULL) {
                if (written + 1 <= maxChars) {
                  memcpy(dest + written, _UT("/"),
                      1 * sizeof(URI_CHAR));
                  written += 1;
                } else {
                  dest[0] = _UT('\0');
                  if (charsWritten != NULL) {
                    *charsWritten = 0;
                  }
                  return URI_ERROR_TOSTRING_TOO_LONG;
                }
              } else {
                (*charsRequired) += 1;
              }
            }

            walker = walker->next;
          } while (walker != NULL);
        }
  /* [11/19]  if defined(query) then */
        if (uri->query.first != NULL) {
  /* [12/19]    append "?" to result; */
          if (dest != NULL) {
            if (written + 1 <= maxChars) {
              memcpy(dest + written, _UT("?"),
                  1 * sizeof(URI_CHAR));
              written += 1;
            } else {
              dest[0] = _UT('\0');
              if (charsWritten != NULL) {
                *charsWritten = 0;
              }
              return URI_ERROR_TOSTRING_TOO_LONG;
            }
          } else {
            (*charsRequired) += 1;
          }
  /* [13/19]    append query to result; */
          {
            const int charsToWrite
                = (int)(uri->query.afterLast - uri->query.first);
            if (dest != NULL) {
              if (written + charsToWrite <= maxChars) {
                memcpy(dest + written, uri->query.first,
                    charsToWrite * sizeof(URI_CHAR));
                written += charsToWrite;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }
            } else {
              (*charsRequired) += charsToWrite;
            }
          }
  /* [14/19]  endif; */
        }
  /* [15/19]  if defined(fragment) then */
        if (uri->fragment.first != NULL) {
  /* [16/19]    append "#" to result; */
          if (dest != NULL) {
            if (written + 1 <= maxChars) {
              memcpy(dest + written, _UT("#"),
                  1 * sizeof(URI_CHAR));
              written += 1;
            } else {
              dest[0] = _UT('\0');
              if (charsWritten != NULL) {
                *charsWritten = 0;
              }
              return URI_ERROR_TOSTRING_TOO_LONG;
            }
          } else {
            (*charsRequired) += 1;
          }
  /* [17/19]    append fragment to result; */
          {
            const int charsToWrite
                = (int)(uri->fragment.afterLast - uri->fragment.first);
            if (dest != NULL) {
              if (written + charsToWrite <= maxChars) {
                memcpy(dest + written, uri->fragment.first,
                    charsToWrite * sizeof(URI_CHAR));
                written += charsToWrite;
              } else {
                dest[0] = _UT('\0');
                if (charsWritten != NULL) {
                  *charsWritten = 0;
                }
                return URI_ERROR_TOSTRING_TOO_LONG;
              }
            } else {
              (*charsRequired) += charsToWrite;
            }
          }
  /* [18/19]  endif; */
        }
  /* [19/19]  return result; */
        if (dest != NULL) {
          dest[written++] = _UT('\0');
          if (charsWritten != NULL) {
            *charsWritten = written;
          }
        }
        return URI_SUCCESS;
}


/* Source: UriResolve.c */
/* Appends a relative URI to an absolute. The last path segement of
 * the absolute URI is replaced. */
static URI_INLINE UriBool URI_FUNC(MergePath)(URI_TYPE(Uri) * absWork,
    const URI_TYPE(Uri) * relAppend) {
  URI_TYPE(PathSegment) * sourceWalker;
  URI_TYPE(PathSegment) * destPrev;
  if (relAppend->pathHead == NULL) {
    return URI_TRUE;
  }

  /* Replace last segment ("" if trailing slash) with first of append chain */
  if (absWork->pathHead == NULL) {
    URI_TYPE(PathSegment) * const dup = ( URI_TYPE(PathSegment) * const )malloc(sizeof(URI_TYPE(PathSegment)));
    if (dup == NULL) {
      return URI_FALSE; /* Raises malloc error */
    }
    dup->next = NULL;
    absWork->pathHead = dup;
    absWork->pathTail = dup;
  }
  absWork->pathTail->text.first = relAppend->pathHead->text.first;
  absWork->pathTail->text.afterLast = relAppend->pathHead->text.afterLast;

  /* Append all the others */
  sourceWalker = relAppend->pathHead->next;
  if (sourceWalker == NULL) {
    return URI_TRUE;
  }
  destPrev = absWork->pathTail;

  for (;;) {
    URI_TYPE(PathSegment) * const dup = ( URI_TYPE(PathSegment) * const )malloc(sizeof(URI_TYPE(PathSegment)));
    if (dup == NULL) {
      destPrev->next = NULL;
      absWork->pathTail = destPrev;
      return URI_FALSE; /* Raises malloc error */
    }
    dup->text = sourceWalker->text;
    destPrev->next = dup;

    if (sourceWalker->next == NULL) {
      absWork->pathTail = dup;
      absWork->pathTail->next = NULL;
      break;
    }
    destPrev = dup;
    sourceWalker = sourceWalker->next;
  }

  return URI_TRUE;
}



static int URI_FUNC(AddBaseUriImpl)(URI_TYPE(Uri) * absDest,
    const URI_TYPE(Uri) * relSource,
    const URI_TYPE(Uri) * absBase) {
  if (absDest == NULL) {
    return URI_ERROR_NULL;
  }
  URI_FUNC(ResetUri)(absDest);

  if ((relSource == NULL) || (absBase == NULL)) {
    return URI_ERROR_NULL;
  }

  /* absBase absolute? */
  if (absBase->scheme.first == NULL) {
    return URI_ERROR_ADDBASE_REL_BASE;
  }

  /* [01/32]  if defined(R.scheme) then */
        if (relSource->scheme.first != NULL) {
  /* [02/32]    T.scheme = R.scheme; */
          absDest->scheme = relSource->scheme;
  /* [03/32]    T.authority = R.authority; */
          if (!URI_FUNC(CopyAuthority)(absDest, relSource)) {
            return URI_ERROR_MALLOC;
          }
  /* [04/32]    T.path = remove_dot_segments(R.path); */
          if (!URI_FUNC(CopyPath)(absDest, relSource)) {
            return URI_ERROR_MALLOC;
          }
          if (!URI_FUNC(RemoveDotSegmentsAbsolute)(absDest)) {
            return URI_ERROR_MALLOC;
          }
  /* [05/32]    T.query = R.query; */
          absDest->query = relSource->query;
  /* [06/32]  else */
        } else {
  /* [07/32]    if defined(R.authority) then */
          if (URI_FUNC(IsHostSet)(relSource)) {
  /* [08/32]      T.authority = R.authority; */
            if (!URI_FUNC(CopyAuthority)(absDest, relSource)) {
              return URI_ERROR_MALLOC;
            }
  /* [09/32]      T.path = remove_dot_segments(R.path); */
            if (!URI_FUNC(CopyPath)(absDest, relSource)) {
              return URI_ERROR_MALLOC;
            }
            if (!URI_FUNC(RemoveDotSegmentsAbsolute)(absDest)) {
              return URI_ERROR_MALLOC;
            }
  /* [10/32]      T.query = R.query; */
            absDest->query = relSource->query;
  /* [11/32]    else */
          } else {
  /* [28/32]      T.authority = Base.authority; */
            if (!URI_FUNC(CopyAuthority)(absDest, absBase)) {
              return URI_ERROR_MALLOC;
            }
  /* [12/32]      if (R.path == "") then */
            if (relSource->pathHead == NULL) {
  /* [13/32]        T.path = Base.path; */
              if (!URI_FUNC(CopyPath)(absDest, absBase)) {
                return URI_ERROR_MALLOC;
              }
  /* [14/32]        if defined(R.query) then */
              if (relSource->query.first != NULL) {
  /* [15/32]          T.query = R.query; */
                absDest->query = relSource->query;
  /* [16/32]        else */
              } else {
  /* [17/32]          T.query = Base.query; */
                absDest->query = absBase->query;
  /* [18/32]        endif; */
              }
  /* [19/32]      else */
            } else {
  /* [20/32]        if (R.path starts-with "/") then */
              if (relSource->absolutePath) {
  /* [21/32]          T.path = remove_dot_segments(R.path); */
                if (!URI_FUNC(CopyPath)(absDest, relSource)) {
                  return URI_ERROR_MALLOC;
                }
                if (!URI_FUNC(RemoveDotSegmentsAbsolute)(absDest)) {
                  return URI_ERROR_MALLOC;
                }
  /* [22/32]        else */
              } else {
  /* [23/32]          T.path = merge(Base.path, R.path); */
                if (!URI_FUNC(CopyPath)(absDest, absBase)) {
                  return URI_ERROR_MALLOC;
                }
                if (!URI_FUNC(MergePath)(absDest, relSource)) {
                  return URI_ERROR_MALLOC;
                }
  /* [24/32]          T.path = remove_dot_segments(T.path); */
                if (!URI_FUNC(RemoveDotSegmentsAbsolute)(absDest)) {
                  return URI_ERROR_MALLOC;
                }

                if (!URI_FUNC(FixAmbiguity)(absDest)) {
                  return URI_ERROR_MALLOC;
                }
  /* [25/32]        endif; */
              }
  /* [26/32]        T.query = R.query; */
              absDest->query = relSource->query;
  /* [27/32]      endif; */
            }
            URI_FUNC(FixEmptyTrailSegment)(absDest);
  /* [29/32]    endif; */
          }
  /* [30/32]    T.scheme = Base.scheme; */
          absDest->scheme = absBase->scheme;
  /* [31/32]  endif; */
        }
  /* [32/32]  T.fragment = R.fragment; */
        absDest->fragment = relSource->fragment;

  return URI_SUCCESS;

}



int URI_FUNC(AddBaseUri)(URI_TYPE(Uri) * absDest,
    const URI_TYPE(Uri) * relSource, const URI_TYPE(Uri) * absBase) {
  const int res = URI_FUNC(AddBaseUriImpl)(absDest, relSource, absBase);
  if ((res != URI_SUCCESS) && (absDest != NULL)) {
    URI_FUNC(FreeUriMembers)(absDest);
  }
  return res;
}


/* Source: UriShorten.c */

static URI_INLINE UriBool URI_FUNC(AppendSegment)(URI_TYPE(Uri) * uri,
    const URI_CHAR * first, const URI_CHAR * afterLast) {
  /* Create segment */
  URI_TYPE(PathSegment) * segment = ( URI_TYPE(PathSegment) * const )malloc(1 * sizeof(URI_TYPE(PathSegment)));
  if (segment == NULL) {
    return URI_FALSE; /* Raises malloc error */
  }
  segment->next = NULL;
  segment->text.first = first;
  segment->text.afterLast = afterLast;

  /* Put into chain */
  if (uri->pathTail == NULL) {
    uri->pathHead = segment;
  } else {
    uri->pathTail->next = segment;
  }
  uri->pathTail = segment;

  return URI_TRUE;
}



static URI_INLINE UriBool URI_FUNC(EqualsAuthority)(const URI_TYPE(Uri) * first,
    const URI_TYPE(Uri) * second) {
  /* IPv4 */
  if (first->hostData.ip4 != NULL) {
    return ((second->hostData.ip4 != NULL)
        && !memcmp(first->hostData.ip4->data,
          second->hostData.ip4->data, 4)) ? URI_TRUE : URI_FALSE;
  }

  /* IPv6 */
  if (first->hostData.ip6 != NULL) {
    return ((second->hostData.ip6 != NULL)
        && !memcmp(first->hostData.ip6->data,
          second->hostData.ip6->data, 16)) ? URI_TRUE : URI_FALSE;
  }

  /* IPvFuture */
  if (first->hostData.ipFuture.first != NULL) {
    return ((second->hostData.ipFuture.first != NULL)
        && !URI_STRNCMP(first->hostData.ipFuture.first,
          second->hostData.ipFuture.first,
          first->hostData.ipFuture.afterLast
          - first->hostData.ipFuture.first))
            ? URI_TRUE : URI_FALSE;
  }

  if (first->hostText.first != NULL) {
    return ((second->hostText.first != NULL)
        && !URI_STRNCMP(first->hostText.first,
          second->hostText.first,
          first->hostText.afterLast
          - first->hostText.first)) ? URI_TRUE : URI_FALSE;
  }

  return (second->hostText.first == NULL);
}



int URI_FUNC(RemoveBaseUriImpl)(URI_TYPE(Uri) * dest,
    const URI_TYPE(Uri) * absSource,
    const URI_TYPE(Uri) * absBase,
    UriBool domainRootMode) {
  if (dest == NULL) {
    return URI_ERROR_NULL;
  }
  URI_FUNC(ResetUri)(dest);

  if ((absSource == NULL) || (absBase == NULL)) {
    return URI_ERROR_NULL;
  }

  /* absBase absolute? */
  if (absBase->scheme.first == NULL) {
    return URI_ERROR_REMOVEBASE_REL_BASE;
  }

  /* absSource absolute? */
  if (absSource->scheme.first == NULL) {
    return URI_ERROR_REMOVEBASE_REL_SOURCE;
  }

  /* [01/50]  if (A.scheme != Base.scheme) then */
        if (URI_STRNCMP(absSource->scheme.first, absBase->scheme.first,
            absSource->scheme.afterLast - absSource->scheme.first)) {
  /* [02/50]     T.scheme    = A.scheme; */
          dest->scheme = absSource->scheme;
  /* [03/50]     T.authority = A.authority; */
          if (!URI_FUNC(CopyAuthority)(dest, absSource)) {
            return URI_ERROR_MALLOC;
          }
  /* [04/50]     T.path      = A.path; */
          if (!URI_FUNC(CopyPath)(dest, absSource)) {
            return URI_ERROR_MALLOC;
          }
  /* [05/50]  else */
        } else {
  /* [06/50]     undef(T.scheme); */
          /* NOOP */
  /* [07/50]     if (A.authority != Base.authority) then */
          if (!URI_FUNC(EqualsAuthority)(absSource, absBase)) {
  /* [08/50]        T.authority = A.authority; */
            if (!URI_FUNC(CopyAuthority)(dest, absSource)) {
              return URI_ERROR_MALLOC;
            }
  /* [09/50]        T.path      = A.path; */
            if (!URI_FUNC(CopyPath)(dest, absSource)) {
              return URI_ERROR_MALLOC;
            }
  /* [10/50]     else */
          } else {
  /* [11/50]        if domainRootMode then */
            if (domainRootMode == URI_TRUE) {
  /* [12/50]           undef(T.authority); */
              /* NOOP */
  /* [13/50]           if (first(A.path) == "") then */
              /* GROUPED */
  /* [14/50]              T.path   = "/." + A.path; */
                /* GROUPED */
  /* [15/50]           else */
                /* GROUPED */
  /* [16/50]              T.path   = A.path; */
                /* GROUPED */
  /* [17/50]           endif; */
              if (!URI_FUNC(CopyPath)(dest, absSource)) {
                return URI_ERROR_MALLOC;
              }
              dest->absolutePath = URI_TRUE;

              if (!URI_FUNC(FixAmbiguity)(dest)) {
                return URI_ERROR_MALLOC;
              }
  /* [18/50]        else */
            } else {
              const URI_TYPE(PathSegment) * sourceSeg = absSource->pathHead;
              const URI_TYPE(PathSegment) * baseSeg = absBase->pathHead;
  /* [19/50]           bool pathNaked = true; */
              UriBool pathNaked = URI_TRUE;
  /* [20/50]           undef(last(Base.path)); */
              /* NOOP */
  /* [21/50]           T.path = ""; */
              dest->absolutePath = URI_FALSE;
  /* [22/50]           while (first(A.path) == first(Base.path)) do */
              while ((sourceSeg != NULL) && (baseSeg != NULL)
                  && !URI_STRNCMP(sourceSeg->text.first, baseSeg->text.first,
                  sourceSeg->text.afterLast - sourceSeg->text.first)
                  && !((sourceSeg->text.first == sourceSeg->text.afterLast)
                    && ((sourceSeg->next == NULL) != (baseSeg->next == NULL)))) {
  /* [23/50]              A.path++; */
                sourceSeg = sourceSeg->next;
  /* [24/50]              Base.path++; */
                baseSeg = baseSeg->next;
  /* [25/50]           endwhile; */
              }
  /* [26/50]           while defined(first(Base.path)) do */
              while ((baseSeg != NULL) && (baseSeg->next != NULL)) {
  /* [27/50]              Base.path++; */
                baseSeg = baseSeg->next;
  /* [28/50]              T.path += "../"; */
                if (!URI_FUNC(AppendSegment)(dest, URI_FUNC(ConstParent),
                    URI_FUNC(ConstParent) + 2)) {
                  return URI_ERROR_MALLOC;
                }
  /* [29/50]              pathNaked = false; */
                pathNaked = URI_FALSE;
  /* [30/50]           endwhile; */
              }
  /* [31/50]           while defined(first(A.path)) do */
              while (sourceSeg != NULL) {
  /* [32/50]              if pathNaked then */
                if (pathNaked == URI_TRUE) {
  /* [33/50]                 if (first(A.path) contains ":") then */
                  UriBool containsColon = URI_FALSE;
                  const URI_CHAR * ch = sourceSeg->text.first;
                  for (; ch < sourceSeg->text.afterLast; ch++) {
                    if (*ch == _UT(':')) {
                      containsColon = URI_TRUE;
                      break;
                    }
                  }

                  if (containsColon) {
  /* [34/50]                    T.path += "./"; */
                    if (!URI_FUNC(AppendSegment)(dest, URI_FUNC(ConstPwd),
                        URI_FUNC(ConstPwd) + 1)) {
                      return URI_ERROR_MALLOC;
                    }
  /* [35/50]                 elseif (first(A.path) == "") then */
                  } else if (sourceSeg->text.first == sourceSeg->text.afterLast) {
  /* [36/50]                    T.path += "/."; */
                    if (!URI_FUNC(AppendSegment)(dest, URI_FUNC(ConstPwd),
                        URI_FUNC(ConstPwd) + 1)) {
                      return URI_ERROR_MALLOC;
                    }
  /* [37/50]                 endif; */
                  }
  /* [38/50]              endif; */
                }
  /* [39/50]              T.path += first(A.path); */
                if (!URI_FUNC(AppendSegment)(dest, sourceSeg->text.first,
                    sourceSeg->text.afterLast)) {
                  return URI_ERROR_MALLOC;
                }
  /* [40/50]              pathNaked = false; */
                pathNaked = URI_FALSE;
  /* [41/50]              A.path++; */
                sourceSeg = sourceSeg->next;
  /* [42/50]              if defined(first(A.path)) then */
                /* NOOP */
  /* [43/50]                 T.path += + "/"; */
                /* NOOP */
  /* [44/50]              endif; */
                /* NOOP */
  /* [45/50]           endwhile; */
              }
  /* [46/50]        endif; */
            }
  /* [47/50]     endif; */
          }
  /* [48/50]  endif; */
        }
  /* [49/50]  T.query     = A.query; */
        dest->query = absSource->query;
  /* [50/50]  T.fragment  = A.fragment; */
        dest->fragment = absSource->fragment;

  return URI_SUCCESS;
}



int URI_FUNC(RemoveBaseUri)(URI_TYPE(Uri) * dest,
    const URI_TYPE(Uri) * absSource,
    const URI_TYPE(Uri) * absBase,
    UriBool domainRootMode) {
  const int res = URI_FUNC(RemoveBaseUriImpl)(dest, absSource,
      absBase, domainRootMode);
  if ((res != URI_SUCCESS) && (dest != NULL)) {
    URI_FUNC(FreeUriMembers)(dest);
  }
  return res;
}


// yajl.c
/*
 * Copyright 2007, Lloyd Hilaiel.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *  3. Neither the name of Lloyd Hilaiel nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */



#include <stdlib.h>
#include <limits.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <assert.h>
#include <math.h>


/* Source: yajl.c */
const char *
yajl_status_to_string(yajl_status stat)
{
    const char * statStr = "unknown";
    switch (stat) {
        case yajl_status_ok:
            statStr = "ok, no error";
            break;
        case yajl_status_client_canceled:
            statStr = "client canceled parse";
            break;
        case yajl_status_insufficient_data:
            statStr = "eof was met before the parse could complete";
            break;
        case yajl_status_error:
            statStr = "parse error";
            break;
    }
    return statStr;
}

yajl_handle
yajl_alloc(const yajl_callbacks * callbacks,
           const yajl_parser_config * config,
           void * ctx)
{
    unsigned int allowComments = 0;
    unsigned int validateUTF8 = 0;
    yajl_handle hand = (yajl_handle) malloc(sizeof(struct yajl_handle_t));

    if (config != NULL) {
        allowComments = config->allowComments;
        validateUTF8 = config->checkUTF8;
    }

    hand->callbacks = callbacks;
    hand->ctx = ctx;
    hand->lexer = yajl_lex_alloc(allowComments, validateUTF8);
    hand->errorOffset = 0;
    hand->decodeBuf = yajl_buf_alloc();
    hand->stateBuf = yajl_buf_alloc();

    yajl_state_push(hand, yajl_state_start);

    return hand;
}

void
yajl_free(yajl_handle handle)
{
    yajl_buf_free(handle->stateBuf);
    yajl_buf_free(handle->decodeBuf);
    yajl_lex_free(handle->lexer);
    free(handle);
}

yajl_status
yajl_parse(yajl_handle hand, const unsigned char * jsonText,
           unsigned int jsonTextLen)
{
    unsigned int offset = 0;
    yajl_status status;
    status = yajl_do_parse(hand, &offset, jsonText, jsonTextLen);
    return status;
}

unsigned char *
yajl_get_error(yajl_handle hand, int verbose,
               const unsigned char * jsonText, unsigned int jsonTextLen)
{
    return yajl_render_error_string(hand, jsonText, jsonTextLen, verbose);
}

void
yajl_free_error(unsigned char * str)
{
    /* XXX: use memory allocation functions if set */
    free(str);
}


/* yajl_buf.c */
#define YAJL_BUF_INIT_SIZE 2048

struct yajl_buf_t {
    unsigned int len;
    unsigned int used;
    unsigned char * data;
};

static
void yajl_buf_ensure_available(yajl_buf buf, unsigned int want)
{
    unsigned int need;

    assert(buf != NULL);

    /* first call */
    if (buf->data == NULL) {
        buf->len = YAJL_BUF_INIT_SIZE;
        buf->data = (unsigned char *) malloc(buf->len);
        buf->data[0] = 0;
    }

    need = buf->len;

    while (want >= (need - buf->used)) need <<= 1;

    if (need != buf->len) {
        buf->data = (unsigned char *) realloc(buf->data, need);
        buf->len = need;
    }
}

yajl_buf yajl_buf_alloc(void)
{
    return (yajl_buf) calloc(1, sizeof(struct yajl_buf_t));
}

void yajl_buf_free(yajl_buf buf)
{
    assert(buf != NULL);
    if (buf->data) free(buf->data);
    free(buf);
}

void yajl_buf_append(yajl_buf buf, const void * data, unsigned int len)
{
    yajl_buf_ensure_available(buf, len);
    if (len > 0) {
        assert(data != NULL);
        memcpy(buf->data + buf->used, data, len);
        buf->used += len;
        buf->data[buf->used] = 0;
    }
}

void yajl_buf_clear(yajl_buf buf)
{
    buf->used = 0;
    if (buf->data) buf->data[buf->used] = 0;
}

const unsigned char * yajl_buf_data(yajl_buf buf)
{
    return buf->data;
}

unsigned int yajl_buf_len(yajl_buf buf)
{
    return buf->used;
}

void
yajl_buf_truncate(yajl_buf buf, unsigned int len)
{
    assert(len <= buf->used);
    buf->used = len;
}


/* yajl_encode.c */
static void CharToHex(unsigned char c, char * hexBuf)
{
    const char * hexchar = "0123456789ABCDEF";
    hexBuf[0] = hexchar[c >> 4];
    hexBuf[1] = hexchar[c & 0x0F];
}

void
yajl_string_encode(yajl_buf buf, const unsigned char * str,
                   unsigned int len)
{
    unsigned int beg = 0;
    unsigned int end = 0;
    char hexBuf[7];
    hexBuf[0] = '\\'; hexBuf[1] = 'u'; hexBuf[2] = '0'; hexBuf[3] = '0';
    hexBuf[6] = 0;

    while (end < len) {
        char * escaped = NULL;
        switch (str[end]) {
            case '\r': escaped = "\\r"; break;
            case '\n': escaped = "\\n"; break;
            case '\\': escaped = "\\\\"; break;
            /* case '/': escaped = "\\/"; break; */
            case '"': escaped = "\\\""; break;
            case '\f': escaped = "\\f"; break;
            case '\b': escaped = "\\b"; break;
            case '\t': escaped = "\\t"; break;
            default:
                if ((unsigned char) str[end] < 32) {
                    CharToHex(str[end], hexBuf + 4);
                    escaped = hexBuf;
                }
                break;
        }
        if (escaped != NULL) {
            yajl_buf_append(buf, str + beg, end - beg);
            yajl_buf_append(buf, escaped, strlen(escaped));
            beg = ++end;
        } else {
            ++end;
        }
    }
    yajl_buf_append(buf, str + beg, end - beg);
}

static void hexToDigit(unsigned int * val, const unsigned char * hex)
{
    unsigned int i;
    for (i=0;i<4;i++) {
        unsigned char c = hex[i];
        if (c >= 'A') c = (c & ~0x20) - 7;
        c -= '0';
        assert(!(c & 0xF0));
        *val = (*val << 4) | c;
    }
}

static void Utf32toUtf8(unsigned int codepoint, char * utf8Buf)
{
    if (codepoint < 0x80) {
        utf8Buf[0] = (char) codepoint;
        utf8Buf[1] = 0;
    } else if (codepoint < 0x0800) {
        utf8Buf[0] = (char) ((codepoint >> 6) | 0xC0);
        utf8Buf[1] = (char) ((codepoint & 0x3F) | 0x80);
        utf8Buf[2] = 0;
    } else if (codepoint < 0x10000) {
        utf8Buf[0] = (char) ((codepoint >> 12) | 0xE0);
        utf8Buf[1] = (char) (((codepoint >> 6) & 0x3F) | 0x80);
        utf8Buf[2] = (char) ((codepoint & 0x3F) | 0x80);
        utf8Buf[3] = 0;
    } else if (codepoint < 0x200000) {
        utf8Buf[0] =(char)((codepoint >> 18) | 0xF0);
        utf8Buf[1] =(char)(((codepoint >> 12) & 0x3F) | 0x80);
        utf8Buf[2] =(char)(((codepoint >> 6) & 0x3F) | 0x80);
        utf8Buf[3] =(char)((codepoint & 0x3F) | 0x80);
        utf8Buf[4] = 0;
    } else {
        utf8Buf[0] = '?';
        utf8Buf[1] = 0;
    }
}

void yajl_string_decode(yajl_buf buf, const unsigned char * str,
                        unsigned int len)
{
    unsigned int beg = 0;
    unsigned int end = 0;

    while (end < len) {
        if (str[end] == '\\') {
            char utf8Buf[5];
            const char * unescaped = "?";
            yajl_buf_append(buf, str + beg, end - beg);
            switch (str[++end]) {
                case 'r': unescaped = "\r"; break;
                case 'n': unescaped = "\n"; break;
                case '\\': unescaped = "\\"; break;
                case '/': unescaped = "/"; break;
                case '"': unescaped = "\""; break;
                case 'f': unescaped = "\f"; break;
                case 'b': unescaped = "\b"; break;
                case 't': unescaped = "\t"; break;
                case 'u': {
                    unsigned int codepoint = 0;
                    hexToDigit(&codepoint, str + ++end);
                    end+=3;
                    /* check if this is a surrogate */
                    if ((codepoint & 0xFC00) == 0xD800) {
                        end++;
                        if (str[end] == '\\' && str[end + 1] == 'u') {
                            unsigned int surrogate = 0;
                            hexToDigit(&surrogate, str + end + 2);
                            codepoint =
                                (((codepoint & 0x3F) << 10) |
                                 ((((codepoint >> 6) & 0xF) + 1) << 16) |
                                 (surrogate & 0x3FF));
                            end += 5;
                        } else {
                            unescaped = "?";
                            break;
                        }
                    }

                    Utf32toUtf8(codepoint, utf8Buf);
                    unescaped = utf8Buf;
                    break;
                }
                default:
                    assert("this should never happen" == NULL);
            }
            yajl_buf_append(buf, unescaped, strlen(unescaped));
            beg = ++end;
        } else {
            end++;
        }
    }
    yajl_buf_append(buf, str + beg, end - beg);
}


/* Source: yajl_gen.c */
typedef enum {
    yajl_gen_start,
    yajl_gen_map_start,
    yajl_gen_map_key,
    yajl_gen_map_val,
    yajl_gen_array_start,
    yajl_gen_in_array,
    yajl_gen_complete,
    yajl_gen_error
} yajl_gen_state;

struct yajl_gen_t
{
    unsigned int depth;
    unsigned int pretty;
    const char * indentString;
    yajl_gen_state state[YAJL_MAX_DEPTH];
    yajl_buf buf;
};

yajl_gen
yajl_gen_alloc(const yajl_gen_config * config)
{
    yajl_gen g = (yajl_gen) malloc(sizeof(struct yajl_gen_t));
    memset((void *) g, 0, sizeof(struct yajl_gen_t));
    if (config) {
        g->pretty = config->beautify;
        g->indentString = config->indentString ? config->indentString : "  ";
    }
    g->buf = yajl_buf_alloc();
    return g;
}

void
yajl_gen_free(yajl_gen g)
{
    yajl_buf_free(g->buf);
    free(g);
}

#define INSERT_SEP \
    if (g->state[g->depth] == yajl_gen_map_key ||               \
        g->state[g->depth] == yajl_gen_in_array) {              \
        yajl_buf_append(g->buf, ",", 1);                        \
        if (g->pretty) yajl_buf_append(g->buf, "\n", 1);        \
    } else if (g->state[g->depth] == yajl_gen_map_val) {        \
        yajl_buf_append(g->buf, ":", 1);                        \
        if (g->pretty) yajl_buf_append(g->buf, " ", 1);         \
   }

#define INSERT_WHITESPACE                                              \
    if (g->pretty) {                                                    \
        if (g->state[g->depth] != yajl_gen_map_val) {                   \
            unsigned int i;                                             \
            for (i=0;i<g->depth;i++)                                    \
                yajl_buf_append(g->buf, g->indentString,                \
                                strlen(g->indentString));               \
        }                                                               \
    }

#define ENSURE_NOT_KEY \
    if (g->state[g->depth] == yajl_gen_map_key) {   \
        return yajl_gen_keys_must_be_strings;       \
    }                                               \

/* check that we're not complete, or in error state.  in a valid state
 * to be generating */
#define ENSURE_VALID_STATE \
    if (g->state[g->depth] == yajl_gen_error) {   \
        return yajl_gen_in_error_state;\
    } else if (g->state[g->depth] == yajl_gen_complete) {   \
        return yajl_gen_generation_complete;                \
    }

#define INCREMENT_DEPTH \
    if (++(g->depth) >= YAJL_MAX_DEPTH) return yajl_max_depth_exceeded;

#define APPENDED_ATOM \
    switch (g->state[g->depth]) {                   \
        case yajl_gen_start:                        \
            g->state[g->depth] = yajl_gen_complete; \
            break;                                  \
        case yajl_gen_map_start:                    \
        case yajl_gen_map_key:                      \
            g->state[g->depth] = yajl_gen_map_val;  \
            break;                                  \
        case yajl_gen_array_start:                  \
            g->state[g->depth] = yajl_gen_in_array; \
            break;                                  \
        case yajl_gen_map_val:                      \
            g->state[g->depth] = yajl_gen_map_key;  \
            break;                                  \
        default:                                    \
            break;                                  \
    }                                               \

#define FINAL_NEWLINE                                        \
    if (g->pretty && g->state[g->depth] == yajl_gen_complete) \
        yajl_buf_append(g->buf, "\n", 1);

yajl_gen_status
yajl_gen_integer(yajl_gen g, long int number)
{
    char i[32];
    ENSURE_VALID_STATE; ENSURE_NOT_KEY; INSERT_SEP; INSERT_WHITESPACE;
    sprintf(i, "%ld", number);
    yajl_buf_append(g->buf, i, strlen(i));
    APPENDED_ATOM;
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_double(yajl_gen g, double number)
{
    char i[32];
    ENSURE_VALID_STATE; ENSURE_NOT_KEY; INSERT_SEP; INSERT_WHITESPACE;
    sprintf(i, "%g", number);
    yajl_buf_append(g->buf, i, strlen(i));
    APPENDED_ATOM;
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_number(yajl_gen g, const char * s, unsigned int l)
{
    ENSURE_VALID_STATE; ENSURE_NOT_KEY; INSERT_SEP; INSERT_WHITESPACE;
    yajl_buf_append(g->buf, s, l);
    APPENDED_ATOM;
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_string(yajl_gen g, const unsigned char * str,
                unsigned int len)
{
    ENSURE_VALID_STATE; INSERT_SEP; INSERT_WHITESPACE;
    yajl_buf_append(g->buf, "\"", 1);
    yajl_string_encode(g->buf, str, len);
    yajl_buf_append(g->buf, "\"", 1);
    APPENDED_ATOM;
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_null(yajl_gen g)
{
    ENSURE_VALID_STATE; ENSURE_NOT_KEY; INSERT_SEP; INSERT_WHITESPACE;
    yajl_buf_append(g->buf, "null", strlen("null"));
    APPENDED_ATOM;
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_bool(yajl_gen g, int boolean)
{
    const char * val = boolean ? "true" : "false";

	ENSURE_VALID_STATE; ENSURE_NOT_KEY; INSERT_SEP; INSERT_WHITESPACE;
    yajl_buf_append(g->buf, val, strlen(val));
    APPENDED_ATOM;
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_map_open(yajl_gen g)
{
    ENSURE_VALID_STATE; ENSURE_NOT_KEY; INSERT_SEP; INSERT_WHITESPACE;
    INCREMENT_DEPTH;

    g->state[g->depth] = yajl_gen_map_start;
    yajl_buf_append(g->buf, "{", 1);
    if (g->pretty) yajl_buf_append(g->buf, "\n", 1);
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_map_close(yajl_gen g)
{
    ENSURE_VALID_STATE;
    (g->depth)--;
    if (g->pretty) yajl_buf_append(g->buf, "\n", 1);
    APPENDED_ATOM;
    INSERT_WHITESPACE;
    yajl_buf_append(g->buf, "}", 1);
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_array_open(yajl_gen g)
{
    ENSURE_VALID_STATE; ENSURE_NOT_KEY; INSERT_SEP; INSERT_WHITESPACE;
    INCREMENT_DEPTH;
    g->state[g->depth] = yajl_gen_array_start;
    yajl_buf_append(g->buf, "[", 1);
    if (g->pretty) yajl_buf_append(g->buf, "\n", 1);
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_array_close(yajl_gen g)
{
    ENSURE_VALID_STATE;
    if (g->pretty) yajl_buf_append(g->buf, "\n", 1);
    (g->depth)--;
    APPENDED_ATOM;
    INSERT_WHITESPACE;
    yajl_buf_append(g->buf, "]", 1);
    FINAL_NEWLINE;
    return yajl_gen_status_ok;
}

yajl_gen_status
yajl_gen_get_buf(yajl_gen g, const unsigned char ** buf,
                 unsigned int * len)
{
    *buf = yajl_buf_data(g->buf);
    *len = yajl_buf_len(g->buf);
    return yajl_gen_status_ok;
}

void
yajl_gen_clear(yajl_gen g)
{
    yajl_buf_clear(g->buf);
}


/* yajl_lex.c */
#ifdef YAJL_LEXER_DEBUG
static const char *
tokToStr(yajl_tok tok)
{
    switch (tok) {
        case yajl_tok_bool: return "bool";
        case yajl_tok_colon: return "colon";
        case yajl_tok_comma: return "comma";
        case yajl_tok_eof: return "eof";
        case yajl_tok_error: return "error";
        case yajl_tok_left_brace: return "brace";
        case yajl_tok_left_bracket: return "bracket";
        case yajl_tok_null: return "null";
        case yajl_tok_integer: return "integer";
        case yajl_tok_double: return "double";
        case yajl_tok_right_brace: return "brace";
        case yajl_tok_right_bracket: return "bracket";
        case yajl_tok_string: return "string";
        case yajl_tok_string_with_escapes: return "string_with_escapes";
    }
    return "unknown";
}
#endif

/* Impact of the stream parsing feature on the lexer:
 *
 * YAJL support stream parsing.  That is, the ability to parse the first
 * bits of a chunk of JSON before the last bits are available (still on
 * the network or disk).  This makes the lexer more complex.  The
 * responsibility of the lexer is to handle transparently the case where
 * a chunk boundary falls in the middle of a token.  This is
 * accomplished is via a buffer and a character reading abstraction.
 *
 * Overview of implementation
 *
 * When we lex to end of input string before end of token is hit, we
 * copy all of the input text composing the token into our lexBuf.
 *
 * Every time we read a character, we do so through the readChar function.
 * readChar's responsibility is to handle pulling all chars from the buffer
 * before pulling chars from input text
 */

struct yajl_lexer_t {
    /* the overal line and char offset into the data */
    unsigned int lineOff;
    unsigned int charOff;

    /* error */
    yajl_lex_error error;

    /* a input buffer to handle the case where a token is spread over
     * multiple chunks */
    yajl_buf buf;

    /* in the case where we have data in the lexBuf, bufOff holds
     * the current offset into the lexBuf. */
    unsigned int bufOff;

    /* are we using the lex buf? */
    unsigned int bufInUse;

    /* shall we allow comments? */
    unsigned int allowComments;

    /* shall we validate utf8 inside strings? */
    unsigned int validateUTF8;
};

static unsigned char
readChar(yajl_lexer lxr, const unsigned char * txt, unsigned int *off)
{
    if (lxr->bufInUse && yajl_buf_len(lxr->buf) &&
        lxr->bufOff < yajl_buf_len(lxr->buf))
    {
        return *((unsigned char *) yajl_buf_data(lxr->buf) + (lxr->bufOff)++);
    }
    return txt[(*off)++];
}

static void
unreadChar(yajl_lexer lxr, unsigned int *off)
{
    if (*off > 0) (*off)--;
    else (lxr->bufOff)--;
}

yajl_lexer
yajl_lex_alloc(unsigned int allowComments, unsigned int validateUTF8)
{
    yajl_lexer lxr = (yajl_lexer) calloc(1, sizeof(struct yajl_lexer_t));
    lxr->buf = yajl_buf_alloc();
    lxr->allowComments = allowComments;
    lxr->validateUTF8 = validateUTF8;
    return lxr;
}

void
yajl_lex_free(yajl_lexer lxr)
{
    yajl_buf_free(lxr->buf);
    free(lxr);
    return;
}

/* a lookup table which lets us quickly determine three things:
 * VEC - valid escaped conrol char
 * IJC - invalid json char
 * VHC - valid hex char
 * note.  the solidus '/' may be escaped or not.
 * note.  the
 */
#define VEC 1
#define IJC 2
#define VHC 4
const static char charLookupTable[256] =
{
/*00*/ IJC    , IJC    , IJC    , IJC    , IJC    , IJC    , IJC    , IJC    ,
/*08*/ IJC    , IJC    , IJC    , IJC    , IJC    , IJC    , IJC    , IJC    ,
/*10*/ IJC    , IJC    , IJC    , IJC    , IJC    , IJC    , IJC    , IJC    ,
/*18*/ IJC    , IJC    , IJC    , IJC    , IJC    , IJC    , IJC    , IJC    ,

/*20*/ 0      , 0      , VEC|IJC, 0      , 0      , 0      , 0      , 0      ,
/*28*/ 0      , 0      , 0      , 0      , 0      , 0      , 0      , VEC    ,
/*30*/ VHC    , VHC    , VHC    , VHC    , VHC    , VHC    , VHC    , VHC    ,
/*38*/ VHC    , VHC    , 0      , 0      , 0      , 0      , 0      , 0      ,

/*40*/ 0      , VHC    , VHC    , VHC    , VHC    , VHC    , VHC    , 0      ,
/*48*/ 0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
/*50*/ 0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
/*58*/ 0      , 0      , 0      , 0      , VEC|IJC, 0      , 0      , 0      ,

/*60*/ 0      , VHC    , VEC|VHC, VHC    , VHC    , VHC    , VEC|VHC, 0      ,
/*68*/ 0      , 0      , 0      , 0      , 0      , 0      , VEC    , 0      ,
/*70*/ 0      , 0      , VEC    , 0      , VEC    , 0      , 0      , 0      ,
/*78*/ 0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,

/* include these so we don't have to always check the range of the char */
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,

       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,

       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,

       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0      ,
       0      , 0      , 0      , 0      , 0      , 0      , 0      , 0
};

/** process a variable length utf8 encoded codepoint.
 *
 *  returns:
 *    yajl_tok_string - if valid utf8 char was parsed and offset was
 *                      advanced
 *    yajl_tok_eof - if end of input was hit before validation could
 *                   complete
 *    yajl_tok_error - if invalid utf8 was encountered
 *
 *  NOTE: on error the offset will point to the first char of the
 *  invalid utf8 */
#define UTF8_CHECK_EOF if (*offset >= jsonTextLen) { return yajl_tok_eof; }

static yajl_tok
yajl_lex_utf8_char(yajl_lexer lexer, const unsigned char * jsonText,
                   unsigned int jsonTextLen, unsigned int * offset,
                   unsigned char curChar)
{
    if (curChar <= 0x7f) {
        /* single byte */
        return yajl_tok_string;
    } else if ((curChar >> 5) == 0x6) {
        /* two byte */
        UTF8_CHECK_EOF;
        curChar = readChar(lexer, jsonText, offset);
        if ((curChar >> 6) == 0x2) return yajl_tok_string;
    } else if ((curChar >> 4) == 0x0d) {
        /* three byte */
        UTF8_CHECK_EOF;
        curChar = readChar(lexer, jsonText, offset);
        if ((curChar >> 6) == 0x2) {
            UTF8_CHECK_EOF;
            curChar = readChar(lexer, jsonText, offset);
            if ((curChar >> 6) == 0x2) return yajl_tok_string;
        }
    } else if ((curChar >> 3) == 0x1d) {
        /* four byte */
        UTF8_CHECK_EOF;
        curChar = readChar(lexer, jsonText, offset);
        if ((curChar >> 6) == 0x2) {
            UTF8_CHECK_EOF;
            curChar = readChar(lexer, jsonText, offset);
            if ((curChar >> 6) == 0x2) {
                UTF8_CHECK_EOF;
                curChar = readChar(lexer, jsonText, offset);
                if ((curChar >> 6) == 0x2) return yajl_tok_string;
            }
        }
    }

    return yajl_tok_error;
}

/* lex a string.  input is the lexer, pointer to beginning of
 * json text, and start of string (offset).
 * a token is returned which has the following meanings:
 * yajl_tok_string: lex of string was successful.  offset points to
 *                  terminating '"'.
 * yajl_tok_eof: end of text was encountered before we could complete
 *               the lex.
 * yajl_tok_error: embedded in the string were unallowable chars.  offset
 *               points to the offending char
 */
#define STR_CHECK_EOF \
if (*offset >= jsonTextLen) { \
   tok = yajl_tok_eof; \
   goto finish_string_lex; \
}

static yajl_tok
yajl_lex_string(yajl_lexer lexer, const unsigned char * jsonText,
                unsigned int jsonTextLen, unsigned int * offset)
{
    yajl_tok tok = yajl_tok_error;
    int hasEscapes = 0;

    for (;;) {
		unsigned char curChar;

		STR_CHECK_EOF;

        curChar = readChar(lexer, jsonText, offset);

        /* quote terminates */
        if (curChar == '"') {
            tok = yajl_tok_string;
            break;
        }
        /* backslash escapes a set of control chars, */
        else if (curChar == '\\') {
            hasEscapes = 1;
            STR_CHECK_EOF;

            /* special case \u */
            curChar = readChar(lexer, jsonText, offset);
            if (curChar == 'u') {
                unsigned int i = 0;

                for (i=0;i<4;i++) {
                    STR_CHECK_EOF;
                    curChar = readChar(lexer, jsonText, offset);
                    if (!(charLookupTable[curChar] & VHC)) {
                        /* back up to offending char */
                        unreadChar(lexer, offset);
                        lexer->error = yajl_lex_string_invalid_hex_char;
                        goto finish_string_lex;
                    }
                }
            } else if (!(charLookupTable[curChar] & VEC)) {
                /* back up to offending char */
                unreadChar(lexer, offset);
                lexer->error = yajl_lex_string_invalid_escaped_char;
                goto finish_string_lex;
            }
        }
        /* when not validating UTF8 it's a simple table lookup to determine
         * if the present character is invalid */
        else if(charLookupTable[curChar] & IJC) {
            /* back up to offending char */
            unreadChar(lexer, offset);
            lexer->error = yajl_lex_string_invalid_json_char;
            goto finish_string_lex;
        }
        /* when in validate UTF8 mode we need to do some extra work */
        else if (lexer->validateUTF8) {
            yajl_tok t = yajl_lex_utf8_char(lexer, jsonText, jsonTextLen,
                                            offset, curChar);

            if (t == yajl_tok_eof) {
                tok = yajl_tok_eof;
                goto finish_string_lex;
            } else if (t == yajl_tok_error) {
                lexer->error = yajl_lex_string_invalid_utf8;
                goto finish_string_lex;
            }
        }
        /* accept it, and move on */
    }
  finish_string_lex:
    /* tell our buddy, the parser, wether he needs to process this string
     * again */
    if (hasEscapes && tok == yajl_tok_string) {
        tok = yajl_tok_string_with_escapes;
    }

    return tok;
}

#define RETURN_IF_EOF if (*offset >= jsonTextLen) return yajl_tok_eof;

static yajl_tok
yajl_lex_number(yajl_lexer lexer, const unsigned char * jsonText,
                unsigned int jsonTextLen, unsigned int * offset)
{
    /** XXX: numbers are the only entities in json that we must lex
     *       _beyond_ in order to know that they are complete.  There
     *       is an ambiguous case for integers at EOF. */

    unsigned char c;

    yajl_tok tok = yajl_tok_integer;

    RETURN_IF_EOF;
    c = readChar(lexer, jsonText, offset);

    /* optional leading minus */
    if (c == '-') {
        RETURN_IF_EOF;
        c = readChar(lexer, jsonText, offset);
    }

    /* a single zero, or a series of integers */
    if (c == '0') {
        RETURN_IF_EOF;
        c = readChar(lexer, jsonText, offset);
    } else if (c >= '1' && c <= '9') {
        do {
            RETURN_IF_EOF;
            c = readChar(lexer, jsonText, offset);
        } while (c >= '0' && c <= '9');
    } else {
        unreadChar(lexer, offset);
        lexer->error = yajl_lex_missing_integer_after_minus;
        return yajl_tok_error;
    }

    /* optional fraction (indicates this is floating point) */
    if (c == '.') {
        int numRd = 0;

        RETURN_IF_EOF;
        c = readChar(lexer, jsonText, offset);

        while (c >= '0' && c <= '9') {
            numRd++;
            RETURN_IF_EOF;
            c = readChar(lexer, jsonText, offset);
        }

        if (!numRd) {
            unreadChar(lexer, offset);
            lexer->error = yajl_lex_missing_integer_after_decimal;
            return yajl_tok_error;
        }
        tok = yajl_tok_double;
    }

    /* optional exponent (indicates this is floating point) */
    if (c == 'e' || c == 'E') {
        RETURN_IF_EOF;
        c = readChar(lexer, jsonText, offset);

        /* optional sign */
        if (c == '+' || c == '-') {
            RETURN_IF_EOF;
            c = readChar(lexer, jsonText, offset);
        }

        if (c >= '0' && c <= '9') {
            do {
                RETURN_IF_EOF;
                c = readChar(lexer, jsonText, offset);
            } while (c >= '0' && c <= '9');
        } else {
            unreadChar(lexer, offset);
            lexer->error = yajl_lex_missing_integer_after_exponent;
            return yajl_tok_error;
        }
        tok = yajl_tok_double;
    }

    /* we always go "one too far" */
    unreadChar(lexer, offset);

    return tok;
}

static yajl_tok
yajl_lex_comment(yajl_lexer lexer, const unsigned char * jsonText,
                 unsigned int jsonTextLen, unsigned int * offset)
{
    unsigned char c;

    yajl_tok tok = yajl_tok_comment;

    RETURN_IF_EOF;
    c = readChar(lexer, jsonText, offset);

    /* either slash or star expected */
    if (c == '/') {
        /* now we throw away until end of line */
        do {
            RETURN_IF_EOF;
            c = readChar(lexer, jsonText, offset);
        } while (c != '\n');
    } else if (c == '*') {
        /* now we throw away until end of comment */
        for (;;) {
            RETURN_IF_EOF;
            c = readChar(lexer, jsonText, offset);
            if (c == '*') {
                RETURN_IF_EOF;
                c = readChar(lexer, jsonText, offset);
                if (c == '/') {
                    break;
                } else {
                    unreadChar(lexer, offset);
                }
            }
        }
    } else {
        lexer->error = yajl_lex_invalid_char;
        tok = yajl_tok_error;
    }

    return tok;
}

yajl_tok
yajl_lex_lex(yajl_lexer lexer, const unsigned char * jsonText,
             unsigned int jsonTextLen, unsigned int * context,
             const unsigned char ** outBuf, unsigned int * outLen)
{
    yajl_tok tok = yajl_tok_error;
    unsigned char c;
    unsigned int startCtx = *context;

    *outBuf = NULL;
    *outLen = 0;

    for (;;) {
        assert(*context <= jsonTextLen);

        if (*context >= jsonTextLen) {
            tok = yajl_tok_eof;
            goto lexed;
        }

        c = readChar(lexer, jsonText, context);

        switch (c) {
            case '{':
                tok = yajl_tok_left_bracket;
                goto lexed;
            case '}':
                tok = yajl_tok_right_bracket;
                goto lexed;
            case '[':
                tok = yajl_tok_left_brace;
                goto lexed;
            case ']':
                tok = yajl_tok_right_brace;
                goto lexed;
            case ',':
                tok = yajl_tok_comma;
                goto lexed;
            case ':':
                tok = yajl_tok_colon;
                goto lexed;
            case '\t': case '\n': case '\v': case '\f': case '\r': case ' ':
                startCtx++;
                break;
            case 't': {
                const char * want = "rue";
                do {
                    if (*context >= jsonTextLen) {
                        tok = yajl_tok_eof;
                        goto lexed;
                    }
                    c = readChar(lexer, jsonText, context);
                    if (c != *want) {
                        unreadChar(lexer, context);
                        lexer->error = yajl_lex_invalid_string;
                        tok = yajl_tok_error;
                        goto lexed;
                    }
                } while (*(++want));
                tok = yajl_tok_bool;
                goto lexed;
            }
            case 'f': {
                const char * want = "alse";
                do {
                    if (*context >= jsonTextLen) {
                        tok = yajl_tok_eof;
                        goto lexed;
                    }
                    c = readChar(lexer, jsonText, context);
                    if (c != *want) {
                        unreadChar(lexer, context);
                        lexer->error = yajl_lex_invalid_string;
                        tok = yajl_tok_error;
                        goto lexed;
                    }
                } while (*(++want));
                tok = yajl_tok_bool;
                goto lexed;
            }
            case 'n': {
                const char * want = "ull";
                do {
                    if (*context >= jsonTextLen) {
                        tok = yajl_tok_eof;
                        goto lexed;
                    }
                    c = readChar(lexer, jsonText, context);
                    if (c != *want) {
                        unreadChar(lexer, context);
                        lexer->error = yajl_lex_invalid_string;
                        tok = yajl_tok_error;
                        goto lexed;
                    }
                } while (*(++want));
                tok = yajl_tok_null;
                goto lexed;
            }
            case '"': {
                tok = yajl_lex_string(lexer, (unsigned char *) jsonText,
                                      jsonTextLen, context);
                goto lexed;
            }
            case '-':
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9': {
                /* integer parsing wants to start from the beginning */
                unreadChar(lexer, context);
                tok = yajl_lex_number(lexer, (unsigned char *) jsonText,
                                      jsonTextLen, context);
                goto lexed;
            }
            case '/':
                /* hey, look, a probable comment!  If comments are disabled
                 * it's an error. */
                if (!lexer->allowComments) {
                    unreadChar(lexer, context);
                    lexer->error = yajl_lex_unallowed_comment;
                    tok = yajl_tok_error;
                    goto lexed;
                }
                /* if comments are enabled, then we should try to lex
                 * the thing.  possible outcomes are
                 * - successful lex (tok_comment, which means continue),
                 * - malformed comment opening (slash not followed by
                 *   '*' or '/') (tok_error)
                 * - eof hit. (tok_eof) */
                tok = yajl_lex_comment(lexer, (unsigned char *) jsonText,
                                       jsonTextLen, context);
                if (tok == yajl_tok_comment) {
                    /* "error" is silly, but that's the initial
                     * state of tok.  guilty until proven innocent. */
                    tok = yajl_tok_error;
                    yajl_buf_clear(lexer->buf);
                    lexer->bufInUse = 0;
                    startCtx = *context;
                    break;
                }
                /* hit error or eof, bail */
                goto lexed;
            default:
                lexer->error = yajl_lex_invalid_char;
                tok = yajl_tok_error;
                goto lexed;
        }
    }


  lexed:
    /* need to append to buffer if the buffer is in use or
     * if it's an EOF token */
    if (tok == yajl_tok_eof || lexer->bufInUse) {
        if (!lexer->bufInUse) yajl_buf_clear(lexer->buf);
        lexer->bufInUse = 1;
        yajl_buf_append(lexer->buf, jsonText + startCtx, *context - startCtx);
        lexer->bufOff = 0;

        if (tok != yajl_tok_eof) {
            *outBuf = yajl_buf_data(lexer->buf);
            *outLen = yajl_buf_len(lexer->buf);
            lexer->bufInUse = 0;
        }
    } else if (tok != yajl_tok_error) {
        *outBuf = jsonText + startCtx;
        *outLen = *context - startCtx;
    }

    /* special case for strings. skip the quotes. */
    if (tok == yajl_tok_string || tok == yajl_tok_string_with_escapes)
    {
        assert(*outLen >= 2);
        (*outBuf)++;
        *outLen -= 2;
    }


#ifdef YAJL_LEXER_DEBUG
    if (tok == yajl_tok_error) {
        printf("lexical error: %s\n",
               yajl_lex_error_to_string(yajl_lex_get_error(lexer)));
    } else if (tok == yajl_tok_eof) {
        printf("EOF hit\n");
    } else {
        printf("lexed %s: '", tokToStr(tok));
        fwrite(*outBuf, 1, *outLen, stdout);
        printf("'\n");
    }
#endif

    return tok;
}

const char *
yajl_lex_error_to_string(yajl_lex_error error)
{
    switch (error) {
        case yajl_lex_e_ok:
            return "ok, no error";
        case yajl_lex_string_invalid_utf8:
            return "invalid bytes in UTF8 string.";
        case yajl_lex_string_invalid_escaped_char:
            return "inside a string, '\\' occurs before a character "
                   "which it may not.";
        case yajl_lex_string_invalid_json_char:
            return "invalid character inside string.";
        case yajl_lex_string_invalid_hex_char:
            return "invalid (non-hex) character occurs after '\\u' inside "
                   "string.";
        case yajl_lex_invalid_char:
            return "invalid char in json text.";
        case yajl_lex_invalid_string:
            return "invalid string in json text.";
        case yajl_lex_missing_integer_after_exponent:
            return "malformed number, a digit is required after the exponent.";
        case yajl_lex_missing_integer_after_decimal:
            return "malformed number, a digit is required after the "
                   "decimal point.";
        case yajl_lex_missing_integer_after_minus:
            return "malformed number, a digit is required after the "
                   "minus sign.";
        case yajl_lex_unallowed_comment:
            return "probable comment found in input text, comments are "
                   "not enabled.";
    }
    return "unknown error code";
}


/** allows access to more specific information about the lexical
 *  error when yajl_lex_lex returns yajl_tok_error. */
yajl_lex_error
yajl_lex_get_error(yajl_lexer lexer)
{
    if (lexer == NULL) return (yajl_lex_error) -1;
    return lexer->error;
}

unsigned int yajl_lex_current_line(yajl_lexer lexer)
{
    return lexer->lineOff;
}

unsigned int yajl_lex_current_char(yajl_lexer lexer)
{
    return lexer->charOff;
}

yajl_tok yajl_lex_peek(yajl_lexer lexer, const unsigned char * jsonText,
                       unsigned int jsonTextLen, unsigned int offset)
{
    const unsigned char * outBuf;
    unsigned int outLen;
    unsigned int bufLen = yajl_buf_len(lexer->buf);
    unsigned int bufOff = lexer->bufOff;
    unsigned int bufInUse = lexer->bufInUse;
    yajl_tok tok;

    tok = yajl_lex_lex(lexer, jsonText, jsonTextLen, &offset,
                       &outBuf, &outLen);

    lexer->bufOff = bufOff;
    lexer->bufInUse = bufInUse;
    yajl_buf_truncate(lexer->buf, bufLen);

    return tok;
}


/* Source: yajl_parser.c */
unsigned char *
yajl_render_error_string(yajl_handle hand, const unsigned char * jsonText,
                         unsigned int jsonTextLen, int verbose)
{
    unsigned int offset = hand->errorOffset;
    unsigned char * str;
    const char * errorType = NULL;
    const char * errorText = NULL;
    char text[72];
    const char * arrow = "                     (right here) ------^\n";

    if (yajl_state_current(hand) == yajl_state_parse_error) {
        errorType = "parse";
        errorText = hand->parseError;
    } else if (yajl_state_current(hand) == yajl_state_lexical_error) {
        errorType = "lexical";
        errorText = yajl_lex_error_to_string(yajl_lex_get_error(hand->lexer));
    } else {
        errorType = "unknown";
    }

    {
        unsigned int memneeded = 0;
        memneeded += strlen(errorType);
        memneeded += strlen(" error");
        if (errorText != NULL) {
            memneeded += strlen(": ");
            memneeded += strlen(errorText);
        }
        str = (unsigned char *) malloc(memneeded + 2);
        str[0] = 0;
        strcat((char *) str, errorType);
        strcat((char *) str, " error");
        if (errorText != NULL) {
            strcat((char *) str, ": ");
            strcat((char *) str, errorText);
        }
        strcat((char *) str, "\n");
    }

    /* now we append as many spaces as needed to make sure the error
     * falls at char 41, if verbose was specified */
    if (verbose) {
        unsigned int start, end, i;
        unsigned int spacesNeeded;

        spacesNeeded = (offset < 30 ? 40 - offset : 10);
        start = (offset >= 30 ? offset - 30 : 0);
        end = (offset + 30 > jsonTextLen ? jsonTextLen : offset + 30);

        for (i=0;i<spacesNeeded;i++) text[i] = ' ';

        for (;start < end;start++, i++) {
            if (jsonText[start] != '\n' && jsonText[start] != '\r')
            {
                text[i] = jsonText[start];
            }
            else
            {
                text[i] = ' ';
            }
        }
        assert(i <= 71);
        text[i++] = '\n';
        text[i] = 0;
        {
            char * newStr = (char *) malloc(strlen((char *) str) +
                                            strlen((char *) text) +
                                            strlen(arrow) + 1);
            newStr[0] = 0;
            strcat((char *) newStr, (char *) str);
            strcat((char *) newStr, text);
            strcat((char *) newStr, arrow);
            free(str);
            str = (unsigned char *) newStr;
        }
    }
    return str;
}

/* check for client cancelation */
#define _CC_CHK(x)                                                \
    if (!(x)) {                                                   \
        yajl_state_set(hand, yajl_state_parse_error);             \
        hand->parseError =                                        \
            "client cancelled parse via callback return value";   \
        return yajl_status_client_canceled;                       \
    }


yajl_status
yajl_do_parse(yajl_handle hand, unsigned int * offset,
              const unsigned char * jsonText, unsigned int jsonTextLen)
{
    yajl_tok tok;
    const unsigned char * buf;
    unsigned int bufLen;

  around_again:
    switch (yajl_state_current(hand)) {
        case yajl_state_parse_complete:
            return yajl_status_ok;
        case yajl_state_lexical_error:
        case yajl_state_parse_error:
            hand->errorOffset = *offset;
            return yajl_status_error;
        case yajl_state_start:
        case yajl_state_map_need_val:
        case yajl_state_array_need_val:
        case yajl_state_array_start: {
            /* for arrays and maps, we advance the state for this
             * depth, then push the state of the next depth.
             * If an error occurs during the parsing of the nesting
             * enitity, the state at this level will not matter.
             * a state that needs pushing will be anything other
             * than state_start */
            yajl_state stateToPush = yajl_state_start;

            tok = yajl_lex_lex(hand->lexer, jsonText, jsonTextLen,
                               offset, &buf, &bufLen);

            switch (tok) {
                case yajl_tok_eof:
                    return yajl_status_insufficient_data;
                case yajl_tok_error:
                    yajl_state_set(hand, yajl_state_lexical_error);
                    goto around_again;
                case yajl_tok_string:
                    if (hand->callbacks && hand->callbacks->yajl_string) {
                        _CC_CHK(hand->callbacks->yajl_string(hand->ctx,
                                                             buf, bufLen));
                    }
                    break;
                case yajl_tok_string_with_escapes:
                    if (hand->callbacks && hand->callbacks->yajl_string) {
                        yajl_buf_clear(hand->decodeBuf);
                        yajl_string_decode(hand->decodeBuf, buf, bufLen);
                        _CC_CHK(hand->callbacks->yajl_string(
                                    hand->ctx, yajl_buf_data(hand->decodeBuf),
                                    yajl_buf_len(hand->decodeBuf)));
                    }
                    break;
                case yajl_tok_bool:
                    if (hand->callbacks && hand->callbacks->yajl_boolean) {
                        _CC_CHK(hand->callbacks->yajl_boolean(hand->ctx,
                                                              *buf == 't'));
                    }
                    break;
                case yajl_tok_null:
                    if (hand->callbacks && hand->callbacks->yajl_null) {
                        _CC_CHK(hand->callbacks->yajl_null(hand->ctx));
                    }
                    break;
                case yajl_tok_left_bracket:
                    if (hand->callbacks && hand->callbacks->yajl_start_map) {
                        _CC_CHK(hand->callbacks->yajl_start_map(hand->ctx));
                    }
                    stateToPush = yajl_state_map_start;
                    break;
                case yajl_tok_left_brace:
                    if (hand->callbacks && hand->callbacks->yajl_start_array) {
                        _CC_CHK(hand->callbacks->yajl_start_array(hand->ctx));
                    }
                    stateToPush = yajl_state_array_start;
                    break;
                case yajl_tok_integer:
                    /*
                     * note.  strtol does not respect the length of
                     * the lexical token.  in a corner case where the
                     * lexed number is a integer with a trailing zero,
                     * immediately followed by the end of buffer,
                     * sscanf could run off into oblivion and cause a
                     * crash.  for this reason we copy the integer
                     * (and doubles), into our parse buffer (the same
                     * one used for unescaping strings), before
                     * calling strtol.  yajl_buf ensures null padding,
                     * so we're safe.
                     */
                    if (hand->callbacks) {
                        if (hand->callbacks->yajl_number) {
                            _CC_CHK(hand->callbacks->yajl_number(hand->ctx,
                                                                 (char *) buf,
                                                                 bufLen));
                        } else if (hand->callbacks->yajl_integer) {
                            long int i = 0;
                            yajl_buf_clear(hand->decodeBuf);
                            yajl_buf_append(hand->decodeBuf, buf, bufLen);
                            buf = yajl_buf_data(hand->decodeBuf);
                            i = strtol((char *) buf, NULL, 10);
                            if ((i == LONG_MIN || i == LONG_MAX) &&
                                errno == ERANGE)
                            {
                                yajl_state_set(hand, yajl_state_parse_error);
                                hand->parseError = "integer overflow" ;
                                /* try to restore error offset */
                                if (*offset >= bufLen) *offset -= bufLen;
                                else *offset = 0;
                                goto around_again;
                            }
                            _CC_CHK(hand->callbacks->yajl_integer(hand->ctx,
                                                                  i));
                        }
                    }
                    break;
                case yajl_tok_double:
                    if (hand->callbacks) {
                        if (hand->callbacks->yajl_number) {
                            _CC_CHK(hand->callbacks->yajl_number(hand->ctx,
                                                                 (char *) buf,
                                                                 bufLen));
                        } else if (hand->callbacks->yajl_double) {
                            double d = 0.0;
                            yajl_buf_clear(hand->decodeBuf);
                            yajl_buf_append(hand->decodeBuf, buf, bufLen);
                            buf = yajl_buf_data(hand->decodeBuf);
                            d = strtod((char *) buf, NULL);
                            if ((d == HUGE_VAL || d == -HUGE_VAL) &&
                                errno == ERANGE)
                            {
                                yajl_state_set(hand, yajl_state_parse_error);
                                hand->parseError = "numeric (floating point) "
                                    "overflow";
                                /* try to restore error offset */
                                if (*offset >= bufLen) *offset -= bufLen;
                                else *offset = 0;
                                goto around_again;
                            }
                            _CC_CHK(hand->callbacks->yajl_double(hand->ctx,
                                                                 d));
                        }
                    }
                    break;
                case yajl_tok_right_brace: {
                    if (yajl_state_current(hand) == yajl_state_array_start) {
                        if (hand->callbacks &&
                            hand->callbacks->yajl_end_array)
                        {
                            _CC_CHK(hand->callbacks->yajl_end_array(hand->ctx));
                        }
                        (void) yajl_state_pop(hand);
                        goto around_again;
                    }
                    /* intentional fall-through */
                }
                case yajl_tok_colon:
                case yajl_tok_comma:
                case yajl_tok_right_bracket:
                    yajl_state_set(hand, yajl_state_parse_error);
                    hand->parseError =
                        "unallowed token at this point in JSON text";
                    goto around_again;
                default:
                    yajl_state_set(hand, yajl_state_parse_error);
                    hand->parseError = "invalid token, internal error";
                    goto around_again;
            }
            /* got a value.  transition depends on the state we're in. */
            {
                yajl_state s = yajl_state_current(hand);
                if (s == yajl_state_start) {
                    yajl_state_set(hand, yajl_state_parse_complete);
                } else if (s == yajl_state_map_need_val) {
                    yajl_state_set(hand, yajl_state_map_got_val);
                } else {
                    yajl_state_set(hand, yajl_state_array_got_val);
                }
            }
            if (stateToPush != yajl_state_start) {
                yajl_state_push(hand, stateToPush);
            }

            goto around_again;
        }
        case yajl_state_map_start:
        case yajl_state_map_need_key: {
            /* only difference between these two states is that in
             * start '}' is valid, whereas in need_key, we've parsed
             * a comma, and a string key _must_ follow */
            tok = yajl_lex_lex(hand->lexer, jsonText, jsonTextLen,
                               offset, &buf, &bufLen);
            switch (tok) {
                case yajl_tok_eof:
                    return yajl_status_insufficient_data;
                case yajl_tok_error:
                    yajl_state_set(hand, yajl_state_lexical_error);
                    goto around_again;
                case yajl_tok_string_with_escapes:
                    if (hand->callbacks && hand->callbacks->yajl_map_key) {
                        yajl_buf_clear(hand->decodeBuf);
                        yajl_string_decode(hand->decodeBuf, buf, bufLen);
                        buf = yajl_buf_data(hand->decodeBuf);
                        bufLen = yajl_buf_len(hand->decodeBuf);
                    }
                    /* intentional fall-through */
                case yajl_tok_string:
                    if (hand->callbacks && hand->callbacks->yajl_map_key) {
                        _CC_CHK(hand->callbacks->yajl_map_key(hand->ctx, buf,
                                                              bufLen));
                    }
                    yajl_state_set(hand, yajl_state_map_sep);
                    goto around_again;
                case yajl_tok_right_bracket:
                    if (yajl_state_current(hand) == yajl_state_map_start) {
                        if (hand->callbacks && hand->callbacks->yajl_end_map) {
                            _CC_CHK(hand->callbacks->yajl_end_map(hand->ctx));
                        }
                        (void) yajl_state_pop(hand);
                        goto around_again;
                    }
                default:
                    yajl_state_set(hand, yajl_state_parse_error);
                    hand->parseError =
                        "invalid object key (must be a string)";
                    goto around_again;
            }
        }
        case yajl_state_map_sep: {
            tok = yajl_lex_lex(hand->lexer, jsonText, jsonTextLen,
                               offset, &buf, &bufLen);
            switch (tok) {
                case yajl_tok_colon:
                    yajl_state_set(hand, yajl_state_map_need_val);
                    goto around_again;
                case yajl_tok_eof:
                    return yajl_status_insufficient_data;
                case yajl_tok_error:
                    yajl_state_set(hand, yajl_state_lexical_error);
                    goto around_again;
                default:
                    yajl_state_set(hand, yajl_state_parse_error);
                    hand->parseError = "object key and value must "
                        "be separated by a colon (':')";
                    goto around_again;
            }
        }
        case yajl_state_map_got_val: {
            tok = yajl_lex_lex(hand->lexer, jsonText, jsonTextLen,
                               offset, &buf, &bufLen);
            switch (tok) {
                case yajl_tok_right_bracket:
                    if (hand->callbacks && hand->callbacks->yajl_end_map) {
                        _CC_CHK(hand->callbacks->yajl_end_map(hand->ctx));
                    }
                    (void) yajl_state_pop(hand);
                    goto around_again;
                case yajl_tok_comma:
                    yajl_state_set(hand, yajl_state_map_need_key);
                    goto around_again;
                case yajl_tok_eof:
                    return yajl_status_insufficient_data;
                case yajl_tok_error:
                    yajl_state_set(hand, yajl_state_lexical_error);
                    goto around_again;
                default:
                    yajl_state_set(hand, yajl_state_parse_error);
                    hand->parseError = "after key and value, inside map, "
                                       "I expect ',' or '}'";
                    /* try to restore error offset */
                    if (*offset >= bufLen) *offset -= bufLen;
                    else *offset = 0;
                    goto around_again;
            }
        }
        case yajl_state_array_got_val: {
            tok = yajl_lex_lex(hand->lexer, jsonText, jsonTextLen,
                               offset, &buf, &bufLen);
            switch (tok) {
                case yajl_tok_right_brace:
                    if (hand->callbacks && hand->callbacks->yajl_end_array) {
                        _CC_CHK(hand->callbacks->yajl_end_array(hand->ctx));
                    }
                    (void) yajl_state_pop(hand);
                    goto around_again;
                case yajl_tok_comma:
                    yajl_state_set(hand, yajl_state_array_need_val);
                    goto around_again;
                case yajl_tok_eof:
                    return yajl_status_insufficient_data;
                case yajl_tok_error:
                    yajl_state_set(hand, yajl_state_lexical_error);
                    goto around_again;
                default:
                    yajl_state_set(hand, yajl_state_parse_error);
                    hand->parseError =
                        "after array element, I expect ',' or ']'";
                    goto around_again;
            }
        }
    }

    abort();
    return yajl_status_error;
}

/* state stack maintenence routines */
yajl_state
yajl_state_current(yajl_handle h)
{
    assert(yajl_buf_len(h->stateBuf) > 0);
    return (yajl_state) *(yajl_buf_data(h->stateBuf) +
                          yajl_buf_len(h->stateBuf) - 1);
}

void yajl_state_push(yajl_handle h, yajl_state s)
{
    unsigned char c = (unsigned char) s;
    yajl_buf_append(h->stateBuf, &c, sizeof(c));
}

yajl_state yajl_state_pop(yajl_handle h)
{
    yajl_state s;
    unsigned int len = yajl_buf_len(h->stateBuf);
    /* start state is never popped */
    assert(len > 1);
    s = (yajl_state) *(yajl_buf_data(h->stateBuf) + len - 1);
    yajl_buf_truncate(h->stateBuf, len - 1);
    return s;
}

unsigned int yajl_parse_depth(yajl_handle h)
{
    assert(yajl_buf_len(h->stateBuf) > 0);
    return (yajl_buf_len(h->stateBuf) - 1);
}

void yajl_state_set(yajl_handle h, yajl_state state)
{
    assert(yajl_buf_len(h->stateBuf) > 0);
    *(unsigned char *) (yajl_buf_data(h->stateBuf) +
                        yajl_buf_len(h->stateBuf) - 1) = (unsigned char) state;
}


// client.cpp
#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#ifndef ECONNABORTED
#define ECONNABORTED WSAECONNABORTED
#endif
#ifndef ETIMEDOUT
#define ETIMEDOUT WSAETIMEDOUT
#endif
#endif


template <class RequestType, class ResponseType>
YIELD::ipc::Client<RequestType, ResponseType>::Client
(
  uint16_t concurrency_level,
  uint32_t flags,
  YIELD::platform::auto_Log log,
  const YIELD::platform::Time& operation_timeout,
  auto_SocketAddress peername,
  uint8_t reconnect_tries_max,
  auto_SocketFactory socket_factory
)
  : concurrency_level( concurrency_level ),
    flags( flags ),
    log( log ),
    operation_timeout( operation_timeout ),
    peername( peername ),
    reconnect_tries_max( reconnect_tries_max ),
    socket_factory( socket_factory )
{
  for ( uint16_t socket_i = 0; socket_i < concurrency_level; socket_i++ )
    sockets.enqueue( NULL ); // Enqueue a placeholder for each socket;
                             // the sockets will be created on demand
}

template <class RequestType, class ResponseType>
YIELD::ipc::Client<RequestType, ResponseType>::~Client()
{
  for ( uint16_t socket_i = 0; socket_i < concurrency_level; socket_i++ )
  {
    Socket* socket_ = sockets.try_dequeue();
    if ( socket_ != NULL )
    {
      socket_->shutdown();
      socket_->close();
      Socket::decRef( *socket_ );
      socket_ = sockets.try_dequeue();
    }
    else
      break;
  }
}

template <class RequestType, class ResponseType>
void YIELD::ipc::Client<RequestType, ResponseType>::handleEvent
(
  YIELD::concurrency::Event& ev
)
{
  switch ( ev.get_type_id() )
  {
    case YIDL_RUNTIME_OBJECT_TYPE_ID( RequestType ):
    {
      RequestType& request = static_cast<RequestType&>( ev );

      if
      (
        ( this->flags & this->CLIENT_FLAG_TRACE_OPERATIONS ) ==
           this->CLIENT_FLAG_TRACE_OPERATIONS
        &&
        log != NULL
      )
      {
        log->getStream( YIELD::platform::Log::LOG_INFO ) <<
        "yield::ipc::Client sending " << request.get_type_name() << "/" <<
        reinterpret_cast<uint64_t>( &request ) << " to <host>:" <<
        this->peername->get_port() << ".";
      }

      for ( ;; )
      {
        Socket* socket_ = sockets.dequeue(); // Blocking dequeue
        if ( socket_ == NULL ) // We dequeued a placeholder;
                               // try to create the socket on demand
        {
          socket_ = socket_factory->createSocket().release();
          if ( socket_ != NULL )
          {
            if
            (
              ( this->flags & this->CLIENT_FLAG_TRACE_NETWORK_IO ) ==
                 this->CLIENT_FLAG_TRACE_NETWORK_IO
               &&
               log != NULL
               &&
               log->get_level() >= YIELD::platform::Log::LOG_INFO
            )
            {
              socket_ = new TracingSocket( socket_, log );
            }
          }
          else
          {
            if ( log != NULL )
            {
              log->getStream( YIELD::platform::Log::LOG_ERR ) <<
              "yield::ipc::Client: could not create new socket " <<
              "to connect to <host>:"
              << this->peername->get_port() <<
              ", error: " << YIELD::platform::Exception() << ".";
            }

            continue; // Try to dequeue another existing socket
          }
        }

        if ( socket_->is_connected() )
        {
          if
          (
            ( this->flags & this->CLIENT_FLAG_TRACE_OPERATIONS ) ==
              this->CLIENT_FLAG_TRACE_OPERATIONS
            &&
            log != NULL
          )
          {
            log->getStream( YIELD::platform::Log::LOG_INFO ) <<
            "yield::ipc::Client: writing " << request.get_type_name()
            << "/" << reinterpret_cast<uint64_t>( &request ) <<
            " to <host>:" << this->peername->get_port() <<
            " on socket #" << static_cast<uint64_t>( *socket_ ) << ".";
          }

          AIOWriteControlBlock* aio_write_control_block =
            new AIOWriteControlBlock( request.serialize(), *this, request );

          YIELD::platform::TimerQueue::getDefaultTimerQueue().
            addTimer
            (
              new OperationTimer
              (
                aio_write_control_block->incRef(),
                operation_timeout
              )
            );

          socket_->aio_write( aio_write_control_block );
        }
        else
        {
          if
          (
            ( this->flags & this->CLIENT_FLAG_TRACE_OPERATIONS ) ==
              this->CLIENT_FLAG_TRACE_OPERATIONS
            &&
            log != NULL
          )
          {
            log->getStream( YIELD::platform::Log::LOG_INFO ) <<
            "yield::ipc::Client: connecting to <host>:" <<
            this->peername->get_port() <<
            " with socket #" << static_cast<uint64_t>( *socket_ ) <<
            " (try #" <<
            static_cast<uint16_t>( request.get_reconnect_tries() + 1 ) <<
            ").";
          }

          AIOConnectControlBlock* aio_connect_control_block =
            new AIOConnectControlBlock( *this, request );

          YIELD::platform::TimerQueue::getDefaultTimerQueue().
            addTimer
            (
              new OperationTimer
              (
                aio_connect_control_block->incRef(),
                operation_timeout
              )
            );

          socket_->aio_connect( aio_connect_control_block );
        }

        Socket::decRef( *socket_ );

        break;
      }
    }
    break;

    default:
    {
      YIELD::concurrency::Event::decRef( ev );
    }
    break;
  }
}


template <class RequestType, class ResponseType>
class YIELD::ipc::Client<RequestType, ResponseType>::AIOConnectControlBlock
  : public Socket::AIOConnectControlBlock
{
public:
  AIOConnectControlBlock
  (
    Client<RequestType,ResponseType>& client,
    yidl::runtime::auto_Object<RequestType> request
  )
    : Socket::AIOConnectControlBlock( client.peername ),
      client( client ),
      request( request )
  { }

  AIOConnectControlBlock& operator=( const AIOConnectControlBlock& )
  {
    return *this;
  }

  // AIOControlBlock
  void onCompletion( size_t )
  {
    if ( request_lock.try_acquire() )
    {
      if
      (
        ( client.flags & client.CLIENT_FLAG_TRACE_OPERATIONS ) ==
          client.CLIENT_FLAG_TRACE_OPERATIONS
        &&
        client.log != NULL
      )
      {
        client.log->getStream( YIELD::platform::Log::LOG_INFO ) <<
        "yield::ipc::Client: successfully connected to <host>:" <<
        client.peername->get_port() << " on socket #" <<
        static_cast<uint64_t>( *get_socket() ) << ".";
      }

      AIOWriteControlBlock* aio_write_control_block =
        new AIOWriteControlBlock( request->serialize(), client, request );

      YIELD::platform::TimerQueue::getDefaultTimerQueue().
        addTimer
        (
          new OperationTimer
          (
            aio_write_control_block->incRef(),
            client.operation_timeout
          )
        );

      get_socket()->aio_write( aio_write_control_block );

      request = NULL;
    }
  }

  void onError( uint32_t error_code )
  {
    if ( request_lock.try_acquire() )
    {
      if ( client.log != NULL )
      {
        client.log->getStream( YIELD::platform::Log::LOG_ERR ) <<
        "yield::ipc::Client: connect() to <host>:" <<
        client.peername->get_port() <<
        " failed: " <<
        YIELD::platform::Exception( error_code ) << ".";
      }

      get_socket()->close();
      if ( get_socket()->recreate() )
        client.sockets.enqueue( get_socket().release() );
      else
        client.sockets.enqueue( NULL );

      if ( request->get_reconnect_tries() < client.reconnect_tries_max )
      {
        request->set_reconnect_tries( request->get_reconnect_tries() + 1 );
        client.handleEvent( *request.release() );
      }
      else
        request->respond
        (
          *( new YIELD::concurrency::ExceptionResponse( error_code ) )
        );

      request = NULL;
    }
  }

private:
  Client<RequestType,ResponseType>& client;
  yidl::runtime::auto_Object<RequestType> request;
  YIELD::platform::Mutex request_lock;
};


template <class RequestType, class ResponseType>
class YIELD::ipc::Client<RequestType, ResponseType>::AIOReadControlBlock
  : public Socket::AIOReadControlBlock
{
public:
  AIOReadControlBlock
  (
    yidl::runtime::auto_Buffer buffer,
    Client<RequestType,ResponseType>& client,
    yidl::runtime::auto_Object<RequestType> request,
    yidl::runtime::auto_Object<ResponseType> response
  )
    : Socket::AIOReadControlBlock( buffer ),
      client( client ),
      request( request ), response( response )
  { }

  AIOReadControlBlock& operator=( const AIOReadControlBlock& )
  {
    return *this;
  }

  // AIOControlBlock
  void onCompletion( size_t bytes_transferred )
  {
#ifdef _DEBUG
    if ( bytes_transferred <= 0 )
      DebugBreak();
#endif

    if ( request_lock.try_acquire() )
    {
      if
      (
        ( client.flags & client.CLIENT_FLAG_TRACE_OPERATIONS ) ==
          client.CLIENT_FLAG_TRACE_OPERATIONS
        &&
        client.log != NULL
      )
      {
        client.log->getStream( YIELD::platform::Log::LOG_INFO ) <<
        "yield::ipc::Client: read " << bytes_transferred <<
        " bytes from socket #" << static_cast<uint64_t>( *get_socket() ) <<
        " for " << response->get_type_name() << "/" <<
        reinterpret_cast<uint64_t>( response.get() ) << ".";
      }

      ssize_t deserialize_ret = response->deserialize( get_buffer() );

      if ( deserialize_ret == 0 )
      {
        if
        (
          ( client.flags & client.CLIENT_FLAG_TRACE_OPERATIONS ) ==
            client.CLIENT_FLAG_TRACE_OPERATIONS
          &&
          client.log != NULL
        )
        {
          client.log->getStream( YIELD::platform::Log::LOG_INFO ) <<
          "yield::ipc::Client: successfully deserialized " <<
          response->get_type_name() <<
            "/" << reinterpret_cast<uint64_t>( response.get() ) <<
          ", responding to " << request->get_type_name() <<
            "/" << reinterpret_cast<uint64_t>( request.get() ) << ".";
        }

        request->respond( *response.release() );
        client.sockets.enqueue( get_socket().release() );
      }
      else if ( deserialize_ret > 0 )
      {
        yidl::runtime::auto_Buffer buffer( get_buffer() );
        if ( buffer->capacity() - buffer->size() <
             static_cast<size_t>( deserialize_ret ) )
          buffer = new yidl::runtime::HeapBuffer( deserialize_ret );
        // else re-use the same buffer

        if
        (
          ( client.flags & client.CLIENT_FLAG_TRACE_OPERATIONS ) ==
            client.CLIENT_FLAG_TRACE_OPERATIONS
          &&
          client.log != NULL
        )
        {
          client.log->getStream( YIELD::platform::Log::LOG_INFO ) <<
          "yield::ipc::Client: partially deserialized " <<
          response->get_type_name() << "/" <<
          reinterpret_cast<uint64_t>( response.get() ) <<
          ", reading again with " << ( buffer->capacity() - buffer->size() ) <<
          " byte buffer.";
        }

        AIOReadControlBlock* aio_read_control_block =
          new AIOReadControlBlock
          (
            buffer,
            client,
            request.release(),
            response.release()
          );

        YIELD::platform::TimerQueue::getDefaultTimerQueue().addTimer
        (
          new OperationTimer
          (
            aio_read_control_block->incRef(),
            client.operation_timeout
          )
        );

        get_socket()->aio_read( aio_read_control_block );
      }
      else
      {
        if ( client.log != NULL )
        {
          client.log->getStream( YIELD::platform::Log::LOG_ERR ) <<
          "yield::ipc::Client: lost connection trying " <<
            response->get_type_name() <<
            "/" << reinterpret_cast<uint64_t>( response.get() ) <<
          ", responding to " << request->get_type_name() <<
            "/" << reinterpret_cast<uint64_t>( request.get() ) <<
          " with ExceptionResponse.";
        }

        request->respond
        (
          *( new YIELD::concurrency::ExceptionResponse( ECONNABORTED ) )
        );

        get_socket()->shutdown();
        get_socket()->close();
        client.sockets.enqueue( get_socket().release() );
      }

      // Clear references so their objects will be deleted now instead of when
      // the timeout occurs (the timeout has the last reference to this control block).
      request = NULL;
      response = NULL;
      unlink_buffer();
    }
  }

  void onError( uint32_t error_code )
  {
    if ( request_lock.try_acquire() )
    {
      if ( client.log != NULL )
      {
        client.log->getStream( YIELD::platform::Log::LOG_ERR ) <<
        "yield::ipc::Client: error reading " << response->get_type_name() <<
          "/" << reinterpret_cast<uint64_t>( response.get() ) <<
        " from socket #" << static_cast<uint64_t>( *get_socket() ) <<
        ", error='" << YIELD::platform::Exception( error_code ) <<
        "', responding to " << request->get_type_name() <<
          "/" << reinterpret_cast<uint64_t>( request.get() ) <<
          " with ExceptionResponse.";
      }

      get_socket()->shutdown();
      get_socket()->close();
      if ( get_socket()->recreate() )
        client.sockets.enqueue( get_socket().release() );
      else
        client.sockets.enqueue( NULL );

      if ( request->get_reconnect_tries() < client.reconnect_tries_max )
      {
        // Hack: if the read timed out, increase the timeout for the next try
        if ( error_code == ETIMEDOUT )
          client.operation_timeout *= 2.0;

        request->set_reconnect_tries( request->get_reconnect_tries() + 1 );
        client.handleEvent( *request.release() );
      }
      else
      {
        request->respond
        (
          *( new YIELD::concurrency::ExceptionResponse( error_code ) )
        );
        request = NULL;
      }

      unlink_buffer();
    }
  }

private:
  Client<RequestType,ResponseType>& client;
  yidl::runtime::auto_Object<RequestType> request;
  YIELD::platform::Mutex request_lock;
  yidl::runtime::auto_Object<ResponseType> response;
};


template <class RequestType, class ResponseType>
class YIELD::ipc::Client<RequestType, ResponseType>::AIOWriteControlBlock
  : public Socket::AIOWriteControlBlock
{
public:
  AIOWriteControlBlock
  (
    yidl::runtime::auto_Buffer buffer,
    Client<RequestType,ResponseType>& client,
    yidl::runtime::auto_Object<RequestType> request
  )
    : Socket::AIOWriteControlBlock( buffer ),
      client( client ),
      request( request )
  { }

  AIOWriteControlBlock& operator=( const AIOWriteControlBlock& )
  {
    return *this;
  }

  // AIOControlBlock
  void onCompletion( size_t bytes_transferred )
  {
    if ( request_lock.try_acquire() )
    {
      if
      (
        ( client.flags & client.CLIENT_FLAG_TRACE_OPERATIONS ) ==
           client.CLIENT_FLAG_TRACE_OPERATIONS
        &&
        client.log != NULL
      )
      {
        client.log->getStream( YIELD::platform::Log::LOG_INFO ) <<
        "yield::ipc::Client: wrote " << bytes_transferred <<
        " bytes to socket #" << static_cast<uint64_t>( *get_socket() ) <<
        " for " << request->get_type_name() <<
          "/" << reinterpret_cast<uint64_t>( request.get() ) << ".";
      }


      yidl::runtime::auto_Object<ResponseType>
        response
        (
          static_cast<ResponseType*>( request->createResponse().release() )
        );

      if
      (
        ( client.flags & client.CLIENT_FLAG_TRACE_OPERATIONS ) ==
           client.CLIENT_FLAG_TRACE_OPERATIONS
        &&
        client.log != NULL
      )
      {
        client.log->getStream( YIELD::platform::Log::LOG_INFO ) <<
        "yield::ipc::Client: created " << response->get_type_name() <<
          "/" << reinterpret_cast<uint64_t>( response.get() ) <<
        " to " << request->get_type_name() <<
          "/" << reinterpret_cast<uint64_t>( request.get() ) << ".";
      }


      AIOReadControlBlock* aio_read_control_block =
        new AIOReadControlBlock
        (
          new yidl::runtime::HeapBuffer( 1024 ), client, request, response
        );

      YIELD::platform::TimerQueue::getDefaultTimerQueue().addTimer
      (
        new OperationTimer
        (
          aio_read_control_block->incRef(),
          client.operation_timeout
        )
      );

      get_socket()->aio_read( aio_read_control_block );


      request = NULL;
      unlink_buffer();
    }
  }

  void onError( uint32_t error_code )
  {
    if ( request_lock.try_acquire() )
    {
      if ( client.log != NULL )
      {
        client.log->getStream( YIELD::platform::Log::LOG_ERR ) <<
        "yield::ipc::Client: error writing " <<
        request->get_type_name() <<
          "/" << reinterpret_cast<uint64_t>( request.get() ) <<
        " to socket #" << static_cast<uint64_t>( *get_socket() ) <<
        ", error='" << YIELD::platform::Exception( error_code ) <<
        "', responding to " << request->get_type_name() <<
          "/" << reinterpret_cast<uint64_t>( request.get() ) <<
          " with ExceptionResponse.";
      }

      get_socket()->shutdown();
      get_socket()->close();
      if ( get_socket()->recreate() )
        client.sockets.enqueue( get_socket().release() );
      else
        client.sockets.enqueue( NULL );

      if ( request->get_reconnect_tries() < client.reconnect_tries_max )
      {
        request->set_reconnect_tries( request->get_reconnect_tries() + 1 );
        client.handleEvent( *request.release() );
      }
      else
      {
        request->respond
        (
          *( new YIELD::concurrency::ExceptionResponse( error_code ) )
        );
        request = NULL;
      }

      unlink_buffer();
    }
  }

private:
  Client<RequestType,ResponseType>& client;
  yidl::runtime::auto_Object<RequestType> request;
  YIELD::platform::Mutex request_lock;
};


template <class RequestType, class ResponseType>
class YIELD::ipc::Client<RequestType, ResponseType>::OperationTimer
  : public YIELD::platform::TimerQueue::Timer
{
public:
  OperationTimer
  (
    YIELD::platform::auto_AIOControlBlock aio_control_block,
    const YIELD::platform::Time& operation_timeout
  )
    : YIELD::platform::TimerQueue::Timer( operation_timeout ),
      aio_control_block( aio_control_block )
  { }

  void fire()
  {
    aio_control_block->onError( ETIMEDOUT );
  }

private:
  YIELD::platform::auto_AIOControlBlock aio_control_block;
};


template class YIELD::ipc::Client<YIELD::ipc::HTTPRequest, YIELD::ipc::HTTPResponse>;
template class YIELD::ipc::Client<YIELD::ipc::ONCRPCRequest, YIELD::ipc::ONCRPCResponse>;


// gather_buffer.cpp
YIELD::ipc::GatherBuffer::GatherBuffer
(
  const struct iovec* iovecs,
  uint32_t iovecs_len
)
  : iovecs( iovecs ), iovecs_len( iovecs_len )
{ }

size_t YIELD::ipc::GatherBuffer::get
(
  void* into_buffer,
  size_t into_buffer_len
)
{
  char* into_buffer_p = static_cast<char*>( into_buffer );
  size_t iovec_offset = position();

  uint32_t iovec_i = 0;
  while ( iovec_offset >= iovecs[iovec_i].iov_len )
  {
    iovec_offset -= iovecs[iovec_i].iov_len;
    if ( ++iovec_i >= iovecs_len )
      return 0;
  }

  for ( ;; )
  {
    if ( iovecs[iovec_i].iov_len - iovec_offset < into_buffer_len )
    {
      // into_buffer_len is larger than the current iovec
      size_t copy_len = iovecs[iovec_i].iov_len - iovec_offset;
      memcpy_s
      (
        into_buffer_p,
        into_buffer_len,
        static_cast<char*>( iovecs[iovec_i].iov_base ) + iovec_offset,
        copy_len
      );
      into_buffer_p += copy_len;
      position( position() + copy_len );
      if ( ++iovec_i < iovecs_len ) // Have more iovecs
      {
        into_buffer_len -= copy_len;
        iovec_offset = 0;
      }
      else
        break;
    }
    else
    {
      // into_buffer_len is smaller than the current iovec
      memcpy_s
      (
        into_buffer_p,
        into_buffer_len,
        static_cast<char*>( iovecs[iovec_i].iov_base ) + iovec_offset,
        into_buffer_len
      );
      into_buffer_p += into_buffer_len;
      position( position() + into_buffer_len );
      break;
    }
  }

  return into_buffer_p - static_cast<char*>( into_buffer );
}

size_t YIELD::ipc::GatherBuffer::size() const
{
  size_t _size = 0;
  for ( uint32_t iovec_i = 0; iovec_i < iovecs_len; iovec_i++ )
    _size += iovecs[iovec_i].iov_len;
  return _size;
}


// http_client.cpp
YIELD::ipc::auto_HTTPClient
  YIELD::ipc::HTTPClient::create
  (
    const URI& absolute_uri,
    uint16_t concurrency_level,
    uint32_t flags,
    YIELD::platform::auto_Log log,
    const YIELD::platform::Time& operation_timeout,
    uint8_t reconnect_tries_max,
    auto_SSLContext ssl_context
  )
{
  URI checked_absolute_uri( absolute_uri );
  if ( checked_absolute_uri.get_port() == 0 )
    checked_absolute_uri.set_port( 80 );

  auto_SocketAddress peername = SocketAddress::create( absolute_uri );

  auto_SocketFactory socket_factory;

#ifdef YIELD_IPC_HAVE_OPENSSL
  if ( absolute_uri.get_scheme() == "https" )
  {
    if ( ssl_context != NULL )
      socket_factory = new SSLSocketFactory( ssl_context );
    else
    {
      ssl_context = SSLContext::create( SSLv23_client_method() );
      if ( ssl_context != NULL )
        socket_factory = new SSLSocketFactory( ssl_context );
      else
        throw YIELD::platform::Exception();
    }
  }
  else
#endif
    socket_factory = new TCPSocketFactory;

  return new HTTPClient
  (
    concurrency_level, flags, log, operation_timeout,
    peername, reconnect_tries_max, socket_factory
  );
}

YIELD::ipc::auto_HTTPResponse YIELD::ipc::HTTPClient::GET
(
  const URI& absolute_uri,
  YIELD::platform::auto_Log log
)
{
  return sendHTTPRequest( "GET", absolute_uri, NULL, log );
}

YIELD::ipc::auto_HTTPResponse YIELD::ipc::HTTPClient::PUT
(
  const URI& absolute_uri,
  yidl::runtime::auto_Buffer body,
  YIELD::platform::auto_Log log
)
{
  return sendHTTPRequest( "PUT", absolute_uri, body, log );
}

YIELD::ipc::auto_HTTPResponse
  YIELD::ipc::HTTPClient::PUT
  (
    const URI& absolute_uri,
    const YIELD::platform::Path& body_file_path,
    YIELD::platform::auto_Log log
  )
{
  YIELD::platform::auto_File
    file( YIELD::platform::Volume().open( body_file_path ) );

  if ( file != NULL )
  {
    size_t file_size = static_cast<size_t>( file->stat()->get_size() );
    yidl::runtime::auto_Object<yidl::runtime::HeapBuffer>
      body( new yidl::runtime::HeapBuffer( file_size ) );
    file->read( *body, file_size );
    return sendHTTPRequest( "PUT", absolute_uri, body.release(), log );
  }
  else
    throw YIELD::platform::Exception();
}

YIELD::ipc::auto_HTTPResponse
  YIELD::ipc::HTTPClient::sendHTTPRequest
  (
    const char* method,
    const URI& absolute_uri,
    yidl::runtime::auto_Buffer body,
    YIELD::platform::auto_Log log
  )
{
  auto_HTTPClient http_client
  (
    HTTPClient::create
    (
      absolute_uri,
      HTTPClient::CONCURRENCY_LEVEL_DEFAULT,
      0,
      log
    )
  );

  auto_HTTPRequest
    http_request( new HTTPRequest( method, absolute_uri, body ) );
  http_request->set_header( "User-Agent", "Flog 0.99" );

  YIELD::concurrency::auto_ResponseQueue<HTTPResponse>
    http_response_queue( new YIELD::concurrency::ResponseQueue<HTTPResponse> );
  http_request->set_response_target( http_response_queue->incRef() );

  http_client->send( http_request->incRef() );

  return http_response_queue->dequeue();
}


// http_message.cpp
YIELD::ipc::HTTPMessage::HTTPMessage( uint8_t reserve_iovecs_count )
  : RFC822Headers( reserve_iovecs_count )
{
  http_version = 1;
}

YIELD::ipc::HTTPMessage::HTTPMessage
(
  uint8_t reserve_iovecs_count,
  yidl::runtime::auto_Buffer body
)
  : RFC822Headers( reserve_iovecs_count ), body( body )
{
  http_version = 1;
}

ssize_t YIELD::ipc::HTTPMessage::deserialize
(
  yidl::runtime::auto_Buffer buffer
)
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_HEADERS:
    {
      ssize_t RFC822Headers_deserialize_ret
        = RFC822Headers::deserialize( buffer );

      if ( RFC822Headers_deserialize_ret == 0 )
      {
        if ( strcmp( get_header( "Transfer-Encoding" ), "chunked" ) == 0 )
        {
          // DebugBreak();
          return -1;
        }
        else
        {
          const char* content_length_str
            = get_header( "Content-Length", NULL ); // Most browsers
          if ( content_length_str == NULL )
            content_length_str = get_header( "Content-length" ); // httperf
          size_t content_length = atoi( content_length_str );

          if ( content_length == 0 )
          {
            deserialize_state = DESERIALIZE_DONE;
            return 0;
          }
          else
          {
            deserialize_state = DESERIALIZING_BODY;

            if ( strcmp( get_header( "Expect" ), "100-continue" ) == 0 )
            {
              // DebugBreak();
              return -1;
            }
            // else fall through
          }
        }
      }
      else
        return RFC822Headers_deserialize_ret;
    }

    case DESERIALIZING_BODY:
    {
      if ( buffer->size() - buffer->position() > 0 )
      {
        if ( body == NULL )
        {
          if ( buffer->position() == 0 )
            body = buffer;
          else
          {
            body
              = new yidl::runtime::StringBuffer
                (
                  static_cast<char*>( *buffer ) + buffer->position(),
                  buffer->size() - buffer->position()
                );
          }
        }
        else if
        (
          body->get_type_id() ==
            YIDL_RUNTIME_OBJECT_TYPE_ID( yidl::runtime::StringBuffer )
        )
        {
          static_cast<yidl::runtime::StringBuffer*>( body.get() )->
            put
            (
              static_cast<char*>( *buffer ) + buffer->position(),
              buffer->size() - buffer->position()
            );
        }
        else
        {
          yidl::runtime::auto_StringBuffer
            concatenated_body( new yidl::runtime::StringBuffer );
          concatenated_body->put( *body, body->size() );
          concatenated_body->put
          (
            static_cast<char*>( *buffer ) + buffer->position(),
            buffer->size() - buffer->position()
          );
          body = concatenated_body.release();
        }

        const char* content_length_str
          = get_header( "Content-Length", NULL ); // Most browsers
        if ( content_length_str == NULL )
          content_length_str = get_header( "Content-length" ); // httperf
        size_t content_length = atoi( content_length_str );
        if ( body->size() >= content_length )
          deserialize_state = DESERIALIZE_DONE;
      }
    }

    case DESERIALIZE_DONE: return 0;

    default: DebugBreak(); return -1;
  }
}

yidl::runtime::auto_Buffer YIELD::ipc::HTTPMessage::serialize()
{
  // Finalize headers
  if ( body != NULL )
  {
    if ( get_header( "Content-Length", NULL ) == NULL )
    {
      char content_length_str[32];
#ifdef _WIN32
      sprintf_s( content_length_str, 32, "%u", body->size() );
#else
      snprintf( content_length_str, 32, "%zu", body->size() );
#endif

      set_header( "Content-Length", content_length_str );
    }

    set_next_iovec( "\r\n", 2 );

    set_next_iovec
    (
      static_cast<const char*>( static_cast<void*>( *body ) ),
      body->size()
    );
  }
  else
    set_next_iovec( "\r\n", 2 );

  return RFC822Headers::serialize();
}


// http_request.cpp
YIELD::ipc::HTTPRequest::HTTPRequest()
  : HTTPMessage( 0 )
{
  method[0] = 0;
  uri = new char[2];
  uri[0] = 0;
  uri_len = 2;
  http_version = 1;
  deserialize_state = DESERIALIZING_METHOD;
}

YIELD::ipc::HTTPRequest::HTTPRequest
(
  const char* method,
  const char* relative_uri,
  const char* host,
  yidl::runtime::auto_Buffer body
)
  : HTTPMessage( 4, body )
{
  init( method, relative_uri, host, body );
}

YIELD::ipc::HTTPRequest::HTTPRequest
(
  const char* method,
  const URI& absolute_uri,
  yidl::runtime::auto_Buffer body
)
  : HTTPMessage( 4, body )
{
  init
  (
    method,
    absolute_uri.get_resource().c_str(),
    absolute_uri.get_host().c_str(),
    body
  );
}

YIELD::concurrency::auto_Response YIELD::ipc::HTTPRequest::createResponse()
{
  return new HTTPResponse;
}

void YIELD::ipc::HTTPRequest::init
(
  const char* method,
  const char* relative_uri,
  const char* host,
  yidl::runtime::auto_Buffer body
)
{
#ifdef _WIN32
  strncpy_s( this->method, 16, method, 16 );
#else
  strncpy( this->method, method, 16 );
#endif
  uri_len = strnlen( relative_uri, UINT16_MAX );
  this->uri = new char[uri_len + 1];
  memcpy_s( this->uri, uri_len + 1, relative_uri, uri_len + 1 );
  http_version = 1;
  set_header( "Host", const_cast<char*>( host ) );
  deserialize_state = DESERIALIZE_DONE;
}

YIELD::ipc::HTTPRequest::~HTTPRequest()
{
  delete [] uri;
}

ssize_t YIELD::ipc::HTTPRequest::deserialize
(
  yidl::runtime::auto_Buffer buffer
)
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_METHOD:
    {
      char* method_p = method + strnlen( method, 16 );
      for ( ;; )
      {
        if ( buffer->get( method_p, 1 ) == 1 )
        {
          if ( *method_p != ' ' )
            method_p++;
          else
          {
            *method_p = 0;
            deserialize_state = DESERIALIZING_URI;
            break;
          }
        }
        else
        {
          *method_p = 0;
          return 1;
        }
      }
      // Fall through
    }

    case DESERIALIZING_URI:
    {
      char* uri_p = uri + strnlen( uri, UINT16_MAX );
      for ( ;; )
      {
        if ( buffer->get( uri_p, 1 ) == 1 )
        {
          if ( *uri_p == ' ' )
          {
            *uri_p = 0;
            uri_len = uri_p - uri;
            deserialize_state = DESERIALIZING_HTTP_VERSION;
            break;
          }
          else
          {
            uri_p++;
            if ( static_cast<size_t>( uri_p - uri ) == uri_len )
            {
              size_t new_uri_len = uri_len * 2;
              char* new_uri = new char[new_uri_len];
              memcpy_s( new_uri, new_uri_len, uri, uri_len );
              delete [] uri;
              uri = new_uri;
              uri_p = uri + uri_len;
              uri_len = new_uri_len;
            }
          }
        }
        else
        {
          *uri_p = 0;
          return 1;
        }
      }
      // Fall through
    }

    case DESERIALIZING_HTTP_VERSION:
    {
      for ( ;; )
      {
        uint8_t test_http_version;
        if ( buffer->get( &test_http_version, 1 ) == 1 )
        {
          if ( test_http_version != '\r' )
          {
            http_version = test_http_version;
            continue;
          }
          else
          {
            http_version = http_version == '1' ? 1 : 0;
            deserialize_state = DESERIALIZING_HEADERS;
            break;
          }
        }
        else
          return 1;
      }
    }
    // Fall through

    default: return HTTPMessage::deserialize( buffer );
  }
}

void YIELD::ipc::HTTPRequest::respond( uint16_t status_code )
{
  respond( *( new HTTPResponse( status_code ) ) );
}

void YIELD::ipc::HTTPRequest::respond
(
  uint16_t status_code,
  yidl::runtime::auto_Buffer body
)
{
  respond( *( new HTTPResponse( status_code, body ) ) );
}

void YIELD::ipc::HTTPRequest::respond( YIELD::concurrency::Response& response )
{
  YIELD::concurrency::Request::respond( response );
}

yidl::runtime::auto_Buffer YIELD::ipc::HTTPRequest::serialize()
{
  RFC822Headers::set_iovec( 0, method, strnlen( method, 16 ) );
  RFC822Headers::set_iovec( 1, " ", 1 );
  RFC822Headers::set_iovec( 2, uri, uri_len );
  RFC822Headers::set_iovec( 3, " HTTP/1.1\r\n", 11 );
  return HTTPMessage::serialize();
}

void YIELD::ipc::HTTPRequest::set_reconnect_tries( uint8_t reconnect_tries )
{
  this->reconnect_tries = reconnect_tries;
}


// http_response.cpp
YIELD::ipc::HTTPResponse::HTTPResponse()
  : HTTPMessage( 0 )
{
  memset( status_code_str, 0, sizeof( status_code_str ) );
  deserialize_state = DESERIALIZING_HTTP_VERSION;
}

YIELD::ipc::HTTPResponse::HTTPResponse( uint16_t status_code )
  : HTTPMessage( 1 ), status_code( status_code )
{
  http_version = 1;
  deserialize_state = DESERIALIZE_DONE;
}

YIELD::ipc::HTTPResponse::HTTPResponse
(
  uint16_t status_code,
  yidl::runtime::auto_Buffer body
)
  : HTTPMessage( 1, body ), status_code( status_code )
{
  deserialize_state = DESERIALIZE_DONE;
}

ssize_t YIELD::ipc::HTTPResponse::deserialize
(
  yidl::runtime::auto_Buffer buffer
)
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_HTTP_VERSION:
    {
      for ( ;; )
      {
        uint8_t test_http_version;
        if ( buffer->get( &test_http_version, 1 ) == 1 )
        {
          if ( test_http_version != ' ' )
          {
            http_version = test_http_version;
            continue;
          }
          else
          {
            http_version = http_version == '1' ? 1 : 0;
            deserialize_state = DESERIALIZING_STATUS_CODE;
            break;
          }
        }
        else
          return 1;
      }
    }
    // Fall through

    case DESERIALIZING_STATUS_CODE:
    {
      char* status_code_str_p
        = status_code_str + strnlen( status_code_str, 3 );
      for ( ;; )
      {
        if ( buffer->get( status_code_str_p, 1 ) == 1 )
        {
          if ( *status_code_str_p != ' ' )
          {
            status_code_str_p++;
            if
            (
              static_cast<uint8_t>( status_code_str_p - status_code_str ) == 4
            )
            {
              deserialize_state = DESERIALIZE_DONE;
              return -1;
            }
          }
          else
          {
            *status_code_str_p = 0;
            status_code = static_cast<uint16_t>( atoi( status_code_str ) );
            if ( status_code == 0 )
              status_code = 500;
            deserialize_state = DESERIALIZING_REASON;
            break;
          }
        }
        else
          return 1;
      }
    }
    // Fall through

    case DESERIALIZING_REASON:
    {
      char c;
      for ( ;; )
      {
        if ( buffer->get( &c, 1 ) == 1 )
        {
          if ( c == '\r' )
          {
            deserialize_state = DESERIALIZING_HEADERS;
            break;
          }
        }
        else
          return 1;
      }
    }
    // Fall through

    default: return HTTPMessage::deserialize( buffer );
  }
}

yidl::runtime::auto_Buffer YIELD::ipc::HTTPResponse::serialize()
{
  const char* status_line;
  size_t status_line_len;

  switch ( status_code )
  {
    case 100: status_line = "HTTP/1.1 100 Continue\r\n"; status_line_len = 23; break;
    case 200: status_line = "HTTP/1.1 200 OK\r\n"; status_line_len = 17; break;
    case 201: status_line = "HTTP/1.1 201 Created\r\n"; status_line_len = 22; break;
    case 202: status_line = "HTTP/1.1 202 Accepted\r\n"; status_line_len = 23; break;
    case 203: status_line = "HTTP/1.1 203 Non-Authoritative Information\r\n"; status_line_len = 44; break;
    case 204: status_line = "HTTP/1.1 204 No Content\r\n"; status_line_len = 25; break;
    case 205: status_line = "HTTP/1.1 205 Reset Content\r\n"; status_line_len = 28; break;
    case 206: status_line = "HTTP/1.1 206 Partial Content\r\n"; status_line_len = 30; break;
    case 207: status_line = "HTTP/1.1 207 Multi-Status\r\n"; status_line_len = 27; break;
    case 300: status_line = "HTTP/1.1 300 Multiple Choices\r\n"; status_line_len = 31; break;
    case 301: status_line = "HTTP/1.1 301 Moved Permanently\r\n"; status_line_len = 32; break;
    case 302: status_line = "HTTP/1.1 302 Found\r\n"; status_line_len = 20; break;
    case 303: status_line = "HTTP/1.1 303 See Other\r\n"; status_line_len = 24; break;
    case 304: status_line = "HTTP/1.1 304 Not Modified\r\n"; status_line_len = 27; break;
    case 305: status_line = "HTTP/1.1 305 Use Proxy\r\n"; status_line_len = 24; break;
    case 307: status_line = "HTTP/1.1 307 Temporary Redirect\r\n"; status_line_len = 33; break;
    case 400: status_line = "HTTP/1.1 400 Bad Request\r\n"; status_line_len = 26; break;
    case 401: status_line = "HTTP/1.1 401 Unauthorized\r\n"; status_line_len = 27; break;
    case 403: status_line = "HTTP/1.1 403 Forbidden\r\n"; status_line_len = 24; break;
    case 404: status_line = "HTTP/1.1 404 Not Found\r\n"; status_line_len = 24; break;
    case 405: status_line = "HTTP/1.1 405 Method Not Allowed\r\n"; status_line_len = 33; break;
    case 406: status_line = "HTTP/1.1 406 Not Acceptable\r\n"; status_line_len = 29; break;
    case 407: status_line = "HTTP/1.1 407 Proxy Authentication Required\r\n"; status_line_len = 44; break;
    case 408: status_line = "HTTP/1.1 408 Request Timeout\r\n"; status_line_len = 30; break;
    case 409: status_line = "HTTP/1.1 409 Conflict\r\n"; status_line_len = 23; break;
    case 410: status_line = "HTTP/1.1 410 Gone\r\n"; status_line_len = 19; break;
    case 411: status_line = "HTTP/1.1 411 Length Required\r\n"; status_line_len = 30; break;
    case 412: status_line = "HTTP/1.1 412 Precondition Failed\r\n"; status_line_len = 34; break;
    case 413: status_line = "HTTP/1.1 413 Request Entity Too Large\r\n"; status_line_len = 39; break;
    case 414: status_line = "HTTP/1.1 414 Request-URI Too Long\r\n"; status_line_len = 35; break;
    case 415: status_line = "HTTP/1.1 415 Unsupported Media Type\r\n"; status_line_len = 37; break;
    case 416: status_line = "HTTP/1.1 416 Request Range Not Satisfiable\r\n"; status_line_len = 44; break;
    case 417: status_line = "HTTP/1.1 417 Expectation Failed\r\n"; status_line_len = 33; break;
    case 422: status_line = "HTTP/1.1 422 Unprocessable Entitiy\r\n"; status_line_len = 36; break;
    case 423: status_line = "HTTP/1.1 423 Locked\r\n"; status_line_len = 21; break;
    case 424: status_line = "HTTP/1.1 424 Failed Dependency\r\n"; status_line_len = 32; break;
    case 500: status_line = "HTTP/1.1 500 Internal Server Error\r\n"; status_line_len = 36; break;
    case 501: status_line = "HTTP/1.1 501 Not Implemented\r\n"; status_line_len = 30; break;
    case 502: status_line = "HTTP/1.1 502 Bad Gateway\r\n"; status_line_len = 26; break;
    case 503: status_line = "HTTP/1.1 503 Service Unavailable\r\n"; status_line_len = 34; break;
    case 504: status_line = "HTTP/1.1 504 Gateway Timeout\r\n"; status_line_len = 30; break;
    case 505: status_line = "HTTP/1.1 505 HTTP Version Not Supported\r\n"; status_line_len = 41; break;
    case 507: status_line = "HTTP/1.1 507 Insufficient Storage\r\n"; status_line_len = 35; break;
    default: status_line = "HTTP/1.1 500 Internal Server Error\r\n"; status_line_len = 36; break;
  }

  RFC822Headers::set_iovec( 0, status_line, status_line_len );

  char date[32];
  YIELD::platform::Time().as_http_date_time( date, 32 );
  set_header( "Date", date );

  return HTTPMessage::serialize();
}

void YIELD::ipc::HTTPResponse::set_body( yidl::runtime::auto_Buffer body )
{
  this->body = body;
}

void YIELD::ipc::HTTPResponse::set_status_code( uint16_t status_code )
{
  this->status_code = status_code;
}


// http_server.cpp
class YIELD::ipc::HTTPServer::AIOWriteControlBlock
  : public Socket::AIOWriteControlBlock
{
public:
  AIOWriteControlBlock( yidl::runtime::auto_Buffer buffer )
    : Socket::AIOWriteControlBlock( buffer )
  { }

  void onCompletion( size_t )
  { }

  void onError( uint32_t )
  {
    get_socket()->shutdown();
    get_socket()->close();
  }
};


class YIELD::ipc::HTTPServer::HTTPResponseTarget
  : public YIELD::concurrency::EventTarget
{
public:
  HTTPResponseTarget( auto_Socket socket_ )
    : socket_( socket_ )
  { }

  // yidl::runtime::Object
  YIDL_RUNTIME_OBJECT_PROTOTYPES( HTTPServer::HTTPResponseTarget, 0 );

  // EventTarget
  void send( YIELD::concurrency::Event& ev )
  {
    if ( ev.get_type_id() == YIDL_RUNTIME_OBJECT_TYPE_ID( HTTPResponse ) )
    {
      HTTPResponse& http_response = static_cast<HTTPResponse&>( ev );

      socket_->aio_write
      (
        new AIOWriteControlBlock( http_response.serialize() )
      );

      HTTPResponse::decRef( http_response );
    }
    else
      DebugBreak();
  }

private:
  auto_Socket socket_;
};


class YIELD::ipc::HTTPServer::AIOReadControlBlock
  : public Socket::AIOReadControlBlock
{
public:
  AIOReadControlBlock
  (
    yidl::runtime::auto_Buffer buffer,
    auto_HTTPRequest http_request,
    YIELD::concurrency::auto_EventTarget http_request_target
  )
    : Socket::AIOReadControlBlock( buffer ),
      http_request( http_request ),
      http_request_target( http_request_target )
  { }

  void onCompletion( size_t )
  {
    for ( ;; )
    {
      ssize_t deserialize_ret = http_request->deserialize( get_buffer() );
      if ( deserialize_ret == 0 )
      {
        http_request->set_response_target
        (
          new HTTPResponseTarget( get_socket() )
        );
        http_request_target->send( *http_request.release() );
        http_request = new HTTPRequest;
      }
      else if ( deserialize_ret > 0 )
      {
        get_socket()->aio_read
        (
          new AIOReadControlBlock
          (
            new yidl::runtime::HeapBuffer( 1024 ),
            http_request,
            http_request_target
          )
        );

        return;
      }
      else
      {
        get_socket()->shutdown();
        get_socket()->close();
        return;
      }
    }
  }

  void onError( uint32_t )
  {
    get_socket()->close();
  }

private:
  auto_HTTPRequest http_request;
  YIELD::concurrency::auto_EventTarget http_request_target;
};


class YIELD::ipc::HTTPServer::AIOAcceptControlBlock
  : public TCPSocket::AIOAcceptControlBlock
{
public:
  AIOAcceptControlBlock
  (
    YIELD::concurrency::auto_EventTarget http_request_target,
    YIELD::platform::auto_Log log
  )
    : http_request_target( http_request_target ), log( log )
  { }

  void onCompletion( size_t )
  {
    auto_Socket accepted_tcp_socket( get_accepted_tcp_socket().release() );
    if ( log != NULL && log->get_level() >= YIELD::platform::Log::LOG_INFO )
      accepted_tcp_socket = new TracingSocket( accepted_tcp_socket, log );

    accepted_tcp_socket->aio_read
    (
      new AIOReadControlBlock
      (
        new yidl::runtime::HeapBuffer( 1024 ),
        new HTTPRequest, http_request_target
      )
    );

    static_cast<TCPSocket*>( get_socket().get() )
      ->aio_accept( new AIOAcceptControlBlock( http_request_target, log ) );
  }

  void onError( uint32_t )
  {
    get_socket()->shutdown();
    get_socket()->close();
  }

private:
  YIELD::concurrency::auto_EventTarget http_request_target;
  YIELD::platform::auto_Log log;
};


YIELD::ipc::HTTPServer::HTTPServer
(
  YIELD::concurrency::auto_EventTarget http_request_target,
  auto_TCPSocket listen_tcp_socket,
  YIELD::platform::auto_Log log
)
  : http_request_target( http_request_target ),
    listen_tcp_socket( listen_tcp_socket ),
    log( log )
{
  for ( uint8_t accept_i = 0; accept_i < 10; accept_i++ )
  {
    listen_tcp_socket->aio_accept
    (
      new AIOAcceptControlBlock( http_request_target, log )
    );
  }
}

YIELD::ipc::auto_HTTPServer
  YIELD::ipc::HTTPServer::create
  (
    const URI& absolute_uri,
    YIELD::concurrency::auto_EventTarget http_request_target,
    YIELD::platform::auto_Log log,
    auto_SSLContext ssl_context
  )
{
  auto_SocketAddress sockname = SocketAddress::create( absolute_uri );

  auto_TCPSocket listen_tcp_socket;
#ifdef YIELD_IPC_HAVE_OPENSSL
  if ( absolute_uri.get_scheme() == "https" && ssl_context != NULL )
    listen_tcp_socket = SSLSocket::create( ssl_context ).release();
  else
#endif
    listen_tcp_socket = TCPSocket::create();

  if ( listen_tcp_socket != NULL &&
       listen_tcp_socket->bind( sockname ) &&
       listen_tcp_socket->listen() )
    return new HTTPServer( http_request_target, listen_tcp_socket, log );
  else
    throw YIELD::platform::Exception();
}


// json_marshaller.cpp
extern "C"
{
};


YIELD::ipc::JSONMarshaller::JSONMarshaller( bool write_empty_strings )
: write_empty_strings( write_empty_strings )
{
  buffer = new yidl::runtime::StringBuffer;
  root_key = NULL;
  writer = yajl_gen_alloc( NULL );
}

YIELD::ipc::JSONMarshaller::JSONMarshaller
(
  JSONMarshaller& parent_json_marshaller,
  const char* root_key
)
  : root_key( root_key ),
    write_empty_strings( parent_json_marshaller.write_empty_strings ),
    writer( parent_json_marshaller.writer ),
    buffer( parent_json_marshaller.buffer )
{ }

YIELD::ipc::JSONMarshaller::~JSONMarshaller()
{
//  if ( root_key == NULL ) // This is the root JSONMarshaller
//    yajl_gen_free( writer );
}

void YIELD::ipc::JSONMarshaller::flushYAJLBuffer()
{
  const unsigned char* buffer;
  unsigned int len;
  yajl_gen_get_buf( writer, &buffer, &len );
  this->buffer->put( buffer, len );
  yajl_gen_clear( writer );
}

void YIELD::ipc::JSONMarshaller::writeBoolean
(
  const char* key,
  uint32_t,
  bool value
)
{
  writeKey( key );
  yajl_gen_bool( writer, static_cast<int>( value ) );
  flushYAJLBuffer();
}

void YIELD::ipc::JSONMarshaller::writeBuffer
(
  const char*,
  uint32_t,
  yidl::runtime::auto_Buffer
)
{
  DebugBreak();
}

void YIELD::ipc::JSONMarshaller::writeKey( const char* key )
{
  if ( in_map && key != NULL )
  {
    yajl_gen_string
    (
      writer,
      reinterpret_cast<const unsigned char*>( key ),
      static_cast<unsigned int>( strnlen( key, UINT16_MAX ) )
    );
  }
}

void YIELD::ipc::JSONMarshaller::writeDouble
(
  const char* key,
  uint32_t,
  double value
)
{
  writeKey( key );
  yajl_gen_double( writer, value );
  flushYAJLBuffer();
}

void YIELD::ipc::JSONMarshaller::writeInt64
(
  const char* key,
  uint32_t,
  int64_t value
)
{
  writeKey( key );
  yajl_gen_integer( writer, static_cast<long>( value ) );
  flushYAJLBuffer();
}

void YIELD::ipc::JSONMarshaller::writeMap
(
  const char* key,
  uint32_t,
  const yidl::runtime::Map& value
)
{
  writeKey( key );
  JSONMarshaller( *this, key ).writeMap( &value );
}

void YIELD::ipc::JSONMarshaller::writeMap
(
  const yidl::runtime::Map* value
)
{
  yajl_gen_map_open( writer );
  in_map = true;
  if ( value )
    value->marshal( *this );
  yajl_gen_map_close( writer );
  flushYAJLBuffer();
}

void YIELD::ipc::JSONMarshaller::writeStruct
(
  const char* key,
  uint32_t,
  const yidl::runtime::Struct& value
)
{
  writeKey( key );
  JSONMarshaller( *this, key ).writeStruct( &value );
}

void YIELD::ipc::JSONMarshaller::writeStruct
(
  const yidl::runtime::Struct* value
)
{
  yajl_gen_map_open( writer );
  in_map = true;
  if ( value )
    value->marshal( *this );
  yajl_gen_map_close( writer );
  flushYAJLBuffer();
}

void YIELD::ipc::JSONMarshaller::writeSequence
(
  const char* key,
  uint32_t,
  const yidl::runtime::Sequence& value
)
{
  writeKey( key );
  JSONMarshaller( *this, key ).writeSequence( &value );
}

void YIELD::ipc::JSONMarshaller::writeSequence
(
  const yidl::runtime::Sequence* value
)
{
  yajl_gen_array_open( writer );
  in_map = false;
  if ( value )
    value->marshal( *this );
  yajl_gen_array_close( writer );
  flushYAJLBuffer();
}

void YIELD::ipc::JSONMarshaller::writeString
(
  const char* key,
  uint32_t,
  const char* value,
  size_t value_len
)
{
  if ( value_len > 0 || write_empty_strings )
  {
    writeKey( key );
    yajl_gen_string
    (
      writer,
      reinterpret_cast<const unsigned char*>( value ),
      static_cast<unsigned int>( value_len )
    );
    flushYAJLBuffer();
  }
}


// json_unmarshaller.cpp
extern "C"
{
};


class YIELD::ipc::JSONUnmarshaller::JSONValue
{
public:
  JSONValue( yidl::runtime::auto_StringBuffer identifier, bool is_map )
    : identifier( identifier ), is_map( is_map )
  {
    as_double = 0;
    as_integer = 0;

    parent = child = prev = next = NULL;
    have_read = false;
  }

  virtual ~JSONValue()
  {
    delete child;
    delete next;
  }

  yidl::runtime::auto_StringBuffer identifier;
  bool is_map;

  double as_double;
  int64_t as_integer;
  yidl::runtime::auto_StringBuffer as_string;

  JSONValue *parent, *child, *prev, *next;
  bool have_read;

protected:
  JSONValue()
  {
    is_map = true;
    parent = child = prev = next = NULL;
    have_read = false;
    as_integer = 0;
  }
};


class YIELD::ipc::JSONUnmarshaller::JSONObject : public JSONValue
{
public:
  JSONObject( yidl::runtime::auto_Buffer json_buffer )
  {
    current_json_value = parent_json_value = NULL;
    reader = yajl_alloc( &JSONObject_yajl_callbacks, NULL, this );
    next_map_key = NULL; next_map_key_len = 0;

    const unsigned char* json_text =
      static_cast<const unsigned char*>
      (
        static_cast<void*>( *json_buffer )
      );
    unsigned int json_text_len
      = static_cast<unsigned int>( json_buffer->size() );

    yajl_status yajl_parse_status
      = yajl_parse( reader, json_text, json_text_len );

    if ( yajl_parse_status == yajl_status_ok )
      return;
    else if ( yajl_parse_status != yajl_status_insufficient_data )
    {
      unsigned char* yajl_error_str
        = yajl_get_error( reader, 1, json_text, json_text_len );
      std::ostringstream what;
      what << __FILE__ << ":" << __LINE__ << ": JSON parsing error: "
        << reinterpret_cast<char*>( yajl_error_str ) << std::endl;
      yajl_free_error( yajl_error_str );
      throw YIELD::platform::Exception( what.str() );
    }
  }

  ~JSONObject()
  {
    yajl_free( reader );
  }

private:
  yajl_handle reader;

  std::string type_name;
  uint32_t tag;

  // Parsing state
  JSONValue *current_json_value, *parent_json_value;
  const char* next_map_key; size_t next_map_key_len;

  // yajl callbacks
  static int handle_yajl_null( void* _self )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    self->createNextJSONValue().as_integer = 0;
    return 1;
  }

  static int handle_yajl_boolean( void* _self, int value )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    self->createNextJSONValue().as_integer = value;
    return 1;
  }

  static int handle_yajl_integer( void* _self, long value )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    self->createNextJSONValue().as_integer = value;
    return 1;
  }

  static int handle_yajl_double( void* _self, double value )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    self->createNextJSONValue().as_double = value;
    return 1;
  }

  static int handle_yajl_string
  (
    void* _self,
    const unsigned char* buffer,
    unsigned int len
  )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    JSONValue& json_value = self->createNextJSONValue();
    json_value.as_string
      = new yidl::runtime::StringBuffer
        (
          reinterpret_cast<const char*>( buffer ),
          len
        );
    return 1;
  }

  static int handle_yajl_start_map( void* _self )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    JSONValue& json_value = self->createNextJSONValue( true );
    self->parent_json_value = &json_value;
    self->current_json_value = json_value.child;
    return 1;
  }

  static int handle_yajl_map_key
  (
    void* _self,
    const unsigned char* map_key,
    unsigned int map_key_len
  )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    self->next_map_key = reinterpret_cast<const char*>( map_key );
    self->next_map_key_len = map_key_len;
    return 1;
  }

  static int handle_yajl_end_map( void* _self )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    if ( self->current_json_value == NULL ) // Empty map
      self->current_json_value = self->parent_json_value;
    else
      self->current_json_value = self->current_json_value->parent;
    self->parent_json_value = NULL;
    return 1;
  }

  static int handle_yajl_start_array( void* _self )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    JSONValue& json_value = self->createNextJSONValue();
    self->parent_json_value = &json_value;
    self->current_json_value = json_value.child;
    return 1;
  }

  static int handle_yajl_end_array( void* _self )
  {
    JSONObject* self = static_cast<JSONObject*>( _self );
    if ( self->current_json_value == NULL ) // Empty array
      self->current_json_value = self->parent_json_value;
    else
      self->current_json_value = self->current_json_value->parent;
    self->parent_json_value = NULL;
    return 1;
  }

  JSONValue& createNextJSONValue( bool is_map = false )
  {
    yidl::runtime::auto_StringBuffer identifier;
    if ( next_map_key_len != 0 )
    {
      identifier
        = new yidl::runtime::StringBuffer( next_map_key, next_map_key_len );
    }
    else
      identifier = NULL;

    next_map_key = NULL;
    next_map_key_len = 0;

    if ( current_json_value == NULL )
    {
      if ( parent_json_value ) // This is the first value of an array or map
      {
        current_json_value = new JSONValue( identifier, is_map );
        current_json_value->parent = parent_json_value;
        parent_json_value->child = current_json_value;
      }
      else // This is the first value of the whole object
      {
#ifdef _DEBUG
        if ( identifier != NULL ) DebugBreak();
#endif
        current_json_value = this;
      }
    }
    else
    {
      JSONValue* next_json_value = new JSONValue( identifier, is_map );
      next_json_value->parent = current_json_value->parent;
      next_json_value->prev = current_json_value;
      current_json_value->next = next_json_value;
      current_json_value = next_json_value;
    }

    return *current_json_value;
  }

  static yajl_callbacks JSONObject_yajl_callbacks;
};

yajl_callbacks
  YIELD::ipc::JSONUnmarshaller::JSONObject::JSONObject_yajl_callbacks =
{
  handle_yajl_null,
  handle_yajl_boolean,
  handle_yajl_integer,
  handle_yajl_double,
  NULL,
  handle_yajl_string,
  handle_yajl_start_map,
  handle_yajl_map_key,
  handle_yajl_end_map,
  handle_yajl_start_array,
  handle_yajl_end_array
};


YIELD::ipc::JSONUnmarshaller::JSONUnmarshaller
(
  yidl::runtime::auto_Buffer buffer
)
{
  root_key = NULL;
  root_json_value = new JSONObject( buffer );
  next_json_value = root_json_value->child;
}

YIELD::ipc::JSONUnmarshaller::JSONUnmarshaller
(
  const char* root_key,
  JSONValue& root_json_value
)
  : root_key( root_key ), root_json_value( &root_json_value ),
    next_json_value( root_json_value.child )
{ }

YIELD::ipc::JSONUnmarshaller::~JSONUnmarshaller()
{
//  if ( root_key == NULL )
//    delete root_json_value;
}

bool YIELD::ipc::JSONUnmarshaller::readBoolean( const char* key, uint32_t )
{
  JSONValue* json_value = readJSONValue( key );
  if ( json_value )
  {
    if ( key != NULL ) // Read the value
      return json_value->as_integer != 0;
    else // Read the identifier
      return false; // Doesn't make any sense
  }
  else
    return false;
}

void YIELD::ipc::JSONUnmarshaller::readBuffer
(
  const char*,
  uint32_t,
  yidl::runtime::auto_Buffer value
)
{
  DebugBreak();
}

double YIELD::ipc::JSONUnmarshaller::readDouble( const char* key, uint32_t )
{
  JSONValue* json_value = readJSONValue( key );
  if ( json_value )
  {
    if ( key != NULL ) // Read the value
    {
      if ( json_value->as_double != 0 || json_value->as_integer == 0 )
        return json_value->as_double;
      else
        return static_cast<double>( json_value->as_integer );
    }
    else // Read the identifier
      return atof( json_value->identifier->c_str() );
  }
  else
    return 0;
}

int64_t YIELD::ipc::JSONUnmarshaller::readInt64( const char* key, uint32_t )
{
  JSONValue* json_value = readJSONValue( key );
  if ( json_value )
  {
    if ( key != NULL ) // Read the value
      return json_value->as_integer;
    else // Read the identifier
      return atoi( json_value->identifier->c_str() );
  }
  else
    return 0;
}

void YIELD::ipc::JSONUnmarshaller::readMap
(
  const char* key,
  uint32_t,
  yidl::runtime::Map& value
)
{
  JSONValue* json_value;
  if ( key != NULL )
  {
    json_value = readJSONValue( key );
    if ( json_value == NULL )
      return;
  }
  else if ( root_json_value && !root_json_value->have_read )
  {
    if ( root_json_value->is_map )
      json_value = root_json_value;
    else
      return;
  }
  else
    return;

  JSONUnmarshaller child_json_unmarshaller( key, *json_value );
  child_json_unmarshaller.readMap( value );
  json_value->have_read = true;
}

void YIELD::ipc::JSONUnmarshaller::readMap( yidl::runtime::Map& value )
{
  while ( next_json_value )
    value.unmarshal( *this );
}

void YIELD::ipc::JSONUnmarshaller::readSequence
(
  const char* key,
  uint32_t,
  yidl::runtime::Sequence& value
)
{
  JSONValue* json_value;
  if ( key != NULL )
  {
    json_value = readJSONValue( key );
    if ( json_value == NULL )
      return;
  }
  else if ( root_json_value && !root_json_value->have_read )
  {
    if ( !root_json_value->is_map )
      json_value = root_json_value;
    else
      return;
  }
  else
    return;

  JSONUnmarshaller child_json_unmarshaller( key, *json_value );
  child_json_unmarshaller.readSequence( value );
  json_value->have_read = true;
}

void YIELD::ipc::JSONUnmarshaller::readSequence
(
  yidl::runtime::Sequence& value
)
{
  while ( next_json_value )
    value.unmarshal( *this );
}

void YIELD::ipc::JSONUnmarshaller::readString
(
  const char* key,
  uint32_t,
  std::string& str
)
{
  JSONValue* json_value = readJSONValue( key );
  if ( json_value )
  {
    if ( key != NULL ) // Read the value
    {
      if ( json_value->as_string != NULL )
      {
        str.assign
        (
          static_cast<const std::string&>( *json_value->as_string )
        );
      }
    }
    else // Read the identifier
      str.assign( static_cast<const std::string&>( *json_value->identifier ) );
  }
}

void YIELD::ipc::JSONUnmarshaller::readStruct
(
  const char* key,
  uint32_t,
  yidl::runtime::Struct& value
)
{
  JSONValue* json_value;
  if ( key != NULL )
  {
    json_value = readJSONValue( key );
    if ( json_value == NULL )
      return;
  }
  else if ( root_json_value && !root_json_value->have_read )
  {
    if ( root_json_value->is_map )
      json_value = root_json_value;
    else
      return;
  }
  else
    return;

  JSONUnmarshaller child_json_unmarshaller( key, *json_value );
  child_json_unmarshaller.readStruct( value );
  json_value->have_read = true;
}

void YIELD::ipc::JSONUnmarshaller::readStruct( yidl::runtime::Struct& s )
{
  s.unmarshal( *this );
}

YIELD::ipc::JSONUnmarshaller::JSONValue*
  YIELD::ipc::JSONUnmarshaller::readJSONValue( const char* key )
{
  if ( root_json_value->is_map )
  {
    if ( key != NULL ) // Given a key, reading a value
    {
      JSONValue* child_json_value = root_json_value->child;

      while ( child_json_value )
      {
        if
        (
          !child_json_value->have_read
          &&
          *child_json_value->identifier == key
        )
        {
          child_json_value->have_read = true;
          return child_json_value;
        }

        child_json_value = child_json_value->next;
      }
    }
    else if ( next_json_value && !next_json_value->have_read )
    {
      // Reading the next key
      JSONValue* json_value = next_json_value;
      next_json_value = json_value->next;
      return json_value;
    }
  }
  else
  {
    if ( next_json_value != NULL && !next_json_value->have_read )
    {
      JSONValue* json_value = next_json_value;
      next_json_value = json_value->next;
      json_value->have_read = true;
      return json_value;
    }
  }

  return NULL;
}


// named_pipe.cpp
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif


YIELD::ipc::auto_NamedPipe YIELD::ipc::NamedPipe::open
(
  const YIELD::platform::Path& path,
  uint32_t flags,
  mode_t mode
)
{
#ifdef _WIN32
  YIELD::platform::Path named_pipe_base_dir_path( TEXT( "\\\\.\\pipe" ) );
  YIELD::platform::Path named_pipe_path( named_pipe_base_dir_path + path );

  if ( ( flags & O_CREAT ) == O_CREAT ) // Server
  {
    HANDLE hPipe
      = CreateNamedPipe
        (
          named_pipe_path,
          PIPE_ACCESS_DUPLEX,
          PIPE_TYPE_BYTE|PIPE_READMODE_BYTE|PIPE_WAIT,
          PIPE_UNLIMITED_INSTANCES,
          4096,
          4096,
          0,
          NULL
        );

    if ( hPipe != INVALID_HANDLE_VALUE )
      return new NamedPipe( new YIELD::platform::File( hPipe ), false );
  }
  else // Client
  {
    YIELD::platform::auto_File
      underlying_file
      (
        YIELD::platform::Volume().open( named_pipe_path, flags )
      );

    if ( underlying_file != NULL )
      return new NamedPipe( underlying_file, true );
  }
#else
  if ( ( flags & O_CREAT ) == O_CREAT )
  {
    if ( ::mkfifo( path, mode ) != -1 ||
         errno == EEXIST )
      flags ^= O_CREAT;
    else
      return NULL;
  }

  YIELD::platform::auto_File
    underlying_file( YIELD::platform::Volume().open( path, flags ) );

  if ( underlying_file != NULL )
    return new NamedPipe( underlying_file );
#endif

  return NULL;
}

#ifdef _WIN32
YIELD::ipc::NamedPipe::NamedPipe
(
  YIELD::platform::auto_File underlying_file,
  bool connected
)
  : underlying_file( underlying_file ), connected( connected )
{ }
#else
YIELD::ipc::NamedPipe::NamedPipe( YIELD::platform::auto_File underlying_file )
  : underlying_file( underlying_file )
{ }
#endif

#ifdef _WIN32
bool YIELD::ipc::NamedPipe::connect()
{
  if ( connected )
    return true;
  else
  {
    if ( ConnectNamedPipe( *underlying_file, NULL ) != 0 ||
         GetLastError() == ERROR_PIPE_CONNECTED )
    {
      connected = true;
      return true;
    }
    else
      return false;
  }
}
#endif

ssize_t YIELD::ipc::NamedPipe::read( void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  if ( connect() )
    return underlying_file->read( buffer, buffer_len );
  else
    return -1;
#else
  return underlying_file->read( buffer, buffer_len );
#endif
}

ssize_t YIELD::ipc::NamedPipe::write( const void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  if ( connect() )
    return underlying_file->write( buffer, buffer_len );
  else
    return -1;
#else
  return underlying_file->write( buffer, buffer_len );
#endif
}

#ifdef _WIN32
#pragma warning( pop )
#endif


// oncrpc_message.cpp
template <class ONCRPCMessageType>
YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::ONCRPCMessage
(
  YIELD::concurrency::auto_Interface interface_
)
  : interface_( interface_ )
{
  xid = 0;
  deserialize_state = DESERIALIZING_RECORD_FRAGMENT_MARKER;
}

template <class ONCRPCMessageType>
YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::ONCRPCMessage
(
  YIELD::concurrency::auto_Interface interface_,
  uint32_t xid, yidl::runtime::auto_Struct body
)
  : interface_( interface_ ), xid( xid ), body( body )
{
  deserialize_state = DESERIALIZING_RECORD_FRAGMENT_MARKER;
}

template <class ONCRPCMessageType>
YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::~ONCRPCMessage()
{ }

template <class ONCRPCMessageType>
ssize_t YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::deserialize
(
  yidl::runtime::auto_Buffer buffer
)
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_RECORD_FRAGMENT_MARKER:
    {
      ssize_t deserialize_ret = deserializeRecordFragmentMarker( buffer );

      if ( deserialize_ret == 0 )
        deserialize_state = DESERIALIZING_RECORD_FRAGMENT;
      else
        return deserialize_ret;
    }
    // Drop down

    case DESERIALIZING_RECORD_FRAGMENT:
    {
      ssize_t deserialize_ret = deserializeRecordFragment( buffer );

      if ( deserialize_ret == 0 )
        deserialize_state = DESERIALIZE_DONE;
      else if ( deserialize_ret > 0 )
        deserialize_state = DESERIALIZING_LONG_RECORD_FRAGMENT;

      return deserialize_ret;
    }

    case DESERIALIZING_LONG_RECORD_FRAGMENT:
    {
      ssize_t deserialize_ret = deserializeLongRecordFragment( buffer );

      if ( deserialize_ret == 0 )
        deserialize_state = DESERIALIZE_DONE;
      else
        return deserialize_ret;
    }
    // Drop down

    case DESERIALIZE_DONE: return 0;
  }

  return -1;
}

template <class ONCRPCMessageType>
ssize_t
YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::deserializeRecordFragmentMarker
(
  yidl::runtime::auto_Buffer buffer
)
{
  uint32_t record_fragment_marker = 0;
  size_t record_fragment_marker_filled
    = buffer->get
      (
        &record_fragment_marker,
        sizeof( record_fragment_marker )
      );

  if ( record_fragment_marker_filled == sizeof( record_fragment_marker ) )
  {
#ifdef __MACH__
    record_fragment_marker = ntohl( record_fragment_marker );
#else
    record_fragment_marker
      = YIELD::platform::Machine::ntohl( record_fragment_marker );
#endif
    if ( ( record_fragment_marker >> 31 ) != 0 )
    {
      // The highest bit set = last record fragment
      record_fragment_length = record_fragment_marker ^ ( 1 << 31UL );
      if ( record_fragment_length < 32 * 1024 * 1024 )
        return 0;
      else
        return -1;
    }
    else
      return -1;
  }
  else if ( record_fragment_marker_filled == 0 )
    return sizeof( record_fragment_marker );
  else
    return -1;
}

template <class ONCRPCMessageType>
ssize_t
YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::deserializeRecordFragment
(
  yidl::runtime::auto_Buffer buffer
)
{
  size_t gettable_buffer_size = buffer->size() - buffer->position();
  if ( gettable_buffer_size == record_fragment_length ) // Common case
  {
    record_fragment_buffer = buffer;
    YIELD::platform::XDRUnmarshaller xdr_unmarshaller( buffer );
    static_cast<ONCRPCMessageType*>( this )->unmarshal( xdr_unmarshaller );
    return 0;
  }
  else if ( gettable_buffer_size < record_fragment_length )
  {
    record_fragment_buffer
      = new yidl::runtime::HeapBuffer( record_fragment_length );

    buffer->get
    (
      static_cast<void*>( *record_fragment_buffer ),
      gettable_buffer_size
    );

    record_fragment_buffer->put( NULL, gettable_buffer_size );

    return static_cast<ssize_t>
          (
            record_fragment_length
              - record_fragment_buffer->size()
          );
  }
  else
    return -1;
}

template <class ONCRPCMessageType>
ssize_t
YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::deserializeLongRecordFragment
(
  yidl::runtime::auto_Buffer buffer
)
{
  size_t gettable_buffer_size = buffer->size() - buffer->position();
  size_t remaining_record_fragment_length
    = record_fragment_length - record_fragment_buffer->size();

  if ( gettable_buffer_size < remaining_record_fragment_length )
  {
    buffer->get
    (
      static_cast<char*>( *record_fragment_buffer )
        + record_fragment_buffer->size(),
      gettable_buffer_size
    );

    record_fragment_buffer->put( NULL, gettable_buffer_size );

    return static_cast<ssize_t>
           (
             record_fragment_length
              - record_fragment_buffer->size()
           );
  }
  else if ( gettable_buffer_size == remaining_record_fragment_length )
  {
    buffer->get( static_cast<char*>
    (
      *record_fragment_buffer )
        + record_fragment_buffer->size(),
      gettable_buffer_size
    );

    record_fragment_buffer->put( NULL, gettable_buffer_size );

    YIELD::platform::XDRUnmarshaller
      xdr_unmarshaller( record_fragment_buffer );

    static_cast<ONCRPCMessageType*>( this )->unmarshal( xdr_unmarshaller );

    return 0;
  }
  else // The buffer is larger than we need to fill the record fragment,
       // logic error somewhere
    return -1;
}

template <class ONCRPCMessageType>
void YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::marshal
(
  yidl::runtime::Marshaller& marshaller
)
{
  marshaller.writeUint32( "xid", 0, xid );
}

template <class ONCRPCMessageType>
yidl::runtime::auto_Buffer
YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::serialize()
{
  YIELD::platform::XDRMarshaller xdr_marshaller;
  xdr_marshaller.writeUint32( "record_fragment_marker", 0, 0 );
  static_cast<ONCRPCMessageType*>( this )->marshal( xdr_marshaller );
  yidl::runtime::auto_StringBuffer xdr_buffer = xdr_marshaller.get_buffer();

  uint32_t record_fragment_length
    = static_cast<uint32_t>( xdr_buffer->size() - sizeof( uint32_t ) );

  // Indicate that this is the last fragment
  uint32_t record_fragment_marker
    = record_fragment_length | ( 1 << 31 );

#ifdef __MACH__
  record_fragment_marker = htonl( record_fragment_marker );
#else
  record_fragment_marker
    = YIELD::platform::Machine::htonl( record_fragment_marker );
#endif

  static_cast<std::string&>( *xdr_buffer )
    .replace
    (
      0,
      sizeof( uint32_t ),
      reinterpret_cast<const char*>( &record_fragment_marker ),
      sizeof( uint32_t )
    );

  return xdr_buffer.release();
}

template <class ONCRPCMessageType>
void YIELD::ipc::ONCRPCMessage<ONCRPCMessageType>::unmarshal
(
  yidl::runtime::Unmarshaller& unmarshaller
)
{
  xid = unmarshaller.readUint32( "xid", 0 );
}

template class YIELD::ipc::ONCRPCMessage<YIELD::ipc::ONCRPCRequest>;
template class YIELD::ipc::ONCRPCMessage<YIELD::ipc::ONCRPCResponse>;


// oncrpc_request.cpp
YIELD::ipc::ONCRPCRequest::ONCRPCRequest
(
  YIELD::concurrency::auto_Interface interface_
)
  : ONCRPCMessage<ONCRPCRequest>( interface_ )
{
  reconnect_tries = 0;
}

YIELD::ipc::ONCRPCRequest::ONCRPCRequest
(
  YIELD::concurrency::auto_Interface interface_,
  yidl::runtime::auto_Struct body
)
  : ONCRPCMessage<ONCRPCRequest>
    (
      interface_,
      static_cast<uint32_t>( YIELD::platform::Time().as_unix_time_s() ),
      body
    )
{
  prog = 0x20000000 + interface_->get_type_id();
  proc = body->get_type_id();
  vers = interface_->get_type_id();
  credential_auth_flavor = AUTH_NONE;
  reconnect_tries = 0;
}

YIELD::ipc::ONCRPCRequest::ONCRPCRequest
(
  YIELD::concurrency::auto_Interface interface_,
  uint32_t credential_auth_flavor,
  yidl::runtime::auto_Struct credential,
  yidl::runtime::auto_Struct body
)
  : ONCRPCMessage<ONCRPCRequest>
    (
      interface_,
      static_cast<uint32_t>( YIELD::platform::Time().as_unix_time_s() ),
      body
    ),
    credential_auth_flavor( credential_auth_flavor ),
    credential( credential )
{
  prog = 0x20000000 + interface_->get_type_id();
  proc = body->get_type_id();
  vers = interface_->get_type_id();
  reconnect_tries = 0;
}

YIELD::ipc::ONCRPCRequest::ONCRPCRequest
(
  uint32_t prog,
  uint32_t proc,
  uint32_t vers,
  yidl::runtime::auto_Struct body
)
  : ONCRPCMessage<ONCRPCRequest>
    (
      NULL,
      static_cast<uint32_t>( YIELD::platform::Time().as_unix_time_s() ),
      body
    ),
    prog( prog ),
    proc( proc ),
    vers( vers )
{
  credential_auth_flavor = AUTH_NONE;
  reconnect_tries = 0;
}

YIELD::ipc::ONCRPCRequest::ONCRPCRequest
(
  uint32_t prog,
  uint32_t proc,
  uint32_t vers,
  uint32_t credential_auth_flavor,
  yidl::runtime::auto_Struct credential,
  yidl::runtime::auto_Struct body
)
  : ONCRPCMessage<ONCRPCRequest>
    (
      NULL,
      static_cast<uint32_t>( YIELD::platform::Time().as_unix_time_s() ),
      body
    ),
    prog( prog ),
    proc( proc ),
    vers( vers ),
    credential_auth_flavor( credential_auth_flavor ),
    credential( credential )
{
  reconnect_tries = 0;
}

YIELD::concurrency::auto_Response YIELD::ipc::ONCRPCRequest::createResponse()
{
  return new ONCRPCResponse
            (
              get_interface(),
              get_xid(),
              static_cast<Request*>( get_body().get() )
                ->createResponse().release()
            );
}

void YIELD::ipc::ONCRPCRequest::marshal
(
  yidl::runtime::Marshaller& marshaller
)
{
  ONCRPCMessage<ONCRPCRequest>::marshal( marshaller );

  marshaller.writeInt32( "msg_type", 0, 0 ); // MSG_CALL
  marshaller.writeInt32( "rpcvers", 0, 2 );
  marshaller.writeInt32( "prog", 0, prog );
  marshaller.writeInt32( "vers", 0, vers );
  marshaller.writeInt32( "proc", 0, proc );

  marshaller.writeInt32( "credential_auth_flavor", 0, credential_auth_flavor );
  if ( credential_auth_flavor == AUTH_NONE || credential == NULL )
    marshaller.writeInt32( "credential_auth_body_length", 0, 0 );
  else
  {
    YIELD::platform::XDRMarshaller credential_auth_body_xdr_marshaller;
    credential->marshal( credential_auth_body_xdr_marshaller );
    marshaller.writeBuffer
    (
      "credential_auth_body",
      0,
      credential_auth_body_xdr_marshaller.get_buffer().release()
    );
  }

  marshaller.writeInt32( "verf_auth_flavor", 0, AUTH_NONE );
  marshaller.writeInt32( "verf_auth_body_length", 0, 0 );

  marshaller.writeStruct( "body", 0, *get_body() );
}

void YIELD::ipc::ONCRPCRequest::respond
(
  YIELD::concurrency::Response& response
)
{
  if ( this->get_response_target() == NULL )
  {
    YIELD::concurrency::auto_Interface interface_( get_interface() );
    yidl::runtime::auto_Struct body( get_body() );

    Request* interface_request = interface_->checkRequest( *body );

    if ( interface_request != NULL )
    {
      if
      (
        response.get_type_id()
          == YIDL_RUNTIME_OBJECT_TYPE_ID( ONCRPCResponse )
      )
      {
        ONCRPCResponse& oncrpc_response
          = static_cast<ONCRPCResponse&>( response );
        yidl::runtime::auto_Struct oncrpc_response_body
          = oncrpc_response.get_body();
        YIELD::concurrency::Response* interface_response
          = interface_->checkResponse( *oncrpc_response_body );
        if ( interface_response != NULL )
        {
          YIELD::concurrency::Response::decRef( response );
          return interface_request->respond( interface_response->incRef() );
        }
        else if
        (
          oncrpc_response_body->get_type_id() ==
           YIDL_RUNTIME_OBJECT_TYPE_ID( YIELD::concurrency::ExceptionResponse )
        )
        {
          YIELD::concurrency::Response::decRef( response );
          return interface_request->respond
                (
                  static_cast<YIELD::concurrency::ExceptionResponse&>
                  (
                    *oncrpc_response_body.release()
                  )
                );
        }
      }
      else
        return interface_request->respond( response );
    }
  }

  return Request::respond( response );
}

void YIELD::ipc::ONCRPCRequest::set_reconnect_tries( uint8_t reconnect_tries )
{
  this->reconnect_tries = reconnect_tries;
}

void YIELD::ipc::ONCRPCRequest::unmarshal
(
  yidl::runtime::Unmarshaller& unmarshaller
)
{
  ONCRPCMessage<ONCRPCRequest>::unmarshal( unmarshaller );

  int32_t msg_type = unmarshaller.readInt32( "msg_type", 0 );
  if ( msg_type == 0 ) // CALL
  {
    uint32_t rpcvers = unmarshaller.readUint32( "rpcvers", 0 );
    if ( rpcvers == 2 )
    {
      unmarshaller.readUint32( "prog", 0 );
      unmarshaller.readUint32( "vers", 0 );
      uint32_t proc = unmarshaller.readUint32( "proc", 0 );

      unmarshaller.readUint32( "credential_auth_flavor", 0 );
      std::string credential_auth_body;
      unmarshaller.readString
      (
        "credential_auth_body",
        0,
        credential_auth_body
      );

      unmarshaller.readUint32( "verf_auth_flavor", 0 );
      uint32_t verf_auth_body_length
        = unmarshaller.readUint32( "credential_auth_body_length", 0 );
      if ( verf_auth_body_length > 0 )
        DebugBreak();

      yidl::runtime::auto_Struct body( get_body() );
      if ( body != NULL )
        unmarshaller.readStruct( "body", 0, *body );
      else
      {
        body = get_interface()->createRequest( proc ).release();
        if ( body != NULL )
        {
          unmarshaller.readStruct( "body", 0, *body );
          set_body( body );
        }
      }
    }
  }
}


// oncrpc_response.cpp
YIELD::ipc::ONCRPCResponse::ONCRPCResponse
(
  YIELD::concurrency::auto_Interface interface_
)
  : ONCRPCMessage<ONCRPCResponse>( interface_ )
{ }

YIELD::ipc::ONCRPCResponse::ONCRPCResponse
(
  YIELD::concurrency::auto_Interface interface_,
  uint32_t xid,
  yidl::runtime::auto_Struct body
)
  : ONCRPCMessage<ONCRPCResponse>( interface_, xid, body )
{ }

void YIELD::ipc::ONCRPCResponse::marshal
(
  yidl::runtime::Marshaller& marshaller
)
{
  ONCRPCMessage<ONCRPCResponse>::marshal( marshaller );
  marshaller.writeInt32( "msg_type", 0, 1 ); // MSG_REPLY
  marshaller.writeInt32( "reply_stat", 0, 0 ); // MSG_ACCEPTED
  marshaller.writeInt32( "verf_auth_flavor", 0, 0 );
  marshaller.writeInt32( "verf_authbody_length", 0, 0 );
  yidl::runtime::auto_Struct body( get_body() );
  if ( body != NULL )
  {
    if
    (
      body->get_type_id()
      != YIDL_RUNTIME_OBJECT_TYPE_ID( YIELD::concurrency::ExceptionResponse )
    )
    {
      marshaller.writeInt32( "accept_stat", 0, 0 ); // SUCCESS
      marshaller.writeStruct( "body", 0, *body );
    }
    else
      marshaller.writeInt32( "accept_stat", 0, 5 ); // SYSTEM_ERR
  }
  else
    marshaller.writeInt32( "accept_stat", 0, 5 ); // SYSTEM_ERR
}

void YIELD::ipc::ONCRPCResponse::unmarshal
(
  yidl::runtime::Unmarshaller& unmarshaller
)
{
  ONCRPCMessage<ONCRPCResponse>::unmarshal( unmarshaller );

  yidl::runtime::auto_Struct body( get_body() );

  int32_t msg_type = unmarshaller.readInt32( "msg_type", 0 );
  if ( msg_type == 1 ) // REPLY
  {
    uint32_t reply_stat = unmarshaller.readUint32( "reply_stat", 0 );
    if ( reply_stat == 0 ) // MSG_ACCEPTED
    {
      uint32_t verf_auth_flavor
        = unmarshaller.readUint32( "verf_auth_flavor", 0 );

      uint32_t verf_authbody_length
        = unmarshaller.readUint32( "verf_authbody_length", 0 );

      if ( verf_auth_flavor == 0 && verf_authbody_length == 0 )
      {
        uint32_t accept_stat = unmarshaller.readUint32( "accept_stat", 0 );

        switch ( accept_stat )
        {
          case 0:
          {
            if ( body != NULL )
              unmarshaller.readStruct( "body", 0, *body );
          }
          break;

          case 1: body = new ONCRPCProgramUnavailableError; break;
          case 2: body = new ONCRPCProgramMismatchError; break;
          case 3: body = new ONCRPCProcedureUnavailableError; break;
          case 4: body = new ONCRPCGarbageArgumentsError; break;
          case 5: body = new ONCRPCSystemError; break;

          default:
          {
            body = get_interface()
              ->createExceptionResponse( accept_stat ).release();

            if ( body != NULL )
              unmarshaller.readStruct( "body", 0, *body );
            else
              body = new ONCRPCSystemError;
          }
          break;
        }
      }
      else
        body = new YIELD::concurrency::ExceptionResponse
               ( "ONC-RPC: received unexpected verification body" );
    }
    else if ( reply_stat == 1 ) // MSG_REJECTED
      body = new ONCRPCMessageRejectedError;
    else // Unknown reply_stat value
      body = new ONCRPCMessageRejectedError;
  }
  else // Unknown msg_type value
    body = new ONCRPCMessageRejectedError;

  set_body( body );
}


// oncrpc_server.cpp
class YIELD::ipc::ONCRPCServer::AIOWriteControlBlock
  : public Socket::AIOWriteControlBlock
{
public:
  AIOWriteControlBlock( yidl::runtime::auto_Buffer buffer )
    : Socket::AIOWriteControlBlock( buffer )
  { }

  void onCompletion( size_t )
  { }

  void onError( uint32_t )
  {
    get_socket()->shutdown();
    get_socket()->close();
  }
};


class YIELD::ipc::ONCRPCServer::ONCRPCResponseTarget
  : public YIELD::concurrency::EventTarget
{
public:
  ONCRPCResponseTarget
  (
    YIELD::concurrency::auto_Interface interface_,
    auto_ONCRPCRequest oncrpc_request,
    auto_SocketAddress peername,
    auto_Socket socket_
  )
    : interface_( interface_ ),
      oncrpc_request( oncrpc_request ),
      peername( peername ),
      socket_( socket_ )
  { }

  // yidl::runtime::Object
  YIDL_RUNTIME_OBJECT_PROTOTYPES( ONCRPCServer::ONCRPCResponseTarget, 0 );

  // EventTarget
  void send( YIELD::concurrency::Event& ev )
  {
    ONCRPCResponse
      oncrpc_response( interface_, oncrpc_request->get_xid(), ev );

    if ( peername != NULL )
    {
      static_cast<UDPSocket*>( socket_.get() )->sendto
      (
        oncrpc_response.serialize(),
        peername
      );
    }
    else
    {
      socket_->aio_write
      (
        new AIOWriteControlBlock( oncrpc_response.serialize() )
      );
    }
  }

private:
  YIELD::concurrency::auto_Interface interface_;
  auto_ONCRPCRequest oncrpc_request;
  auto_SocketAddress peername;
  auto_Socket socket_;
};


class YIELD::ipc::ONCRPCServer::AIOReadControlBlock
  : public Socket::AIOReadControlBlock
{
public:
  AIOReadControlBlock
  (
    yidl::runtime::auto_Buffer buffer,
    YIELD::concurrency::auto_Interface interface_,
    auto_ONCRPCRequest oncrpc_request
  )
    : Socket::AIOReadControlBlock( buffer ),
      interface_( interface_ ),
      oncrpc_request( oncrpc_request )
  { }

  void onCompletion( size_t )
  {
    for ( ;; )
    {
      ssize_t deserialize_ret = oncrpc_request->deserialize( get_buffer() );

      if ( deserialize_ret == 0 )
      {
        yidl::runtime::auto_Struct oncrpc_request_body
          = oncrpc_request->get_body();
        YIELD::concurrency::Request* interface_request
          = interface_->checkRequest( *oncrpc_request_body );
        if ( interface_request != NULL )
        {
          oncrpc_request_body.release();

          interface_request->set_response_target
          (
            new ONCRPCResponseTarget
            (
              interface_,
              oncrpc_request,
              NULL,
              get_socket()
            )
          );

          interface_->send( *interface_request );
        }
        else
          DebugBreak();

        oncrpc_request = new ONCRPCRequest( interface_ );
      }
      else if ( deserialize_ret > 0 )
      {
        get_socket()->aio_read
        (
          new AIOReadControlBlock
          (
            new yidl::runtime::HeapBuffer( deserialize_ret ),
            interface_,
            oncrpc_request
          )
        );

        return;
      }
      else
      {
        get_socket()->shutdown();
        get_socket()->close();
        return;
      }
    }
  }

  void onError( uint32_t )
  {
    get_socket()->close();
  }

private:
  YIELD::concurrency::auto_Interface interface_;
  auto_ONCRPCRequest oncrpc_request;
};


class YIELD::ipc::ONCRPCServer::AIORecvFromControlBlock
  : public UDPSocket::AIORecvFromControlBlock
{
public:
  AIORecvFromControlBlock( YIELD::concurrency::auto_Interface interface_ )
    : UDPSocket::AIORecvFromControlBlock
      (
        new yidl::runtime::HeapBuffer( 1024 )
      ),
      interface_( interface_ )
  { }

  // AIOControlBlock
  void onCompletion( size_t )
  {
    ONCRPCRequest* oncrpc_request = new ONCRPCRequest( interface_ );
    ssize_t deserialize_ret = oncrpc_request->deserialize( get_buffer() );
    if ( deserialize_ret == 0 )
    {
      yidl::runtime::auto_Struct oncrpc_request_body
        = oncrpc_request->get_body();
      YIELD::concurrency::Request* interface_request
        = interface_->checkRequest( *oncrpc_request_body );
      if ( interface_request != NULL )
      {
        oncrpc_request_body.release();

        interface_request->set_response_target
        (
          new ONCRPCResponseTarget
          (
            interface_,
            oncrpc_request,
            get_peername(),
            get_socket()
          )
        );

        interface_->send( *interface_request );
      }

      static_cast<UDPSocket*>( get_socket().get() )->aio_recvfrom
      (
        new AIORecvFromControlBlock( interface_ )
      );
    }
    else if ( deserialize_ret < 0 )
      ONCRPCRequest::decRef( *oncrpc_request );
    else
      DebugBreak();
  }

  void onError( uint32_t )
  {
    // DebugBreak();
  }

private:
  YIELD::concurrency::auto_Interface interface_;
};


class YIELD::ipc::ONCRPCServer::AIOAcceptControlBlock
  : public TCPSocket::AIOAcceptControlBlock
{
public:
  AIOAcceptControlBlock( YIELD::concurrency::auto_Interface interface_ )
    : interface_( interface_ )
  { }

  void onCompletion( size_t )
  {
    get_accepted_tcp_socket()->aio_read
    (
      new AIOReadControlBlock
      (
        new yidl::runtime::HeapBuffer( 1024 ),
        interface_,
        new ONCRPCRequest( interface_ )
      )
    );

    static_cast<TCPSocket*>( get_socket().get() )->aio_accept
    (
      new AIOAcceptControlBlock( interface_ )
    );
  }

  void onError( uint32_t )
  { }

private:
  YIELD::concurrency::auto_Interface interface_;
};


YIELD::ipc::ONCRPCServer::ONCRPCServer
(
  YIELD::concurrency::auto_Interface interface_,
  auto_Socket socket_
)
  : interface_( interface_ ), socket_( socket_ )
{ }

YIELD::ipc::auto_ONCRPCServer
YIELD::ipc::ONCRPCServer::create
(
  const URI& absolute_uri,
  YIELD::concurrency::auto_Interface interface_,
  YIELD::platform::auto_Log log,
  auto_SSLContext ssl_context
)
{
  auto_SocketAddress sockname = SocketAddress::create( absolute_uri );

  if ( absolute_uri.get_scheme() == "oncrpcu" )
  {
    auto_UDPSocket udp_socket( UDPSocket::create() );
    if ( udp_socket != NULL &&
         udp_socket->bind( sockname ) )
    {
      udp_socket->aio_recvfrom( new AIORecvFromControlBlock( interface_ ) );
      return new ONCRPCServer( interface_, udp_socket.release() );
    }
  }
  else
  {
    auto_TCPSocket listen_tcp_socket;
#ifdef YIELD_IPC_HAVE_OPENSSL
    if ( absolute_uri.get_scheme() == "oncrpcs" && ssl_context != NULL )
      listen_tcp_socket = SSLSocket::create( ssl_context ).release();
    else
#endif
      listen_tcp_socket = TCPSocket::create();

    if
    (
      listen_tcp_socket != NULL &&
      listen_tcp_socket->bind( sockname ) &&
      listen_tcp_socket->listen()
    )
    {
      listen_tcp_socket->aio_accept( new AIOAcceptControlBlock( interface_ ) );
      return new ONCRPCServer( interface_, listen_tcp_socket.release() );
    }
  }

  throw YIELD::platform::Exception();
}


// pipe.cpp
#ifdef _WIN32
#include <windows.h>
#endif


void YIELD::ipc::Pipe::close()
{
#ifdef _WIN32
  if ( ends[0] != INVALID_HANDLE_VALUE )
  {
    CloseHandle( ends[0] );
    ends[0] = INVALID_HANDLE_VALUE;
  }

  if ( ends[1] != INVALID_HANDLE_VALUE )
  {
    CloseHandle( ends[1] );
    ends[1] = INVALID_HANDLE_VALUE;
  }
#else
  ::close( ends[0] );
  ::close( ends[1] );
#endif
}

YIELD::ipc::auto_Pipe YIELD::ipc::Pipe::create()
{
#ifdef _WIN32
  SECURITY_ATTRIBUTES pipe_security_attributes;
  pipe_security_attributes.nLength = sizeof( SECURITY_ATTRIBUTES );
  pipe_security_attributes.bInheritHandle = TRUE;
  pipe_security_attributes.lpSecurityDescriptor = NULL;
  void* ends[2];
  if ( CreatePipe( &ends[0], &ends[1], &pipe_security_attributes, 0 ) )
  {
    if
    (
      SetHandleInformation( ends[0], HANDLE_FLAG_INHERIT, 0 ) &&
      SetHandleInformation( ends[1], HANDLE_FLAG_INHERIT, 0 )
    )
      return new Pipe( ends );
    else
    {
      CloseHandle( ends[0] );
      CloseHandle( ends[1] );
    }
  }
#else
  int ends[2];
  if ( ::pipe( ends ) != -1 )
    return new Pipe( ends );
#endif

  throw YIELD::platform::Exception();
}

#ifdef _WIN32
YIELD::ipc::Pipe::Pipe( void* ends[2] )
#else
YIELD::ipc::Pipe::Pipe( int ends[2] )
#endif
{
  this->ends[0] = ends[0];
  this->ends[1] = ends[1];
}

YIELD::ipc::Pipe::~Pipe()
{
  close();
}

ssize_t YIELD::ipc::Pipe::read( void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  DWORD dwBytesRead;
  if
  (
    ReadFile
    (
      ends[0],
      buffer,
      static_cast<DWORD>( buffer_len ),
      &dwBytesRead,
      NULL
    )
  )
    return static_cast<ssize_t>( dwBytesRead );
  else
    return -1;
#else
  return ::read( ends[0], buffer, buffer_len );
#endif
}

bool YIELD::ipc::Pipe::set_blocking_mode( bool blocking )
{
#ifdef _WIN32
  return false;
#else
  int current_fcntl_flags = fcntl( ends[0], F_GETFL, 0 );
  if ( blocking )
  {
    if ( ( current_fcntl_flags & O_NONBLOCK ) == O_NONBLOCK )
    {
      return fcntl
             (
               ends[0],
               F_SETFL,
               current_fcntl_flags ^ O_NONBLOCK
             ) != -1
             &&
             fcntl
             (
               ends[1],
               F_SETFL,
               current_fcntl_flags ^ O_NONBLOCK
             ) != -1;
    }
    else
      return true;
  }
  else
  {
    return fcntl
           (
             ends[0],
             F_SETFL,
             current_fcntl_flags | O_NONBLOCK
           ) != -1
           &&
           fcntl
           (
             ends[1],
             F_SETFL,
             current_fcntl_flags | O_NONBLOCK
           ) != -1;
  }
#endif
}

ssize_t YIELD::ipc::Pipe::write( const void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  DWORD dwBytesWritten;
  if
  (
    WriteFile
    (
      ends[1],
      buffer,
      static_cast<DWORD>( buffer_len ),
      &dwBytesWritten,
      NULL
    )
  )
    return static_cast<ssize_t>( dwBytesWritten );
  else
    return -1;
#else
  return ::write( ends[1], buffer, buffer_len );
#endif
}


// process.cpp
#if defined(_WIN32)
#include <windows.h>
#else
#include <signal.h>
#include <sys/wait.h> // For waitpid
#endif


YIELD::ipc::auto_Process YIELD::ipc::Process::create
(
  const YIELD::platform::Path& command_line
)
{
#ifdef _WIN32
  auto_Pipe child_stdin, child_stdout, child_stderr;
  //auto_Pipe child_stdin = Pipe::create(),
  //                  child_stdout = Pipe::create(),
  //                  child_stderr = Pipe::create();

  STARTUPINFO startup_info;
  ZeroMemory( &startup_info, sizeof( STARTUPINFO ) );
  startup_info.cb = sizeof( STARTUPINFO );
  //startup_info.hStdInput = *child_stdin->get_input_stream()->get_file();
  //startup_info.hStdOutput = *child_stdout->get_output_stream()->get_file();
  //startup_info.hStdError = *child_stdout->get_output_stream()->get_file();
  //startup_info.dwFlags = STARTF_USESTDHANDLES;

  PROCESS_INFORMATION proc_info;
  ZeroMemory( &proc_info, sizeof( PROCESS_INFORMATION ) );

  if
  (
    CreateProcess
    (
      NULL,
      const_cast<wchar_t*>( static_cast<const wchar_t*>( command_line ) ),
      NULL,
      NULL,
      TRUE,
      CREATE_NO_WINDOW,
      NULL,
      NULL,
      &startup_info,
      &proc_info
    )
  )
  {
    return new Process
               (
                 proc_info.hProcess,
                 proc_info.hThread,
                 child_stdin,
                 child_stdout,
                 child_stderr
               );
  }
  else
    throw YIELD::platform::Exception();
#else
  const char* argv[] = { static_cast<const char*>( NULL ) };
  return create( command_line, argv );
#endif
}

YIELD::ipc::auto_Process YIELD::ipc::Process::create( int argc, char** argv )
{
  std::vector<char*> argvv;
  for ( int arg_i = 1; arg_i < argc; arg_i++ )
    argvv.push_back( argv[arg_i] );
  argvv.push_back( NULL );
  return create( argv[0], const_cast<const char**>( &argvv[0] ) );
}

YIELD::ipc::auto_Process YIELD::ipc::Process::create
(
  const YIELD::platform::Path& executable_file_path,
  const char** null_terminated_argv
)
{
#ifdef _WIN32
  const std::string& executable_file_path_str
    = static_cast<const std::string&>( executable_file_path );

  std::string command_line;
  if ( executable_file_path_str.find( ' ' ) == -1 )
    command_line.append( executable_file_path_str );
  else
  {
    command_line.append( "\"", 1 );
    command_line.append( executable_file_path_str );
    command_line.append( "\"", 1 );
  }

  size_t arg_i = 0;
  while ( null_terminated_argv[arg_i] != NULL )
  {
    command_line.append( " ", 1 );
    command_line.append( null_terminated_argv[arg_i] );
    arg_i++;
  }

  return create( command_line );
#else
  auto_Pipe child_stdin, child_stdout, child_stderr;
  //auto_Pipe child_stdin = Pipe::create(),
  //                  child_stdout = Pipe::create(),
  //                  child_stderr = Pipe::create();

  pid_t child_pid = fork();
  if ( child_pid == -1 )
    throw YIELD::platform::Exception();
  else if ( child_pid == 0 ) // Child
  {
    //close( STDIN_FILENO );
    // Set stdin to read end of stdin pipe
    //dup2( *child_stdin->get_input_stream()->get_file(), STDIN_FILENO );

    //close( STDOUT_FILENO );
    // Set stdout to write end of stdout pipe
    //dup2( *child_stdout->get_output_stream()->get_file(), STDOUT_FILENO );

    //close( STDERR_FILENO );
    // Set stderr to write end of stderr pipe
    //dup2( *child_stderr->get_output_stream()->get_file(), STDERR_FILENO );

    std::vector<char*> argv_with_executable_file_path;
    argv_with_executable_file_path.push_back
    (
      const_cast<char*>( static_cast<const char*>( executable_file_path ) )
    );
    size_t arg_i = 0;
    while ( null_terminated_argv[arg_i] != NULL )
    {
      argv_with_executable_file_path.push_back
      (
        const_cast<char*>( null_terminated_argv[arg_i] )
      );
      arg_i++;
    }
    argv_with_executable_file_path.push_back( NULL );

    execv( executable_file_path, &argv_with_executable_file_path[0] );
    return NULL; // Should never be reached
  }
  else // Parent
    return new Process( child_pid, child_stdin, child_stdout, child_stderr );
#endif
}

#ifdef _WIN32
YIELD::ipc::Process::Process
(
  HANDLE hChildProcess,
  HANDLE hChildThread,
  auto_Pipe child_stdin,
  auto_Pipe child_stdout,
  auto_Pipe child_stderr
)
  : hChildProcess( hChildProcess ),
    hChildThread( hChildThread ),
#else
YIELD::ipc::Process::Process
(
  pid_t child_pid,
  auto_Pipe child_stdin,
  auto_Pipe child_stdout,
  auto_Pipe child_stderr
)
  : child_pid( child_pid ),
#endif
    child_stdin( child_stdin ),
    child_stdout( child_stdout ),
    child_stderr( child_stderr )
{ }

YIELD::ipc::Process::~Process()
{
#ifdef _WIN32
  CloseHandle( hChildProcess );
  CloseHandle( hChildThread );
#endif
}

unsigned long YIELD::ipc::Process::getpid()
{
#ifdef _WIN32
  return GetCurrentProcessId();
#else
  return ::getpid();
#endif
}

bool YIELD::ipc::Process::kill()
{
#ifdef _WIN32
  return TerminateProcess( hChildProcess, 0 ) == TRUE;
#else
  return ::kill( child_pid, SIGKILL ) == 0;
#endif
}

bool YIELD::ipc::Process::poll( int* out_return_code )
{
#ifdef _WIN32
  if ( WaitForSingleObject( hChildProcess, 0 ) != WAIT_TIMEOUT )
  {
    if ( out_return_code )
    {
      DWORD dwChildExitCode;
      GetExitCodeProcess( hChildProcess, &dwChildExitCode );
      *out_return_code = ( int )dwChildExitCode;
    }

    return true;
  }
  else
    return false;
#else
  if ( waitpid( child_pid, out_return_code, WNOHANG ) > 0 )
  {
    // "waitpid() was successful. The value returned indicates the process ID
    // of the child process whose status information was recorded in the
    // storage pointed to by stat_loc."
#ifdef __FreeBSD__
    if ( WIFEXITED( *out_return_code ) ) // Child exited normally
    {
      *out_return_code = WEXITSTATUS( *out_return_code );
#else
    if ( WIFEXITED( out_return_code ) ) // Child exited normally
    {
      *out_return_code = WEXITSTATUS( out_return_code );
#endif
      return true;
    }
    else
      return false;
  }
  // 0 = WNOHANG was specified on the options parameter, but no child process
  // was immediately available.
  // -1 = waitpid() was not successful. The errno value is set
  // to indicate the error.
  else
    return false;
#endif
}

bool YIELD::ipc::Process::terminate()
{
#ifdef _WIN32
  return TerminateProcess( hChildProcess, 0 ) == TRUE;
#else
  return ::kill( child_pid, SIGTERM ) == 0;
#endif
}

int YIELD::ipc::Process::wait()
{
#ifdef _WIN32
  WaitForSingleObject( hChildProcess, INFINITE );
  DWORD dwChildExitCode;
  GetExitCodeProcess( hChildProcess, &dwChildExitCode );
  return ( int )dwChildExitCode;
#else
  int stat_loc;
  if ( waitpid( child_pid, &stat_loc, 0 ) >= 0 )
    return stat_loc;
  else
    return -1;
#endif
}


// rfc822_headers.cpp
YIELD::ipc::RFC822Headers::RFC822Headers( uint8_t reserve_iovecs_count )
{
  deserialize_state = DESERIALIZING_LEADING_WHITESPACE;
  buffer_p = stack_buffer;
  heap_buffer = NULL;
  heap_buffer_len = 0;
  heap_iovecs = NULL;
  for ( uint8_t iovec_i = 0; iovec_i < reserve_iovecs_count; iovec_i++ )
    memset( &stack_iovecs[iovec_i], 0, sizeof( stack_iovecs[iovec_i] ) );
  iovecs_filled = reserve_iovecs_count;
}

YIELD::ipc::RFC822Headers::~RFC822Headers()
{
  delete [] heap_buffer;
}

void YIELD::ipc::RFC822Headers::allocateHeapBuffer()
{
  if ( heap_buffer_len == 0 )
  {
    heap_buffer = new char[512];
    heap_buffer_len = 512;
    memcpy_s
    (
      heap_buffer,
      heap_buffer_len,
      stack_buffer,
      buffer_p - stack_buffer
    );
    buffer_p = heap_buffer + ( buffer_p - stack_buffer );
  }
  else
  {
    heap_buffer_len += 512;
    char* new_heap_buffer = new char[heap_buffer_len];
    memcpy_s
    (
      new_heap_buffer,
      heap_buffer_len,
      heap_buffer,
      buffer_p - heap_buffer
    );
    buffer_p = new_heap_buffer + ( buffer_p - heap_buffer );
    delete [] heap_buffer;
    heap_buffer = new_heap_buffer;
  }
}

ssize_t
YIELD::ipc::RFC822Headers::deserialize
(
  yidl::runtime::auto_Buffer buffer
)
{
  for ( ;; )
  {
    switch ( deserialize_state )
    {
      case DESERIALIZING_LEADING_WHITESPACE:
      {
        char c;
        for ( ;; )
        {
          if ( buffer->get( &c, 1 ) == 1 )
          {
            if ( isspace( c ) )
              continue;
            else
            {
              *buffer_p = c;
              buffer_p++; // Don't need to check the end of the buffer here
              deserialize_state = DESERIALIZING_HEADER_NAME;
              break;
            }
          }
          else
            return 1;
        }
      }
      // Fall through

      case DESERIALIZING_HEADER_NAME:
      {
        char c;
        if ( buffer->get( &c, 1 ) == 1 )
        {
          switch ( c )
          {
            case '\r':
            case '\n':
            {
              deserialize_state = DESERIALIZING_TRAILING_CRLF;
            }
            continue;

            // TODO: support folded lines here (look for isspace( c ),
            // if so it's an extension of the previous line

            default:
            {
              *buffer_p = c;
              advanceBufferPointer();

              for ( ;; )
              {
                if ( buffer->get( buffer_p, 1 ) )
                {
                  if ( *buffer_p == ':' )
                  {
                    *buffer_p = 0;
                    advanceBufferPointer();
                    deserialize_state
                      = DESERIALIZING_HEADER_NAME_VALUE_SEPARATOR;
                    break;
                  }
                  else
                    advanceBufferPointer();
                }
                else
                  return 1;
              }
            }
            break;
          }
        }
        else
          return 1;
      }
      // Fall through

      case DESERIALIZING_HEADER_NAME_VALUE_SEPARATOR:
      {
        char c;
        for ( ;; )
        {
          if ( buffer->get( &c, 1 ) == 1 )
          {
            if ( isspace( c ) )
              continue;
            else
            {
              *buffer_p = c;
              advanceBufferPointer();
              deserialize_state = DESERIALIZING_HEADER_VALUE;
              break;
            }
          }
          else
            return 1;
        }
      }
      // Fall through

      case DESERIALIZING_HEADER_VALUE:
      {
        for ( ;; )
        {
          if ( buffer->get( buffer_p, 1 ) == 1 )
          {
            if ( *buffer_p == '\r' )
            {
              *buffer_p = 0;
              advanceBufferPointer();
              deserialize_state = DESERIALIZING_HEADER_VALUE_TERMINATOR;
              break;
            }
            else
              advanceBufferPointer();
          }
          else
            return 1;
        }
      }
      // Fall through

      case DESERIALIZING_HEADER_VALUE_TERMINATOR:
      {
        char c;
        for ( ;; )
        {
          if ( buffer->get( &c, 1 ) == 1 )
          {
            if ( c == '\n' )
            {
              deserialize_state = DESERIALIZING_HEADER_NAME;
              break;
            }
          }
          else
            return 1;
        }
      }
      continue; // To the next header name

      case DESERIALIZING_TRAILING_CRLF:
      {
        char c;
        for ( ;; )
        {
          if ( buffer->get( &c, 1 ) == 1 )
          {
            if ( c == '\n' )
            {
              *buffer_p = 0;

              // Fill the iovecs so get_header will work
              // TODO: do this as we're parsing
              const char* temp_buffer_p;
              if ( heap_buffer != NULL )
                temp_buffer_p = heap_buffer;
              else
                temp_buffer_p = stack_buffer;

              while ( temp_buffer_p < buffer_p )
              {
                const char* header_name = temp_buffer_p;
                size_t header_name_len = strnlen( header_name, UINT16_MAX );
                temp_buffer_p += header_name_len + 1;
                const char* header_value = temp_buffer_p;
                size_t header_value_len = strnlen( header_value, UINT16_MAX );
                temp_buffer_p += header_value_len + 1;
                set_next_iovec( header_name, header_name_len );
                set_next_iovec( ": ", 2 );
                set_next_iovec( header_value, header_value_len );
                set_next_iovec( "\r\n", 2 );
              }

              deserialize_state = DESERIALIZE_DONE;
              return 0;
            }
          }
          else
            return 1;
        }

        case DESERIALIZE_DONE: return 0;
      }
    } // switch
  } // for ( ;; )
}

char* YIELD::ipc::RFC822Headers::get_header
(
  const char* header_name,
  const char* default_value
)
{
  size_t header_name_len = strnlen( header_name, UINT16_MAX );
  struct iovec* iovecs = heap_iovecs != NULL ? heap_iovecs : stack_iovecs;
  for ( uint16_t iovec_i = 0; iovec_i < iovecs_filled; iovec_i += 4 )
  {
    if ( iovecs[iovec_i].iov_len == header_name_len )
    {
      if
      (
        strncmp
        (
          static_cast<const char*>( iovecs[iovec_i].iov_base ),
          header_name,
          header_name_len
        ) == 0
      )
        return static_cast<char*>( iovecs[iovec_i+2].iov_base );
    }
  }
  return const_cast<char*>( default_value );
}

yidl::runtime::auto_Buffer YIELD::ipc::RFC822Headers::serialize()
{
  if ( heap_iovecs != NULL )
    return new GatherBuffer( heap_iovecs, iovecs_filled );
  else
    return new GatherBuffer( stack_iovecs, iovecs_filled );
}

//void RFC822Headers::set_header( const char* header, size_t header_len )
//{
//  DebugBreak(); // TODO: Separate header name and value
//  /*
//  if ( header[header_len-1] != '\n' )
//  {
//    copy_iovec( header, header_len );
//    set_next_iovec( "\r\n", 2 );
//  }
//  else
//    copy_iovec( header, header_len );
//    */
//}

void YIELD::ipc::RFC822Headers::set_header
(
  const char* header_name,
  const char* header_value
)
{
  set_next_iovec( header_name, strnlen( header_name, UINT16_MAX ) );
  set_next_iovec( ": ", 2 );
  set_next_iovec( header_value, strnlen( header_value, UINT16_MAX ) );
  set_next_iovec( "\r\n", 2 );
}

void YIELD::ipc::RFC822Headers::set_header
(
  const char* header_name,
  char* header_value
)
{
  set_next_iovec( header_name, strnlen( header_name, UINT16_MAX ) );
  set_next_iovec( ": ", 2 );
  set_next_iovec( header_value, strnlen( header_value, UINT16_MAX ) );
  set_next_iovec( "\r\n", 2 );
}

void YIELD::ipc::RFC822Headers::set_header
(
  char* header_name,
  char* header_value
)
{
  set_next_iovec( header_name, strnlen( header_name, UINT16_MAX ) );
  set_next_iovec( ": ", 2 );
  set_next_iovec( header_value, strnlen( header_value, UINT16_MAX ) );
  set_next_iovec( "\r\n", 2 );
}

void YIELD::ipc::RFC822Headers::set_header
(
  const std::string& header_name,
  const std::string& header_value
)
{
  set_next_iovec
  (
    const_cast<char*>( header_name.c_str() ),
    header_name.size()
  ); // Copy

  set_next_iovec( ": ", 2 );

  set_next_iovec
  (
    const_cast<char*>( header_value.c_str() ),
    header_value.size()
  ); // Copy

  set_next_iovec( "\r\n", 2 );
}

void YIELD::ipc::RFC822Headers::set_iovec
(
  uint8_t iovec_i,
  const char* data,
  size_t len
)
{
  struct iovec _iovec;
  _iovec.iov_base = const_cast<char*>( data );
  _iovec.iov_len = len;
  if ( heap_iovecs == NULL )
  {
    stack_iovecs[iovec_i].iov_base = const_cast<char*>( data );
    stack_iovecs[iovec_i].iov_len = len;
  }
  else
  {
    heap_iovecs[iovec_i].iov_base = const_cast<char*>( data );
    heap_iovecs[iovec_i].iov_len = len;
  }
}

void YIELD::ipc::RFC822Headers::set_next_iovec( char* data, size_t len )
{
  if ( heap_buffer == NULL )
  {
    if
    (
      ( buffer_p + len - stack_buffer )
        > YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH
    )
    {
      heap_buffer = new char[len];
      heap_buffer_len = len;
      // Don't need to copy anything from the stack buffer or change pointers,
      // since we're not deleting that memory or parsing over it again
      buffer_p = heap_buffer;
    }
  }
  else if
  (
    static_cast<size_t>( buffer_p + len - heap_buffer )
      > heap_buffer_len
  )
  {
    heap_buffer_len += len;
    char* new_heap_buffer = new char[heap_buffer_len];
    memcpy_s
    (
      new_heap_buffer,
      heap_buffer_len,
      heap_buffer,
      buffer_p - heap_buffer
    );
    // Since we're copying the old heap_buffer and deleting its contents
    // we need to adjust the pointers in all iovecs
    struct iovec* iovecs;
    if ( heap_iovecs != NULL )
      iovecs = heap_iovecs;
    else
      iovecs = stack_iovecs;

    for ( uint8_t iovec_i = 0; iovec_i < iovecs_filled; iovec_i++ )
    {
      if
      (
        iovecs[iovec_i].iov_base >= heap_buffer
        &&
        iovecs[iovec_i].iov_base <= buffer_p
      )
      {
        iovecs[iovec_i].iov_base
          = new_heap_buffer +
            ( static_cast<char*>( iovecs[iovec_i].iov_base ) - heap_buffer );
      }
    }
    buffer_p = new_heap_buffer + ( buffer_p - heap_buffer );
    delete [] heap_buffer;
    heap_buffer = new_heap_buffer;
  }

  const char* buffer_p_before = buffer_p;
  memcpy_s( buffer_p, len, data, len );
  buffer_p += len;
  if ( data[len-1] == 0 ) len--;
  set_next_iovec( buffer_p_before, len );
}

void YIELD::ipc::RFC822Headers::set_next_iovec( const char* data, size_t len )
{
  struct iovec _iovec;
  _iovec.iov_base = const_cast<char*>( data );
  _iovec.iov_len = len;
  set_next_iovec( _iovec );
}

void YIELD::ipc::RFC822Headers::set_next_iovec( const struct iovec& iovec )
{
  if ( heap_iovecs == NULL )
  {
    if ( iovecs_filled < YIELD_RFC822_HEADERS_STACK_IOVECS_LENGTH )
      stack_iovecs[iovecs_filled] = iovec;
    else
    {
      heap_iovecs = new struct iovec[UINT8_MAX];
      memcpy_s
      (
        heap_iovecs,
        sizeof( struct iovec ) * UINT8_MAX,
        stack_iovecs,
        sizeof( stack_iovecs )
      );
      heap_iovecs[iovecs_filled] = iovec;
    }
  }
  else if ( iovecs_filled < UINT8_MAX )
    heap_iovecs[iovecs_filled] = iovec;
  else
    return;

  iovecs_filled++;
}


// socket.cpp
#ifdef _WIN32
#pragma warning( 4 : 4365 )
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#pragma comment( lib, "ws2_32.lib" )
#else
#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <signal.h>
#include <sys/socket.h>
#include <unistd.h>
#endif


YIELD::ipc::Socket::AIOQueue* YIELD::ipc::Socket::aio_queue = NULL;


YIELD::ipc::Socket::AIOControlBlock::ExecuteStatus
YIELD::ipc::Socket::AIOConnectControlBlock::execute()
{
  if ( get_socket()->connect( get_peername() ) )
    onCompletion( 0 );
  else if ( get_socket()->want_connect() )
    return EXECUTE_STATUS_WANT_WRITE;
  else
#ifdef _WIN32
    onError( WSAGetLastError() );
#else
    onError( errno );
#endif

  return EXECUTE_STATUS_DONE;
}


YIELD::ipc::Socket::AIOControlBlock::ExecuteStatus
YIELD::ipc::Socket::AIOReadControlBlock::execute()
{
  ssize_t read_ret = get_socket()->read( buffer );
  if ( read_ret > 0 )
    onCompletion( static_cast<size_t>( read_ret ) );
  else if ( read_ret == 0 )
#ifdef _WIN32
    onError( WSAECONNABORTED );
#else
    onError( ECONNABORTED );
#endif
  else if ( get_socket()->want_read() )
    return EXECUTE_STATUS_WANT_READ;
  else if ( get_socket()->want_write() )
    return EXECUTE_STATUS_WANT_WRITE;
  else
#ifdef _WIN32
    onError( WSAGetLastError() );
#else
    onError( errno );
#endif

  return EXECUTE_STATUS_DONE;
}


YIELD::ipc::Socket::AIOControlBlock::ExecuteStatus
YIELD::ipc::Socket::AIOWriteControlBlock::execute()
{
  ssize_t write_ret = get_socket()->write( get_buffer() );

  if ( write_ret >= 0 )
    onCompletion( static_cast<size_t>( write_ret ) );
  else if ( get_socket()->want_write() )
    return EXECUTE_STATUS_WANT_WRITE;
  else if ( get_socket()->want_read() )
    return EXECUTE_STATUS_WANT_READ;
  else
#ifdef _WIN32
    onError( WSAGetLastError() );
#else
    onError( errno );
#endif

  return EXECUTE_STATUS_DONE;
}


YIELD::ipc::Socket::Socket
(
  int domain,
  int type,
  int protocol,
#ifdef _WIN32
  SOCKET socket_
#else
  int socket_
#endif
)
: domain( domain ), type( type ), protocol( protocol ), socket_( socket_ )
{
  blocking_mode = true;
  connected = false;
}

YIELD::ipc::Socket::~Socket()
{
  close();
}

void YIELD::ipc::Socket::aio_connect
(
  auto_AIOConnectControlBlock aio_connect_control_block
)
{
  aio_connect_nbio( aio_connect_control_block );
}

void YIELD::ipc::Socket::aio_connect_nbio
(
  auto_AIOConnectControlBlock aio_connect_control_block
)
{
  aio_connect_control_block->set_socket( *this );

  set_blocking_mode( false );
  if ( connect( aio_connect_control_block->get_peername() ) )
    aio_connect_control_block->onCompletion( 0 );
  else if ( want_connect() )
    get_aio_queue().submit( aio_connect_control_block.release() );
  else
#ifdef _WIN32
    aio_connect_control_block->onError( WSAGetLastError() );
#else
    aio_connect_control_block->onError( errno );
#endif
}

void YIELD::ipc::Socket::aio_read
(
  auto_AIOReadControlBlock aio_read_control_block
)
{
#ifdef _WIN32
  aio_read_iocp( aio_read_control_block );
#else
  aio_read_nbio( aio_read_control_block );
#endif
}

#ifdef _WIN32
void YIELD::ipc::Socket::aio_read_iocp
(
  auto_AIOReadControlBlock aio_read_control_block
)
{
  aio_read_control_block->set_socket( *this );
  get_aio_queue().associate( *this );

  yidl::runtime::auto_Buffer buffer( aio_read_control_block->get_buffer() );
  WSABUF wsabuf[1];
  wsabuf[0].buf = static_cast<char*>( *buffer ) + buffer->size();
  wsabuf[0].len = static_cast<ULONG>( buffer->capacity() - buffer->size() );
  DWORD dwNumberOfBytesReceived, dwFlags = 0;
  if
  (
    WSARecv
    (
      socket_,
      wsabuf,
      1,
      &dwNumberOfBytesReceived,
      &dwFlags,
      *aio_read_control_block,
      NULL
    ) == 0
    ||
    WSAGetLastError() == WSA_IO_PENDING
  )
    aio_read_control_block.release();
  else
    aio_read_control_block->onError( WSAGetLastError() );
}
#endif

void YIELD::ipc::Socket::aio_read_nbio
(
  auto_AIOReadControlBlock aio_read_control_block
)
{
  aio_read_control_block->set_socket( *this );
  set_blocking_mode( false );
  ssize_t read_ret = read( aio_read_control_block->get_buffer() );
  if ( read_ret > 0 )
    aio_read_control_block->onCompletion( static_cast<size_t>( read_ret ) );
  else if ( read_ret == 0 )
#ifdef _WIN32
    aio_read_control_block->onError( WSAECONNRESET );
#else
    aio_read_control_block->onError( ECONNRESET );
#endif
  else if ( want_read() || want_write() )
    get_aio_queue().submit( aio_read_control_block.release() );
  else
#ifdef _WIN32
    aio_read_control_block->onError( WSAGetLastError() );
#else
    aio_read_control_block->onError( errno );
#endif
}

void YIELD::ipc::Socket::aio_write
(
  auto_AIOWriteControlBlock aio_write_control_block
)
{
#ifdef _WIN32
  aio_write_iocp( aio_write_control_block );
#else
  aio_write_nbio( aio_write_control_block );
#endif
}

#ifdef _WIN32
void YIELD::ipc::Socket::aio_write_iocp
(
  auto_AIOWriteControlBlock aio_write_control_block
)
{
  aio_write_control_block->set_socket( *this );
  get_aio_queue().associate( *this );

  yidl::runtime::auto_Buffer buffer( aio_write_control_block->get_buffer() );
  if ( buffer->get_type_id() == YIDL_RUNTIME_OBJECT_TYPE_ID( GatherBuffer ) )
  {
    DWORD dwNumberOfBytesSent;
#ifdef _WIN64
  // See note in writev re: the logic behind this
  const struct iovec* iovecs
    = static_cast<GatherBuffer*>( buffer.get() )->get_iovecs();
  uint32_t iovecs_len
    = static_cast<GatherBuffer*>( buffer.get() )->get_iovecs_len();
  std::vector<WSABUF> wsabufs( iovecs_len );
  for ( uint32_t iovec_i = 0; iovec_i < iovecs_len; iovec_i++ )
  {
    wsabufs[iovec_i].len = static_cast<ULONG>( iovecs[iovec_i].iov_len );
    wsabufs[iovec_i].buf = static_cast<char*>( iovecs[iovec_i].iov_base );
  }
  if
  (
    WSASend
    (
      socket_,
      &wsabufs[0],
      iovecs_len,
      &dwNumberOfBytesSent,
      0,
      *aio_write_control_block,
      NULL
    ) == 0
    ||
#else
    if
    (
      WSASend
      (
        socket_,
        reinterpret_cast<WSABUF*>
        (
          const_cast<struct iovec*>
          (
            static_cast<GatherBuffer*>( buffer.get() )->get_iovecs()
          )
        ),
        static_cast<GatherBuffer*>( buffer.get() )->get_iovecs_len(),
        &dwNumberOfBytesSent,
        0,
        *aio_write_control_block,
        NULL
      ) == 0
      ||
#endif
      WSAGetLastError() == WSA_IO_PENDING
    )
      aio_write_control_block.release();
    else
      aio_write_control_block->onError( WSAGetLastError() );
  }
  else
  {
    WSABUF wsabuf[1];
    wsabuf[0].buf = static_cast<char*>( *buffer );
    wsabuf[0].len = static_cast<ULONG>( buffer->size() );
    DWORD dwNumberOfBytesSent;
    if
    (
      WSASend
      (
        socket_,
        wsabuf,
        1,
        &dwNumberOfBytesSent,
        0,
        *aio_write_control_block,
        NULL
      ) == 0
      ||
      WSAGetLastError() == WSA_IO_PENDING
    )
      aio_write_control_block.release();
    else
      aio_write_control_block->onError( WSAGetLastError() );
  }
}
#endif

void YIELD::ipc::Socket::aio_write_nbio
(
  auto_AIOWriteControlBlock aio_write_control_block
)
{
  aio_write_control_block->set_socket( *this );
  set_blocking_mode( false );
  ssize_t write_ret = write( aio_write_control_block->get_buffer() );
  if ( write_ret >= 0 )
    aio_write_control_block->onCompletion( static_cast<size_t>( write_ret ) );
  else if ( want_write() || want_read() )
    get_aio_queue().submit( aio_write_control_block.release() );
  else
#ifdef _WIN32
    aio_write_control_block->onError( WSAGetLastError() );
#else
    aio_write_control_block->onError( errno );
#endif
}

bool YIELD::ipc::Socket::bind( auto_SocketAddress to_sockaddr )
{
  for ( ;; )
  {
    struct sockaddr* name; socklen_t namelen;
    if ( to_sockaddr->as_struct_sockaddr( domain, name, namelen ) )
    {
      if ( ::bind( *this, name, namelen ) != -1 )
        return true;
    }

    if ( domain == AF_INET6 &&
#ifdef _WIN32
        WSAGetLastError() == WSAEAFNOSUPPORT )
#else
        errno == EAFNOSUPPORT )
#endif
    {
      if ( recreate( AF_INET ) )
        continue;
      else
        return false;
    }
    else
      return false;
  }
}

bool YIELD::ipc::Socket::close()
{
#ifdef _WIN32
  if ( socket_ != INVALID_SOCKET )
  {
    if ( closesocket( socket_ ) != SOCKET_ERROR )
    {
      connected = false;
      socket_ = INVALID_SOCKET;
      return true;
    }
    else
      return false;
  }
#else
  if ( socket_ != -1 )
  {
    if ( ::close( socket_ ) != -1 )
    {
      connected = false;
      socket_ = -1;
      return true;
    }
    else
      return false;
  }
#endif
  else
    return true;
}

bool YIELD::ipc::Socket::connect( auto_SocketAddress to_sockaddr )
{
  if ( !connected )
  {
    for ( ;; )
    {
      struct sockaddr* name; socklen_t namelen;
      if ( to_sockaddr->as_struct_sockaddr( domain, name, namelen ) )
      {
        if ( ::connect( *this, name, namelen ) != -1 )
        {
          connected = true;
          return true;
        }
        else
        {
#ifdef _WIN32
          switch ( WSAGetLastError() )
          {
            case WSAEISCONN: connected = true; return true;
            case WSAEAFNOSUPPORT:
#else
          switch ( errno )
          {
            case EISCONN: connected = true; return true;
            case EAFNOSUPPORT:
#endif
            {
              if ( domain == AF_INET6 &&
                   recreate( AF_INET ) )
                continue;
              else
                return false;
            }
            break;

            default: return false;
          }
        }
      }
      else if ( domain == AF_INET6 &&
                recreate( AF_INET ) )
        continue;
      else
        return false;
    }
  }
  else
    return true;
}

#ifdef _WIN32
SOCKET YIELD::ipc::Socket::create( int& domain, int type, int protocol )
#else
int YIELD::ipc::Socket::create( int& domain, int type, int protocol )
#endif
{
#ifdef _WIN32
  SOCKET socket_ = ::socket( domain, type, protocol );
  if ( socket_ != INVALID_SOCKET )
  {
    if ( domain == AF_INET6 )
    {
      DWORD ipv6only = 0; // Allow dual-mode sockets
      setsockopt
      (
        socket_,
        IPPROTO_IPV6,
        IPV6_V6ONLY,
        ( char* )&ipv6only,
        sizeof( ipv6only )
      );
    }
  }
  else if ( domain == AF_INET6 && WSAGetLastError() == WSAEAFNOSUPPORT )
  {
    domain = AF_INET;
    socket_ = ::socket( AF_INET, type, protocol );
    if ( socket_ == INVALID_SOCKET )
      return INVALID_SOCKET;
  }
  else
    return INVALID_SOCKET;

  return socket_;
#else
  int socket_ = ::socket( AF_INET6, type, protocol );
  if ( socket_ != -1 )
    return socket_;
  else if ( domain == AF_INET6 && errno == EAFNOSUPPORT )
  {
    domain = AF_INET;
    return ::socket( AF_INET, type, protocol );
  }
  else
    return -1;
#endif
}

void YIELD::ipc::Socket::destroy()
{
  delete aio_queue;
#ifdef _WIN32
  WSACleanup();
#endif
}

YIELD::ipc::Socket::AIOQueue& YIELD::ipc::Socket::get_aio_queue()
{
  if ( aio_queue == NULL )
    aio_queue = new AIOQueue;
  return *aio_queue;
}

bool YIELD::ipc::Socket::get_blocking_mode() const
{
  return blocking_mode;
}

std::string YIELD::ipc::Socket::getfqdn()
{
#ifdef _WIN32
  DWORD dwFQDNLength = 0;
  GetComputerNameExA( ComputerNameDnsHostname, NULL, &dwFQDNLength );
  if ( dwFQDNLength > 0 )
  {
    char* fqdn_temp = new char[dwFQDNLength];
    if
    (
      GetComputerNameExA
      (
        ComputerNameDnsFullyQualified,
        fqdn_temp,
        &dwFQDNLength
      )
    )
    {
      std::string fqdn( fqdn_temp, dwFQDNLength );
      delete [] fqdn_temp;
      return fqdn;
    }
    else
      delete [] fqdn_temp;
  }

  return std::string();
#else
  char fqdn[256];
  ::gethostname( fqdn, 256 );
  char* first_dot = strstr( fqdn, "." );
  if ( first_dot != NULL ) *first_dot = 0;

  // getnameinfo does not return aliases, which means we get "localhost"
  // on Linux if that's the first
  // entry for 127.0.0.1 in /etc/hosts

#ifndef __sun
  char domainname[256];
  // getdomainname is not a public call on Solaris, apparently
  if ( getdomainname( domainname, 256 ) == 0 &&
       domainname[0] != 0 &&
       strcmp( domainname, "(none)" ) != 0 &&
       strcmp( domainname, fqdn ) != 0 &&
       strstr( domainname, "localdomain" ) == NULL )
         strcat( fqdn, domainname );
  else
  {
#endif
    // Try gethostbyaddr, like Python
    uint32_t local_host_addr = inet_addr( "127.0.0.1" );
    struct hostent* hostents
      = gethostbyaddr
      (
        reinterpret_cast<char*>( &local_host_addr ),
        sizeof( uint32_t ),
        AF_INET
      );

    if ( hostents != NULL )
    {
      if
      (
        strchr( hostents->h_name, '.' ) != NULL &&
        strstr( hostents->h_name, "localhost" ) == NULL
      )
      {
        strncpy( fqdn, hostents->h_name, 256 );
      }
      else
      {
        for ( unsigned char i = 0; hostents->h_aliases[i] != NULL; i++ )
        {
          if
          (
            strchr( hostents->h_aliases[i], '.' ) != NULL &&
            strstr( hostents->h_name, "localhost" ) == NULL
          )
          {
            strncpy( fqdn, hostents->h_aliases[i], 256 );
            break;
          }
        }
      }
    }
#ifndef __sun
  }
#endif
  return fqdn;
#endif
}

std::string YIELD::ipc::Socket::gethostname()
{
#ifdef _WIN32
  DWORD dwHostNameLength = 0;
  GetComputerNameExA( ComputerNameDnsHostname, NULL, &dwHostNameLength );
  if ( dwHostNameLength > 0 )
  {
    char* hostname_temp = new char[dwHostNameLength];
    if
    (
      GetComputerNameExA
      (
        ComputerNameDnsHostname,
        hostname_temp,
        &dwHostNameLength
      )
    )
    {
      std::string hostname( hostname_temp, dwHostNameLength );
      delete [] hostname_temp;
      return hostname;
    }
    else
      delete [] hostname_temp;
  }

  return std::string();
#else
  char hostname[256];
  ::gethostname( hostname, 256 );
  return hostname;
#endif
}

YIELD::ipc::auto_SocketAddress YIELD::ipc::Socket::getpeername()
{
  struct sockaddr_storage peername_sockaddr_storage;
  memset( &peername_sockaddr_storage, 0, sizeof( peername_sockaddr_storage ) );
  socklen_t peername_sockaddr_storage_len = sizeof( peername_sockaddr_storage );
  if
  (
    ::getpeername
    (
      *this,
      reinterpret_cast<struct sockaddr*>( &peername_sockaddr_storage ),
      &peername_sockaddr_storage_len
    ) != -1
  )
    return new SocketAddress( peername_sockaddr_storage );
  else
    return NULL;
}

YIELD::ipc::auto_SocketAddress YIELD::ipc::Socket::getsockname()
{
  struct sockaddr_storage sockname_sockaddr_storage;
  memset( &sockname_sockaddr_storage, 0, sizeof( sockname_sockaddr_storage ) );
  socklen_t sockname_sockaddr_storage_len = sizeof( sockname_sockaddr_storage );
  if
  (
    ::getsockname
    (
      *this,
      reinterpret_cast<struct sockaddr*>( &sockname_sockaddr_storage ),
      &sockname_sockaddr_storage_len
    ) != -1
  )
    return new SocketAddress( sockname_sockaddr_storage );
  else
    return NULL;
}

void YIELD::ipc::Socket::init()
{
#ifdef _WIN32
  WORD wVersionRequested = MAKEWORD( 2, 2 );
  WSADATA wsaData;
  WSAStartup( wVersionRequested, &wsaData );
#else
  signal( SIGPIPE, SIG_IGN );
#endif
}

bool YIELD::ipc::Socket::is_closed() const
{
#ifdef _WIN32
  return socket_ == INVALID_SOCKET;
#else
  return socket_ == -1;
#endif
}

bool YIELD::ipc::Socket::operator==( const Socket& other ) const
{
  return socket_ == other.socket_;
}

ssize_t YIELD::ipc::Socket::read( yidl::runtime::auto_Buffer buffer )
{
  ssize_t read_ret
    = read
      (
        static_cast<char*>( *buffer ) + buffer->size(),
        buffer->capacity() - buffer->size()
      );

  if ( read_ret > 0 )
    buffer->put( NULL, static_cast<size_t>( read_ret ) );
  return read_ret;
}

ssize_t YIELD::ipc::Socket::read( void* buffer, size_t buffer_len )
{
#if defined(_WIN32)
  return ::recv
         (
           socket_,
           static_cast<char*>( buffer ),
           static_cast<int>( buffer_len ),
           0
         ); // No real advantage to WSARecv on Win32 for one buffer
#elif defined(__linux)
  return ::recv( socket_, buffer, buffer_len, MSG_NOSIGNAL );
#else
  return ::recv( socket_, buffer, buffer_len, 0 );
#endif
}

bool YIELD::ipc::Socket::recreate()
{
  return recreate( AF_INET6 );
}

bool YIELD::ipc::Socket::recreate( int domain )
{
  close();
#ifdef _WIN32
  socket_ = ::socket( domain, type, protocol );
  if ( socket_ != INVALID_SOCKET )
#else
  socket_ = ::socket( domain, type, protocol );
  if ( socket_ != -1 )
#endif
  {
    if ( !blocking_mode )
      set_blocking_mode( false );
    this->domain = domain;
    return true;
  }
  else
    return false;
}

bool YIELD::ipc::Socket::set_blocking_mode( bool blocking )
{
#ifdef _WIN32
  unsigned long val = blocking ? 0 : 1;
  if ( ioctlsocket( *this, FIONBIO, &val ) != SOCKET_ERROR )
  {
    this->blocking_mode = blocking;
    return true;
  }
  else
    return false;
#else
  int current_fcntl_flags = fcntl( *this, F_GETFL, 0 );
  if ( blocking )
  {
    if ( ( current_fcntl_flags & O_NONBLOCK ) == O_NONBLOCK )
    {
      if ( fcntl( *this, F_SETFL, current_fcntl_flags ^ O_NONBLOCK ) != -1 )
      {
        this->blocking_mode = true;
        return true;
      }
      else
        return false;
    }
    else
      return true;
  }
  else
  {
    if ( fcntl( *this, F_SETFL, current_fcntl_flags | O_NONBLOCK ) != -1 )
    {
      this->blocking_mode = false;
      return true;
    }
    else
      return false;
  }
#endif
}

bool YIELD::ipc::Socket::shutdown()
{
  connected = false;
  return true;
}

bool YIELD::ipc::Socket::want_connect() const
{
#ifdef _WIN32
  switch ( WSAGetLastError() )
  {
    case WSAEALREADY:
    case WSAEINPROGRESS:
    case WSAEINVAL:
    case WSAEWOULDBLOCK: return true;
    default: return false;
  }
#else
  switch ( errno )
  {
    case EALREADY:
    case EINPROGRESS:
    case EWOULDBLOCK: return true;
    default: return false;
  }
#endif
}

bool YIELD::ipc::Socket::want_read() const
{
#ifdef _WIN32
  return WSAGetLastError() == WSAEWOULDBLOCK;
#else
  return errno == EWOULDBLOCK;
#endif
}

bool YIELD::ipc::Socket::want_write() const
{
#ifdef _WIN32
  return WSAGetLastError() == WSAEWOULDBLOCK;
#else
  return errno == EWOULDBLOCK;
#endif
}

ssize_t YIELD::ipc::Socket::write( yidl::runtime::auto_Buffer buffer )
{
  if ( buffer->get_type_id() == YIDL_RUNTIME_OBJECT_TYPE_ID( GatherBuffer ) )
    return writev
           (
             static_cast<GatherBuffer*>( buffer.get() )->get_iovecs(),
             static_cast<GatherBuffer*>( buffer.get() )->get_iovecs_len()
           );
  else
    return write( static_cast<void*>( *buffer ), buffer->size() );
}

ssize_t YIELD::ipc::Socket::write( const void* buffer, size_t buffer_len )
{
  // Go through writev to have a unified partial
  // write path in TCPSocket::writev
  struct iovec buffers[1];
  buffers[0].iov_base = const_cast<void*>( buffer );
  buffers[0].iov_len = buffer_len;
  return writev( buffers, 1 );
}

ssize_t YIELD::ipc::Socket::writev
(
  const struct iovec* buffers,
  uint32_t buffers_count
)
{
#if defined(_WIN32)
  DWORD dwWrittenLength;
#ifdef _WIN64
  // The WSABUF .len is a ULONG, which is != size_t on Win64,
  // so we have to truncate it here.
  // This is easier (compiler warnings, using sizeof, etc.)
  // than changing the struct iovec definition to use uint32_t.
  std::vector<WSABUF> wsabufs( buffers_count );
  for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
  {
    wsabufs[buffer_i].len = static_cast<ULONG>( buffers[buffer_i].iov_len );
    wsabufs[buffer_i].buf = static_cast<char*>( buffers[buffer_i].iov_base );
  }
  ssize_t write_ret
    = WSASend
      (
        socket_,
        &wsabufs[0],
        buffers_count,
        &dwWrittenLength,
        0,
        NULL,
        NULL
      );
#else
  ssize_t write_ret
    = WSASend
      (
        socket_,
        reinterpret_cast<WSABUF*>( const_cast<struct iovec*>( buffers ) ),
        buffers_count,
        &dwWrittenLength,
        0,
        NULL,
        NULL
      );
#endif
  if ( write_ret >= 0 )
    return static_cast<ssize_t>( dwWrittenLength );
  else
    return write_ret;
#elif defined(__linux)
  // Use sendmsg instead of writev to pass flags on Linux
  struct msghdr _msghdr;
  memset( &_msghdr, 0, sizeof( _msghdr ) );
  _msghdr.msg_iov = const_cast<iovec*>( buffers );
  _msghdr.msg_iovlen = buffers_count;
  return ::sendmsg( socket_, &_msghdr, MSG_NOSIGNAL ); // MSG_NOSIGNAL
                                                       // = disable SIGPIPE
#elif defined(__sun)
  // Sun's socket writev appears to be broken;
  // concatenate the buffers and write()
  std::string buffer;
  for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
  {
    buffer.append
    (
      static_cast<char*>( buffers[buffer_i].iov_base ),
      buffers[buffer_i].iov_len
    );
  }
  return write( buffer.c_str(), buffer.size() );
#else
  return ::writev( socket_, buffers, buffers_count );
#endif
}


// socket_address.cpp
#ifdef _WIN32
#pragma warning( 4 : 4365 )
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#else
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>
#endif


YIELD::ipc::SocketAddress::SocketAddress( struct addrinfo& addrinfo_list )
  : addrinfo_list( &addrinfo_list ), _sockaddr_storage( NULL )
{ }

YIELD::ipc::SocketAddress::~SocketAddress()
{
  if ( addrinfo_list != NULL )
    freeaddrinfo( addrinfo_list );
  else if ( _sockaddr_storage != NULL )
    delete _sockaddr_storage;
}

YIELD::ipc::SocketAddress::SocketAddress
(
  const struct sockaddr_storage& _sockaddr_storage
)
{
  addrinfo_list = NULL;
  this->_sockaddr_storage = new struct sockaddr_storage;
  memcpy_s
  (
    this->_sockaddr_storage,
    sizeof( *this->_sockaddr_storage ),
    &_sockaddr_storage,
    sizeof( _sockaddr_storage )
  );
}

YIELD::ipc::auto_SocketAddress
YIELD::ipc::SocketAddress::create( const URI& uri )
{
  if ( uri.get_host() == "*" )
    return create( NULL, uri.get_port() );
  else
    return create( uri.get_host().c_str(), uri.get_port() );
}

YIELD::ipc::auto_SocketAddress
YIELD::ipc::SocketAddress::create( const char* hostname, uint16_t port )
{
  struct addrinfo* addrinfo_list = getaddrinfo( hostname, port );
  if ( addrinfo_list != NULL )
    return new SocketAddress( *addrinfo_list );
  else
  {
    std::ostringstream what;
    what << "error resolving host \"";
    if ( hostname != NULL )
      what << hostname;
    else
      what << "*";
    what << "\": ";
    what << YIELD::platform::Exception();

    throw YIELD::platform::Exception( what.str() );
  }
}

bool YIELD::ipc::SocketAddress::as_struct_sockaddr
(
  int family,
  struct sockaddr*& out_sockaddr,
  socklen_t& out_sockaddrlen
)
{
  if ( addrinfo_list != NULL )
  {
    struct addrinfo* addrinfo_p = addrinfo_list;
    while ( addrinfo_p != NULL )
    {
      if ( addrinfo_p->ai_family == family )
      {
        out_sockaddr = addrinfo_p->ai_addr;
        out_sockaddrlen = static_cast<socklen_t>( addrinfo_p->ai_addrlen );
        return true;
      }
      else
        addrinfo_p = addrinfo_p->ai_next;
    }
  }
  else if ( _sockaddr_storage->ss_family == family )
  {
    out_sockaddr = reinterpret_cast<struct sockaddr*>( _sockaddr_storage );
    out_sockaddrlen = sizeof( *_sockaddr_storage );
    return true;
  }

#ifdef _WIN32
  ::WSASetLastError( WSAEAFNOSUPPORT );
#else
  errno = EAFNOSUPPORT;
#endif
  return false;
}

struct addrinfo*
YIELD::ipc::SocketAddress::getaddrinfo( const char* hostname, uint16_t port )
{
#ifdef _WIN32
  Socket::init();
#endif

  const char* servname;
#ifdef __sun
  if ( hostname == NULL )
    hostname = "0.0.0.0";
  servname = NULL;
#else
  std::ostringstream servname_oss; // ltoa is not very portable
  servname_oss << port; // servname = decimal port or service name.
  std::string servname_str = servname_oss.str();
  servname = servname_str.c_str();
#endif

  struct addrinfo addrinfo_hints;
  memset( &addrinfo_hints, 0, sizeof( addrinfo_hints ) );
  addrinfo_hints.ai_family = AF_UNSPEC;
  if ( hostname == NULL )
    addrinfo_hints.ai_flags |= AI_PASSIVE; // To get INADDR_ANYs

  struct addrinfo* addrinfo_list;

  int getaddrinfo_ret
    = ::getaddrinfo
      (
        hostname,
        servname,
        &addrinfo_hints,
        &addrinfo_list
      );

  if ( getaddrinfo_ret == 0 )
  {
#ifdef __sun
    struct addrinfo* addrinfo_p = addrinfo_list;
    while ( addrinfo_p != NULL )
    {
      switch ( addrinfo_p->ai_family )
      {
        case AF_INET:
        {
          reinterpret_cast<struct sockaddr_in*>( addrinfo_p->ai_addr )
            ->sin_port = htons( port );
        }
        break;

        case AF_INET6:
        {
          reinterpret_cast<struct sockaddr_in6*>( addrinfo_p->ai_addr )
            ->sin6_port = htons( port );
        }
        break;

        default: DebugBreak();
      }

      addrinfo_p = addrinfo_p->ai_next;
    }
#endif

    return addrinfo_list;
  }
  else
    return NULL;
}

bool YIELD::ipc::SocketAddress::getnameinfo
(
  std::string& out_hostname,
  bool numeric
) const
{
  char nameinfo[NI_MAXHOST];
  if ( this->getnameinfo( nameinfo, NI_MAXHOST, numeric ) )
  {
    out_hostname.assign( nameinfo );
    return true;
  }
  else
    return false;
}

bool YIELD::ipc::SocketAddress::getnameinfo
(
  char* out_hostname,
  uint32_t out_hostname_len,
  bool numeric
) const
{
  if ( addrinfo_list != NULL )
  {
    struct addrinfo* addrinfo_p = addrinfo_list;
    while ( addrinfo_p != NULL )
    {
      if
      (
        ::getnameinfo
        (
          addrinfo_p->ai_addr,
          static_cast<socklen_t>( addrinfo_p->ai_addrlen ),
          out_hostname,
          out_hostname_len,
          NULL,
          0,
          numeric ? NI_NUMERICHOST : 0
        ) == 0
      )
        return true;
      else
        addrinfo_p = addrinfo_p->ai_next;
    }
    return false;
  }
  else
    return ::getnameinfo
           (
             reinterpret_cast<sockaddr*>( _sockaddr_storage ),
             static_cast<socklen_t>( sizeof( *_sockaddr_storage ) ),
             out_hostname,
             out_hostname_len,
             NULL,
             0,
             numeric ? NI_NUMERICHOST : 0
           ) == 0;
}

uint16_t YIELD::ipc::SocketAddress::get_port() const
{
  if ( addrinfo_list != NULL )
  {
    switch ( addrinfo_list->ai_family )
    {
      case AF_INET:
      {
        return ntohs
               (
                 reinterpret_cast<struct sockaddr_in*>
                 (
                   addrinfo_list->ai_addr
                 )->sin_port
               );
      }

      case AF_INET6:
      {
        return ntohs
               (
                 reinterpret_cast<struct sockaddr_in6*>
                 (
                   addrinfo_list->ai_addr
                 )->sin6_port
               );
      }

      default:
      {
        DebugBreak();
        return 0;
      }
    }
  }
  else
  {
    switch ( _sockaddr_storage->ss_family )
    {
      case AF_INET:
      {
        return ntohs
               (
                 reinterpret_cast<struct sockaddr_in*>( _sockaddr_storage )
                   ->sin_port
               );
      }

      case AF_INET6:
      {
        return ntohs
               (
                 reinterpret_cast<struct sockaddr_in6*>( _sockaddr_storage )
                  ->sin6_port
               );
      }

      default:
      {
        DebugBreak();
        return 0;
      }
    }
  }
}

bool YIELD::ipc::SocketAddress::operator==( const SocketAddress& other ) const
{
  if ( addrinfo_list != NULL )
  {
    if ( other.addrinfo_list != NULL )
    {
      struct addrinfo* addrinfo_p = addrinfo_list;
      while ( addrinfo_p != NULL )
      {
        struct addrinfo* other_addrinfo_p = other.addrinfo_list;
        while ( other_addrinfo_p != NULL )
        {
          if
          (
            addrinfo_p->ai_addrlen == other_addrinfo_p->ai_addrlen &&
            memcmp
            (
              addrinfo_p->ai_addr,
              other_addrinfo_p->ai_addr,
              addrinfo_p->ai_addrlen
            ) == 0 &&
            addrinfo_p->ai_family == other_addrinfo_p->ai_family &&
            addrinfo_p->ai_protocol == other_addrinfo_p->ai_protocol &&
            addrinfo_p->ai_socktype == other_addrinfo_p->ai_socktype
          )
            break;
          else
            other_addrinfo_p = other_addrinfo_p->ai_next;
        }

        if ( other_addrinfo_p != NULL ) // i.e. we found the addrinfo
                                        // in the other's list
          addrinfo_p = addrinfo_p->ai_next;
        else
          return false;
      }

      return true;
    }
    else
      return false;
  }
  else if ( other._sockaddr_storage != NULL )
    return memcmp
           (
             _sockaddr_storage,
             other._sockaddr_storage,
             sizeof( *_sockaddr_storage )
           ) == 0;
  else
    return false;
}


// socket_aio_queue.cpp
#ifdef _WIN32
#ifndef FD_SETSIZE
#define FD_SETSIZE 1024
#endif
#pragma warning( 4 : 4365 )
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#pragma warning( push )
#pragma warning( disable: 4127 4389 ) // Warnings in the FD_* macros
#else
#include <errno.h>
#include <signal.h>
#include <sys/socket.h>
#include <unistd.h>
#if defined(__FreeBSD__) || defined(__OpenBSD__) || defined(__MACH__)
#define YIELD_IPC_HAVE_FREEBSD_KQUEUE 1
#include <netinet/in.h>
#include <sys/types.h>
#include <sys/event.h>
#include <sys/time.h>
#elif defined(__linux__)
#define YIELD_IPC_HAVE_LINUX_EPOLL 1
#include <sys/epoll.h>
#elif defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
#include <port.h>
#include <sys/poll.h>
#else
#include <poll.h>
#include <vector>
#endif
#endif


#ifdef _WIN32
class YIELD::ipc::Socket::AIOQueue::IOCPWorkerThread
  : public YIELD::platform::Thread
{
public:
  IOCPWorkerThread( HANDLE hIoCompletionPort )
    : hIoCompletionPort( hIoCompletionPort )
  {
    is_running = false;
  }

  void stop()
  {
    while ( is_running )
      Thread::yield();
  }

  // Thread
  void run()
  {
    is_running = true;

    set_name( "Socket::AIOQueue::IOCPWorkerThread" );

    for ( ;; )
    {
      DWORD dwBytesTransferred;
      ULONG_PTR ulCompletionKey;
      LPOVERLAPPED lpOverlapped;

      if
      (
        GetQueuedCompletionStatus
        (
          hIoCompletionPort,
          &dwBytesTransferred,
          &ulCompletionKey,
          &lpOverlapped,
          INFINITE
        )
      )
      {
        YIELD::platform::AIOControlBlock* aio_control_block
          = AIOControlBlock::from_OVERLAPPED( lpOverlapped );

        switch ( aio_control_block->get_type_id() )
        {
          case YIDL_RUNTIME_OBJECT_TYPE_ID( Socket::AIOConnectControlBlock ):
          {
            static_cast<Socket::AIOConnectControlBlock*>( aio_control_block )
              ->get_socket()->connected = true;

            aio_control_block->onCompletion( dwBytesTransferred );
          }
          break;

          case YIDL_RUNTIME_OBJECT_TYPE_ID( Socket::AIOReadControlBlock ):
          {
            static_cast<Socket::AIOReadControlBlock*>( aio_control_block )
              ->get_buffer()->put( NULL, dwBytesTransferred );

            if ( dwBytesTransferred > 0 )
              aio_control_block->onCompletion( dwBytesTransferred );
            else
              aio_control_block->onError( WSAECONNRESET );
          }
          break;

          case YIDL_RUNTIME_OBJECT_TYPE_ID
               ( UDPSocket::AIORecvFromControlBlock ):
          {
            static_cast<UDPSocket::AIORecvFromControlBlock*>
            (
              aio_control_block
            )->get_buffer()->put( NULL, dwBytesTransferred );

            aio_control_block->onCompletion( dwBytesTransferred );
          }
          break;

          default:
          {
            aio_control_block->onCompletion( dwBytesTransferred );
          }
          break;
        }

        YIELD::platform::AIOControlBlock::decRef( *aio_control_block );
      }
      else if ( lpOverlapped != NULL )
      {
        YIELD::platform::AIOControlBlock* aio_control_block
          = AIOControlBlock::from_OVERLAPPED( lpOverlapped );

        aio_control_block->onError( ::GetLastError() );

        YIELD::platform::AIOControlBlock::decRef( *aio_control_block );
      }
      else
        break;
    }

    is_running = false;
  }

private:
  HANDLE hIoCompletionPort;
  bool is_running;
};
#endif


class YIELD::ipc::Socket::AIOQueue::NBIOWorkerThread
  : public YIELD::platform::Thread
{
public:
  NBIOWorkerThread()
  {
    is_running = false;
    should_run = true;

#if defined(_WIN32)
    FD_ZERO( &read_fds );
    FD_ZERO( &write_fds );
    FD_ZERO( &except_fds );
#elif defined(YIELD_IPC_HAVE_LINUX_EPOLL)
    poll_fd = epoll_create( 32768 );
#elif defined(YIELD_IPC_HAVE_FREEBSD_KQUEUE)
    poll_fd = kqueue();
#elif defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
    poll_fd = port_create();
#endif

#if defined(_WIN32)
    auto_TCPSocket submit_listen_tcp_socket = TCPSocket::create( AF_INET );
    if
    (
      submit_listen_tcp_socket != NULL &&
      submit_listen_tcp_socket->bind
      (
        SocketAddress::create( "localhost", 0 )
      ) &&
      submit_listen_tcp_socket->listen()
    )
    {
      submit_pipe_write_end = TCPSocket::create( AF_INET );
      if
      (
        submit_pipe_write_end != NULL &&
        submit_pipe_write_end->connect
        (
          submit_listen_tcp_socket->getsockname()
        )
      )
      {
        submit_pipe_read_end = submit_listen_tcp_socket->accept();
        if
        (
          submit_pipe_read_end != NULL &&
          submit_pipe_read_end->set_blocking_mode( false ) &&
          associate( *submit_pipe_read_end, true, false )
        )
          return;
      }
    }
#elif defined(__MACH__)
    int socket_vector[2];
    if ( ::socketpair( AF_UNIX, SOCK_STREAM, 0, socket_vector ) != -1 )
    {
      submit_pipe_read_end
        = new Socket( AF_UNIX, SOCK_STREAM, 0, socket_vector[0] );

      submit_pipe_write_end
        = new Socket( AF_UNIX, SOCK_STREAM, 0, socket_vector[1] );

      if
      (
        submit_pipe_read_end->set_blocking_mode( false ) &&
        associate( *submit_pipe_read_end, true, false )
      )
         return;
    }
#else
    submit_pipe = Pipe::create();
    if ( submit_pipe != NULL &&
         submit_pipe->set_blocking_mode( false ) &&
         associate( submit_pipe->get_read_end(), true, false ) )
        return;
#endif

    std::cerr << "yield::ipc::Socket::AIOQueue::NBIOWorkerThread: " <<
                 "error creating submit pipe: " <<
                 YIELD::platform::Exception()
                 << "." << std::endl;
  }

  ~NBIOWorkerThread()
  {
#if defined(YIELD_IPC_HAVE_LINUX_EPOLL) || \
    defined(YIELD_IPC_HAVE_FREEBSD_KQUEUE) || \
    defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
    ::close( poll_fd );
#endif
  }

  void submit( yidl::runtime::auto_Object<AIOControlBlock> aio_control_block )
  {
    AIOControlBlock* submit_aio_control_block = aio_control_block.release();
#if defined(_WIN32) || defined(__MACH__)
    submit_pipe_write_end->write
    (
      &submit_aio_control_block,
      sizeof( submit_aio_control_block )
    );
#else
    submit_pipe->write
    (
      &submit_aio_control_block,
      sizeof( submit_aio_control_block )
    );
#endif
  }

  void stop()
  {
    should_run = false;
#if defined(_WIN32) || defined(__MACH__)
    submit_pipe_read_end->close();
    submit_pipe_write_end->close();
#else
    submit_pipe->close();
#endif
    while ( is_running )
      Thread::yield();
  }

  // Thread
  void run()
  {
    is_running = true;
    set_name( "Socket::AIOQueue::NBIOWorkerThread" );

#if defined(_WIN32)
    fd_set read_fds_copy, write_fds_copy, except_fds_copy;
    FD_ZERO( &read_fds_copy );
    FD_ZERO( &write_fds_copy );
    FD_ZERO( &except_fds_copy );
#elif defined(YIELD_IPC_HAVE_LINUX_EPOLL)
    struct epoll_event returned_events[8192];
#elif defined(YIELD_IPC_HAVE_FREEBSD_KQUEUE)
    struct kevent returned_events[8192];
#elif defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
    port_event_t returned_events[1];
#endif

    while ( should_run )
    {
#if defined(_WIN32)
      memcpy_s
      (
        &read_fds_copy,
        sizeof( read_fds_copy ),
        &read_fds,
        sizeof( read_fds )
      );

      memcpy_s
      (
        &write_fds_copy,
        sizeof( write_fds_copy ),
        &write_fds,
        sizeof( write_fds )
      );

      memcpy_s
      (
        &except_fds_copy,
        sizeof( except_fds_copy ),
        &except_fds,
        sizeof( except_fds )
      );

      int active_fds
        = select( 0, &read_fds_copy, &write_fds_copy, &except_fds_copy, NULL );
#elif defined(YIELD_IPC_HAVE_LINUX_EPOLL)
      int active_fds = epoll_wait( poll_fd, returned_events, 8192, -1 );
#elif defined(YIELD_IPC_HAVE_FREEBSD_KQUEUE)
      int active_fds = kevent( poll_fd, 0, 0, returned_events, 8192, NULL );
#elif defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
      int active_fds = port_get( poll_fd, returned_events, NULL );
      if ( active_fds == 0 )
        active_fds = 1;
#else
      int active_fds = poll( &pollfds[0], pollfds.size(), -1 );
#endif

      if ( active_fds > 0 && should_run )
      {
#ifdef _WIN32
        std::map<SOCKET, AIOControlBlock*>::iterator fd_to_aio_control_block_i;
        if ( FD_ISSET( *submit_pipe_read_end, &read_fds_copy ) )
        {
          active_fds--;
          FD_CLR( *submit_pipe_read_end, &read_fds_copy );
          dequeueSubmittedAIOControlBlocks();
        }

        fd_to_aio_control_block_i = fd_to_aio_control_block_map.begin();
        while
        (
          active_fds > 0 &&
          fd_to_aio_control_block_i != fd_to_aio_control_block_map.end()
        )
        {
          SOCKET fd = fd_to_aio_control_block_i->first;
          if ( FD_ISSET( fd, &read_fds_copy ) )
            FD_CLR( fd, &read_fds_copy );
          else if ( FD_ISSET( fd, &write_fds_copy ) )
            FD_CLR( fd, &write_fds_copy );
          else if ( FD_ISSET( fd, &except_fds_copy ) )
             FD_CLR( fd, &except_fds_copy );
          else
          {
            fd_to_aio_control_block_i++;
            continue;
          }

          active_fds--;
#else
        std::map<int, AIOControlBlock*>::iterator fd_to_aio_control_block_i;
        while ( active_fds > 0 )
        {
#if defined(YIELD_IPC_HAVE_LINUX_EPOLL) || \
    defined(YIELD_IPC_HAVE_FREEBSD_KQUEUE) || \
    defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
          active_fds--;
#if defined(YIELD_IPC_HAVE_LINUX_EPOLL)
          int fd = returned_events[active_fds].data.fd;
#elif defined(YIELD_IPC_HAVE_FREEBSD_KQUEUE)
          int fd = returned_events[active_fds].ident;
#elif defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
          int fd = returned_events[active_fds].portev_object;
#else
          DebugBreak(); // Look through pollfds
#endif
#endif

#ifdef __MACH__
          if ( fd == *submit_pipe_read_end )
#else
          if ( fd == submit_pipe->get_read_end() )
#endif
          {
            dequeueSubmittedAIOControlBlocks();
#ifdef YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS
            toggle( submit_pipe->get_read_end(), true, false );
#endif
            continue;
          }

          fd_to_aio_control_block_i = fd_to_aio_control_block_map.find( fd );
#endif

          if ( processAIOControlBlock( fd_to_aio_control_block_i->second ) )
          {
#ifdef _WIN32
            fd_to_aio_control_block_i
              = fd_to_aio_control_block_map.erase( fd_to_aio_control_block_i );
#else
            fd_to_aio_control_block_map.erase( fd_to_aio_control_block_i );
#endif
          }
        }
      }
      else if ( active_fds < 0 )
      {
#ifndef _WIN32
        if ( errno != EINTR )
#endif
          std::cerr << "yield::ipc::Socket::AIOQueue::NBIOWorkerThread: " <<
            "error on poll: " << YIELD::platform::Exception() <<
            "." << std::endl;
      }
    }

    is_running = false;
  }

private:
  bool is_running, should_run;

#if defined(_WIN32)
  std::map<SOCKET, AIOControlBlock*> fd_to_aio_control_block_map;
  fd_set read_fds, write_fds, except_fds;
#else
  std::map<int, AIOControlBlock*> fd_to_aio_control_block_map;
#if defined(YIELD_IPC_HAVE_LINUX_EPOLL) || \
    defined(YIELD_IPC_HAVE_FREEBSD_KQUEUE) || \
    defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
  int poll_fd;
#else
  std::vector<pollfd> pollfds;
#endif
#endif

#if defined(_WIN32)
  auto_TCPSocket submit_pipe_read_end, submit_pipe_write_end;
#elif defined(__MACH__)
  auto_Socket submit_pipe_read_end, submit_pipe_write_end;
#else
  auto_Pipe submit_pipe;
#endif

#ifdef _WIN32
  bool associate( SOCKET fd, bool enable_read, bool enable_write )
#else
  bool associate( int fd, bool enable_read, bool enable_write )
#endif
  {
#if defined(_WIN32)
    if ( enable_read )
      FD_SET( fd, &read_fds );

    if ( enable_write )
    {
        FD_SET( fd, &write_fds );
        FD_SET( fd, &except_fds );
    }

    return true;
#elif defined(YIELD_IPC_HAVE_LINUX_EPOLL)
    struct epoll_event change_event;
    memset( &change_event, 0, sizeof( change_event ) );
    if ( enable_read ) change_event.events |= EPOLLIN;
    if ( enable_write ) change_event.events |= EPOLLOUT;
    change_event.data.fd = fd;
    return epoll_ctl( poll_fd, EPOLL_CTL_ADD, fd, &change_event ) != -1;
#elif defined(YIELD_IPC_HAVE_FREEBSD_KQUEUE)
    struct kevent change_events[2];

    EV_SET
    (
      &change_events[0],
      fd,
      EVFILT_READ,
      EV_ADD | ( enable_read ? EV_ENABLE : EV_DISABLE ),
      0,
      0,
      NULL
    );

    EV_SET
    (
      &change_events[1],
      fd,
      EVFILT_WRITE,
      EV_ADD | ( enable_write ? EV_ENABLE : EV_DISABLE ),
      0,
      0,
      NULL
    );

    return kevent( poll_fd, change_events, 2, 0, 0, NULL ) != -1;
#elif defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
    int events = 0;
    if ( enable_read ) events |= POLLIN;
    if ( enable_write ) events |= POLLOUT;
    return port_associate( poll_fd, PORT_SOURCE_FD, fd, events, NULL ) != -1;
#else
    std::vector<struct pollfd>::size_type pollfd_i_max = pollfds.size();
    for
    (
      std::vector<struct pollfd>::size_type pollfd_i = 0;
      pollfd_i < pollfd_i_max;
      pollfd_i++
    )
    {
      if ( pollfds[pollfd_i].fd == fd )
        return false;
    }

    struct pollfd attach_pollfd;
    memset( &attach_pollfd, 0, sizeof( attach_pollfd ) );
    if ( enable_read ) attach_pollfd.events |= POLLIN;
    if ( enable_write ) attach_pollfd.events |= POLLOUT;
    attach_pollfd.fd = fd;
    attach_pollfd.revents = 0;
    pollfds.push_back( attach_pollfd );
    return true;
#endif
  }

  void dequeueSubmittedAIOControlBlocks()
  {
    AIOControlBlock* aio_control_block;
    for ( ;; )
    {
      ssize_t read_ret;
#if defined(_WIN32) || defined(__MACH__)
      read_ret
        = submit_pipe_read_end->read
          (
            &aio_control_block,
            sizeof( aio_control_block )
          );
#else
      read_ret
        = submit_pipe->read
          (
            &aio_control_block,
            sizeof( aio_control_block )
          );
#endif
      if ( read_ret == sizeof( aio_control_block ) )
      {
#ifdef _WIN32
        SOCKET fd = *aio_control_block->get_socket();
#else
        int fd = *aio_control_block->get_socket();
#endif
        associate( fd, false, false );
        if ( !processAIOControlBlock( aio_control_block ) )
          fd_to_aio_control_block_map[fd] = aio_control_block;
      }
      else if ( read_ret <= 0 )
        break;
      else
        DebugBreak();
    }
  }

#ifdef _WIN32
  void dissociate( SOCKET fd )
#else
  void dissociate( int fd )
#endif
  {
#if defined(_WIN32)
    FD_CLR( fd, &read_fds );
    FD_CLR( fd, &write_fds );
    FD_CLR( fd, &except_fds );
#elif defined(YIELD_IPC_HAVE_LINUX_EPOLL)
    // From the man page: In kernel versions before 2.6.9,
    // the EPOLL_CTL_DEL operation required a non-NULL pointer in event,
    // even though this argument is ignored. Since kernel 2.6.9,
    // event can be specified as NULL when using EPOLL_CTL_DEL.
    struct epoll_event change_event;
    epoll_ctl( poll_fd, EPOLL_CTL_DEL, fd, &change_event );
#elif defined(YIELD_IPC_HAVE_FREEBSD_KQUEUE)
    struct kevent change_events[2];
    EV_SET( &change_events[0], fd, EVFILT_READ, EV_DELETE, 0, 0, NULL );
    EV_SET( &change_events[1], fd, EVFILT_WRITE, EV_DELETE, 0, 0, NULL );
    kevent( poll_fd, change_events, 2, 0, 0, NULL );
#elif defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
    port_dissociate( poll_fd, PORT_SOURCE_FD, fd );
#else
    for
    (
      std::vector<struct pollfd>::iterator pollfd_i = pollfds.begin();
      pollfd_i != pollfds.end();
    )
    {
      if ( ( *pollfd_i ).fd == fd )
        pollfd_i = pollfds.erase( pollfd_i );
      else
        ++pollfd_i;
    }
#endif
  }

  bool processAIOControlBlock( AIOControlBlock* aio_control_block )
  {
#ifdef _WIN32
    SOCKET fd = *aio_control_block->get_socket();
#else
    int fd = *aio_control_block->get_socket();
#endif

    switch ( aio_control_block->execute() )
    {
      case Socket::AIOControlBlock::EXECUTE_STATUS_DONE:
      {
        dissociate( fd );
        AIOControlBlock::decRef( *aio_control_block );
        return true;
      }

      case Socket::AIOControlBlock::EXECUTE_STATUS_WANT_READ:
      {
        toggle( fd, true, false );
        return false;
      }

      case Socket::AIOControlBlock::EXECUTE_STATUS_WANT_WRITE:
      {
        toggle( fd, false, true );
        return false;
      }

      default:
      {
        DebugBreak();
        return false;
      }
    }
  }

#ifdef _WIN32
  bool toggle( SOCKET fd, bool enable_read, bool enable_write )
#else
  bool toggle( int fd, bool enable_read, bool enable_write )
#endif
  {
#if defined(_WIN32)
    if ( enable_read )
      FD_SET( fd, &read_fds );
    else
      FD_CLR( fd, &read_fds );

    if ( enable_write )
    {
      FD_SET( fd, &write_fds );
      FD_SET( fd, &except_fds );
    }
    else
    {
      FD_CLR( fd, &write_fds );
      FD_CLR( fd, &except_fds );
    }

    return true;
#elif defined(YIELD_IPC_HAVE_LINUX_EPOLL)
    struct epoll_event change_event;
    memset( &change_event, 0, sizeof( change_event ) );
    if ( enable_read ) change_event.events |= EPOLLIN;
    if ( enable_write ) change_event.events |= EPOLLOUT;
    change_event.data.fd = fd;
    return epoll_ctl( poll_fd, EPOLL_CTL_MOD, fd, &change_event ) != -1;
#elif defined YIELD_IPC_HAVE_FREEBSD_KQUEUE
    struct kevent change_events[2];

    EV_SET
    (
      &change_events[0],
      fd,
      EVFILT_READ,
      enable_read ? EV_ENABLE : EV_DISABLE,
      0,
      0,
      NULL
    );

    EV_SET
    (
      &change_events[1],
      fd,
      EVFILT_WRITE,
      enable_write ? EV_ENABLE : EV_DISABLE,
      0,
      0,
      NULL
    );

    return kevent
           (
             poll_fd,
             change_events,
             2,
             0,
             0,
             NULL
           ) != -1
           ||
           errno == ENOENT; // ENOENT = the event was not originally enabled
#elif defined(YIELD_IPC_HAVE_SOLARIS_EVENT_PORTS)
    if ( enable_read || enable_write )
    {
      int events = 0;
      if ( enable_read ) events |= POLLIN;
      if ( enable_write ) events |= POLLOUT;
      return port_associate( poll_fd, PORT_SOURCE_FD, fd, events, NULL ) != -1;
    }
    else
      return port_dissociate( poll_fd, PORT_SOURCE_FD, fd ) != -1;
#else
    for
    (
      std::vector<struct pollfd>::iterator pollfd_i = pollfds.begin();
      pollfd_i != pollfds.end();
    )
    {
      if ( ( *pollfd_i ).fd == fd )
      {
        int events = 0;
        if ( enable_read ) events |= POLLIN;
        if ( enable_write ) events |= POLLOUT;
        ( *pollfd_i ).events = events;
        return true;
      }
    }

    return false;
#endif
  }
};


YIELD::ipc::Socket::AIOQueue::AIOQueue()
{
#ifdef _WIN32
  hIoCompletionPort
    = CreateIoCompletionPort( INVALID_HANDLE_VALUE, NULL, 0, 0 );
  if ( hIoCompletionPort == INVALID_HANDLE_VALUE ) DebugBreak();

  uint16_t iocp_worker_thread_count
    = YIELD::platform::Machine::getOnlineLogicalProcessorCount();
  for
  (
    uint16_t iocp_worker_thread_i = 0;
    iocp_worker_thread_i < iocp_worker_thread_count;
    iocp_worker_thread_i++
  )
  {
    IOCPWorkerThread* iocp_worker_thread
      = new IOCPWorkerThread( hIoCompletionPort );
    iocp_worker_threads.push_back( iocp_worker_thread );
    iocp_worker_thread->start();
  }
#endif
}

YIELD::ipc::Socket::AIOQueue::~AIOQueue()
{
#ifdef _WIN32
  CloseHandle( hIoCompletionPort );

  for
  (
    std::vector<IOCPWorkerThread*>::iterator
      iocp_worker_thread_i = iocp_worker_threads.begin();
    iocp_worker_thread_i != iocp_worker_threads.end();
    iocp_worker_thread_i++
  )
  {
    ( *iocp_worker_thread_i )->stop();
    IOCPWorkerThread::decRef( **iocp_worker_thread_i );
  }
#endif

  for
  (
    std::vector<NBIOWorkerThread*>::iterator
      nbio_worker_thread_i = nbio_worker_threads.begin();
    nbio_worker_thread_i != nbio_worker_threads.end();
    nbio_worker_thread_i++
  )
  {
    ( *nbio_worker_thread_i )->stop();
    NBIOWorkerThread::decRef( **nbio_worker_thread_i );
  }
}

#ifdef _WIN32
void YIELD::ipc::Socket::AIOQueue::associate( Socket& socket_ )
{
  CreateIoCompletionPort
  (
    reinterpret_cast<HANDLE>( static_cast<SOCKET>( socket_ ) ),
    hIoCompletionPort,
    0,
    0
  );
}
#endif

void YIELD::ipc::Socket::AIOQueue::submit
(
  auto_AIOControlBlock aio_control_block
)
{
  if ( nbio_worker_threads.empty() )
  {
    uint16_t nbio_worker_thread_count
      = YIELD::platform::Machine::getOnlineLogicalProcessorCount();
    // uint16_t nbio_worker_thread_count = 1;
    for
    (
      uint16_t nbio_worker_thread_i = 0;
      nbio_worker_thread_i < nbio_worker_thread_count;
      nbio_worker_thread_i++
    )
    {
      NBIOWorkerThread* nbio_worker_thread = new NBIOWorkerThread;
      nbio_worker_thread->start();
      nbio_worker_threads.push_back( nbio_worker_thread );
    }
  }

  nbio_worker_threads
  [
    YIELD::platform::Thread::gettid() % nbio_worker_threads.size()
  ]->submit( aio_control_block );
}

#ifdef _WIN32
#pragma warning( pop )
#endif


// ssl_context.cpp
#ifdef YIELD_IPC_HAVE_OPENSSL
#include <openssl/err.h>
#include <openssl/pem.h>
#include <openssl/pkcs12.h>
#include <openssl/rsa.h>
#include <openssl/ssl.h>
#include <openssl/x509.h>
#ifdef _WIN32
#pragma comment( lib, "libeay32.lib" )
#pragma comment( lib, "ssleay32.lib" )
#endif
#endif


#ifdef YIELD_IPC_HAVE_OPENSSL

namespace YIELD
{
  namespace ipc
  {
    class SSLException : public YIELD::platform::Exception
    {
    public:
      SSLException()
        : YIELD::platform::Exception( ERR_peek_error() )
      {
        SSL_load_error_strings();

        char error_message[256];
        ERR_error_string_n( ERR_peek_error(), error_message, 256 );
        set_error_message( error_message );
      }
    };
  };
};


static int pem_password_callback( char *buf, int size, int, void *userdata )
{
  const std::string* pem_password
    = static_cast<const std::string*>( userdata );
  if ( size > static_cast<int>( pem_password->size() ) )
    size = static_cast<int>( pem_password->size() );
  memcpy_s( buf, size, pem_password->c_str(), size );
  return size;
}


YIELD::ipc::SSLContext::SSLContext( SSL_CTX* ctx )
  : ctx( ctx )
{ }

YIELD::ipc::auto_SSLContext
YIELD::ipc::SSLContext::create
(
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
  const
#endif
  SSL_METHOD* method
)
{
  return new SSLContext( createSSL_CTX( method ) );
}

YIELD::ipc::auto_SSLContext
YIELD::ipc::SSLContext::create
(
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
  const
#endif
  SSL_METHOD* method,
#ifdef _WIN32
  const YIELD::platform::Path& _pem_certificate_file_path,
  const YIELD::platform::Path& _pem_private_key_file_path,
#else
  const YIELD::platform::Path& pem_certificate_file_path,
  const YIELD::platform::Path& pem_private_key_file_path,
#endif
  const std::string& pem_private_key_passphrase
)
{
  SSL_CTX* ctx = createSSL_CTX( method );

#ifdef _WIN32
  // Need to get a string on Windows, because SSL doesn't support wide paths
  std::string pem_certificate_file_path( _pem_certificate_file_path );
  std::string pem_private_key_file_path( _pem_private_key_file_path );
#endif

  if
  (
    SSL_CTX_use_certificate_file
    (
      ctx,
#ifdef _WIN32
      pem_certificate_file_path.c_str(),
#else
      pem_certificate_file_path,
#endif
      SSL_FILETYPE_PEM
    ) > 0
  )
  {
    if ( !pem_private_key_passphrase.empty() )
    {
      SSL_CTX_set_default_passwd_cb( ctx, pem_password_callback );
      SSL_CTX_set_default_passwd_cb_userdata
      (
        ctx,
        const_cast<std::string*>( &pem_private_key_passphrase )
      );
    }

    if
    (
      SSL_CTX_use_PrivateKey_file
      (
        ctx,
#ifdef _WIN32
        pem_private_key_file_path.c_str(),
#else
        pem_private_key_file_path,
#endif
        SSL_FILETYPE_PEM
      ) > 0
    )
      return new SSLContext( ctx );
  }

  throw SSLException();
}

YIELD::ipc::auto_SSLContext
YIELD::ipc::SSLContext::create
(
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
  const
#endif
  SSL_METHOD* method,
  const std::string& pem_certificate_str,
  const std::string& pem_private_key_str,
  const std::string& pem_private_key_passphrase
)
{
  SSL_CTX* ctx = createSSL_CTX( method );

  BIO* pem_certificate_bio
    = BIO_new_mem_buf
      (
        reinterpret_cast<void*>
        (
          const_cast<char*>( pem_certificate_str.c_str() )
        ),
        static_cast<int>( pem_certificate_str.size() )
      );

  if ( pem_certificate_bio != NULL )
  {
    X509* cert
      = PEM_read_bio_X509
        (
          pem_certificate_bio,
          NULL,
          pem_password_callback,
          const_cast<std::string*>( &pem_private_key_passphrase )
        );

    if ( cert != NULL )
    {
      SSL_CTX_use_certificate( ctx, cert );

      BIO* pem_private_key_bio
        = BIO_new_mem_buf
          (
            reinterpret_cast<void*>
            (
              const_cast<char*>( pem_private_key_str.c_str() )
            ),
            static_cast<int>( pem_private_key_str.size() )
          );

      if ( pem_private_key_bio != NULL )
      {
        EVP_PKEY* pkey
          = PEM_read_bio_PrivateKey
            (
              pem_private_key_bio,
              NULL,
              pem_password_callback,
              const_cast<std::string*>( &pem_private_key_passphrase )
            );

        if ( pkey != NULL )
        {
          SSL_CTX_use_PrivateKey( ctx, pkey );

          BIO_free( pem_certificate_bio );
          BIO_free( pem_private_key_bio );

          return new SSLContext( ctx );
        }

        BIO_free( pem_private_key_bio );
      }
    }

    BIO_free( pem_certificate_bio );
  }

  throw SSLException();
}

YIELD::ipc::auto_SSLContext
YIELD::ipc::SSLContext::create
(
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
  const
#endif
  SSL_METHOD* method,
#ifdef _WIN32
  const YIELD::platform::Path& _pkcs12_file_path,
#else
  const YIELD::platform::Path& pkcs12_file_path,
#endif
  const std::string& pkcs12_passphrase
)
{
  SSL_CTX* ctx = createSSL_CTX( method );

#ifdef _WIN32
  // See note in the PEM create above re: the rationale for this
  std::string pkcs12_file_path( _pkcs12_file_path );
#endif

#ifdef _WIN32
  BIO* bio = BIO_new_file( pkcs12_file_path.c_str(), "rb" );
#else
  BIO* bio = BIO_new_file( pkcs12_file_path, "rb" );
#endif
  if ( bio != NULL )
  {
    PKCS12* p12 = d2i_PKCS12_bio( bio, NULL );
    if ( p12 != NULL )
    {
      EVP_PKEY* pkey = NULL;
      X509* cert = NULL;
      STACK_OF( X509 )* ca = NULL;
      if ( PKCS12_parse( p12, pkcs12_passphrase.c_str(), &pkey, &cert, &ca ) )
      {
        if ( pkey != NULL && cert != NULL && ca != NULL )
        {
          SSL_CTX_use_certificate( ctx, cert );
          SSL_CTX_use_PrivateKey( ctx, pkey );

          X509_STORE* store = SSL_CTX_get_cert_store( ctx );
          for ( int i = 0; i < sk_X509_num( ca ); i++ )
          {
            X509* store_cert = sk_X509_value( ca, i );
            X509_STORE_add_cert( store, store_cert );
          }

          BIO_free( bio );

          return new SSLContext( ctx );
        }
      }
    }

    BIO_free( bio );
  }

  throw SSLException();
}

#else

YIELD::ipc::SSLContext::SSLContext()
{ }

YIELD::ipc::auto_SSLContext YIELD::ipc::SSLContext::create()
{
  return new SSLContext;
}

#endif

YIELD::ipc::SSLContext::~SSLContext()
{
#ifdef YIELD_IPC_HAVE_OPENSSL
  SSL_CTX_free( ctx );
#endif
}

#ifdef YIELD_IPC_HAVE_OPENSSL

SSL_CTX*
YIELD::ipc::SSLContext::createSSL_CTX
(
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
  const
#endif
  SSL_METHOD* method
)
{
  SSL_library_init();
  OpenSSL_add_all_algorithms();

  SSL_CTX* ctx = SSL_CTX_new( method );
  if ( ctx != NULL )
  {
#ifdef SSL_OP_NO_TICKET
    SSL_CTX_set_options( ctx, SSL_OP_ALL|SSL_OP_NO_TICKET );
#else
    SSL_CTX_set_options( ctx, SSL_OP_ALL );
#endif
    SSL_CTX_set_verify( ctx, SSL_VERIFY_NONE, NULL );
    return ctx;
  }
  else
    throw SSLException();
}

#endif


// ssl_socket.cpp
#ifdef YIELD_IPC_HAVE_OPENSSL

#ifdef _WIN32
#pragma warning( 4 : 4365 )
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <sys/socket.h>
#endif


YIELD::ipc::SSLSocket::SSLSocket
(
  int domain,
#ifdef _WIN32
  SOCKET socket_,
#else
  int socket_,
#endif
  auto_SSLContext ctx,
  SSL* ssl
)
  : TCPSocket( domain, socket_ ), ctx( ctx ), ssl( ssl )
{ }

YIELD::ipc::SSLSocket::~SSLSocket()
{
  SSL_free( ssl );
}

YIELD::ipc::auto_TCPSocket YIELD::ipc::SSLSocket::accept()
{
  SSL_set_fd( ssl, *this );
#ifdef _WIN32
  SOCKET peer_socket = TCPSocket::_accept();
#else
  int peer_socket = TCPSocket::_accept();
#endif
  if ( peer_socket != -1 )
  {
    SSL* peer_ssl = SSL_new( ctx->get_ssl_ctx() );
    SSL_set_fd( peer_ssl, peer_socket );
    SSL_set_accept_state( peer_ssl );
    return new SSLSocket( get_domain(), peer_socket, ctx, peer_ssl );
  }
  else
    return NULL;
}

void YIELD::ipc::SSLSocket::aio_accept
(
  auto_AIOAcceptControlBlock aio_accept_control_block
)
{
  aio_accept_nbio( aio_accept_control_block );
}

void YIELD::ipc::SSLSocket::aio_connect
(
  auto_AIOConnectControlBlock aio_connect_control_block
)
{
  aio_connect_nbio( aio_connect_control_block );
}

void YIELD::ipc::SSLSocket::aio_read
(
  auto_AIOReadControlBlock aio_read_control_block
)
{
  aio_read_nbio( aio_read_control_block );
}

void YIELD::ipc::SSLSocket::aio_write
(
  auto_AIOWriteControlBlock aio_write_control_block
)
{
  aio_write_nbio( aio_write_control_block );
}

bool YIELD::ipc::SSLSocket::connect( auto_SocketAddress peername )
{
  if ( TCPSocket::connect( peername ) )
  {
    SSL_set_fd( ssl, *this );
    SSL_set_connect_state( ssl );
    return true;
  }
  else
    return false;
}

YIELD::ipc::auto_SSLSocket
YIELD::ipc::SSLSocket::create( auto_SSLContext ctx )
{
  return create( AF_INET6, ctx );
}

YIELD::ipc::auto_SSLSocket
YIELD::ipc::SSLSocket::create( int domain, auto_SSLContext ctx )
{
  SSL* ssl = SSL_new( ctx->get_ssl_ctx() );
  if ( ssl != NULL )
  {
#ifdef _WIN32
    SOCKET socket_ = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
    if ( socket_ != INVALID_SOCKET )
#else
    int socket_ = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
    if ( socket_ != -1 )
#endif
      return new SSLSocket( domain, socket_, ctx, ssl );
    else
      return NULL;
  }
  else
    return NULL;
}

/*
void SSLSocket::info_callback( const SSL* ssl, int where, int ret )
{
  std::ostringstream info;

  int w = where & ~SSL_ST_MASK;
  if ( ( w & SSL_ST_CONNECT ) == SSL_ST_CONNECT ) info << "SSL_connect:";
  else if ( ( w & SSL_ST_ACCEPT ) == SSL_ST_ACCEPT ) info << "SSL_accept:";
  else info << "undefined:";

  if ( ( where & SSL_CB_LOOP ) == SSL_CB_LOOP )
    info << SSL_state_string_long( ssl );
  else if ( ( where & SSL_CB_ALERT ) == SSL_CB_ALERT )
  {
    if ( ( where & SSL_CB_READ ) == SSL_CB_READ )
      info << "read:";
    else
      info << "write:";
    info << "SSL3 alert" << SSL_alert_type_string_long( ret ) << ":" <<
            SSL_alert_desc_string_long( ret );
  }
  else if ( ( where & SSL_CB_EXIT ) == SSL_CB_EXIT )
  {
    if ( ret == 0 )
      info << "failed in " << SSL_state_string_long( ssl );
    else
      info << "error in " << SSL_state_string_long( ssl );
  }
  else
    return;

  reinterpret_cast<SSLSocket*>( SSL_get_app_data( const_cast<SSL*>( ssl ) ) )
    ->log->getStream( Log::LOG_NOTICE ) << "SSLSocket: " << info.str();
}
*/

ssize_t YIELD::ipc::SSLSocket::read( void* buffer, size_t buffer_len )
{
  return SSL_read( ssl, buffer, static_cast<int>( buffer_len ) );
}

bool YIELD::ipc::SSLSocket::shutdown()
{
  if ( SSL_shutdown( ssl ) != -1 )
    return TCPSocket::shutdown();
  else
    return false;
}

bool YIELD::ipc::SSLSocket::want_read() const
{
  return SSL_want_read( ssl ) == 1;
}

bool YIELD::ipc::SSLSocket::want_write() const
{
  return SSL_want_write( ssl ) == 1;
}

ssize_t YIELD::ipc::SSLSocket::write( const void* buffer, size_t buffer_len )
{
  return SSL_write( ssl, buffer, static_cast<int>( buffer_len ) );
}

ssize_t
YIELD::ipc::SSLSocket::writev
(
  const struct iovec* buffers,
  uint32_t buffers_count
)
{
  if ( buffers_count == 1 )
    return write( buffers[0].iov_base, buffers[0].iov_len );
  else
  {
    std::string buffer;
    for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
    {
      buffer.append
      (
        static_cast<const char*>( buffers[buffer_i].iov_base ),
        buffers[buffer_i].iov_len
      );
    }
    return write( buffer.c_str(), buffer.size() );
  }
}

#endif


// tcp_socket.cpp
#if defined(_WIN32)
#pragma warning( 4 : 4365 )
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#include <mswsock.h>
#pragma warning( pop )
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <netinet/tcp.h> // For the TCP_* constants
#include <sys/socket.h>
#endif

#include <cstring>


#ifdef _WIN32
void* YIELD::ipc::TCPSocket::lpfnAcceptEx = NULL;
void* YIELD::ipc::TCPSocket::lpfnConnectEx = NULL;
#endif


YIELD::ipc::Socket::AIOControlBlock::ExecuteStatus
YIELD::ipc::TCPSocket::AIOAcceptControlBlock::execute()
{
  accepted_tcp_socket =
    static_cast<TCPSocket*>( get_socket().get() )->accept();

  if ( accepted_tcp_socket != NULL )
    onCompletion( 0 );
#ifdef _WIN32
  else if ( WSAGetLastError() == WSAEWOULDBLOCK )
#else
  else if ( errno == EWOULDBLOCK )
#endif
    return EXECUTE_STATUS_WANT_READ;
  else
#ifdef _WIN32
    onError( WSAGetLastError() );
#else
    onError( errno );
#endif

  return EXECUTE_STATUS_DONE;
}


#ifdef _WIN32
YIELD::ipc::TCPSocket::TCPSocket( int domain, SOCKET socket_ )
#else
YIELD::ipc::TCPSocket::TCPSocket( int domain, int socket_ )
#endif
  : Socket( domain, SOCK_STREAM, IPPROTO_TCP, socket_ )
{
  partial_write_len = 0;
}

YIELD::ipc::auto_TCPSocket YIELD::ipc::TCPSocket::accept()
{
#ifdef _WIN32
  SOCKET peer_socket = _accept();
  if ( peer_socket != INVALID_SOCKET )
#else
  int peer_socket = _accept();
  if ( peer_socket != -1 )
#endif
    return new TCPSocket( get_domain(), peer_socket );
  else
    return NULL;
}

#ifdef _WIN32
SOCKET YIELD::ipc::TCPSocket::_accept()
#else
int YIELD::ipc::TCPSocket::_accept()
#endif
{
  sockaddr_storage peername_storage;
  socklen_t peername_storage_len = sizeof( peername_storage );
  return ::accept
         (
           *this,
           reinterpret_cast<struct sockaddr*>( &peername_storage ),
           &peername_storage_len
         );
}

void
YIELD::ipc::TCPSocket::aio_accept
(
  auto_AIOAcceptControlBlock aio_accept_control_block
)
{
#ifdef _WIN32
  aio_accept_iocp( aio_accept_control_block );
#else
  aio_accept_nbio( aio_accept_control_block );
#endif
}

#ifdef _WIN32
void
YIELD::ipc::TCPSocket::aio_accept_iocp
(
  auto_AIOAcceptControlBlock aio_accept_control_block
)
{
  aio_accept_control_block->set_socket( *this );
  get_aio_queue().associate( *this );

  if ( lpfnAcceptEx == NULL )
  {
    GUID GuidAcceptEx = WSAID_ACCEPTEX;
    DWORD dwBytes;
    WSAIoctl
    (
      *this,
      SIO_GET_EXTENSION_FUNCTION_POINTER,
      &GuidAcceptEx,
      sizeof( GuidAcceptEx ),
      &lpfnAcceptEx,
      sizeof( lpfnAcceptEx ),
      &dwBytes,
      NULL,
      NULL
    );
  }

  aio_accept_control_block->accepted_tcp_socket = TCPSocket::create( get_domain() );

  DWORD sizeof_peername;
  if ( get_domain() == AF_INET6 )
    sizeof_peername = sizeof( sockaddr_in6 );
  else
    sizeof_peername = sizeof( sockaddr_in );

  DWORD dwBytesReceived;
  if
  (
    static_cast<LPFN_ACCEPTEX>( lpfnAcceptEx )
    (
      *this,
      *aio_accept_control_block->accepted_tcp_socket,
      aio_accept_control_block->peername,
      0,
      sizeof_peername + 16,
      sizeof_peername + 16,
      &dwBytesReceived,
      ( LPOVERLAPPED )*aio_accept_control_block
    )
    ||
    WSAGetLastError() == WSA_IO_PENDING
  )
    aio_accept_control_block.release();
  else
    aio_accept_control_block->onError( WSAGetLastError() );
}
#endif

void
YIELD::ipc::TCPSocket::aio_accept_nbio
(
  auto_AIOAcceptControlBlock aio_accept_control_block
)
{
  aio_accept_control_block->set_socket( *this );
  set_blocking_mode( false );
  get_aio_queue().submit( aio_accept_control_block.release() );
}

void
YIELD::ipc::TCPSocket::aio_connect
(
  auto_AIOConnectControlBlock aio_connect_control_block
)
{
#ifdef _WIN32
  aio_connect_iocp( aio_connect_control_block );
#else
  aio_connect_nbio( aio_connect_control_block );
#endif
}

#ifdef _WIN32
void
YIELD::ipc::TCPSocket::aio_connect_iocp
(
  auto_AIOConnectControlBlock aio_connect_control_block
)
{
  aio_connect_control_block->set_socket( *this );

  if ( lpfnConnectEx == NULL )
  {
    GUID GuidConnectEx = WSAID_CONNECTEX;
    DWORD dwBytes;
    WSAIoctl
    (
      *this,
      SIO_GET_EXTENSION_FUNCTION_POINTER,
      &GuidConnectEx,
      sizeof( GuidConnectEx ),
      &lpfnConnectEx,
      sizeof( lpfnConnectEx ),
      &dwBytes,
      NULL,
      NULL
    );
  }

  for ( ;; )
  {
    struct sockaddr* name; socklen_t namelen;
    if
    (
      aio_connect_control_block->get_peername()->as_struct_sockaddr
      (
        get_domain(),
        name,
        namelen
      )
    )
    {
      if ( bind( SocketAddress::create( NULL, 0 ) ) )
      {
        get_aio_queue().associate( *this );

        DWORD dwBytesSent;
        if
        (
          static_cast<LPFN_CONNECTEX>( lpfnConnectEx )
          (
            *this,
            name,
            namelen,
            NULL,
            0,
            &dwBytesSent,
            *aio_connect_control_block
          )
          ||
          WSAGetLastError() == WSA_IO_PENDING
        )
        {
          aio_connect_control_block.release();
          return;
        }
        else
          break;
      }
      else
        break;
    }
    else if ( get_domain() == AF_INET6 )
    {
      if ( recreate( AF_INET ) )
      {
        get_aio_queue().associate( *this );
        continue; // Try to connect again
      }
      else
        break;
    }
    else
      break;
  }

  aio_connect_control_block->onError( WSAGetLastError() );
}
#endif

YIELD::ipc::auto_TCPSocket YIELD::ipc::TCPSocket::create()
{
  return create( AF_INET6 );
}

YIELD::ipc::auto_TCPSocket YIELD::ipc::TCPSocket::create( int domain )
{
#ifdef _WIN32
  SOCKET socket_ = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
  if ( socket_ != INVALID_SOCKET )
#else
  int socket_ = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );;
  if ( socket_ != -1 )
#endif
    return new TCPSocket( domain, socket_ );
  else
    return NULL;
}

bool YIELD::ipc::TCPSocket::listen()
{
  int flag = 1;
  setsockopt
  (
    *this,
    IPPROTO_TCP,
    TCP_NODELAY,
    reinterpret_cast<char*>( &flag ),
    sizeof( int )
  );

  flag = 1;
  setsockopt
  (
    *this,
    SOL_SOCKET,
    SO_KEEPALIVE,
    reinterpret_cast<char*>( &flag ),
    sizeof( int )
  );

  linger lingeropt;
  lingeropt.l_onoff = 1;
  lingeropt.l_linger = 0;
  setsockopt
  (
    *this,
    SOL_SOCKET,
    SO_LINGER,
    reinterpret_cast<char*>( &lingeropt ),
    static_cast<int>( sizeof( lingeropt ) )
  );

  return ::listen( *this, SOMAXCONN ) != -1;
}

bool YIELD::ipc::TCPSocket::shutdown()
{
#ifdef _WIN32
  if ( ::shutdown( *this, SD_BOTH ) == 0 )
#else
  if ( ::shutdown( *this, SHUT_RDWR ) != -1 )
#endif
    return YIELD::ipc::Socket::shutdown();
  else
    return false;
}

ssize_t
YIELD::ipc::TCPSocket::writev
(
  const struct iovec* buffers,
  uint32_t buffers_count
)
{
  size_t buffers_len = 0;
  for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
    buffers_len += buffers[buffer_i].iov_len;

  ssize_t ret = 0;

  for ( ;; )
  {
    // Recalculate these every time we do a socket writev
    // Less efficient than other ways but it reduces the
    // number of (rarely-tested) branches
    uint32_t wrote_until_buffer_i = 0;
    size_t wrote_until_buffer_i_pos = 0;

    if ( partial_write_len > 0 )
    {
      size_t temp_partial_write_len = partial_write_len;
      for ( ;; )
      {
        if ( buffers[wrote_until_buffer_i].iov_len < temp_partial_write_len )
        {
          // The buffer and part of the next was already written
          temp_partial_write_len -= buffers[wrote_until_buffer_i].iov_len;
          wrote_until_buffer_i++;
        }
        else if
        (
          buffers[wrote_until_buffer_i].iov_len == temp_partial_write_len
        )
        {
          // The buffer was already written, but none of the next
          temp_partial_write_len = 0;
          wrote_until_buffer_i++;
          break;
        }
        else // Part of the buffer was written
        {
          wrote_until_buffer_i_pos = temp_partial_write_len;
          break;
        }
      }
    }

    ssize_t Socket_writev_ret;
    if ( wrote_until_buffer_i_pos == 0 ) // Writing whole buffers
    {
      Socket_writev_ret
        = Socket::writev
          (
            &buffers[wrote_until_buffer_i],
            buffers_count - wrote_until_buffer_i
          );
    }
    else // Writing part of a buffer
    {
      struct iovec temp_iovec;

      temp_iovec.iov_base
        = static_cast<char*>( buffers[wrote_until_buffer_i].iov_base )
          + wrote_until_buffer_i_pos;

      temp_iovec.iov_len = buffers[wrote_until_buffer_i].iov_len
                           - wrote_until_buffer_i_pos;

      Socket_writev_ret = Socket::writev( &temp_iovec, 1 );
    }

    if ( Socket_writev_ret > 0 )
    {
      ret += Socket_writev_ret;
      partial_write_len += Socket_writev_ret;
      if ( partial_write_len == buffers_len )
      {
        partial_write_len = 0;
        return ret;
      }
      else
        continue; // A large write filled the socket buffer,
                  // try to write again until we finish or get an error
    }
    else
      return Socket_writev_ret;
  }
}


// tracing_socket.cpp
#ifdef _WIN32
#pragma warning( 4 : 4365 )
#endif


YIELD::ipc::TracingSocket::TracingSocket
(
  auto_Socket underlying_socket,
  YIELD::platform::auto_Log log
)
  : Socket
    (
      underlying_socket->get_domain(),
      underlying_socket->get_type(),
      underlying_socket->get_protocol(),
      *underlying_socket
    ),
    underlying_socket( underlying_socket ), log( log )
{ }

void
YIELD::ipc::TracingSocket::aio_connect
(
  auto_AIOConnectControlBlock aio_connect_control_block
)
{
  aio_connect_nbio( aio_connect_control_block );
}

void
YIELD::ipc::TracingSocket::aio_read
(
  auto_AIOReadControlBlock aio_read_control_block
)
{
  aio_read_nbio( aio_read_control_block );
}

void
YIELD::ipc::TracingSocket::aio_write
(
  auto_AIOWriteControlBlock aio_write_control_block
)
{
  aio_write_nbio( aio_write_control_block );
}

bool YIELD::ipc::TracingSocket::bind( auto_SocketAddress to_sockaddr )
{
  return underlying_socket->bind( to_sockaddr );
}

bool YIELD::ipc::TracingSocket::close()
{
  return underlying_socket->close();
}

bool YIELD::ipc::TracingSocket::connect( auto_SocketAddress to_sockaddr )
{
  std::string to_hostname;
  if ( to_sockaddr->getnameinfo( to_hostname ) )
  {
    log->getStream( YIELD::platform::Log::LOG_INFO ) <<
      "yield::ipc::TracingSocket: connecting socket #" <<
      static_cast<uint64_t>( *this ) <<
      " to " << to_hostname << ".";
  }
  return underlying_socket->connect( to_sockaddr );
}

bool YIELD::ipc::TracingSocket::get_blocking_mode() const
{
  return underlying_socket->get_blocking_mode();
}

YIELD::ipc::auto_SocketAddress YIELD::ipc::TracingSocket::getpeername()
{
  return underlying_socket->getpeername();
}

YIELD::ipc::auto_SocketAddress YIELD::ipc::TracingSocket::getsockname()
{
  return underlying_socket->getsockname();
}

ssize_t YIELD::ipc::TracingSocket::read( void* buffer, size_t buffer_len )
{
  log->getStream( YIELD::platform::Log::LOG_INFO ) <<
    "yield::ipc::TracingSocket: trying to read " << buffer_len <<
    " bytes from socket #" << static_cast<uint64_t>( *this ) << ".";

  ssize_t read_ret = underlying_socket->read( buffer, buffer_len );

  if ( read_ret > 0 )
  {
    log->getStream( YIELD::platform::Log::LOG_INFO ) <<
      "yield::ipc::TracingSocket: read " << read_ret <<
      " bytes from socket #" << static_cast<uint64_t>( *this ) << ".";

    log->write
    (
      buffer,
      static_cast<size_t>( read_ret ),
      YIELD::platform::Log::LOG_DEBUG
    );

    log->write( "\n", YIELD::platform::Log::LOG_DEBUG );
  }
  else if
  (
    read_ret == 0 ||
    ( !underlying_socket->want_read() && !underlying_socket->want_write() )
  )
    log->getStream( YIELD::platform::Log::LOG_INFO ) <<
      "yield::ipc::TracingSocket: lost connection while trying to read " <<
      "socket #" << static_cast<uint64_t>( *this ) << ".";

  return read_ret;
}

bool YIELD::ipc::TracingSocket::set_blocking_mode( bool blocking )
{
  return underlying_socket->set_blocking_mode( blocking );
}


bool YIELD::ipc::TracingSocket::want_connect() const
{
  bool want_connect_ret = underlying_socket->want_connect();

  if ( want_connect_ret )
  {
    log->getStream( YIELD::platform::Log::LOG_INFO ) <<
      "yield::ipc::TracingSocket: would block on connect on socket #" <<
      static_cast<uint64_t>( *this ) << ".";
  }

  return want_connect_ret;
}

bool YIELD::ipc::TracingSocket::want_read() const
{
  bool want_read_ret = underlying_socket->want_read();

  if ( want_read_ret )
  {
    log->getStream( YIELD::platform::Log::LOG_INFO ) <<
      "yield::ipc::TracingSocket: would block on read on socket #" <<
      static_cast<uint64_t>( *this ) << ".";
  }

  return want_read_ret;
}

bool YIELD::ipc::TracingSocket::want_write() const
{
  bool want_write_ret = underlying_socket->want_write();

  if ( want_write_ret )
  {
    log->getStream( YIELD::platform::Log::LOG_INFO ) <<
      "yield::ipc::TracingSocket: would block on write on socket #" <<
      static_cast<uint64_t>( *this ) << ".";
  }

  return want_write_ret;
}

ssize_t
YIELD::ipc::TracingSocket::writev
(
  const struct iovec* buffers,
  uint32_t buffers_count
)
{
  size_t buffers_len = 0;
  for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
    buffers_len += buffers[buffer_i].iov_len;

  log->getStream( YIELD::platform::Log::LOG_INFO ) <<
    "yield::ipc::TracingSocket: trying to write " << buffers_len <<
    " bytes to socket #" << static_cast<uint64_t>( *this ) << ".";

  ssize_t writev_ret = underlying_socket->writev( buffers, buffers_count );

  if ( writev_ret >= 0 )
  {
    size_t temp_writev_ret = static_cast<size_t>( writev_ret );
    log->getStream( YIELD::platform::Log::LOG_INFO ) <<
      "yield::ipc::TracingSocket: wrote " << writev_ret <<
      " bytes to socket #" << static_cast<uint64_t>( *this ) << ".";

    for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
    {
      if ( buffers[buffer_i].iov_len <= temp_writev_ret )
      {
        log->write
        (
          buffers[buffer_i].iov_base,
          buffers[buffer_i].iov_len,
          YIELD::platform::Log::LOG_DEBUG
        );

        temp_writev_ret -= buffers[buffer_i].iov_len;
      }
      else
      {
        log->write
        (
          buffers[buffer_i].iov_base,
          temp_writev_ret,
          YIELD::platform::Log::LOG_DEBUG
        );

        break;
      }
    }
    log->write( "\n", YIELD::platform::Log::LOG_DEBUG );
  }
  else if
  (
    !underlying_socket->want_read() &&
    !underlying_socket->want_write()
  )
  {
    log->getStream( YIELD::platform::Log::LOG_DEBUG ) <<
      "yield::ipc::TracingSocket: lost connection while trying to write to " <<
      "socket #" <<  static_cast<uint64_t>( *this ) << ".";
  }

  return writev_ret;
}


// udp_socket.cpp
#if defined(_WIN32)
#pragma warning( 4 : 4365 )
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <sys/socket.h>
#endif


YIELD::ipc::Socket::AIOControlBlock::ExecuteStatus
YIELD::ipc::UDPSocket::AIORecvFromControlBlock::execute()
{
  ssize_t recvfrom_ret =
    static_cast<UDPSocket*>( get_socket().get() )->
      recvfrom( buffer, *peername );

  if ( recvfrom_ret > 0 )
    onCompletion( static_cast<size_t>( recvfrom_ret ) );
  else if ( recvfrom_ret == 0 )
    DebugBreak();
  else if ( get_socket()->want_read() )
    return EXECUTE_STATUS_WANT_READ;
  else
#ifdef _WIN32
    onError( WSAGetLastError() );
#else
    onError( errno );
#endif

  return EXECUTE_STATUS_DONE;
}


YIELD::ipc::UDPSocket::UDPSocket
(
  int domain,
#ifdef _WIN32
  SOCKET socket_
#else
  int socket_
#endif
)
  : Socket( domain, SOCK_DGRAM, IPPROTO_UDP, socket_ )
{ }

void
YIELD::ipc::UDPSocket::aio_recvfrom
(
  auto_AIORecvFromControlBlock aio_recvfrom_control_block
)
{
#ifdef _WIN32
  aio_recvfrom_iocp( aio_recvfrom_control_block );
#else
  aio_recvfrom_nbio( aio_recvfrom_control_block );
#endif
}

#ifdef _WIN32
void
YIELD::ipc::UDPSocket::aio_recvfrom_iocp
(
  auto_AIORecvFromControlBlock aio_recvfrom_control_block
)
{
  aio_recvfrom_control_block->set_socket( *this );
  get_aio_queue().associate( *this );

  yidl::runtime::auto_Buffer buffer( aio_recvfrom_control_block->get_buffer() );
  WSABUF wsabuf[1];
  wsabuf[0].buf = static_cast<CHAR*>( static_cast<void*>( *buffer ) );
  wsabuf[0].len = static_cast<ULONG>( buffer->capacity() - buffer->size() );
  DWORD dwNumberOfBytesReceived, dwFlags = 0;
  socklen_t peername_len = sizeof( *aio_recvfrom_control_block->peername );
  if
  (
    WSARecvFrom
    (
      *this,
      wsabuf,
      1,
      &dwNumberOfBytesReceived,
      &dwFlags,
      reinterpret_cast<struct sockaddr*>
      (
        aio_recvfrom_control_block->peername
      ),
      &peername_len,
      *aio_recvfrom_control_block,
      NULL
    ) == 0
    ||
    WSAGetLastError() == WSA_IO_PENDING
  )
       aio_recvfrom_control_block.release();
  else
    aio_recvfrom_control_block->onError( WSAGetLastError() );
}
#endif

void
YIELD::ipc::UDPSocket::aio_recvfrom_nbio
(
  auto_AIORecvFromControlBlock aio_recvfrom_control_block
)
{
  aio_recvfrom_control_block->set_socket( *this );
  set_blocking_mode( false );
  get_aio_queue().submit( aio_recvfrom_control_block.release() );
}

YIELD::ipc::auto_UDPSocket YIELD::ipc::UDPSocket::create()
{
  int domain = AF_INET6;
#ifdef _WIN32
  SOCKET socket_ = Socket::create( domain, SOCK_DGRAM, IPPROTO_UDP );
  if ( socket_ != INVALID_SOCKET )
#else
  int socket_ = Socket::create( domain, SOCK_DGRAM, IPPROTO_UDP );
  if ( socket_ != -1 )
#endif
    return new UDPSocket( domain, socket_ );
  else
  {
    domain = AF_INET;
    socket_ = Socket::create( domain, SOCK_DGRAM, IPPROTO_UDP );
    if ( socket_ != -1 )
      return new UDPSocket( domain, socket_ );
    else
      return NULL;
  }
}

ssize_t
YIELD::ipc::UDPSocket::recvfrom
(
  yidl::runtime::auto_Buffer buffer,
  struct sockaddr_storage& peername
)
{
  ssize_t recvfrom_ret
    = recvfrom
      (
        static_cast<char*>( *buffer ) + buffer->size(),
        buffer->capacity() - buffer->size(),
        peername
      );

  if ( recvfrom_ret > 0 )
    buffer->put( NULL, static_cast<size_t>( recvfrom_ret ) );

  return recvfrom_ret;
}

ssize_t
YIELD::ipc::UDPSocket::recvfrom
(
  void* buffer,
  size_t buffer_len,
  struct sockaddr_storage& peername
)
{
  socklen_t peername_len = sizeof( peername );
  return ::recvfrom
         (
           *this,
           static_cast<char*>( buffer ),
           buffer_len,
           0,
           reinterpret_cast<struct sockaddr*>( &peername ),
           &peername_len
         );
}

ssize_t
YIELD::ipc::UDPSocket::sendto
(
  yidl::runtime::auto_Buffer buffer,
  auto_SocketAddress peername
)
{
  return sendto( static_cast<void*>( *buffer ), buffer->size(), peername );
}

ssize_t
YIELD::ipc::UDPSocket::sendto
(
  const void* buffer,
  size_t buffer_len,
  auto_SocketAddress _peername
)
{
  struct sockaddr* peername; socklen_t peername_len;
  if ( _peername->as_struct_sockaddr( get_domain(), peername, peername_len ) )
  {
    return ::sendto
           (
             *this,
             static_cast<const char*>( buffer ),
             buffer_len,
             0,
             peername,
             peername_len
           );
  }
  else
    return -1;
}


YIELD::ipc::UDPSocket::AIORecvFromControlBlock::AIORecvFromControlBlock
(
  yidl::runtime::auto_Buffer buffer
)
: buffer( buffer )
{
  peername = new sockaddr_storage;
}

YIELD::ipc::UDPSocket::AIORecvFromControlBlock::~AIORecvFromControlBlock()
{
  delete peername;
}

YIELD::ipc::auto_SocketAddress
YIELD::ipc::UDPSocket::AIORecvFromControlBlock::get_peername() const
{
  return new SocketAddress( *peername );
}


// uri.cpp
extern "C"
{
};


YIELD::ipc::URI::URI( const char* uri )
{
  init( uri, strnlen( uri, UINT16_MAX ) );
}

YIELD::ipc::URI::URI( const std::string& uri )
{
  init( uri.c_str(), uri.size() );
}

YIELD::ipc::URI::URI( const char* uri, size_t uri_len )
{
  init( uri, uri_len );
}

YIELD::ipc::URI::URI( const char* scheme, const char* host, uint16_t port )
  : scheme( scheme ), host( host ), port( port ), resource( "/" )
{ }

YIELD::ipc::URI::URI
(
  const char* scheme,
  const char* host,
  uint16_t port,
  const char* resource
)
  : scheme( scheme ), host( host ), port( port ), resource( resource )
{ }

YIELD::ipc::URI::URI( const URI& other )
: scheme( other.scheme ), user( other.user ), password( other.password ),
  host( other.host ), port( other.port ), resource( other.resource )
{ }

YIELD::ipc::auto_URI YIELD::ipc::URI::parse( const char* uri )
{
  return parse( uri, strnlen( uri, UINT16_MAX ) );
}

YIELD::ipc::auto_URI YIELD::ipc::URI::parse( const std::string& uri )
{
  return parse( uri.c_str(), uri.size() );
}

YIELD::ipc::auto_URI YIELD::ipc::URI::parse( const char* uri, size_t uri_len )
{
  UriParserStateA parser_state;
  UriUriA parsed_uri;
  parser_state.uri = &parsed_uri;
  if ( uriParseUriExA( &parser_state, uri, uri + uri_len ) == URI_SUCCESS )
  {
    URI* uri = new URI( parsed_uri );
    uriFreeUriMembersA( &parsed_uri );
    return uri;
  }
  else
  {
    uriFreeUriMembersA( &parsed_uri );
    return NULL;
  }
}

void YIELD::ipc::URI::init( const char* uri, size_t uri_len )
{
  UriParserStateA parser_state;
  UriUriA parsed_uri;
  parser_state.uri = &parsed_uri;
  if ( uriParseUriExA( &parser_state, uri, uri + uri_len ) == URI_SUCCESS )
  {
    init( parsed_uri );
    uriFreeUriMembersA( &parsed_uri );
  }
  else
  {
    uriFreeUriMembersA( &parsed_uri );
    throw YIELD::platform::Exception( "invalid URI" );
  }
}

void YIELD::ipc::URI::init( UriUriA& parsed_uri )
{
  scheme.assign
  (
    parsed_uri.scheme.first,
    parsed_uri.scheme.afterLast - parsed_uri.scheme.first
  );

  host.assign
  (
    parsed_uri.hostText.first,
    parsed_uri.hostText.afterLast - parsed_uri.hostText.first
  );

  if ( parsed_uri.portText.first != NULL )
  {
    port
      = static_cast<uint16_t>
        (
          strtol
          (
            parsed_uri.portText.first,
            NULL,
            0
          )
        );
  }
  else
    port = 0;

  if ( parsed_uri.userInfo.first != NULL )
  {
    const char* userInfo_p = parsed_uri.userInfo.first;
    while ( userInfo_p < parsed_uri.userInfo.afterLast )
    {
      if ( *userInfo_p == ':' )
      {
        user.assign
        (
          parsed_uri.userInfo.first,
          userInfo_p - parsed_uri.userInfo.first
        );

        password.assign
        (
          userInfo_p + 1,
          parsed_uri.userInfo.afterLast - userInfo_p - 1
        );

        break;
      }
      userInfo_p++;
    }

    if ( user.empty() ) // No colon found => no password, just the user
    {
      user.assign
      (
        parsed_uri.userInfo.first,
        parsed_uri.userInfo.afterLast - parsed_uri.userInfo.first
      );
    }
  }

  if ( parsed_uri.pathHead != NULL )
  {
    UriPathSegmentA* path_segment = parsed_uri.pathHead;
    do
    {
      resource.append( "/" );
      resource.append
      (
        path_segment->text.first,
        path_segment->text.afterLast - path_segment->text.first
      );
      path_segment = path_segment->next;
    }
    while ( path_segment != NULL );

    if ( parsed_uri.query.first != NULL )
    {
      UriQueryListA* query_list;
      uriDissectQueryMallocA
      (
        &query_list,
        NULL,
        parsed_uri.query.first,
        parsed_uri.query.afterLast
      );

      UriQueryListA* query_list_p = query_list;
      while ( query_list_p != NULL )
      {
        query.insert( std::make_pair( query_list_p->key, query_list_p->value ) );
        query_list_p = query_list_p->next;
      }

      uriFreeQueryListA( query_list );
    }
  }
  else
    resource = "/";
}

std::string
YIELD::ipc::URI::get_query_value
(
  const std::string& key,
  const char* default_query_value
) const
{
  std::multimap<std::string, std::string>::const_iterator
    query_value_i = query.find( key );

  if ( query_value_i != query.end() )
    return query_value_i->second;
  else
    return default_query_value;
}

std::multimap<std::string, std::string>::const_iterator
YIELD::ipc::URI::get_query_values( const std::string& key ) const
{
  return query.find( key );
}


// uuid.cpp
#if defined(_WIN32)
#include <Rpc.h>
#pragma comment( lib, "Rpcrt4.lib" )
#endif

#ifdef YIELD_IPC_HAVE_OPENSSL
#include <openssl/sha.h>
#endif

#ifndef _WIN32
#include <cstring>
#endif


YIELD::ipc::UUID::UUID()
{
#if defined(_WIN32)
  win32_uuid = new ::UUID;
  UuidCreate( static_cast<::UUID*>( win32_uuid ) );
#elif defined(YIELD_IPC_HAVE_LIBUUID)
  uuid_generate( libuuid_uuid );
#else
  std::strncpy( generic_uuid, Socket::getfqdn().c_str(), 256 );
#ifdef YIELD_IPC_HAVE_OPENSSL
  SHA_CTX ctx; SHA1_Init( &ctx );
  SHA1_Update( &ctx, generic_uuid, strlen( generic_uuid ) );
  memset( generic_uuid, 0, sizeof( generic_uuid ) );
  unsigned char sha1sum[SHA_DIGEST_LENGTH]; SHA1_Final( sha1sum, &ctx );
  static char hex_array[]
    =
    {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F'
    };
  unsigned int sha1sum_i = 0, generic_uuid_i = 0;
  for ( ; sha1sum_i < SHA_DIGEST_LENGTH; sha1sum_i++, generic_uuid_i += 2 )
  {
   generic_uuid[generic_uuid_i]
     = hex_array[( sha1sum[sha1sum_i] & 0xf0 ) >> 4];

   generic_uuid[generic_uuid_i+1]
     = hex_array[( sha1sum[sha1sum_i] & 0x0f )];
  }
  generic_uuid[generic_uuid_i] = 0;
#endif
#endif
}

YIELD::ipc::UUID::UUID( const std::string& from_string )
{
#if defined(_WIN32)
  win32_uuid = new ::UUID;
  UuidFromStringA
  (
    reinterpret_cast<RPC_CSTR>( const_cast<char*>( from_string.c_str() ) ),
    static_cast<::UUID*>( win32_uuid )
  );
#elif defined(YIELD_IPC_HAVE_LIBUUID)
  uuid_parse( from_string.c_str(), libuuid_uuid );
#else
  std::strncpy( generic_uuid, from_string.c_str(), 256 );
#endif
}

YIELD::ipc::UUID::~UUID()
{
#if defined(_WIN32)
  delete static_cast<::UUID*>( win32_uuid );
#endif
}

bool YIELD::ipc::UUID::operator==( const YIELD::ipc::UUID& other ) const
{
#ifdef _WIN32
  return memcmp( win32_uuid, other.win32_uuid, sizeof( ::UUID ) ) == 0;
#elif defined(YIELD_IPC_HAVE_LIBUUID)
  return uuid_compare( libuuid_uuid, other.libuuid_uuid ) == 0;
#else
  return strncmp( generic_uuid, other.generic_uuid, 256 );
#endif
}

YIELD::ipc::UUID::operator std::string() const
{
#if defined(_WIN32)
  RPC_CSTR temp_to_string;
  UuidToStringA( static_cast<::UUID*>( win32_uuid ), &temp_to_string );
  std::string to_string( reinterpret_cast<char*>( temp_to_string ) );
  RpcStringFreeA( &temp_to_string );
  return to_string;
#elif defined(YIELD_IPC_HAVE_LIBUUID)
  char out[37];
  uuid_unparse( libuuid_uuid, out );
  return out;
#else
  return generic_uuid;
#endif
}

