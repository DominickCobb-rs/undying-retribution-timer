/*
 * Copyright (c) 2022, Tom
 * Copyright (c) 2023, DominickCobb-rs <https://github.com/DominickCobb-rs>
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

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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
	private static final String CONFIG_GROUP = "undyingretributiontimer";
	@Inject
	private Client client;

	@Inject
	private UndyingRetributionTimerConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	private UndyingRetributionTimerInfoBox infoBox;

	public boolean onCooldown = false;
	public boolean pause = false;
	private static final int maxTicks = 300;
	public int remainingTicks;

	private static final String cooldownNotification = "Your Undying Retribution Relic saves your life. The Relic has lost power for 3 minutes.";
	private static final String resetNotification = "You are able to benefit from the Undying Retribution Relic's effect.";
	@Override
	protected void startUp() throws Exception
	{
		if (configManager.getConfiguration(CONFIG_GROUP,"cooldown") == null)
		{
			configManager.setConfiguration(CONFIG_GROUP, "cooldown", "-1");
		}
		if (client.getGameState().equals(GameState.LOGGED_IN))
		{
			checkCooldown();
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
		System.out.println(String.valueOf(event.getGameState()));
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			createInfobox();
			checkCooldown();
			pause = false;
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
		if (!onCooldown || pause)
		{
			return;
		}
		if (remainingTicks >= 0)
		{
			remainingTicks-=1;
		}

	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		MessageNode messageNode = chatMessage.getMessageNode();
		String message = messageNode.getValue();
		if (!messageNode.getType().equals(ChatMessageType.GAMEMESSAGE))
		{
			return;
		}
		if (message.contains(cooldownNotification))
		{
			cooldown(maxTicks);
		}
		if (message.contains(resetNotification))
		{
			onCooldown = false;
			remainingTicks = 0;
			save(false);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		Actor actor = actorDeath.getActor();
		if (actor instanceof Player)
		{
			Player player = (Player) actor;
			if (player == client.getLocalPlayer() && onCooldown)
			{
				onCooldown = false;
				remainingTicks = 0;
			}
		}
	}


	private void createInfobox()
	{
		if (infoBox == null)
		{
			BufferedImage icon = ImageUtil.loadImageResource(UndyingRetributionTimerPlugin.class,"/icons/infoboxIcon.png");
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
		int storedCooldown = Integer.parseInt(configManager.getConfiguration(CONFIG_GROUP,"cooldown"));
		if (storedCooldown > 0)
		{
			cooldown(storedCooldown);
			return;
		}
		remainingTicks = storedCooldown;
	}

	private void save(boolean quitting)
	{
		if (infoBox != null && quitting)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
		configManager.setConfiguration(CONFIG_GROUP, "cooldown", Integer.toString(remainingTicks));
	}

	@Provides
	UndyingRetributionTimerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UndyingRetributionTimerConfig.class);
	}
}
