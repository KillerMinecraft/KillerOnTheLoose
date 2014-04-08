package com.ftwinston.KillerMinecraft.Modules.KillerOnTheLoose;

import java.util.List;

import com.ftwinston.KillerMinecraft.GameMode;
import com.ftwinston.KillerMinecraft.Helper;
import com.ftwinston.KillerMinecraft.KillerMinecraft;
import com.ftwinston.KillerMinecraft.Option;
import com.ftwinston.KillerMinecraft.PlayerFilter;
import com.ftwinston.KillerMinecraft.Configuration.ChoiceOption;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;
import com.ftwinston.KillerMinecraft.Configuration.ToggleOption;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Material;

public class KillerOnTheLoose extends GameMode
{
	ToggleOption ghastTearVictory, allowCraftingMonsters;
	ChoiceOption<KillerType> killerType;
	
	enum KillerType
	{
		MYSTERY_KILLER,
		INVISIBLE_KILLER,
		CRAZY_KILLER,
	}
	
	Material[] winningItems;

	@Override
	public int getMinPlayers() { return killerType.getValue() == KillerType.MYSTERY_KILLER ? 3 : 2; }
	
	TeamInfo survivors = new TeamInfo() {
		@Override
		public String getName() { return "Survivors"; }
		@Override
		public boolean allowTeamChat() { return killerType.getValue() != KillerType.MYSTERY_KILLER; }
	};
	TeamInfo killer = new TeamInfo() {
		@Override
		public String getName() { return "Killer"; }
		@Override
		public boolean allowTeamChat() { return false; }
	};
	
	public KillerOnTheLoose()
	{
		setTeams(new TeamInfo[] { survivors, killer });
	}
	
	@Override
	public Option[] setupOptions()
	{
		killerType = new ChoiceOption<KillerType>("Killer type");
		killerType.addChoice("Mystery Killer", KillerType.MYSTERY_KILLER, Material.FLINT_AND_STEEL, "No special powers, but", "Killer's identity is", "kept secret");
		killerType.addChoice("Invisible Killer", KillerType.INVISIBLE_KILLER, Material.GLASS, "Killer can't be seen,", "but emits particles. Other", "players get infinity bows.");
		killerType.addChoice("Crazy Killer", KillerType.CRAZY_KILLER, Material.TNT, "Any dirt the Killer", "picks up turns into", "TNT, and their bow fires TNT.");
		
		ghastTearVictory = new ToggleOption("Ghast tear victory", true, "When enabled, friendly players can", "win by returning a ghast tear or", "a blaze rod. When disabled, they", "can only win with a blaze rod.");
		allowCraftingMonsters = new ToggleOption("Allow crafting monsters", true, "Adds recipes for crafting monster", "eggs by combining a common", "monster drop with an iron ingot.");
		
		return new Option[] { killerType, ghastTearVictory, allowCraftingMonsters };
	}

	@Override
	public org.bukkit.scoreboard.Scoreboard createScoreboard()
	{
		// in mystery killer, we don't want players to be able to distinguish the teams
		if ( killerType.getValue() == KillerType.MYSTERY_KILLER )
			return Bukkit.getScoreboardManager().getMainScoreboard();
		
		return super.createScoreboard();
	}
	
	@Override
	public void allocateTeams(List<Player> players)
	{
		// pick a killer immediately, if not on mystery killer.
		if ( killerType.getValue() != KillerType.MYSTERY_KILLER )
		{
			int index = random.nextInt(players.size());
			Player player = players.remove(index);
			setTeam(player, killer);
		}
		
		// put everyone (else) into the survivors
		for ( Player player : players )
			setTeam(player, survivors);	
	}

	@Override
	public String getHelpMessage(int num, TeamInfo team)
	{
		switch ( killerType.getValue() )
		{
		case MYSTERY_KILLER:
			return getHelpMessageMysteryKiler(num, team);
		case INVISIBLE_KILLER:
			return getHelpMessageInvisibleKiler(num, team);
		case CRAZY_KILLER:
			return getHelpMessageCrazyKiler(num, team);
		default:
			return null;
		}
	}

	public String getHelpMessageMysteryKiler(int num, TeamInfo team)
	{
		switch ( num )
		{
			case 0:
				if ( team == killer )
					return "You have been chosen to try and kill everyone else.\nIf there are many players in the game, multiple players will have been chosen.\nNo one else has been told who was chosen.";
				else if ( getPlayers(new PlayerFilter().team(killer)).size() > 0 )
					return "(At least) one player has been chosen to try and kill everyone else.\nIf there are many players in the game, multiple players will be chosen.\nNo one else has been told who they are.";
				else
					return "At the start of the next game day, a player will be chosen to try and kill everyone else.\nIf there are many players in the game, multiple players will be chosen.\nNo one else will be told who they are.";
			case 1:
				if ( team == killer )
					return "As a killer, you win if all the friendly players die. You won't be told who the other killers are.";
				else
					return "The killer(s) win if everyone else dies... so watch your back!";
			case 2:
				String message = "To win, the other players must bring a ";
				
				message += Helper.tidyItemName(winningItems[0]);
				
				if ( winningItems.length > 1 )
				{
					for ( int i=1; i<winningItems.length-1; i++)
						message += ", a " + Helper.tidyItemName(winningItems[i]);
					
					message += " or a " + Helper.tidyItemName(winningItems[winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn.";
				return message;
			case 3:
				return "The other players will not automatically win when all the killers are dead, and additional killers may be assigned to replace dead ones.";

			case 4:
				return "Death messages won't say how someone died, or who killed them.";
			
			case 5:
				if ( team == killer )
					return "If you make a compass, it will point at the nearest player. This won't work for other players.";
				else
					return "If a killer makes a compass, it will point at the nearest player. This won't work for other players.";

			case 6:
				return "Eyes of ender will help you find nether fortresses (to get blaze rods).\nThey can be crafted from an ender pearl and a spider eye.";
				
			case 7:
				return allowCraftingMonsters.isEnabled() ? "Several monster eggs can be crafted by combining one of their dropped items with an iron ingot." : null;
			case 8:
				return allowCraftingMonsters.isEnabled() ? "Dispensers can be crafted using a sapling instead of a bow. These work well with monster eggs." : null;
				
			default:
				return null;
		}
	}
	
	public String getHelpMessageInvisibleKiler(int num, TeamInfo team)
	{
		switch ( num )
		{
			case 0:
				if ( team == killer )
					return "You have been chosen to be the killer, and must kill everyone else.\nYou are invisible, but they know who you are.";
				else
					return "A player has been chosen to be the killer, and must kill everyone else.\nThey are invisible!";
			case 1:
				if ( team == killer )
					return "If they look closely, other players will see the grey particles that you emit.";
				else
					return "If you look closely, you will see the grey particles that the killer emits.";
			case 2:
				if ( team == killer )
					return "Your items and armor will be visible to other players\nYour compass points at the nearest player.";
				else
					return "Items wielded by the killer will be still visible.\nThe killer's compass points at the nearest player.";
			case 3:
				if ( team == killer )
					return "You will briefly become visible when damaged.";
				else
					return "The killer will briefly become visible when damaged.";
			case 4:
				return "The other players get infinity bows.";
			case 5:
				String message = "To win, the other players must kill the killer, or bring a ";
			
				message += Helper.tidyItemName(winningItems[0]);
				
				if ( winningItems.length > 1 )
				{
					for ( int i=1; i<winningItems.length-1; i++)
						message += ", a " + Helper.tidyItemName(winningItems[i]);
					
					message += " or a " + Helper.tidyItemName(winningItems[winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn.";
				return message;
			
			default:
				return null;
		}
	}
	
	public String getHelpMessageCrazyKiler(int num, TeamInfo team)
	{
		switch ( num )
		{
			case 0:
				if ( team == killer )
					return "You have been chosen to be the killer, and must kill everyone else. They know who you are.";
				else
					return "A player has been chosen to be the killer, and must kill everyone else.";
			case 1:
				if ( team == killer )
					return "Every dirt block you pick up will turn into TNT, so have fun with that.";
				else
					return "Every dirt block the killer picks up will turn into TNT, so beware.";
			case 2:
				if ( team == killer )
					return "The other players each start with a sword, so avoid a direct fight.";
				else
					return "The killer doesn't start with a sword, but all the other players do.";
			case 3:
				if ( team == killer )
					return "Your compass will point at the nearest player.";
				else
					return "The killer starts with a compass, which points at the nearest player.";
			case 4:
				if ( team == killer )
					return "You will respawn, but the other players can't.";
				else
					return "The killer can respawn, but the other players can't.";
			case 5:
				String message = "The other players win if the killer dies, or if they bring a ";			
				message += Helper.tidyItemName(winningItems[0]);
				
				if ( winningItems.length > 1 )
				{
					for ( int i=1; i<winningItems.length-1; i++)
						message += ", a " + Helper.tidyItemName(winningItems[i]);
					
					message += " or a " + Helper.tidyItemName(winningItems[winningItems.length-1]);
				}
				
				message += " to the plinth near the spawn.";
				return message;
			case 6:
				if ( team == killer )
					return "You can make buttons and pressure plates with the stone you started with.\nTry to avoid blowing yourself up!";
				else
					return "The killer starts with enough stone and redstone to make plenty buttons, wires and pressure plates.";

			default:
				return null;
		}
	}
	
	@Override
	public boolean allowTeamSelection() { return false; }

	@Override
	public boolean isLocationProtected(Location l, Player p)
	{
		// no protection, except for the plinth
		return plinthLoc != null && l.getWorld() == plinthLoc.getWorld()
	           && l.getX() >= plinthLoc.getBlockX() - 1
	           && l.getX() <= plinthLoc.getBlockX() + 1
	           && l.getZ() >= plinthLoc.getBlockZ() - 1
	           && l.getZ() <= plinthLoc.getBlockZ() + 1;
	} 

	@Override
	public Location getSpawnLocation(Player player)
	{
		Location spawnPoint;
		if ( getTeam(player) == killer )
		{
			// the killer starts a good distance away from the other players
			spawnPoint = Helper.randomizeLocation(getWorld(0).getSpawnLocation(), 64, 0, 64, 96, 0, 96);
		}
		else
			spawnPoint = Helper.randomizeLocation(getWorld(0).getSpawnLocation(), 0, 0, 0, 8, 0, 8);	

		return Helper.getSafeSpawnLocationNear(spawnPoint);
	}

	int allocationProcessID = -1, restoreMessageProcessID = -1;
	Location plinthLoc;
	
	@Override
	public void gameStarted()
	{
		if ( ghastTearVictory.isEnabled() )
			winningItems = new Material[] { Material.BLAZE_ROD, Material.GHAST_TEAR };
		else
			winningItems = new Material[] { Material.BLAZE_ROD };
		
		plinthLoc = generatePlinth(getWorld(0));
		
		restoreMessageProcessID = -1;
		
		if ( killerType.getValue() == KillerType.MYSTERY_KILLER )
		{// check based on the time of day
			allocationProcessID = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
				long lastRun = 0;
				public void run()
				{
					long time = getWorld(0).getTime();
					
					if ( time < lastRun ) // time of day has gone backwards: must be a new day! Allocate the killers
					{
						allocateMysteryKiller();
						getPlugin().getServer().getScheduler().cancelTask(allocationProcessID);
						
						allocationProcessID = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
							public void run()
							{
								allocateMysteryKiller();									
							}
						}, 1800L, 1800L); // check every 90 seconds
					}
					
					lastRun = time;
				}
			}, 600L, 100L); // initial wait: 30s, then check every 5s (still won't try to assign unless it detects a new day starting)
		}
		
		List<Player> killerPlayers = getOnlinePlayers(new PlayerFilter().includeSpectators().team(killer));
		List<Player> survivorPlayers = getOnlinePlayers(new PlayerFilter().includeSpectators().team(survivors));
		float ratio = survivorPlayers.size() / (float)killerPlayers.size();
		
		for ( Player player : killerPlayers )
			prepareKiller(player, killerPlayers.size(), ratio);
		
		if ( killerType.getValue() != KillerType.MYSTERY_KILLER )
		{
			if ( killerPlayers.size() == 0 )
				return;
			
			for ( Player player : survivorPlayers )
				prepareSurvivor(player, killerPlayers, ratio);
		}
	}
	
	static Location generatePlinth(World world)
	{
		Location loc = world.getSpawnLocation().add(20, 0, 0);
		int x = loc.getBlockX(), z = loc.getBlockZ();
		
		int highestGround = world.getSeaLevel();		
		for ( int testX = x-1; testX <= x+1; testX++ )
			for ( int testZ = z-1; testZ <= z+1; testZ++ )
			{
				int groundY = Helper.getHighestYIgnoring(world, testX, testZ, world.getSeaLevel(), Material.LOG, Material.LEAVES, Material.HUGE_MUSHROOM_1, Material.HUGE_MUSHROOM_2);
				
				if ( groundY > highestGround )
					highestGround = groundY;
			}
		
		int plinthPeakHeight = highestGround + 12, spaceBetweenPlinthAndGlowstone = 4;
		
		// a 3x3 column from bedrock to the plinth height
		for ( int y = 0; y < plinthPeakHeight; y++ )
			for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(Material.BEDROCK);
				}
		
		// with one block sticking up from it
		int y = plinthPeakHeight;
		for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(ix == x && iz == z ? Material.BEDROCK : Material.AIR);
				}
		
		// that has a pressure plate on it
		y = plinthPeakHeight + 1;
		Location plinthLoc = new Location(world, x, y, z);
		for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(ix == x && iz == z ? Material.STONE_PLATE : Material.AIR);
				}
				
		// then a space
		for ( y = plinthPeakHeight + 2; y <= plinthPeakHeight + spaceBetweenPlinthAndGlowstone; y++ )
			for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(Material.AIR);
				}
		
		// and then a 1x1 pillar of glowstone, up to max height
		for ( y = plinthPeakHeight + spaceBetweenPlinthAndGlowstone + 1; y < world.getMaxHeight(); y++ )
			for ( int ix = x - 1; ix < x + 2; ix++ )
				for ( int iz = z - 1; iz < z + 2; iz++ )
				{
					Block b = world.getBlockAt(ix, y, iz);
					b.setType(ix == x && iz == z ? Material.GLOWSTONE : Material.AIR);
				}
		
		return plinthLoc;
	}

	private void allocateMysteryKiller()
	{
		int numAlive = getOnlinePlayers(new PlayerFilter()).size();
		int numAliveKillers = getOnlinePlayers(new PlayerFilter().team(killer)).size();
		
		if ( numAlive == 0 )
			return;
		
		// 1-5 players should have 1 killer. 6-11 should have 2. 12-17 should have 3. 18-23 should have 4. 
		int targetNumKillers = numAlive / 6 + 1;
		int numToAdd = targetNumKillers - numAliveKillers;
		
		if ( numToAdd <= 0 )
			return;

		// pick players
		List<Player> players = getOnlinePlayers(new PlayerFilter().team(survivors));
		float numFriendliesPerKiller = (float)(players.size() - numToAdd) / (float)(numAliveKillers + numToAdd);
		for ( int i=0; i<numToAdd; i++ )
		{
			Player player = Helper.selectRandom(players);
			if ( player == null )
			{
				broadcastMessage("Error selecting player to allocate as the killer");
				return;
			}

			setTeam(player,  killer);
			prepareKiller(player, numToAdd, numFriendliesPerKiller);
		}
		
		players = getOnlinePlayers(new PlayerFilter().includeSpectators().team(survivors)); // some have moved to the killer team now, so re-select
		String message = ChatColor.YELLOW + (numToAdd == 1 ? "A killer has been allocated. You are not the killer!" : numToAdd + " killers have been allocated. You are not a killer!"); 
				
		for ( Player player : players )
			player.sendMessage(message);
	}
	
	private void prepareSurvivor(Player player, List<Player> killers, float numSurvivorsPerKiller)
	{
		PlayerInventory inv = player.getInventory();
		
		switch ( killerType.getValue() )
		{
		case INVISIBLE_KILLER:
			ItemStack stack = new ItemStack(Material.BOW, 1);
			stack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
			inv.addItem(stack);
			inv.addItem(new ItemStack(Material.ARROW, 1)); // you need 1 arrow for the infinity bow
			break;
			
		case CRAZY_KILLER:
			inv.addItem(new ItemStack(Material.IRON_SWORD, 1));
			break;
		
		case MYSTERY_KILLER:
			return;
		}
		
		announceKillers(player, killers);
	}
	
	private void announceKillers(Player player, List<Player> killers)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.RED);
		
		if ( killers.size() == 1 )
		{
			sb.append(killers.get(0).getName());
			sb.append(" is the killer!");
		}
		else if ( killers.size() == 0 )
			sb.append("There are no killers for some reason!");
		else
		{
			for ( int i=0; i<killers.size()-1; i++ )
			{
				if ( i > 0 )
					sb.append(", ");
				sb.append(killers.get(i).getName());
			}
			sb.append(" and ");
			sb.append(killers.get(killers.size()-1).getName());
			sb.append(" are the killers!");
		}
		
		sb.append("\n");
		sb.append(ChatColor.RESET);
		sb.append("Use the /team command to chat without them seeing your messages");
		
		player.sendMessage(sb.toString());
	}
	
	private void prepareKiller(Player player, int numKillersAllocated, float numSurvivorsPerKiller)
	{
		String message = ChatColor.YELLOW + "A killer has been allocated. " + ChatColor.RED;
		if ( numKillersAllocated == 1 )
			message += "You are the killer!\n" + ChatColor.RESET;
		else
			message += "You are a killer!" + ChatColor.RESET + " You are one of " + numKillersAllocated + " killers.\n";
		
		
		switch ( killerType.getValue() )
		{
		case MYSTERY_KILLER:
			message += "Try to kill the other players without them working out who's after them.";
			giveMysteryKillerItems(player.getInventory(), numSurvivorsPerKiller);
			break;
		case INVISIBLE_KILLER:
			message += "You are invisible.";
			giveInvisibleKillerItems(player.getInventory(), numSurvivorsPerKiller);
			setVisibility(player, false);
			break;
		case CRAZY_KILLER:
			message += "Every dirt block you pick up will turn into TNT...";
			giveCrazyKillerItems(player.getInventory(), numSurvivorsPerKiller);
			break;
		}
		
		player.sendMessage(message);
	}

	private void giveInvisibleKillerItems(PlayerInventory inv, float numSurvivorsPerKiller)
	{
		inv.addItem(new ItemStack(Material.COMPASS, 1));
		inv.addItem(new ItemStack(Material.COOKED_BEEF, 10));
	}

	private void giveMysteryKillerItems(PlayerInventory inv, float numFriendliesPerKiller)
	{
		if ( numFriendliesPerKiller >= 2 )
			inv.addItem(new ItemStack(Material.STONE, 6));
		else
			return;
		
		if ( numFriendliesPerKiller >= 3 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 1), new ItemStack(Material.REDSTONE, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 4 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.SULPHUR, 1));
		else
			return;
		
		if ( numFriendliesPerKiller >= 5 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 1), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.ARROW, 3));
		else
			return;
		
		if ( numFriendliesPerKiller >= 6 )
			inv.addItem(new ItemStack(Material.MONSTER_EGG, 1, (short)50), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.SULPHUR, 1), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 7 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.SULPHUR, 1), new ItemStack(Material.ARROW, 2));
			
			if ( numFriendliesPerKiller < 11 )
				inv.addItem(new ItemStack(Material.IRON_PICKAXE, 1)); // at 11 friendlies, they'll get a diamond pick instead
		}
		else
			return;
		
		if ( numFriendliesPerKiller >= 8 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.BOW, 1), new ItemStack(Material.ARROW, 3));
		else
			return;
		
		if ( numFriendliesPerKiller >= 9 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.MONSTER_EGGS, 4, (short)0), new ItemStack(Material.STONE, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 10 )
			inv.addItem(new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.MONSTER_EGG, 1, (short)50), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 11 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.SULPHUR, 1));
			
			if ( numFriendliesPerKiller < 18 )
				inv.addItem(new ItemStack(Material.DIAMOND_PICKAXE, 1)); // at 18 friendlies, they get an enchanted version
		}
		else
			return;
		
		if ( numFriendliesPerKiller >= 12 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.STONE, 2), new ItemStack(Material.SULPHUR, 1));
		else
			return;
		
		if ( numFriendliesPerKiller >= 13 )
			inv.addItem(new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.MONSTER_EGGS, 2, (short)0), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 14 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.MONSTER_EGGS, 1, (short)0), new ItemStack(Material.REDSTONE, 2), new ItemStack(Material.STONE, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 15 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.MONSTER_EGGS, 1, (short)0), new ItemStack(Material.PISTON_STICKY_BASE, 3));
		else
			return;
		
		if ( numFriendliesPerKiller >= 16 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 1), new ItemStack(Material.SULPHUR, 5));
		else
			return;
		
		if ( numFriendliesPerKiller >= 17 )
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 1), new ItemStack(Material.MONSTER_EGG, 1, (short)50), new ItemStack(Material.ARROW, 2));
		else
			return;
		
		if ( numFriendliesPerKiller >= 18 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2));
			if ( numFriendliesPerKiller == 18 )
			{
				ItemStack stack = new ItemStack(Material.DIAMOND_PICKAXE, 1);
				stack.addEnchantment(Enchantment.DIG_SPEED, 1);
				inv.addItem(stack);
			}
		}
		else
			return;
		
		if ( numFriendliesPerKiller >= 19 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2));
			if ( numFriendliesPerKiller == 19 )
			{
				ItemStack stack = new ItemStack(Material.DIAMOND_PICKAXE, 1);
				stack.addEnchantment(Enchantment.DIG_SPEED, 2);
				inv.addItem(stack);
			}
		}
		else
			return;
		
		if ( numFriendliesPerKiller >= 20 )
		{
			inv.addItem(new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.DIAMOND, 2));
		
			ItemStack stack = new ItemStack(Material.DIAMOND_PICKAXE, 1);
			stack.addEnchantment(Enchantment.DIG_SPEED, 3);
			inv.addItem(stack);
		}

		return;
	}
	
	private void giveCrazyKillerItems(PlayerInventory inv, float numFriendliesPerKiller)
	{
		inv.addItem(new ItemStack(Material.DIAMOND_PICKAXE, 1));
		inv.addItem(new ItemStack(Material.COMPASS, 1));
		inv.addItem(new ItemStack(Material.COOKED_BEEF, 10));
		
		inv.addItem(new ItemStack(Material.REDSTONE, 64));
		inv.addItem(new ItemStack(Material.STONE, 64));
		
		inv.addItem(new ItemStack(Material.STRING, 32));
		inv.addItem(new ItemStack(Material.DIRT, 16));
		
		// give them one swiftness potion for each player on the other team
		// 8290 = swiftness 2 extended, 8226 = swiftness 2, 8194 = swiftness, 8258 = swiftness extended
		inv.addItem(new ItemStack(Material.POTION, (int)numFriendliesPerKiller, (short)8226));
	}
	
	@Override
	public void gameFinished()
	{
		// stop our scheduled processes
		if ( allocationProcessID != -1 )
		{
			getPlugin().getServer().getScheduler().cancelTask(allocationProcessID);
			allocationProcessID = -1;
		}
		
		if ( restoreMessageProcessID != -1 )
		{
			getPlugin().getServer().getScheduler().cancelTask(restoreMessageProcessID);
			restoreMessageProcessID = -1;
		}
		
		if ( killerType.getValue() == KillerType.MYSTERY_KILLER )
		{
			// announce who the killer(s) was/were
			List<OfflinePlayer> killers = getPlayers(new PlayerFilter().includeSpectators().team(killer));
			String message = killers.size() == 1 ? "The killer was: " : "The killers were:\n";
			
			for ( OfflinePlayer killer : killers )
				message += killer.getName() + "\n";
					
			broadcastMessage(message);
		}
	}
	
	@Override
	public void playerJoinedLate(Player player)
	{
		setTeam(player,  survivors);
		
		float numSurvivors = getOnlinePlayers(new PlayerFilter().includeSpectators().team(survivors)).size();
		List<Player> killers = getOnlinePlayers(new PlayerFilter().includeSpectators().team(killer));
		
		prepareSurvivor(player, killers, numSurvivors/killers.size());
	}
	
	@Override
	public void playerReconnected(Player player)
	{
		if ( getTeam(player) == killer ) // inform them that they're still a killer
			player.sendMessage("Welcome back. " + ChatColor.RED + "You are still " + (getPlayers(new PlayerFilter().includeSpectators().team(killer)).size() > 1 ? "a" : "the" ) + " killer!");
		else if ( getTeam(player) == survivors )
			player.sendMessage("Welcome back. You are not the killer, and you're still alive.");
	};
	
	@Override
	public void playerQuit(OfflinePlayer player)
	{
		playerKilledOrQuit(player);
	}
	
	void playerKilledOrQuit(OfflinePlayer player)
	{
		final String playerName = player.getName();
		
		getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
			public void run()
			{
				OfflinePlayer player2 = getPlugin().getServer().getPlayerExact(playerName);
				
				if ( killerType.getValue() == KillerType.MYSTERY_KILLER )
				{
					if ( hasGameFinished() || getOnlinePlayers(new PlayerFilter().includeSpectators().team(killer)).size() == 0 )
						return;
					
					int numFriendlies = getOnlinePlayers(new PlayerFilter().team(survivors).exclude(player2)).size();
					int numKillers = getOnlinePlayers(new PlayerFilter().team(killer)).size();
					
					if ( numFriendlies > 0 )
					{
						// if only one person left (and they're not the killer), tell people they can /vote if they want to start a new game
						if ( numFriendlies == 1 && numKillers == 0 )
							broadcastMessage(ChatColor.YELLOW + "There's only one player left, and they're not the killer. The friendly players win!");
						else
							return; // as long as there are survivors, the game isn't over
					}
					
					else if ( numKillers > 0 )
						broadcastMessage(ChatColor.YELLOW + "All the friendly players died - the killer wins!");
					else
						broadcastMessage(ChatColor.YELLOW + "Everybody died - nobody wins!");

					finishGame();
				}
				else
				{
					if ( hasGameFinished() )
						return;
					
					TeamInfo team = getTeam(player2);
					int numSurvivorsOnTeam = getOnlinePlayers(new PlayerFilter().team(team)).size();
					
					if ( numSurvivorsOnTeam > 0 )
						return; // this players still has living allies, so this doesn't end the game
					
					int numSurvivorsTotal = getOnlinePlayers().size();
					if ( numSurvivorsTotal == 0 )
						broadcastMessage(ChatColor.YELLOW + "Everybody died - nobody wins!");
					else if ( team == killer )
						broadcastMessage(ChatColor.YELLOW + "The killer died - the friendly players win!");
					else
						broadcastMessage(ChatColor.YELLOW + "All the friendly players died - the killer wins!");

					finishGame();
				}
			}
		});
	}

	@Override
	public Location getCompassTarget(Player player)
	{
		TeamInfo team = getTeam(player);
		if ( team == killer )
			return Helper.getNearestPlayerTo(player, getOnlinePlayers(new PlayerFilter().notTeam(team))); // points in a random direction if no players are found
		
		return null;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerKilled(PlayerDeathEvent event)
	{	
		if ( killerType.getValue() == KillerType.MYSTERY_KILLER )
			event.setDeathMessage(ChatColor.RED + event.getEntity().getName() + " died");
		else if ( killerType.getValue() == KillerType.CRAZY_KILLER && getTeam(event.getEntity()) == killer)
		{
			// if crazy killer dies, they respawn, but don't drop anything
			event.getDrops().clear();
			return;
		}
		
		boolean killerAllocated = getPlayers(new PlayerFilter().includeSpectators().team(killer)).size() != 0;
		if ( killerAllocated )
		{// if no killers allocated, allow respawning
			Helper.makeSpectator(getGame(), event.getEntity());
			playerKilledOrQuit(event.getEntity());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRespawn(PlayerRespawnEvent event)
	{
		if ( killerType.getValue() != KillerType.CRAZY_KILLER || getTeam(event.getPlayer()) != killer )
			return;
		
		giveCrazyKillerItems(event.getPlayer().getInventory(), getOnlinePlayers(new PlayerFilter().team(survivors)).size());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if(event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.STONE_PLATE && event.getAction() == Action.PHYSICAL && plinthLoc != null && event.getClickedBlock().getX() == plinthLoc.getBlockX() && event.getClickedBlock().getZ() == plinthLoc.getBlockZ())
	  	{
			// see if the player's inventory contains a winning item
			PlayerInventory inv = event.getPlayer().getInventory();
			
			for ( Material material : winningItems )
				if ( inv.contains(material) )
				{
					broadcastMessage(event.getPlayer().getName() + " brought a " + Helper.tidyItemName(material) + " to the plinth - the friendly players win!");
					finishGame(); // winning item brought to the plinth, friendlies win
					return;
				}
	  	}
		
		// eyes of ender can be made to seek out nether fortresses
		else if ( event.getPlayer().getWorld().getEnvironment() == Environment.NETHER && event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() == Material.EYE_OF_ENDER && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) )
		{
			Location target = KillerMinecraft.craftBukkitHelper().findNearestNetherFortress(event.getPlayer().getLocation());
			if ( target == null )
				event.getPlayer().sendMessage("No nether fortresses nearby");
			else
			{
				KillerMinecraft.craftBukkitHelper().createFlyingEnderEye(event.getPlayer(), target);
				event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);				
			}
			
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityDamaged(EntityDamageEvent event)
	{
		if ( killerType.getValue() != KillerType.INVISIBLE_KILLER || !(event.getEntity() instanceof Player) )
			return;
		
		Player victim = (Player)event.getEntity();
		if ( getTeam(victim) != killer )
			return;
		
		if ( restoreMessageProcessID != -1 )
		{// the "cooldown" must be reset
			getPlugin().getServer().getScheduler().cancelTask(restoreMessageProcessID);
		}
		else
		{// make them visible for a period of time
			setVisibility(victim, true);
		}
		
		restoreMessageProcessID = getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new RestoreInvisibility(victim.getName()), 100L); // 5 seconds
	}
	
	private void setVisibility(Player player, boolean visible)
	{
		if ( visible )
		{
			player.removePotionEffect(PotionEffectType.INVISIBILITY);
			player.sendMessage(ChatColor.RED + "You can be seen!");
		}
		else
			player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false), true);
	}
	
    class RestoreInvisibility implements Runnable
    {
    	String name;
    	public RestoreInvisibility(String playerName)
		{
			name = playerName;
		}
    	
    	public void run()
    	{
			Player player = getPlugin().getServer().getPlayerExact(name);
			if ( player == null || !player.isOnline() || Helper.isSpectator(getGame(), player) )
				return; // only if the player is still in the game
			
			setVisibility(player, false);
			player.sendMessage("You are now invisible again");
			restoreMessageProcessID = -1;
    	}
    }

	@EventHandler(ignoreCancelled = true)
	public void playerPickedUpItem(PlayerPickupItemEvent event)
	{
		if ( killerType.getValue() == KillerType.CRAZY_KILLER && event.getItem().getItemStack().getType() == Material.DIRT && getTeam(event.getPlayer()) == killer )
			event.getItem().getItemStack().setType(Material.TNT);
	}

    @EventHandler(priority = EventPriority.HIGH)
	public void onPrepareCraft(PrepareItemCraftEvent event)
	{
    	if ( allowCraftingMonsters.isEnabled() )
    		return;
    	
    	if ( event.getRecipe().getResult().getType() == Material.MONSTER_EGG )
    		event.getInventory().setResult(null);
	}
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onShootBow(EntityShootBowEvent event)
    {
    	if ( killerType.getValue() != KillerType.CRAZY_KILLER || !(event.getEntity() instanceof Player) )
    		return;
    	
    	Player player = (Player)event.getEntity();
    	if ( getTeam(player) != killer )
    		return;
    	
    	if ( !player.getInventory().contains(Material.TNT) )
    		return;
    	
    	player.getInventory().removeItem(new ItemStack(Material.TNT, 1));
    	TNTPrimed tnt = (TNTPrimed)player.getWorld().spawn(player.getLocation().add(0, 1.62, 0), TNTPrimed.class);
    	tnt.setVelocity(event.getProjectile().getVelocity().multiply(0.75f));
    	tnt.setFuseTicks(65);
    	
    	event.setProjectile(tnt);
    }
}