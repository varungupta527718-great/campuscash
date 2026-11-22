$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
Set-Location -LiteralPath $root
$out = Join-Path $root 'out'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$javaSourceFolder = Join-Path $root 'src\main\java\com\studentexpensetracker'
$javaFiles = Get-ChildItem -Path $javaSourceFolder -Filter '*.java' -File | ForEach-Object FullName
javac -encoding UTF-8 -d $out $javaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -cp $out com.studentexpensetracker.ApiServer
