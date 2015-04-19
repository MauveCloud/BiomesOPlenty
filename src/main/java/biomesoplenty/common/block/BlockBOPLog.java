/*******************************************************************************
 * Copyright 2014, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/

package biomesoplenty.common.block;

import java.util.HashMap;
import java.util.Map;

import biomesoplenty.api.block.BOPWoodEnums.AllWoods;
import biomesoplenty.api.block.BOPWoodEnums;
import biomesoplenty.api.block.IBOPBlock;
import biomesoplenty.common.item.ItemBOPBlock;
import net.minecraft.block.BlockLog;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public abstract class BlockBOPLog extends BlockLog implements IBOPBlock
{
    
    // setup paged variant property
    
    // LOG_AXIS requires two bits, so we have 2 bits left for the VARIANT which means we can have four per instance
    public static final int VARIANTS_PER_PAGE = 4;
    // child classes must implement to define their page number
    abstract public int getPageNum();
    // fetch the variant property for a given page
    public static PropertyEnum getVariantProperty(int pageNum)
    {
        return BOPWoodEnums.getVariantProperty(pageNum, VARIANTS_PER_PAGE);
    }
    // fetch the current instance's variant property
    public PropertyEnum getMyVariantProperty()
    {
        return getVariantProperty(getPageNum());
    }
    // get the meta bits from the variant
    public int metaFromVariant(AllWoods wood)
    {
        return wood.ordinal() % VARIANTS_PER_PAGE;
    }
    // get the variant from meta bits (safely)
    public AllWoods variantFromMeta(int meta)
    {
        int i = Math.max(0, Math.min(meta + (this.getPageNum() * VARIANTS_PER_PAGE), AllWoods.values().length));
        return AllWoods.values()[i];
    }
    
    // store reference to each created instance, indexed by page num, so that later we can look up the right BlockBOPLog instance for a particular variant
    private static Map<Integer, BlockBOPLog> instances = new HashMap<Integer, BlockBOPLog>();
    // get the BlockFoliage instance for the given variant
    public static BlockBOPLog getVariantBlock(AllWoods wood)
    {
        int pageNum = wood.ordinal() / VARIANTS_PER_PAGE;
        BlockBOPLog block = instances.get(pageNum);
        if (block == null) {throw new IllegalArgumentException("No BlockBOPLog instance created yet for page "+pageNum);}
        return block;
    }
    // get the default block state for the given variant
    public static IBlockState getVariantState(AllWoods wood)
    {
        BlockBOPLog block = getVariantBlock(wood);
        return block.getDefaultState().withProperty(block.getMyVariantProperty() , wood);
    }
    // get the item representation of the given variant
    public static ItemStack getVariantItem(AllWoods wood, int howMany)
    {
        return new ItemStack(getVariantBlock(wood), howMany, getVariantBlock(wood).getMetaFromState(getVariantState(wood)));
    }
    public static ItemStack getVariantItem(AllWoods wood)
    {
        return getVariantItem(wood, 1);
    }
    
    
    @Override
    protected BlockState createBlockState() {return new BlockState(this, new IProperty[] { LOG_AXIS, getMyVariantProperty() });}
    
    
    // implement IBOPBlock
    @Override
    public Class<? extends ItemBlock> getItemClass() { return ItemBOPBlock.class; }
    @Override
    public int getItemRenderColor(IBlockState state, int tintIndex) { return this.getRenderColor(state); }
    @Override
    public IProperty[] getPresetProperties() { return new IProperty[] {getMyVariantProperty()}; }
    @Override
    public IProperty[] getNonRenderingProperties() { return null; }
    @Override
    public String getStateName(IBlockState state)
    {
        AllWoods wood = (AllWoods) state.getValue(getMyVariantProperty());
        switch (wood)
        {
            case GIANT_FLOWER:
                return wood.getName() + "_stem";
            default:
                return wood.getName() + "_log";
        }
    }

    
    public BlockBOPLog()
    {
        super();
        // save a reference to this instance so that later we can look up the right BlockBOPLog instance for a particular variant
        instances.put(this.getPageNum(), this);
        this.setDefaultState(this.blockState.getBaseState().withProperty(LOG_AXIS, BlockLog.EnumAxis.Y));
        this.setHarvestLevel("axe", 0);
    }
    
    // map from state to meta and vice verca - use high 2 bits for LOG_AXIS, low 2 bits for VARIANT
    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(LOG_AXIS, BlockLog.EnumAxis.values()[meta >> 2]).withProperty(getMyVariantProperty(), variantFromMeta(meta & 3));
    }
    @Override
    public int getMetaFromState(IBlockState state)
    {
        return ((BlockLog.EnumAxis) state.getValue(LOG_AXIS)).ordinal() * 4 + metaFromVariant((AllWoods) state.getValue(getMyVariantProperty()));
    }

    // discard the axis information - otherwise logs facing different directions would not stack together
    @Override
    public int damageDropped(IBlockState state)
    {
        return this.getMetaFromState(state.withProperty(LOG_AXIS, BlockLog.EnumAxis.Y));
    }
    
    
}
