# Space Calculator

Visualiseur d'espace de stockage Android, façon *SpaceMonger / WinDirStat* :
un **treemap squarifié imbriqué** où chaque carré représente un gros
dossier/fichier, avec exploration par drill-down et une fiche de détail (taille,
application propriétaire, aperçu miniature) par élément.

> 📌 **Reprise par Claude Code : lire `CLAUDE.md`** (toolchain exacte, Shizuku,
> commandes adb, état détaillé). Ce README est un résumé.

## État d'avancement

| Phase | Contenu | Statut |
|------|---------|:--:|
| 0 | Squelette Gradle, manifeste, thème, icône | ✅ |
| 1 | Intégration Shizuku (AIDL, user service, manager) | ✅ |
| 2 | Sélection du volume + permissions | ✅ |
| 3 | Moteur de scan récursif + sérialisation | ✅ |
| 4 | Attribution par application (`StorageStatsManager`) | ✅ |
| 5 | Treemap squarifié (algorithme + rendu Canvas) | ✅ |
| 6 | Drill-down + modale de détail | ✅ |
| + | Nesting récursif, seuil adaptatif, titres lisibles (dp) | ✅ |
| + | Aperçu miniature image/vidéo, espace libre, barre retour | ✅ |
| 7 | Vue par app, bouton « Ouvrir », légende, perf, export | ⏳ à venir |

**Testé sur device réel** (Samsung S10 / SM-G973F) : treemap imbriqué, Shizuku
lisant `Android/data`, miniatures, espace libre.

## Architecture

```
ui/            Compose : MainActivity, MainViewModel, écrans, TreemapView, DetailBottomSheet
treemap/       Squarified.kt — algorithme de pavage (Bruls et al. 2000)
scan/          FileScanner (walk), ScanRepository, StorageStatsProvider, AppResolver, VolumeProvider
shizuku/       ShizukuManager + UserService (exécuté en process shell)
model/         Node, AppInfo, StorageVolumeInfo, ScanProgress
aidl/          IUserService.aidl
```

**Idée clé** : le parcours de fichiers tourne dans un process lancé par
**Shizuku** avec les privilèges du shell ADB (UID 2000). Cela permet de lire
`Android/data/` et `Android/obb/` — l'essentiel de la catégorie « Autre » —
que le scoped storage interdit à une app normale depuis Android 11.
L'arbre résultant est sérialisé en JSON dans le cache externe (lisible par les
deux process) puis désérialisé côté app.

### Limites assumées (sans root)
- `/data/data/<pkg>` (données privées internes) et `/system` restent illisibles.
  Ces zones sont affichées honnêtement comme « non lisible ».
- Sans Shizuku, l'app retombe sur un scan in-process limité au stockage
  accessible (toggle dans l'écran de configuration).

## Permissions
- **Accès à tous les fichiers** (`MANAGE_EXTERNAL_STORAGE`) — lecture du stockage partagé.
- **Accès aux données d'utilisation** (`PACKAGE_USAGE_STATS`) — attribution par app.
- **Shizuku** — accès étendu aux dossiers bloqués. À installer et démarrer séparément.

## Build (chez toi, dans Android Studio)
1. Ouvrir le dossier dans **Android Studio** (Ladybug ou +). Il génère
   `local.properties` (chemin du SDK) et le **gradle wrapper jar** au 1er sync.
   - Sinon en ligne de commande : `gradle wrapper` puis `./gradlew assembleDebug`.
2. SDK requis : compileSdk 35, minSdk 30. JDK 17.
3. Brancher un appareil (Android 11+), `Run`.
4. Au 1er lancement : accorder *tous les fichiers*, *données d'utilisation*,
   installer **Shizuku** (Play Store / GitHub) et le démarrer via *Wireless
   debugging* (ADB), puis « Autoriser » dans l'app.

## À vérifier en premier (smoke test)
- L'écran de configuration affiche bien les 3 cartes d'état + la liste des volumes.
- Un scan du stockage interne produit un treemap ; double-tap = drill-down,
  simple tap = fiche de détail.
- Sur un dossier `Android/data/<pkg>`, la fiche affiche le nom de l'app.

## Pistes Phase 7
- Rendu **imbriqué** (nesting récursif) comme la capture SpaceMonger.
- Streaming de progression depuis le user service (callback AIDL).
- Sérialisation binaire (au lieu de JSON) pour les très gros arbres.
- Vue « par application » dédiée + légende des couleurs.
- Cache des scans, comparaison avant/après.
