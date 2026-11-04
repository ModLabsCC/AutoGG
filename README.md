# Auto GG

Enhance your gaming experience with the Auto GG Mod, a must-have for Minecraft enthusiasts!
This mod automatically sends a "GG" message at the end of each game, promoting good sportsmanship and camaraderie.
Compatible with popular servers such as Hypixel, BedwarsPractice, PvP Land, Minemen, and Mineplex, this mod ensures you never miss the chance to spread positivity.
Simple to install and lightweight, it runs seamlessly in the background, letting you focus on your game.
Elevate your gameplay and foster a friendly community with Auto GG!

## Per-server settings

Open Auto GG's settings in Mod Menu. Enter a hostname, such as `hypixel.net`, and select **Load** before editing it. A blank hostname edits the default outgoing messages. Switch between **Messages** and **End triggers**, choose **Customize**, edit the list, then **Save**. **Done** closes the screen without saving unsaved edits. Both tabs are saved together.

Messages and triggers inherit independently from the most specific matching hostname, then fall back to default messages and built-in detection. For example, a message override for `play.example.net` can still use triggers configured for `example.net`. Hostname matching ignores case and the connection's port. Enter saved hostnames without a port.

- **Messages:** separate alternatives with ` | `. One is chosen at random when a game ends. Messages are trimmed and must be 1–256 characters, without commands, control characters or formatting codes.
- **End triggers:** separate literal snippets with `|`. Matching is case-sensitive, and **spaces are significant**, including spaces next to the separator. A match anywhere in a received game/system message ends the game. Each snippet must be 1–1024 characters, without control characters or formatting codes. These are plain text, not regular expressions.
- In either editor, use `\|` for a literal pipe and `\\` for a literal backslash.
- Custom triggers **replace** that hostname's built-in detection. A completely empty custom trigger list explicitly disables detection. **Reset to inherited detection** removes the custom override after you select **Save**; this may reveal a parent-domain override or the server's built-in detector. Resetting messages does not reset triggers, and vice versa.
- Unsupported servers, including mcpvp.club and PvP Legacy, remain inactive until you enter reliable end-message snippets from that server. No guessed presets are supplied. Capture the actual system/game chat text; title-only or scoreboard-only results are not detected.

The built-in servers remain Hypixel, BedwarsPractice, PvP Land, Minemen, and Mineplex. Their existing behavior is unchanged unless custom triggers apply. Repeated end messages within three seconds are suppressed. Hypixel additionally avoids repeating the last sent chat message when another configured message is available, or skips sending until another chat message is sent.

Use `/autogg <message>` in game to replace the default message list with one message, or `/autogg` to view its first message. Server messages and triggers remain unchanged. The default is `gg`.

### Saved configuration

Settings are stored locally in Minecraft's Fabric config directory as `autogg.json` and loaded at startup. Mod Menu changes apply immediately after saving. Existing message-only JSON and the older `autogg-message.txt` setting remain supported. Unreadable settings are not overwritten.

```json
{
  "default": ["gg"],
  "servers": {
    "example.net": ["gg", "well played"]
  },
  "triggers": {
    "example.net": ["YOUR VERIFIED END MESSAGE"]
  }
}
```

The example is illustrative, not a real server preset. The optional `triggers` map is independent of `servers`. An absent hostname inherits detection; `"example.net": []` disables detection for that hostname and its subdomains unless a more specific override exists. Restart Minecraft after manual file edits. Back up the file before switching to an older AutoGG build, which does not understand trigger overrides.

## Publishing

Add a `MODRINTH_TOKEN` GitHub Actions secret containing a Modrinth personal access token with permission to create versions for Auto GG. In GitHub Actions, run **Publish to Modrinth** on `main` and enter a unique version number, such as `2026.10.27` or `2026.10.27-1`. The workflow builds and tests the mod before publishing through Gradle Minotaur. The selected version is also written into the mod's metadata; local builds still use the current UTC date by default.
