package net.minecraft.src.buildcraft.logisticspipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import buildcraft.core.CoreProxy;

import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTBase;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.NBTTagString;
import net.minecraft.src.buildcraft.logisticspipes.modules.IClientInformationProvider;
import net.minecraft.src.buildcraft.logisticspipes.modules.ILogisticsModule;

public class ItemModuleInformationManager {
	
	private static final List<String> Filter = new ArrayList<String>();
	static {
		Filter.add("moduleInformation");
		Filter.add("informationList");
		Filter.add("Random-Stack-Prevent");
	}
	
	public static void saveInfotmation(ItemStack itemStack, ILogisticsModule module) {
		if(module == null) return;
		NBTTagCompound nbt = new NBTTagCompound();
        module.writeToNBT(nbt, "");
        if(nbt.equals(new NBTTagCompound())) {
        	return;
        }
        if(CoreProxy.isRemote()) {
			 NBTTagList list = new NBTTagList();
			String info1 = "Please reopen the window";
			String info2 = "to see the information.";
    		list.appendTag(new NBTTagString(info1,info1));
    		list.appendTag(new NBTTagString(info2,info2));
    		if(!itemStack.hasTagCompound()) {
            	itemStack.setTagCompound(new NBTTagCompound());
            }
    		NBTTagCompound stacktag = itemStack.getTagCompound();
    		stacktag.setTag("informationList", list);
    		stacktag.setDouble("Random-Stack-Prevent", new Random().nextDouble());
    		return;
		}
        if(!itemStack.hasTagCompound()) {
        	itemStack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound stacktag = itemStack.getTagCompound();
        stacktag.setCompoundTag("moduleInformation", nbt);
        if(module instanceof IClientInformationProvider) {
        	List<String> information = ((IClientInformationProvider)module).getClientInformation();
        	if(information.size() > 0) {
        		NBTTagList list = new NBTTagList();
        		for(String info:information) {
        			list.appendTag(new NBTTagString(info,info));
        		}
        		stacktag.setTag("informationList", list);
        	}
        }
		stacktag.setDouble("Random-Stack-Prevent", new Random().nextDouble());
	}
	
	public static void readInformation(ItemStack itemStack, ILogisticsModule module) {
		if(module == null) return;
		if(CoreProxy.isRemote()) return;
		if(itemStack.hasTagCompound()) {
			NBTTagCompound nbt = itemStack.getTagCompound();
			if(nbt.hasKey("moduleInformation")) {
				NBTTagCompound moduleInformation = nbt.getCompoundTag("moduleInformation");
				module.readFromNBT(moduleInformation, "");
			}
			
		}
	}
	
	public static void removeInformation(ItemStack itemStack) {
		if(itemStack == null) return;
		if(itemStack.hasTagCompound()) {
			NBTTagCompound nbt = itemStack.getTagCompound();
			Collection collection = nbt.getTags();
			nbt = new NBTTagCompound();
			for(Object obj:collection) {
				if(obj instanceof NBTBase) {
					if(!Filter.contains(((NBTBase)obj).getName())) {
						nbt.setTag(((NBTBase)obj).getName(), ((NBTBase)obj));
					}
				}
			}
			itemStack.setTagCompound(nbt);
		}
	}
}
