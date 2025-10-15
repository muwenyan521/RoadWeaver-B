# MC-249136 Fix

[![Requires Fabric API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/requires/fabric-api_vector.svg)](https://modrinth.com/mod/fabric-api)
[![Available on GitLab](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/gitlab_vector.svg)](https://gitlab.com/horrific-tweaks/MC-249136)
[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/mc-249136-fix)

This mod implements a minimal hacky fix for [MC-249136](https://bugs.mojang.com/browse/MC-249136), a significant server lag spike that occurs when locating buried treasure structures for shipwreck loot.

To avoid this lag spike, structure location is moved into a separate thread that does not block the server tick loop. This does nothing to improve the structure location performance, but it does at least prevent it from significantly impeding gameplay.

![An empty Minecraft buried treasure map, with the text "Loading" in the center](https://gitlab.com/horrific-tweaks/MC-249136/-/raw/main/screenshot.png?v=1)

## Configuration

The map "loading" images can be customized by placing `128x128` PNG files in `/config/mc249136/map_loading.png` and `/config/mc249136/map_not_found.png`.

For performance reasons, these images will draw all pixels as black unless they are fully transparent (i.e. `rgba(0,0,0,0)`). Minecraft's map color palette is difficult and expensive to calculate - so this mod literally just does an `if (rgb != 0)` to draw the image.

Known issues:
- This mod does not handle state persistence, and only updates loaded maps once they are inside a player's inventory.
  This means that it is possible to prevent a treasure map from loading by breaking the chest, leaving it in item entity
  form, and then restarting the server.
