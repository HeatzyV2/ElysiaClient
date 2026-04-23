function formatBytes(bytes) {
    if (!Number.isFinite(bytes) || bytes <= 0) {
        return '0 B';
    }

    const units = ['B', 'KB', 'MB', 'GB'];
    let value = bytes;
    let unitIndex = 0;

    while (value >= 1024 && unitIndex < units.length - 1) {
        value /= 1024;
        unitIndex += 1;
    }

    return `${value.toFixed(value >= 100 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

export function initConsole() {
    const output = document.getElementById('console-output');
    const clearButton = document.getElementById('btn-clear-console');

    if (!output) {
        return;
    }

    const appendLine = (text, className = '') => {
        const line = document.createElement('div');
        line.className = `console-line${className ? ` ${className}` : ''}`;
        line.textContent = text;
        output.appendChild(line);
        output.scrollTop = output.scrollHeight;

        while (output.children.length > 500) {
            output.removeChild(output.firstChild);
        }
    };

    clearButton?.addEventListener('click', () => {
        output.innerHTML = '<div class="console-welcome"><span class="console-prefix">[Elysia]</span> Console effacee.</div>';
    });

    window.electronAPI.onProgress((data) => {
        appendLine(
            `[Download] ${data.type || 'Fichier'} ${data.percent}% (${formatBytes(data.progress)}/${formatBytes(data.size)})`,
            'console-line-muted'
        );
    });

    window.electronAPI.onData((line) => {
        appendLine(String(line ?? ''));
    });

    window.electronAPI.onPatch((patch) => {
        appendLine(`[Patch] ${patch}`, 'console-line-muted');
    });

    window.electronAPI.onError((error) => {
        appendLine(`[Erreur] ${error}`, 'console-line-error');
    });

    window.electronAPI.onClose((code) => {
        appendLine(`[Elysia] Jeu ferme (code: ${code})`, code === 0 ? 'console-line-muted' : 'console-line-error');
    });
}
