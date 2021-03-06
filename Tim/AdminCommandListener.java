package Tim;

import java.util.Date;
import java.util.regex.Pattern;

import Tim.Commands.Utility.ChannelGroups;
import Tim.Commands.Utility.Shout;
import Tim.Data.ChannelInfo;
import Tim.Data.CommandData;
import Tim.Utility.CommandParser;
import Tim.Utility.Permissions;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Colors;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

/**
 * @author Matthew Walker
 */
class AdminCommandListener extends ListenerAdapter {
	private ChannelGroups channelGroups = new ChannelGroups();
	private Shout         shout         = new Shout();

	@Override
	public void onMessage(MessageEvent event) {
		String      message     = Colors.removeFormattingAndColors(event.getMessage());
		CommandData commandData = CommandParser.parseCommand(event);

		if (message.charAt(0) == '$') {
			if (message.startsWith("$skynet")) {
				return;
			}

			if (Pattern.matches("\\$\\d+.*", message)) {
				event.respond("Thank you for your donation to my pizza fund!");
			} else if (Pattern.matches("\\$-\\d+.*", message)) {
				event.respond("No stealing from the pizza fund, or I'll report you to Skynet!");
			} else if (Permissions.isAdmin(commandData)) {
				String   command = commandData.command;
				String[] args    = commandData.args;

				switch (command) {
					case "channelgroup":
						channelGroups.parseAdminCommand(command, args, event);
						break;
					case "setmuzzleflag":
						if (args != null && args.length == 2) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								boolean flag = false;
								if (!"0".equals(args[1])) {
									flag = true;
								}

								ChannelInfo ci = Tim.db.channel_data.get(target);
								ci.setMuzzleFlag(flag);

								event.respond("Channel muzzle flag updated for " + target);
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $setmuzzleflag <#channel> <0/1>");
						}
						break;
					case "automuzzlewars":
						if (args != null && args.length == 2) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								boolean flag = false;
								if (!"0".equals(args[1])) {
									flag = true;
								}

								ChannelInfo ci = Tim.db.channel_data.get(target);
								ci.setWarAutoMuzzle(flag);
								Tim.db.saveChannelSettings(ci);

								event.respond("Channel auto muzzle flag updated for " + target);
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $setmuzzleflag <#channel> <0/1>");
						}
						break;
					case "muzzle":
						if (args != null && ((args.length == 1 && StringUtils.isNumeric(args[0])) || (args.length == 2 && StringUtils.isNumeric(args[1])))) {
							String target;
							Date   date = new Date();
							long   expires;

							if (args.length == 2) {
								target = args[0].toLowerCase();
								expires = date.getTime() + (Long.parseLong(args[1]) * 60 * 1000);
							} else {
								target = event.getChannel()
											  .getName()
											  .toLowerCase();
								expires = date.getTime() + (Long.parseLong(args[0]) * 60 * 1000);
							}

							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);
								ci.setMuzzleFlag(true, expires);
								event.respond("Channel muzzled for specified time.");
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $muzzle <time in minutes> or $muzzle <#channel> <time in minutes>");
						}
						break;
					case "chatterlevel":
						if (args != null && args.length == 2 && args[1].equalsIgnoreCase("list")) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);
								event.respond(String.format("Reactive Chatter Level: %6.3f%%/Msg - Name Multiplier: %6.3f", ci.reactiveChatterLevel,
															ci.chatterNameMultiplier));
								event.respond(String.format("Random Chatter Level: %6.3f%%/Min", ci.randomChatterLevel));
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 4 && args[1].equalsIgnoreCase("reactive")) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								float level = Float.parseFloat(args[2]);
								float multi = Float.parseFloat(args[3]);

								if (level < 0 || level > 100) {
									event.respond("Chatter level must be between 0 and 100 (inclusive)");
								} else if (multi < 0 || multi > 100) {
									event.respond("Name multiplier must be between 0 and 100 (inclusive)");
								} else {
									ChannelInfo ci = Tim.db.channel_data.get(target);
									ci.setReactiveChatter(level, multi);
									Tim.db.saveChannelSettings(ci);

									event.respond("Reactive chatter level updated for " + target);
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 3 && args[1].equalsIgnoreCase("random")) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								float level = Float.parseFloat(args[2]);

								if (level < 0 || level > 100) {
									event.respond("Chatter level must be between 0 and 100 (inclusive)");
								} else {
									ChannelInfo ci = Tim.db.channel_data.get(target);
									ci.setRandomChatter(level);
									Tim.db.saveChannelSettings(ci);

									event.respond("Random chatter level updated for " + target);
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $chatterlevel <#channel> list");
							event.respond("Usage: $chatterlevel <#channel> reactive <%/Msg> <Name Multiplier>");
							event.respond("Usage: $chatterlevel <#channel> random <%/Min>");
						}
						break;
					case "chatterflag":
						if (args != null && args.length == 2 && args[0].equalsIgnoreCase("list")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								event.respond("Sending status of chatter settings for " + target + " via private message.");

								ci.chatter_enabled.keySet()
												  .forEach((setting) -> event.getUser()
																			 .send()
																			 .message(setting + ": " + ci.chatter_enabled.get(setting)
																														 .toString()));
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 4 && args[0].equalsIgnoreCase("set")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								boolean flag = false;
								if (!"0".equals(args[3])) {
									flag = true;
								}

								if (args[2].equalsIgnoreCase("all")) {
									for (String key : ci.chatter_enabled.keySet()) {
										ci.chatter_enabled.put(key, flag);
									}
									Tim.db.saveChannelSettings(ci);

									event.respond("All chatter flags updated.");
								} else {
									if (ci.chatter_enabled.containsKey(args[2])) {
										ci.chatter_enabled.put(args[2], flag);
										Tim.db.saveChannelSettings(ci);

										event.respond("Chatter flag updated.");
									} else {
										event.respond("I'm sorry, but I don't have a setting for " + args[2]);
									}
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $chatterflag list <#channel> OR $chatterflag set <#channel> <type> <0/1>");
							event.respond(
								"Valid Chatter Types: all, bored, catch, chainstory, challenge, dance, defenestrate, eightball, foof, fridge, get, greetings, "
								+ "groot, helpful_reactions, markov, search, sing, silly_reactions, summon, velociraptor");
						}
						break;
					case "commandflag":
						if (args != null && args.length == 2 && args[0].equalsIgnoreCase("list")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								event.respond("Sending status of command settings for " + target + " via private message.");

								ci.commands_enabled.keySet()
												   .forEach((setting) -> event.getUser()
																			  .send()
																			  .message(setting + ": " + ci.commands_enabled.get(setting)
																														   .toString()));
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 4 && args[0].equalsIgnoreCase("set")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								boolean flag = false;
								if (!"0".equals(args[3])) {
									flag = true;
								}

								if (args[2].equalsIgnoreCase("all")) {
									for (String key : ci.commands_enabled.keySet()) {
										ci.commands_enabled.put(key, flag);
									}
									Tim.db.saveChannelSettings(ci);

									event.respond("All command flags updated.");
								} else {
									if (ci.commands_enabled.containsKey(args[2])) {
										ci.commands_enabled.put(args[2], flag);
										Tim.db.saveChannelSettings(ci);

										event.respond("Command flag updated.");
									} else {
										event.respond("I'm sorry, but I don't have a setting for " + args[2]);
									}
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $commandflag list <#channel> OR $commandflag set <#channel> <command> <0/1>");
							event.respond(
								"Valid Commands: all, attack, catch, chainstory, challenge, commandment, dance, defenestrate, dice, eightball, expound, foof, "
								+ "fridge, get, lick, markov, ping, search, sing, summon, velociraptor, woot");
						}
						break;
					case "twitterrelay":
						if (args != null && args.length == 2 && args[0].equalsIgnoreCase("list")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								event.respond("Sending Twitter accounts relayed for " + target + " via private message.");

								ci.twitter_accounts.forEach((account) -> event.getUser()
																			  .send()
																			  .message(account));
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 3 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								if (args[0].equalsIgnoreCase("add")) {
									if (Tim.twitterStream.checkAccount(args[2]) > 0) {
										event.respond(
											"Twitter account added to channel's twitter feed. There may be a short delay (up to 90 seconds) before it takes "
											+ "effect.");
										ci.addTwitterAccount(args[2], true);
									} else {
										event.respond("I'm sorry, but that isn't a valid twitter account.");
									}
								} else {
									event.respond(
										"Twitter account removed from local feed. There may be a short delay (up to 90 seconds) before it takes effect.");
									ci.removeTwitterAccount(args[2]);
								}
								Tim.db.saveChannelSettings(ci);
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $twitterrelay list <#channel> OR $twitterrelay <add/remove> <#channel> <account>");
							event.respond("Suggested Accounts: NaNoWriMo, NaNoWordSprints, officeduckfrank, BotTimmy.");
							event.respond("Note, no '@' before account names for this command.");
						}
						break;
					case "twitterbucket":
						if (args != null && args.length == 2 && args[0].equalsIgnoreCase("list")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								event.respond(String.format(
									"Current Twitter Bucket settings for %s - Current Bucket: %.2f  Max Bucket: %.1f  Charge Rate / Minute: %" + ".2f", target,
									ci.tweetBucket, ci.tweetBucketMax, ci.tweetBucketChargeRate));

								ci.twitter_accounts.forEach(event::respond);
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 4 && args[0].equalsIgnoreCase("set")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								float max    = Float.parseFloat(args[2]);
								float charge = Float.parseFloat(args[3]);

								if (max > 0 && charge > 0) {
									ci.setTwitterTimers(max, charge);
									event.respond(String.format(
										"Current Twitter Bucket settings for %s - Current Bucket: %.2f  Max Bucket: %.1f  Charge Rate / Minute: %.2f", target,
										ci.tweetBucket, ci.tweetBucketMax, ci.tweetBucketChargeRate));
								} else {
									event.respond("Max bucket and charge rate must both be greater than 0.");
								}

								Tim.db.saveChannelSettings(ci);
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $twitterbucket list <#channel> OR $twitterbucket set <#channel> <max bucket> <charge rate>");
						}
						break;
					case "shutdown":
						if (event.getUser() != null && event.getUser()
															.getNick()
															.equalsIgnoreCase("Utoxin")) {
							event.respond("Shutting down...");
							Tim.shutdown();
						} else {
							event.respond("You're probably looking for the command '/kick Timmy' or '$part'");
						}
						break;
					case "reload":
						event.respond("Reading database tables ...");
						Tim.db.refreshDbLists();
						event.respond("Loading War Ticker ...");
						Tim.warticker = WarTicker.getInstance();
						event.respond("Loading Idle Ticker ...");
						Tim.deidler = DeIdler.getInstance();

						if (!Tim.db.getSetting("twitter_access_key")
								   .equals("")) {
							if (Tim.twitterStream != null) {
								event.respond("Closing old Twitter connection ...");
								Tim.twitterStream.publicStream.shutdown();
							}

							event.respond("Connecting to Twitter ...");
							Tim.twitterStream = new TwitterIntegration();
							Tim.twitterStream.startStream();
						}

						event.respond("Reload complete.");
						break;
					case "ignore":
						if (args != null && args.length > 0) {
							StringBuilder users = new StringBuilder();

							for (String arg : args) {
								users.append(" ")
									 .append(arg);
								Tim.db.ignore_list.add(arg);
								Tim.db.saveIgnore(arg, "hard");
							}

							event.respond("The following users have been ignored:" + users);
						} else {
							event.respond("Usage: $ignore <user 1> [ <user 2> [<user 3> [...] ] ]");
						}
						break;
					case "unignore":
						if (args != null && args.length > 0) {
							StringBuilder users = new StringBuilder();

							for (String arg : args) {
								users.append(" ")
									 .append(arg);
								Tim.db.ignore_list.remove(arg);
								Tim.db.soft_ignore_list.remove(arg);
								Tim.db.deleteIgnore(arg);
							}

							event.respond("The following users have been unignored:" + users);
						} else {
							event.respond("Usage: $unignore <user 1> [ <user 2> [<user 3> [...] ] ]");
						}
						break;
					case "listignores":
						event.respond("There are " + Tim.db.ignore_list.size() + " users ignored.");
						Tim.db.ignore_list.forEach(event::respond);
						break;
					case "listbadwords":
						event.respond("There are " + Tim.markov.badwordPatterns.keySet()
																			   .size() + " total bad words.");
						Tim.markov.badwordPatterns.keySet()
												  .forEach((key) -> event.respond("Key: " + key + "  Pattern: " + Tim.markov.badwordPatterns.get(key)
																																			.toString()));
						break;
					case "shout":
						shout.parseAdminCommand(message, args, event);
						break;
					case "part":
						event.getChannel()
							 .send()
							 .part();
						Tim.db.deleteChannel(event.getChannel());
						Tim.channelStorage.channelList.remove(event.getChannel()
																   .getName()
																   .toLowerCase());
						break;
					case "help":
						this.printAdminCommandList(event);
						break;
					case "checkuser":
						if (event.getUser() != null) {
							event.respond(event.getUser()
											   .toString());
						} else {
							event.respond("NULL");
						}
						break;
					default:
						if (!(Tim.markov.parseAdminCommand(event))) {
							event.respond("$" + command + " is not a valid admin command - try $help");
						}
						break;
				}
			} else {
				event.respond("You are not an admin. Only Admins have access to that command.");
			}
		}
	}

	@Override
	public void onPrivateMessage(PrivateMessageEvent event) {
		String message = Colors.removeFormattingAndColors(event.getMessage());

		if (event.getUser() != null && Tim.db.admin_list.contains(event.getUser()
																	   .getNick()
																	   .toLowerCase())) {
			String[] args = message.split(" ");
			if (args.length > 2) {
				StringBuilder msg = new StringBuilder();
				for (int i = 2; i < args.length; i++) {
					msg.append(args[i])
					   .append(" ");
				}
				if (args[0].equalsIgnoreCase("say")) {
					Tim.bot.sendIRC()
						   .message(args[1], msg.toString());
				} else if (args[0].equalsIgnoreCase("act")) {
					Tim.bot.sendIRC()
						   .action(args[1], msg.toString());
				}
			}
		}
	}

	private void printAdminCommandList(MessageEvent event) {
		if (event.getUser() == null) {
			return;
		}

		event.getChannel()
			 .send()
			 .action("whispers something to " + event.getUser()
													 .getNick() + ". (Check for a new window or tab with the help text.)");

		String[] helplines = {
			"Core Admin Commands:",
			"    $ignore <username>        - Places user on the bot's ignore list",
			"    $unignore <username>      - Removes user from bot's ignore list",
			"    $listignores              - Prints the list of ignored users",
			"    $part                     - Leaves channel message was sent from",
			"Channel Setting Commands:",
			"    $muzzle <time in minutes>        - Turns on the muzzle flag temporarily",
			"    $setmuzzleflag <#channel> <0/1>  - Sets the channel's current muzzle state",
			"    $automuzzlewars <#channel> <0/1> - Whether to auto-muzzle the channel during wars.",
			"    $chatterlevel                    - Set the chatter level for Timmy.",
			"    $chatterflag                     - Control Timmy's chatter settings in your channel",
			"    $commandflag                     - Control which commands can be used in your channel",
			"    $twitterrelay                    - Control Timmy's twitter relays.",
			"    $twitterbucket                   - Control the frequency of the twitter relays.",
			"    $channelgroup                    - Channel grouping commands."
		};

		for (String helpline : helplines) {
			event.getUser()
				 .send()
				 .message(helpline);
		}

		Tim.markov.adminHelpSection(event);
	}
}
