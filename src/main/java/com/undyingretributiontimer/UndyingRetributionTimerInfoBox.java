/*
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

import javax.inject.Inject;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import java.awt.Color;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

public class UndyingRetributionTimerInfoBox extends InfoBox
{
	private final UndyingRetributionTimerPlugin plugin;
	private final UndyingRetributionTimerConfig config;
	@Inject
	public UndyingRetributionTimerInfoBox(UndyingRetributionTimerPlugin plugin, UndyingRetributionTimerConfig config)
	{
		super(null, plugin);
		this.plugin = plugin;
		this.config = config;
		setPriority(InfoBoxPriority.MED);
	}

	@Override
	public String getText()
	{
		String str;
		if (!plugin.onCooldown)
		{
			return "";
		}

		if (config.displayMode() == UndyingRetributionTimerConfig.DisplayMode.TICKS)
		{
			str = String.valueOf(plugin.remainingTicks);
		}
		else if (config.displayMode() == UndyingRetributionTimerConfig.DisplayMode.DECIMALS)
		{
			str = UndyingRetributionTimerPlugin.to_mmss_precise_short(plugin.remainingTicks);
		}
		else
		{
			str = UndyingRetributionTimerPlugin.to_mmss(plugin.remainingTicks);
		}
		return str;
	}

	@Override
	public Color getTextColor()
	{
		return config.textColor();
	}

	@Override
	public String getTooltip()
	{
		return "Undying Retribution";
	}
}
