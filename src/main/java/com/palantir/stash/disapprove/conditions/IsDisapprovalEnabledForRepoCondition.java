package com.palantir.stash.disapprove.conditions;

import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.stash.repository.Repository;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.DisapprovalConfiguration;
import com.palantir.stash.disapprove.persistence.PersistenceManager;

public class IsDisapprovalEnabledForRepoCondition implements Condition {

    private final PersistenceManager pm;
    private final Logger log;

    public IsDisapprovalEnabledForRepoCondition(PersistenceManager pm, PluginLoggerFactory lf) {
        this.pm = pm;
        this.log = lf.getLoggerForThis(this);
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context) {

        // request, principal, changeset, repository
        Repository repo = (Repository) context.get("repository");
        DisapprovalConfiguration dc;
        if (repo == null) {
            return false;
        }
        try {
            dc = pm.getDisapprovalConfiguration(repo);
        } catch (SQLException e) {
            log.error("Failed to get DisapprovalConfiguration for repo: " + repo.toString(), e);
            return false;
        }

        if (dc.isEnabled()) {
            return true;
        }
        return false;
    }
}
