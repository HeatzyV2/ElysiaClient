const fs = require('fs');
const path = require('path');

const sourcePath = "C:\\Users\\Zorat\\.gemini\\antigravity\\brain\\b4c0b73b-17c5-4a4a-8f1a-0d1de8437216\\elysia_launcher_icon_1776801453845.png";
const destPath = path.join(__dirname, "build", "icon.png");

// Copy the file to the build directory so the user can see it
fs.copyFileSync(sourcePath, destPath);
console.log("Image copiée vers: " + destPath);
