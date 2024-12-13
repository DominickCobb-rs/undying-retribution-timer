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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import java.awt.Color;
import net.runelite.client.config.Alpha;

@ConfigGroup("undyingretributiontimer")
public interface UndyingRetributionTimerConfig extends Config
{

	enum DisplayMode
	{
		TICKS,
		SECONDS,
		DECIMALS
	}
	@ConfigItem(
		keyName = "displayMode",
		name = "Display mode",
		description = "Configures how the timer is displayed.",
		position = 0
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.SECONDS;
	}

	@Alpha
	@ConfigItem(
		keyName = "textColor",
		name = "Text Color",
		description = "Infobox text color",
		position = 1
	)
	default Color textColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "onlyShowOnCooldown",
		name = "Only show on cooldown",
		description = "Only show the infobox while the timer is ticking down",
		position = 2
	)
	default boolean onlyShowOnCooldown()
	{
		return false;
	}

	@ConfigItem(
			keyName = "printRemaining",
			name = "Print remaining time in chat",
			description = "Print how much time was left on cooldown when you died",
			position = 2
	)
	default boolean printRemaining()
	{
		return true;
	}
}
