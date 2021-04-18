package com.github.ikhrome.floppa.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ikhrome.floppa.entity.Track;
import com.github.ikhrome.floppa.managers.GuildMusicManager;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import kong.unirest.Unirest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayCommand extends Command {

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    public PlayCommand() {

        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(playerManager);
        this.name = "play";
        this.guildOnly = true;
        this.arguments = "<action>";
        this.help = "creates random-based playlist from number of tracks you requested from retrowave.ru Radio API";
        this.botPermissions = new Permission[]{Permission.MESSAGE_READ, Permission.MESSAGE_WRITE};
    }

    @Override
    protected void execute(CommandEvent event) {

        String[] command = event.getArgs().split(" ");

        if("init".equals(command[0])) {
            ObjectMapper mapper = new ObjectMapper();
            String limit = "3";

            if(!command[1].equals("")) {
                limit = command[1];
            }

            Unirest.config().enableCookieManagement(false);
            String result = Unirest.get("https://retrowave.ru/api/v1/tracks?limit=" + limit)
                    .asJson()
                    .getBody()
                    .getObject()
                    .getJSONObject("body").getJSONArray("tracks").toString();
            try {
                List<Track> tracks = mapper.readValue(result, new TypeReference<List<Track>>() {});
                EmbedBuilder infoEmbed = new EmbedBuilder();
                infoEmbed
                        .setColor(Color.BLUE)
                        .setTitle("Init Floppa player")
                        .setDescription("I'll try to find " + limit + " tracks for you!")
                        .setFooter("Make Floppa eat pelmen!");
                event.reply(infoEmbed.build());
                tracks.forEach(track -> {
                    loadAndPlay(event.getTextChannel(), "https://retrowave.ru" + track.getStreamUrl());
                });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            Unirest.config().reset();
        }

        if("skip".equals(command[0])) {
            skipTrack(event.getTextChannel());
        }
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());

        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    private void loadAndPlay(final TextChannel channel, final String track) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, track, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(Color.ORANGE)
                        .setDescription(":notes: Added new track: **" + track.getInfo().title + "** by **" + track.getInfo().author + "**!")
                        .addField("Shard", channel.getJDA().getShardInfo().getShardString(), true)
                        .setFooter("Make Floppa eat pelmen!");
                channel.sendMessage(embed.build()).queue();

                play(channel.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(channel.getGuild(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + track).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        connectToFirstVoiceChannel(guild.getAudioManager());

        musicManager.scheduler.queue(track);
    }

    private void skipTrack(TextChannel channel) {
        try {
            GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
            musicManager.scheduler.nextTrack();
            AudioTrackInfo track = musicManager.player.getPlayingTrack().getInfo();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.ORANGE)
                    .setTitle("Skipping Floppa to next track!")
                    .setDescription("**" + track.title + "** by **" + track.author + "**")
                    .addField("Shard", channel.getJDA().getShardInfo().getShardString(), true)
                    .setFooter("Make Floppa eat pelmen!");

            channel.sendMessage(embed.build()).queue();
        } catch (NullPointerException ex) {
            EmbedBuilder embedError = new EmbedBuilder();

            embedError.setTitle("Floppa Error!!!").setDescription("Meow! The playlist was accidentally ended." +
                    "\n\nYou can init new playlist by `~play init <number_of_tracks>` command!");
            channel.sendMessage(embedError.build()).queue();
        }
    }

    private static void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
        }
    }
}
