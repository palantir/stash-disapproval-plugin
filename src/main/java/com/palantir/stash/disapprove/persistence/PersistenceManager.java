// Copyright 2014 Palantir Technologies
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.palantir.stash.disapprove.persistence;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import net.java.ao.DBParam;

import org.slf4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.repository.Repository;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;

public class PersistenceManager {

    private final ActiveObjects ao;
    private final Logger log;

    public PersistenceManager(ActiveObjects ao,
        PluginLoggerFactory lf) {
        this.ao = ao;
        this.log = lf.getLoggerForThis(this);
    }

    ///////
    // Setter from request object
    ///////
    public void setDisapprovalConfigurationFromRequest(Repository repo, HttpServletRequest req) {
        log.trace("Setting configuration for repo" + repo.getId());

        DisapprovalMode dm = DisapprovalMode.ADVISORY_MODE;
        if (req.getParameter("strictModeEnabled") != null) {
            dm = DisapprovalMode.STRICT_MODE;
        }

        Boolean isEnabled = false;
        if (req.getParameter("enabled") != null) {
            isEnabled = true;
        }
        setDisapprovalConfiguration(repo, dm, isEnabled);
    }

    ///////
    // Other methods
    ///////
    public DisapprovalConfiguration getDisapprovalConfiguration(Repository repo) throws SQLException {
        log.trace("Getting configuration for repo" + repo.getId());
        return DisapprovalConfigurationImpl.getByRepository(ao, repo);
    }

    public void setDisapprovalConfiguration(Repository repo, DisapprovalMode mode, Boolean isEnabled) {
        DisapprovalConfiguration[] configs = ao.find(DisapprovalConfiguration.class, "REPO_ID = ?", repo.getId());
        DisapprovalConfiguration dc;
        if (configs.length == 0) {
            dc = ao.create(DisapprovalConfiguration.class, new DBParam("REPO_ID", repo.getId()));
        } else {
            dc = configs[0];
        }
        dc.setDisapprovalMode(mode);
        dc.setEnabled(isEnabled);
        dc.save();
    }

    public PullRequestDisapproval getPullRequestDisapproval(PullRequest pr) throws SQLException {
        return PullRequestDisapprovalImpl.getPullRequestDisapproval(ao, pr);
    }

    public void setPullRequestDisapproval(PullRequest pr, String username, boolean isDisapproved) throws SQLException {
        Repository repo = pr.getToRef().getRepository();
        if (isDisapproved) {
            log.trace("Disapproving pull request " + pr.getId() + " for repo " + repo.getName());
        } else {
            log.trace("Un-disapproving pull request " + pr.getId() + " for repo " + repo.getName());
        }
        PullRequestDisapprovalImpl.setPullRequestDisapproval(ao, pr, username, isDisapproved);
    }

}
