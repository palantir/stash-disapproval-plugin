package ut.com.palantir.stash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.stash.request.RequestContext;
import com.atlassian.stash.request.RequestManager;
import com.atlassian.stash.user.PermissionValidationService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.PersistenceManager;
import com.palantir.stash.disapprove.servlet.StaticContentServlet;

public class StaticContentServletTest {

    private static final String TEST_FILE_PATH = "/test.txt";
    private static final String TEST_FILE_CONTENTS = "This is a test.\n";
    private static final String USERNAME = "someuser";
    private static final String REQUEST_URL =
        "http://localhost:2990/stash/plugins/servlet/disapproval/static-content/test.txt";

    private StaticContentServlet scs;

    @Mock
    private LoginUriProvider lup;
    @Mock
    private PersistenceManager pm;
    @Mock
    private PermissionValidationService pvs;
    @Mock
    private RequestManager rm;

    final private PluginLoggerFactory plf = new PluginLoggerFactory();
    final private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Mock
    private HttpServletRequest req;
    @Mock
    private HttpServletResponse res;
    @Mock
    private RequestContext rc;
    @Mock
    private StashAuthenticationContext sac;
    @Mock
    private StashUser su;
    @Mock
    private ServletOutputStream sos;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        MockitoAnnotations.initMocks(this);

        Mockito.when(rm.getRequestContext()).thenReturn(rc);
        Mockito.when(rc.getAuthenticationContext()).thenReturn(sac);
        Mockito.when(sac.getCurrentUser()).thenReturn(su);
        Mockito.when(su.getName()).thenReturn(USERNAME);
        Mockito.when(req.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(lup.getLoginUri(Mockito.any(URI.class))).thenReturn(new URI(REQUEST_URL));

        Answer<Void> delegate = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments().length == 3) {
                    baos.write((byte[]) invocation.getArguments()[0], (int) invocation.getArguments()[1],
                        (int) invocation.getArguments()[2]);

                } else if (invocation.getArguments().length == 1) {
                    baos.write((byte[]) invocation.getArguments()[0]);
                } else {
                    Assert.fail("Called an unsupported form of OutputStream.write(...) - fix the test!");
                }
                return null;
            }
        };
        Mockito.doAnswer(delegate).when(sos).write(Mockito.anyInt());
        Mockito.doAnswer(delegate).when(sos).write((byte[]) Mockito.any());
        Mockito.doAnswer(delegate).when(sos).write((byte[]) Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.when(res.getOutputStream()).thenReturn(sos);

        scs = new StaticContentServlet(lup, pvs, pm, rm, plf);

    }

    @Test
    public void testStaticContentServlet() throws Exception {

        Mockito.when(req.getPathInfo()).thenReturn(TEST_FILE_PATH);

        scs.doGet(req, res);

        Assert.assertEquals(TEST_FILE_CONTENTS, baos.toString());
    }

    @Test
    public void testStaticContentServlet404() throws Exception {

        Mockito.when(req.getPathInfo()).thenReturn("/notfound/" + TEST_FILE_PATH);

        scs.doGet(req, res);

        Mockito.verify(res).sendError(Mockito.eq(404), Mockito.anyString());
    }

    @Test
    public void testStaticContentServletNotLoggedIn() throws Exception {

        Mockito.when(req.getPathInfo()).thenReturn(TEST_FILE_PATH);
        Mockito.when(su.getName()).thenReturn(null);

        scs.doGet(req, res);

        Mockito.verify(res).sendRedirect(Mockito.anyString());
    }
}
