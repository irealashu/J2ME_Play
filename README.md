# J2ME-Loader 

[![Build Status](https://app.bitrise.io/app/d9254be52c74982a/status.svg?token=DIHxcpAPIg0VXSHpeXsHHA&branch=master)](https://app.bitrise.io/app/d9254be52c74982a)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/j2me-loader/localized.svg)](https://crowdin.com/project/j2me-loader)
[![GitHub release](https://img.shields.io/github/release/nikita36078/J2ME-Loader.svg)](https://github.com/nikita36078/J2ME-Loader/releases)

J2ME-Loader is a J2ME emulator for Android. It supports most 2D and 3D games (including Mascot Capsule 3D ones). Emulator has a virtual keyboard, individual settings for each application, scaling support.
This project is a fork of [J2meLoader](https://github.com/NaikSoftware/J2meLoader).  
Special thanks to [woesss](https://github.com/woesss), the author of [JL-Mod](https://github.com/woesss/JL-Mod), for creating open-source Mascot Capsule implementation.

System requirements: Android 4.0+  
[4PDA discussion](https://4pda.to/forum/index.php?showtopic=824201)  
[XDA-Developers](https://forum.xda-developers.com/android/apps-games/app-j2me-loader-t3777889)  
[EmuGen wiki](https://emulation.gametechwiki.com/index.php/J2ME_Loader)  
[Discord](https://discord.gg/Ag4rcpz)  
[Automated builds](https://install.appcenter.ms/users/nikita36078/apps/j2me-loader/distribution_groups/testers)

<a href="https://play.google.com/store/apps/details?id=ru.playsoftware.j2meloader">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="75"></a>
<a href="https://f-droid.org/app/ru.playsoftware.j2meloader">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="75"></a>

## Features

- **High Compatibility**: Supports most 2D and 3D J2ME games, including Mascot Capsule 3D implementations.
- **Complete Application Data Import / Export**:
  - Full backup and restore of all installed games, RMS save states, high scores, per-game configurations, and virtual keyboard profiles.
  - Export to a single ZIP archive to device storage (via Storage Access Framework) or share directly to other apps/devices via Android Sharesheet.
  - Safe, automatic restoration with database synchronization and path-traversal (Zip-Slip) verification.
- **Enhanced In-Game Navigation & Controls**:
  - Quick floating screenshot action button (draggable, toggleable).
  - Double-tap back protection to prevent accidental game exits.
  - Quick return to home / game library from in-game menu.
  - Screen orientation lock toggle directly inside running MIDlets.
- **Custom Graphics Shaders & Display Options**:
  - Motion smoothing and OLED vibrant color boost post-processing shaders.
  - Configurable scaling filters, immediate processing mode, and custom resolutions.
- **Built-in Demo Games Package**:
  - Quick-start demo game installation for instant testing of retro classic games.
- **Customizable Virtual Controls**:
  - Flexible on-screen touch keyboard with customizable button layout, haptics, and sizing.

## Compatibility
[List of the tested Java Games (Touchscreen)](https://github.com/nikita36078/J2ME-Loader/wiki/List-of-Tested-Java-Games-(Touchscreen))  
[List of the tested Java Games (Non Touchscreen)](https://github.com/nikita36078/J2ME-Loader/wiki/List-of-Tested-Java-Games-(Non-Touchscreen))  
[List of the Java Games with Bugs](https://github.com/nikita36078/J2ME-Loader/wiki/List-of-Java-Games-with-Bugs)

## Tips
 - Enabling filtering in some cases can greatly reduce performance. Disable this option if game is too slow.
 - Image flickering issues can be fixed by enabling the "Immediate processing mode" option.

## Screenshots

<img src="/screenshots/screen.jpg" width="288" height="512"> <img src="/screenshots/screen2.jpg" width="288" height="512">
<img src="/screenshots/screen3.jpg" width="288" height="512"> <img src="/screenshots/screen4.jpg" width="288" height="512">
* For more screenshots check out the [wiki](https://emulation.gametechwiki.com/index.php/J2ME_Loader#Screenshots)

## License
> Copyright 2017-2024 Nikita Shakarun.  
> Fork By: Ashutosh Singh Copyright 2026.  
> Github: [https://github.com/irealashu](https://github.com/irealashu)  
>  
> Licensed under the [Apache License, Version 2.0.](http://www.apache.org/licenses/LICENSE-2.0)  
> (See the [LICENSE](https://github.com/nikita36078/J2ME-Loader/blob/master/LICENSE) file for the whole license text.)
