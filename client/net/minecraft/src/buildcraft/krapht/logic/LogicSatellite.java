/**
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package net.minecraft.src.buildcraft.krapht.logic;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.mod_LogisticsPipes;
import buildcraft.core.CoreProxy;
import buildcraft.core.network.TileNetworkData;
import net.minecraft.src.buildcraft.krapht.GuiIDs;
import net.minecraft.src.buildcraft.krapht.network.NetworkConstants;
import net.minecraft.src.buildcraft.krapht.network.PacketCoordinates;
import net.minecraft.src.krapht.SimpleInventory;

public class LogicSatellite extends BaseLogicSatellite {

	/* ** NETWORK FUNCTIONS ** */

	@Override
	public void setNextId() {
		super.setNextId();

		if (CoreProxy.isRemote()) {
			// Using existing BuildCraft packet system
			final PacketCoordinates packet = new PacketCoordinates(NetworkConstants.SATELLITE_PIPE_NEXT, xCoord, yCoord, zCoord);
			CoreProxy.sendToServer(packet.getPacket());
		}
	}

	@Override
	public void setPrevId() {
		super.setPrevId();

		if (CoreProxy.isRemote()) {
			// Using existing BuildCraft packet system
			final PacketCoordinates packet = new PacketCoordinates(NetworkConstants.SATELLITE_PIPE_PREV, xCoord, yCoord, zCoord);
			CoreProxy.sendToServer(packet.getPacket());
		}
	}

	public void setSatelliteId(int satelliteId) {
		this.satelliteId = satelliteId;
	}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		if (!CoreProxy.isRemote()) {
			entityplayer.openGui(mod_LogisticsPipes.instance, GuiIDs.GUI_SatelitePipe_ID, worldObj, xCoord, yCoord, zCoord);
		}
	}
}
