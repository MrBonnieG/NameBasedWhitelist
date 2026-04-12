package org.mrbonnieg.namebasedwhitelist;

import java.util.List;

public interface Storage {
    List<String> getPlayers();
    boolean containsPlayer(String username);
    boolean addPlayer(String username);
    boolean removePlayer(String username);
    void saveWhitelist();
}
