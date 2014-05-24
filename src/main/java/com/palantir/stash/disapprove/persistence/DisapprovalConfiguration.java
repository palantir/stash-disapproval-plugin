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

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Implementation;
import net.java.ao.Mutator;
import net.java.ao.Preload;
import net.java.ao.schema.Default;
import net.java.ao.schema.Ignore;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;

@Table("DPConfig001")
@Preload
@Implementation(DisapprovalConfigurationImpl.class)
public interface DisapprovalConfiguration extends Entity {

    @NotNull
    @Default(DisapprovalMode.Constants.STRICT_VALUE)
    @Accessor("MODE_STR")
    public String getDisapprovalModeStr();

    @Mutator("MODE_STR")
    public void setDisapprovalModeStr(String mode);

    @NotNull
    @Unique
    @Accessor("REPO_ID")
    public Integer getRepositoryId();

    @Mutator("REPO_ID")
    public void setRepositoryId(Integer repoId);

    /////
    // These are implemented in DisapprovalPluginConfigurationImpl - so the user can use enums
    /////
    @Ignore
    public DisapprovalMode getDisapprovalMode();

    @Ignore
    public void setDisapprovalMode(DisapprovalMode mode);

    @Ignore
    public Repository getRepository(RepositoryService rs);

    @Ignore
    public void setRepository(Repository repo);

}
