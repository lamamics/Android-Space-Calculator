# CLAUDE.md — Space Calculator

Guide de reprise pour Claude Code. Lis-le en entier avant d'agir : il contient
la toolchain exacte (sinon le build échoue) et l'état réel du projet.

## Le projet en une phrase
App Android (Kotlin + Jetpack Compose) qui visualise l'espace de stockage en
**treemap squarifié imbriqué** façon SpaceMonger/WinDirStat, avec drill-down,
fiche de détail (taille, app propriétaire, aperçu miniature) et représentation
de l'espace libre. Accès étendu via **Shizuku** (pas de root). Usage **perso /
sideload** (pas de publication Play Store visée).

## ⚠️ Build : toolchain EXACTE (sinon échec)
Le projet **compile et tourne** en ligne de commande. Tout est sur la machine.

```powershell
# JDK 21 = JBR d'Android Studio. Le `java` du PATH est un JDK 8 → INUTILISABLE.
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd d:\WS_DEV\AndroidSpaceCalculator
.\gradlew.bat assembleDebug --console=plain   # ajoute -q pour réduire la sortie
```
- **SDK Android** : `C:\Users\mathi\AppData\Local\Android\Sdk` (déclaré dans `local.properties`, non versionné).
- **compileSdk / targetSdk = 35** (obligatoire : androidx.core 1.15 refuse 34). **minSdk 30**.
  La plateforme `android-35` a été installée via
  `...\Sdk\cmdline-tools\latest\bin\sdkmanager.bat "platforms;android-35"`.
- **Gradle 8.11.1** (wrapper généré, présent aussi en cache). AGP 8.7.3, Kotlin 2.1.0.
- Le 1er build peut prendre ~3 min ; APK : `app\build\outputs\apk\debug\app-debug.apk` (~24 Mo).

## Déploiement sur l'appareil (Samsung Galaxy S10, SM-G973F)
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n "com.lamamics.spacecalculator/.MainActivity"
```
- **Écran 1080×2280, densité 420 dpi (facteur ~2,6)** — important : tout dessin de
  texte/tuile doit raisonner en **dp** (via `density`), pas en px bruts (cf. bug
  corrigé sur les bandes de titre).
- **Captures d'écran** (la redirection `>` de PowerShell corrompt le PNG, passer par le device) :
  ```powershell
  & $adb shell screencap -p /sdcard/s.png; & $adb pull /sdcard/s.png .\s.png; & $adb shell rm /sdcard/s.png
  ```
- **Piloter l'UI** sans toucher au téléphone : `adb shell uiautomator dump` puis lire les
  `bounds` (Compose expose la sémantique) et `adb shell input tap X Y`.
- Permissions accordables via adb (déjà accordées sur ce device) :
  ```powershell
  & $adb shell appops set com.lamamics.spacecalculator MANAGE_EXTERNAL_STORAGE allow
  & $adb shell appops set com.lamamics.spacecalculator android:get_usage_stats allow
  ```

## Shizuku (clé de l'accès à `Android/data` / `Android/obb`)
App séparée (`moe.shizuku.privileged.api`), à démarrer après chaque reboot du tél.
Démarrage via adb (le téléphone doit avoir ouvert Shizuku une fois → `start.sh` présent) :
```powershell
& $adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
```
Puis dans l'app : « Rafraîchir l'état » → « Autoriser » → la popup Shizuku → Autoriser.
Sans Shizuku, le scan retombe sur un walk in-process (toggle dans l'écran de config) :
fonctionne mais `Android/data`/`obb` apparaissent « non lisible ». `/data/data` et
`/system` ne sont JAMAIS lisibles sans root (limite assumée, affichée honnêtement).

## Architecture (chemins sous `app/src/main/java/com/lamamics/spacecalculator/`)
- `MainActivity.kt` — point d'entrée, navigation par état, intents permissions, back.
- `ui/MainViewModel.kt` — état (scan, navStack drill-down, détail), seuil adaptatif, injection espace libre.
- `ui/screens/` — `SetupScreen` (Shizuku + permissions + volumes), `ScanningScreen`, `TreemapScreen` (barre retour/nouveau scan + breadcrumb).
- `ui/components/TreemapView.kt` — Canvas : dessin des tuiles + labels (dp-aware), hit-test (tap=détail, double-tap=drill).
- `ui/components/DetailBottomSheet.kt` — fiche modale + aperçu miniature image/vidéo.
- `treemap/Squarified.kt` — pavage squarifié 1 niveau (Bruls et al.). `treemap/NestedTreemap.kt` — récursion imbriquée + bandes de titre (dp).
- `scan/FileScanner.kt` — walk récursif pur-JVM (tourne aussi dans le process Shizuku), élagage + plafond 400 enfants/dossier, détection package via chemin.
- `scan/ScanRepository.kt` — orchestre (Shizuku→JSON / in-process), `StorageStatsProvider` (tailles par app), `AppResolver` (libellés), `VolumeProvider`.
- `shizuku/ShizukuManager.kt` — binder/permission/binding. `shizuku/UserService.kt` + `aidl/IUserService.aidl` — service à privilèges shell.
- `model/Node.kt` — arbre `@Serializable` (champs : size, isReadable, ownerPackage, isFree, children, hiddenSize).

## État actuel — FAIT et testé sur device réel
Phases 0–6 + extras. Vérifié : treemap imbriqué, titres lisibles, Shizuku lisant
`Android/data` (ex. `deezer.android.app` sur la SD), aperçu miniature, espace libre.
Concepts importants déjà résolus :
- **Seuil d'élagage adaptatif** : `used/20000`, borné 64 Ko–32 Mo (sinon photos ~2-4 Mo masquées).
- **Bandes de titre en dp** : hauteur ≤12% de la tuile, cap 22 dp, masquée sous le seuil de lisibilité (corrige la déformation des proportions). Police ∝ bande.
- **Bandes de dossiers = nom SEUL** (pas la taille) : éviter la confusion d'addition hiérarchique (un dossier inclut déjà ses enfants). Taille dispo dans la fiche + breadcrumb.
- **Espace libre** : tuile synthétique (`Node.isFree`) à la racine, remplie d'une **trame hachurée** (`drawHatch`) pour ne pas la confondre avec les gris « petits fichiers ».
- **Bouton « Ouvrir »** (fiche détail) : `util/OpenFile.kt` + `FileProvider` (`res/xml/file_paths.xml`, autorité `${applicationId}.fileprovider`) → ACTION_VIEW via chooser. Ne marche que sur fichiers lisibles par le process app (pas Android/data).
- **Vue « par application »** : `ui/screens/AppListScreen.kt`, ouverte depuis SetupScreen (bouton) → `MainViewModel.openAppList()`. Liste triée par taille, barre proportionnelle, répartition App/Données/Cache (StorageStatsManager). En-tête récap (totaux App/Données/Cache) qui note que ces chiffres incluent `/data/data` — l'angle mort du treemap.
- **Déploiement systématique des dossiers** : un dossier dessine ses enfants même sans bande de titre (fini les blocs jaunes vides). Seuil titre : ≤15% hauteur, cap 22dp, plancher 10dp.
- **Couleur app data/cache** : cyan (`APP_FILE`/`APP_FOLDER`) pour tout ce qui est sous `Android/data`|`obb` (regex `APP_DATA_REGEX`), distinct des médias.
- **Légende couleurs** : `LegendBar` dans TreemapScreen, repliable via l'icône Info de la barre. Source des couleurs+libellés : `TreemapColors.legend`.

### « Autre » du téléphone
Pas un dossier : c'est une catégorie Samsung. Son contenu apparaît sous les vrais
dossiers (`Android/data`, `Android/obb`, miniatures, divers). Une partie vit dans
`/data/data` (invisible sans root).

## Reste à faire / idées (Phase 7+)
- Streaming de progression depuis le user service Shizuku (callback AIDL ; actuellement start/finish seulement).
- Sérialisation binaire au lieu de JSON pour les très gros arbres (interne 110 Go).
- Miniature aussi pour les dossiers (plus gros média).
- Persistance / comparaison de scans, export.
- Point d'entrée « par application » aussi depuis l'écran treemap (actuellement seulement depuis SetupScreen).
- À VÉRIFIER sur device (tél était déchargé au moment du dev) : rendu de la légende + en-tête récap app-list + couleur cyan app-data sur un vrai scan.

## Conventions
- L'utilisateur (Mathias) travaille en **français**, veut avancer **phase par phase**, et
  attend que je **compile + installe + vérifie par capture** moi-même (j'ai adb).
- Builds/installs sans demander confirmation tant que ça reste dans le dossier de travail.
- Nettoyer les artefacts de test (captures `.png`, `ui.xml`) du dossier après usage.
