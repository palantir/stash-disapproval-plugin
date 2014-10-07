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

import net.java.ao.DBParam;
import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;

public class DisapprovalConfigurationImpl {

    private final DisapprovalConfiguration dpc;

    public DisapprovalConfigurationImpl(DisapprovalConfiguration dpc) {
        this.dpc = dpc;
    }

    public DisapprovalMode getDisapprovalMode() {
        String modestr = dpc.getDisapprovalModeStr();
        return DisapprovalMode.fromMode(modestr);
    }

    public void setDisapprovalMode(DisapprovalMode authMode) {
        dpc.setDisapprovalModeStr(authMode.getMode());
    }

    public Repository getRepository(RepositoryService rs) {
        return rs.getById(dpc.getRepositoryId());
    }

    public void setRepository(Repository repo) {
        dpc.setRepositoryId(repo.getId());
    }

    // static methods for getting by id, etc.
    public static DisapprovalConfiguration getByRepository(final ActiveObjects ao, final Repository repo)
        throws SQLException {
        DisapprovalConfiguration[] configs =
            ao.find(DisapprovalConfiguration.class, Query.select().where("REPO_ID = ?", repo.getId()));
        if (configs.length == 0) {
            DisapprovalConfiguration dpc =
                ao.create(DisapprovalConfiguration.class, new DBParam("REPO_ID", repo.getId()));
            dpc.save();
            configs = ao.find(DisapprovalConfiguration.class, Query.select().where("REPO_ID = ?", repo.getId()));
            if (configs.length == 0) {
                throw new IllegalStateException("Failed to create a DisapprovalConfiguration for unknown reason");
            }
            return configs[0];
        }
        return configs[0];
    }
}
