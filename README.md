<div align="center">

  <img src="composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark.png" alt="Nuvio" width="300" />
  <br />
  <br />

  [![Contributors][contributors-shield]][contributors-url]
  [![Forks][forks-shield]][forks-url]
  [![Stargazers][stars-shield]][stars-url]
  [![Issues][issues-shield]][issues-url]
  [![License][license-shield]][license-url]

  <p>
    A desktop media app for Windows, macOS, and Linux.
    <br />
    Browse, organize, and play media from sources you add.
  </p>

</div>

## ⚠️ Alpha Software - Slow Development - Testers Only

Nuvio Desktop is currently in alpha and is intended only for testers. It is under development and is not suitable for daily use.

Expect breaking changes with every update. Features, settings, stored data, and compatibility may change or stop working without notice. Do not rely on this build as your primary media app, and report any issues you encounter during testing.

## About

Nuvio Desktop is a media client for browsing metadata, managing collections and watch progress, downloading media, and playing streams from user-installed extensions or user-provided sources.

## Installation

Download the latest desktop build from [GitHub Releases](https://github.com/NuvioMedia/NuvioDesktop/releases/latest).

Release packages are provided for supported desktop platforms:

- Windows: MSI installer
- macOS: DMG installer
- Linux: DEB, RPM, FLATPAK and AppImage available.

## Development

```bash
git clone https://github.com/NuvioMedia/NuvioDesktop.git
cd NuvioDesktop
```

Run from source:

```bash
./gradlew :composeApp:run
```

On Windows PowerShell:

```powershell
.\gradlew.bat :composeApp:run
```

Build a release package for the current host:

```bash
./gradlew :composeApp:packageReleaseDistributionForCurrentOS
```

Platform-specific packaging:

```bash
# Windows
./gradlew :composeApp:packageReleaseMsi --rerun-tasks

# macOS
./scripts/build-macos-release-dmgs.sh --package-only

# Linux
./gradlew :composeApp:packageReleaseDeb
```

## Project Structure

- `composeApp/` contains the app code.
- `composeApp/src/commonMain/` contains shared UI, features, repositories, and platform-agnostic logic.
- `composeApp/src/desktopMain/` contains desktop-specific integrations.
- `composeApp/Configuration/DesktopVersion.properties` contains the desktop release version and build code.

## Versioning

Desktop versions are set in `composeApp/Configuration/DesktopVersion.properties`.

```properties
VERSION_NAME=0.1.1-alpha
VERSION_CODE=1
```

Use the version helper when changing desktop release versions:

```bash
./scripts/set-version.sh --desktop 0.1.2-alpha --desktop-code 2
./scripts/set-version.sh --show
```

## Anime filler/recap support (this fork)

This fork (`feature/tenrai-filler-skip`) adds native anime episode metadata support on
Nuvio Desktop, powered by [Tenrai](https://api.tenrai.org/v1) (a Jikan v4-compatible API).

**Data provider:** Tenrai. Base URL: `https://api.tenrai.org/v1`. No API key is required.

**Anime detection:** a show/movie is only treated as a filler/recap candidate when its
TMDB metadata has BOTH the "Animation" genre AND a Japanese origin signal
(`original_language == "ja"` or origin country `JP`). See `AnimeDetector`.

**MAL resolution:** `TenraiAnimeIdResolver` searches Tenrai's own `/anime?q=` endpoint
(no hardcoded anime database) using the show's title, and scores candidates by
normalized title similarity plus year/media-type/episode-count tie-breakers. A match
below a confidence threshold is treated as unresolved rather than risking an incorrect
match (e.g. "Naruto" is never confused with "Naruto Shippuden" or "Boruto"). See
`AnimeIdResolver.kt`.

**Episode classification:** `TenraiEpisodeProvider` fetches all pages from
`/anime/{malId}/episodes`, classifying each episode as `NORMAL`, `FILLER`, `RECAP`, or
`UNKNOWN`. If an episode is ever reported as both filler and recap, `RECAP` wins.

**Caching:** `AnimeEpisodeClassificationRepository` caches MAL id resolution (14 days)
and episode classifications (30 days) to disk under the app's data directory, with an
in-memory hot cache and request de-duplication on top. Tenrai being offline never
prevents an episode list from loading or an episode from playing - a failed request
falls back to any cached data, and otherwise the app behaves exactly as it did before
this feature existed.

**Settings:** Settings -> Playback -> Anime exposes independent "Filler episodes" and
"Recap episodes" controls, each with `Show and label` (default) / `Automatically skip` /
`Hide from episode list`. Automatic skip only affects automatic next-episode/autoplay
selection (`AnimeEpisodeSkipResolver`) - manually selecting a specific filler/recap
episode from the episode list always plays it. Skipped episodes are never marked
watched and never reported to Trakt/Simkl.

**Known limitations:**
- Episode classification is only applied when a show is represented as a single season
  in Nuvio's metadata (the common case for long-running shonen like Naruto/Naruto
  Shippuden, where MAL episode N == Nuvio episode N). If a show has more than one real
  season, classifications are intentionally dropped rather than risk mislabeling
  episodes across a season boundary.
- A couple of secondary/compact episode-row entry points do not yet show filler/recap
  badges or apply the "hide" filter (only the main details-screen episode list does).

**Development-only MAL id override:** set the `NUVIO_TENRAI_DEBUG_MAL_ID` environment
variable to force the MAL id used for the current anime candidate, bypassing title
matching - useful for testing classification independently of resolution accuracy.
Unset by default; never set it for a release build. Useful ids for manual testing:
Naruto = `20`, Naruto Shippuden = `1735`, Cowboy Bebop = `1` (Naruto/Shippuden contain
filler episodes and are the best titles for testing filler-skip behavior).

**Manual test:** open Naruto or Naruto Shippuden, open its episode list, wait briefly
for badges to appear (FILLER/RECAP), then in Settings -> Playback -> Anime set filler
handling to "Automatically skip" and use Next Episode/autoplay near a run of filler
episodes to confirm it jumps to the next normal episode with a toast, while a manual
click on a filler episode still plays it directly.

## Legal & DMCA

Nuvio functions solely as a client-side interface for browsing metadata and playing media provided by user-installed extensions and/or user-provided sources. It is intended for content the user owns or is otherwise authorized to access.

Nuvio is not affiliated with any third-party extensions, catalogs, sources, or content providers. It does not host, store, or distribute any media content.

For comprehensive legal information, including our full disclaimer, third-party extension policy, and DMCA/Copyright information, please visit our [Legal & Disclaimer Page](https://nuvioapp.space/legal).

## Built With

- Kotlin Multiplatform
- Compose Multiplatform
- Kotlin
- Compose Desktop packaging
- Native desktop player integrations

## Star History

<a href="https://www.star-history.com/#NuvioMedia/NuvioDesktop&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=NuvioMedia/NuvioDesktop&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=NuvioMedia/NuvioDesktop&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=NuvioMedia/NuvioDesktop&type=date&legend=top-left" />
 </picture>
</a>

<!-- MARKDOWN LINKS & IMAGES -->
[contributors-shield]: https://img.shields.io/github/contributors/NuvioMedia/NuvioDesktop.svg?style=for-the-badge
[contributors-url]: https://github.com/NuvioMedia/NuvioDesktop/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/NuvioMedia/NuvioDesktop.svg?style=for-the-badge
[forks-url]: https://github.com/NuvioMedia/NuvioDesktop/network/members
[stars-shield]: https://img.shields.io/github/stars/NuvioMedia/NuvioDesktop.svg?style=for-the-badge
[stars-url]: https://github.com/NuvioMedia/NuvioDesktop/stargazers
[issues-shield]: https://img.shields.io/github/issues/NuvioMedia/NuvioDesktop.svg?style=for-the-badge
[issues-url]: https://github.com/NuvioMedia/NuvioDesktop/issues
[license-shield]: https://img.shields.io/github/license/NuvioMedia/NuvioDesktop.svg?style=for-the-badge
[license-url]: https://github.com/NuvioMedia/NuvioDesktop/blob/main/LICENSE
