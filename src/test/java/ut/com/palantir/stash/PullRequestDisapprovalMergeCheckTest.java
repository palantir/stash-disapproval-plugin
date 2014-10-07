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
package ut.com.palantir.stash;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestRef;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.scm.pull.MergeRequest;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.mergecheck.PullRequestDisapprovalMergeCheck;
import com.palantir.stash.disapprove.persistence.DisapprovalConfiguration;
import com.palantir.stash.disapprove.persistence.DisapprovalMode;
import com.palantir.stash.disapprove.persistence.PersistenceManager;
import com.palantir.stash.disapprove.persistence.PullRequestDisapproval;

public class PullRequestDisapprovalMergeCheckTest {

    @Mock
    private PersistenceManager pm;

    @Mock
    private MergeRequest mr;
    @Mock
    private PullRequest pr;
    @Mock
    private PullRequestRef toRef;
    @Mock
    private Repository repo;
    @Mock
    private PullRequestDisapproval prd;
    @Mock
    private DisapprovalConfiguration dc;

    private PullRequestDisapprovalMergeCheck prdmc;

    private PluginLoggerFactory plf;

    @Before
    public void setUp() throws SQLException {
        plf = new PluginLoggerFactory();

        MockitoAnnotations.initMocks(this);

        Mockito.when(mr.getPullRequest()).thenReturn(pr);
        Mockito.when(pr.getToRef()).thenReturn(toRef);
        Mockito.when(toRef.getRepository()).thenReturn(repo);
        Mockito.when(pm.getPullRequestDisapproval(pr)).thenReturn(prd);
        Mockito.when(pm.getDisapprovalConfiguration(repo)).thenReturn(dc);
        Mockito.when(dc.getDisapprovalMode()).thenReturn(DisapprovalMode.STRICT_MODE);
        Mockito.when(dc.isEnabled()).thenReturn(true);

        prdmc = new PullRequestDisapprovalMergeCheck(pm, plf);
    }

    @Test
    public void testNoDisapproval() throws Exception {
        Mockito.when(prd.isDisapproved()).thenReturn(false);

        prdmc.check(mr);

        Mockito.verify(mr, Mockito.never()).veto(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testDisapproval() throws Exception {
        Mockito.when(prd.isDisapproved()).thenReturn(true);

        prdmc.check(mr);

        Mockito.verify(mr).veto(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testDisapprovalWhenDisabled() throws Exception {
        Mockito.when(prd.isDisapproved()).thenReturn(true);
        Mockito.when(dc.isEnabled()).thenReturn(false);

        prdmc.check(mr);

        Mockito.verify(mr, Mockito.never()).veto(Mockito.anyString(), Mockito.anyString());
    }
}
