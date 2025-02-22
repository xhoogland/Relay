package co.fusionx.relay.internal.base;

import android.util.Pair;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import co.fusionx.relay.base.Channel;
import co.fusionx.relay.base.FormatSpanInfo;
import co.fusionx.relay.base.Server;
import co.fusionx.relay.base.UserChannelInterface;
import co.fusionx.relay.event.channel.ChannelActionEvent;
import co.fusionx.relay.event.channel.ChannelEvent;
import co.fusionx.relay.event.channel.ChannelMessageEvent;
import co.fusionx.relay.internal.sender.BaseSender;
import co.fusionx.relay.internal.sender.RelayChannelSender;
import co.fusionx.relay.sender.ChannelSender;
import co.fusionx.relay.util.Utils;

import static co.fusionx.relay.misc.RelayConfigurationProvider.getPreferences;

public class RelayChannel extends RelayAbstractConversation<ChannelEvent> implements Channel {

    // As set out in RFC2812
    private final static ImmutableList<Character> CHANNEL_PREFIXES = ImmutableList.of('#', '&',
            '+', '!', '%', '@', '~');

    private final RelayMainUser mUser;

    private final String mChannelName;

    private final Collection<RelayChannelUser> mUsers;

    private final ChannelSender mChannelSender;

    RelayChannel(final Server server, final RelayMainUser user, final BaseSender baseSender,
            final String channelName) {
        super(server);

        mUser = user;
        mChannelSender = new RelayChannelSender(this, baseSender);
        mChannelName = channelName;

        mUsers = new HashSet<>();

        clearInternalData();
    }

    /**
     * Returns whether a string is a channel name based on the first character of the string
     *
     * @param firstCharacter the first character of the string that is to be tested
     * @return whether the character can be one at the start of a channel
     */
    public static boolean isChannelPrefix(char firstCharacter) {
        return CHANNEL_PREFIXES.contains(firstCharacter);
    }

    /**
     * Returns a list of all the users currently in the channel
     *
     * @return list of users currently in the channel
     */
    @Override
    public Collection<RelayChannelUser> getUsers() {
        return mUsers;
    }

    /**
     * Gets the name of the channel
     *
     * @return the name of the channel
     */
    @Override
    public String getName() {
        return mChannelName;
    }

    /**
     * Returns the id of the channel which is simply its name
     *
     * @return the id (name) of the channel
     */
    @Override
    public String getId() {
        return mChannelName;
    }

    /*
     * A channel is equal to another if the servers are equal and if the channel's names are
     * equal regardless of case
     */
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof RelayChannel)) {
            return false;
        }
        final RelayChannel otherChannel = (RelayChannel) o;
        return otherChannel.getServer().equals(getServer())
                && otherChannel.getName().equalsIgnoreCase(mChannelName);
    }

    /*
     * The hashcode of a channel can simply be the same as that of its name
     */
    @Override
    public int hashCode() {
        return mChannelName.toLowerCase().hashCode();
    }

    /*
     * A channel's string representation is simply its name
     */
    @Override
    public String toString() {
        return mChannelName;
    }

    // ChannelSender interface
    @Override
    public void sendAction(final String action) {
        mChannelSender.sendAction(action);
        final Pair<String, List<FormatSpanInfo>> actionAndColors =
                Utils.parseAndStripColorsFromMessage(action);
        sendChannelSelfMessage(() -> new ChannelActionEvent(this,
                actionAndColors.first, actionAndColors.second, mUser));
    }

    @Override
    public void sendKick(final String userNick, final Optional<String> reason) {
        mChannelSender.sendKick(userNick, reason);
    }

    @Override
    public void sendMessage(final String message) {
        mChannelSender.sendMessage(message);
        final Pair<String, List<FormatSpanInfo>> messageAndColors =
                Utils.parseAndStripColorsFromMessage(message);
        sendChannelSelfMessage(() -> new ChannelMessageEvent(this,
                messageAndColors.first, messageAndColors.second, mUser));
    }

    @Override
    public void sendPart(final Optional<String> reason) {
        mChannelSender.sendPart(reason);
    }

    @Override
    public void sendTopic(final String newTopic) {
        mChannelSender.sendTopic(newTopic);
    }

    @Override
    public void sendUserMode(final String userNick, final String mode) {
        mChannelSender.sendUserMode(userNick, mode);
    }

    // Helpers
    public void clearInternalData() {
        // Clear the list of users
        mUsers.clear();
    }

    void addUser(final RelayChannelUser user) {
        mUsers.add(user);
    }

    void removeUser(final RelayChannelUser user) {
        mUsers.remove(user);
    }

    private void sendChannelSelfMessage(final Supplier<ChannelEvent> function) {
        if (getPreferences().isSelfEventHidden()) {
            return;
        }
        postAndStoreEvent(function.get());
    }
}