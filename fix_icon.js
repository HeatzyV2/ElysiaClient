const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

const pngPath = "C:\\Users\\Zorat\\.gemini\\antigravity\\brain\\b4c0b73b-17c5-4a4a-8f1a-0d1de8437216\\elysia_launcher_icon_1776801453845.png";
const outDir = path.join(__dirname, 'build');
const outPath = path.join(outDir, 'icon.ico');

if (!fs.existsSync(outDir)) fs.mkdirSync(outDir);

console.log('Installation temporaire de png-to-ico...');
execSync('npm install --no-save png-to-ico', { stdio: 'inherit' });

console.log('Conversion en cours avec png-to-ico (CLI)...');
// En utilisant l'exécutable binaire avec execSync, on évite les soucis de modules CommonJS/ESM.
// Sous Windows, execSync utilise cmd.exe qui gère très bien la redirection binaire contrairement à PowerShell.
try {
    execSync(`npx png-to-ico "${pngPath}" > "${outPath}"`);
    console.log('Conversion terminée avec succès !');
    console.log('Ton icon.ico est maintenant 100% compatible avec electron-builder !');
} catch (e) {
    console.error('Erreur lors de la conversion:', e.message);
}
