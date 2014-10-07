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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.atlassian.stash.event.pull.PullRequestEvent;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestRef;
import com.atlassian.stash.repository.Repository;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.mergecheck.PullRequestMetadataDeRuiner;
import com.palantir.stash.disapprove.persistence.PersistenceManager;

public class PullRequestMetadataDeRuinerTest {

    private static final long ID = 100001L;

    private PullRequestMetadataDeRuiner prmd;

    @Mock
    private PersistenceManager pm;
    @Mock
    private PullRequestEvent pre;
    @Mock
    private PullRequest pr;
    @Mock
    private PullRequestRef ref;
    @Mock
    private Repository repo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        PluginLoggerFactory plf = new PluginLoggerFactory();

        Mockito.when(pre.getPullRequest()).thenReturn(pr);
        Mockito.when(pr.getFromRef()).thenReturn(ref);
        Mockito.when(pr.getToRef()).thenReturn(ref);
        Mockito.when(ref.getRepository()).thenReturn(repo);

        prmd = new PullRequestMetadataDeRuiner(pm, plf);
    }

    @Test
    public void testMetadataExists() throws Exception {

        Mockito.when(pr.getId()).thenReturn(null);
        prmd.listenToPullRequestEvents(pre);

        Mockito.verify(pm, Mockito.never()).getDisapprovalConfiguration(Mockito.any(Repository.class));
        Mockito.verify(pm, Mockito.never()).getPullRequestDisapproval(Mockito.any(PullRequest.class));
    }

    @Test
    public void testMetadataDoesntExist() throws Exception {

        Mockito.when(pr.getId()).thenReturn(ID);
        prmd.listenToPullRequestEvents(pre);

        Mockito.verify(pm).getDisapprovalConfiguration(Mockito.any(Repository.class));
        Mockito.verify(pm).getPullRequestDisapproval(Mockito.any(PullRequest.class));
    }
}
