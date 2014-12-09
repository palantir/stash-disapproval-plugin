require(['model/page-state', 'util/navbuilder', 'jquery'], function(state, nav, $) {

  var prTotal = 0;
  var intervalNumber = 0;
  var baseUrl = undefined;
  var repoId = undefined;

  /*
    Extract query information from the page's URL
   */
  function getUrlParam (name) {
    var results = new RegExp('[\?&]' + name + '=([^&#]*)').exec(window.location.href)
    if (results == null) {
      return null
    } else {
      return results[1] || 0
    }
  }

  /*
    Apply the disapproval information to a pull request row
   */
  function doDisapproval (row) {
    // Add a disapproval cell first
    // We make it invisible because we don't actually know yet if we are enabled
    if (row.find("td.disapproval").size() === 0) {
      row.find("td.reviewers").before($("<td class=\"disapproval\"></td>").attr("style","display:none;"))
    } else {
      return // Somehow we got here again?
    }

    var prId = row.attr("data-pullrequestid");
    $.getJSON (baseUrl + "/disapproval/disapprove/" + repoId + "/" + prId, function(data) {
      if (!data.enabledForRepo) {
        return;
      }

      disapprovalDiv = $("<div class=\"disapproval\"></div>")
      if (data.disapproval) {
        console.log("Pull Request " + prId + " is Disapproved by " + data.disapprovedBy)
        disapprovalDiv.html($("<img></img>").attr("src", baseUrl + "/disapproval/static-content/disapprovalface-trim.png")
                                            .css("max-height", "24px"))
      } else {
        console.log("Pull Request " + prId + " NOT Disapproved")
      }

      // Add the info and make the cell visible
      row.find("td.disapproval").html(disapprovalDiv).removeAttr("style")

      if ($("table#pull-requests-table thead tr").find("th.disapproval").size() === 0) {
        $("table#pull-requests-table thead tr").find("th.reviewers").before($("<th class=\"disapproval\">Disapproval</th>"))
      }
    });
  }

  /*
    Scan for new rows without the disapprovals set up
   */
  function doNewDisapprovals () {
    var prRows = $("table#pull-requests-table tbody tr.pull-request-row")
    prRows.each(function (rowIndex) {
      if ($(this).find("td.disapproval").size() === 0) {
        doDisapproval($(this))
      }
    });
    console.log("We've done " + prRows.size() + " PR disapprovals so far.")

    // We've loaded the same # of PRs we know about, let's stop the interval
    if (prRows.size() >= prTotal) {
      clearInterval(intervalNumber)
      intervalNumber = 0;
    }
  }




  // http://stackoverflow.com/questions/6285491/are-there-universal-alternatives-to-window-onload-without-using-frameworks
  // Decided to do with jquery, to avoid messing up the window.onload
  $(document).ready(function() {

    baseUrl = nav.pluginServlets().build()
    repoId = state.getRepository().id;

    // Make sure we're querying for the right kind of PRs
    var prState = getUrlParam("state")
    if (prState == null) {
      prState = "open"
    }
    prRestBaseUrl = nav.rest().currentRepo().allPullRequests().build() + "?limit=100&state=" + prState

    // The rest api for pull requests won't let me get everything at once
    var allPRsCounted = false
    while (!allPRsCounted) {
      $.ajax({ url: prRestBaseUrl + "&start=" + prTotal,
               dataType:'json',
               async: false,
               success: function (data) {
                 prTotal += data.size;
                 if (data.isLastPage) {
                   allPRsCounted = true
                 }
               }
      });
    }
    console.log("There are " + prTotal + " total PRs")

    // Apply to the already loaded rows
    $("tr.pull-request-row").each(function (rowIndex) {
      doDisapproval($(this));
    });

    if (prTotal > 25) {
      intervalNumber = setInterval(doNewDisapprovals, 750);
    }

  })


})

