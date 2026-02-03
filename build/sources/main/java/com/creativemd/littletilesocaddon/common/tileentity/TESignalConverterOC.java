package com.creativemd.littletilesocaddon.common.tileentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.creativemd.creativecore.common.tileentity.TileEntityCreative;
import com.creativemd.creativecore.common.utils.math.BooleanUtils;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.signal.component.ISignalStructureBase;
import com.creativemd.littletiles.common.structure.signal.component.ISignalStructureComponent;

import com.creativemd.littletiles.common.structure.signal.component.SignalComponentType;
import com.creativemd.littletiles.common.structure.signal.network.SignalNetwork;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.Persistable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TESignalConverterOC extends TileEntityCreative implements ISignalStructureComponent, Environment, Persistable {
    
    private static final String TAG_NODE = "oc:node";
    private static final String TAG_CABLE_STATES = "cableStates";
    
    protected Node node;
    
    // Track individual cable connections
    // We don't have our own network - we're part of cable networks!
    private List<CableConnection> cables = new ArrayList<>();
    private boolean[] combinedState = new boolean[4];
    
    // Inner class to track each cable connection
    public static class CableConnection {
        public ISignalStructureBase base;
        public EnumFacing facing;
        public boolean[] lastState = new boolean[4];
        
        public CableConnection(ISignalStructureBase base, EnumFacing facing) {
            this.base = base;
            this.facing = facing;
        }
        
        // Always get current network - it can change after merge!
        public SignalNetwork getNetwork() {
            return base.hasNetwork() ? base.getNetwork() : null;
        }
    }
    
    public TESignalConverterOC() {
    }
    
    @Override
    public void handleUpdate(NBTTagCompound nbt, boolean chunkUpdate) {
        readFromNBT(nbt);
    }
    
    // OC Environment
    @Override
    public Node node() {
        return node;
    }
    
    @Override
    public void onConnect(Node node) {
    }
    
    @Override
    public void onDisconnect(Node node) {
    }
    
    @Override
    public void onMessage(Message message) {
    }
    
    @Override
    public void load(NBTTagCompound tag) {
        if (node != null && node.host() == this) {
            node.load(tag.getCompoundTag(TAG_NODE));
        }
    }
    
    @Override
    public void save(NBTTagCompound tag) {
        if (node != null && node.host() == this) {
            NBTTagCompound nodeTag = new NBTTagCompound();
            node.save(nodeTag);
            tag.setTag(TAG_NODE, nodeTag);
        }
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        save(compound);
        return super.writeToNBT(compound);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        load(compound);
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        
        // DEBUG: Print loading info
        System.out.println("[LT-OC-Addon] TESignalConverterOC loaded at " + pos);
        System.out.println("[LT-OC-Addon] World: " + (world != null ? world.isRemote ? "client" : "server" : "null"));
        
        if (node == null) {
            node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
                .withComponent("lt_signal_converter", Visibility.Network)
                .create();
            System.out.println("[LT-OC-Addon] OC Node created");
        }
        li.cil.oc.api.Network.joinOrCreateNetwork(this);
    }
    

    
    @Override
    public void invalidate() {
        super.invalidate();
        if (node != null) {
            node.remove();
            node = null;
        }
    }
    
    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (node != null) {
            node.remove();
            node = null;
        }
    }
    
    // Find cable by base reference
    private CableConnection findCable(ISignalStructureBase base) {
        for (CableConnection conn : cables) {
            if (conn.base == base) {
                return conn;
            }
        }
        return null;
    }
    
    // Signal Component methods
    @Override
    public World getStructureWorld() {
        return getWorld();
    }
    
    @Override
    public int getBandwidth() {
        return 4;
    }
    
    @Override
    public SignalNetwork getNetwork() {
        // We don't have our own network - we're part of cable networks
        // hasNetwork() will return false, which is correct
        return null;
    }
    
    @Override
    public void setNetwork(SignalNetwork network) {
        // We don't track our own network - we register directly in cable networks
        // This is called when cableNetwork.add(this) is called, but we ignore it
        // because we're already registered in that cable's network as an input
    }
    
    @Override
    public Iterator<ISignalStructureBase> connections() {
        return new Iterator<ISignalStructureBase>() {
            int idx = 0;
            
            @Override
            public boolean hasNext() {
                return idx < cables.size();
            }
            
            @Override
            public ISignalStructureBase next() {
                return cables.get(idx++).base;
            }
        };
    }
    
    @Override
    public boolean canConnect(EnumFacing facing) {
        return true;
    }
    
    @Override
    public boolean connect(EnumFacing facing, ISignalStructureBase base, LittleGridContext context, int distance, boolean oneSidedRenderer) {
        if (findCable(base) != null) {
            return false;
        }
        
        // DEBUG: Log connection
        SignalNetwork cableNetwork = base.hasNetwork() ? base.getNetwork() : null;
        System.out.println("[LT-OC-Addon] Connecting cable from " + facing + ", network=" + cableNetwork);
        
        // CRITICAL: Register ourselves in the cable's network as an input
        // This ensures we receive updateState() calls when the network changes
        if (cableNetwork != null) {
            cableNetwork.add(this);
            System.out.println("[LT-OC-Addon] Registered in network " + cableNetwork + " as input");
        }
        
        // Don't store network reference - it can change after merge!
        CableConnection conn = new CableConnection(base, facing);
        cables.add(conn);
        
        return true;
    }
    
    @Override
    public void disconnect(EnumFacing facing, ISignalStructureBase base) {
        CableConnection conn = findCable(base);
        if (conn != null) {
            // Remove ourselves from the cable's network
            SignalNetwork network = conn.getNetwork();
            if (network != null) {
                network.remove(this);
                System.out.println("[LT-OC-Addon] Removed from network " + network);
            }
            cables.remove(conn);
        }
    }
    
    @Override
    public SignalComponentType getType() {
        return SignalComponentType.INPUT; // Only receive, don't transmit to network
    }
    
    @Override
    public void updateState(boolean[] state) {
        // Called by the network when any output (plate, button, etc.) changes
        // The network already OR'd all output states together
        // Just accept the state from the network!
        if (!BooleanUtils.equals(state, combinedState)) {
            BooleanUtils.set(combinedState, state);
            int signalValue = BooleanUtils.toNumber(state);
            System.out.println("[LT-OC-Addon] updateState received: " + signalValue);
            
            // Send event to OpenComputers
            if (node != null && node.network() != null) {
                node.sendToReachable("signal_changed", signalValue);
            }
            
            changed();
        }
    }
    
    @Override
    public void changed() {
        IBlockState state = world.getBlockState(pos);
        for (EnumFacing facing : EnumFacing.VALUES) {
            world.neighborChanged(pos.offset(facing), state.getBlock(), pos);
        }
    }
    
    @Override
    public boolean[] getState() {
        return combinedState;
    }
    
    @Override
    public LittleStructure getStructure() {
        return null;
    }
    
    @Override
    public int getId() {
        return 0;
    }
    
    @Override
    public int getColor() {
        return -1;
    }
    
    @Override
    public void unload(EnumFacing facing, ISignalStructureBase base) {
        disconnect(facing, base);
    }
    
    // OC Callbacks - Combined signal
    @Callback(direct = true, limit = 4)
    public Object[] getSignal(Context context, Arguments args) {
        return new Object[] { BooleanUtils.toNumber(combinedState) };
    }
    
    @Callback(direct = true, limit = 4)
    public Object[] isSignalHigh(Context context, Arguments args) {
        int bit = args.checkInteger(0);
        if (bit < 0 || bit > 3) {
            return new Object[] { false, "Bit must be 0-3" };
        }
        return new Object[] { combinedState[bit] };
    }

    @Callback(direct = true, limit = 4)
    public Object[] getBits(Context context, Arguments args) {
        return new Object[] { combinedState[0], combinedState[1], combinedState[2], combinedState[3] };
    }
    
    // OC Callbacks - Per-cable access
    @Callback(direct = true, limit = 4)
    public Object[] getCableCount(Context context, Arguments args) {
        System.out.println("[LT-OC-Addon] getCableCount: " + cables.size());
        return new Object[] { cables.size() };
    }
    
    @Callback(direct = true, limit = 4)
    public Object[] getCableSignal(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index < 0 || index >= cables.size()) {
            System.out.println("[LT-OC-Addon] ERROR: Invalid cable index " + index + ", count: " + cables.size());
            return new Object[] { null, "Invalid cable index" };
        }
        
        CableConnection conn = cables.get(index);
        int signal = BooleanUtils.toNumber(conn.lastState);
        
        System.out.println("[LT-OC-Addon] getCableSignal(" + index + ") from " + conn.facing + ": " + signal);
        return new Object[] { signal };
    }
    
    @Callback(direct = true, limit = 4)
    public Object[] getCableBits(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index < 0 || index >= cables.size()) {
            return new Object[] { null, null, null, null, "Invalid cable index" };
        }
        
        CableConnection conn = cables.get(index);
        return new Object[] { conn.lastState[0], conn.lastState[1], conn.lastState[2], conn.lastState[3] };
    }
    
    @Callback(direct = true, limit = 4)
    public Object[] getCableFacing(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index < 0 || index >= cables.size()) {
            return new Object[] { null, "Invalid cable index" };
        }
        
        CableConnection conn = cables.get(index);
        return new Object[] { conn.facing.getName() };
    }
    
    @Callback(direct = true, limit = 4)
    public Object[] getAllCables(Context context, Arguments args) {
        Map<Integer, Map<String, Object>> result = new HashMap<>();
        
        for (int i = 0; i < cables.size(); i++) {
            CableConnection conn = cables.get(i);
            Map<String, Object> cable = new HashMap<>();
            cable.put("index", i);
            cable.put("facing", conn.facing.getName());
            cable.put("signal", BooleanUtils.toNumber(conn.lastState));
            cable.put("bit0", conn.lastState[0]);
            cable.put("bit1", conn.lastState[1]);
            cable.put("bit2", conn.lastState[2]);
            cable.put("bit3", conn.lastState[3]);
            result.put(i, cable);
        }
        
        return new Object[] { result };
    }
    
    @Callback(direct = true, limit = 4)
    public Object[] debugInfo(Context context, Arguments args) {
        Map<String, Object> result = new HashMap<>();
        result.put("cableCount", cables.size());
        result.put("combinedSignal", BooleanUtils.toNumber(combinedState));
        
        List<Map<String, Object>> cableList = new ArrayList<>();
        for (int i = 0; i < cables.size(); i++) {
            CableConnection conn = cables.get(i);
            Map<String, Object> info = new HashMap<>();
            info.put("index", i);
            info.put("facing", conn.facing.getName());
            info.put("hasNetwork", conn.getNetwork() != null);
            info.put("signal", BooleanUtils.toNumber(conn.lastState));
            cableList.add(info);
        }
        result.put("cables", cableList);
        
        return new Object[] { result };
    }
}
