@echo off
REM push_all_changes.bat - Stage, commit, and push all changes to the configured remote (origin) and current branch.
REM Usage: push_all_changes.bat "Commit message"

SETLOCAL ENABLEDELAYEDEXPANSION

:: Capture full commit message from all arguments
SET MSG=%*
IF "%MSG%"=="" (
  SET MSG=Save workspace changes
)

necho Checking git repository...
git rev-parse --is-inside-work-tree >nul 2>&1
IF ERRORLEVEL 1 (
  echo Not a git repository. Exiting.
  exit /b 1
)

necho Determining current branch...
for /f "delims=" %%b in ('git rev-parse --abbrev-ref HEAD') do set BRANCH=%%b
echo Current branch: %BRANCH%

necho Checking for remote 'origin'...
git remote get-url origin >nul 2>&1
IF ERRORLEVEL 1 (
  echo Remote 'origin' not found. Listing remotes:
  git remote -v
  echo.
  echo If you need to add a remote, run:
  echo   git remote add origin <url>
  echo Aborting push.
  exit /b 2
)

necho Showing working tree status:
git status --short

necho Adding all changes (including untracked files)...
git add -A

necho Committing with message: "%MSG%"
git commit -m "%MSG%" || (
  echo Commit returned non-zero (there may be nothing to commit). Continuing to push if there are commits not yet pushed.
)

necho Pushing to origin/%BRANCH% (setting upstream if needed)...
git push -u origin %BRANCH%
IF ERRORLEVEL 1 (
  echo Push failed. Common reasons: authentication required, branch protected, or remote URL invalid.
  echo See README_PUSH.md for troubleshooting steps.
  exit /b 3
)

necho Push succeeded.
ENDLOCAL
exit /b 0

