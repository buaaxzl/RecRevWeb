package com.cooper.recrev.data.crawler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHubAPICrawler {

	private String repo = "netty";
	private String owner = "netty";
	private RepositoryId ri = null;
	
	private GitHubClient ghClient = new GitHubClient()
			.setOAuth2Token("2bf19ca5ba0d7270548169517f43e4d574a1c599");         //buaaxzl
	
	private final String[] tokens = {
				"c72a4955c2634320c5859e0e4636cfe52a106376",  //xzl
				"7ec9e318bfb7c35078c067837a3ce30beeeac7d2",  //baohan
				"1b1f7cabe30599727608125aa777790ea779c881",  //yeluting
				"d16b8d00a6e2de29792c8e854b5106fb8b17e1ae",  //yanjiafei
				"a3bf263f60ab87a9c42c4b16273a9b5a6908be5d",  //dulian
				"01f7cd93341b56ab19161aac71f66d0d28d253f9",  //zhagnyong
				"2bf19ca5ba0d7270548169517f43e4d574a1c599"   //buaaxzl
	};
	
	private int t = 0;
	private void updateToken() {
		if (ghClient.getRemainingRequests() <= 105 && 
				ghClient.getRemainingRequests() != -1) {
			System.out.println("********change Token*******Limit : " + ghClient.getRequestLimit());
			t = (t+1) % tokens.length;
			ghClient.setOAuth2Token(tokens[t]);
		}
	}
	
	public GitHubAPICrawler(String repo, String owner) {
		this.repo = repo;
		this.owner = owner;
		this.ri = new RepositoryId(owner, repo);
		
	}
	
	public void retrieveRepo() throws IOException {
		System.out.println("repo begin");
		RepositoryService rs = new RepositoryService(ghClient);
		Repository repo = rs.getRepository(ri);
		writeToFile("repos.json", repo);
		System.out.println("repo end");
	}
	
	public void retrieveCommits() {
		System.out.println("commit begin");
		
		CommitService cs = new CommitService(ghClient);
		PageIterator<RepositoryCommit> iterator = cs.pageCommits(ri);
		List<RepositoryCommit> cmts = new ArrayList<RepositoryCommit>();
		while (iterator.hasNext()) {
			updateToken();
			cmts.addAll(iterator.next());
		}
		
		System.out.println("commit retrieve all shas");
		
		List<String> error = new ArrayList<String>();
		List<RepositoryCommit> filesCmts = new ArrayList<RepositoryCommit>();
		int metric = 0;
		for (RepositoryCommit each : cmts) {
			try {
				updateToken();
				filesCmts.add(cs.getCommit(ri, each.getSha()));
				metric++;
				System.out.println("GET " + metric + " commits");
			} catch (Exception e) {
				if (e.getMessage().matches(".*Bad.*"))
					updateToken();
				error.add(each.getSha());
				e.printStackTrace();
			}
		}
		System.out.println("begin to write commits " +  filesCmts.size());
		writeToFile("commits.json", filesCmts);
		System.out.println("error : " + error.size());
		System.out.println("commit end");
	}
	
	public void retrieveCommitComments() {
		System.out.println("commit comments begin");
		
		CommitService cs = new CommitService(ghClient);
		PageIterator<CommitComment> iterator = cs.pageComments(ri);
		List<CommitComment> comments = new ArrayList<CommitComment>();
		int metric = 0;
		while (iterator.hasNext()) {
			updateToken();
			comments.addAll(iterator.next());
			metric += 100;
			System.out.println("GET " + metric + " commit comments");
		}
		
		Map<String, List<CommitComment>> cmtComments = 
				new HashMap<String, List<CommitComment>>();
		for (CommitComment cc : comments) {
			if (!cmtComments.containsKey(cc.getCommitId())) {
				ArrayList<CommitComment> tmp = new ArrayList<CommitComment>();
				tmp.add(cc);
				cmtComments.put(cc.getCommitId(), tmp);
			}
			else
				cmtComments.get(cc.getCommitId()).add(cc);
		}
		writeToFile("commit_comments.json", cmtComments);
		System.out.println("commit comments end");
	}
	
	List<Integer> issueIds = new ArrayList<Integer>();
	public void retrieveIssues() {
		System.out.println("issues begin");
		
		IssueService is = new IssueService(ghClient);
		Map<String, String> filterData = new HashMap<String, String>();
		filterData.put("state", "all");
		PageIterator<Issue> iterator = is.pageIssues(ri, filterData);
		List<Issue> issues = new ArrayList<Issue>();
		int metric = 0;
 		while (iterator.hasNext()) {
 			updateToken();
			issues.addAll(iterator.next());
			metric += 100;
			System.out.println("GET " + metric + " issues");
		}
 		for (Issue each : issues) 
 			issueIds.add(each.getNumber());
 		writeToFile("issues.json", issues);
 		System.out.println("issues end");
	}
	
	public void retrieveIssueEvents() {
		System.out.println("issue events begin");
		
		IssueService is = new IssueService(ghClient);
		PageIterator<IssueEvent> iterator = is.pageEvents(owner, repo);
		List<IssueEvent> events = new ArrayList<IssueEvent>();
		int metric = 0;
		while (iterator.hasNext()) {
			updateToken();
			events.addAll(iterator.next());
			metric += 100;
			System.out.println("GET " + metric + " issue events");
		}
		
		Map<String, List<IssueEvent>> evts = 
				new HashMap<String, List<IssueEvent>>();
		for (IssueEvent each : events) {
			if (nullChecker(each.getIssue())) continue;
			if (nullChecker(each.getIssue().getNumber())) continue;
			
			if (!evts.containsKey(each.getIssue().getNumber()+"")) {
				ArrayList<IssueEvent> tmp = new ArrayList<IssueEvent>();
				tmp.add(each);
				evts.put(each.getIssue().getNumber()+"", tmp);
			}
			else
				evts.get(each.getIssue().getNumber()+"").add(each);
		}
		
		writeToFile("issue_events.json", evts);
		System.out.println("issue events end");
	}
	
	public void retrieveIssueComments() {
		System.out.println("issue comments begin");
		
		IssueService is = new IssueService(ghClient);
		List<Integer> error = new ArrayList<Integer>();
		Map<String,List<Comment>> comments = new HashMap<String, List<Comment>>();
		
		for (Integer id : issueIds) {
			try {
				updateToken();
				comments.put(id.toString(), is.getComments(ri, Integer.toString(id)));
				System.out.println("GET " + comments.size() + " issue comments");
			} catch (IOException e) {
				error.add(id);
				e.printStackTrace();
			}
		}
		System.out.println("error : " + error.size());
		writeToFile("issue_comments.json", comments);
		System.out.println("issue comments end");
	}
	
	private List<Integer> prIds = new ArrayList<Integer>();
	
	public void retrievePullRequests() {
		System.out.println("pull request begin");
		
		PullRequestService prs = new PullRequestService(ghClient);
		PageIterator<PullRequest> iterator = prs.pagePullRequests(ri, "all");
		List<PullRequest> pullRequests = new ArrayList<PullRequest>();
		int metric = 0;
		while (iterator.hasNext()) {
			updateToken();
			pullRequests.addAll(iterator.next());
			metric += 100;
			System.out.println("GET " + metric + " pull requests");
		}
		for (PullRequest pr : pullRequests)
			prIds.add(pr.getNumber());
		writeToFile("prs.json", pullRequests);
		System.out.println("pull request end");
	}
	
	public void retrievePullRequestCommits() {
		System.out.println("pull request commits begin");
		
		Map<String, List<RepositoryCommit>> prCommits = 
				new HashMap<String, List<RepositoryCommit>>();
		PullRequestService prs = new PullRequestService(ghClient);
		for (Integer id : prIds) {
			try {
				updateToken();
				List<RepositoryCommit> commits = prs.getCommits(ri, id);
				prCommits.put(id.toString(), commits);
				System.out.println("GET id:" + id + " pull requests commits");
			} catch (IOException e) {
				System.out.println("prID: " + id + " get commits failed");
				e.printStackTrace();
			}
		}
		writeToFile("prCommits.json", prCommits);
		System.out.println("pull request commits end");
	}
	
	public void retrievePullRequestCommentsAndFiles() {
		System.out.println("pull request comments begin");
		
		PullRequestService prs = new PullRequestService(ghClient);
		Map<String,List<CommitComment>> comments = new HashMap<String,List<CommitComment>>();
		Map<String, List<CommitFile>> prFileMap = new HashMap<String, List<CommitFile>>();
		for (Integer prId : prIds) {
			updateToken();
			PageIterator<CommitComment> iterator = prs.pageComments(ri, prId);
			try {
				updateToken();
				List<CommitFile> files = prs.getFiles(ri, prId);
				prFileMap.put(prId.toString(), files);
			} catch (IOException e) {
				System.out.println("prID: " + prId + " get files failed");
				e.printStackTrace();
			}
			
			while (iterator.hasNext()) {
				updateToken();
				if (!comments.containsKey(prId.toString()))
						comments.put(prId.toString(), 
								new ArrayList<CommitComment>(iterator.next()));
				else
					comments.get(prId.toString()).addAll(iterator.next());
			}
			System.out.println("GET " + comments.size() + " pull requests comments");
		}
		writeToFile("pull_request_comments.json", comments);
		writeToFile("prFileMap.json", prFileMap);
		System.out.println("pull request comments end");
	}
	
	
	
	private void write(String filename , Object content) {
		JSONObject ob = null;
		String str = null;
		if (content instanceof JSONObject) 
			ob = (JSONObject)content;
		else
			str = (String)content;
		
		System.out.println("write " + filename);
		
//		String writeFilename = "C:\\Users\\buaaxzl\\Desktop\\ExpData\\";
		String writeFilename = "/home/xzl/";
		writeFilename = writeFilename + owner + "-" + repo;
		File file = new File(writeFilename);
		if (!file.exists()) file.mkdir();
		
		try (BufferedWriter bWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(
//								new File("C:\\Users\\buaaxzl\\Desktop\\ExpData\\"
//										+ owner + "-" + repo + 
//										"\\" + filename)),"utf-8"))) {
								new File("/home/xzl/"
										+ owner + "-" + repo + 
										"/" + filename)),"utf-8"))) {
			if ( str != null) {
				bWriter.write(str);
				bWriter.flush();
			}
			else {
				System.out.println("write JSONObject");
				ob.writeJSONString(bWriter);
				System.out.println("write finished");
			}
		} catch (Exception e) {
			System.out.println(filename + "failed");
			e.printStackTrace();
		}
	}
	
	public void writeToFile(String filename, Object ob) {
		JSONObject json = new JSONObject();
		if (ob instanceof Repository) {
			Repository repo = (Repository)ob;
			write(filename, JSON.toJSONString(repo));
		}
		else if (ob instanceof List ) {
			@SuppressWarnings("rawtypes")
			List list = (List) ob;
			
			System.out.println(filename + " " + list.size());
			
			if (list.get(0) instanceof RepositoryCommit) {
				for (int i = 0; i < list.size(); i++) {
					RepositoryCommit cmt = (RepositoryCommit)(list.get(i));
					json.put(cmt.getSha(), cmt);
				}
			}
			else if (list.get(0) instanceof Issue) {
				for (int i = 0; i < list.size(); i++) {
					Issue issue = (Issue)(list.get(i));
					json.put(issue.getNumber()+"", issue);
				}
			}
			else if (list.get(0) instanceof PullRequest) {
				for (int i = 0; i < list.size(); i++) {
					PullRequest pr = (PullRequest)(list.get(i));
					json.put(pr.getNumber()+"", pr);
				}
			}
			
			System.out.println("begin to write");
			write(filename, json);	
		}
		else if (ob instanceof Map) {
			if (filename.equals("issue_comments.json")) {
				@SuppressWarnings("unchecked")
				Map<String, List<Comment>> comments = (Map<String,List<Comment>>)ob;
				for (Map.Entry<String, List<Comment>> each : comments.entrySet())
					json.put(each.getKey(), each.getValue());
			}
			else if (filename.equals("pull_request_comments.json")) {
				@SuppressWarnings("unchecked")
				Map<String, List<CommitComment>> comments = (Map<String,List<CommitComment>>)ob;
				for (Map.Entry<String, List<CommitComment>> each : comments.entrySet())
					json.put(each.getKey(), each.getValue());
			}
			else if (filename.equals("prFileMap.json")) {
				@SuppressWarnings("unchecked")
				Map<String, List<CommitFile>> comments = (Map<String,List<CommitFile>>)ob;
				for (Map.Entry<String, List<CommitFile>> each : comments.entrySet())
					json.put(each.getKey(), each.getValue());
			}
			else if (filename.equals("commit_comments.json")) {
				@SuppressWarnings("unchecked")
				Map<String, List<CommitComment>> comments = (Map<String,List<CommitComment>>)ob;
				for (Map.Entry<String, List<CommitComment>> each : comments.entrySet())
					json.put(each.getKey(), each.getValue());
			}
			else if (filename.equals("issue_events.json")) {
				@SuppressWarnings("unchecked")
				Map<String, List<IssueEvent>> events = (Map<String,List<IssueEvent>>)ob;
				for (Map.Entry<String, List<IssueEvent>> each : events.entrySet())
					json.put(each.getKey(), each.getValue());
			}
			else if (filename.equals("prCommits.json")) {
				@SuppressWarnings("unchecked")
				Map<String, List<RepositoryCommit>> commits = (Map<String, List<RepositoryCommit>>)ob;
				for (Map.Entry<String, List<RepositoryCommit>> each : commits.entrySet())
					json.put(each.getKey(), each.getValue());
			}
			write(filename, json);	
		}
	}
	
	private boolean nullChecker(Object ob) {
		if (ob == null)
			return true;
		else return false;
	}
	
	public static void main(String[] args) throws IOException {

		String[] repos = {
//				"rust-lang/rust",                // rust
//				"rails/rails",                 //ruby
//				"jquery/jquery",               //js
				"angular/angular.js",          //js
				"saltstack/salt",              //python
//				"cocos2d/cocos2d-x",           //C++
//				"akka/akka",                   //Scala
//				"docker/docker" ,              //Go
				"netty/netty",                 //Java
//				"atom/atom",                   //CoffeScript
//				"duckduckgo/zeroclickinfo-goodies"   //Perl
				"ipython/ipython",
//				"zendframework/zf2",
//				"scala/scala",
//				"cakephp/cakephp",
//				"bitcoin/bitcoin",
				"symfony/symfony",
//				"gitlabhq/gitlabhq",
//				"scikit-learn/scikit-learn",
//				"nodejs/node",
//				"coreos/etcd"
		};
		
		if (args.length != 0) {
			int index = Integer.parseInt(args[0]);
			int stage = 0;
			if (args.length > 1) 
				stage = Integer.parseInt(args[1]);
			
			String[] arg = repos[index].split("/");
			GitHubAPICrawler gh = new GitHubAPICrawler(arg[1], arg[0]);
			if (gh.repo != null && gh.owner != null) {
				System.out.println("BEGIN-----------"+gh.owner + " " + gh.repo);
				
				switch(stage) {
				case 0:
//					gh.retrieveRepo();
//					gh.retrieveCommits();
//					gh.retrieveCommitComments();
				case 1:
					gh.retrieveIssues();
//					gh.retrieveIssueEvents();
					gh.retrieveIssueComments();
				case 2:
					gh.retrievePullRequests();
					gh.retrievePullRequestCommentsAndFiles();
//					gh.retrievePullRequestCommits();
				}
				
				System.out.println("END-------------"+gh.owner + " " + gh.repo);
			}
		}
		else
			for (int i = 0; i < repos.length; i++) {
				String[] arg = repos[i].split("/");
				GitHubAPICrawler gh = new GitHubAPICrawler(arg[1], arg[0]);
				if (gh.repo != null && gh.owner != null) {
					System.out.println("BEGIN-----------"+gh.owner + " " + gh.repo);
					
//					gh.retrieveRepo();
//					gh.retrieveCommits();
//					gh.retrieveCommitComments();
					gh.retrieveIssues();
//					gh.retrieveIssueEvents();
					gh.retrieveIssueComments();
					gh.retrievePullRequests();
					gh.retrievePullRequestCommentsAndFiles();
//					gh.retrievePullRequestCommits();
					
					System.out.println("END-------------"+gh.owner + " " + gh.repo);
				}
			}
	}
}
