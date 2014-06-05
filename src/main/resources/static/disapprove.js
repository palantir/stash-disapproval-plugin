require(['model/page-state', 'util/navbuilder', 'jquery'], function(state, nav, $) {
    //alert("foo4")
    //console.log(state);
    //alert(state);
	//baseUrl = nav.pluginServlets().build()
    //alert("bar: " + baseUrl)
    
    window.onload = function() {
    	baseUrl = nav.pluginServlets().build()
        repoId = state.getPullRequest().attributes.toRef.attributes.repository.id;
        commit = state.getPullRequest().attributes.fromRef.attributes.latestChangeset;
	    prId = state.getPullRequest().attributes.id;
	    version = state.getPullRequest().attributes.version;
        var prd = $.get(baseUrl + '/disapproval/disapprove/' + repoId + '/' + prId)
        
        // Surface to user who disapproved the request and make its status more obvious
        $(".undisapprove-pull-request").css({"color":"#ff0000"})
        
        prd.success(function(data) {
        	//alert("Got data: " + JSON.stringify(data))
        	if (data.disapproved) {
        		console.log("Pull Request is Disapproved by " + data.disapprovedBy)
        	} else {
        		console.log("Pull Request NOT Disapproved")
        	}
	        var upr = $(".undisapprove-pull-request")
	        upr.html(upr.html() + " <small>(by " + data.disapprovedBy + ")</small>")
        });
        
                
        pr = state.getPullRequest();
        
        button = document.getElementsByClassName('disapprove-pull-request');
        
        if (button.length > 0) {
            button = button[0];
            button.html
            button.onclick = function() {
                //var jqxhr = $.get(baseUrl + '/disapproval/disapprove/' + repoId + '/' + prId + '/true');
                var jqxhr = $.post(baseUrl + '/disapproval/disapprove', {"repoId":repoId, "prId":prId, "disapproved":"true"});
                jqxhr.done(function(data) {
	                console.log("success: " + JSON.stringify(data));
                	window.location.reload(true);
                });
                jqxhr.fail(function(data) {
	                alert("fail: " + JSON.stringify(data));
                });
                jqxhr.always(function(data) {
	                //alert("always!");
                });
            }
        }
        
        button = document.getElementsByClassName('undisapprove-pull-request');
        
        if (button.length > 0) {
            button = button[0];
            button.onclick = function() {
                //var jqxhr = $.get(baseUrl + '/disapproval/disapprove/' + repoId + '/' + prId + '/false');
                var jqxhr = $.post(baseUrl + '/disapproval/disapprove', {"repoId":repoId, "prId":prId, "disapproved":"false"});
                jqxhr.done(function(data) {
	                console.log("success: " + JSON.stringify(data));
                	window.location.reload(true);
                });
                jqxhr.fail(function(data) {
	                alert("fail: " + JSON.stringify(data));
                });
                jqxhr.always(function(data) {
	                //alert("always!");
                });
            }
        }
    }
})
