'get the environment
Set fso = CreateObject("Scripting.FileSystemObject")
Set WshShell = CreateObject("WScript.Shell")
Set objEnv = WshShell.Environment("PROCESS")

WshShell.Popup "finding the path to the dokanctl.exe to copy it to the windows system directory. this will take several minutes...",10,"dokanCtl"

Dim source 
source = ""
Dim dest 
dest = objenv("SystemRoot") & "\" 

'check the default path
If fso.FileExists(objenv("ProgramFiles") & "\Dokan\DokanLibrary\dokanctl.exe") Then
    source = objenv("ProgramFiles") & "\Dokan\DokanLibrary\dokanctl.exe"
Else
    FindFile(objenv("ProgramFiles") & "\")
End If

If source = "" Then
    MsgBox "execution failed. unmount will not be available on the GUI tool.",0,"dokanCtl"
Else
    fso.CopyFile source, dest, TRUE
    MsgBox "execution successful.",0,"dokanCtl"
End If



Sub FindFile(ThisFolder)

    Dim File
    Dim Folder
    For Each Folder In fso.GetFolder(ThisFolder).SubFolders
       For Each File In Folder.Files
          If LCase(File.Name) = "dokanctl.exe" Then
             Set source = File
	     Exit For
          End If
       Next 'File

       If source = "" Then
          Call FindFile(Folder)
       Else
	  Exit For
       End If

    Next 'Folder

END SUB