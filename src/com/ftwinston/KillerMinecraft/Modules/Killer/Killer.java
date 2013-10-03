package com.ftwinston.KillerMinecraft.Modules.Killer;

import java.util.List;

import com.ftwinston.KillerMinecraft.GameMode;
import com.ftwinston.KillerMinecraft.Helper;
import com.ftwinston.KillerMinecraft.KillerMinecraft;
import com.ftwinston.KillerMinecraft.Option;
import com.ftwinston.KillerMinecraft.PlayerFilter;
import com.ftwinston.KillerMinecraft.Configuration.ChoiceOption;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;
import com.ftwinston.KillerMinecraft.Configuration.ToggleOption;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;

public class Killer extends GameMode
{
	ToggleOption dontAssignKillerUntilSecondDay, autoReallocateKillers, allowMultipleKillers;
	ChoiceOption<KillerType> killerType;
	
	enum KillerType
	{
		MYSTERY_KILLER,
		INVISIBLE_KILLER,
		CRAZY_KILLER,
	}
	
	static final Material[] winningItems = { Material.BLAZE_ROD, Material.GHAST_TEAR };

	@Override
	public int getMinPlayers() { return 3; }
	
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
	TeamInfo[] teams = new TeamInfo[] { survivors, killer };
	
	@Override
	public TeamInfo[] getTeams() { return teams; }
	
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
	public Option[] setupOptions()
	{
		dontAssignKillerUntilSecondDay = new ToggleOption("Don't assign killer until the second day", true);
		autoReallocateKillers = new ToggleOption("Allocate new killer if old ones die", true);
		allowMultipleKillers = new ToggleOption("Assign multiple killers if lots of people play", false);
		
		killerType = new ChoiceOption<KillerType>("Killer type");
		killerType.addChoice("Mystery Killer", KillerType.MYSTERY_KILLER, Material.FLINT_AND_STEEL, "No special powers, but", "Killer's identity is", "kept secret");
		killerType.addChoice("Invisible Killer", KillerType.INVISIBLE_KILLER, Material.GLASS, "Killer can't be seen,", "other players get infinity", "bows and warnings when", "the killer is nearby");
		killerType.addChoice("Crazy Killer", KillerType.CRAZY_KILLER, Material.TNT, "Any dirt the Killer", "picks up turns into", "TNT, and their bow fires TNT.");
		
		return new Option[] { dontAssignKillerUntilSecondDay, autoReallocateKillers, allowMultipleKillers, killerType };
	}
		
	@Override
	public String getHelpMessage(int num, TeamInfo team)
	{
		switch ( num )
		{
			case 0:
				if ( team == killer )
				{
					if ( allowMultipleKillers.isEnabled() )
						return "You have been chosen to try and kill everyone else.\nIf there are more than 5 players in the game, multiple players will have been chosen.\nNo one else has been told who was chosen.";
					else
						return "You have been chosen to try and kill everyone else.\nNo one else has been told who was chosen.";
				}
				else if ( getPlayers(new PlayerFilter().team(killer)).size() > 0 )
				{
					if ( allowMultipleKillers.isEnabled() )
						return "(At least) one player has been chosen to try and kill everyone else.\nIf there are more than 5 players in the game, multiple players will be chosen.\nNo one else has been told who they are.";
					else
						return "One player has been chosen to try and kill everyone else. No one else has been told who it is.";
				}
				else
				{
					if ( dontAssignKillerUntilSecondDay.isEnabled() )
					{
						if ( allowMultipleKillers.isEnabled() )
							return "At the start of the next game day, (at least) one player will be chosen to try and kill everyone else.\nIf there are more than 5 players in the game, multiple players will be chosen.\nNo one else will be told who they are.";
						else
							return "At the start of the next game day, one player will be chosen to try and kill everyone else.\nNo one else will be told who it is.";
					}
					else
					{
						if ( allowMultipleKillers.isEnabled() )
							return "(At least) one player will shortly be chosen to try and kill everyone else.\nIf there are more than 5 players in the game, multiple players will be chosen.\nNo one else will be told who they are.";
						else
							return "One player will shortly be chosen to try and kill everyone else.\nNo one else will be told who it is.";
					}
				}
			case 1:
				if ( team == killer )
				{
					if ( allowMultipleKillers.isEnabled() )
						return "As a killer, you win if all the friendly players die. You won't be told who the other killers are.";
					else
						return "As the killer, you win if everyone else dies.";
				}
				else
				{
					if ( allowMultipleKillers.isEnabled() )
						return "The killers win if everyone else dies... so watch your back!";
					else
						return "The killer wins if everyone else dies... so watch your back!";
				}
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
				if ( allowMultipleKillers.isEnabled() )
				{
					if ( autoReallocateKillers.isEnabled() )
						return "The other players will not automatically win when all the killers are dead, and additional killers may be assigned once to replace dead ones.";
					else
						return "The other players will not automatically win when all the killers are dead.";
				}
				else
				{
					if ( autoReallocateKillers.isEnabled() )
						return "The other players will not automatically win when the killer dies, and another killer may be assigned once the first one is dead.";
					else
						return "The other players will not automatically win when the killer dies.";
				}
			
			case 4:
				return "Death messages won't say how someone died, or who killed them.";
			
			case 5:
				if ( team == killer )
					return "If you make a compass, it will point at the nearest player. This won't work for other players.";
				else if ( allowMultipleKillers.isEnabled() )
					return "If one of the killers make a compass, it will point at the nearest player. This won't work for other players.";
				else
					return "If the killer makes a compass, it will point at the nearest player. This won't work for other players.";

			case 6:
				return "Several monster eggs can be crafted by combining one of their dropped items with an iron ingot.";
			case 7:
				return "Dispensers can be crafted using a sapling instead of a bow. These work well with monster eggs.";
			case 8:
				return "Eyes of ender will help you find nether fortresses (to get blaze rods).\nThey can be crafted from an ender pearl and a spider eye.";
				
			default:
				return null;
		}
	}
	
	@Override
	public boolean teamAllocationIsSecret() { return true; }
	
	@Override
	public boolean isLocationProtected(Location l, Player p)
	{
		// no protection, except for the plinth
		return  plinthLoc != null && l.getWorld() == plinthLoc.getWorld()
	            && l.getX() >= plinthLoc.getBlockX() - 1
	            && l.getX() <= plinthLoc.getBlockX() + 1
	            && l.getZ() >= plinthLoc.getBlockZ() - 1
	            && l.getZ() <= plinthLoc.getBlockZ() + 1;
	}
	
	@Override
	public boolean isAllowedToRespawn(Player player) { return getPlayers(new PlayerFilter().team(killer)).size() == 0; } // respawn if no killers allocated
	
	@Override
	public boolean useDiscreetDeathMessages() { return true; }
	
	@Override
	public Location getSpawnLocation(Player player)
	{
		Location spawnPoint = Helper.randomizeLocation(getWorld(0).getSpawnLocation(), 0, 0, 0, 8, 0, 8);
		return Helper.getSafeSpawnLocationNear(spawnPoint);
	}
	
	int allocationProcessID = -1;
	
	Location plinthLoc;
	
	@Override
	public void gameStarted()
	{
		plinthLoc = Helper.generatePlinth(getWorld(0));
		
		if ( dontAssignKillerUntilSecondDay.isEnabled() )
		{// check based on the time of day
			allocationProcessID = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
				long lastRun = 0;
				public void run()
				{
					long time = getPlugin().getServer().getWorlds().get(0).getTime();
					
					if ( time < lastRun ) // time of day has gone backwards: must be a new day! Allocate the killers
					{
						allocateKillers();
						getPlugin().getServer().getScheduler().cancelTask(allocationProcessID);
						
						if ( autoReallocateKillers.isEnabled() )
							allocationProcessID = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
								public void run()
								{
									allocateKillers();									
								}
							}, 1800L, 1800L); // check every 90 seconds
						else
							allocationProcessID = -1;
					}
					
					lastRun = time;
				}
			}, 600L, 100L); // initial wait: 30s, then check every 5s (still won't try to assign unless it detects a new day starting)
		}
		else // immediate allocation (team already been assigned)
		{
			List<Player> killers = getOnlinePlayers(new PlayerFilter().team(killer));
			float ratio = ((float)getOnlinePlayers(new PlayerFilter().team(survivors)).size())/killers.size();
			for ( Player player : killers )
				prepareKiller(player, killers.size(), ratio);
			
			if ( autoReallocateKillers.isEnabled() )
				allocationProcessID = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
						public void run()
						{
							allocateKillers();									
						}
					}, 1800L, 1800L); // check every 90 seconds
			else
				allocationProcessID = -1;
		}
	}
	
	private void allocateKillers()
	{
		int numAlive = getOnlinePlayers(new PlayerFilter().alive()).size();
		int numKillers = getOnlinePlayers(new PlayerFilter().team(killer)).size();
		int numAliveKillers = getOnlinePlayers(new PlayerFilter().alive().team(killer)).size();
		
		int numToAdd;
	
		// if any killers have already been assigned, and we're not meant to reallocate, don't add any more
		if ( !autoReallocateKillers.isEnabled() && numKillers > 0 )
			numToAdd = 0;
		
		// if we don't allow multiple killers, only ever add 0 or 1
		else if ( !allowMultipleKillers.isEnabled() )
			numToAdd = numAliveKillers > 0 ? 0 : 1;

		// 1-5 players should have 1 killer. 6-11 should have 2. 12-17 should have 3. 18-23 should have 4. 
		else
		{
			int targetNumKillers = numAlive / 6 + 1;
			numToAdd = targetNumKillers - numAliveKillers;
		}
		
		if ( numToAdd <= 0 )
			return;

		// pick players
		List<Player> players = getOnlinePlayers(new PlayerFilter().alive().team(survivors));
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
		
		players = getOnlinePlayers(new PlayerFilter().alive().team(survivors)); // some have moved to the killer team now, so re-select
		String message = ChatColor.YELLOW + (numToAdd == 1 ? "A killer has been allocated. You are not the killer!" : numToAdd + " killers have been allocated. You are not a killer!"); 
				
		for ( Player player : players )
			player.sendMessage(message);
	}
	
	private void prepareKiller(Player player, int numKillersAllocated, float numFriendliesPerKiller)
	{
		// this ougth to say "a" if multiple killers are/have been present in the game
		String message = ChatColor.RED.toString();
		if ( numKillersAllocated == 1 )
			message += "You are the killer!\n" + ChatColor.RESET;
		else
			message += "You are a killer!" + ChatColor.RESET + " You are one of " + numKillersAllocated + " that have just been allocated.\n";
		
		message += "Try to kill the other players without them working out who's after them.";
		player.sendMessage(message);
		
		PlayerInventory inv = player.getInventory();
		
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
		else
			return;	
	}
	
	@Override
	public void gameFinished()
	{
		if ( allocationProcessID != -1 )
		{
			getPlugin().getServer().getScheduler().cancelTask(allocationProcessID);
			allocationProcessID = -1;
		}
		
		// announce who the killer(s) was/were
		List<OfflinePlayer> killers = getPlayers(new PlayerFilter().team(killer));
		String message = killers.size() == 1 ? "The killer was: " : "The killers were:\n";
		
		for ( OfflinePlayer killer : killers )
			message += killer.getName() + "\n";
				
		broadcastMessage(message);
	}
	
	@Override
	public void playerJoined(Player player, boolean isNewPlayer)
	{
		if ( isNewPlayer )
			setTeam(player,  survivors);
		else if ( Helper.getTeam(getGame(), player) == killer ) // inform them that they're still a killer
			player.sendMessage("Welcome back. " + ChatColor.RED + "You are still " + (getPlayers(new PlayerFilter().team(killer)).size() > 1 ? "a" : "the" ) + " killer!"); 
		else
			player.sendMessage("Welcome back. You are not the killer, and you're still alive.");
	}
	
	@Override
	public void playerQuit(OfflinePlayer player)
	{
		if ( hasGameFinished() || getOnlinePlayers(new PlayerFilter().team(killer)).size() == 0 )
			return;
		
		int numFriendlies = getOnlinePlayers(new PlayerFilter().alive().team(survivors)).size();
		int numKillers = getOnlinePlayers(new PlayerFilter().alive().team(killer)).size();
		
		if ( numFriendlies != 0 )
		{
			// if only one person left (and they're not the killer), tell people they can /vote if they want to start a new game
			if ( numFriendlies == 1 && numKillers == 0 )
				broadcastMessage("There's only one player left, and they're not the killer.\nIf you want to draw this game and start another, start a vote by typing " + ChatColor.YELLOW + "/vote");
			return;
		}
		
		if ( numKillers > 0 )
		{
			broadcastMessage("All the friendly players died - the killer wins!");
			finishGame(); // killers win
		}
		else
		{
			broadcastMessage("Everybody died - nobody wins!");
			finishGame(); // nobody wins
		}
	}
	
	@Override
	public Location getCompassTarget(Player player)
	{
		TeamInfo team = Helper.getTeam(getGame(), player);
		if ( team == killer )
			return Helper.getNearestPlayerTo(player, getOnlinePlayers(new PlayerFilter().alive().notTeam(team))); // points in a random direction if no players are found
		
		return null;
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
}
