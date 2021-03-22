package io.pocketvote.task;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import cn.nukkit.Server;
import io.pocketvote.PocketVote;
import io.pocketvote.data.TaskResult;
import io.pocketvote.event.VoteEvent;

public class VoteCheckTask extends ApiRequest {

    private PocketVote plugin;
    
    public VoteCheckTask(PocketVote plugin) {
        super(plugin.isDev() ? "http://127.0.0.1:9000/v2/check" : "https://api.pocketvote.io/v2/check", "GET", "VOTE", null);
        this.plugin = plugin;

        plugin.getLogger().debug("Checking for outstanding votes.");
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onCompletion(Server server) {
        if(!(super.getResult() instanceof TaskResult)) {
            server.getLogger().error("[PocketVote] Result of " + getClass().getCanonicalName() + " was not an instance of TaskResult.");
            return;
        }

        if(!hasResult() || !(getResult() instanceof TaskResult)) return;

        TaskResult result = (TaskResult) getResult();

        if(result.hasError()) {
            server.getLogger().error("[PocketVote] VoteCheckTask: " + result.getMessage());
            return;
        }

        // Having no claims is the same has having no votes to process.
        if(!result.hasClaims() || !result.hasVotes()) {
            server.getLogger().debug("[PocketVote] " + result.getMessage());
            if(result.hasMeta()) plugin.startScheduler(result.getMeta().containsKey("frequency") ? (int) result.getMeta().get("frequency") : 60);
            return;
        }

        for(LinkedHashMap<String, String> vote : result.getVotes()) {
            if(Server.getInstance().getOfflinePlayer(vote.get("player")).hasPlayedBefore()) {
                server.getPluginManager().callEvent(new VoteEvent(vote.get("player"), vote.get("ip"), vote.get("site")));
            }
        }

        if(result.hasMeta()) plugin.startScheduler(result.getMeta().containsKey("frequency") ? (int) result.getMeta().get("frequency") : 60);
    }

}