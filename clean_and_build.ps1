# 清理并重新构建脚本

Write-Host "停止所有 Gradle Daemon..." -ForegroundColor Yellow
.\gradlew.bat --stop

Write-Host "删除本地 Gradle 缓存..." -ForegroundColor Yellow
if (Test-Path ".gradle") {
    Remove-Item -Recurse -Force .gradle
}

Write-Host "删除构建输出..." -ForegroundColor Yellow
if (Test-Path "build") {
    Remove-Item -Recurse -Force build
}
if (Test-Path "common\build") {
    Remove-Item -Recurse -Force common\build
}
if (Test-Path "fabric\build") {
    Remove-Item -Recurse -Force fabric\build
}
if (Test-Path "neoforge\build") {
    Remove-Item -Recurse -Force neoforge\build
}

Write-Host "设置 Java 17..." -ForegroundColor Yellow
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-17"

Write-Host "开始构建..." -ForegroundColor Green
.\gradlew.bat clean build --refresh-dependencies --no-daemon

Write-Host "完成！" -ForegroundColor Green
