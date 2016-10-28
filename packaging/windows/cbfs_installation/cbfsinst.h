// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the CBFSISTALL_EXPORTS
// symbol defined on the command line. this symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// CBFSISTALL_API functions as being imported from a DLL, whereas this DLL sees symbols
// defined with this macro as being exported.
#ifdef CBFSINST_EXPORTS
#ifdef __cplusplus
#define CBFSINST_API extern "C" __declspec(dllexport)
#else
#define CBFSINST_API __declspec(dllexport)
#endif
#else
#define CBFSINST_API __declspec(dllimport)
#endif

#define CBFS_MODULE_PNP_BUS                0x00000001
#define CBFS_MODULE_DRIVER                 0x00000002
#define CBFS_MODULE_NET_REDIRECTOR_DLL     0x00010000
#define CBFS_MODULE_MOUNT_NOTIFIER_DLL     0x00020000

CBFSINST_API BOOL __stdcall InstallA(
    IN LPCSTR  CabPathName, 
    IN LPCSTR  ProductName,
    IN LPCSTR PathToInstall,
    IN BOOL SupportPnP,
    IN DWORD ModulesToInstall,
    OUT LPDWORD RebootNeeded
    );

CBFSINST_API BOOL __stdcall InstallW(
    IN LPCWSTR  CabPathName, 
    IN LPCWSTR ProductName,
    IN LPCWSTR PathToInstall,
    IN BOOL SupportPnP,
    IN DWORD ModulesToInstall,
    OUT LPDWORD RebootNeeded
    );

CBFSINST_API BOOL __stdcall UninstallA(
    IN LPCSTR  CabPathName, 
    IN LPCSTR   ProductName,
    IN LPCSTR InstalledPath OPTIONAL,
    OUT LPDWORD  RebootNeeded
    );

CBFSINST_API BOOL __stdcall UninstallW(
    IN LPCWSTR  CabPathName, 
    IN LPCWSTR  ProductName,
    IN LPCWSTR InstalledPath OPTIONAL,
    OUT LPDWORD  RebootNeeded
    );

CBFSINST_API BOOL __stdcall GetModuleStatusA(
    IN LPCSTR   ProductName,
    IN DWORD    Module,
    OUT LPBOOL  Installed,
    OUT LPDWORD FileVersionHigh OPTIONAL,
    OUT LPDWORD FileVersionLow OPTIONAL
    );

CBFSINST_API BOOL __stdcall GetModuleStatusW(
    IN LPCWSTR  ProductName,
    IN DWORD    Module,
    OUT LPBOOL  Installed,
    OUT LPDWORD FileVersionHigh OPTIONAL,
    OUT LPDWORD FileVersionLow OPTIONAL
    );

CBFSINST_API BOOL __stdcall InstallIconA(
    IN LPCSTR ProductName,
    IN LPCSTR IconPath,
    IN LPCSTR IconId,
    OUT LPBOOL  RebootNeeded
    );

CBFSINST_API BOOL __stdcall InstallIconW(
    IN LPCWSTR ProductName,
    IN LPCWSTR IconPath,
    IN LPCWSTR IconId,
    OUT LPBOOL  RebootNeeded
    );

CBFSINST_API BOOL __stdcall UninstallIconA(
    IN LPCSTR ProductName,
    IN LPCSTR IconId,
    OUT LPBOOL  RebootNeeded
    );

CBFSINST_API BOOL __stdcall UninstallIconW(
    IN LPCWSTR ProductName,
    IN LPCWSTR IconId,
    OUT LPBOOL  RebootNeeded
    );
