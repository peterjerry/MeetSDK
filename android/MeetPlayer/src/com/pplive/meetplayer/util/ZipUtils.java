package com.pplive.meetplayer.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

	/**
	 * ѹ��һ���ļ�
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	public static void zipFile(String source, String target) throws IOException {
		File sourceFile = new File(source);
		File targetFile = new File(target);
		if (!sourceFile.exists()) {
			sourceFile.createNewFile();
		}
		if (!targetFile.exists()) {
			targetFile.createNewFile();
		}
		
		FileInputStream fis = new FileInputStream(sourceFile);
		FileOutputStream fos = new FileOutputStream(targetFile);
		GZIPOutputStream gzos = new GZIPOutputStream(fos);
		
		int b = 0;
		byte[] buffer = new byte[1024];
		while ((b = fis.read(buffer)) != -1) {
			gzos.write(buffer, 0, b);
		}
		gzos.finish();
		gzos.flush();
		closeStream(gzos);
		closeStream(fis);
		closeStream(fos);
	}
	
	/**
	 * ѹ������ļ�
	 * @param files
	 * @param zipFilePath
	 * @throws IOException
	 */
	public static void zipFiles(File[] files, String zipFilePath) throws IOException {
		File zipFile = new File(zipFilePath);
		if (!zipFile.exists()) {
			zipFile.createNewFile();
		}
		
		FileOutputStream fos = new FileOutputStream(zipFile);;
		ZipOutputStream zos = new ZipOutputStream(fos);
		for (File file:files) {
			FileInputStream fis = new FileInputStream(file);
			zos.putNextEntry(new ZipEntry(file.getName()));
			
			int b = 0;
			byte[] buffer = new byte[1024];
			while ((b = fis.read(buffer)) != -1) {
				zos.write(buffer, 0, b);
			}
			zos.closeEntry();
			closeStream(fis);
		}
		
		closeStream(zos);
		closeStream(fos);
	}
	
	private static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
