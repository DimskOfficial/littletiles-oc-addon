package com.creativemd.littletilesocaddon;

import com.creativemd.littletilesocaddon.common.block.BlockSignalConverterOC;
import com.creativemd.littletilesocaddon.common.tileentity.TESignalConverterOC;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = LittleTilesOCAddon.MODID, version = LittleTilesOCAddon.VERSION, name = LittleTilesOCAddon.NAME, 
    dependencies = "required-after:littletiles;required-after:opencomputers")
@Mod.EventBusSubscriber
public class LittleTilesOCAddon {
    
    public static final String MODID = "littletilesocaddon";
    public static final String VERSION = "1.0.0";
    public static final String NAME = "LittleTiles OC Addon";
    
    public static Block signalConverterOC;
    
    public static CreativeTabs tab = new CreativeTabs("littletilesocaddon") {
        @Override
        public ItemStack createIcon() {
            ItemStack stack = new ItemStack(Item.getItemFromBlock(signalConverterOC));
            return stack;
        }
    };
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Pre-initialization
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Register tile entity
        GameRegistry.registerTileEntity(TESignalConverterOC.class, new ResourceLocation(MODID, "signal_converter_oc"));
    }
    
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        signalConverterOC = new BlockSignalConverterOC();
        event.getRegistry().register(signalConverterOC);
    }
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ItemBlock itemBlock = new ItemBlock(signalConverterOC);
        itemBlock.setRegistryName(signalConverterOC.getRegistryName());
        itemBlock.setCreativeTab(tab);
        event.getRegistry().register(itemBlock);
    }
    
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        Item item = Item.getItemFromBlock(signalConverterOC);
        ModelLoader.setCustomModelResourceLocation(item, 0, 
            new ModelResourceLocation(new ResourceLocation(MODID, "signal_converter_oc"), "inventory"));
    }
}
