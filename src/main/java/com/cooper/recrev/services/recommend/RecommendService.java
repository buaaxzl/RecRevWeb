package com.cooper.recrev.services.recommend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.PullRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cooper.recrev.common.SortMapElement;
import com.cooper.recrev.data.DataPreparation;

@Component
public class RecommendService {
	
	@Autowired
	private DataPreparation dp; 
	
	public List<PullRequest> listPullRequests(String owner, String repo) {
		List<PullRequest> prs = new ArrayList<PullRequest>();
		try {
			prs = dp.getPrlist(owner, repo);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return prs;
	}
	
	public List<String> doRecommendation(String owner, String project, String number) {
		List<String> res = new ArrayList<String>();
		
		try {
			List<PullRequest> prs = dp.getPrlist(owner, project);
			
			int index = -1;
			for (int i = 0; i < prs.size(); i ++) {
				if ((prs.get(i).getNumber()+"").equals(number)) {
					index = i;
					break;
				}
			}
			
			if (index == -1) {
				return res;
			}
			
			if (dp.getPrFilesMap(owner, project).get(index) == null || 
					dp.getPrFilesMap(owner, project).get(index).size() == 0) 
				return res;
			
			res = getCandidateReviewers(owner, project, index, new LCP());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	private int temporalLocality(int n) {
		if (n >= 500) {
			return n - 500;
		} else {
			return 0;
		}
	}
	
	private List<String> getCandidateReviewers(String owner, String project, int rn, FilePathComparator fpc) {
		
		Map<String, Double> reviewers = new HashMap<String, Double>();
		
		Map<Integer, List<String>> files = dp.getPrFilesMap(owner, project);
		
		List<String> filesn = files.get(rn);
		
		int beginIndex = 0;
		beginIndex = temporalLocality(rn);

		for (int i = beginIndex; i < rn; i++) {
			if (!validatePR(owner, project, i))
				continue;
			
			reviewerExpertise(i, filesn, fpc, owner, project, reviewers);
		}
		
		List<Map.Entry<String, Double>> entryList = SortMapElement.sortDouble(reviewers);
		
		List<String> ret = new ArrayList<String>();
		for (Map.Entry<String, Double> each : entryList) 
			ret.add(each.getKey());
		
		return ret;
	}
	
	private void reviewerExpertise(int i, List<String> filesn, FilePathComparator fpc,
									String owner, String project, Map<String, Double> reviewers) {
		List<String> filesp = dp.getPrFilesMap(owner, project).get(i);
		
		double scorep = 0.0;
		for (String a : filesn)
			for (String b : filesp)
				scorep = scorep + fpc.similar(a, b);

		scorep = scorep / (filesn.size() * filesp.size());
		
		List<String> revs = dp.getCodeReviewers(owner, project).get(i);
		for (String reviewer : revs) {
			if (reviewers.containsKey(reviewer))
				reviewers.put(reviewer, reviewers.get(reviewer)+scorep);
			else
				reviewers.put(reviewer, scorep);
		}
	}
	
	private boolean validatePR(String owner, String project, int index) {
		List<String> filesp = dp.getPrFilesMap(owner, project).get(index);
		if (filesp == null || filesp.size() == 0) 
			return false;
		
		if (dp.getCodeReviewers(owner, project).get(index) == null ||
				dp.getCodeReviewers(owner, project).get(index).size() == 0)
			return false;
		
		return true;
	}
}
