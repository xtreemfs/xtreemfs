@echo off

Setlocal EnableDelayedExpansion

FOR %%p IN (flease foundation pbrpcgen servers) DO (
  FOR %%f IN (.classpath .project) DO (
    set dest=%CD%\%%p\%%f
    
    IF not exist !dest! (
      set source=%cd%\%%p\eclipse-project\%%f
      
      if exist !source! (
        copy "!source!" "!dest!"
      ) else (
        echo !source! not found and therefore not copied.
      )
    )
  )
)

pause