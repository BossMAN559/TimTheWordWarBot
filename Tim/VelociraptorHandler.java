package Tim;

/*
 * Copyright (C) 2015 mwalker
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import org.pircbotx.Channel;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author mwalker
 */
class VelociraptorHandler {
	void sighting(Event event) {
		Channel channel = null;
		boolean action = false;
		
		if (event instanceof MessageEvent) {
			channel = ((MessageEvent) event).getChannel();
		} else if (event instanceof ActionEvent) {
			channel = ((ActionEvent) event).getChannel();
			action = true;
		}

		if (channel == null) {
			return;
		}

		ChannelInfo cdata = Tim.db.channel_data.get(channel.getName().toLowerCase());

		if (Tim.rand.nextInt(100) < cdata.velociraptor_odds) {
			cdata.recordVelociraptorSighting();

			if (action) {
				event.respond("jots down a note in a red notebook labeled 'Velociraptor Sighting Log'.");
			} else {
				event.respond("Velociraptor sighted! Incident has been logged.");
			}

			cdata.velociraptor_odds--;
			
			if (Tim.rand.nextInt(100) < 10) {
				swarm(cdata.channel);
			}
		}
	}

	private int oddsIncreasing(int input) {
		return (int) Math.floor(0.001 * input * input);
	}

	private int oddsDecreasing(int input) {
		return (int) Math.floor(Math.log(input) * 8);
	}

	void swarm(String channel) {
		ChannelInfo cdata = Tim.db.channel_data.get(channel.toLowerCase());

		Collection<ChannelInfo> attackCandidates = Tim.db.channel_data.entrySet().stream().filter(map -> map.getValue().chatter_enabled.get("velociraptor") && map.getValue().activeVelociraptors >= cdata.activeVelociraptors * 0.9).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).values();

		Collection<ChannelInfo> migrateCandidates = Tim.db.channel_data.entrySet().stream().filter(map -> map.getValue().chatter_enabled.get("velociraptor") && map.getValue().activeVelociraptors <= cdata.activeVelociraptors * 0.5).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).values();

		if (cdata.activeVelociraptors > 10 && Tim.rand.nextInt(100) < oddsDecreasing(cdata.activeVelociraptors)) {
			int attackCount = Tim.rand.nextInt(cdata.activeVelociraptors / 2);

			if (migrateCandidates.size() > 0 && Tim.rand.nextInt(100) < oddsIncreasing(cdata.activeVelociraptors)) {
				ChannelInfo victimCdata = (ChannelInfo) migrateCandidates.toArray()[Tim.rand.nextInt(migrateCandidates.size())];

				cdata.recordSwarmKills(attackCount, 0);
				victimCdata.recordSwarmDeaths(-attackCount);

				Tim.bot.sendIRC().message(channel, String.format("Apparently feeling crowded, %d of the velociraptors head off in search of new territory. "
					+ "After a searching, they settle in %s.", attackCount, victimCdata.channel));

				if (victimCdata.chatter_enabled.get("velociraptor") && !victimCdata.muzzled) {
					Tim.bot.sendIRC().message(victimCdata.channel, String.format("A swarm of %d velociraptors appears from the direction of %s. "
						+ "The local raptors are nervous, but the strangers simply want to join the colony.", attackCount, cdata.channel));
				}
			} else if (attackCandidates.size() > 0) {
				ChannelInfo victimCdata = (ChannelInfo) attackCandidates.toArray()[Tim.rand.nextInt(attackCandidates.size())];

				int returnCount = Math.max(0, Tim.rand.nextInt(attackCount / 2) - Tim.rand.nextInt((int) Math.floor(Math.sqrt(cdata.activeVelociraptors))));
				int defendingCount = victimCdata.activeVelociraptors;

				double killPercent = (((double) attackCount / (double) defendingCount) * 25.0) + (Math.log(Tim.rand.nextInt(attackCount)) * 5);

				if (killPercent > 100) {
					killPercent = 100;
				}

				int killCount = (int) (defendingCount * (killPercent / 100.0));
				if (killCount < 0) {
					return;
				}

				cdata.recordSwarmKills(attackCount - returnCount, killCount);
				victimCdata.recordSwarmDeaths(killCount);

				Tim.bot.sendIRC().message(channel, String.format("Suddenly, %d of the velociraptors go charging off to attack a group in %s! "
					+ "After a horrific battle, they manage to kill %d of them, and %d return home!", attackCount, victimCdata.channel, killCount, returnCount));

				if (victimCdata.chatter_enabled.get("velociraptor") && !victimCdata.muzzled) {
					Tim.bot.sendIRC().message(victimCdata.channel, String.format("A swarm of %d velociraptors suddenly appears from the direction of %s. "
						+ "The local raptors do their best to fight them off, and %d of them die before the swarm disappears.", attackCount, cdata.channel, killCount));
				}
			}
		} else {
			if (cdata.activeVelociraptors > 1) {
				int newCount = Tim.rand.nextInt(cdata.activeVelociraptors / 2);
				
				if (cdata.activeVelociraptors >= 4) {
					newCount -= Tim.rand.nextInt(cdata.activeVelociraptors / 4);
				}

				if (newCount < 1) {
					return;
				}

				cdata.recordVelociraptorSighting(newCount);

				if (newCount > 1) {
					Tim.bot.sendIRC().message(channel, String.format("Something is going on in the swarm... hey, where did those %d baby"
						+ " raptors come from?! Clever girls.", newCount));
				} else {
					Tim.bot.sendIRC().message(channel, "Something is going on in the swarm... hey, where did that baby"
						+ " raptor come from?! Clever girl.");
				}
			} else if (cdata.activeVelociraptors < 0) {
				cdata.activeVelociraptors = 0;
			}
		}
	}
}