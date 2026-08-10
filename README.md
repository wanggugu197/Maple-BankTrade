# Maple BankTrade

Maple BankTrade turns money into a system you can actually play with. Carry wallets, manage bank cards, trade items for currency, bind permissions to cards and machines, and push repetitive work into automated trading stations. The mod is built as a real banking and trading layer for NeoForge, with a public API so other content can plug into the same economy instead of inventing a second one.

---

## What It Adds

- Wallets for browsing the cards you can use
- World-saved bank cards with permissions and balances
- Permission cards for sharing access
- Currency-for-item trading in the wallet UI
- Multi-resource trading stations for items, fluids, energy, and currency
- Auto-trading stations for supported sell recipes
- A configurable content switch for servers and modpacks

Built-in content can be disabled, leaving the API and wallet systems available.

---

## API Overview

| Package | Purpose |
|---|---|
| `com.maple.maple_banktrade.api.bank` | Wallet access, world bank storage, card types, permissions, and UI entry points |
| `com.maple.maple_banktrade.api.trade.base` | Generic three-phase trade framework: check, execute, afterSuccess |
| `com.maple.maple_banktrade.api.trade.currency_item` | Currency-to-item trading runtime |
| `com.maple.maple_banktrade.api.trade.machine` | Multi-resource machine trading runtime |
| `com.maple.maple_banktrade.api.machineTrade.station` | Trading station block and block entity host APIs |

Key entry points:

- `MBTBankStates`
- `WalletApiRegistration`
- `TradeRegistry`
- `TradeRunner`
- `MachineTradeHandler`

---

## Installation

1. Install Minecraft 26.1.2 and NeoForge 26.1.2.x.
2. Place the mod jar in your `mods` folder.
3. Add the required dependencies used by this project.
4. Launch the game.

The creative tab `Maple BankTrade` contains the wallet, permission tools, and trading stations.

---

## Quick Start

1. Open the wallet.
   - Use the wallet item, or run `/mbt_bank wallet`.
2. Create a bank card.
   - Use `/mbt_bank factories` to list card factories.
   - Use `/mbt_bank create <factory>` to create one for yourself.
3. Earn currency.
   - Open a tradable card and sell supported items into its sell slot.
4. Spend currency.
   - Left-click buyable entries in the trade panel.
5. Share access.
   - Use the permission UI or a permission card.

New cards start with a zero balance.

---

## Commands

`/mbt_bank` is the main command family.

| Command | Description |
|---|---|
| `/mbt_bank` | List cards you can use |
| `/mbt_bank list` | Same as above |
| `/mbt_bank factories` | List available card factories |
| `/mbt_bank create <factory>` | Create a card for yourself |
| `/mbt_bank info <cardUuid>` | Show card details and balance |
| `/mbt_bank wallet` | Open the wallet UI |
| `/mbt_bank perm_builder` | Open the permission card builder UI |

`create` requires server permission level 4.

---

## Core Systems

### Banking

- Bank cards are stored in world saved data.
- Permissions are server-authoritative.
- Cards can be single-currency, multi-currency, large-balance, tradable, or tagged.
- Wallet UI only shows cards you are allowed to use.

### Currency

- `coins` is the default item-trade currency.
- `gold` and `diamonds` are available for multi-currency cards.
- Balances are handled through resource handlers and transactions.

### Trading

- Currency-item trades run in the wallet UI.
- Machine trades support items, fluids, energy, and currency.
- Auto-trading is available for single-input recipes.
- Failed trades roll back through transaction logic.

### Trading Stations

- `trading_station`
- `item_card_trading_station`
- `auto_trading_station`

These blocks share the same trading framework and differ by supported recipe sets and UI layout.

---

## Configuration

Config file:

```text
config/maple_banktrade/maple_banktrade.yaml
```

| Option | Default | Meaning |
|---|---|---|
| `general.enableModContent` | `true` | Register built-in banks, cards, currencies, trading stations, and commands |
| `general.enableBuiltInTrades` | `true` | Register built-in item trades and machine recipes |

Disabling built-in content leaves the API layer available.

---

## Data and Permissions

- Bank cards are stored in the world save.
- Permission changes are persisted with the world.
- Removing a card also removes related permission entries.
- Card details are validated on the server before opening the detail UI.

World save location:

```text
<world>/data/maple_banktrade/bank_cards.dat
```

---

## Development

Project basics:

- Java 25
- NeoForge 26.1.2
- Gradle build with generated resources in `src/generated/resources`

Useful commands:

```bat
gradlew.bat compileJava
gradlew.bat runClient
gradlew.bat runData
```

`runData` writes generated assets and language files into `src/generated/resources`.

---

## Extending The Mod

- Add a new bank type with `BankType` and `BankInfo`.
- Add a new card type with `BankCardType`, a codec, and optionally a `BankCardFactory`.
- Add a new currency with `CurrencyType`.
- Add a new currency-item trade type with `CurrencyItemTradeType` and a storage entry set.
- Add a new machine trade type with `MachineTradeType` and `MachineTradeStorage`.
- Add a new trading station by extending `BaseTradingStationBlock` and `BaseTradingStationBlockEntity`.

If you add generated recipes, keep them in the `maple_banktrade` namespace.

---

## Links

- Repository: https://github.com/wanggugu197/Maple-BankTrade
- Issues: https://github.com/wanggugu197/Maple-BankTrade/issues
- License: LGPL v3, see `TEMPLATE_LICENSE.txt`
- Author: maple197
