package org.confluence.lib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ConfluenceMagicLib.MODID)
public class ConfluenceMagicLib {
    public static final String MODID = "confluence_magic_lib";
    public static final Logger LOGGER = LoggerFactory.getLogger("Confluence Magic Lib");

    public ConfluenceMagicLib(IEventBus modEventBus, ModContainer modContainer) {

    }
}
