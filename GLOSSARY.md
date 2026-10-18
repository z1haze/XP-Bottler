# XPBottler

Lets players move their experience out of themselves and into items they can store, trade and use later.

## Language

**XP**:
Raw experience points, the unit every item in this plugin stores and hands back. Never levels.
_Avoid_: Levels, experience levels

**Bottle**:
An Experience Bottle made by this plugin that remembers the exact XP it holds. Thrown to release it.
_Avoid_: XP bottle, potion

**Flask**:
A crafted, reusable, unstackable item that holds any amount of XP up to its Capacity and is drunk from rather than thrown.
_Avoid_: Bottle, jar, container

**Capacity**:
The most XP a single Flask may hold, fixed for that Flask's lifetime when it is created; unlimited when not set.
_Avoid_: Size, max

**Fill**:
Moving one Sip's worth of a player's XP into a Flask, or less if the player has less or the Flask is nearly at Capacity. The mirror of a Sip.
_Avoid_: Deposit, charge

**Sip**:
One drink from a Flask, returning a configured amount of XP, or whatever remains if the Flask holds less.
_Avoid_: Withdraw, drink, use
