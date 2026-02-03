# LittleTiles OC Addon (Minecraft 1.12.2)

📖 [Русская версия](README_RU.md)

A Forge/FML addon that extends LittleTiles with OpenComputers integration. Adds a Signal Converter block that reads LittleTiles cable signals and sends events when signals change.

## What it does

- Adds **Signal Converter OC** block
- Connects to LittleTiles signal cables (up to 4 cables, 4 bits each)
- Exposes signals to OpenComputers Lua scripts
- Sends **`signal_changed`** events when any signal changes
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

### 3. Reading Signals (Polling)

#### Get combined signal (0-15)
Returns the OR-combined value from all connected cables:
```lua
local signal = converter.getSignal()
print("Combined signal:", signal) -- 0 to 15
```

#### Check specific bit
```lua
local isHigh = converter.isSignalHigh(0) -- check bit 0
print("Bit 0 is high:", isHigh)
```

#### Get individual bits
```lua
local bit0, bit1, bit2, bit3 = converter.getBits()
print("Bits:", bit0, bit1, bit2, bit3)
```

### 4. Event-Based Monitoring (Recommended)

Instead of polling, use **`event.pull()`** to wait for signal changes:

```lua
local component = require("component")
local event = require("event")

local addr = component.get("lt_signal_converter")
local converter = component.proxy(addr)

print("Waiting for signal changes...")

while true do
  -- Wait for signal_changed event (timeout 10 seconds)
  local _, newSignal = event.pull(10, "signal_changed")
  
  if newSignal then
    print("Signal changed to:", newSignal)
    
    -- Get detailed info
    local bit0 = converter.isSignalHigh(0)
    local bit1 = converter.isSignalHigh(1)
    print("Bit0:", bit0, "Bit1:", bit1)
  else
    print("No change within 10 seconds")
  end
end
```

### 5. Per-Cable Access

#### Get cable count
```lua
local count = converter.getCableCount()
print("Connected cables:", count)
```

#### Get signal from specific cable
```lua
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

### 6. Get all cables info

```lua
local cables = converter.getAllCables()

for index, cable in pairs(cables) do
  print("Cable #" .. index)
  print("  Facing:", cable.facing)
  print("  Signal:", cable.signal)
  print("  Bits:", cable.bit0, cable.bit1, cable.bit2, cable.bit3)
end
```

### 7. Debug info

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

## Complete Examples

### Example 1: Simple Signal Monitor with Events

```lua
local component = require("component")
local event = require("event")

local addr = component.get("lt_signal_converter")
if not addr then
  print("Signal Converter not found!")
  return
end

local sc = component.proxy(addr)
print("Monitoring signals... Press Ctrl+C to stop")

while true do
  -- Wait for signal change event
  local _, signal = event.pull("signal_changed")
  print("Signal changed to:", signal)
  
  -- Check individual bits
  for i = 0, 3 do
    if sc.isSignalHigh(i) then
      print("  Bit " .. i .. " is ON")
    end
  end
end
```

### Example 2: Monitor Specific Cable with Events

```lua
local component = require("component")
local event = require("event")

local sc = component.proxy(component.get("lt_signal_converter"))

print("Monitoring cable 0...")
local lastSignal = sc.getCableSignal(0)

while true do
  local _, newSignal = event.pull(5, "signal_changed")
  
  if newSignal then
    local current = sc.getCableSignal(0)
    if current ~= lastSignal then
      print("Cable 0 changed from", lastSignal, "to", current)
      lastSignal = current
    end
  end
end
```

### Example 3: Simple Redstone Control

```lua
local component = require("component")
local event = require("event")
local redstone = component.redstone -- if available

local sc = component.proxy(component.get("lt_signal_converter"))

print("Controlling redstone based on signal...")

while true do
  local _, signal = event.pull("signal_changed")
  
  if signal > 0 then
    print("Signal detected! Turning on redstone.")
    -- Turn on redstone output
    if redstone then
      redstone.setOutput(1, 15) -- side, power level
    end
  else
    print("No signal. Turning off redstone.")
    if redstone then
      redstone.setOutput(1, 0)
    end
  end
end
```

## How it works

- The converter acts as an **INPUT** component in LittleTiles networks
- It receives signals from connected cables (up to 4 cables)
- Each cable carries 4 bits (values 0-15)
- When **any** signal changes, it sends `signal_changed` event with the new combined value
- Multiple cables are combined with OR operation
- You can use either polling (`getSignal()`) or events (`event.pull()`)

## API Reference

| Method | Arguments | Returns | Description |
|--------|-----------|---------|-------------|
| `getSignal()` | - | `number` | Combined signal from all cables (0-15) |
| `isSignalHigh(bit)` | `bit: 0-3` | `boolean` | Check if specific bit is high |
| `getBits()` | - | `b0, b1, b2, b3` | All bits as booleans |
| `getCableCount()` | - | `number` | Number of connected cables |
| `getCableSignal(index)` | `index: 0+` | `number` | Signal from specific cable |
| `getCableBits(index)` | `index: 0+` | `b0, b1, b2, b3` | Bits from specific cable |
| `getCableFacing(index)` | `index: 0+` | `string` | Direction (north, south, etc.) |
| `getAllCables()` | - | `table` | Table with all cable data |
| `debugInfo()` | - | `table` | Debug information |

### Events

| Event | Arguments | Description |
|-------|-----------|-------------|
| `signal_changed` | `newSignal: number` | Fired when any signal changes |

## Troubleshooting

**"lt_signal_converter not found"**
- Make sure the block is placed and computer is connected via OC network
- Check that converter block has cables attached
- Verify the mod is loaded in Minecraft

**Events not firing**
- Ensure you're using `event.pull()` correctly
- Check that the computer is connected to the converter
- Try the debugInfo() method to verify cables are detected

**Always returns 0**
- Verify cables are connected to the converter block
- Check that LittleTiles signal source (lever, button, etc.) is active
- Use debugInfo() to check if cables are detected

