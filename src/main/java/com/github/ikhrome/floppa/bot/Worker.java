package com.github.ikhrome.floppa.bot;

import com.github.ikhrome.floppa.commands.PlayCommand;
import com.github.ikhrome.floppa.config.Settings;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.AboutCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;

@Slf4j
public class Worker {
    public static void main(String[] args) throws IOException, LoginException {
        String token = Settings.get("botToken");
        String ownerId = Settings.get("ownerId");

        log.info("Setting bot owner id: " + ownerId);

        EventWaiter waiter = new EventWaiter();

        CommandClientBuilder client = new CommandClientBuilder();

        //client.useDefaultGame();
        client.setActivity(Activity.listening("RETROWAVE"));
        client.setStatus(OnlineStatus.DO_NOT_DISTURB);
        client.setOwnerId(ownerId);
        client.setEmojis("\uD83D\uDE03", "\uD83D\uDE2E", "\uD83D\uDE26");
        client.setPrefix("~");

        client.addCommands(
                new AboutCommand(Color.ORANGE, "the RETROWAVE bot",
                        new String[]{"Retrowave radio player","Floppa pictures","Reaction-based roles","... maybe more"},
                        Permission.MESSAGE_WRITE),
                new PlayCommand()
        );


        JDABuilder.createDefault(token)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.playing("...loading"))
                .addEventListeners(waiter, client.build())
                .build();
    }
}
