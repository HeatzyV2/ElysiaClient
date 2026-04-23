!ifndef BUILD_UNINSTALLER
!include "LogicLib.nsh"
!include "nsDialogs.nsh"

!define LEGACY_UNINSTALL_PARENT "Software\Microsoft\Windows\CurrentVersion\Uninstall"

Var legacyCleanupCheckbox
Var legacyCleanupRequested
Var legacyRootKey
Var legacyInstallLocation
Var legacyUninstallString
Var legacyRegEntryKey
Var LEGACY_STR_HAYSTACK
Var LEGACY_STR_NEEDLE
Var LEGACY_STR_VAR_1
Var LEGACY_STR_VAR_2
Var LEGACY_STR_VAR_3
Var LEGACY_STR_VAR_4
Var LEGACY_STR_RETURN

Function LegacyStrContains
  Exch $LEGACY_STR_NEEDLE
  Exch 1
  Exch $LEGACY_STR_HAYSTACK

  StrCpy $LEGACY_STR_RETURN ""
  StrCpy $LEGACY_STR_VAR_1 -1
  StrLen $LEGACY_STR_VAR_2 $LEGACY_STR_NEEDLE
  StrLen $LEGACY_STR_VAR_4 $LEGACY_STR_HAYSTACK

legacyStrContainsLoop:
  IntOp $LEGACY_STR_VAR_1 $LEGACY_STR_VAR_1 + 1
  StrCpy $LEGACY_STR_VAR_3 $LEGACY_STR_HAYSTACK $LEGACY_STR_VAR_2 $LEGACY_STR_VAR_1
  StrCmp $LEGACY_STR_VAR_3 $LEGACY_STR_NEEDLE legacyStrContainsFound
  StrCmp $LEGACY_STR_VAR_1 $LEGACY_STR_VAR_4 legacyStrContainsDone
  Goto legacyStrContainsLoop

legacyStrContainsFound:
  StrCpy $LEGACY_STR_RETURN $LEGACY_STR_NEEDLE

legacyStrContainsDone:
  Pop $LEGACY_STR_NEEDLE
  Exch $LEGACY_STR_RETURN
FunctionEnd

!macro LegacyStrContains OUT NEEDLE HAYSTACK
  Push `${HAYSTACK}`
  Push `${NEEDLE}`
  Call LegacyStrContains
  Pop `${OUT}`
!macroend

Function LegacyGetInQuotes
  Exch $R0
  Push $R1
  Push $R2
  Push $R3

   StrCpy $R2 -1
   IntOp $R2 $R2 + 1
    StrCpy $R3 $R0 1 $R2
    StrCmp $R3 "" 0 +3
     StrCpy $R0 ""
     Goto legacyGetInQuotesDone
    StrCmp $R3 '"' 0 -5

   IntOp $R2 $R2 + 1
   StrCpy $R0 $R0 "" $R2

   StrCpy $R2 0
   IntOp $R2 $R2 + 1
    StrCpy $R3 $R0 1 $R2
    StrCmp $R3 "" 0 +3
     StrCpy $R0 ""
     Goto legacyGetInQuotesDone
    StrCmp $R3 '"' 0 -5

   StrCpy $R0 $R0 $R2

legacyGetInQuotesDone:
  Pop $R3
  Pop $R2
  Pop $R1
  Exch $R0
FunctionEnd

Function LegacyGetFileParent
  Exch $R0
  Push $R1
  Push $R2
  Push $R3

  StrCpy $R1 0
  StrLen $R2 $R0

legacyGetFileParentLoop:
  IntOp $R1 $R1 + 1
  IntCmp $R1 $R2 legacyGetFileParentDone 0 legacyGetFileParentDone
  StrCpy $R3 $R0 1 -$R1
  StrCmp $R3 "\" legacyGetFileParentDone
  Goto legacyGetFileParentLoop

legacyGetFileParentDone:
  StrCpy $R0 $R0 -$R1

  Pop $R3
  Pop $R2
  Pop $R1
  Exch $R0
FunctionEnd

Function LegacyEntryMatches
  Exch $0
  Push $1

  !insertmacro LegacyStrContains $1 "Elysia Launcher" "$0"
  ${If} $1 != ""
    StrCpy $0 "1"
    Goto legacyEntryMatchesDone
  ${EndIf}

  !insertmacro LegacyStrContains $1 "Elysia Client" "$0"
  ${If} $1 != ""
    StrCpy $0 "1"
    Goto legacyEntryMatchesDone
  ${EndIf}

  !insertmacro LegacyStrContains $1 "ElysiaClient" "$0"
  ${If} $1 != ""
    StrCpy $0 "1"
    Goto legacyEntryMatchesDone
  ${EndIf}

  StrCpy $0 "0"

legacyEntryMatchesDone:
  Pop $1
  Exch $0
FunctionEnd

Function LegacyResolveInstallLocation
  Push $0

  ${If} $legacyInstallLocation != ""
    Goto legacyResolveDone
  ${EndIf}

  ${If} $legacyUninstallString == ""
    Goto legacyResolveDone
  ${EndIf}

  Push "$legacyUninstallString"
  Call LegacyGetInQuotes
  Pop $0
  ${If} $0 == ""
    Goto legacyResolveDone
  ${EndIf}

  Push $0
  Call LegacyGetFileParent
  Pop $legacyInstallLocation

legacyResolveDone:
  Pop $0
FunctionEnd

Function LegacyEntryWasRemoved
  Push $0

  ${If} $legacyRegEntryKey == ""
    StrCpy $0 "0"
    Goto legacyEntryWasRemovedDone
  ${EndIf}

  ClearErrors
  ${If} $legacyRootKey == "HKEY_CURRENT_USER"
    ReadRegStr $0 HKEY_CURRENT_USER "${LEGACY_UNINSTALL_PARENT}\$legacyRegEntryKey" "UninstallString"
  ${Else}
    ReadRegStr $0 HKEY_LOCAL_MACHINE "${LEGACY_UNINSTALL_PARENT}\$legacyRegEntryKey" "UninstallString"
  ${EndIf}

  ${If} ${Errors}
    StrCpy $0 "1"
  ${Else}
    StrCpy $0 "0"
  ${EndIf}

legacyEntryWasRemovedDone:
  Exch $0
FunctionEnd

Function UninstallLegacyEntry
  Push $0
  Push $1
  Push $2
  Push $3

  ${If} $legacyUninstallString == ""
    Goto legacyUninstallDone
  ${EndIf}

  Call LegacyResolveInstallLocation
  ${If} $legacyInstallLocation == ""
    Goto legacyUninstallDone
  ${EndIf}

  Push "$legacyUninstallString"
  Call LegacyGetInQuotes
  Pop $0
  ${If} $0 == ""
    Goto legacyUninstallDone
  ${EndIf}

  ${If} $legacyRootKey == "HKEY_LOCAL_MACHINE"
    StrCpy $1 "/allusers"
  ${Else}
    StrCpy $1 "/currentuser"
  ${EndIf}

  DetailPrint "Suppression d'une ancienne installation Elysia..."
  StrCpy $2 "$PLUGINSDIR\legacy-uninstaller.exe"
  ClearErrors
  CopyFiles /SILENT "$0" "$2"

  ClearErrors
  ExecWait '"$2" /S /KEEP_APP_DATA --updated $1 _?=$legacyInstallLocation' $3
  ${If} ${Errors}
    ClearErrors
    ExecWait '"$0" /S /KEEP_APP_DATA --updated $1 _?=$legacyInstallLocation' $3
  ${EndIf}

legacyUninstallDone:
  Pop $3
  Pop $2
  Pop $1
  Pop $0
FunctionEnd

Function RemoveLegacyInstallsForCurrentRoot
  Push $0
  Push $1
  Push $2
  Push $3
  Push $4
  Push $5

legacyRemoveRestart:
  StrCpy $0 0

legacyRemoveLoop:
  ClearErrors
  ${If} $legacyRootKey == "HKEY_CURRENT_USER"
    EnumRegKey $1 HKCU "${LEGACY_UNINSTALL_PARENT}" $0
  ${Else}
    EnumRegKey $1 HKLM "${LEGACY_UNINSTALL_PARENT}" $0
  ${EndIf}
  ${If} ${Errors}
    Goto legacyRemoveDone
  ${EndIf}

  ${If} $legacyRootKey == "HKEY_CURRENT_USER"
    ReadRegStr $2 HKEY_CURRENT_USER "${LEGACY_UNINSTALL_PARENT}\$1" "DisplayName"
    ReadRegStr $3 HKEY_CURRENT_USER "${LEGACY_UNINSTALL_PARENT}\$1" "InstallLocation"
    ReadRegStr $4 HKEY_CURRENT_USER "${LEGACY_UNINSTALL_PARENT}\$1" "UninstallString"
  ${Else}
    ReadRegStr $2 HKEY_LOCAL_MACHINE "${LEGACY_UNINSTALL_PARENT}\$1" "DisplayName"
    ReadRegStr $3 HKEY_LOCAL_MACHINE "${LEGACY_UNINSTALL_PARENT}\$1" "InstallLocation"
    ReadRegStr $4 HKEY_LOCAL_MACHINE "${LEGACY_UNINSTALL_PARENT}\$1" "UninstallString"
  ${EndIf}

  ${If} $4 != ""
    Push "$2|$3|$4"
    Call LegacyEntryMatches
    Pop $5
    ${If} $5 == "1"
      StrCpy $legacyInstallLocation "$3"
      StrCpy $legacyUninstallString "$4"
      StrCpy $legacyRegEntryKey "$1"
      Call LegacyResolveInstallLocation
      ${If} $legacyInstallLocation == $INSTDIR
        IntOp $0 $0 + 1
        Goto legacyRemoveLoop
      ${EndIf}

      Call UninstallLegacyEntry
      Sleep 500

      Call LegacyEntryWasRemoved
      Pop $5
      ${If} $5 == "1"
        Goto legacyRemoveRestart
      ${EndIf}
    ${EndIf}
  ${EndIf}

  IntOp $0 $0 + 1
  Goto legacyRemoveLoop

legacyRemoveDone:
  Pop $5
  Pop $4
  Pop $3
  Pop $2
  Pop $1
  Pop $0
FunctionEnd

Function LegacyCleanupPageCreate
  ${If} ${Silent}
    Abort
  ${EndIf}

  nsDialogs::Create 1018
  Pop $0
  ${If} $0 == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0 0u 100% 18u "Option de nettoyage des anciennes installations Elysia."
  Pop $0
  ${NSD_CreateLabel} 0 18u 100% 30u "Le setup peut supprimer les autres installations Elysia enregistrees avant de terminer cette installation."
  Pop $0
  ${NSD_CreateCheckbox} 0 56u 100% 12u "Installer et supprimer les autres versions Elysia"
  Pop $legacyCleanupCheckbox
  ${NSD_Check} $legacyCleanupCheckbox
  ${NSD_CreateLabel} 0 74u 100% 24u "Recommande si tu as deja installe plusieurs builds ou plusieurs chemins differents."
  Pop $0

  nsDialogs::Show
FunctionEnd

Function LegacyCleanupPageLeave
  StrCpy $legacyCleanupRequested "0"
  ${NSD_GetState} $legacyCleanupCheckbox $0
  ${If} $0 != ${BST_CHECKED}
    Return
  ${EndIf}

  StrCpy $legacyCleanupRequested "1"
FunctionEnd

Function RunLegacyCleanupIfRequested
  ${If} $legacyCleanupRequested != "1"
    Return
  ${EndIf}

  DetailPrint "Recherche des anciennes installations Elysia..."
  StrCpy $legacyRootKey "HKEY_CURRENT_USER"
  Call RemoveLegacyInstallsForCurrentRoot

  StrCpy $legacyRootKey "HKEY_LOCAL_MACHINE"
  Call RemoveLegacyInstallsForCurrentRoot
FunctionEnd

!macro customPageAfterChangeDir
  Page custom LegacyCleanupPageCreate LegacyCleanupPageLeave
!macroend

!macro customInstall
  Call RunLegacyCleanupIfRequested
!macroend
!endif
