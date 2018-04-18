package com.cooper.recrev.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.PullRequest;
import org.springframework.stereotype.Component;


@Component
public class DataPreparation {
	
	private static Map<String, Map<Integer, List<String>>> 
			filesCache = new ConcurrentHashMap<String, Map<Integer, List<String>>>();
	
	private static Map<String, Map<Integer, List<String>>> 
			reviewersCache = new ConcurrentHashMap<String, Map<Integer, List<String>>>();
	
	private static Map<String, List<PullRequest>> 
			prCache = new ConcurrentHashMap<String, List<PullRequest>>();
	

	private Object loadFile(String project, String file, IDataParser parser) {

		String filePath;
		if (System.getProperty("os.name").contains("Windows"))
			filePath = "C:\\Users\\buaaxzl\\Desktop\\ExpData\\" + project + "\\" + file;
		else
			filePath = "/home/xzl/" + project + "/" + file;
		
		File cond = new File(filePath);
		if (!cond.exists())
			return null;
		
		try (BufferedReader bReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), "utf-8"));) {
			String jsonString = "";
			String line = null;
			while ((line = bReader.readLine()) != null) {
				jsonString += line;
			}
			return parser.parse(jsonString);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, PullRequest> loadPrs (String project) {
		Map<String, PullRequest> prs = (Map<String, PullRequest>)loadFile(
				project, "prs.json", new PullRequestDataProcess());
		return prs;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, List<CommitComment>> loadPrComments(String project) {
		Map<String, List<CommitComment>> prComments = 
				(Map<String, List<CommitComment>>)loadFile(
				project, "pull_request_comments.json", new PRCommentsDataProcess());
		return prComments;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, List<CommitFile>> loadPrFiles(String project) {
		Map<String, List<CommitFile>> prFiles = 
				(Map<String, List<CommitFile>>)loadFile(
				project, "prFileMap.json", new PRFilesDataProcess());
		return prFiles;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, List<Comment>> loadIssueComments(String project) {
		Map<String, List<Comment>> issueComments = 
				(Map<String, List<Comment>>)loadFile(
				project, "issue_comments.json", new IssueCommentsDataProcess());
		return issueComments;
	}
	
	private Map<Integer, List<String>> identifyCodeReviewers(List<PullRequest> prList, 
			Map<String, List<CommitComment>> prComments, Map<String, List<Comment>> issueComments) {
		
		Map<Integer, List<String>> prToReviewers = new HashMap<Integer, List<String>>();
		
		for (int i = 0; i < prList.size(); i++) {
			PullRequest pr = prList.get(i);
			List<CommitComment> prCom = prComments.get(pr.getNumber()+"");
			List<Comment> issueCom = issueComments.get(pr.getNumber()+"");

			if (prCom == null && issueCom == null) {
				prToReviewers.put(i, null);
				continue;
			}
			
			List<String> reviewersFromComment = new ArrayList<String>();

			if (prCom != null)
				for (CommitComment each : prCom) {
					if (each.getUser() == null)
						continue;
					if (!reviewersFromComment.contains(each.getUser().getLogin()))
						reviewersFromComment.add(each.getUser().getLogin());
				}
			
			if (issueCom != null)
				for (Comment each : issueCom) {
					if (each.getUser() == null)
						continue;
					if (!reviewersFromComment.contains(each.getUser().getLogin()))
						reviewersFromComment.add(each.getUser().getLogin());
				}
			
			prToReviewers.put(i, reviewersFromComment);
		}
		return prToReviewers;
	}
	
	/*
	 * 筛掉评审者数目少于2个的pull request
	 */
	private List<PullRequest> filterData(List<PullRequest> prList, String owner, String repo,
			Map<String, List<CommitComment>> prComments, Map<String, List<Comment>> issueComments) {
		
		Map<Integer, List<String>> prReviewers = identifyCodeReviewers(prList, prComments, issueComments);
		
		Set<Integer> removal = new HashSet<Integer>();
		
		for (int i = 0; i < prList.size(); i++) {
			if (prReviewers.get(i) == null || prReviewers.get(i).size() < 0) {
				removal.add(i);
			}
		}
		
		List<PullRequest> prListTmp = new ArrayList<PullRequest>();
		Map<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
		
		for (int i = 0; i < prList.size(); i++) {
			if (!removal.contains(i)) {
				prListTmp.add(prList.get(i));
				indexMap.put(prListTmp.size()-1, i);
			}
			else {
				prReviewers.remove(i);
			}
		}
		
		Map<Integer, List<String>> prReviewersTmp = new HashMap<Integer, List<String>>();
		for (Map.Entry<Integer, Integer> each : indexMap.entrySet()) {
			List<String> val = prReviewers.get(each.getValue());
			prReviewersTmp.put(each.getKey(), val);
		}
		
		reviewersCache.put(owner + "/" + repo, prReviewersTmp);
		
		return prListTmp;
	}
	
	private boolean loadAndProcessData(String owner, String repo) {
		String project = owner + "-" + repo;
		
		Map<String, PullRequest> prs = loadPrs(project);
		Map<String, List<CommitComment>> prComments = loadPrComments(project);
		Map<String, List<CommitFile>> prFiles = loadPrFiles(project);
		Map<String, List<Comment>> issueComments = loadIssueComments(project);
		
		
		if (prs == null || prComments == null || prFiles == null || issueComments == null) {
			return false;
		}
	
		List<PullRequest> prList = new ArrayList<PullRequest>();
		
		// filter prs
		for (Map.Entry<String, PullRequest> each : prs.entrySet()) {
//			if (each.getValue().getState().equals("closed") ||
//					each.getValue().isMerged())
				prList.add(each.getValue());
		}
		
		Collections.sort(prList, new Comparator<PullRequest>() {
			@Override
			public int compare(PullRequest o1, PullRequest o2) {
				return o1.getCreatedAt().compareTo(o2.getCreatedAt());
			}
		});
		
		prList = filterData(prList, owner, repo, prComments, issueComments);
		prCache.put(owner + "/" + repo, prList);
		
		
		// 填充 prFileMap 到 filesCache
		Map<Integer, List<String>> prFileName = new HashMap<Integer, List<String>>();
		
		for (int i = 0; i < prList.size(); i++) {
			PullRequest pr = prList.get(i);
			String number = pr.getNumber()+"";
			List<CommitFile> files = prFiles.get(number);
			
			if (files == null) {
				prFileName.put(i, null);
				continue;
			}
			
			List<String> filenames = new ArrayList<String>();
			// How to process duplicate filename 
			for (CommitFile each : files) {
				if (each.getFilename() == null)
					continue;
				if (!filenames.contains(each.getFilename()))
					filenames.add(each.getFilename());
			}
			prFileName.put(i, filenames);
		}
		filesCache.put(owner + "/" + repo, prFileName);
		
		return true;
	}
	
	private void init(String owner, String repo) {
		
		// 项目安全性检查
		if (!loadAndProcessData(owner, repo)) {
			throw new RuntimeException("load file error!");
		}
	}
	
	public Map<Integer, List<String>> getPrFilesMap(String owner, String repo) {
		String key = owner + "/" + repo;
		
		if (!filesCache.containsKey(key)) {
			init(owner, repo);
		}
		
		return filesCache.get(owner + "/" + repo);
	}
	
	public Map<Integer, List<String>> getCodeReviewers(String owner, String repo) {
		if (!reviewersCache.containsKey(owner + "/" + repo)) {
			init(owner, repo);
		}
		return reviewersCache.get(owner + "/" + repo);
	}
	
	public List<PullRequest> getPrlist(String owner, String repo) {
		if (!prCache.containsKey(owner + "/" + repo)) {
			init(owner, repo);
		}
		return prCache.get(owner + "/" + repo);
	}
	
	public static void main(String[] args) {
		System.out.println(System.getProperty("os.name"));
		
//		DataPreparation dp = new DataPreparation();
//		dp.loadData("netty-netty");
//		System.out.println(dp.getPrComments().get("5613").size());
//
//		System.out.println(dp.getPrFiles().containsKey("5613"));
	}
}
