const fs = require('fs');
const path = require('path');

const pngPath = "C:\\Users\\Zorat\\.gemini\\antigravity\\brain\\b4c0b73b-17c5-4a4a-8f1a-0d1de8437216\\elysia_launcher_icon_1776801453845.png";
const outPath = path.join(__dirname, "build", "icon.ico");

// Create build directory if it doesn't exist
if (!fs.existsSync(path.join(__dirname, "build"))) {
    fs.mkdirSync(path.join(__dirname, "build"));
}

const pngData = fs.readFileSync(pngPath);

// Create ICO header (22 bytes for 1 image)
const icoHeader = Buffer.alloc(22);

// Reserved (0)
icoHeader.writeUInt16LE(0, 0);
// Type (1 for ICO)
icoHeader.writeUInt16LE(1, 2);
// Number of images (1)
icoHeader.writeUInt16LE(1, 4);

// Image properties
icoHeader.writeUInt8(0, 6); // Width (0 means 256 or larger)
icoHeader.writeUInt8(0, 7); // Height (0 means 256 or larger)
icoHeader.writeUInt8(0, 8); // Color count
icoHeader.writeUInt8(0, 9); // Reserved
icoHeader.writeUInt16LE(1, 10); // Color planes
icoHeader.writeUInt16LE(32, 12); // Bit count (32 bits per pixel)

// Size of PNG data
icoHeader.writeUInt32LE(pngData.length, 14);
// Offset to PNG data (header is 22 bytes long)
icoHeader.writeUInt32LE(22, 18);

// Combine header and PNG data
const icoData = Buffer.concat([icoHeader, pngData]);

fs.writeFileSync(outPath, icoData);
console.log('Icon successfully saved to: ' + outPath);
