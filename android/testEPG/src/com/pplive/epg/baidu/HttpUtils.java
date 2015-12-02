package com.pplive.epg.baidu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

public class HttpUtils {
	/**
	 * map转换成entity
	 * 
	 * @param map
	 *            待处理的
	 * @return 处理后的数据
	 */
	public static HttpEntity mapToEntity(HashMap<String, String> map)
			throws Exception {
		BasicNameValuePair pair = null;
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		for (Map.Entry<String, String> m : map.entrySet()) {
			pair = new BasicNameValuePair(m.getKey(), m.getValue());
			params.add(pair);
		}
		HttpEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
		return entity;
	}

	/**
	 * 取文本之间的字符串
	 * 
	 * @param string
	 *            源字符串
	 * @param start
	 *            开始字符串
	 * @param end
	 *            结束字符串
	 * @return 成功返回中间子串，失败返回null
	 */
	public static String mid(String string, String start, String end) {
		int s = string.indexOf(start) + start.length();
		int e = string.indexOf(end, s);
		if (s > 0 && e > s)
			return string.substring(s, e);
		return null;
	}

	/**
	 * 
	 * @param regex
	 *            正则表达式
	 * @param input
	 *            待匹配的字符串
	 * @return 返回的是匹配的list集合（可能由于正则表达式的不同有多条记录）
	 */
	public static ArrayList<String> myRegex(String regex, String input) {
		ArrayList<String> list = new ArrayList<String>();
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(input);
		while (m.find()) {
			list.add(m.group());
		}
		return list;
	}
}
