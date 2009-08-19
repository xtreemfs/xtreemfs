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

#include "uriparser.h"


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
				URI_TYPE(PathSegment) * const prev = walker->reserved;
				URI_TYPE(PathSegment) * const nextBackup = walker->next;

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
				URI_TYPE(PathSegment) * const prev = walker->reserved;
				URI_TYPE(PathSegment) * prevPrev;
				URI_TYPE(PathSegment) * const nextBackup = walker->next;

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
						prevPrev = prev->reserved;
						if (prevPrev != NULL) {
							/* Not even prev is the first one */
							prevPrev->next = walker->next;
							if (walker->next != NULL) {
								walker->next->reserved = prevPrev;
							} else {
								/* Last segment -> insert "" segment to represent trailing slash, update tail */
								URI_TYPE(PathSegment) * const segment = malloc(1 * sizeof(URI_TYPE(PathSegment)));
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
			URI_TYPE(PathSegment) * cur = malloc(sizeof(URI_TYPE(PathSegment)));
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
		dest->hostData.ip4 = malloc(sizeof(UriIp4));
		if (dest->hostData.ip4 == NULL) {
			return URI_FALSE; /* Raises malloc error */
		}
		*(dest->hostData.ip4) = *(source->hostData.ip4);
		dest->hostData.ip6 = NULL;
		dest->hostData.ipFuture.first = NULL;
		dest->hostData.ipFuture.afterLast = NULL;
	} else if (source->hostData.ip6 != NULL) {
		dest->hostData.ip4 = NULL;
		dest->hostData.ip6 = malloc(sizeof(UriIp6));
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

	if (	/* Case 1: absolute path, empty first segment */
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

	segment = malloc(1 * sizeof(URI_TYPE(PathSegment)));
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

	buffer = malloc(lenInChars * sizeof(URI_CHAR));
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
	buffer = malloc(lenInChars * sizeof(URI_CHAR));
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
		URI_CHAR * dup = malloc(lenInBytes);
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
		state->uri->hostData.ip6 = malloc(1 * sizeof(UriIp6)); /* Freed when stopping on parse error */
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
	state->uri->hostData.ip4 = malloc(1 * sizeof(UriIp4)); /* Freed when stopping on parse error */
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
	state->uri->hostData.ip4 = malloc(1 * sizeof(UriIp4)); /* Freed when stopping on parse error */
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
	state->uri->hostData.ip4 = malloc(1 * sizeof(UriIp4)); /* Freed when stopping on parse error */
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
	URI_TYPE(PathSegment) * segment = malloc(1 * sizeof(URI_TYPE(PathSegment)));
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
	parser.uri->hostData.ip6 = malloc(1 * sizeof(UriIp6));
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
	queryString = malloc(charsRequired * sizeof(URI_CHAR));
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
	*prevNext = malloc(1 * sizeof(URI_TYPE(QueryList)));
	if (*prevNext == NULL) {
		return URI_FALSE; /* Raises malloc error */
	}
	(*prevNext)->next = NULL;


	/* Fill key */
	key = malloc((keyLen + 1) * sizeof(URI_CHAR));
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
		value = malloc((valueLen + 1) * sizeof(URI_CHAR));
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

	/* [01/19]	result = "" */
				if (dest != NULL) {
					dest[0] = _UT('\0');
				} else {
					(*charsRequired) = 0;
				}
	/* [02/19]	if defined(scheme) then */
				if (uri->scheme.first != NULL) {
	/* [03/19]		append scheme to result; */
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
	/* [04/19]		append ":" to result; */
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
	/* [05/19]	endif; */
				}
	/* [06/19]	if defined(authority) then */
				if (URI_FUNC(IsHostSet)(uri)) {
	/* [07/19]		append "//" to result; */
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
	/* [08/19]		append authority to result; */
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
	/* [09/19]	endif; */
				}
	/* [10/19]	append path to result; */
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
	/* [11/19]	if defined(query) then */
				if (uri->query.first != NULL) {
	/* [12/19]		append "?" to result; */
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
	/* [13/19]		append query to result; */
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
	/* [14/19]	endif; */
				}
	/* [15/19]	if defined(fragment) then */
				if (uri->fragment.first != NULL) {
	/* [16/19]		append "#" to result; */
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
	/* [17/19]		append fragment to result; */
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
	/* [18/19]	endif; */
				}
	/* [19/19]	return result; */
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
		URI_TYPE(PathSegment) * const dup = malloc(sizeof(URI_TYPE(PathSegment)));
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
		URI_TYPE(PathSegment) * const dup = malloc(sizeof(URI_TYPE(PathSegment)));
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

	/* [01/32]	if defined(R.scheme) then */
				if (relSource->scheme.first != NULL) {
	/* [02/32]		T.scheme = R.scheme; */
					absDest->scheme = relSource->scheme;
	/* [03/32]		T.authority = R.authority; */
					if (!URI_FUNC(CopyAuthority)(absDest, relSource)) {
						return URI_ERROR_MALLOC;
					}
	/* [04/32]		T.path = remove_dot_segments(R.path); */
					if (!URI_FUNC(CopyPath)(absDest, relSource)) {
						return URI_ERROR_MALLOC;
					}
					if (!URI_FUNC(RemoveDotSegmentsAbsolute)(absDest)) {
						return URI_ERROR_MALLOC;
					}
	/* [05/32]		T.query = R.query; */
					absDest->query = relSource->query;
	/* [06/32]	else */
				} else {
	/* [07/32]		if defined(R.authority) then */
					if (URI_FUNC(IsHostSet)(relSource)) {
	/* [08/32]			T.authority = R.authority; */
						if (!URI_FUNC(CopyAuthority)(absDest, relSource)) {
							return URI_ERROR_MALLOC;
						}
	/* [09/32]			T.path = remove_dot_segments(R.path); */
						if (!URI_FUNC(CopyPath)(absDest, relSource)) {
							return URI_ERROR_MALLOC;
						}
						if (!URI_FUNC(RemoveDotSegmentsAbsolute)(absDest)) {
							return URI_ERROR_MALLOC;
						}
	/* [10/32]			T.query = R.query; */
						absDest->query = relSource->query;
	/* [11/32]		else */
					} else {
	/* [28/32]			T.authority = Base.authority; */
						if (!URI_FUNC(CopyAuthority)(absDest, absBase)) {
							return URI_ERROR_MALLOC;
						}
	/* [12/32]			if (R.path == "") then */
						if (relSource->pathHead == NULL) {
	/* [13/32]				T.path = Base.path; */
							if (!URI_FUNC(CopyPath)(absDest, absBase)) {
								return URI_ERROR_MALLOC;
							}
	/* [14/32]				if defined(R.query) then */
							if (relSource->query.first != NULL) {
	/* [15/32]					T.query = R.query; */
								absDest->query = relSource->query;
	/* [16/32]				else */
							} else {
	/* [17/32]					T.query = Base.query; */
								absDest->query = absBase->query;
	/* [18/32]				endif; */
							}
	/* [19/32]			else */
						} else {
	/* [20/32]				if (R.path starts-with "/") then */
							if (relSource->absolutePath) {
	/* [21/32]					T.path = remove_dot_segments(R.path); */
								if (!URI_FUNC(CopyPath)(absDest, relSource)) {
									return URI_ERROR_MALLOC;
								}
								if (!URI_FUNC(RemoveDotSegmentsAbsolute)(absDest)) {
									return URI_ERROR_MALLOC;
								}
	/* [22/32]				else */
							} else {
	/* [23/32]					T.path = merge(Base.path, R.path); */
								if (!URI_FUNC(CopyPath)(absDest, absBase)) {
									return URI_ERROR_MALLOC;
								}
								if (!URI_FUNC(MergePath)(absDest, relSource)) {
									return URI_ERROR_MALLOC;
								}
	/* [24/32]					T.path = remove_dot_segments(T.path); */
								if (!URI_FUNC(RemoveDotSegmentsAbsolute)(absDest)) {
									return URI_ERROR_MALLOC;
								}

								if (!URI_FUNC(FixAmbiguity)(absDest)) {
									return URI_ERROR_MALLOC;
								}
	/* [25/32]				endif; */
							}
	/* [26/32]				T.query = R.query; */
							absDest->query = relSource->query;
	/* [27/32]			endif; */
						}
						URI_FUNC(FixEmptyTrailSegment)(absDest);
	/* [29/32]		endif; */
					}
	/* [30/32]		T.scheme = Base.scheme; */
					absDest->scheme = absBase->scheme;
	/* [31/32]	endif; */
				}
	/* [32/32]	T.fragment = R.fragment; */
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
	URI_TYPE(PathSegment) * segment = malloc(1 * sizeof(URI_TYPE(PathSegment)));
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

	/* [01/50]	if (A.scheme != Base.scheme) then */
				if (URI_STRNCMP(absSource->scheme.first, absBase->scheme.first,
						absSource->scheme.afterLast - absSource->scheme.first)) {
	/* [02/50]	   T.scheme    = A.scheme; */
					dest->scheme = absSource->scheme;
	/* [03/50]	   T.authority = A.authority; */
					if (!URI_FUNC(CopyAuthority)(dest, absSource)) {
						return URI_ERROR_MALLOC;
					}
	/* [04/50]	   T.path      = A.path; */
					if (!URI_FUNC(CopyPath)(dest, absSource)) {
						return URI_ERROR_MALLOC;
					}
	/* [05/50]	else */
				} else {
	/* [06/50]	   undef(T.scheme); */
					/* NOOP */
	/* [07/50]	   if (A.authority != Base.authority) then */
					if (!URI_FUNC(EqualsAuthority)(absSource, absBase)) {
	/* [08/50]	      T.authority = A.authority; */
						if (!URI_FUNC(CopyAuthority)(dest, absSource)) {
							return URI_ERROR_MALLOC;
						}
	/* [09/50]	      T.path      = A.path; */
						if (!URI_FUNC(CopyPath)(dest, absSource)) {
							return URI_ERROR_MALLOC;
						}
	/* [10/50]	   else */
					} else {
	/* [11/50]	      if domainRootMode then */
						if (domainRootMode == URI_TRUE) {
	/* [12/50]	         undef(T.authority); */
							/* NOOP */
	/* [13/50]	         if (first(A.path) == "") then */
							/* GROUPED */
	/* [14/50]	            T.path   = "/." + A.path; */
								/* GROUPED */
	/* [15/50]	         else */
								/* GROUPED */
	/* [16/50]	            T.path   = A.path; */
								/* GROUPED */
	/* [17/50]	         endif; */
							if (!URI_FUNC(CopyPath)(dest, absSource)) {
								return URI_ERROR_MALLOC;
							}
							dest->absolutePath = URI_TRUE;

							if (!URI_FUNC(FixAmbiguity)(dest)) {
								return URI_ERROR_MALLOC;
							}
	/* [18/50]	      else */
						} else {
							const URI_TYPE(PathSegment) * sourceSeg = absSource->pathHead;
							const URI_TYPE(PathSegment) * baseSeg = absBase->pathHead;
	/* [19/50]	         bool pathNaked = true; */
							UriBool pathNaked = URI_TRUE;
	/* [20/50]	         undef(last(Base.path)); */
							/* NOOP */
	/* [21/50]	         T.path = ""; */
							dest->absolutePath = URI_FALSE;
	/* [22/50]	         while (first(A.path) == first(Base.path)) do */
							while ((sourceSeg != NULL) && (baseSeg != NULL)
									&& !URI_STRNCMP(sourceSeg->text.first, baseSeg->text.first,
									sourceSeg->text.afterLast - sourceSeg->text.first)
									&& !((sourceSeg->text.first == sourceSeg->text.afterLast)
										&& ((sourceSeg->next == NULL) != (baseSeg->next == NULL)))) {
	/* [23/50]	            A.path++; */
								sourceSeg = sourceSeg->next;
	/* [24/50]	            Base.path++; */
								baseSeg = baseSeg->next;
	/* [25/50]	         endwhile; */
							}
	/* [26/50]	         while defined(first(Base.path)) do */
							while ((baseSeg != NULL) && (baseSeg->next != NULL)) {
	/* [27/50]	            Base.path++; */
								baseSeg = baseSeg->next;
	/* [28/50]	            T.path += "../"; */
								if (!URI_FUNC(AppendSegment)(dest, URI_FUNC(ConstParent),
										URI_FUNC(ConstParent) + 2)) {
									return URI_ERROR_MALLOC;
								}
	/* [29/50]	            pathNaked = false; */
								pathNaked = URI_FALSE;
	/* [30/50]	         endwhile; */
							}
	/* [31/50]	         while defined(first(A.path)) do */
							while (sourceSeg != NULL) {
	/* [32/50]	            if pathNaked then */
								if (pathNaked == URI_TRUE) {
	/* [33/50]	               if (first(A.path) contains ":") then */
									UriBool containsColon = URI_FALSE;
									const URI_CHAR * ch = sourceSeg->text.first;
									for (; ch < sourceSeg->text.afterLast; ch++) {
										if (*ch == _UT(':')) {
											containsColon = URI_TRUE;
											break;
										}
									}

									if (containsColon) {
	/* [34/50]	                  T.path += "./"; */
										if (!URI_FUNC(AppendSegment)(dest, URI_FUNC(ConstPwd),
												URI_FUNC(ConstPwd) + 1)) {
											return URI_ERROR_MALLOC;
										}
	/* [35/50]	               elseif (first(A.path) == "") then */
									} else if (sourceSeg->text.first == sourceSeg->text.afterLast) {
	/* [36/50]	                  T.path += "/."; */
										if (!URI_FUNC(AppendSegment)(dest, URI_FUNC(ConstPwd),
												URI_FUNC(ConstPwd) + 1)) {
											return URI_ERROR_MALLOC;
										}
	/* [37/50]	               endif; */
									}
	/* [38/50]	            endif; */
								}
	/* [39/50]	            T.path += first(A.path); */
								if (!URI_FUNC(AppendSegment)(dest, sourceSeg->text.first,
										sourceSeg->text.afterLast)) {
									return URI_ERROR_MALLOC;
								}
	/* [40/50]	            pathNaked = false; */
								pathNaked = URI_FALSE;
	/* [41/50]	            A.path++; */
								sourceSeg = sourceSeg->next;
	/* [42/50]	            if defined(first(A.path)) then */
								/* NOOP */
	/* [43/50]	               T.path += + "/"; */
								/* NOOP */
	/* [44/50]	            endif; */
								/* NOOP */
	/* [45/50]	         endwhile; */
							}
	/* [46/50]	      endif; */
						}
	/* [47/50]	   endif; */
					}
	/* [48/50]	endif; */
				}
	/* [49/50]	T.query     = A.query; */
				dest->query = absSource->query;
	/* [50/50]	T.fragment  = A.fragment; */
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
