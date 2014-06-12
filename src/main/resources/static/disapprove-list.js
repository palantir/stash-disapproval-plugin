require(['model/page-state', 'util/navbuilder', 'jquery'], function(state, nav, $) {
	
	window.onload = function() {
	
		disapprovalEnabled = true
		headerNeeded = "unknown"
		baseUrl = nav.pluginServlets().build()
        repoId = state.getRepository().id;
        
		tables = $("table tbody")
		tables.each(function() {
			console.log("AAA")
			$(this).find('tr').each(function() {
				var prId = $(this).attr("data-pullrequestid")
		        prd = $.get(baseUrl + '/disapproval/disapprove/' + repoId + '/' + prId)
		        
				reviewers = $(this).children('.reviewers')
		        prd.success(function(data) {
		        
			        // If things are not enabled, immediately bail out
			        disapprovalEnabled = data.enabledForRepo
			        if (disapprovalEnabled) {
			        	headerNeeded = "no"
			        	return;
		        	} else {
		        		headerNeeded = "yes"
	        		}
		        	
		        	console.log(JSON.stringify(data))
		        	disapprovalHtml = "";
		        	if (data.disapproval) {
		        		console.log("Pull Request is Disapproved by " + data.disapprovedBy)
						disapprovalHtml = "<td class=\"reviewers\"><font color=\"#AA0000\">ಠ_ಠ</font></td>"
		        	} else {
		        		console.log("Pull Request NOT Disapproved")
		        	}
					reviewers.before(disapprovalHtml)
				})
			})
		})
		
        // Add the header if it needs to be added
        $(document).ajaxComplete(function() {
        	console.log("I'm here")
        	if (headerNeeded == "yes") {
				tableHead = $("table thead")
				tableHead.each(function() {
					$(this).find('tr').each(function() {
						reviewers = $(this).children('.reviewers')
						reviewers.before('<th class="reviewers">Disapproval</th>')
					})
				})
			}
		})
	}
})
		
//console.log("ROW NAME: " + this.id)
//$('<td/>').append("foo")
//})
//console.log("PRT: " + JSON.stringify(table))

//    //console.log(state);
//    //alert(state);
//	//baseUrl = nav.pluginServlets().build()
//    //alert("bar: " + baseUrl)
//    
//    window.onload = function() {
//    	//$(".disapproval-face").css({"color":"#AA0000", "font-weight":"1000"})
//    	$(".disapproval-face").css({"color":"#AA0000", "font-weight":"bolder"})
//    	
//    	
//    	baseUrl = nav.pluginServlets().build()
//        repoId = state.getPullRequest().attributes.toRef.attributes.repository.id;
//        commit = state.getPullRequest().attributes.fromRef.attributes.latestChangeset;
//	    prId = state.getPullRequest().attributes.id;
//	    version = state.getPullRequest().attributes.version;
//        prd = $.get(baseUrl + '/disapproval/disapprove/' + repoId + '/' + prId)
//        
//        // Surface to user who disapproved the request and make its status more obvious
//        prd.success(function(data) {
//        	//alert("Got data: " + JSON.stringify(data))
//        	if (data.disapproved) {
//        		console.log("Pull Request is Disapproved by " + data.disapprovedBy)
//        	} else {
//        		console.log("Pull Request NOT Disapproved")
//        	}
//	        //var upr = $(".undisapprove-pull-request")
//	        var upr = $(".disapproval-face")
//	        upr.html(upr.html() + " <small>(by " + data.disapprovedBy + ")</small>")
//	        
//        });
//        
//        
//                
//        pr = state.getPullRequest();
//        
//        button = document.getElementsByClassName('disapprove-pull-request');
//        
//        if (button.length > 0) {
//            button = button[0];
//            button.html
//            button.onclick = function() {
//                //var jqxhr = $.get(baseUrl + '/disapproval/disapprove/' + repoId + '/' + prId + '/true');
//                var jqxhr = $.post(baseUrl + '/disapproval/disapprove', {"repoId":repoId, "prId":prId, "disapproved":"true"});
//                jqxhr.done(function(data) {
//	                console.log("success: " + JSON.stringify(data));
//                	window.location.reload(true);
//                });
//                jqxhr.fail(function(data) {
//                	if (data.responseJSON.error) {
//                		alert("Could not disapprove PR: " + data.responseJSON.error)
//                	} else {
//		                alert("fail: " + JSON.stringify(data));
//		            }
//                });
//                jqxhr.always(function(data) {
//	                //alert("always!");
//                });
//            }
//        }
//        
//        button = document.getElementsByClassName('undisapprove-pull-request');
//        
//        if (button.length > 0) {
//            button = button[0];
//            button.onclick = function() {
//                //var jqxhr = $.get(baseUrl + '/disapproval/disapprove/' + repoId + '/' + prId + '/false');
//                var jqxhr = $.post(baseUrl + '/disapproval/disapprove', {"repoId":repoId, "prId":prId, "disapproved":"false"});
//                jqxhr.done(function(data) {
//	                console.log("success: " + JSON.stringify(data));
//                	window.location.reload(true);
//                });
//                jqxhr.fail(function(data) {
//                	if (data.responseJSON.error) {
//                		alert("Could not remove disapproval for PR: " + data.responseJSON.error)
//                	} else {
//		                alert("fail: " + JSON.stringify(data));
//		            }
//                });
//                jqxhr.always(function(data) {
//	                //alert("always!");
//                });
//            }
//        }
//    }
//})
