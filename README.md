# PvM Tools

PvM Tools is a RuneLite combat companion focused on clear timers, useful drop reminders, and persistent PvM statistics.

## Features

- Prayer and supported combat potion timer infoboxes.
- Configurable screen warnings and expiry sounds.
- Cannon empty and repair warnings, optional RuneLite notifications, an empty ping, pickup suppression, usage cost tracking, and a live ammo estimate.
- Valuable NPC drop reminders with flash and sound controls.
- Configurable always-on Loot Glow for drops visible through RuneLite's Ground Items settings.
- Superior Slayer spawn alerts with short prayer and kill hints on examine.
- Optional Death Spawn hiding for Nechryael tasks.
- A free-inventory-space infobox with inventory value on hover.
- Shared three-flash warnings with adjustable intensity and timing, plus optional collection-log-style action messages.
- A side panel for daily, weekly, monthly, yearly, and all-time loot, supply, XP, and Slayer task statistics.
- Persistent Slayer task history with time, kills, loot, supplies, profit, and XP.
- Dynamic chat tabs for loot, supply cost, combat XP, Slayer XP, and top skill, plus a fixed Trade-tab clock.
- An optional non-blocking update scroll that can be dismissed or disabled permanently.

## Tracking principles

- Loot is counted only when an item associated with a recent NPC death is picked up.
- Stackable drops use their full stack value, while pickup frequency counts one drop event rather than every item in the stack.
- Supply cost is recorded from potions, food, and cannonballs actually consumed during play.
- Combat and Slayer trackers record XP gained after tracking starts, never the account's existing total XP.
- Tracker values persist by default until the user resets them; chat-tab trackers appear after their first activity.
- Slayer task timing pauses outside active play, and resumed progress for the same task remains one task record.
- Screen warnings stop as soon as their cause is resolved; prayer remains eligible outside combat and cannon pickup suppresses false reload warnings.
- PvM Tools does not modify another plugin's saved settings or globally rewrite Ground Items configuration.

## Support

Suggestions and bug reports are welcome on Discord: https://discord.gg/utYem4XhQS
