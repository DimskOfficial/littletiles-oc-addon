# LittleTiles OC Addon (Minecraft 1.12.2)

📖 [Русская версия](README_RU.md)

A Forge/FML addon that extends LittleTiles with OpenComputers integration. Adds a Signal Converter block that reads LittleTiles cable signals.

## What it does

- Adds **Signal Converter OC** block
- Connects to LittleTiles signal cables
- Exposes signals to OpenComputers Lua scripts
- Component name: `lt_signal_converter`

## Requirements

- Minecraft 1.12.2
- Forge 14.23.x
- LittleTiles mod
- OpenComputers mod

## Installation

1. Install Forge, LittleTiles, OpenComputers
2. Put this mod's JAR in `mods/` folder
3. Launch Minecraft

## Usage

### 1. Setup in game

1. Place **Signal Converter OC** block
2. Connect LittleTiles signal cables to it (from any side)
3. Place OpenComputers computer nearby
4. Connect computer to converter with OC cable OR place computer touching the converter

### 2. Discover the component

```lua
local component = require("component")

-- Find the converter
local addr = component.get("lt_signal_converter")
if not addr then
  error("Signal Converter not found! Make sure it's connected.")
end

local converter = component.proxy(addr)
print("Connected to Signal Converter")
```

### 3. Read signals

#### Get combined signal (0-15)
Returns the OR-combined value from all connected cables:
```lua
local signal = converter.getSignal()
print("Combined signal:", signal) -- 0 to 15
```

#### Get individual bits
Returns 4 boolean values:
```lua
local bit0, bit1, bit2, bit3 = converter.getBits()
print("Bits:", bit0, bit1, bit2, bit3)
```

### 4. Per-cable access

#### Get cable count
```lua
local count = converter.getCableCount()
print("Connected cables:", count)
```

#### Get signal from specific cable
```lua
-- Get signal from cable #0
local signal = converter.getCableSignal(0)
print("Cable 0 signal:", signal) -- 0 to 15
```

#### Get bits from specific cable
```lua
local b0, b1, b2, b3 = converter.getCableBits(0)
print("Cable 0 bits:", b0, b1, b2, b3)
```

#### Get cable facing direction
```lua
local facing = converter.getCableFacing(0)
print("Cable 0 is on side:", facing) -- "north", "south", etc.
```

### 5. Get all cables info

```lua
local cables = converter.getAllCables()

for index, cable in pairs(cables) do
  print("Cable #" .. index)
  print("  Facing:", cable.facing)
  print("  Signal:", cable.signal)
  print("  Bits:", cable.bit0, cable.bit1, cable.bit2, cable.bit3)
end
```

### 6. Debug info

```lua
local info = converter.debugInfo()
print("Total cables:", info.cableCount)
print("Combined signal:", info.combinedSignal)

for i, cable in ipairs(info.cables) do
  print("Cable " .. i .. ":")
  print("  Facing:", cable.facing)
  print("  Has network:", cable.hasNetwork)
  print("  Signal:", cable.signal)
end
```

## Complete example script

```lua
local component = require("component")
local event = require("event")

-- Connect to converter
local addr = component.get("lt_signal_converter")
if not addr then
  print("Signal Converter not found!")
  return
end

local sc = component.proxy(addr)
print("Signal Converter connected!")

-- Monitor signals
while true do
  os.sleep(1)
  
  -- Get combined signal
  local signal = sc.getSignal()
  print("Signal:", signal)
  
  -- Get all cables
  local cables = sc.getAllCables()
  for i, cable in pairs(cables) do
    print("  Cable " .. i .. " on " .. cable.facing .. ": " .. cable.signal)
  end
end
```

## How it works

- The converter acts as an **INPUT** component in LittleTiles networks
- It receives signals from connected cables
- When cables merge/split, it automatically updates
- Signal values: 0-15 (4 bits)
- Multiple cables are combined with OR operation

## API Reference

| Method | Arguments | Returns | Description |
|--------|-----------|---------|-------------|
| `getSignal()` | - | `number` | Combined signal from all cables (0-15) |
| `getBits()` | - | `b0, b1, b2, b3` | Individual bits as booleans |
| `getCableCount()` | - | `number` | Number of connected cables |
| `getCableSignal(index)` | `index: number` | `number` | Signal from cable #index (0-15) |
| `getCableBits(index)` | `index: number` | `b0, b1, b2, b3` | Bits from cable #index |
| `getCableFacing(index)` | `index: number` | `string` | Direction cable is connected from |
| `getAllCables()` | - | `table` | Table with all cable data |
| `debugInfo()` | - | `table` | Debug information |

## Troubleshooting

**"lt_signal_converter not found"**
- Make sure the block is placed and computer is connected via OC network
- Check that converter block has cables attached

**Always returns 0**
- Verify cables are connected to the converter block
- Check that LittleTiles signal source (lever, button, etc.) is active
