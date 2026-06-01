# RuneLite API Events Reference

**Package:** `net.runelite.api.events`  
**API Version:** 1.12.27

## Overview

The events package contains classes representing various occurrences within the RuneLite client and game engine. These events are fired in response to player actions, game state changes, and other client activities.

Subscribe to events in a plugin by annotating a method with `@Subscribe`:

```java
@Subscribe
public void onGameTick(GameTick event) { ... }
```

## Events

| Event | Description |
|-------|-------------|
| AccountHashChanged | Triggered when account hash is modified |
| ActorDeath | An event fired when an actor dies |
| AmbientSoundEffectCreated | An event fired when an ambient sound effect is created |
| AnimationChanged | An event where the Actor has changed animations |
| AreaSoundEffectPlayed | Sound effect played in an area |
| BeforeMenuRender | Occurs before menu rendering |
| BeforeRender | Posted at the start of every frame |
| CanvasSizeChanged | An event posted when the canvas size might have changed |
| ChatMessage | An event where a new chat message is received |
| ClanChannelChanged | An event fired when the local player joins or leaves a clan channel |
| ClanMemberJoined | An event when a clan member joins a clan channel |
| ClanMemberLeft | An event when a clan member leaves a channel |
| ClientTick | Posted every client tick (20ms) |
| CommandExecuted | An event where a command has been used in the chat |
| DecorativeObjectDespawned | Decorative object removed from tile |
| DecorativeObjectSpawned | Decorative object attached to tile |
| FakeXpDrop | Experience drop that doesn't affect stats |
| FocusChanged | An event where the focus state of the client changes |
| FriendsChatChanged | An event where the client has joined or left a friends chat |
| FriendsChatMemberJoined | Member joins friends chat |
| FriendsChatMemberLeft | Member leaves friends chat |
| GameObjectDespawned | GameObject removed from tile |
| GameObjectSpawned | An event where a GameObject is added to a Tile |
| GameStateChanged | An event where the clients game state has changed |
| GameTick | An event called once every game tick, after all packets have processed |
| GrandExchangeOfferChanged | An event where a GrandExchangeOffer has been updated with new information |
| GrandExchangeSearched | An event where the Grand Exchange has been searched |
| GraphicChanged | An event where the graphic of an Actor has changed |
| GraphicsObjectCreated | An event where a new GraphicsObject has been created |
| GroundObjectDespawned | GroundObject removed from tile |
| GroundObjectSpawned | GroundObject added to tile |
| HitsplatApplied | An event called when a Hitsplat is processed on an Actor |
| InteractingChanged | An event called when the actor an actor is interacting with changes |
| ItemContainerChanged | An event called whenever the stack size of an Item in an ItemContainer is modified |
| ItemDespawned | Called when an item pile despawns from the ground |
| ItemQuantityChanged | Called when the quantity of an item pile changes |
| ItemSpawned | Called when an item pile spawns on the ground |
| MenuEntryAdded | An event when a new entry is added to a right-click menu |
| MenuOpened | An event where a menu has been opened |
| MenuOptionClicked | An event where a menu option has been clicked |
| MenuShouldLeftClick | Event called when the client is checking if the menu should be opened on left click |
| NameableNameChanged | An event where a Nameable has had their name changed |
| NpcChanged | Fires after the composition of an NPC changes |
| NpcDespawned | An event where an NPC has despawned |
| NpcSpawned | An event where an NPC has spawned |
| OverheadTextChanged | Event fired when an actors overhead text is changed |
| PlayerChanged | Player entity changed |
| PlayerDespawned | An event where a Player has despawned |
| PlayerMenuOptionsChanged | Player menu options updated |
| PlayerSpawned | An event where a Player has spawned |
| PostAnimation | An event posted when an Animation is loaded |
| PostClientTick | Posted at the end of each client tick (20ms) |
| PostHealthBarConfig | Health bar configuration applied |
| PostItemComposition | An event called after a new ItemComposition is created and its data is initialized |
| PostMenuSort | Posted after the menu is sorted, but before clicks are processed |
| PostObjectComposition | An event called after a new ObjectComposition is created and its data is initialized |
| PostStructComposition | An event called after a new StructComposition is created and its data is initialized |
| PreMapLoad | This event is run from the maploader thread prior to the map load completing |
| ProjectileMoved | An event called whenever a Projectile has moved towards a point |
| RemovedFriend | An event triggered when a player is removed from the friend or ignore list |
| ResizeableChanged | An event where the game has changed from fixed to resizable mode or vice versa |
| ScriptCallbackEvent | A callback from a runelite_callback opcode in a cs2 |
| ScriptPostFired | An event that is fired after the designated script is ran |
| ScriptPreFired | An event that is fired before the designated script is ran |
| SoundEffectPlayed | Sound effect triggered |
| StatChanged | An event where the experience, level, or boosted level of a Skill has been modified |
| UsernameChanged | An event where the username the client will log in with has changed |
| VarbitChanged | An event when a varbit or varplayer has changed |
| VarClientIntChanged | Client integer variable changed |
| VarClientStrChanged | Client string variable changed |
| VolumeChanged | Audio volume adjusted |
| WallObjectDespawned | WallObject removed from tile |
| WallObjectSpawned | WallObject added to tile |
| WidgetClosed | Posted when an interface is about to be closed |
| WidgetDrag | Called each game cycle when a widget is being dragged |
| WidgetLoaded | An event where a Widget has been loaded |
| WorldChanged | Posted when the game world the client wants to connect to has changed |
| WorldEntityDespawned | Called when a world entity despawns |
| WorldEntitySpawned | Called when a world entity spawns |
| WorldListLoad | Event when the world list is loaded for the world switcher |
| WorldViewLoaded | Called when a worldview has been loaded |
| WorldViewUnloaded | Called when a worldview has been unloaded |
