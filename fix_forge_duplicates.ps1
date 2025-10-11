# 修复 Forge 版本的重复文件问题

Write-Host "开始清理 Forge 模块中的重复文件..." -ForegroundColor Green

# 删除重复的 worldgen 文件
$worldgenPath = "forge\src\main\resources\data\roadweaver\worldgen"
if (Test-Path $worldgenPath) {
    Write-Host "删除重复的 worldgen 目录: $worldgenPath" -ForegroundColor Yellow
    Remove-Item -Recurse -Force $worldgenPath
    Write-Host "✓ 已删除" -ForegroundColor Green
} else {
    Write-Host "worldgen 目录不存在，跳过" -ForegroundColor Gray
}

Write-Host "`n修复完成！" -ForegroundColor Green
Write-Host "现在可以重新运行游戏测试。" -ForegroundColor Cyan
