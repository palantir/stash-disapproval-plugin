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

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Implementation;
import net.java.ao.Mutator;
import net.java.ao.Preload;
import net.java.ao.schema.Default;
import net.java.ao.schema.Ignore;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;

@Table("PRDisapproval001")
@Preload
@Implementation(PullRequestDisapprovalImpl.class)
public interface PullRequestDisapproval extends Entity {

    @NotNull
    @Accessor("REPO_ID")
    public Integer getRepositoryId();

    @Mutator("REPO_ID")
    public void setRepositoryId(Integer repoId);

    @NotNull
    @Accessor("PR_ID")
    public Long getPullRequestId();

    @Mutator("PR_ID")
    public void setPullRequestId(Long prId);

    @NotNull
    @Default("None")
    @Accessor("USERNAME")
    public String getDisapprovedBy();

    @Mutator("USERNAME")
    public void setDisapprovedBy(String user);

    @NotNull
    @Default("false")
    @Accessor("DISAPPROVED")
    public Boolean isDisapproved();

    @Mutator("DISAPPROVED")
    public void setDisapproved(Boolean isDissaproved);

    /////
    // These are implemented in DisapprovalPluginConfigurationImpl - so the user can use enums
    /////
    @Ignore
    public Repository getRepository(RepositoryService rs);

    @Ignore
    public void setRepository(Repository repo);

    @Ignore
    public PullRequest getPullRequest(PullRequestService prs);

    // sets both repo and pull request id, this is the API most people should use.
    @Ignore
    public void setPullRequest(PullRequest pr);
}
