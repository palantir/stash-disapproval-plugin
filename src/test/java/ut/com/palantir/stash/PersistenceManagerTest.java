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

import javax.servlet.http.HttpServletRequest;

import junit.framework.Assert;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.jdbc.DynamicJdbcConfiguration;
import net.java.ao.test.jdbc.Jdbc;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ut.com.palantir.stash.PersistenceManagerTest.DataStuff;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestRef;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.DisapprovalConfiguration;
import com.palantir.stash.disapprove.persistence.DisapprovalMode;
import com.palantir.stash.disapprove.persistence.PersistenceManager;
import com.palantir.stash.disapprove.persistence.PullRequestDisapproval;

@RunWith(ActiveObjectsJUnitRunner.class)
@Jdbc(DynamicJdbcConfiguration.class)
@Data(DataStuff.class)
public class PersistenceManagerTest {

    private static final Long PR_ID = 1234L;
    private static final Integer REPO_ID = 1235;
    private static final String FROM_SHA = "8e57a8b77501710fe1e30a3500102c0968763107";
    private static final String TO_SHA = "beefbeef7501710fe1e30a3500102c0968763107";

    private EntityManager entityManager;
    private ActiveObjects ao;
    private PersistenceManager cpm;

    @Mock
    private PullRequestService prs;
    @Mock
    private RepositoryService rs;

    @Mock
    private PullRequestRef fromRef;
    @Mock
    private PullRequestRef toRef;
    @Mock
    private PullRequest pr;
    @Mock
    private Repository repo;
    @Mock
    private HttpServletRequest req;

    private final PluginLoggerFactory lf = new PluginLoggerFactory();

    public static class DataStuff implements DatabaseUpdater {

        @SuppressWarnings("unchecked")
        @Override
        public void update(EntityManager entityManager) throws Exception {
            entityManager.migrate(DisapprovalConfiguration.class, PullRequestDisapproval.class);
        }

    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ensure our runner sets this for us
        Assert.assertNotNull(entityManager);

        Mockito.when(pr.getToRef()).thenReturn(toRef);
        Mockito.when(pr.getFromRef()).thenReturn(fromRef);
        Mockito.when(pr.getId()).thenReturn(PR_ID);
        Mockito.when(toRef.getRepository()).thenReturn(repo);
        Mockito.when(toRef.getLatestChangeset()).thenReturn(TO_SHA);
        Mockito.when(fromRef.getLatestChangeset()).thenReturn(FROM_SHA);
        Mockito.when(repo.getId()).thenReturn(REPO_ID);

        Mockito.when(prs.getById(Mockito.anyInt(), Mockito.anyLong())).thenReturn(pr);
        Mockito.when(rs.getById(Mockito.anyInt())).thenReturn(repo);

        ao = new TestActiveObjects(entityManager);

        cpm = new PersistenceManager(ao, lf);
    }

    @Test
    public void testPullRequestDisapproval() throws Exception {

        int sizeOfData = ao.count(PullRequestDisapproval.class);

        cpm.setPullRequestDisapproval(pr, "someuser", true);

        // Assert a new row was added
        Assert.assertEquals(sizeOfData + 1, ao.count(PullRequestDisapproval.class));

        PullRequestDisapproval prd = cpm.getPullRequestDisapproval(pr);

        Assert.assertTrue(prd.isDisapproved());
        Assert.assertEquals(prd.getDisapprovedBy(), "someuser");

        cpm.setPullRequestDisapproval(pr, "someuser", false);

        // Assert the same row was modified
        Assert.assertEquals(sizeOfData + 1, ao.count(PullRequestDisapproval.class));

        prd = cpm.getPullRequestDisapproval(pr);
        Assert.assertFalse(prd.isDisapproved());
    }

    @Test
    public void testDisapprovalConfiguration() throws Exception {

        int sizeOfData = ao.count(DisapprovalConfiguration.class);

        Mockito.when(req.getParameter("strictModeEnabled")).thenReturn(null);

        cpm.setDisapprovalConfigurationFromRequest(repo, req);

        Assert.assertEquals(sizeOfData + 1, ao.count(DisapprovalConfiguration.class));

        DisapprovalConfiguration dc = cpm.getDisapprovalConfiguration(repo);

        Assert.assertEquals(dc.getDisapprovalMode(), DisapprovalMode.ADVISORY_MODE);
        Assert.assertEquals(dc.getRepository(rs), repo);

        dc = cpm.getDisapprovalConfiguration(repo);
    }

    @Test
    public void testDisapprovalConfiguration2() throws Exception {

        int sizeOfData = ao.count(DisapprovalConfiguration.class);

        Mockito.when(req.getParameter("strictModeEnabled")).thenReturn(DisapprovalMode.Constants.STRICT_VALUE);

        cpm.setDisapprovalConfigurationFromRequest(repo, req);

        Assert.assertEquals(sizeOfData + 1, ao.count(DisapprovalConfiguration.class));

        DisapprovalConfiguration dc = cpm.getDisapprovalConfiguration(repo);

        Assert.assertEquals(dc.getDisapprovalMode(), DisapprovalMode.STRICT_MODE);
        Assert.assertEquals(dc.getRepository(rs), repo);

        dc = cpm.getDisapprovalConfiguration(repo);
    }

    @Test
    public void testDisapprovalConfigurationGetCreates() throws Exception {

        int sizeOfData = ao.count(DisapprovalConfiguration.class);

        DisapprovalConfiguration dc = cpm.getDisapprovalConfiguration(repo);

        Assert.assertNotNull(dc);
        Assert.assertEquals(sizeOfData + 1, ao.count(DisapprovalConfiguration.class));
    }

    @Test
    public void testDisapprovalConfigurationImpl() throws Exception {

        DisapprovalConfiguration dc = cpm.getDisapprovalConfiguration(repo);

        DisapprovalMode dm = dc.getDisapprovalMode();

        Assert.assertEquals(DisapprovalMode.STRICT_MODE, dm);

        dc.setDisapprovalMode(DisapprovalMode.ADVISORY_MODE);
        dc.save();

        dc = cpm.getDisapprovalConfiguration(repo);

        Assert.assertEquals(DisapprovalMode.ADVISORY_MODE, dc.getDisapprovalMode());

    }
}
