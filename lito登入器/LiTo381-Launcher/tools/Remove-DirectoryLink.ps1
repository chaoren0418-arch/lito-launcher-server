# Windows PowerShell 5.1 Remove-Item can prompt/traverse directory junctions.
# RemoveDirectoryW removes only the directory link entry, never its contents.
if(-not ('LiToCleanup.NativeLink' -as [type])){
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
namespace LiToCleanup {
    public static class NativeLink {
        [DllImport("kernel32.dll", CharSet=CharSet.Unicode, ExactSpelling=true, SetLastError=true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        public static extern bool RemoveDirectoryW(string path);
        [DllImport("kernel32.dll", CharSet=CharSet.Unicode, ExactSpelling=true, SetLastError=true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        public static extern bool DeleteFileW(string path);
    }
}
'@
}
function Remove-ProjectReparseLink {
    param([Parameter(Mandatory=$true)][string]$LiteralPath,[Parameter(Mandatory=$true)][string]$Scope)
    $root=[IO.Path]::GetFullPath($Scope).TrimEnd('\')
    $link=[IO.Path]::GetFullPath($LiteralPath)
    if(-not $link.StartsWith($root+'\',[StringComparison]::OrdinalIgnoreCase)){throw "Link outside cleanup scope: $link"}
    $parent=[IO.Path]::GetDirectoryName($link)
    while($parent -and $parent.Length -ge $root.Length){
        if((Get-Item -LiteralPath $parent -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw "Reparse ancestor refused: $parent"}
        $parent=[IO.Path]::GetDirectoryName($parent)
    }
    $item=Get-Item -LiteralPath $link -Force
    if(-not($item.Attributes -band [IO.FileAttributes]::ReparsePoint)){throw "Expected a link, refusing ordinary item: $link"}
    if($item.Attributes -band [IO.FileAttributes]::Directory){$ok=[LiToCleanup.NativeLink]::RemoveDirectoryW($link)}
    else{$ok=[LiToCleanup.NativeLink]::DeleteFileW($link)}
    if(-not $ok){throw (New-Object ComponentModel.Win32Exception([Runtime.InteropServices.Marshal]::GetLastWin32Error()))}
}
