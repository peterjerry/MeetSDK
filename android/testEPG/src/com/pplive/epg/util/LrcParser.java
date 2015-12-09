package com.pplive.epg.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 此类用来解析LRC文件 将解析完整的LRC文件放入一个LrcInfo对象中 并且返回这个LrcInfo对象s author:java_mzd
 */
public class LrcParser {
	private String TAG = "LycParser";
	
	private LrcInfo lrcinfo = new LrcInfo();
	private List<TimeLrc> lrcList = new ArrayList<TimeLrc>();//用户保存所有的歌词和时间点信息间的映射关系的Map
	private String lastLine;


	/**
	 * 根据文件路径，读取文件，返回一个输入流
	 * 
	 * @param path
	 *            路径
	 * @return 输入流
	 * @throws FileNotFoundException
	 */
	private InputStream readLrcFile(String path) throws FileNotFoundException {
		File f = new File(path);
		InputStream ins = new FileInputStream(f);
		return ins;
	}

	public LrcInfo parseFile(String path, String charset) throws Exception {
		InputStream in = readLrcFile(path);
		lrcinfo = parseStream(in, charset);
		return lrcinfo;

	}
	
	/**
	 * 将输入流中的信息解析，返回一个LrcInfo对象
	 * 
	 * @param inputStream
	 *            输入流
	 * @return 解析好的LrcInfo对象
	 * @throws IOException
	 */
	public LrcInfo parseStream(InputStream inputStream, String charset) throws IOException {
		// 三层包装
		InputStreamReader inr = new InputStreamReader(inputStream, charset);
		BufferedReader reader = new BufferedReader(inr);
		// 一行一行的读，每读一行，解析一行
		String line = null;
		while ((line = reader.readLine()) != null) {
			parserLine(line);
		}
		// 全部解析完后，设置info
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		Collections.sort(lrcList, new Comparator<TimeLrc>() {

			@Override
			public int compare(TimeLrc line1, TimeLrc line2) {
				// TODO Auto-generated method stub
				if (line1.getTimePoint() > line2.getTimePoint())
					return 1;
				else if (line1.getTimePoint() == line2.getTimePoint())
					return 0;
				else
					return -1;
			}
			
		});
		
		lrcinfo.setInfos(lrcList);
		return lrcinfo;
	}

	/**
	 * 利用正则表达式解析每行具体语句
	 * 并在解析完该语句后，将解析出来的信息设置在LrcInfo对象中
	 * 
	 * @param str
	 */
	private void parserLine(String str) {
		
		System.out.println("Java: parse line " + str);
		
		if (str.startsWith("[ti:")) {
			// 取得歌曲名信息
			String title = str.substring(4, str.length() - 1);
			System.out.println("Java: title--->" + title);
			lrcinfo.setTitle(title);
		}
		else if (str.startsWith("[ar:")) {
			// 取得歌手信息
			String artist = str.substring(4, str.length() - 1);
			System.out.println("Java: artist--->" + artist);
			lrcinfo.setArtist(artist);
		}
		else if (str.startsWith("[al:")) {
			// 取得专辑信息
			String album = str.substring(4, str.length() - 1);
			System.out.println("Java: album--->" + album);
			lrcinfo.setAlbum(album);
		}
		else {
			// 通过正则取得每句歌词信息
			
			// 设置正则规则
			String reg = "\\[(\\d{2}:\\d{2})\\]"; // "\[(\d{2}:\d{2})\]"
			boolean bTimeMsec = false;
			
			if (str.contains(".")) {
				reg = "\\[(\\d{2}:\\d{2}\\.\\d{2})\\]"; // "\[(\d{2}:\d{2}\.\d{2})\]";
				bTimeMsec = true;
			}

			// 编译
			Pattern pattern = Pattern.compile(reg);
			Matcher matcher = pattern.matcher(str);

			// 如果存在匹配项，则执行以下操作
			while (matcher.find()) {
				// 得到这个匹配项中的所有内容和组数
				int groupCount = matcher.groupCount();
				System.out.println(String.format("Java: group %s, count %d", matcher.group(), groupCount));
				
				// 得到这个匹配项开始的索引
				int start = matcher.start();
				// 得到这个匹配项结束的索引
				int end = matcher.end();
				System.out.println(String.format("Java: start\'[\' %d, end\']\'  %d", start, end));
				
				long currentTime = 0;//存放临时时间
				String currentContent = "";//存放临时歌词
				// 得到每个组中内容
				for (int i = 0; i <= groupCount; i++) {
					String timeStr = matcher.group(i);
					System.out.println(String.format("Java: group #%d %s", i, timeStr));
					
					if (i == 1) {
						// 将第二组中的内容设置为当前的一个时间点
						if (bTimeMsec)
							currentTime = strToLongMsec(timeStr);
						else
							currentTime = strToLong(timeStr);
					}
				}

				// 得到时间点后的内容
				String[] content = pattern.split(str); // [aaa], [bbb], [ccc], lrc_text
				if (content.length > 0) {
					System.out.println(String.format("Java: content size %d, duplicated src %d", 
							content.length, content.length - 1));
					currentContent = content[content.length - 1];
				}
				else {
					currentContent = "";
				}
				
				if (currentContent.isEmpty() && lastLine != null)
					currentContent = lastLine;
				
				// 设置时间点和内容的映射
				lrcList.add(new TimeLrc(currentContent, currentTime, 0));
				System.out.println("Java: put---currentTime--->" + currentTime
						+ "----currentContent---->" + currentContent);

				if (!currentContent.isEmpty())
					lastLine = currentContent;
			}
		}
	}

	/**
	 * 将解析得到的表示时间的字符转化为Long型
	 * 
	 * @param group
	 *            字符形式的时间点
	 * @return Long形式的时间
	 */
	private long strToLong(String timeStr) {
		// 因为给如的字符串的时间格式为XX:XX.XX,返回的long要求是以毫秒为单位
		// 1:使用：分割 2：使用.分割
		String[] s = timeStr.split(":");
		int min = Integer.parseInt(s[0]);
		/*String[] ss = s[1].split("\\.");
		int sec = Integer.parseInt(ss[0]);
		int mill = Integer.parseInt(ss[1]);
		return min * 60 * 1000 + sec * 1000 + mill * 10;
		*/
		int sec = Integer.parseInt(s[1]);
		return min * 60 * 1000 + sec * 1000;
	}
	
	private long strToLongMsec(String timeStr) {
		// 因为给如的字符串的时间格式为XX:XX.XX,返回的long要求是以毫秒为单位
		// 1:使用：分割 2：使用.分割
		String[] s = timeStr.split(":");
		int min = Integer.parseInt(s[0]);
		String[] ss = s[1].split("\\.");
		int sec = Integer.parseInt(ss[0]);
		int mill = Integer.parseInt(ss[1]);
		return min * 60 * 1000 + sec * 1000 + mill * 10;
	}
}