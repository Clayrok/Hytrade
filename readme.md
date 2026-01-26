# Hytrade - Hytale mod
<p align="center">This is my first mod ever, please be kind <3</p>

## Description
Hytrade adds a secure trading system to Hytale, allowing players to exchange items safely through a dedicated UI. Prevent theft and trade with confidence!

## Features
* **Trade Requests:** Players must now accept or decline trade invitations before a session begins.
* **Settings Panel:** Customize your experience in-game. Toggle layouts, adjust positions, and manage your ignore list.
* **Ignore System:** Block specific players from sending you requests or toggle "Ignore All" for total peace of mind.
* **Layout & Positioning:** Choose between horizontal or vertical layouts and position the panel to the left, center, or right.
* **Validation Checkmarks:** Clear visual feedback when both parties have confirmed the deal.
* **Trade Cooldown:** Built-in anti-spam protection (6 seconds by default).

## Commands
Sends a trade request to the specified player.
### `/trade <playerName>`

Opens the personal settings panel.
### `/hytrade settings`

Hot-reloads the configuration file (No server restart required).
### `/hytrade reload`

## Permissions
**Base Trading:**
### `clayrok.hytrade.trade`

**Long Distance (No distance check):**
### `clayrok.hytrade.fromfar`

**Config Reload:**
### `clayrok.hytrade.reload`

## Configuration
The `config.json` allows server owners to customize the experience:
* **Customizable Permissions:** Disable permissions entirely or change them to fit your server.
* **Behavior:** Adjust trade cooldowns and maximum trade distance.
* **Sounds:** All trade-related sounds can be modified via the config file.

## Notes
* Default trade range is **5 blocks** (unless bypassed by permission).
* Only items in your inventory can be traded (**hotbar excluded**).
* Only **full slots** are tradable. You must split stacks before trading specific quantities.

## Screenshots
![Trade requests](https://i.imgur.com/D7VnKsv.png)
![Settings panel](https://i.imgur.com/If3FAtu.png)
![Trade UI vertical left](https://i.imgur.com/cBX5Sab.png)
![Trade UI vertical center](https://i.imgur.com/ThYTDy5.png)
![Trade UI vertical right](https://i.imgur.com/vXaT6TI.png)
![Trade UI horizontal left](https://i.imgur.com/vnSONE5.png)
![Trade UI horizontal center](https://i.imgur.com/THDiyR6.png)
![Trade UI horizontal right](https://i.imgur.com/IHOHHk0.png)
![Trade Success Notification](https://i.imgur.com/3oDclUP.jpeg)
![Trade Cancel Notification](https://i.imgur.com/6qoEprG.jpeg)
