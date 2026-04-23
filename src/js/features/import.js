import { notify } from '../core/utils.js';
import { state } from '../core/state.js';

export function initImport() {
    const btnOpenImport = document.getElementById('btn-open-import');
    const modalImport = document.getElementById('modal-import-assistant');
    const step1 = document.getElementById('import-step-1');
    const step2 = document.getElementById('import-step-2');
    const step3 = document.getElementById('import-step-3');
    const footer2 = document.getElementById('import-footer-2');
    const launchersList = document.getElementById('import-launchers-list');
    const launchersLoading = document.getElementById('import-launchers-loading');
    const btnBack = document.getElementById('btn-import-back');
    const instancesContainer = document.getElementById('import-instances-container');
    const instanceSelect = document.getElementById('import-instance-select');
    const btnConfirm = document.getElementById('btn-import-confirm');

    const progressBar = document.getElementById('import-progress-bar');
    const statusMsg = document.getElementById('import-status-msg');
    const statusTitle = document.getElementById('import-status-title');
    const spinner = document.getElementById('import-spinner');
    const successIcon = document.getElementById('import-success-icon');
    const finishedActions = document.getElementById('import-finished-actions');
    const btnOpenFolder = document.getElementById('btn-import-open-folder');
    const btnCloseModal = document.getElementById('btn-import-close');

    let selectedLauncher = null;

    // Listen for progress
    window.electronAPI.onImportProgress((data) => {
        if (progressBar) progressBar.style.width = `${data.percent}%`;
        if (statusMsg) statusMsg.textContent = data.message;
    });

    btnOpenImport?.addEventListener('click', async () => {
        modalImport.classList.remove('hidden');
        step1.classList.remove('hidden');
        step2.classList.add('hidden');
        step3.classList.add('hidden');
        footer2.style.display = 'none';
        
        launchersList.innerHTML = '';
        launchersLoading.style.display = 'block';

        const launchers = await window.electronAPI.importScan();
        launchersLoading.style.display = 'none';

        if (launchers.length === 0) {
            launchersList.innerHTML = '<p class="form-hint" style="text-align:center;">Aucun launcher compatible trouvé sur votre PC.</p>';
            return;
        }

        launchers.forEach(l => {
            const btn = document.createElement('button');
            btn.className = 'btn btn-secondary';
            btn.style.cssText = 'width: 100%; justify-content: flex-start; padding: 12px; gap: 12px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05); transition: 0.2s; margin-bottom: 4px;';
            
            let iconColor = 'var(--accent)';
            if (l.id === 'lunar') iconColor = '#5eead4';
            if (l.id === 'feather') iconColor = '#60a5fa';
            if (l.id === 'curseforge') iconColor = '#fb923c';
            if (l.id === 'prism') iconColor = '#c084fc';

            btn.innerHTML = `
                <div style="width:40px;height:40px;background:${iconColor}20;border:1px solid ${iconColor}40;border-radius:10px;display:flex;align-items:center;justify-content:center;color:${iconColor};flex-shrink:0;">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:20px;height:20px;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                </div>
                <div style="text-align: left; flex: 1; overflow: hidden;">
                    <div style="font-weight: 700; font-size: 14px; color: #fff;">${l.name}</div>
                    <div style="font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${l.path}</div>
                </div>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;opacity:0.3;"><polyline points="9 18 15 12 9 6"></polyline></svg>
            `;
            btn.onclick = () => selectLauncher(l);
            launchersList.appendChild(btn);
        });
    });

    btnBack?.addEventListener('click', () => {
        step2.classList.add('hidden');
        footer2.style.display = 'none';
        step1.classList.remove('hidden');
    });

    async function selectLauncher(launcher) {
        selectedLauncher = launcher;
        step1.classList.add('hidden');
        step2.classList.remove('hidden');
        footer2.style.display = 'block';

        instancesContainer.style.display = 'none';
        instanceSelect.innerHTML = '';

        if (['curseforge', 'prism', 'modrinth'].includes(launcher.id)) {
            const instances = await window.electronAPI.importGetInstances(launcher.path);
            if (instances && instances.length > 0) {
                instancesContainer.style.display = 'block';
                instances.forEach(inst => {
                    const opt = document.createElement('option');
                    opt.value = inst.path;
                    opt.textContent = inst.name;
                    instanceSelect.appendChild(opt);
                });
            } else {
                const opt = document.createElement('option');
                opt.value = launcher.path;
                opt.textContent = 'Dossier racine par défaut';
                instanceSelect.appendChild(opt);
            }
        }
    }

    btnConfirm?.addEventListener('click', async () => {
        const items = [];
        if (document.getElementById('import-chk-options').checked) items.push('options');
        if (document.getElementById('import-chk-resourcepacks').checked) items.push('resourcepacks');
        if (document.getElementById('import-chk-mods').checked) items.push('mods');
        if (document.getElementById('import-chk-config').checked) items.push('config');

        if (items.length === 0) {
            notify('Veuillez sélectionner au moins un élément à importer.', 'error');
            return;
        }

        let sourcePath = selectedLauncher.path;
        if (['curseforge', 'prism', 'modrinth'].includes(selectedLauncher.id)) {
            if (instanceSelect.value) sourcePath = instanceSelect.value;
        }

        // Switch to progress step
        step2.classList.add('hidden');
        footer2.style.display = 'none';
        step3.classList.remove('hidden');
        
        // Reset progress state
        progressBar.style.width = '0%';
        statusTitle.textContent = 'Importation en cours...';
        statusMsg.textContent = 'Préparation...';
        spinner.classList.remove('hidden');
        successIcon.classList.add('hidden');
        finishedActions.classList.add('hidden');

        const result = await window.electronAPI.importExecute({ sourcePath, items });

        if (result.success) {
            statusTitle.textContent = 'Importation terminée !';
            statusMsg.textContent = 'Tous les fichiers ont été copiés avec succès.';
            spinner.classList.add('hidden');
            successIcon.classList.remove('hidden');
            finishedActions.classList.remove('hidden');
            
            if (window.refreshModsList) window.refreshModsList();
            notify('Importation terminée ! 🎉', 'success');
        } else {
            statusTitle.textContent = 'Erreur lors de l\'importation';
            statusMsg.textContent = result.error;
            spinner.classList.add('hidden');
            notify('Erreur lors de l\'importation : ' + result.error, 'error');
        }
    });

    btnOpenFolder?.addEventListener('click', () => {
        window.electronAPI.openFolder();
    });

    btnCloseModal?.addEventListener('click', () => {
        modalImport.classList.add('hidden');
    });
}
