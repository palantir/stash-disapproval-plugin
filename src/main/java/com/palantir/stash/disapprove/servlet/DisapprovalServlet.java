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
package com.palantir.stash.disapprove.servlet;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.stash.exception.AuthorisationException;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.request.RequestManager;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionValidationService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.google.common.collect.ImmutableMap;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.PersistenceManager;
import com.palantir.stash.disapprove.persistence.PullRequestDisapproval;

public class DisapprovalServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final LoginUriProvider lup;
    private final PermissionValidationService permissionValidationService;
    private final PullRequestService pullRequestService;
    private final PersistenceManager pm;
    private final RequestManager rm;
    private final Logger log;

    public DisapprovalServlet(LoginUriProvider lup, PermissionValidationService permissionValidationService,
        PullRequestService pullRequestService, PersistenceManager pm, RequestManager rm, PluginLoggerFactory lf) {
        this.permissionValidationService = permissionValidationService;
        this.log = lf.getLoggerForThis(this);
        this.lup = lup;
        this.pm = pm;
        this.pullRequestService = pullRequestService;
        this.rm = rm;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        final String user = authenticateUser(req, res);
        if (user == null) {
            // not logged in, redirect
            res.sendRedirect(lup.getLoginUri(getUri(req)).toASCIIString());
            return;
        }

        final String REQ_PARAMS = "repoId(int), prId(long), disapproved(true|false)";
        final Integer repoId;
        final Long prId;
        try {
            repoId = Integer.valueOf(req.getParameter("repoId"));
            prId = Long.valueOf(req.getParameter("prId"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The required paramaters are: " + REQ_PARAMS, e);
        }
        final PullRequest pr = pullRequestService.getById(repoId, prId);
        final Repository repo = pr.getToRef().getRepository();

        if (pr == null) {
            throw new IllegalArgumentException("No PR found for repo id " + repoId.toString() + " pr id "
                + prId.toString());
        }

        PullRequestDisapproval prd;
        try {
            prd = pm.getPullRequestDisapproval(pr);
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        boolean oldDisapproval = prd.isDisapproved();
        boolean disapproval;

        String disapproved = req.getParameter("disapproved");
        if (disapproved != null && disapproved.equalsIgnoreCase("true")) {
            disapproval = true;
        } else if (disapproved != null && disapproved.equalsIgnoreCase("false")) {
            disapproval = false;
        } else {
            throw new IllegalArgumentException("The required parameters are: " + REQ_PARAMS);
        }
        Writer w = res.getWriter();
        res.setContentType("application/json;charset=UTF-8");
        try {
            processDisapprovalChange(repo, user, prd, oldDisapproval, disapproval);
            //res.setContentType("text/html;charset=UTF-8");
            w.append(new JSONObject(ImmutableMap.of("disapproval", prd.isDisapproved(), "disapprovedBy",
                prd.getDisapprovedBy())).toString());
        } catch (IllegalStateException e) {
            w.append(new JSONObject(ImmutableMap.of("error", e.getMessage())).toString());
            res.setStatus(401);
        } finally {
            w.close();
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        final String user = authenticateUser(req, res);
        if (user == null) {
            // not logged in, redirect
            res.sendRedirect(lup.getLoginUri(getUri(req)).toASCIIString());
            return;
        }

        final String URL_FORMAT = "BASE_URL/REPO_ID";
        final String pathInfo = req.getPathInfo();
        final String[] parts = pathInfo.split("/");

        if (parts.length != 5) {
            throw new IllegalArgumentException("The format of the URL is " + URL_FORMAT);
        }

        final Integer repoId;
        final Long prId;
        try {
            repoId = Integer.valueOf(parts[3]);
            prId = Long.valueOf(parts[4]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The format of the URL is " + URL_FORMAT, e);
        }

        final PullRequest pr = pullRequestService.getById(repoId, prId);
        if (pr == null) {
            throw new IllegalArgumentException("No PR found for repo id " + repoId.toString() + " pr id "
                + prId.toString());
        }

        PullRequestDisapproval prd;
        try {
            prd = pm.getPullRequestDisapproval(pr);
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        try {
            Writer w = res.getWriter();
            //res.setContentType("text/html;charset=UTF-8");
            res.setContentType("application/json;charset=UTF-8");
            w.append(new JSONObject(ImmutableMap.of("disapproval", prd.isDisapproved(), "disapprovedBy",
                prd.getDisapprovedBy())).toString());
        } finally {
            res.getWriter().close();
        }
    }

    private String authenticateUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            permissionValidationService.validateAuthenticated();
        } catch (AuthorisationException notLoggedInException) {
            log.debug("User not logged in", notLoggedInException);
            return null;
        }
        StashAuthenticationContext ac = rm.getRequestContext().getAuthenticationContext();
        final String user = ac.getCurrentUser().getName();

        log.debug("User {} logged in", user);
        return user;
    }

    private void processDisapprovalChange(final Repository repo, final String user, PullRequestDisapproval prd,
        boolean oldDisapproval, boolean disapproval) throws IOException {

        if (disapproval) {
            // we are setting disapproval
            if (oldDisapproval) {
                // disapproval already set, do nothing
                log.warn("PR already disapproved by " + prd.getDisapprovedBy());
                return;
            }

            prd.setDisapprovedBy(user);
            prd.setDisapproved(true);
            prd.save();
            log.info("PR has been disapproved by " + user);
            return;
        }

        // unsetting disapproval
        if (!oldDisapproval) {
            log.warn("PR is not disapproved");
            return;
        }

        if (!user.equalsIgnoreCase(prd.getDisapprovedBy())) {
            // TODO: is user an admin?
            try {
                permissionValidationService.validateForRepository(repo, Permission.REPO_ADMIN);
            } catch (AuthorisationException e) {
                throw new IllegalStateException("User " + user + " is not able to remove disapproval set by user "
                    + prd.getDisapprovedBy());
            }
        }
        prd.setDisapproved(false);
        prd.setDisapprovedBy("None");
        prd.save();
        log.info("PR is no longer disapproved");

        // TODO: let admins undisapprove other people's disapproval?
        /*
        try {
            permissionValidationService.validateForGlobal(Permission.SYS_ADMIN);
        } catch (AuthorisationException notAdminException) {
            log.warn("User {} is not a system administrator", req.getRemoteUser());
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You do not have permission to access this page.");
            return;
        }
        */
    }

    private URI getUri(HttpServletRequest req) {
        StringBuffer builder = req.getRequestURL();
        if (req.getQueryString() != null) {
            builder.append("?");
            builder.append(req.getQueryString());
        }
        return URI.create(builder.toString());
    }
}
