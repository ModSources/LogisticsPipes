/** 
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package net.minecraft.src.buildcraft.krapht.gui;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.IInventory;
import buildcraft.core.CoreProxy;
import net.minecraft.src.buildcraft.krapht.GuiIDs;
import net.minecraft.src.buildcraft.krapht.logic.LogicSupplier;
import net.minecraft.src.buildcraft.krapht.network.NetworkConstants;
import net.minecraft.src.buildcraft.krapht.network.PacketCoordinates;
import net.minecraft.src.buildcraft.logisticspipes.modules.IGuiIDHandlerProvider;
import net.minecraft.src.krapht.gui.DummyContainer;

import org.lwjgl.opengl.GL11;

public class GuiSupplierPipe extends GuiContainer implements IGuiIDHandlerProvider {
	
	private IInventory playerInventory;
	private IInventory dummyInventory;
	private LogicSupplier logic; 
	
	public GuiSupplierPipe(IInventory playerInventory, IInventory dummyInventory, LogicSupplier logic) {
		super(null);
		
		
		DummyContainer dummy = new DummyContainer(playerInventory, dummyInventory);
		dummy.addNormalSlotsForPlayerInventory(18, 97);
		
		int xOffset = 72;
		int yOffset = 18;
		
		for (int row = 0; row < 3; row++){
			for (int column = 0; column < 3; column++){
				dummy.addDummySlot(column + row * 3, xOffset + column * 18, yOffset + row * 18);					
			}
		}
		this.inventorySlots = dummy; 

		this.playerInventory = playerInventory;
		this.dummyInventory = dummyInventory;
		this.logic = logic;
		xSize = 194;
		ySize = 186;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer() {
		fontRenderer.drawString(dummyInventory.getInvName(), xSize / 2 - fontRenderer.getStringWidth(dummyInventory.getInvName())/2, 6, 0x404040);
		fontRenderer.drawString("Inventory", 18, ySize - 102, 0x404040);
		fontRenderer.drawString("Partial requests:", xSize - 140, ySize - 112, 0x404040);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {
		int i = mc.renderEngine.getTexture("/logisticspipes/gui/supplier.png");
				
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(i);
		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		drawTexturedModalRect(j, k, 0, 0, xSize, ySize);
	}
	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();
       controlList.clear();
       controlList.add(new GuiButton(0, width / 2 + 45, height / 2 - 25, 30, 20, logic.isRequestingPartials() ? "Yes" : "No"));

	}

	@Override
	protected void actionPerformed(GuiButton guibutton) {
		// TODO Auto-generated method stub
		if (guibutton.id == 0){
			logic.setRequestingPartials(!logic.isRequestingPartials());
			((GuiButton)controlList.get(0)).displayString = logic.isRequestingPartials() ? "Yes" : "No";
			if(CoreProxy.isRemote()) {
				CoreProxy.sendToServer(new PacketCoordinates(NetworkConstants.SUPPLIER_PIPE_MODE_CHANGE, logic.xCoord, logic.yCoord, logic.zCoord).getPacket());
			}
		}
		super.actionPerformed(guibutton);
		
	}
	
	public void refreshMode() {
		((GuiButton)controlList.get(0)).displayString = logic.isRequestingPartials() ? "Yes" : "No";
	}
	
	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		logic.pause = false;
		
	}

	@Override
	public int getGuiID() {
		return GuiIDs.GUI_SupplierPipe_ID;
	}

}
