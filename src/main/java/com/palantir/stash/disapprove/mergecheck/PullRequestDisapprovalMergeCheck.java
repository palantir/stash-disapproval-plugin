// Copyright 2013 Palantir Technologies
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
package com.palantir.stash.disapprove.mergecheck;

import java.sql.SQLException;

import javax.annotation.Nonnull;

import net.java.ao.DBParam;

import org.slf4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.scm.pull.MergeRequest;
import com.atlassian.stash.scm.pull.MergeRequestCheck;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.DisapprovalConfiguration;
import com.palantir.stash.disapprove.persistence.DisapprovalMode;
import com.palantir.stash.disapprove.persistence.PersistenceManager;
import com.palantir.stash.disapprove.persistence.PullRequestDisapproval;

/**
 * This class is a MergeRequestCheck to disable merging where the target repo
 * has CI enabled and no comments which
 * 
 * @author cmyers
 * 
 */
public class PullRequestDisapprovalMergeCheck implements MergeRequestCheck {

    private final PersistenceManager cpm;
    private final Logger log;

    private final ActiveObjects ao;

    public PullRequestDisapprovalMergeCheck(
        ActiveObjects ao,
        PersistenceManager cpm, PluginLoggerFactory lf) {
        this.cpm = cpm;
        this.log = lf.getLoggerForThis(this);
        this.ao = ao;
    }

    public void notCheck(@Nonnull MergeRequest mr) {
        PullRequest pr = mr.getPullRequest();
        Repository repo = pr.getToRef().getRepository();

        DisapprovalConfiguration dpc;
        try {
            dpc = ao.create(DisapprovalConfiguration.class, new DBParam("REPO_ID", repo.getId()));
        } catch (Exception e) {
            log.error("Undeclared exception: ", e);
            return;
        }
    }

    @Override
    public void check(@Nonnull MergeRequest mr) {
        PullRequest pr = mr.getPullRequest();
        Repository repo = pr.getToRef().getRepository();

        DisapprovalConfiguration dpc;
        try {
            dpc = cpm.getDisapprovalConfiguration(repo);
        } catch (SQLException e) {
            log.error("Unable to get disapproval configuration - ignoring");
            return;
        }

        if (dpc.getDisapprovalMode().equals(DisapprovalMode.ADVISORY_MODE)) {
            // if in advisory mode, don't actually prevent merges
            return;
        }

        PullRequestDisapproval prd;
        try {
            prd = cpm.getPullRequestDisapproval(pr);
        } catch (SQLException e) {
            log.error("Unable to get disapproval configuration - ignoring");
            return;
        }

        if (prd.isDisapproved()) {
            mr.veto("This Pull Request is Disapproved",
                "Ask the disapprover '" + prd.getDisapprovedBy() + "' to undisapprove the pull request");
        }
    }
}
