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
package com.palantir.stash.disapprove.config;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.stash.exception.AuthorisationException;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionValidationService;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.collect.ImmutableMap;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.DisapprovalConfiguration;
import com.palantir.stash.disapprove.persistence.PersistenceManager;

public class DisapproveConfigurationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final SoyTemplateRenderer soyTemplateRenderer;
    private final PageBuilderService pageBuilderService;
    private final LoginUriProvider lup;
    private final PermissionValidationService permissionValidationService;
    private final RepositoryService repositoryService;
    private final PersistenceManager pm;
    private final Logger log;

    public DisapproveConfigurationServlet(SoyTemplateRenderer soyTemplateRenderer,
        PageBuilderService pageBuilderService, LoginUriProvider lup,
        PermissionValidationService permissionValidationService, RepositoryService repositoryService,
        PersistenceManager pm, PluginLoggerFactory lf) {
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.pageBuilderService = pageBuilderService;
        this.permissionValidationService = permissionValidationService;
        this.log = lf.getLoggerForThis(this);
        this.lup = lup;
        this.pm = pm;
        this.repositoryService = repositoryService;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Repository repo = getRepository(req);

        // Authenticate user
        try {
            permissionValidationService.validateAuthenticated();
        } catch (AuthorisationException notLoggedInException) {
            log.debug("User not logged in, redirecting to login page");
            // not logged in, redirect
            res.sendRedirect(lup.getLoginUri(getUri(req)).toASCIIString());
            return;
        }
        log.debug("User {} logged in", req.getRemoteUser());
        try {
            permissionValidationService.validateForGlobal(Permission.SYS_ADMIN);
        } catch (AuthorisationException notAdminException) {
            log.warn("User {} is not a system administrator", req.getRemoteUser());
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You do not have permission to access this page.");
            return;
        }

        res.setContentType("text/html;charset=UTF-8");
        try {
            // Build select data for disapproval modes
            //ImmutableList.Builder<ImmutableMap<String, String>> disapprovalModes = ImmutableList.Builder<ImmutableMap<String,String>>();

            /*
            
            DisapprovalMode.getSelectList(dc)

            for (JenkinsServerConfiguration jsc : configurationPersistanceManager.getAllJenkinsServerConfigurations()) {
                AuthenticationMode am = jsc.getAuthenticationMode();
                ImmutableList<ImmutableMap<String, String>> selectList = AuthenticationMode.getSelectList(am);

                authDataBuilder.put(jsc.getName(), selectList);

                // For convenience, store the value of the selected field in a separate map
                authDataSelectedBuilder.put(jsc.getName(),
                    jsc.getAuthenticationMode().getSelectListEntry(false).get("value"));

            }
            */
            DisapprovalConfiguration dc = pm.getDisapprovalConfiguration(repo);
            pageBuilderService.assembler().resources().requireContext("plugin.page.disapproval");
            soyTemplateRenderer.render(res.getWriter(),
                // This key is the POM groupId + "." + the POM artifactID + ":" + the atlassian-plugin.xml resource name
                "com.palantir.stash.stash-disapprove-plugin:disapprovalServletResources",
                "plugin.page.disapproval.repositoryConfigurationPanel",
                ImmutableMap.<String, Object> builder()
                    .put("repository", repo)
                    .put("disapprovalConfiguration", dc)
                    .put("isStrict", "foo")
                    //.put("isStrict", dc.getDisapprovalMode().equals(DisapprovalMode.STRICT_MODE) ? "true" : "")
                    .build()
                );
        } catch (SoyException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new ServletException(e);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
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
