import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.watchers.IssueWatcherAccessor
import com.atlassian.jira.issue.worklog.WorklogImpl

def getIssues(user, jql) { 
	def searchService = ComponentAccessor.getComponent(SearchService)
	def issueManager = ComponentAccessor.getIssueManager()
   	SearchService.ParseResult parseResult = searchService.parseQuery(user, jql)
	if (parseResult.isValid()) {
    	def searchResult = searchService.search(user, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
    	return searchResult.issues.collect { issueManager.getIssueObject(it.id) }
	}
    return null
}

def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def jqlWhereWatcherIsCurrentUser = "watcher = currentUser()"
def watcherManager = ComponentAccessor.getWatcherManager()
def issueWatcherAccessor = ComponentAccessor.getComponent(IssueWatcherAccessor)
def worklogManager = ComponentAccessor.getWorklogManager()
 
Locale en = new Locale("en");
getIssues(user, jqlWhereWatcherIsCurrentUser).each { issue ->
    issueWatcherAccessor.getWatchers(issue,en).each { watcher ->
        if (watcher != user) {
			watcherManager.stopWatching(watcher, issue)
        }
    }
}

getIssues(user, "").each { issue ->
    worklogManager.create(issue.reporter, new WorklogImpl(worklogManager, issue, null, issue.reporter.name, issue.summary, new Date(), null, null, 3600), 0L, false)
}
