package wueffi.MiniGameCore.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.managers.PartyManager;
import wueffi.MiniGameCore.utils.*;
import java.util.HashMap;

public class PartyCommand implements CommandExecutor {
    private final MiniGameCore plugin;

    public PartyCommand(MiniGameCore plugin) {
        this.plugin = plugin;
    }

    private static @NotNull HashMap<String, String> getCommandsPermissions() {
        HashMap<String, String> commands_permissions = new HashMap<>();
        commands_permissions.put("create", "mgcore.party.create");
        commands_permissions.put("leave", "mgcore.party.join");
        commands_permissions.put("join", "mgcore.party.join");
        commands_permissions.put("invite", "mgcore.party.invite");
        commands_permissions.put("deny", "mgcore.party.invite");
        commands_permissions.put("list", "mgcore.party.list");
        return commands_permissions;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        PartyManager partyManager = PartyManager.getInstance();
        Party party;
        Player target;

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        HashMap<String, String> commands_permissions = getCommandsPermissions();

        if (args.length < 1) {
            StringBuilder availableCommands = new StringBuilder("§fUsage: §6/party <");

            for (String command: commands_permissions.keySet()) {
                if (player.hasPermission(commands_permissions.get(command))) {
                    availableCommands.append(command).append(" | ");
                }
            }
            if (!availableCommands.isEmpty()) {
                availableCommands.setLength(availableCommands.length() - 3);
            }
            availableCommands.append(">");
            player.sendMessage(availableCommands.toString());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (plugin.getBannedPlayers().contains(player.getUniqueId())) {
                    player.sendMessage("§cYou were banned by an Administrator.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /party create <name>");
                    return true;
                }
                if (!player.hasPermission("mgcore.party.create")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                if (PartyManager.getPartyByPlayer(player) != null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are already in another party!");
                    return true;
                }
                String partyName = args[1];
                partyManager.createParty(partyName, player);
                player.sendMessage("§8[§6MiniGameCore§8]§a You created the party: " + args[1]);
                break;
            case "leave":
                if (args.length != 1) {
                    player.sendMessage("§cToo many Args! Usage: /party leave");
                    return true;
                }
                if (!player.hasPermission("mgcore.party.join")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                party = PartyManager.getPartyByPlayer(player);
                if (party == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are in no party!");
                    return true;
                }
                if (party.getPlayers().size() == 1) {
                    player.sendMessage("§8[§6MiniGameCore§8]§a You were the last player, deleting party...");
                    if (party.removePlayer(player)) {
                        player.sendMessage("§8[§6MiniGameCore§8]§a You left the party: " + party.getPartyName());
                        PartyManager.removeParty(party.getPartyId());
                    } else {
                        player.sendMessage("§8[§6MiniGameCore§8]§c Could not leave party: " + party.getPartyName());
                    }
                    return true;
                }
                if (party.isOwner(player)) {
                    player.sendMessage("§8[§6MiniGameCore§8]§a You were the owner, deleting party...");
                    if (party.removePlayer(player)) {
                        player.sendMessage("§8[§6MiniGameCore§8]§a You left the party: " + party.getPartyName());
                        for (Player player1 : party.getPlayers()) {
                            player1.sendMessage("§8[§6MiniGameCore§8]§a Owner §7 " + player.getName() + "§a has left the party!");
                        }
                        PartyManager.removeParty(party.getPartyId());
                    } else {
                        player.sendMessage("§8[§6MiniGameCore§8]§c Could not leave party: " + party.getPartyName());
                    }
                    return true;
                }
                if (party.removePlayer(player)) {
                    player.sendMessage("§8[§6MiniGameCore§8]§a You left the party: " + party.getPartyName());
                    for (Player player1 : party.getPlayers()) {
                        player1.sendMessage("§8[§6MiniGameCore§8]§7 " + player.getName() + "§a has left the party! (" + party.getPlayers().size() + " Members)");
                    }
                } else {
                    player.sendMessage("§8[§6MiniGameCore§8]§c Could not leave party: " + party.getPartyName());
                }
                break;
            case "join":
                if (plugin.getBannedPlayers().contains(player.getUniqueId())) {
                    player.sendMessage("§cYou were banned by an Administrator.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /party join <player>");
                    return true;
                }
                if (!player.hasPermission("mgcore.party.join")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§8[§6MiniGameCore§8]§c Inviter is not online!");
                    return true;
                }
                party = PartyManager.getPartyByPlayer(target);
                if (party == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cInviter is not in a party!");
                    return true;
                }
                if (party.isInvited(player)) {
                    for (Player player1 : party.getPlayers()) {
                        player1.sendMessage("§8[§6MiniGameCore§8]§7 " + player.getName() + "§a has joined the party! (" + party.getPlayers().size() + " Members)");
                    }
                    if (party.addPlayer(player)) {
                        player.sendMessage("§8[§6MiniGameCore§8]§a You joined the party: " + party.getPartyName());
                        return true;
                    } else {
                        player.sendMessage("§8[§6MiniGameCore§8]§a Could not join " + party.getPartyName() + ".");
                        return true;
                    }
                } else {
                    player.sendMessage("§8[§6MiniGameCore§8]§a You were not invited to " + party.getPartyName() + ".");
                }
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /party invite <player>");
                    return true;
                }
                if (!player.hasPermission("mgcore.party.invite")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§8[§6MiniGameCore§8]§c Player is not online!");
                    return true;
                }
                party = PartyManager.getPartyByPlayer(player);
                if (party == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are not in a party!");
                    return true;
                }
                if (party.invitePlayer(target)) {
                    target.sendMessage("§b " + player.getName() + "§3 invited you to a party: §6" + party.getPartyName() + "§3! (§b"+ party.getPlayers().size() + "§3P.)");
                    target.sendMessage("§f");

                    Component accept = Component.text("[ACCEPT]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/party join " + player.getName())).hoverEvent(HoverEvent.showText(Component.text("Click to accept invitation")));
                    Component deny = Component.text("[DENY]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/party deny " + player.getName())).hoverEvent(HoverEvent.showText(Component.text("Click to deny invitation")));

                    target.sendMessage(Component.text("             ").append(accept).append(Component.text("           ")).append(deny));
                    player.sendMessage("§8[§6MiniGameCore§8] §aYou invited " + args[1] + "!");
                } else {
                    player.sendMessage("§8[§6MiniGameCore§8] §cCould not invite " + args[1]);
                }
                break;
            case "deny":
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /party deny <player>");
                    return true;
                }
                if (!player.hasPermission("mgcore.party.invite")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§8[§6MiniGameCore§8]§c Player is not online!");
                    return true;
                }
                party = PartyManager.getPartyByPlayer(target);
                if (party == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cInviter is not in a party!");
                    return true;
                }
                if (party.isInvited(player)) {
                    if (!party.denyInvite(player)) {
                        player.sendMessage("§8[§6MiniGameCore§8]§a Could not deny " + party.getPartyName() + ".");
                    } else {
                        target.sendMessage("§8[§6MiniGameCore§8]§7 " + player.getName() + "§a has denied your invitation.");
                    }
                    return true;
                } else {
                    player.sendMessage("§8[§6MiniGameCore§8]§a You were not invited to " + party.getPartyName() + ".");
                }
                break;
            case "list":
                if (args.length > 2) {
                    player.sendMessage("§cMissing Args! Usage: /party list");
                    return true;
                }
                if (!player.hasPermission("mgcore.party.list")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                party = PartyManager.getPartyByPlayer(player);
                if (party == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are not in a party!");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8]§3 Party Members:");
                for (Player player1 : party.getPlayers()) {
                    player.sendMessage("§7- §a" + player1.getName());
                }
                break;

            default:
                player.sendMessage("§8[§6MiniGameCore§8] §cUnknown subcommand!");
                break;
        }

        return true;
    }
}