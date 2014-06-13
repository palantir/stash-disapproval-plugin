require(['model/page-state', 'util/navbuilder', 'jquery'], function(state, nav, $) {
	
	window.onload = function() {
	
	
		compiledData = {}
		disapprovalEnabled = false
		baseUrl = nav.pluginServlets().build()
        repoId = state.getRepository().id;
        outstandingCount = 0
        callbacks = []
        
		tables = $("table tbody")
		tables.each(function() {
			$(this).find('tr').each(function() {
				var prId = $(this).attr("data-pullrequestid")
				outstandingCount += 1
		        prd = $.get(baseUrl + '/disapproval/disapprove/' + repoId + '/' + prId)
		        callbacks.push(prd)
		        
				reviewers = $(this).children('.reviewers')
		        prd.success(function(data) {
		        
			        // If things are not enabled, immediately bail out
			        disapprovalEnabled = data.enabledForRepo
			        if (!disapprovalEnabled) {
			        	return;
		        	}
		        	
		        	compiledData[prId.toString()] = data
				})
				// know when we are done by tracking outstanding calls
				prd.always(function() {
					outstandingCount -= 1
				})
			})
		})
		
		// when all callbacks are done...
		$.when.apply($, callbacks).then(function() {
			tables.each(function() {
				$(this).find('tr').each(function() {
					var prId = $(this).attr("data-pullrequestid")
					reviewers = $(this).children('.reviewers')
					
		        	disapprovalHtml = "";
		        	data = compiledData[prId.toString()]
		        	if (data.disapproval) {
		        		console.log("Pull Request is Disapproved by " + data.disapprovedBy)
						disapprovalHtml = "<td class=\"reviewers\"><font color=\"#AA0000\">ಠ_ಠ</font></td>"
		        	} else {
		        		console.log("Pull Request NOT Disapproved")
		        	}
					reviewers.before(disapprovalHtml)
				})
			})
						
	        // Add the header if it needs to be added
			if (disapprovalEnabled) {
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
		