package me.v0rham.infvalets.commands.balance;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class Balance implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int maxLine = page * 5;
        int minLine = ((page - 1) * 5) + 1;
        int pageValue = (inviteList.size() % 5 == 0) ? (inviteList.size() / 5) : ((inviteList.size() / 5) + 1);
        msg.replaceAll("%page%", page + "/" + pageValue);
        return false;
    }

    public HashMap<String, Integer> getBalance(Player player) {
        HashMap<String, Integer> balance = new HashMap<>();


    }
}


//В вашем кошельке:
//6. SpawnValet - 123000
//7. NeSpawnValet - 16000
//8. Valet3 - 41
//9. Valet4 - 5123
//10. Valet5 - 9105
//<<< Предыдущая [2/20] Следующая >>>
