# Villager Shops [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This mod is for Fabric Servers (or Single-Player) and requires [Fabric API](https://modrinth.com/mod/fabric-api) for event handling. Works perfectly with **Vanilla clients**!

### Current Features
- **Transform Any Villager** - Turn any Unemployed or Nitwit villager into a professional shopkeeper.
- **Linked Storage** - Shops are powered by physical containers (Chests, Barrels, etc.) placed beneath the villager.
- **Internal Currency System** - Payments are stored safely "inside" the villager, preventing chest-stealing or overflow issues.
- **Owner "Cash Out"** - Securely withdraw your earnings if you use a signed book.
- **Client Optional** - No client-side installation required! Players can join and trade with the vanilla client.

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

### Development Note
This project was developed with the assistance of **AI**. While it has been tested for stability on Minecraft 26.1 some bugs might have slick trough. Feel free to report them!

