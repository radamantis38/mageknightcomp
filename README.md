# Mage Knight Companion

An Android companion app for the **solo game of Mage Knight** (base game). It shuffles and
manages the offers and decks, and plays the **Dummy player** for you, so you can focus on
your own Hero.

Supports the four base-game Heroes: Arythea, Goldyx, Norowas and Tovak.

## Download

**[Download the APK](https://github.com/radamantis38/mageknightcomp/raw/main/MageKnightCompanion-public.apk)**
(Android 6.0 or later). It comes without card pictures; see [Card pictures](#card-pictures).
To install it, allow installing apps from unknown sources on your device.

## Features

**Offers**
- Advanced Action, Spell and Unit offers, each with its deck. Taking a card replenishes the
  offer by the rules (new cards come in at the top).
- Monasteries (each adds an Advanced Action to the Unit offer) and the core tile switch
  (Elite and Regular Units alternate once it is revealed).
- Conquered cities: a conquered White city lets you add an Elite Unit to the offer, a
  conquered Green city lets you draw an Advanced Action from the deck.
- Gaining Artifacts: draws that many + 1, you pick which one goes back to the bottom of the
  deck.

**Dummy**
- Fully automated turns: flips 3 cards, then as many more as it has crystals of the last
  card's color. Announces the End of the Round when its deck runs out.
- Starts with its Hero's 16 Basic Actions and crystals.
- At the end of each Round it gets the lowest Advanced Action from the offer and a crystal
  of the lowest Spell's color, then reshuffles.

**Round**
- Tactic selection for Day and Night: you pick, the Dummy gets a random one, and a popup
  says who goes first. Used Tactics are removed for the rest of the game.
- Prepare the next Round in one tap: refreshes the offers and the Dummy as described above.

**General**
- New games start with random, different Heroes for you and the Dummy (both can be changed).
- The game is saved after every change, and every action can be undone.
- Card pictures are shown when available. The app works fine without them.

## Card pictures

Card pictures are for personal use only and are **not included in this repository**.
`app/src/main/assets/cards/` is ignored by git. Without pictures, the app shows card names
and colors instead.

## Card catalog editor

The cards live in `app/src/main/assets/cards.json`. A small local web editor helps you edit
that list and add pictures to the cards:

```sh
python3 tools/card-editor/server.py [port]
```

Then open http://localhost:8000. Only Python 3 is needed, and the server only listens on
localhost. Pictures are shrunk in the browser before being saved.

Each physical card is its own entry with its own picture, so duplicates such as two copies
of a Basic Action are separate entries.

## Building

Requires JDK 17 and the Android SDK (minSdk 23).

```sh
./gradlew installDebug        # build and install on a connected device or emulator
./gradlew testDebugUnitTest   # run the unit tests
```

To build an APK you can share, without the card pictures:

```sh
./gradlew assembleDebug -PwithoutCardImages
```

## Rules reference

The offer and Dummy rules follow the solo game rules in the Mage Knight Ultimate Edition
rulebook.
