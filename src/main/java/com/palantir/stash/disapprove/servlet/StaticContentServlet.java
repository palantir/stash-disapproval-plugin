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
package com.palantir.stash.disapprove.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.stash.exception.AuthorisationException;
import com.atlassian.stash.request.RequestManager;
import com.atlassian.stash.user.PermissionValidationService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.palantir.stash.disapprove.logger.PluginLoggerFactory;
import com.palantir.stash.disapprove.persistence.PersistenceManager;

public class StaticContentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String PREFIX = "static-resources";

    private final LoginUriProvider lup;
    private final PermissionValidationService permissionValidationService;
    private final RequestManager rm;
    private final Logger log;

    public StaticContentServlet(LoginUriProvider lup, PermissionValidationService permissionValidationService,
        PersistenceManager pm, RequestManager rm, PluginLoggerFactory lf) {
        this.permissionValidationService = permissionValidationService;
        this.log = lf.getLoggerForThis(this);
        this.lup = lup;
        this.rm = rm;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        final String user = authenticateUser(req, res);
        if (user == null) {
            // not logged in, redirect
            res.sendRedirect(lup.getLoginUri(getUri(req)).toASCIIString());
            return;
        }

        final String pathInfo = req.getPathInfo();
        OutputStream os = null;
        try {
            // The class loader that found this class will also find the static resources
            ClassLoader cl = this.getClass().getClassLoader();
            InputStream is = cl.getResourceAsStream(PREFIX + pathInfo);
            if (is == null) {
                res.sendError(404, "File " + pathInfo + " could not be found");
                return;
            }
            os = res.getOutputStream();
            //res.setContentType("text/html;charset=UTF-8");
            String contentType = URLConnection.guessContentTypeFromStream(is);
            if (contentType == null) {
                contentType = URLConnection.guessContentTypeFromName(pathInfo);
            }
            if (contentType == null) {
                contentType = "application/binary";
            }
            log.debug("Serving file " + pathInfo + " with content type " + contentType);
            res.setContentType(contentType);
            IOUtils.copy(is, os);
            /*
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = bis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            */
        } finally {
            if (os != null) {
                os.close();
            }
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

    private URI getUri(HttpServletRequest req) {
        StringBuffer builder = req.getRequestURL();
        if (req.getQueryString() != null) {
            builder.append("?");
            builder.append(req.getQueryString());
        }
        return URI.create(builder.toString());
    }

}
