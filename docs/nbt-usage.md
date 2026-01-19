# NBT

Timber frame variants can be placed directly with block-entity NBT. The
`timber_id` value is a namespaced Minecraft resource ID for the timber texture:

```mcfunction
/fill [x y z] [x y z] timber_frames:timber_frame{timber_id:"minecraft:stripped_dark_oak_log"}
```

The value is saved with each placed block, sent to clients, and retained by
pick-block and the dropped timber-frame item. Omitting `timber_id` uses the
default `minecraft:stripped_oak_log` timber.

## Limitations

- `timber_id` changes the timber texture only. It does not change the block,
  collision, recipes, tags, or the block's structural lines.
- Use a valid resource ID with a corresponding block texture. An invalid ID will render as a missing texture.
- Replacing the block with a command that does not provide the block-entity NBT
  resets it to the default timber.
