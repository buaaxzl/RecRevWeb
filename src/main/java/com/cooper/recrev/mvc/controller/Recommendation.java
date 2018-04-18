package com.cooper.recrev.mvc.controller;

import java.util.*;

import org.eclipse.egit.github.core.PullRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cooper.recrev.services.UserIdService;
import com.cooper.recrev.services.recommend.RecommendService;
  
 
@Controller  
public class Recommendation {  
	
	@Autowired
	private RecommendService recommendService;

	@Autowired
	private UserIdService idService;
	
	@ResponseBody
    @RequestMapping("doRecommend")
	@CrossOrigin(origins = "*")
    private  Map<String, String> doRecommend(  
            @RequestParam(value = "owner", required = false)String owner,  
            @RequestParam(value = "repo", required = false)String repo,
            @RequestParam(value = "no", required = false)String no
            ) {
    	Map<String, String> res = new HashMap<String, String>();
    	
		if (owner == null || repo == null || no == null) {
			return res;
		}
		List<String> names = recommendService.doRecommendation(owner, repo, no);
		List<String> ids = idService.transferToIds(names);
		
		for (int i = 0; i < names.size(); i ++) {
			if (i < ids.size())
				res.put(names.get(i), ids.get(i));
			else
				res.put(names.get(i), "");
		}

		if (res.isEmpty()) {
			res.put("该条数据还未缓存！ <br/>  正在缓存中，可能需要几分钟的时间!  <br/> 稍后请回来!", "0");
		}

        return res;  
    }  
	
	@ResponseBody
    @RequestMapping("listPullRequests")
	@CrossOrigin(origins = "*")
    private Map<Integer, List<String>> listPullRequests(  
            @RequestParam(value = "owner", required = false)String owner,  
            @RequestParam(value = "repo", required = false)String repo
            ){  
		Map<Integer, List<String>> res = new HashMap<Integer, List<String>>();
		
		if (owner == null || repo == null) {
			return res;
		}
		
		List<PullRequest> prs = recommendService.listPullRequests(owner, repo);
		
		for (PullRequest pr : prs) {
			List<String> content = new ArrayList<String>();
			content.add(pr.getUser().getLogin());
			content.add(pr.getTitle());
			content.add(pr.getUrl());
			content.add(pr.getDiffUrl());
			content.add(pr.getPatchUrl());
			content.add(pr.getBody());
			content.add(pr.getCreatedAt().getTime()+"");
			
			res.put(pr.getNumber(), content);
		}

		if(res.isEmpty()) {
			res.put(-1, Arrays.asList("该项目数据还未缓存！ <br/> 正在缓存中，可能需要几分钟的时间! <br/> 稍后请回来!"));
		}

        return res;  
    }
}  
