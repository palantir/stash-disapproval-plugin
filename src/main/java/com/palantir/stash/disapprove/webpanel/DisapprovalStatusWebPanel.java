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
package com.palantir.stash.disapprove.webpanel;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;

import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.stash.pull.PullRequest;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.PersistenceManager;
import com.palantir.stash.disapprove.persistence.PullRequestDisapproval;

public class DisapprovalStatusWebPanel implements WebPanel {

    private final PersistenceManager pm;
    private final Logger log;

    public DisapprovalStatusWebPanel(PersistenceManager pm, PluginLoggerFactory lf) {
        this.pm = pm;
        this.log = lf.getLoggerForThis(this);
    }

    @Override
    public String getHtml(Map<String, Object> context) {
        Writer holdSomeText = new StringWriter();
        try {
            writeHtml(holdSomeText, context);
        } catch (IOException e) {
            log.error("Error occured rendering web panel", e);
            return "Error occured loading text";
        }
        return holdSomeText.toString();
    }

    @Override
    public void writeHtml(Writer writer, Map<String, Object> context) throws IOException {
        final String DISAPPROVED_HTML = "<font color=\"#AA0000\">ಠ_ಠ</font> by __USER__";
        final String UNDISAPPROVED_HTML = "<font color=\"#00AA00\">( ͡° ͜ʖ ͡°)</font> by __USER__";
        try {
            // TODO: need this?  Repository repo = (Repository) context.get("repository");
            PullRequest pr = (PullRequest) context.get("pullRequest");
            PullRequestDisapproval prd = pm.getPullRequestDisapproval(pr);

            log.error("TRACE TRACE:");
            if (prd.isDisapproved()) {
                writer.append(DISAPPROVED_HTML.replace("__USER__", prd.getDisapprovedBy()));
            } else {
                writer.append(UNDISAPPROVED_HTML.replace("__USER__", prd.getDisapprovedBy()));
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
