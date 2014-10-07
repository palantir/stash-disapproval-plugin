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
package ut.com.palantir.stash.conditions;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.atlassian.stash.pull.PullRequest;
import com.google.common.collect.ImmutableMap;
import com.palantir.stash.disapprove.conditions.IsPullRequestDisapproved;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.PersistenceManager;
import com.palantir.stash.disapprove.persistence.PullRequestDisapproval;

public class IsPullRequestDisapprovedTest {

    @Mock
    private PersistenceManager pm;
    @Mock
    private PullRequest truePR;
    @Mock
    private PullRequest falsePR;
    @Mock
    private PullRequestDisapproval truePRD;
    @Mock
    private PullRequestDisapproval falsePRD;

    private IsPullRequestDisapproved iprd;
    private PluginLoggerFactory plf;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        Mockito.when(pm.getPullRequestDisapproval(truePR)).thenReturn(truePRD);
        Mockito.when(pm.getPullRequestDisapproval(falsePR)).thenReturn(falsePRD);
        Mockito.when(truePRD.isDisapproved()).thenReturn(true);
        Mockito.when(falsePRD.isDisapproved()).thenReturn(false);

        plf = new PluginLoggerFactory();

        iprd = new IsPullRequestDisapproved(pm, plf);
    }

    @Test
    public void testPRD() {

        iprd.init(ImmutableMap.of("", ""));

        Map<String, Object> trueContext = ImmutableMap.of("pullRequest", (Object) truePR);
        Assert.assertTrue(iprd.shouldDisplay(trueContext));

        Map<String, Object> falseContext = ImmutableMap.of("pullRequest", (Object) falsePR);
        Assert.assertFalse(iprd.shouldDisplay(falseContext));
    }
}
