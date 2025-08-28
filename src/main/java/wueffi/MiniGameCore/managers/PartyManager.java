package wueffi.MiniGameCore.managers;

import org.bukkit.entity.Player;
import wueffi.MiniGameCore.utils.Party;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PartyManager {
    private static final PartyManager instance = new PartyManager();
    private static final Map<String, Party> parties = new HashMap<>();
    private final Map<String, Integer> partyCounter = new HashMap<>();

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

    public Party getPartyById(String partyId) {
        return parties.get(partyId);
    }

    public List<Party> getParties() {
        return parties.values().stream().toList();
    }

    public static boolean removeParty(String partyId) {
        return parties.remove(partyId) != null;
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