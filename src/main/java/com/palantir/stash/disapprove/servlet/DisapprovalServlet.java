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

import org.slf4j.Logger;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.stash.exception.AuthorisationException;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.atlassian.stash.request.RequestManager;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionValidationService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.DisapprovalConfiguration;
import com.palantir.stash.disapprove.persistence.PersistenceManager;
import com.palantir.stash.disapprove.persistence.PullRequestDisapproval;

public class DisapprovalServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final LoginUriProvider lup;
    private final PermissionValidationService permissionValidationService;
    private final RepositoryService repositoryService;
    private final PullRequestService pullRequestService;
    private final PersistenceManager pm;
    private final RequestManager rm;
    private final Logger log;

    public DisapprovalServlet(LoginUriProvider lup, PermissionValidationService permissionValidationService,
        RepositoryService repositoryService, PullRequestService pullRequestService, PersistenceManager pm,
        RequestManager rm, PluginLoggerFactory lf) {
        this.permissionValidationService = permissionValidationService;
        this.log = lf.getLoggerForThis(this);
        this.lup = lup;
        this.pm = pm;
        this.repositoryService = repositoryService;
        this.pullRequestService = pullRequestService;
        this.rm = rm;
    }

    public void doOldGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PullRequest pr = pullRequestService.getById(1, 1);
        Repository repo = pr.getToRef().getRepository();

        try {
            DisapprovalConfiguration dc = pm.getDisapprovalConfiguration(repo);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        try {
            PullRequestDisapproval prd = pm.getPullRequestDisapproval(pr);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        // Authenticate user
        try {
            permissionValidationService.validateAuthenticated();
        } catch (AuthorisationException notLoggedInException) {
            log.debug("User not logged in, redirecting to login page");
            // not logged in, redirect
            res.sendRedirect(lup.getLoginUri(getUri(req)).toASCIIString());
            return;
        }
        StashAuthenticationContext ac = rm.getRequestContext().getAuthenticationContext();
        final String user = ac.getCurrentUser().getName();

        //final String user = req.getRemoteUser();
        log.debug("User {} logged in", user);

        final String URL_FORMAT = "BASE_URL/REPO_ID/PR_ID/[disapproved: true|false]";
        final String pathInfo = req.getPathInfo();
        final String[] parts = pathInfo.split("/");

        log.debug("PATH INFO: " + pathInfo);
        if (parts.length != 4) {
            throw new IllegalArgumentException("The format of the URL is " + URL_FORMAT);
        }

        final Integer repoId;
        final Long prId;
        try {
            repoId = Integer.valueOf(parts[1]);
            prId = Long.valueOf(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The format of the URL is " + URL_FORMAT, e);
        }

        PullRequest pr = pullRequestService.getById(repoId, prId);
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
        if (parts[3].equalsIgnoreCase("true")) {
            disapproval = true;
        } else if (parts[3].equalsIgnoreCase("false")) {
            disapproval = false;
        } else {
            throw new IllegalArgumentException("The format of the URL is " + URL_FORMAT);
        }

        res.setContentType("text/html;charset=UTF-8");
        Writer w = res.getWriter();

        if (disapproval) {
            // we are setting disapproval
            if (oldDisapproval) {
                // disapproval already set, do nothing
                w.append("PR already disapproved by " + prd.getDisapprovedBy());
                w.close();
                return;
            }

            prd.setDisapprovedBy(user);
            prd.setDisapproved(true);
            prd.save();
            w.append("PR has been disapproved by " + user);
            w.close();
            return;
        }

        // unsetting disapproval
        if (!oldDisapproval) {
            w.append("PR is not disapproved");
            w.close();
            return;
        }

        prd.setDisapproved(false);
        prd.setDisapprovedBy("None");
        prd.save();
        w.append("PR is no longer disapproved");
        w.close();
        return;

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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            permissionValidationService.validateForGlobal(Permission.SYS_ADMIN);
        } catch (AuthorisationException e) {
            // Skip form processing
            doGet(req, res);
            return;
        }

        //String name = req.getParameter("name");

        try {
            //configurationPersistanceManager.setJenkinsServerConfigurationFromRequest(req);
            //pluginUserManager.createStashbotUser(configurationPersistanceManager.getJenkinsServerConfiguration(name));
        } catch (Exception e) {
            res.sendRedirect(req.getRequestURL().toString() + "?error=" + e.getMessage());
        }
        doGet(req, res);
    }

    private URI getUri(HttpServletRequest req) {
        StringBuffer builder = req.getRequestURL();
        if (req.getQueryString() != null) {
            builder.append("?");
            builder.append(req.getQueryString());
        }
        return URI.create(builder.toString());
    }

    private Repository getRepository(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            return null;
        }
        pathInfo = pathInfo.startsWith("/") ? pathInfo.substring(0) : pathInfo;
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length != 3) {
            return null;
        }
        return repositoryService.getBySlug(pathParts[1], pathParts[2]);
    }
}
