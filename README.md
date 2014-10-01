Stash Disapproval Plugin is designed to enable users to voluntarily block a
pull request (so other users cannot merge it until they are 'ready').

This provides a visual cue to the users that someone has explicitly requested
the pull request not be merged at this time.  The disapproval plugin can be
configured to require the "disapprover" to approve before the PR can be merged,
or it can be in "advisory mode" where the disapproval is not binding.

# INSTALL GUIDE

Nothing special, simply compile and install the plugin.  There are no external dependencies.

# USER GUIDE

Stash Disapproval Plugin adds an admin page to each repository.  Any repository
administrator may enable the plugin on that repository.  Once enabled, the
"Disapprove" button shows up.  To disapprove, click the link.  To remove
disapproval, the same user, or a repo admin, should click "undisapprove".
Disapprovals are auditable because changing the status also makes a comment on
the PR with the time/user information.

# DEV GUIDE

## Necessary Tools

1. [Atlassian Plugin SDK](https://developer.atlassian.com/display/DOCS/Set+up+the+Atlassian+Plugin+SDK+and+Build+a+Project) (or run `bin/invoke-sdk.sh` on Linux)
2. [Eclipse](http://eclipse.org) (or the java IDE of your choice)

## Eclipse Setup

1. Generate project files by running `atlas-mvn eclipse:eclipse`
2. Load the code formatter settings by going to File -> Import -> Preferences and loading the .epf file in code-style/
3. Finally, again under preferences, filter on "save actions" for the java editor and check the options for "format source code", "format all lines", and "organize imports".

Doing these 4 things will ensure you do not introduce unneccessary whitespace changes.

NOTE: Please ensure you add a LICENSE block to the top of each newly added file.

## Test Plan

Currently there are no integration tests.  After major changes, the following tests should be performed manually:

* Plugin successfully loads in stash (if fails, did you forget to add a new class to atlassian-plugin.xml?)
* Enable disapproval for a repository
* Create a pull request
* Disapprove the pull request
* Pull request shows as disapproved, and cannot be merged
* A non-admin that didn't disapprove it cannot undisapprove it
* Undisapprove it
* The pull request can now be merged

# TODO

* Support for multiple individuals disapproving, and each needing to remove their disapproval before it can be merged (i.e. the complete mirror of approval)
* Implement git-flow (https://bitbucket.org/atlassian/maven-jgitflow-plugin is a candidate, but doesn't work with the atlassian plugin SDK at this time, see https://bitbucket.org/atlassian/maven-jgitflow-plugin/issue/56/requires-maven-221-doesnt-work-with)
* Better Test coverage - especially integration tests

## KNOWN BUGS

* Sometimes the javascript that inserts disapproval information into the list view is janky.

# LICENSE

Stash Disapproval Plugin is released by Palantir Technologies, Inc. under the
Apache 2.0 License.  see the included LICENSE file for details.

# Similar Plugins

This plugin is similar in purpose (but written without consulting in any way)
the
[Block PullRequest plugin](https://marketplace.atlassian.com/plugins/com.bolyuba.stash.plugin.stash-block-pullrequest-plugin)
for Stash, which is a commercial and non-free alternative.

The workflow that actually inspired this plugin, however, is the "-1" and "-2"
capability built into the [Gerrit Code Review](https://code.google.com/p/gerrit/) software.
