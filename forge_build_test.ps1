# Forge 1.20.1 构建测试脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RoadWeaver Forge 1.20.1 构建测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 清理旧的构建文件
Write-Host "[1/4] 清理旧的构建文件..." -ForegroundColor Yellow
./gradlew :forge:clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 清理失败!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ 清理完成" -ForegroundColor Green
Write-Host ""

# 2. 编译Common模块
Write-Host "[2/4] 编译Common模块..." -ForegroundColor Yellow
./gradlew :common:build
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Common模块编译失败!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Common模块编译成功" -ForegroundColor Green
Write-Host ""

# 3. 编译Forge模块
Write-Host "[3/4] 编译Forge模块..." -ForegroundColor Yellow
./gradlew :forge:build
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Forge模块编译失败!" -ForegroundColor Red
    Write-Host ""
    Write-Host "请检查以下内容:" -ForegroundColor Yellow
    Write-Host "  1. Java版本是否为17或更高" -ForegroundColor White
    Write-Host "  2. 网络连接是否正常（需要下载依赖）" -ForegroundColor White
    Write-Host "  3. 查看上方错误信息定位问题" -ForegroundColor White
    exit 1
}
Write-Host "✅ Forge模块编译成功" -ForegroundColor Green
Write-Host ""

# 4. 检查输出文件
Write-Host "[4/4] 检查输出文件..." -ForegroundColor Yellow
$jarPath = "forge\build\libs\roadweaver-forge-1.0.0.jar"
if (Test-Path $jarPath) {
    $jarSize = (Get-Item $jarPath).Length / 1MB
    Write-Host "✅ 模组JAR已生成: $jarPath" -ForegroundColor Green
    Write-Host "   文件大小: $([math]::Round($jarSize, 2)) MB" -ForegroundColor White
} else {
    Write-Host "⚠️  未找到输出JAR文件" -ForegroundColor Yellow
}
Write-Host ""

# 完成
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ✅ 构建测试完成!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步操作:" -ForegroundColor Yellow
Write-Host "  • 运行客户端测试: ./gradlew :forge:runClient" -ForegroundColor White
Write-Host "  • 生成数据文件: ./gradlew :forge:runData" -ForegroundColor White
Write-Host "  • 查看构建产物: forge\build\libs\" -ForegroundColor White
Write-Host ""
