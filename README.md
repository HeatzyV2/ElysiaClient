# Elysia Launcher

Launcher Electron Windows pour Elysia, avec client Minecraft intégré dans `Elysia-Client`.

## Développement

```powershell
npm ci
cd Elysia-Client
.\gradlew.bat build
cd ..
npm start
```

## Release et auto-updater

L'auto-updater repose sur `electron-updater` et les GitHub Releases de ce dépôt.

1. Mettre à jour la version dans `package.json`
2. Commit les changements
3. Créer un tag `vX.Y.Z`
4. Push la branche puis le tag

Le workflow GitHub Actions publie automatiquement les artefacts Windows nécessaires :

- `latest.yml`
- `Elysia Launcher Setup ... .exe`
- `Elysia Launcher Setup ... .exe.blockmap`

Le launcher vérifie ensuite les mises à jour depuis l'onglet dédié, sans téléchargement automatique.
