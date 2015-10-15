package com.ftwinston.KillerMinecraft.Modules.KillerOnTheLoose;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.ftwinston.KillerMinecraft.Helper;

public class DistanceEffectHandler implements Runnable
{	
	public DistanceEffectHandler(KillerOnTheLoose game)
	{
		this.game = game;
	}
	
	KillerOnTheLoose game;
	final double maximumDistance = 25, maximumDistanceSq = maximumDistance * maximumDistance;
	final int playerEffectTickInterval = 300, playerEffectTierDuration = 1200, ticksPerEffectTier = playerEffectTierDuration / playerEffectTickInterval;
	
	public void run()
	{
		LinkedList<Player> players = game.getOnlinePlayers();
		
		while (!players.isEmpty())
		{
			Player toCheck = players.pop();
			
			for (Player other : players)
			{
				if (toCheck.getWorld() != other.getWorld())
					continue;
				
				double distanceSq = toCheck.getLocation().distanceSquared(other.getLocation());
				
				if (distanceSq <= maximumDistanceSq)
				{
					// clear/restart the process for this player, and the other player (who we have just demonstrated is within range of another).
					restartPlayerProcess(toCheck);
					restartPlayerProcess(other);
					
					// also remove that other player from the list.
					players.remove(other);
					break;
				}
			}
		}
	}

	HashMap<String, Integer> playerEffectProcessIDs = new HashMap<String, Integer>();
	private void restartPlayerProcess(Player player)
	{
		Integer processID = playerEffectProcessIDs.get(player.getName());
		if (processID != null)
			game.getScheduler().cancelTask(processID.intValue());
		
		final String playerName = player.getName(); 
		processID = game.getScheduler().scheduleSyncRepeatingTask(game.getPlugin(), new Runnable() {
			int tickNumber = 0;
			public void run()
			{
				Player player = Helper.getPlayer(playerName);
				if (player == null)
					return;
				
				if (tickNumber == 0)
					player.sendMessage(ChatColor.RED + "You feel ill. Move close to other players and you'll get better. Otherwise, things will get worse...");
				
				Collection<PotionEffect> effects = getPotionEffects((++tickNumber) / ticksPerEffectTier);
				player.addPotionEffects(effects);
			}
			
			private Collection<PotionEffect> getPotionEffects(int tierNumber)
			{
				Collection<PotionEffect> effects = new LinkedList<PotionEffect>();
				
				// apply hunger
				effects.add(new PotionEffect(PotionEffectType.HUNGER, playerEffectTickInterval, 0, false));
				if (tierNumber < 2)
					return effects;
				
				// apply mining fatigue
				effects.add(new PotionEffect(PotionEffectType.SLOW_DIGGING, playerEffectTickInterval, 0, false));
				if (tierNumber < 3)
					return effects;
				
				// apply slowness
				effects.add(new PotionEffect(PotionEffectType.SLOW, playerEffectTickInterval, 0, false));				
				if (tierNumber < 4)
					return effects;
				
				// apply nausea
				effects.add(new PotionEffect(PotionEffectType.CONFUSION, playerEffectTickInterval, 0, false));
				if (tierNumber < 5)
					return effects;
				
				// apply blindness
				effects.add(new PotionEffect(PotionEffectType.BLINDNESS, playerEffectTickInterval, 0, false));
				if (tierNumber < 6)
					return effects;
				
				// apply poison
				effects.add(new PotionEffect(PotionEffectType.POISON, playerEffectTickInterval, 0, false));
				
				return effects;
			}
		}, 6000L, playerEffectTickInterval); // initial wait: 5 mins, then repeats every minute after that
		
		playerEffectProcessIDs.put(player.getName(), processID);
	}
	
	public void stopPlayerProcesses()
	{
		for (Integer processID : playerEffectProcessIDs.values())
			game.getScheduler().cancelTask(processID.intValue());
	}
}
