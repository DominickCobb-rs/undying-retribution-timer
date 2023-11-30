/*
 * Copyright (c) 2022, Tom
 * Copyright (c) 2023, DominickCobb-rs <https://github.com/DominickCobb-rs>
 * Copyright (c) 2022, LlemonDuck
 * Copyright (c) 2022, TheStonedTurtle
 * Copyright (c) 2019, Ron Young <https://github.com/raiyni>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.undyingretributiontimer;
import com.undyingretributiontimer.RaidRoom;


import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.World;
import net.runelite.api.WorldType;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Undying Retribution Timer",
	description = "A timer to keep track of undying retribution cooldown",
	tags = {"undying", "retribution", "timer", "relic"}
)
public class UndyingRetributionTimerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private UndyingRetributionTimerConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private InfoBoxManager infoBoxManager;
	private static final String CONFIG_GROUP = "undyingretributiontimer";
	private static final int maxTicks = 300;
	private static final int NOT_IN_RAID = 0;

	private boolean previouslyInRaid = false;

	private UndyingRetributionTimerInfoBox infoBox;

	private boolean previouslyInToa = false;
	private boolean leftRaid = false;
	public boolean onCooldown = false;
	public boolean pause = false;
	public int remainingTicks;
	//private static final String cooldownNotification = "Your Undying Retribution Relic saves your life. The Relic has lost power for 3 minutes.";
	//private static final String resetNotification1 = "You are able to benefit from the Undying Retribution Relic's effect.";
	//private static final String resetNotification2 = "Your Undying Retribution Relic's cooldown has ended and you may now benefit from its effect.";
	private static final String cooldownNotification = "Test start";
	private static final String resetNotification1 = "Test stop";
	private static final String resetNotification2 = "Test stop2";
	// delay inRaid = false by 3 ticks to alleviate any unexpected delays between rooms
	private int raidLeaveTicks = 0;
	private static final int WIDGET_PARENT_ID = 481;
	private static final int WIDGET_CHILD_ID = 40;

	@Override
	protected void startUp() throws Exception
	{
		if (configManager.getConfiguration(CONFIG_GROUP, "cooldown") == null)
		{
			configManager.setConfiguration(CONFIG_GROUP, "previouslyInRaid", "false");
			configManager.setConfiguration(CONFIG_GROUP, "cooldown", "-1");
		}
		if (client.getGameState().equals(GameState.LOGGED_IN) && client.getWorldType().contains(WorldType.SEASONAL))
		{
			previouslyInRaid = Boolean.parseBoolean(configManager.getConfiguration(CONFIG_GROUP, "previouslyInRaid"));
			if (previouslyInRaid && !inRaidNow())
			{
				configManager.setConfiguration(CONFIG_GROUP, "previouslyInRaid", false);
				offCooldown();
				remainingTicks = 0;
				if (!config.onlyShowOnCooldown())
				{
					createInfobox();
				}
			}
			else
			{
				checkCooldown();
			}
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		save(true);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			if (!client.getWorldType().contains(WorldType.SEASONAL))
			{
				removeInfobox();
				return;
			}
			if (!config.onlyShowOnCooldown())
			{
				createInfobox();
			}
			checkCooldown();
			pause=false;
			return;
		}
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			save(true);
			return;
		}
		if (event.getGameState() == GameState.HOPPING)
		{
			save(false);
			pause = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!onCooldown || pause || !client.getWorldType().contains(WorldType.SEASONAL))
		{
			return;
		}
		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		int region = lp == null ? -1 : WorldPoint.fromLocalInstance(client, lp).getRegionID();

		Widget w = client.getWidget(WIDGET_PARENT_ID, WIDGET_CHILD_ID);

		RaidRoom currentRoom = RaidRoom.forRegionId(region);
		boolean inRaidRaw = currentRoom != null || (w != null && !w.isHidden());

		raidLeaveTicks = inRaidRaw ? 3 : raidLeaveTicks - 1;

		boolean inToa = raidLeaveTicks > 0;

		if (leftRaid || (previouslyInToa && !inToa))
		{
			System.out.println("Leave raid detected, resetting cooldown");
			offCooldown();
			previouslyInToa = false;
			leftRaid = false;
			return;
		}
		if (remainingTicks >= 0)
		{
			remainingTicks -= 1;
		}
		previouslyInToa = inToa;
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		MessageNode messageNode = chatMessage.getMessageNode();
		String message = messageNode.getValue();
		if (!messageNode.getType().equals(ChatMessageType.GAMEMESSAGE))
		{
			//return;
		}
		if (message.contains(cooldownNotification))
		{
			cooldown(maxTicks);
		}
		if (message.contains(resetNotification1) || message.contains(resetNotification2))
		{
			offCooldown();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		if (!client.getWorldType().contains(WorldType.SEASONAL))
		{
			return;
		}
		Actor actor = actorDeath.getActor();
		if (actor instanceof Player)
		{
			Player player = (Player) actor;
			if (player == client.getLocalPlayer() && onCooldown)
			{
				offCooldown();
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (!(client.getWorldType().contains(WorldType.SEASONAL)) && e.getVarbitId() != Varbits.THEATRE_OF_BLOOD && e.getVarbitId() != Varbits.IN_RAID)
		{
			return;
		}

		boolean currentlyInRaid = inRaidNow();
		if (currentlyInRaid && !previouslyInRaid)
		{
			System.out.println("Raid entered");
			configManager.setConfiguration(CONFIG_GROUP, "previouslyInRaid", "true");
		}
		else if (!currentlyInRaid && previouslyInRaid)
		{
			System.out.println("Raid left");
			configManager.setConfiguration(CONFIG_GROUP, "previouslyInRaid", "false");
			if (onCooldown)
			{
				leftRaid = true;
			}
		}
		previouslyInRaid = currentlyInRaid;
	}

	public boolean inRaidNow()
	{
		// ToB
		if (client.getVarbitValue(Varbits.THEATRE_OF_BLOOD) == 2 || client.getVarbitValue(Varbits.THEATRE_OF_BLOOD) == 3)
		{
			return true;
		}
		// Chambers
		if (client.getVarbitValue(Varbits.IN_RAID) != NOT_IN_RAID)
		{
			return true;
		}
		return false;
	}

	private void offCooldown()
	{
		if (config.onlyShowOnCooldown())
		{
			removeInfobox();
		}
		onCooldown = false;
		remainingTicks = 0;
		save(false);
	}

	private void createInfobox()
	{
		if (infoBox == null)
		{
			BufferedImage icon = ImageUtil.loadImageResource(UndyingRetributionTimerPlugin.class, "/icons/infoboxIcon.png");
			infoBox = new UndyingRetributionTimerInfoBox(this, config);
			infoBox.setImage(icon);
			infoBoxManager.addInfoBox(infoBox);
		}
	}

	private void cooldown(int ticks)
	{
		if (ticks != 0)
		{
			createInfobox();
			onCooldown = true;
			remainingTicks = ticks;
		}
	}

	public static String to_mmss(int ticks)
	{
		int m = ticks / 100;
		int s = (ticks - m * 100) * 6 / 10;
		return m + (s < 10 ? ":0" : ":") + s;
	}

	public static String to_mmss_precise_short(int ticks)
	{
		int min = ticks / 100;
		int tmp = (ticks - min * 100) * 6;
		int sec = tmp / 10;
		int sec_tenth = tmp - sec * 10;
		return min + (sec < 10 ? ":0" : ":") + sec + "." +
			sec_tenth;
	}

	private void checkCooldown()
	{
		if (!onCooldown || pause)
		{
			int storedCooldown = Integer.parseInt(configManager.getConfiguration(CONFIG_GROUP, "cooldown"));
			if (storedCooldown > 0)
			{
				cooldown(storedCooldown);
				return;
			}
			remainingTicks = storedCooldown;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!e.getGroup().equals(CONFIG_GROUP))
		{
			return;
		}
		if (e.getKey().contains("onlyShowOnCooldown"))
		{
			if (Boolean.parseBoolean(e.getNewValue()) && !onCooldown)
			{
				removeInfobox();
				return;
			}
			// Don't need to check if on cooldown, and createInfobox() checks if it's not null
			if (!Boolean.parseBoolean(e.getNewValue()))
			{
				createInfobox();
			}
		}
	}

	private void save(boolean quitting)
	{
		if (infoBox != null && quitting)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
		configManager.setConfiguration(CONFIG_GROUP, "cooldown", Integer.toString(remainingTicks));
		configManager.setConfiguration(CONFIG_GROUP, "previouslyInRaid", (previouslyInRaid));
	}

	private void removeInfobox()
	{
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
	}

	@Provides
	UndyingRetributionTimerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UndyingRetributionTimerConfig.class);
	}
}
