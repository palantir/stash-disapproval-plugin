require(['model/page-state', 'util/navbuilder', 'jquery'], function(state, nav, $) {

	// http://stackoverflow.com/questions/6285491/are-there-universal-alternatives-to-window-onload-without-using-frameworks
	// Decided to do with jquery, to avoid messing up the window.onload
	$(document).ready(function() {
    	$(".disapproval-face").css({"color":"#AA0000", "font-weight":"bolder"})
    	
    	
    	baseUrl = nav.pluginServlets().build()
        repoId = state.getPullRequest().attributes.toRef.attributes.repository.id;
        commit = state.getPullRequest().attributes.fromRef.attributes.latestChangeset;
	    prId = state.getPullRequest().attributes.id;
	    version = state.getPullRequest().attributes.version;
        prd = $.get(baseUrl + '/disapproval/disapprove/' + repoId + '/' + prId)
        
        // Surface to user who disapproved the request and make its status more obvious
        prd.success(function(data) {
        	if (data.disapproved) {
        		console.log("Pull Request is Disapproved by " + data.disapprovedBy)
        	} else {
        		console.log("Pull Request NOT Disapproved")
        	}
	        var upr = $(".disapproval-face")
	        upr.html(upr.html() + " <small>(by " + data.disapprovedBy + ")</small>")
        });
        prd.fail(function(data) {
        	if (data.responseJSON.error) {
        		alert("Could not fetch disapproval configuration: " + data.responseJSON.error)
        	} else {
        		alert("Fail: " + JSON.stringify(data));
            }
        });
        
        
                
        pr = state.getPullRequest();
        
        button = document.getElementsByClassName('disapprove-pull-request');
        
        if (button.length > 0) {
            button = button[0];
            button.html
            button.onclick = function() {
                var jqxhr = $.post(baseUrl + '/disapproval/disapprove', {"repoId":repoId, "prId":prId, "disapproved":"true"});
                jqxhr.done(function(data) {
	                console.log("success: " + JSON.stringify(data));
                	window.location.reload(true);
                });
                jqxhr.fail(function(data) {
                	if (data.responseJSON.error) {
                		alert("Could not disapprove PR: " + data.responseJSON.error)
                	} else {
		                alert("fail: " + JSON.stringify(data));
		            }
                });
            }
        }
        
        button = document.getElementsByClassName('undisapprove-pull-request');
        
        if (button.length > 0) {
            button = button[0];
            button.onclick = function() {
                var jqxhr = $.post(baseUrl + '/disapproval/disapprove', {"repoId":repoId, "prId":prId, "disapproved":"false"});
                jqxhr.done(function(data) {
	                console.log("success: " + JSON.stringify(data));
                	window.location.reload(true);
                });
                jqxhr.fail(function(data) {
                	if (data.responseJSON.error) {
                		alert("Could not remove disapproval for PR: " + data.responseJSON.error)
                	} else {
		                alert("fail: " + JSON.stringify(data));
		            }
                });
            }
        }
    })
})
