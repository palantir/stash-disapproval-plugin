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
package com.palantir.stash.disapprove.persistence;

import java.sql.SQLException;

import net.java.ao.DBParam;
import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;

public class PullRequestDisapprovalImpl {

    private final PullRequestDisapproval prd;

    public PullRequestDisapprovalImpl(PullRequestDisapproval prd) {
        this.prd = prd;
    }

    public Repository getRepository(RepositoryService rs) {
        return rs.getById(prd.getRepositoryId());
    }

    public void setRepository(Repository repo) {
        prd.setRepositoryId(repo.getId());
    }

    public PullRequest getPullRequest(PullRequestService prs) {
        return prs.getById(prd.getRepositoryId(), prd.getPullRequestId());
    }

    public void setPullRequest(PullRequest pr) {
        prd.setPullRequestId(pr.getId());
        prd.setRepositoryId(pr.getToRef().getRepository().getId());
    }

    // static methods for getting by id, etc.
    public static PullRequestDisapproval getPullRequestDisapproval(ActiveObjects ao, PullRequest pr)
        throws SQLException {
        Repository repo = pr.getToRef().getRepository();
        PullRequestDisapproval[] disapprovals =
            ao.find(PullRequestDisapproval.class,
                Query.select().where("REPO_ID = ? and PR_ID = ?", repo.getId(), pr.getId()));
        if (disapprovals.length == 0) {
            PullRequestDisapproval prd =
                ao.create(PullRequestDisapproval.class, new DBParam("REPO_ID", repo.getId()),
                    new DBParam("PR_ID", pr.getId()), new DBParam("USERNAME", "None"));
            prd.save();
            return prd;
        }
        return disapprovals[0];
    }

    public static void setPullRequestDisapproval(ActiveObjects ao, PullRequest pr, String username,
        boolean isDisapproved) throws SQLException {
        Repository repo = pr.getToRef().getRepository();
        PullRequestDisapproval[] disapprovals =
            ao.find(PullRequestDisapproval.class,
                Query.select().where("REPO_ID = ? and PR_ID = ?", repo.getId(), pr.getId()));
        if (disapprovals.length == 0) {
            PullRequestDisapproval prd =
                ao.create(PullRequestDisapproval.class, new DBParam("REPO_ID", repo.getId()),
                    new DBParam("PR_ID", pr.getId()), new DBParam("USERNAME", username));
            prd.setDisapproved(isDisapproved);
            prd.save();
            return;
        }
        disapprovals[0].setDisapprovedBy(username);
        disapprovals[0].setDisapproved(isDisapproved);
        disapprovals[0].save();
    }
}
