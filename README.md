# Villager Shops [![License: CC0](https://img.shields.io/badge/License-CC0-blue.svg)](https://creativecommons.org/publicdomain/zero/1.0/)

This mod is for Fabric Servers (or Single-Player) and requires [Fabric API](https://modrinth.com/mod/fabric-api) for event handling. Works perfectly with **Vanilla clients**!

### Current Features
- **Transform Any Villager** - Turn any Unemployed or Nitwit villager into a professional shopkeeper.
- **Linked Storage** - Shops are powered by physical containers (Chests, Barrels, etc.) placed beneath the villager.
- **Internal Currency System** - Payments are stored safely "inside" the villager, preventing chest-stealing or overflow issues.
- **Owner "Cash Out"** - Securely withdraw your earnings using a custom "Admin Key" (signed book).
- **Client Optional** - No client-side installation required! Players can join and trade with zero mods installed.

### How to use Villager Shops
- **Create a Shop**:
  - Find an **Unemployed** or **Nitwit** villager.
  - Place a container (9+ slots) at the villager's feet or directly below them.
  - Fill the container's first three slots:
    - **Slot 1**: The item you want to sell.
    - **Slot 2**: The item you want as payment (the price).
    - **Slot 3**: Your **Admin Key** (a signed book).
  - Name the villager "**Shop**" using a Name Tag to activate the storefront.
- **Cashing Out**:
  - Hold the Admin Key (from Slot 3) and trade it with the villager to receive all stored currency.
- **Unlinking**:
  - Rename the villager to anything *except* "Shop".
  - **Requirement**: The villager must have 0 stored currency and the chest must be empty of the "Sold Item" for unlinking to work.

### (Optional) Client-side Features
- Installing the mod client-side is not required for server connectivity, but it allows you to use the mod in your Singleplayer worlds!

### 📝 Development Note
This project was developed with the assistance of **AI** and is provided **as-is**, without warranty of any kind. While it has been tested for stability on Minecraft 26.1, please ensure you make backups of your world before installing.

### Shout-outs
- **TinoMartino** for the "Shop Permit" icon and development direction.
