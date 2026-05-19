package wueffi.MiniGameCore.managers;

import org.bukkit.entity.Player;
import wueffi.MiniGameCore.utils.Party;

import java.util.HashMap;
import java.util.Map;

public final class PartyManager {
    private static final PartyManager instance = new PartyManager();
    private static final Map<String, Party> parties = new HashMap<>();
    private static final Map<String, Integer> partyCounter = new HashMap<>();

    private PartyManager() {
    }

    public static PartyManager getInstance() {
        return instance;
    }

    public static Party getPartyByPlayer(Player player) {
        return parties.values().stream()
                .filter(party -> party.containsPlayer(player))
                .findFirst()
                .orElse(null);
    }

    public boolean removeParty(String partyId) {
        Party party = parties.remove(partyId);
        if (party == null) return false;
        partyCounter.remove(party.getPartyName());
        return true;
    }

    public Party createParty(String partyName, Player owner) {
        int id = partyCounter.getOrDefault(partyName, 0) + 1;
        partyCounter.put(partyName, id);

        String partyId = partyName + "-" + id;
        Party party = new Party(partyId, partyName, owner);
        parties.put(partyId, party);
        party.addPlayer(owner);

        return party;
    }
}