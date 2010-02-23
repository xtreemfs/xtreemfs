// Copyright (c) 2010 Minor Gordon
// With original implementations and ideas contributed by Felix Hupfeld
// All rights reserved
// 
// This source file is part of the Yield project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the Yield project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#ifndef _YIELD_MAIN_H_
#define _YIELD_MAIN_H_

#include "yield/platform.h"

#include <algorithm>
#include <cstring>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

#ifdef _WIN32
//#include <vld.h>
#include <windows.h> // For ConsoleCtrlHandler
#else
#ifdef __MACH__
#include <mach-o/dyld.h> // For _NSGetExecutablePath
#endif
#include <signal.h>
#endif




/*! @file SimpleOpt.h

    Copyright (c) 2006-2007, Brodie Thiesfield

    Permission is hereby granted, free of charge, to any person obtaining a
    copy of this software and associated documentation files (the "Software"),
    to deal in the Software without restriction, including without limitation
    the rights to use, copy, modify, merge, publish, distribute, sublicense,
    and/or sell copies of the Software, and to permit persons to whom the
    Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included
    in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
    OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
    IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
    CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
    TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
    SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/


// Default the max arguments to a fixed value. If you want to be able to 
// handle any number of arguments, then predefine this to 0 and it will 
// use an internal dynamically allocated buffer instead.
#ifdef SO_MAX_ARGS
# define SO_STATICBUF   SO_MAX_ARGS
#else
# include <stdlib.h>    // malloc, free
# include <string.h>    // memcpy
# define SO_STATICBUF   50
#endif

//! Error values
typedef enum _ESOError
{
    //! No error
    SO_SUCCESS          =  0,   

    /*! It looks like an option (it starts with a switch character), but 
        it isn't registered in the option table. */
    SO_OPT_INVALID      = -1,   

    /*! Multiple options matched the supplied option text. 
        Only returned when NOT using SO_O_EXACT. */
    SO_OPT_MULTIPLE     = -2,   

    /*! Option doesn't take an argument, but a combined argument was 
        supplied. */
    SO_ARG_INVALID      = -3,   

    /*! SO_REQ_CMB style-argument was supplied to a SO_REQ_SEP option
        Only returned when using SO_O_PEDANTIC. */
    SO_ARG_INVALID_TYPE = -4,   

    //! Required argument was not supplied
    SO_ARG_MISSING      = -5,   

    /*! Option argument looks like another option. 
        Only returned when NOT using SO_O_NOERR. */
    SO_ARG_INVALID_DATA = -6    
} ESOError;

//! Option flags
enum _ESOFlags
{
    /*! Disallow partial matching of option names */
    SO_O_EXACT       = 0x0001, 

    /*! Disallow use of slash as an option marker on Windows. 
        Un*x only ever recognizes a hyphen. */
    SO_O_NOSLASH     = 0x0002, 

    /*! Permit arguments on single letter options with no equals sign. 
        e.g. -oARG or -o[ARG] */
    SO_O_SHORTARG    = 0x0004, 

    /*! Permit single character options to be clumped into a single 
        option string. e.g. "-a -b -c" <==> "-abc" */
    SO_O_CLUMP       = 0x0008, 

    /*! Process the entire argv array for options, including the 
        argv[0] entry. */
    SO_O_USEALL      = 0x0010, 

    /*! Do not generate an error for invalid options. errors for missing 
        arguments will still be generated. invalid options will be 
        treated as files. invalid options in clumps will be silently 
        ignored. */
    SO_O_NOERR       = 0x0020, 

    /*! Validate argument type pedantically. Return an error when a 
        separated argument "-opt arg" is supplied by the user as a 
        combined argument "-opt=arg". By default this is not considered 
        an error. */
    SO_O_PEDANTIC    = 0x0040, 

    /*! Case-insensitive comparisons for short arguments */
    SO_O_ICASE_SHORT = 0x0100, 

    /*! Case-insensitive comparisons for long arguments */
    SO_O_ICASE_LONG  = 0x0200, 

    /*! Case-insensitive comparisons for word arguments 
        i.e. arguments without any hyphens at the start. */
    SO_O_ICASE_WORD  = 0x0400, 

    /*! Case-insensitive comparisons for all arg types */
    SO_O_ICASE       = 0x0700  
};

/*! Types of arguments that options may have. Note that some of the _ESOFlags
    are not compatible with all argument types. SO_O_SHORTARG requires that
    relevant options use either SO_REQ_CMB or SO_OPT. SO_O_CLUMP requires 
    that relevant options use only SO_NONE.
 */
typedef enum _ESOArgType {
    /*! No argument. Just the option flags.
        e.g. -o         --opt */
    SO_NONE,    

    /*! Required separate argument.  
        e.g. -o ARG     --opt ARG */
    SO_REQ_SEP, 

    /*! Required combined argument.  
        e.g. -oARG      -o=ARG      --opt=ARG  */
    SO_REQ_CMB, 

    /*! Optional combined argument.  
        e.g. -o[ARG]    -o[=ARG]    --opt[=ARG] */
    SO_OPT, 

    /*! Multiple separate arguments. The actual number of arguments is
        determined programatically at the time the argument is processed.
        e.g. -o N ARG1 ARG2 ... ARGN    --opt N ARG1 ARG2 ... ARGN */
    SO_MULTI
} ESOArgType;

//! this option definition must be the last entry in the table
#define SO_END_OF_OPTIONS   { -1, NULL, SO_NONE }

#ifdef _DEBUG
# ifdef _MSC_VER
#  include <crtdbg.h>
#  define SO_ASSERT(b)  _ASSERTE(b)
# else
#  include <assert.h>
#  define SO_ASSERT(b)  assert(b)
# endif
#else
# define SO_ASSERT(b)   //!< assertion used to test input data
#endif

// ---------------------------------------------------------------------------
//                              MAIN TEMPLATE CLASS
// ---------------------------------------------------------------------------

/*! @brief Implementation of the SimpleOpt class */
template<class SOCHAR>
class CSimpleOptTempl
{
public:
    /*! @brief Structure used to define all known options. */
    struct SOption {
        /*! ID to return for this flag. Optional but must be >= 0 */
        int nId;        

        /*! arg string to search for, e.g.  "open", "-", "-f", "--file" 
            Note that on Windows the slash option marker will be converted
            to a hyphen so that "-f" will also match "/f". */
        const SOCHAR * pszArg;

        /*! type of argument accepted by this option */
        ESOArgType nArgType;   
    };

    /*! @brief Initialize the class. Init() must be called later. */
    CSimpleOptTempl() 
        : m_rgShuffleBuf(NULL) 
    { 
        Init(0, NULL, NULL, 0); 
    }

    /*! @brief Initialize the class in preparation for use. */
    CSimpleOptTempl(
        int             argc, 
        SOCHAR *        argv[], 
        const SOption * a_rgOptions, 
        int             a_nFlags = 0
        ) 
        : m_rgShuffleBuf(NULL) 
    { 
        Init(argc, argv, a_rgOptions, a_nFlags); 
    }

#ifndef SO_MAX_ARGS
    /*! @brief Deallocate any allocated memory. */
    ~CSimpleOptTempl() { if (m_rgShuffleBuf) free(m_rgShuffleBuf); }
#endif

    /*! @brief Initialize the class in preparation for calling Next.

        The table of options pointed to by a_rgOptions does not need to be
        valid at the time that Init() is called. However on every call to
        Next() the table pointed to must be a valid options table with the
        last valid entry set to SO_END_OF_OPTIONS.

        NOTE: the array pointed to by a_argv will be modified by this
        class and must not be used or modified outside of member calls to
        this class.

        @param a_argc       Argument array size
        @param a_argv       Argument array
        @param a_rgOptions  Valid option array
        @param a_nFlags     Optional flags to modify the processing of 
                            the arguments

        @return true        Successful 
        @return false       if SO_MAX_ARGC > 0:  Too many arguments
                            if SO_MAX_ARGC == 0: Memory allocation failure
    */
    bool Init(
        int             a_argc, 
        SOCHAR *        a_argv[], 
        const SOption * a_rgOptions, 
        int             a_nFlags = 0
        );

    /*! @brief Change the current options table during option parsing.

        @param a_rgOptions  Valid option array
     */
    inline void SetOptions(const SOption * a_rgOptions) { 
        m_rgOptions = a_rgOptions; 
    }

    /*! @brief Change the current flags during option parsing.

        Note that changing the SO_O_USEALL flag here will have no affect.
        It must be set using Init() or the constructor.

        @param a_nFlags     Flags to modify the processing of the arguments
     */
    inline void SetFlags(int a_nFlags) { m_nFlags = a_nFlags; }

    /*! @brief Query if a particular flag is set */
    inline bool HasFlag(int a_nFlag) const { 
        return (m_nFlags & a_nFlag) == a_nFlag; 
    }

    /*! @brief Advance to the next option if available.

        When all options have been processed it will return false. When true
        has been returned, you must check for an invalid or unrecognized
        option using the LastError() method. This will be return an error 
        value other than SO_SUCCESS on an error. All standard data 
        (e.g. OptionText(), OptionArg(), OptionId(), etc) will be available
        depending on the error.

        After all options have been processed, the remaining files from the
        command line can be processed in same order as they were passed to
        the program.

        @return true    option or error available for processing
        @return false   all options have been processed
    */
    bool Next();

    /*! Stops processing of the command line and returns all remaining
        arguments as files. The next call to Next() will return false.
     */
    void Stop();

    /*! @brief Return the last error that occurred.

        This function must always be called before processing the current 
        option. This function is available only when Next() has returned true.
     */
    inline ESOError LastError() const  { return m_nLastError; }

    /*! @brief Return the nId value from the options array for the current
        option.

        This function is available only when Next() has returned true.
     */
    inline int OptionId() const { return m_nOptionId; }

    /*! @brief Return the pszArg from the options array for the current 
        option.

        This function is available only when Next() has returned true.
     */
    inline const SOCHAR * OptionText() const { return m_pszOptionText; }

    /*! @brief Return the argument for the current option where one exists.

        If there is no argument for the option, this will return NULL.
        This function is available only when Next() has returned true.
     */
    inline SOCHAR * OptionArg() const { return m_pszOptionArg; }

    /*! @brief Validate and return the desired number of arguments.

        This is only valid when OptionId() has return the ID of an option
        that is registered as SO_MULTI. It may be called multiple times
        each time returning the desired number of arguments. Previously
        returned argument pointers are remain valid.

        If an error occurs during processing, NULL will be returned and
        the error will be available via LastError().

        @param n    Number of arguments to return.
     */
    SOCHAR ** MultiArg(int n);

    /*! @brief Returned the number of entries in the Files() array.

        After Next() has returned false, this will be the list of files (or
        otherwise unprocessed arguments).
     */
    inline int FileCount() const { return m_argc - m_nLastArg; }

    /*! @brief Return the specified file argument.

        @param n    Index of the file to return. This must be between 0
                    and FileCount() - 1;
     */
    inline SOCHAR * File(int n) const {
        SO_ASSERT(n >= 0 && n < FileCount());
        return m_argv[m_nLastArg + n];
    }

    /*! @brief Return the array of files. */
    inline SOCHAR ** Files() const { return &m_argv[m_nLastArg]; }

private:
    CSimpleOptTempl(const CSimpleOptTempl &); // disabled
    CSimpleOptTempl & operator=(const CSimpleOptTempl &); // disabled

    SOCHAR PrepareArg(SOCHAR * a_pszString) const;
    bool NextClumped();
    void ShuffleArg(int a_nStartIdx, int a_nCount);
    int LookupOption(const SOCHAR * a_pszOption) const;
    int CalcMatch(const SOCHAR *a_pszSource, const SOCHAR *a_pszTest) const;

    // Find the '=' character within a string.
    inline SOCHAR * FindEquals(SOCHAR *s) const {
        while (*s && *s != (SOCHAR)'=') ++s;
        return *s ? s : NULL;
    }
    bool IsEqual(SOCHAR a_cLeft, SOCHAR a_cRight, int a_nArgType) const;

    inline void Copy(SOCHAR ** ppDst, SOCHAR ** ppSrc, int nCount) const {
#ifdef SO_MAX_ARGS
        // keep our promise of no CLIB usage
        while (nCount-- > 0) *ppDst++ = *ppSrc++;
#else
        memcpy(ppDst, ppSrc, nCount * sizeof(SOCHAR*));
#endif
    }

private:
    const SOption * m_rgOptions;     //!< pointer to options table 
    int             m_nFlags;        //!< flags 
    int             m_nOptionIdx;    //!< current argv option index
    int             m_nOptionId;     //!< id of current option (-1 = invalid)
    int             m_nNextOption;   //!< index of next option 
    int             m_nLastArg;      //!< last argument, after this are files
    int             m_argc;          //!< argc to process
    SOCHAR **       m_argv;          //!< argv
    const SOCHAR *  m_pszOptionText; //!< curr option text, e.g. "-f"
    SOCHAR *        m_pszOptionArg;  //!< curr option arg, e.g. "c:\file.txt"
    SOCHAR *        m_pszClump;      //!< clumped single character options
    SOCHAR          m_szShort[3];    //!< temp for clump and combined args
    ESOError        m_nLastError;    //!< error status from the last call
    SOCHAR **       m_rgShuffleBuf;  //!< shuffle buffer for large argc
};

// ---------------------------------------------------------------------------
//                                  IMPLEMENTATION
// ---------------------------------------------------------------------------

template<class SOCHAR>
bool
CSimpleOptTempl<SOCHAR>::Init(
    int             a_argc,
    SOCHAR *        a_argv[],
    const SOption * a_rgOptions,
    int             a_nFlags
    )
{
    m_argc           = a_argc;
    m_nLastArg       = a_argc;
    m_argv           = a_argv;
    m_rgOptions      = a_rgOptions;
    m_nLastError     = SO_SUCCESS;
    m_nOptionIdx     = 0;
    m_nOptionId      = -1;
    m_pszOptionText  = NULL;
    m_pszOptionArg   = NULL;
    m_nNextOption    = (a_nFlags & SO_O_USEALL) ? 0 : 1;
    m_szShort[0]     = (SOCHAR)'-';
    m_szShort[2]     = (SOCHAR)'\0';
    m_nFlags         = a_nFlags;
    m_pszClump       = NULL;

#ifdef SO_MAX_ARGS
	if (m_argc > SO_MAX_ARGS) {
        m_nLastError = SO_ARG_INVALID_DATA;
        m_nLastArg = 0;
		return false;
	}
#else
    if (m_rgShuffleBuf) {
        free(m_rgShuffleBuf);
    }
    if (m_argc > SO_STATICBUF) {
        m_rgShuffleBuf = (SOCHAR**) malloc(sizeof(SOCHAR*) * m_argc);
        if (!m_rgShuffleBuf) {
            return false;
        }
    }
#endif

    return true;
}

template<class SOCHAR>
bool
CSimpleOptTempl<SOCHAR>::Next()
{
#ifdef SO_MAX_ARGS
    if (m_argc > SO_MAX_ARGS) {
        SO_ASSERT(!"Too many args! Check the return value of Init()!");
        return false;
    }
#endif

    // process a clumped option string if appropriate
    if (m_pszClump && *m_pszClump) {
        // silently discard invalid clumped option
        bool bIsValid = NextClumped();
        while (*m_pszClump && !bIsValid && HasFlag(SO_O_NOERR)) {
            bIsValid = NextClumped();
        }

        // return this option if valid or we are returning errors
        if (bIsValid || !HasFlag(SO_O_NOERR)) {
            return true;
        }
    }
    SO_ASSERT(!m_pszClump || !*m_pszClump);
    m_pszClump = NULL;

    // init for the next option
    m_nOptionIdx    = m_nNextOption;
    m_nOptionId     = -1;
    m_pszOptionText = NULL;
    m_pszOptionArg  = NULL;
    m_nLastError    = SO_SUCCESS;

    // find the next option
    SOCHAR cFirst;
    int nTableIdx = -1;
    int nOptIdx = m_nOptionIdx;
    while (nTableIdx < 0 && nOptIdx < m_nLastArg) {
        SOCHAR * pszArg = m_argv[nOptIdx];
        m_pszOptionArg  = NULL;

        // find this option in the options table
        cFirst = PrepareArg(pszArg);
        if (pszArg[0] == (SOCHAR)'-') {
            // find any combined argument string and remove equals sign
            m_pszOptionArg = FindEquals(pszArg);
            if (m_pszOptionArg) {
                *m_pszOptionArg++ = (SOCHAR)'\0';
            }
        }
        nTableIdx = LookupOption(pszArg);

        // if we didn't find this option but if it is a short form
        // option then we try the alternative forms
        if (nTableIdx < 0
            && !m_pszOptionArg
            && pszArg[0] == (SOCHAR)'-'
            && pszArg[1]
            && pszArg[1] != (SOCHAR)'-'
            && pszArg[2])
        {
            // test for a short-form with argument if appropriate
            if (HasFlag(SO_O_SHORTARG)) {
                m_szShort[1] = pszArg[1];
                int nIdx = LookupOption(m_szShort);
                if (nIdx >= 0
                    && (m_rgOptions[nIdx].nArgType == SO_REQ_CMB
                        || m_rgOptions[nIdx].nArgType == SO_OPT))
                {
                    m_pszOptionArg = &pszArg[2];
                    pszArg         = m_szShort;
                    nTableIdx      = nIdx;
                }
            }

            // test for a clumped short-form option string and we didn't
            // match on the short-form argument above
            if (nTableIdx < 0 && HasFlag(SO_O_CLUMP))  {
                m_pszClump = &pszArg[1];
                ++m_nNextOption;
                if (nOptIdx > m_nOptionIdx) {
                    ShuffleArg(m_nOptionIdx, nOptIdx - m_nOptionIdx);
                }
                return Next();
            }
        }

        // The option wasn't found. If it starts with a switch character
        // and we are not suppressing errors for invalid options then it
        // is reported as an error, otherwise it is data.
        if (nTableIdx < 0) {
            if (!HasFlag(SO_O_NOERR) && pszArg[0] == (SOCHAR)'-') {
                m_pszOptionText = pszArg;
                break;
            }
            
            pszArg[0] = cFirst;
            ++nOptIdx;
            if (m_pszOptionArg) {
                *(--m_pszOptionArg) = (SOCHAR)'=';
            }
        }
    }

    // end of options
    if (nOptIdx >= m_nLastArg) {
        if (nOptIdx > m_nOptionIdx) {
            ShuffleArg(m_nOptionIdx, nOptIdx - m_nOptionIdx);
        }
        return false;
    }
    ++m_nNextOption;

    // get the option id
    ESOArgType nArgType = SO_NONE;
    if (nTableIdx < 0) {
        m_nLastError    = (ESOError) nTableIdx; // error code
    }
    else {
        m_nOptionId     = m_rgOptions[nTableIdx].nId;
        m_pszOptionText = m_rgOptions[nTableIdx].pszArg;

        // ensure that the arg type is valid
        nArgType = m_rgOptions[nTableIdx].nArgType;
        switch (nArgType) {
        case SO_NONE:
            if (m_pszOptionArg) {
                m_nLastError = SO_ARG_INVALID;
            }
            break;

        case SO_REQ_SEP:
            if (m_pszOptionArg) {
                // they wanted separate args, but we got a combined one, 
                // unless we are pedantic, just accept it.
                if (HasFlag(SO_O_PEDANTIC)) {
                    m_nLastError = SO_ARG_INVALID_TYPE;
                }
            }
            // more processing after we shuffle
            break;

        case SO_REQ_CMB:
            if (!m_pszOptionArg) {
                m_nLastError = SO_ARG_MISSING;
            }
            break;

        case SO_OPT:
            // nothing to do
            break;

        case SO_MULTI:
            // nothing to do. Caller must now check for valid arguments
            // using GetMultiArg()
            break;
        }
    }

    // shuffle the files out of the way
    if (nOptIdx > m_nOptionIdx) {
        ShuffleArg(m_nOptionIdx, nOptIdx - m_nOptionIdx);
    }

    // we need to return the separate arg if required, just re-use the
    // multi-arg code because it all does the same thing
    if (   nArgType == SO_REQ_SEP 
        && !m_pszOptionArg 
        && m_nLastError == SO_SUCCESS) 
    {
        SOCHAR ** ppArgs = MultiArg(1);
        if (ppArgs) {
            m_pszOptionArg = *ppArgs;
        }
    }

    return true;
}

template<class SOCHAR>
void
CSimpleOptTempl<SOCHAR>::Stop()
{
    if (m_nNextOption < m_nLastArg) {
        ShuffleArg(m_nNextOption, m_nLastArg - m_nNextOption);
    }
}

template<class SOCHAR>
SOCHAR
CSimpleOptTempl<SOCHAR>::PrepareArg(
    SOCHAR * a_pszString
    ) const
{
#ifdef _WIN32
    // On Windows we can accept the forward slash as a single character
    // option delimiter, but it cannot replace the '-' option used to
    // denote stdin. On Un*x paths may start with slash so it may not
    // be used to start an option.
    if (!HasFlag(SO_O_NOSLASH)
        && a_pszString[0] == (SOCHAR)'/'
        && a_pszString[1]
        && a_pszString[1] != (SOCHAR)'-')
    {
        a_pszString[0] = (SOCHAR)'-';
        return (SOCHAR)'/';
    }
#endif
    return a_pszString[0];
}

template<class SOCHAR>
bool
CSimpleOptTempl<SOCHAR>::NextClumped()
{
    // prepare for the next clumped option
    m_szShort[1]    = *m_pszClump++;
    m_nOptionId     = -1;
    m_pszOptionText = NULL;
    m_pszOptionArg  = NULL;
    m_nLastError    = SO_SUCCESS;

    // lookup this option, ensure that we are using exact matching
    int nSavedFlags = m_nFlags;
    m_nFlags = SO_O_EXACT;
    int nTableIdx = LookupOption(m_szShort);
    m_nFlags = nSavedFlags;

    // unknown option
    if (nTableIdx < 0) {
        m_nLastError = (ESOError) nTableIdx; // error code
        return false;
    }

    // valid option
    m_pszOptionText = m_rgOptions[nTableIdx].pszArg;
    ESOArgType nArgType = m_rgOptions[nTableIdx].nArgType;
    if (nArgType == SO_NONE) {
        m_nOptionId = m_rgOptions[nTableIdx].nId;
        return true;
    }

    if (nArgType == SO_REQ_CMB && *m_pszClump) {
        m_nOptionId = m_rgOptions[nTableIdx].nId;
        m_pszOptionArg = m_pszClump;
        while (*m_pszClump) ++m_pszClump; // must point to an empty string
        return true;
    }

    // invalid option as it requires an argument
    m_nLastError = SO_ARG_MISSING;
    return true;
}

// Shuffle arguments to the end of the argv array.
//
// For example:
//      argv[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8" };
//
//  ShuffleArg(1, 1) = { "0", "2", "3", "4", "5", "6", "7", "8", "1" };
//  ShuffleArg(5, 2) = { "0", "1", "2", "3", "4", "7", "8", "5", "6" };
//  ShuffleArg(2, 4) = { "0", "1", "6", "7", "8", "2", "3", "4", "5" };
template<class SOCHAR>
void
CSimpleOptTempl<SOCHAR>::ShuffleArg(
    int a_nStartIdx,
    int a_nCount
    )
{
    SOCHAR * staticBuf[SO_STATICBUF];
    SOCHAR ** buf = m_rgShuffleBuf ? m_rgShuffleBuf : staticBuf;
    int nTail = m_argc - a_nStartIdx - a_nCount;

    // make a copy of the elements to be moved
    Copy(buf, m_argv + a_nStartIdx, a_nCount);

    // move the tail down
    Copy(m_argv + a_nStartIdx, m_argv + a_nStartIdx + a_nCount, nTail);

    // append the moved elements to the tail
    Copy(m_argv + a_nStartIdx + nTail, buf, a_nCount);

    // update the index of the last unshuffled arg
    m_nLastArg -= a_nCount;
}

// match on the long format strings. partial matches will be
// accepted only if that feature is enabled.
template<class SOCHAR>
int
CSimpleOptTempl<SOCHAR>::LookupOption(
    const SOCHAR * a_pszOption
    ) const
{
    int nBestMatch = -1;    // index of best match so far
    int nBestMatchLen = 0;  // matching characters of best match
    int nLastMatchLen = 0;  // matching characters of last best match

    for (int n = 0; m_rgOptions[n].nId >= 0; ++n) {
        // the option table must use hyphens as the option character,
        // the slash character is converted to a hyphen for testing.
        SO_ASSERT(m_rgOptions[n].pszArg[0] != (SOCHAR)'/');

        int nMatchLen = CalcMatch(m_rgOptions[n].pszArg, a_pszOption);
        if (nMatchLen == -1) {
            return n;
        }
        if (nMatchLen > 0 && nMatchLen >= nBestMatchLen) {
            nLastMatchLen = nBestMatchLen;
            nBestMatchLen = nMatchLen;
            nBestMatch = n;
        }
    }

    // only partial matches or no match gets to here, ensure that we
    // don't return a partial match unless it is a clear winner
    if (HasFlag(SO_O_EXACT) || nBestMatch == -1) {
        return SO_OPT_INVALID;
    }
    return (nBestMatchLen > nLastMatchLen) ? nBestMatch : SO_OPT_MULTIPLE;
}

// calculate the number of characters that match (case-sensitive)
// 0 = no match, > 0 == number of characters, -1 == perfect match
template<class SOCHAR>
int
CSimpleOptTempl<SOCHAR>::CalcMatch(
    const SOCHAR *  a_pszSource,
    const SOCHAR *  a_pszTest
    ) const
{
    if (!a_pszSource || !a_pszTest) {
        return 0;
    }

    // determine the argument type
    int nArgType = SO_O_ICASE_LONG;
    if (a_pszSource[0] != '-') {
        nArgType = SO_O_ICASE_WORD;
    }
    else if (a_pszSource[1] != '-' && !a_pszSource[2]) {
        nArgType = SO_O_ICASE_SHORT;
    }

    // match and skip leading hyphens
    while (*a_pszSource == (SOCHAR)'-' && *a_pszSource == *a_pszTest) {
        ++a_pszSource; 
        ++a_pszTest;
    }
    if (*a_pszSource == (SOCHAR)'-' || *a_pszTest == (SOCHAR)'-') {
        return 0;
    }

    // find matching number of characters in the strings
    int nLen = 0;
    while (*a_pszSource && IsEqual(*a_pszSource, *a_pszTest, nArgType)) {
        ++a_pszSource; 
        ++a_pszTest; 
        ++nLen;
    }

    // if we have exhausted the source...
    if (!*a_pszSource) {
        // and the test strings, then it's a perfect match
        if (!*a_pszTest) {
            return -1;
        }

        // otherwise the match failed as the test is longer than
        // the source. i.e. "--mant" will not match the option "--man".
        return 0;
    }

    // if we haven't exhausted the test string then it is not a match
    // i.e. "--mantle" will not best-fit match to "--mandate" at all.
    if (*a_pszTest) {
        return 0;
    }

    // partial match to the current length of the test string
    return nLen;
}

template<class SOCHAR>
bool
CSimpleOptTempl<SOCHAR>::IsEqual(
    SOCHAR  a_cLeft,
    SOCHAR  a_cRight,
    int     a_nArgType
    ) const
{
    // if this matches then we are doing case-insensitive matching
    if (m_nFlags & a_nArgType) {
        if (a_cLeft  >= 'A' && a_cLeft  <= 'Z') a_cLeft  += 'a' - 'A';
        if (a_cRight >= 'A' && a_cRight <= 'Z') a_cRight += 'a' - 'A';
    }
    return a_cLeft == a_cRight;
}

// calculate the number of characters that match (case-sensitive)
// 0 = no match, > 0 == number of characters, -1 == perfect match
template<class SOCHAR>
SOCHAR **
CSimpleOptTempl<SOCHAR>::MultiArg(
    int a_nCount
    )
{
    // ensure we have enough arguments
    if (m_nNextOption + a_nCount > m_nLastArg) {
        m_nLastError = SO_ARG_MISSING;
        return NULL;
    }

    // our argument array
    SOCHAR ** rgpszArg = &m_argv[m_nNextOption];

    // Ensure that each of the following don't start with an switch character.
    // Only make this check if we are returning errors for unknown arguments.
    if (!HasFlag(SO_O_NOERR)) {
        for (int n = 0; n < a_nCount; ++n) {
            SOCHAR ch = PrepareArg(rgpszArg[n]);
            if (rgpszArg[n][0] == (SOCHAR)'-') {
                rgpszArg[n][0] = ch;
                m_nLastError = SO_ARG_INVALID_DATA;
                return NULL;
            }
            rgpszArg[n][0] = ch;
        }
    }

    // all good
    m_nNextOption += a_nCount;
    return rgpszArg;
}


// ---------------------------------------------------------------------------
//                                  TYPE DEFINITIONS
// ---------------------------------------------------------------------------

/*! @brief ASCII/MBCS version of CSimpleOpt */
typedef CSimpleOptTempl<char>    CSimpleOptA; 

/*! @brief wchar_t version of CSimpleOpt */
typedef CSimpleOptTempl<wchar_t> CSimpleOptW; 

#if defined(_UNICODE)
/*! @brief TCHAR version dependent on if _UNICODE is defined */
# define CSimpleOpt CSimpleOptW   
#else
/*! @brief TCHAR version dependent on if _UNICODE is defined */
# define CSimpleOpt CSimpleOptA   
#endif

/* end SimpleOpt.h */



namespace YIELD
{
  class Main
  {
  public:
    const char* get_program_name() const { return program_name; }

#ifdef _WIN32
    virtual int main( char* args ) // From WinMain
    {
      std::vector<char*> argvv;

      char argv0[PATH_MAX];
      GetModuleFileNameA( NULL, argv0, PATH_MAX );
      argvv.push_back( argv0 );

      const char *start_args_p = args, *args_p = start_args_p;
      while ( *args_p != 0 )
      {
        while ( *args_p != ' ' && *args_p != 0 ) args_p++;
        size_t arg_len = args_p - start_args_p;
        char* arg = new char[arg_len+1];
        memcpy_s( arg, arg_len, start_args_p, arg_len );
        arg[arg_len] = 0;
        argvv.push_back( arg );
        if ( *args_p != 0 )
        {
          args_p++;
          start_args_p = args_p;
        }
      }

      int ret = main( static_cast<int>( argvv.size() ), &argvv[0] );

      for
      (
        std::vector<char*>::size_type argvv_i = 1;
        argvv_i < argvv.size();
        argvv_i++
      )
        delete [] argvv[argvv_i];

      return ret;
    }
#endif

    virtual int main( int argc, char** argv )
    {
      int ret = 0;

      try
      {
        std::vector<CSimpleOpt::SOption> simpleopt_options;
        for
        (
          std::vector<Option>::const_iterator option_i = options.begin();
          option_i != options.end();
          option_i++
        )
        {
          const Option& option = *option_i;
          CSimpleOpt::SOption short_simpleopt_option
            =
            {
              option.get_id(),
              option.get_short_arg(),
              option.get_default_values() ? SO_REQ_SEP : SO_NONE
            };
          simpleopt_options.push_back( short_simpleopt_option );

          if ( option.get_long_arg() )
          {
            CSimpleOpt::SOption long_simpleopt_option
              =
              {
                option.get_id(),
                option.get_long_arg(),
                option.get_default_values() ? SO_REQ_SEP : SO_NONE
              };
            simpleopt_options.push_back( long_simpleopt_option );
          }
        }

        CSimpleOpt::SOption sentinel_simpleopt_option = SO_END_OF_OPTIONS;
        simpleopt_options.push_back( sentinel_simpleopt_option );

        // Make copies of the strings in argv so that
        // SimpleOpt can punch holes in them
        std::vector<char*> argvv( argc );
        for ( int arg_i = 0; arg_i < argc; arg_i++ )
        {
          size_t arg_len = strnlen( argv[arg_i], SIZE_MAX ) + 1;
          argvv[arg_i] = new char[arg_len];
          memcpy_s( argvv[arg_i], arg_len, argv[arg_i], arg_len );
        }

        CSimpleOpt args( argc, &argvv[0], &simpleopt_options[0] );

        while ( args.Next() )
        {
          switch ( args.LastError() )
          {
            case SO_SUCCESS:
            {
              if ( args.OptionId() == 0 )
              {
                printUsage();
                return 0;
              }
              else
                parseOption( args.OptionId(), args.OptionArg() );
            }
            break;

            case SO_OPT_INVALID: // Unregistered option
            {
              std::cerr << program_name << ": unknown option: " <<
                           args.OptionText() << std::endl;
              return 1;
            }
            break;

            case SO_OPT_MULTIPLE:
            {
              DebugBreak(); // Should never happen
            }
            break;

            case SO_ARG_INVALID: // Option doesn't take an argument
            {
              std::cerr << program_name << ": " << args.OptionText() <<
                           " does not take an argument." << std::endl;
              return 1;
            }
            break;

            case SO_ARG_INVALID_TYPE:
            {
              DebugBreak(); // Should never happen
            }
            break;

            case SO_ARG_MISSING: // Option missing an argument
            {
              std::cerr << program_name << ": " << args.OptionText() <<
                           " requires an argument." << std::endl;
              return 1;
            }
            break;

            case SO_ARG_INVALID_DATA: // Argument looks like another option
            {
              std::cerr << program_name << ": " << args.OptionText() <<
                           " requires an argument, but you appear to have"
                           << " passed another option." << std::endl;
              return 1;
            }
            break;
          }
        }

        parseFiles( args.FileCount(), args.Files() );

        for
        (
          std::vector<char*>::iterator arg_i = argvv.begin();
          arg_i != argvv.end();
          arg_i++
        )
          delete [] *arg_i;
        argvv.clear();

        // Replace argv[0] with the absolute path to the executable
#if defined(_WIN32)
        char argv0[PATH_MAX];
        if ( GetModuleFileNameA( NULL, argv0, PATH_MAX ) )
          argvv.push_back( argv0 );
        else
          argvv.push_back( argv[0] );
#elif defined(__linux__)
        char argv0[PATH_MAX];
        int ret;
        if ( ( ret = readlink( "/proc/self/exe", argv0, PATH_MAX ) ) != -1 )
        {
          argv0[ret] = 0;
          argvv.push_back( argv0 );
        }
        else
          argvv.push_back( argv[0] );
#elif defined(__MACH__)
        char argv0[PATH_MAX];
        uint32_t bufsize = PATH_MAX;
        if ( _NSGetExecutablePath( argv0, &bufsize ) == 0 )
        {
          argv0[bufsize] = 0;

          char linked_argv0[PATH_MAX]; int ret;
          if ( ( ret = readlink( argv0, linked_argv0, PATH_MAX ) ) != -1 )
          {
            linked_argv0[ret] = 0;
            argvv.push_back( linked_argv0 );
          }
          else
          {
            char absolute_argv0[PATH_MAX];
            if ( realpath( argv0, absolute_argv0 ) != NULL )
              argvv.push_back( absolute_argv0 );
            else
              argvv.push_back( argv0 );
          }
        }
        else
          argvv.push_back( argv[0] );
#elif defined(__sun)
        argvv.push_back( const_cast<char*>( getexecname() ) );
#else
        argvv.push_back( argv[0] );
#endif

        for ( int arg_i = 1; arg_i < argc; arg_i++ )
          argvv.push_back( argv[arg_i] );

        // Pass the original argv to _main instead of the copies
        // SimpleOpt punched holes in
        ret = _main( static_cast<int>( argvv.size() ), &argvv[0] );
      }
      catch ( YIELD::platform::Exception& exc )
      {
        std::cerr << program_name << ": " << exc.what() << std::endl;

        if ( exc.get_error_code() != 0 )
          ret = exc.get_error_code();
        else
          ret = 1;
      }
      // Don't catch std::exceptions like bad_alloc

      // TimerQueue::destroyDefaultTimerQueue();

      return ret;
    }

  protected:
    Main
    (
      const char* program_name,
      const char* program_description = NULL,
      const char* files_usage = NULL
    )
      : program_name( program_name ),
        program_description( program_description ),
        files_usage( files_usage )
    {
      addOption( 0, "-h", "--help" );
    }

    virtual ~Main() { }

    void
    addOption
    (
      int id,
      const char* short_arg,
      const char* long_arg = NULL,
      const char* default_values = NULL
    )
    {
      options.push_back( Option( id, short_arg, long_arg, default_values ) );
    }

    void pause()
    {
#ifdef _WIN32
      SetConsoleCtrlHandler( ConsoleCtrlHandler, TRUE );
      pause_semaphore.acquire();
#else
      signal( SIGINT, SignalHandler );
      ::pause();
#endif
    }

    void printUsage()
    {
      std::cout << std::endl;
      std::cout << program_name;
      if ( program_description )
        std::cout << ": " << program_description;
      std::cout << std::endl;
      std::cout << std::endl;
      std::cout << "Usage:" << std::endl;
      std::cout << "  " << program_name << " [options]";
      if ( files_usage )
        std::cout << " " << files_usage;
      std::cout << std::endl;
      std::cout << std::endl;

      std::sort( options.begin(), options.end() );
      for
      (
        std::vector<Option>::const_iterator option_i = options.begin();
        option_i != options.end();
        option_i++
      )
      {
        const Option& option = *option_i;
        std::cout << "  " << option.get_short_arg();
        if ( option.get_long_arg() )
          std::cout << "/" << option.get_long_arg();
        if ( option.get_default_values() )
          std::cout << "=" << option.get_default_values();
        std::cout << std::endl;
      }

      std::cout << std::endl;
    }

    // Methods for subclasses to override
    virtual int _main( int argc, char** argv ) = 0;
    virtual void parseOption( int, char* ) { }
    virtual void parseFiles( int, char** ) { }

  private:
    const char *program_name, *program_description, *files_usage;


    class Option
    {
    public:
      Option
      (
        int id,
        const char* short_arg,
        const char* long_arg,
        const char* default_values
      )
        : id( id ),
          short_arg( short_arg ),
          long_arg( long_arg ),
          default_values( default_values )
      { }

      int get_id() const { return id; }
      const char* get_short_arg() const { return short_arg; }
      const char* get_long_arg() const { return long_arg; }
      const char* get_default_values() const { return default_values; }

      bool operator<( const Option& other ) const
      {
        return std::strcmp( get_short_arg(), other.get_short_arg() ) < 0;
      }

    private:
      int id;
      const char *short_arg, *long_arg, *default_values;
    };

    std::vector<Option> options;

#ifdef _WIN32
    static YIELD::platform::CountingSemaphore pause_semaphore;

    static BOOL WINAPI ConsoleCtrlHandler( DWORD fdwCtrlType )
    {
      if ( fdwCtrlType == CTRL_C_EVENT )
      {
        pause_semaphore.release();
        return TRUE;
      }
      else
        return FALSE;
    }
#else
   static void SignalHandler( int ) { }
#endif
  };
};


#endif
