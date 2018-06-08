package com.game.service.classes.loader;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;

import com.game.common.constant.Loggers;

/**
 * 
 * @author JiangBangMing
 *
 * 2018年6月1日 下午2:05:09
 */
public class FileClassLoader extends ClassLoader{
	
	public static final Logger logger=Loggers.serverLogger;
	
	private Hashtable<String, Class> loadedClasses;
	private File rootPath;
	private HashMap<String, byte[]> classFiles;
	
	public FileClassLoader(File root) throws Exception{
		classFiles=new HashMap<>();
		loadedClasses=new Hashtable<>();
		if(root.isDirectory()) {
			rootPath=root;
		}else {
			loadJarContent(root);
		}
	}
	
	public void initJarPath(JarFile root) throws Exception{
		rootPath=null;
		loadJarContent(root);
	}
	
	public void initClassPath(File root,String classString) throws Exception{
		if(root.getName().endsWith(".class")) {
			//class
			byte[] classData=loadFileData(root);
			classFiles.put(classString, classData);
		}
	}
	
	public static byte[] loadFileData(File src) throws IOException{
		FileInputStream fis=null;
		try{
			fis=new FileInputStream(src);
			BufferedInputStream bis=new BufferedInputStream(fis);
			return getBytesFromInput(bis);
		}catch(IOException e) {
			throw e;
		}finally {
			if(fis!=null) {
				try {
					fis.close();
				}catch(IOException e) {
					
				}
			}
		}
	}
	
	public synchronized Class loadClass(String className,boolean resolve) throws ClassNotFoundException{
		Class newClass;
		byte[] classData;
		
		newClass=loadedClasses.get(className);
		
		if(newClass!=null) {
			if(resolve) {
				resolveClass(newClass);
			}
			return newClass;
		}
		
		try {
			newClass=this.findSystemClass(className);
			if(newClass!=null) {
				return newClass;
			}
		}catch(ClassNotFoundException e) {
			throw new ClassNotFoundException();
		}
		
		try {
			classData=getClassData(className);
			if(classData!=null) {
				
				newClass=defineClass(className, classData, 0,classData.length);
				if(newClass!=null) {
					logger.info("class loader put:"+className);
					loadedClasses.put(className, newClass);
					if(resolve) {
						resolveClass(newClass);
					}
					return newClass;
				}
			}
		}catch(Exception e) {
			
		}
		
		loadedClasses.put(className, newClass);
		if(resolve) {
			resolveClass(newClass);
		}
		return newClass;
		
	}
	
	public byte[] getClassData(String className) throws IOException{
		if(rootPath!=null) {
			try {
				String subPath=className.replace('.','/')+".class";
				File file=new File(rootPath,subPath);
				return loadFileData(file);
			}catch(Exception e) {
				throw new IOException(className);
			}
		}else {
			return classFiles.get(className);
		}
	}
	private void loadJarContent(JarFile jf) throws Exception{
		Enumeration<JarEntry> ee=jf.entries();
		while(ee.hasMoreElements()) {
			JarEntry je=ee.nextElement();
			if(je.getName().endsWith(".class")) {
				String className=je.getName().substring(0, je.getName().length()-6);
				className=className.replace('/','.');
				InputStream is=jf.getInputStream(je);
				byte[] classData=getBytesFromInput(is);
				is.getClass();
				logger.info("jar load Name()"+ jf.getName()+" class"+className);
				classFiles.put(className, classData);
			}
		}
		jf.close();
	}
	
	private void loadJarContent(File f) throws Exception{
		JarFile jf=new JarFile(f);
		loadJarContent(jf);
	}
	
	private static byte[] getBytesFromInput(InputStream in) throws IOException{
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		int rd=0;
		int len=0;
		byte[] buf=new byte[64];
		while((rd=in.read(buf))!=-1) {
			len+=rd;
			out.write(buf,0,rd);
		}
		byte[] rt=out.toByteArray();
		out.close();
		return rt;
	}
}
