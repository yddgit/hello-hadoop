package com.my.project;

import java.io.FileInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

/**
 * HDFS入门
 * @author yang
 *
 */
public class HelloHDFS {

	public static void main(String[] args) throws Exception {
		/*
		URL url = new URL("http://www.baidu.com");
		InputStream in = url.openStream();
		IOUtils.copyBytes(in, System.out, 4096, true);
		*/

		//示例1：读取HDFS上的文件
		/*
		URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
		URL url = new URL("hdfs://192.168.56.10:9000/test.txt");
		InputStream in = url.openStream();
		IOUtils.copyBytes(in, System.out, 4096, true);
		*/

		//示例2：使用FileSystem类
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://192.168.56.10:9000");
		conf.set("dfs.replication", "2"); //设置dfs.replication属性
		FileSystem fileSystem = FileSystem.get(conf);

		//创建删除目录，判断目录是否存在
		boolean exists = fileSystem.exists(new Path("/hello"));
		System.out.println("/hello is exists? " + exists);
		boolean success = fileSystem.mkdirs(new Path("/ydd"));
		System.out.println("create dir /ydd success? " + success);
		boolean deleted = fileSystem.delete(new Path("/ydd"), true);
		System.out.println("delete /ydd success? " + deleted);
		boolean check = fileSystem.exists(new Path("/ydd"));
		System.out.println("/ydd is exists? " + check);

		//上传文件到HDFS1
		FSDataOutputStream out = fileSystem.create(new Path("/test.data"), true);
		FileInputStream fis = new FileInputStream("hadoop.txt");
		IOUtils.copyBytes(fis, out, 4096, true);
	
		//上传文件到HDFS2
		/*
		FSDataOutputStream out = fileSystem.create(new Path("/test.data"), true);
		FileInputStream fis = new FileInputStream("hadoop.txt");
		byte[] buf = new byte[4096];
		int len = fis.read(buf);
		while(len != -1) {
			out.write(buf, 0, len);
			len = fis.read(buf);
		}
		fis.close();
		out.close();
		*/

		//列出根目录下的文件状态
		FileStatus[] statuses = fileSystem.listStatus(new Path("/"));
		for(FileStatus status : statuses) {
			System.out.print(status.getPath() + "\t");
			System.out.print(status.getPermission() + "\t");
			System.out.print(status.getReplication() + "\t");
			System.out.print(status.getLen() + "\n");
		}
	}

}
