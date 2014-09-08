package co.fusionx.relay.internal.parser.main.command;

import java.util.List;

import co.fusionx.relay.base.Server;
import co.fusionx.relay.event.server.InviteEvent;
import co.fusionx.relay.internal.base.RelayQueryUserGroup;
import co.fusionx.relay.internal.base.RelayUserChannelGroup;
import co.fusionx.relay.util.ParseUtils;

public class InviteParser extends CommandParser {

    public InviteParser(final Server server,
            final RelayUserChannelGroup ucmanager,
            final RelayQueryUserGroup queryManager) {
        super(server, ucmanager, queryManager);
    }

    @Override
    public void onParseCommand(final List<String> parsedArray, final String prefix) {
        final String invitingNick = ParseUtils.getNickFromPrefix(prefix);
        final String invitedNick = parsedArray.get(0);

        if (mUCManager.getUser().isNickEqual(invitedNick)) {
            final String channelName = parsedArray.get(1);
            mServer.getBus().post(new InviteEvent(mServer, channelName, invitingNick));
        } else {
            // This is impossible - breaks RFC if it occurs - just ignore it
        }
    }
}