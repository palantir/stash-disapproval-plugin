package com.palantir.stash.disapprove.conditions;

import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionService;
import com.atlassian.stash.user.StashUser;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.PersistenceManager;
import com.palantir.stash.disapprove.persistence.PullRequestDisapproval;

/**
 * A condition which checks if the user can disapprove or remove disapproval for the PR
 * 
 * <pre>
 * &lt;condition class="com.palantir.stash.disapprove.conditions.CanUserDisapprove"/>
 * </pre>
 * 
 * </code> Add <code>invert="true"</code> to reverse the result.
 * 
 * @author cmyers
 */
public class CanUserDisapprove implements Condition {

    private final PermissionService ps;

    private final PersistenceManager pm;
    private final Logger log;

    public CanUserDisapprove(PermissionService ps, PersistenceManager pm, PluginLoggerFactory plf) {
        this.ps = ps;
        this.pm = pm;
        this.log = plf.getLogger(CanUserDisapprove.class.toString());
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
        // nothing to do here, no params are needed
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context) {
        final PullRequest pr = (PullRequest) context.get("pullRequest");
        final Repository repo = pr.getToRef().getRepository();
        // No idea if I need to do all this - in the context I care about, at least, the correct one is "currentUser", but I've seen other conditions use these other keys.
        StashUser user = (StashUser) context.get("currentUser");
        if (user == null) {
            user = (StashUser) context.get("user");
        }
        if (user == null) {
            user = (StashUser) context.get("accountUser");
        }
        if (user == null) {
            throw new IllegalStateException("Unable to get user!");
        }

        PullRequestDisapproval prd;
        try {
            prd = pm.getPullRequestDisapproval(pr);
            // if it isn't disapproved yet, anyone with read perms can disapprove
            if (!prd.isDisapproved()) {
                log.trace("PR is not disapproved");
                if (ps.hasRepositoryPermission(repo, Permission.REPO_READ)) {
                    return true;
                }
                return false;
            }
            // if it is already disapproved, only the disapprover or an admin can undisapprove
            log.trace("PR is disapproved");
            if (user.getName().equals(prd.getDisapprovedBy())) {
                return true;
            }
            if (ps.hasRepositoryPermission(repo, Permission.REPO_ADMIN)) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Unable to get disapproval metadata", e);
            // err on the side of not showing the buttons
            return false;
        }
    }
}
