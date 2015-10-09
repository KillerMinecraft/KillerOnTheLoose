package com.ftwinston.KillerMinecraft.Modules.KillerOnTheLoose;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.SpawnEgg;

import com.ftwinston.KillerMinecraft.GameMode;
import com.ftwinston.KillerMinecraft.GameModePlugin;

public class Plugin extends GameModePlugin
{
	@Override
	public Material getMenuIcon() { return Material.EYE_OF_ENDER; }
	
	@Override
	public String[] getDescriptionText() { return new String[] {"Players cooperate to retrieve an", "item from the nether. One player", "(the killer) is selected to try and", "stop the others. The killer's identity", "is secret, and the game won't", "end if they die."}; }
	
	@Override
	public GameMode createInstance()
	{
		return new KillerOnTheLoose();
	}

	@Override
	protected ArrayList<Recipe> createCustomRecipes()
	{
		ArrayList<Recipe> recipes = new ArrayList<Recipe>();
		ShapedRecipe shaped; ShapelessRecipe shapeless;
		
		// add "simplified" dispenser recipe: replace bow with sapling (of any sort) 
		shaped = new ShapedRecipe(new ItemStack(Material.DISPENSER, 1));
		shaped.shape(new String[] { "AAA", "ABA", "ACA" });
		shaped.setIngredient('A', Material.COBBLESTONE);
		shaped.setIngredient('B', Material.SAPLING);
		shaped.setIngredient('C', Material.REDSTONE);
		recipes.add(shaped);
		
		// eye of ender recipe for use in finding nether fortresses. Replacing blaze powder with spider eye!
		shapeless = new ShapelessRecipe(new ItemStack(Material.EYE_OF_ENDER, 1));
		shapeless.addIngredient(Material.ENDER_PEARL);
		shapeless.addIngredient(Material.SPIDER_EYE);
		recipes.add(shapeless);
		
		// add recipes to create monster eggs using an iron ingot and a drop from that particular monster
		ItemStack stack = new ItemStack(Material.MONSTER_EGG, 1);
		SpawnEgg data = (SpawnEgg)stack.getData();
		data.setSpawnedType(EntityType.SPIDER);
		stack.setData(data);
		
		shapeless = new ShapelessRecipe(stack);
		shapeless.addIngredient(Material.STRING);
		shapeless.addIngredient(Material.IRON_INGOT);
		recipes.add(shapeless);
		
		stack = new ItemStack(Material.MONSTER_EGG, 1);
		data = (SpawnEgg)stack.getData();
		data.setSpawnedType(EntityType.ZOMBIE);
		stack.setData(data);
		
		shapeless = new ShapelessRecipe(stack);
		shapeless.addIngredient(Material.ROTTEN_FLESH);
		shapeless.addIngredient(Material.IRON_INGOT);
		recipes.add(shapeless);
		
		stack = new ItemStack(Material.MONSTER_EGG, 1);
		data = (SpawnEgg)stack.getData();
		data.setSpawnedType(EntityType.CREEPER);
		stack.setData(data);
		
		shapeless = new ShapelessRecipe(stack);
		shapeless.addIngredient(Material.SULPHUR);
		shapeless.addIngredient(Material.IRON_INGOT);
		recipes.add(shapeless);
		
		stack = new ItemStack(Material.MONSTER_EGG, 1);
		data = (SpawnEgg)stack.getData();
		data.setSpawnedType(EntityType.SKELETON);
		stack.setData(data);
		
		shapeless = new ShapelessRecipe(stack);
		shapeless.addIngredient(Material.BONE);
		shapeless.addIngredient(Material.IRON_INGOT);
		recipes.add(shapeless);
		
		stack = new ItemStack(Material.MONSTER_EGG, 1);
		data = (SpawnEgg)stack.getData();
		data.setSpawnedType(EntityType.SLIME);
		stack.setData(data);
		
		shapeless = new ShapelessRecipe(stack);
		shapeless.addIngredient(Material.SLIME_BALL);
		shapeless.addIngredient(Material.IRON_INGOT);
		recipes.add(shapeless);
		
		return recipes;
	}
}