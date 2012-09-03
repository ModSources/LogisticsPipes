package logisticspipes.pipes;

import java.util.LinkedList;
import java.util.UUID;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

import logisticspipes.LogisticsPipes;
import logisticspipes.config.Textures;
import logisticspipes.interfaces.ILogisticsModule;
import logisticspipes.interfaces.routing.IDirectRoutingConnection;
import logisticspipes.logic.LogicInvSysConnection;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.logisticspipes.SidedInventoryAdapter;
import logisticspipes.main.CoreRoutedPipe;
import logisticspipes.main.GuiIDs;
import logisticspipes.main.RoutedPipe;
import logisticspipes.main.SimpleServiceLocator;
import logisticspipes.modules.ModuleExtractor;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.network.NetworkConstants;
import logisticspipes.network.packets.PacketPipeInteger;
import logisticspipes.proxy.MainProxy;
import logisticspipes.transport.TransportInvConnection;
import logisticspipes.utils.ItemIdentifier;
import logisticspipes.utils.ItemIdentifierStack;
import logisticspipes.utils.Pair;
import logisticspipes.utils.SimpleInventory;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraftforge.common.ISidedInventory;
import buildcraft.BuildCraftCore;
import buildcraft.api.core.Orientations;
import buildcraft.api.core.Position;
import buildcraft.transport.EntityData;

public class PipeItemsInvSysConnector extends RoutedPipe implements IDirectRoutingConnection {
	
	private boolean init = false;
	private LinkedList<Pair<ItemIdentifier,Pair<UUID,UUID>>> destination = new LinkedList<Pair<ItemIdentifier,Pair<UUID,UUID>>>();
	public SimpleInventory inv = new SimpleInventory(1, "Freq. card", 1);
	public int resistance;
	
	public PipeItemsInvSysConnector(int itemID) {
		super(new TransportInvConnection(), new LogicInvSysConnection(), itemID);
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		if(MainProxy.isClient(worldObj)) return;
		if(!init) {
			if(hasConnectionUUID()) {
				if(!SimpleServiceLocator.connectionManager.addDirectConnection(getConnectionUUID(), getRouter())) {
					dropFreqCard();
				}
				CoreRoutedPipe CRP = SimpleServiceLocator.connectionManager.getConnectedPipe(getRouter());
				if(CRP != null) {
					CRP.refreshRender();
				}
				getRouter().update(true);
				this.refreshRender();
				init = true;
			}
		}
		if(init && !hasConnectionUUID()) {
			init = false;
			CoreRoutedPipe CRP = SimpleServiceLocator.connectionManager.getConnectedPipe(getRouter());
			SimpleServiceLocator.connectionManager.removeDirectConnection(getRouter());
			if(CRP != null) {
				CRP.refreshRender();
			}
		}
		if(destination.size() > 0) {
			checkConnectedInvs();
		}
	}

	private void checkConnectedInvs() {
		for (int i = 0; i < 6; i++)	{
			Position p = new Position(xCoord, yCoord, zCoord, Orientations.values()[i]);
			p.moveForwards(1);
			TileEntity tile = worldObj.getBlockTileEntity((int) p.x, (int) p.y, (int) p.z);
			if(tile instanceof IInventory) {
				IInventory inv = (IInventory) tile;
				if(inv instanceof ISidedInventory) {
					inv = new SidedInventoryAdapter((ISidedInventory)inv, Orientations.values()[i].reverse());
				}
				checkOneConnectedInv(inv,Orientations.values()[i]);
				break;
			}
		}
	}
	
	private void checkOneConnectedInv(IInventory inv, Orientations dir) {
		for(int i=0; i<inv.getSizeInventory();i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if(stack != null) {
				ItemIdentifier ident = ItemIdentifier.get(stack);
				for(Pair<ItemIdentifier,Pair<UUID,UUID>> pair:destination) {
					if(pair.getValue1() == ident) {
						sendStack(stack.splitStack(1),pair.getValue2().getValue1(),pair.getValue2().getValue2(),dir);
						destination.remove(pair);
						if(stack.stackSize <=0 ) {
							inv.setInventorySlotContents(i, null);	
						} else {
							inv.setInventorySlotContents(i, stack);
						}
						break;
					}
				}
			}
		}
	}

	public void sendStack(ItemStack stack, UUID source, UUID destination, Orientations dir) {
		IRoutedItem itemToSend = SimpleServiceLocator.buildCraftProxy.CreateRoutedItem(stack, this.worldObj);
		itemToSend.setSource(source);
		itemToSend.setDestination(destination);
		itemToSend.setTransportMode(TransportMode.Active);
		super.queueRoutedItem(itemToSend, dir);
	}
	
	private UUID getConnectionUUID() {
		if(inv != null) {
			if(inv.getStackInSlot(0) != null) {
				if(inv.getStackInSlot(0).hasTagCompound()) {
					if(inv.getStackInSlot(0).getTagCompound().hasKey("UUID")) {
						return UUID.fromString(inv.getStackInSlot(0).getTagCompound().getString("UUID"));
					}
				}
			}
		}
		return null;
	}
	
	private boolean hasConnectionUUID() {
		if(inv != null) {
			if(inv.getStackInSlot(0) != null) {
				if(inv.getStackInSlot(0).hasTagCompound()) {
					if(inv.getStackInSlot(0).getTagCompound().hasKey("UUID")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void dropFreqCard() {
		if(inv.getStackInSlot(0) == null) return;
		EntityItem item = new EntityItem(worldObj,this.xCoord, this.yCoord, this.zCoord, inv.getStackInSlot(0));
		worldObj.spawnEntityInWorld(item);
		inv.setInventorySlotContents(0, null);
	}
	
	@Override
	public boolean blockActivated(World world, int i, int j, int k,	EntityPlayer entityplayer) {
		if (entityplayer.getCurrentEquippedItem() != null && entityplayer.getCurrentEquippedItem().getItem() == BuildCraftCore.wrenchItem && !(entityplayer.isSneaking())){
			entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_INV_SYS_CONNECTOR, world, i, j, k);
			return true;
		}
		return false;
	}

	@Override
	public void onBlockRemoval() {
		super.onBlockRemoval();
		CoreRoutedPipe CRP = SimpleServiceLocator.connectionManager.getConnectedPipe(getRouter());
		SimpleServiceLocator.connectionManager.removeDirectConnection(getRouter());
		if(CRP != null) {
			CRP.refreshRender();
		}
		dropFreqCard();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		inv.writeToNBT(nbttagcompound, "");
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		inv.readFromNBT(nbttagcompound, "");
	}
	
	private boolean hasRemoteConnection() {
		return hasConnectionUUID() && this.worldObj != null && SimpleServiceLocator.connectionManager.hasDirectConnection(getRouter());
	}
	
	private boolean inventoryConnected() {
		for (int i = 0; i < 6; i++)	{
			Position p = new Position(xCoord, yCoord, zCoord, Orientations.values()[i]);
			p.moveForwards(1);
			TileEntity tile = worldObj.getBlockTileEntity((int) p.x, (int) p.y, (int) p.z);
			if(tile instanceof IInventory) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getCenterTexture() {
		return hasRemoteConnection() ? inventoryConnected() ? Textures.LOGISTICSPIPE_INVSYSCON_CON_TEXTURE : Textures.LOGISTICSPIPE_INVSYSCON_MIS_TEXTURE : Textures.LOGISTICSPIPE_INVSYSCON_DIS_TEXTURE;
	}

	@Override
	public ILogisticsModule getLogisticsModule() {
		return null;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Fast;
	}

	@Override
	public int getConnectionResistance() {
		return resistance;
	}

	@Override
	public void addItem(ItemIdentifier item, UUID sourceId, UUID destinationId) {
		if(item != null && destinationId != null) {
			destination.addLast(new Pair<ItemIdentifier,Pair<UUID,UUID>>(item,new Pair<UUID,UUID>(sourceId,destinationId)));
		}
	}
	
	public boolean isConnectedInv(TileEntity tile) {
		for (int i = 0; i < 6; i++)	{
			Position p = new Position(xCoord, yCoord, zCoord, Orientations.values()[i]);
			p.moveForwards(1);
			TileEntity lTile = worldObj.getBlockTileEntity((int) p.x, (int) p.y, (int) p.z);
			if(lTile instanceof IInventory) {
				if(lTile == tile) {
					return true;
				}
				return false;
			}
		}
		return false;
	}
	
	public void handleItemEnterInv(EntityData data, TileEntity tile) {
		if(isConnectedInv(tile)) {
			if(data.item instanceof IRoutedItem) {
				IRoutedItem routed = (IRoutedItem)data.item;
				if(hasRemoteConnection()) {
					CoreRoutedPipe CRP = SimpleServiceLocator.connectionManager.getConnectedPipe(getRouter());
					if(CRP instanceof IDirectRoutingConnection) {
						IDirectRoutingConnection pipe = (IDirectRoutingConnection) CRP;
						for(int i=0; i < data.item.getItemStack().stackSize;i++) {
							pipe.addItem(ItemIdentifier.get(routed.getItemStack()), routed.getSource(), routed.getDestination());
						}
					}
				}
			}
		}
	}

	public LinkedList<ItemIdentifierStack> getExpectedItems() {
		LinkedList<ItemIdentifierStack> list = new LinkedList<ItemIdentifierStack>();
		for(Pair<ItemIdentifier,Pair<UUID,UUID>> pair:destination) {
			boolean found = false;
			for(ItemIdentifierStack stack:list) {
				if(stack.getItem() == pair.getValue1()) {
					found = true;
					stack.stackSize += 1;
				}
			}
			if(!found) {
				list.add(new ItemIdentifierStack(pair.getValue1(), 1));
			}
		}
		return list;
	}
}