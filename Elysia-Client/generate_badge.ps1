Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap(10, 10)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::Transparent)

$mainColor = [System.Drawing.Color]::FromArgb(255, 168, 85, 247)
$shadowColor = [System.Drawing.Color]::FromArgb(255, 60, 10, 110)

$brush = New-Object System.Drawing.SolidBrush($mainColor)
$shadow = New-Object System.Drawing.SolidBrush($shadowColor)

# Shadow
$g.FillRectangle($shadow, 2, 2, 2, 8)
$g.FillRectangle($shadow, 4, 2, 5, 2)
$g.FillRectangle($shadow, 4, 5, 3, 2)
$g.FillRectangle($shadow, 4, 8, 5, 2)

# Main E
$g.FillRectangle($brush, 1, 1, 2, 8)
$g.FillRectangle($brush, 3, 1, 5, 2)
$g.FillRectangle($brush, 3, 4, 3, 2)
$g.FillRectangle($brush, 3, 7, 5, 2)

$bmp.Save("c:\Users\Zorat\Downloads\Elysia-Launcher\Elysia-Client\src\main\resources\assets\elysia-client\textures\font\elysia_badge.png", [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$bmp.Dispose()
$brush.Dispose()
$shadow.Dispose()
