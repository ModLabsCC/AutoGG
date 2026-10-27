# Auto GG

Enhance your gaming experience with the Auto GG Mod, a must-have for Minecraft enthusiasts!
This mod automatically sends a "GG" message at the end of each game, promoting good sportsmanship and camaraderie.
Compatible with popular servers such as Hypixel, BedwarsPractice, PvP Land, Minemen, and Mineplex, this mod ensures you never miss the chance to spread positivity.
Simple to install and lightweight, it runs seamlessly in the background, letting you focus on your game.
Elevate your gameplay and foster a friendly community with Auto GG!

Open Auto GG's settings in Mod Menu to set default messages and server overrides. Enter a hostname such as `hypixel.net`, then use **Load**, **Save**, or **Remove override**. Subdomains use the most specific matching hostname; unmatched servers use the defaults. Separate multiple messages with ` | `; use `\|` for a literal pipe and `\\` for a literal backslash. One message is chosen at random when a game ends.

Use `/autogg <message>` in game to replace the default list with one message, or `/autogg` to view its first message. Existing server overrides remain unchanged. The default is `gg`.
On Hypixel, Auto GG avoids repeating the last sent chat message when a different configured message is available. Otherwise it skips sending until another chat message is sent, reducing duplicate-message warnings.

Overrides do not add game-end detection: Auto GG only sends on the supported servers listed above. Adding an override for another server, such as `modlabs.cc`, stores the messages but does not enable game-end detection there.

## Publishing

Add a `MODRINTH_TOKEN` GitHub Actions secret containing a Modrinth personal access token with permission to create versions for Auto GG. In GitHub Actions, run **Publish to Modrinth** on `main` and enter a unique version number, such as `2026.10.27` or `2026.10.27-1`. The workflow builds and tests the mod before publishing through Gradle Minotaur. The selected version is also written into the mod's metadata; local builds still use the current UTC date by default.
