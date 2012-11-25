package com.ftwinston.Killer.MysteryKiller;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.GameModePlugin;
import com.ftwinston.Killer.Killer;

public class Plugin extends GameModePlugin
{
	public void onEnable()
	{
		Killer.registerGameMode(this);
	}
	
	@Override
	public GameMode createInstance()
	{
		return new MysteryKiller();
	}
	
	@Override
	public String[] getSignDescription()
	{
		return new String[] {
			"A player is",
			"chosen to kill",
			"the rest. Other",
			"players aren't",
			
			"told who is the",
			"killer! Death",
			"messages are a",
			"bit more vague.",

			"The others must",
			"get a blaze rod",
			"and bring it to",
			"the spawn point"
		};
	}
}